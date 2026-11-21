package com.pagelence.sync

import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.SocketTimeoutException
import java.util.Base64
import java.util.concurrent.TimeUnit
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response

/**
 * WebDAV over OkHttp.
 *
 * Deliberate choices:
 * - redirects are **not** followed: a 302 to a login page would otherwise look
 *   like a 200 with HTML, and the failure would surface as "invalid JSON";
 * - preemptive Basic auth (servers that answer 401 first cost a round trip);
 * - every request has a hard timeout;
 * - downloads are capped by Content-Length *and* by a streaming counter, so a
 *   lying or missing header cannot exhaust memory;
 * - PUT bodies reuse the exact bytes written, never the response length.
 *
 * See [WebDavModels.PutCondition] for the conditional-write contract.
 */
class OkHttpWebDavClient(
    private val http: OkHttpClient = defaultHttpClient(),
    override val maxBytes: Long = DEFAULT_MAX_BYTES,
) : WebDavClient {

    override fun head(credentials: WebDavCredentials): RemoteFile {
        val response = exchange(credentials) { builder ->
            builder.url(WebDavUrl.validateFileUrl(credentials.url, credentials.allowHttp))
                .head()
                .build()
        }
        response.use {
            return when {
                it.isSuccessful -> metaOf(it)
                it.code == 404 || it.code == 410 -> RemoteFile.Missing
                it.code == 405 || it.code == 501 -> headViaRangeGet(credentials)
                else -> throw statusError(it.code)
            }
        }
    }

    override fun get(credentials: WebDavCredentials): RemoteContent? {
        val response = exchange(credentials) { builder ->
            builder.url(WebDavUrl.validateFileUrl(credentials.url, credentials.allowHttp))
                .get()
                .build()
        }
        response.use {
            if (it.code == 404 || it.code == 410) return null
            if (!it.isSuccessful) throw statusError(it.code)
            val declared = it.header("Content-Length")?.toLongOrNull()
            if (declared != null && declared > maxBytes) throw tooLargeError(declared)
            val text = readCapped(it, maxBytes)
            return RemoteContent(
                text = text,
                etag = Etag.normalize(it.header("ETag")),
                size = text.toByteArray(Charsets.UTF_8).size.toLong(),
            )
        }
    }

    override fun readPrefix(credentials: WebDavCredentials, maxBytes: Int): String? {
        val limit = maxBytes.coerceAtLeast(1)
        val response = exchange(credentials) { builder ->
            builder.url(WebDavUrl.validateFileUrl(credentials.url, credentials.allowHttp))
                .header("Range", "bytes=0-${limit - 1}")
                .get()
                .build()
        }
        response.use {
            if (it.code == 404 || it.code == 410) return null
            if (!it.isSuccessful) throw statusError(it.code)
            return readPrefixText(it, limit)
        }
    }

    override fun put(
        credentials: WebDavCredentials,
        body: String,
        condition: PutCondition,
        contentType: String,
    ): RemoteFile {
        val bytes = body.toByteArray(Charsets.UTF_8)
        if (bytes.size.toLong() > maxBytes) throw tooLargeError(bytes.size.toLong())
        return try {
            putOnce(credentials, bytes, condition, contentType)
        } catch (error: SyncException) {
            // 409 = missing parent collection. Apache mod_dav and Nextcloud both
            // do this; create the ancestors once and retry the same write.
            if (error.status == 409) {
                ensureDirectories(credentials)
                putOnce(credentials, bytes, condition, contentType)
            } else {
                throw error
            }
        }
    }

    override fun ensureDirectories(credentials: WebDavCredentials) {
        val fileUrl = WebDavUrl.validateFileUrl(credentials.url, credentials.allowHttp)
        for (directory in WebDavUrl.ancestorDirectories(fileUrl)) {
            val response = exchange(credentials) { builder ->
                builder.url(directory).method("MKCOL", null).build()
            }
            response.use {
                when (it.code) {
                    200, 201, 204, 301, 302, 405, 409 -> Unit
                    else -> throw statusError(it.code)
                }
            }
        }
    }

    private fun headViaRangeGet(credentials: WebDavCredentials): RemoteFile {
        val response = exchange(credentials) { builder ->
            builder.url(WebDavUrl.validateFileUrl(credentials.url, credentials.allowHttp))
                .header("Range", "bytes=0-0")
                .get()
                .build()
        }
        response.use {
            if (it.code == 404 || it.code == 410) return RemoteFile.Missing
            if (!it.isSuccessful) throw statusError(it.code)
            val meta = metaOf(it)
            if (it.code == 206) {
                // 206 reports one byte in Content-Length; the real size is in
                // Content-Range: bytes 0-0/12345.
                val total = it.header("Content-Range")
                    ?.substringAfterLast('/')
                    ?.toLongOrNull()
                return if (total != null) meta.copy(size = total) else meta
            }
            return meta
        }
    }

    private fun putOnce(
        credentials: WebDavCredentials,
        bytes: ByteArray,
        condition: PutCondition,
        contentType: String,
    ): RemoteFile {
        val response = exchange(credentials) { builder ->
            val request = builder
                .url(WebDavUrl.validateFileUrl(credentials.url, credentials.allowHttp))
                .put(bytes.toRequestBody(contentType.toMediaType()))
            when (condition) {
                is PutCondition.IfMatch -> {
                    val header = Etag.toHeader(condition.etag)
                        ?: throw SyncException(
                            "条件写缺少 ETag，已中止",
                            SyncErrorCategory.OTHER,
                            "MISSING_ETAG",
                        )
                    request.header("If-Match", header)
                }

                PutCondition.MustNotExist -> request.header("If-None-Match", "*")
                PutCondition.Force -> Unit
            }
            request.build()
        }
        response.use {
            if (it.isSuccessful) {
                return RemoteFile(
                    exists = true,
                    etag = Etag.normalize(it.header("ETag")),
                    // Response Content-Length describes the response body, not
                    // the stored file; use the bytes we actually sent.
                    size = bytes.size.toLong(),
                    lastModified = it.header("Last-Modified"),
                )
            }
            throw statusError(it.code)
        }
    }

    private fun exchange(
        credentials: WebDavCredentials,
        build: (Request.Builder) -> Request,
    ): Response {
        val builder = Request.Builder()
            .header("Accept", "*/*")
            .header("User-Agent", "Pagelence/1.0 (Android)")
            .header("Cache-Control", "no-store")
        authHeader(credentials)?.let { builder.header("Authorization", it) }
        val request = build(builder)
        return try {
            http.newCall(request).execute()
        } catch (error: SocketTimeoutException) {
            throw networkError("请求超时（${http.callTimeoutMillis / 1000}s），请检查网络或服务器地址", error)
        } catch (error: IOException) {
            throw networkError("连接失败：请检查网络、HTTPS 证书与服务器地址", error)
        } catch (error: IllegalArgumentException) {
            throw SyncException(
                "地址不合法：${error.message.orEmpty()}",
                SyncErrorCategory.URL,
                "INVALID_URL",
                0,
                error,
            )
        }
    }

    private fun authHeader(credentials: WebDavCredentials): String? {
        if (credentials.username.isBlank() && credentials.password.isBlank()) return null
        if (credentials.username.contains(':')) {
            throw SyncException(
                "用户名不能包含冒号",
                SyncErrorCategory.URL,
                "COLON_IN_USERNAME",
            )
        }
        val raw = "${credentials.username}:${credentials.password}"
        val encoded = Base64.getEncoder().encodeToString(raw.toByteArray(Charsets.UTF_8))
        return "Basic $encoded"
    }

    private fun metaOf(response: Response): RemoteFile = RemoteFile(
        exists = true,
        etag = Etag.normalize(response.header("ETag")),
        size = response.header("Content-Length")?.toLongOrNull()?.coerceAtLeast(0) ?: 0,
        lastModified = response.header("Last-Modified"),
    )

    private fun readCapped(response: Response, limit: Long): String {
        val stream = response.body.byteStream()
        val output = ByteArrayOutputStream()
        val chunk = ByteArray(16 * 1024)
        var total = 0L
        try {
            while (true) {
                val read = stream.read(chunk)
                if (read <= 0) break
                total += read
                if (total > limit) throw tooLargeError(total)
                output.write(chunk, 0, read)
            }
        } finally {
            runCatching { stream.close() }
        }
        return output.toString(Charsets.UTF_8.name())
    }

    /** Read at most [limit] bytes and disconnect, even if the server ignores Range. */
    private fun readPrefixText(response: Response, limit: Int): String {
        val stream: InputStream = response.body.byteStream()
        val output = ByteArrayOutputStream()
        val chunk = ByteArray(1024)
        var total = 0
        try {
            while (total < limit) {
                val read = stream.read(chunk, 0, minOf(chunk.size, limit - total))
                if (read <= 0) break
                output.write(chunk, 0, read)
                total += read
            }
        } finally {
            runCatching { stream.close() }
        }
        return output.toString(Charsets.UTF_8.name())
    }

    companion object {
        /** 20 MB: the same ceiling for upload and download, so we never upload a file we cannot read back. */
        const val DEFAULT_MAX_BYTES: Long = 20L * 1024 * 1024

        fun defaultHttpClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .callTimeout(45, TimeUnit.SECONDS)
            .followRedirects(false)
            .followSslRedirects(false)
            .build()
    }
}
