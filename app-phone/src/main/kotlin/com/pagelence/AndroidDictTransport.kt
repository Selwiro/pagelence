package com.pagelence

import com.pagelence.core.dict.DictStream
import com.pagelence.core.dict.DictTransport
import com.pagelence.core.source.CatalogRules
import com.pagelence.core.source.SourceLimits
import java.net.HttpURLConnection
import java.net.URL

/**
 * Streaming HTTP for the optional full dictionary.
 *
 * Unlike [AndroidHttpFetcher] this never buffers the body: the table is tens of
 * megabytes and must arrive in chunks, with a `Range` header so a paused
 * download can resume. Every call is still started by an explicit tap.
 */
class AndroidDictTransport(private val allowLocalHttp: () -> Boolean) : DictTransport {
    override fun open(url: String, offset: Long): DictStream {
        if (!CatalogRules.isAllowedUrl(url, allowLocalHttp())) return ClosedStream(code = 0)
        val connection = runCatching {
            URL(url).openConnection() as HttpURLConnection
        }.getOrNull() ?: return ClosedStream(code = 0)
        connection.connectTimeout = SourceLimits.CONNECT_TIMEOUT_MS
        connection.readTimeout = SourceLimits.READ_TIMEOUT_MS
        connection.instanceFollowRedirects = true
        connection.setRequestProperty("User-Agent", "Pagelence/0.1 (Android)")
        // No transparent gzip: byte offsets must match the published file.
        connection.setRequestProperty("Accept-Encoding", "identity")
        if (offset > 0) connection.setRequestProperty("Range", "bytes=$offset-")
        val code = runCatching { connection.responseCode }.getOrElse {
            connection.disconnect()
            return ClosedStream(code = 0)
        }
        val body = if (code in 200..299) runCatching { connection.inputStream }.getOrNull() else null
        return object : DictStream {
            override val code: Int = code

            override fun read(buffer: ByteArray): Int = body?.read(buffer) ?: -1

            override fun close() {
                runCatching { body?.close() }
                connection.disconnect()
            }
        }
    }

    /** A refusal or connection error, shaped as an empty response. */
    private class ClosedStream(override val code: Int) : DictStream {
        override fun read(buffer: ByteArray): Int = -1

        override fun close() = Unit
    }
}
