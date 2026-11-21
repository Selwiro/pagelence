package com.pagelence

import com.pagelence.core.source.CatalogRules
import com.pagelence.core.source.HttpFetcher
import com.pagelence.core.source.HttpResult
import com.pagelence.core.source.SourceLimits
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * The only place the app talks to the network: a small `HttpURLConnection`
 * client with fixed timeouts, a manual redirect cap and a hard byte limit.
 *
 * No cookies, no auth, no background work: every call is started by a user
 * action (refresh the catalog or download a book) and ends with it.
 */
class AndroidHttpFetcher(private val allowLocalHttp: () -> Boolean) : HttpFetcher {
    override fun get(url: String, etag: String?, maxBytes: Long): HttpResult {
        var current = url
        var redirects = 0
        while (true) {
            if (!CatalogRules.isAllowedUrl(current, allowLocalHttp())) return HttpResult(code = 0)
            val connection = try {
                URL(current).openConnection() as HttpURLConnection
            } catch (_: Exception) {
                return HttpResult(code = 0)
            }
            try {
                connection.connectTimeout = SourceLimits.CONNECT_TIMEOUT_MS
                connection.readTimeout = SourceLimits.READ_TIMEOUT_MS
                connection.instanceFollowRedirects = false
                connection.setRequestProperty("User-Agent", "Pagelence/0.1 (Android)")
                connection.setRequestProperty("Accept", "application/json, application/zip, application/octet-stream")
                etag?.let { connection.setRequestProperty("If-None-Match", it) }
                val code = connection.responseCode
                if (code in 300..399) {
                    val location = connection.getHeaderField("Location")
                    if (location.isNullOrBlank()) return HttpResult(code)
                    redirects += 1
                    if (redirects > SourceLimits.MAX_REDIRECTS) return HttpResult(code = 0)
                    current = URL(URL(current), location).toString()
                    continue
                }
                val stream: InputStream? = if (code in 200..299) connection.inputStream else connection.errorStream
                val body = stream?.use { readLimited(it, maxBytes) } ?: ByteArray(0)
                return HttpResult(
                    code = code,
                    body = body,
                    etag = connection.getHeaderField("ETag"),
                    contentType = connection.contentType,
                )
            } catch (_: Exception) {
                return HttpResult(code = 0)
            } finally {
                connection.disconnect()
            }
        }
    }

    /** Reads at most [maxBytes] + 1 so the caller can detect an oversized body. */
    private fun readLimited(stream: InputStream, maxBytes: Long): ByteArray {
        val output = ByteArrayOutputStream()
        val chunk = ByteArray(16 * 1024)
        var total = 0L
        while (true) {
            val read = stream.read(chunk)
            if (read <= 0) break
            total += read
            if (total > maxBytes + 1) break
            output.write(chunk, 0, read)
        }
        return output.toByteArray()
    }
}
