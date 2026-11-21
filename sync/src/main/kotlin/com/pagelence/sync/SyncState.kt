package com.pagelence.sync

import java.nio.file.Path
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Sync bookkeeping, persisted next to `state.json`.
 *
 * It must survive restarts: a failure kept only in memory disappears when the
 * app is killed and the reader believes sync is fine. The last failure and the
 * backoff deadline are therefore written here, while the password never is.
 */
@Serializable
data class SyncState(
    val version: Int = 1,
    /** URL + username; a change clears the baseline so a new server is never merged against the old one. */
    val serverKey: String? = null,
    /** ETag observed by the *last successful* fetch/write; never reused for If-Match after a failed fetch. */
    val lastEtag: String? = null,
    /** Content hash when the server gives no ETag (some proxies strip it). */
    val lastRemoteHash: String? = null,
    /** Hash of the document both sides agreed on after the last success. */
    val baselineHash: String? = null,
    val lastSuccessAt: String? = null,
    val lastFailureCategory: String? = null,
    val lastFailureCode: String? = null,
    val lastFailureMessage: String? = null,
    val lastFailureAt: String? = null,
    val consecutiveFailures: Int = 0,
    /** AUTO sync refuses to run before this instant; manual sync ignores it. */
    val nextAttemptAt: String? = null,
)

class SyncStateStore(private val file: Path) {
    fun load(): SyncState {
        val text = AtomicFiles.read(file) ?: return SyncState()
        return runCatching { JSON.decodeFromString(SyncState.serializer(), text) }.getOrNull() ?: SyncState()
    }

    fun save(state: SyncState) {
        AtomicFiles.write(file, JSON.encodeToString(SyncState.serializer(), state))
    }

    fun clear() {
        AtomicFiles.delete(file)
    }

    companion object {
        private val JSON = Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }
    }
}

/**
 * The last successfully synced document, used as the three-way base.
 *
 * Written only after a successful upload/download/merge. A read failure simply
 * degrades to a base-less merge; it never blocks sync.
 */
class SyncBaselineStore(private val file: Path) {
    fun read(): String? = AtomicFiles.read(file)

    fun write(json: String) = AtomicFiles.write(file, json)

    fun clear() = AtomicFiles.delete(file)
}
