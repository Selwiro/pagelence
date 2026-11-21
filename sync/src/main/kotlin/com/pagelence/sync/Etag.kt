package com.pagelence.sync

/**
 * ETag helpers.
 *
 * Servers disagree on weak markers and quoting: HEAD may return `W/"a"` while
 * PUT returns `"a"`. Storing the raw header and comparing it verbatim makes
 * "unchanged" look "changed", which turns into an upload on every sync.
 *
 * Normalise before storing and comparing; re-add quotes only when writing the
 * `If-Match` header. Comparison is case-sensitive (ETags are).
 */
object Etag {
    /** Strip `W/`, surrounding double quotes and whitespace; blank -> null. */
    fun normalize(raw: String?): String? {
        var value = raw?.trim().orEmpty()
        if (value.startsWith("W/", ignoreCase = true)) value = value.substring(2).trim()
        if (value.length >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
            value = value.substring(1, value.length - 1)
        }
        return value.ifBlank { null }
    }

    /** Normalised ETag -> legal header value (quotes restored). */
    fun toHeader(normalized: String?): String? {
        val value = normalized?.trim().orEmpty()
        if (value.isEmpty()) return null
        return if (value.startsWith("\"")) value else "\"$value\""
    }

    /** Compare two raw or normalised ETags. Null never equals another value. */
    fun equal(a: String?, b: String?): Boolean {
        val left = normalize(a) ?: return false
        val right = normalize(b) ?: return false
        return left == right
    }
}
