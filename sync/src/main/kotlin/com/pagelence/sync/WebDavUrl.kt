package com.pagelence.sync

import java.net.URI
import java.net.URISyntaxException

/**
 * Validation for the one URL the user configures: the full path of the state
 * file on the WebDAV server.
 *
 * Measured mistakes (all produce "save failed"-looking errors on real servers):
 * - a directory URL (`.../dav/`) => PUT answers 409/405;
 * - account and password inside the URL => the password reaches logs;
 * - a query string or fragment => every request carries it and caches differ;
 * - plain `http://` to the public internet => password and data travel in clear.
 *
 * Cleartext is allowed only for the loopback / emulator host range, mirroring
 * `network_security_config.xml`; the Android platform blocks the rest anyway.
 */
object WebDavUrl {
    private val LOCAL_HOSTS = setOf("localhost", "127.0.0.1", "[::1]", "::1", "10.0.2.2")

    /**
     * @param allowHttp user explicitly enabled the debug-only cleartext switch.
     * @return the normalised absolute URL.
     * @throws SyncException category [SyncErrorCategory.URL].
     */
    fun validateFileUrl(input: String, allowHttp: Boolean = false): String {
        val raw = input.trim()
        if (raw.isEmpty()) throw urlError("请填写 WebDAV 文件地址", "EMPTY_URL")

        val uri = try {
            URI(raw)
        } catch (_: URISyntaxException) {
            throw urlError("地址格式不正确，请填写完整的 http(s) 文件地址", "INVALID_URL")
        }
        val scheme = uri.scheme?.lowercase()
        if (scheme != "https" && scheme != "http") {
            throw urlError("只支持 http 或 https 地址", "UNSUPPORTED_SCHEME")
        }
        if (uri.host.isNullOrBlank()) throw urlError("地址缺少服务器名", "MISSING_HOST")
        if (uri.rawUserInfo != null) {
            throw urlError("请不要把账号密码写进地址，改用独立的用户名 / 密码字段", "CREDENTIALS_IN_URL")
        }
        if (uri.rawQuery != null || uri.rawFragment != null) {
            throw urlError("请填写不含查询参数或锚点的文件地址", "QUERY_OR_HASH_IN_URL")
        }
        val path = uri.rawPath.orEmpty()
        if (path == "/" || path.isEmpty() || path.endsWith("/")) {
            throw urlError("请填写完整的文件地址（以文件名结尾，不能以 / 结尾）", "NOT_A_FILE_URL")
        }
        if (scheme == "http") {
            val local = LOCAL_HOSTS.contains(uri.host?.lowercase())
            if (!allowHttp || !local) {
                throw urlError("请使用 HTTPS 地址（本机调试地址除外）", "HTTP_NOT_ALLOWED")
            }
        }
        return uri.toString()
    }

    /**
     * Parent directories that must exist before PUT, shallow first.
     * `https://h/dav/a/state.json` -> `https://h/dav/`, `https://h/dav/a/`.
     */
    fun ancestorDirectories(fileUrl: String): List<String> {
        val uri = URI(fileUrl)
        val segments = uri.rawPath.orEmpty().split('/').filter { it.isNotEmpty() }
        if (segments.size <= 1) return emptyList()
        val scheme = uri.scheme
        val authority = uri.rawAuthority ?: return emptyList()
        var path = ""
        return segments.dropLast(1).map { segment ->
            path += "/$segment"
            "$scheme://$authority$path/"
        }
    }

    private fun urlError(message: String, code: String): SyncException =
        SyncException(message, SyncErrorCategory.URL, code)
}
