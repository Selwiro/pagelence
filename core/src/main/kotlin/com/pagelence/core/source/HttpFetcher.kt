package com.pagelence.core.source

/** Result of one HTTP GET; [body] is empty for 304 responses. */
data class HttpResult(
    val code: Int,
    val body: ByteArray = ByteArray(0),
    val etag: String? = null,
    val contentType: String? = null,
)

/**
 * The only network seam. The app implements it with `HttpURLConnection`; tests
 * inject a fake, so catalog, download and package rules stay device-free.
 *
 * Implementations must: follow at most [SourceLimits.MAX_REDIRECTS], enforce
 * [maxBytes] while reading, and reject non-HTTPS URLs unless the caller allows
 * local HTTP.
 */
interface HttpFetcher {
    fun get(url: String, etag: String?, maxBytes: Long): HttpResult
}

/** Always failing fetcher for offline builds and previews. */
class OfflineHttpFetcher : HttpFetcher {
    override fun get(url: String, etag: String?, maxBytes: Long): HttpResult = HttpResult(code = 0)
}
