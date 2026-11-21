package com.pagelence.sync

/** Server-side file metadata; [exists] is false for 404/410, never an error. */
data class RemoteFile(
    val exists: Boolean,
    val etag: String? = null,
    val size: Long = 0,
    val lastModified: String? = null,
) {
    companion object {
        val Missing = RemoteFile(exists = false)
    }
}

/** A downloaded document plus the ETag observed on the same response. */
data class RemoteContent(
    val text: String,
    val etag: String? = null,
    val size: Long = 0,
)

/**
 * The only way a PUT may be issued. Exactly one mode must be chosen; there is
 * deliberately no "fall back to last synced ETag" convenience — that is the
 * root cause of the permanent-412 incident: after the remote file is deleted,
 * an old ETag can never match, so every retry fails forever.
 */
sealed interface PutCondition {
    /** Use the ETag fetched in *this* sync round. */
    data class IfMatch(val etag: String) : PutCondition

    /** Remote must not exist (`If-None-Match: *`). */
    data object MustNotExist : PutCondition

    /** User confirmed "overwrite the remote"; no condition header. */
    data object Force : PutCondition
}

/** Connection details; the password never leaves the client layer. */
data class WebDavCredentials(
    val url: String,
    val username: String = "",
    val password: String = "",
    val allowHttp: Boolean = false,
)

/** Protocol client. Implementations do no business decisions on purpose. */
interface WebDavClient {
    val maxBytes: Long

    /** HEAD with a `GET Range: bytes=0-0` fallback for servers that reject HEAD. */
    fun head(credentials: WebDavCredentials): RemoteFile

    /** Whole document, or null when the remote does not exist. */
    fun get(credentials: WebDavCredentials): RemoteContent?

    /** First [maxBytes] bytes, for format sniffing; null when missing. */
    fun readPrefix(credentials: WebDavCredentials, maxBytes: Int = 256): String?

    /** Conditional write; creates missing parent directories once on 409. */
    fun put(
        credentials: WebDavCredentials,
        body: String,
        condition: PutCondition,
        contentType: String = "application/json; charset=utf-8",
    ): RemoteFile

    /** Idempotent MKCOL for every parent directory. */
    fun ensureDirectories(credentials: WebDavCredentials)
}
