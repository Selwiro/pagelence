package com.pagelence.sync

import com.pagelence.core.progress.ReadingState
import com.pagelence.core.progress.ReadingStateCodec
import com.pagelence.core.prefs.Preferences
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

enum class SyncMode { AUTO, MANUAL }

sealed interface SyncOutcome {
    data class Success(
        val action: SyncAction,
        val summary: MergeSummary? = null,
        /** Local document changed (download or merge), so the UI should refresh. */
        val localChanged: Boolean = false,
        val warnings: List<String> = emptyList(),
        val etag: String? = null,
    ) : SyncOutcome

    data class Failure(
        val category: SyncErrorCategory,
        val code: String,
        val message: String,
        val status: Int = 0,
        val nextAttemptAt: String? = null,
    ) : SyncOutcome

    /** Another sync round holds the lock (manual + auto must share one lock). */
    data object Busy : SyncOutcome

    /** Auto sync is inside its backoff window. */
    data object Skipped : SyncOutcome
}

/**
 * The sync state machine: fetch -> decide -> merge -> conditional write.
 *
 * Invariants copied from the playbook that this project cannot afford to
 * relearn:
 * - `If-Match` only ever receives the ETag fetched in the *same* round; when
 *   the remote is missing the write is `If-None-Match: *`;
 * - a 412 restarts the whole round (fetch again, merge again); it is never a
 *   blind PUT retry with a fresh ETag, which would write a merge computed from
 *   a stale remote;
 * - one lock for manual and automatic sync;
 * - local changes made during the round win: `replaceIfUnchanged` refuses and
 *   the round restarts, up to [maxRestarts] times;
 * - failures are persisted with a stable category and 30/60/120 s backoff
 *   (capped at two minutes);
 * - with no ETag from the server, the remote copy is saved locally before an
 *   unconditional overwrite, because that write cannot be made safe.
 */
class SyncEngine(
    private val client: WebDavClient,
    private val local: LocalState,
    private val stateStore: SyncStateStore,
    private val baselineStore: SyncBaselineStore,
    private val backupDir: Path,
    private val now: () -> Instant = Instant::now,
    private val today: () -> String = { LocalDate.now().toString() },
    private val maxAttempts: Int = 3,
    private val maxRestarts: Int = 2,
) {
    private val lock = ReentrantLock()
    private var last: SyncOutcome? = null

    fun lastOutcome(): SyncOutcome? = last

    fun status(): SyncState = stateStore.load()

    /** HEAD without touching sync state, for "save and test". */
    fun probe(credentials: WebDavCredentials): RemoteFile = client.head(credentials)

    /** Coming back to the foreground clears the wait, as the playbook requires. */
    fun clearBackoff() = lock.withLock {
        val state = stateStore.load()
        if (state.nextAttemptAt != null) stateStore.save(state.copy(nextAttemptAt = null))
    }

    /** Drop ETag, baseline and failure state; used when the server config changes. */
    fun reset() = lock.withLock {
        stateStore.clear()
        baselineStore.clear()
        last = null
    }

    fun sync(
        credentials: WebDavCredentials,
        mode: SyncMode = SyncMode.AUTO,
    ): SyncOutcome {
        if (!lock.tryLock()) return SyncOutcome.Busy
        try {
            val key = serverKey(credentials)
            var state = stateStore.load()
            if (state.serverKey != key) {
                // New address or account: an old baseline belongs to another
                // server and merging against it would produce wrong results.
                stateStore.clear()
                baselineStore.clear()
                state = SyncState(serverKey = key)
                stateStore.save(state)
            }
            if (mode == SyncMode.AUTO && isBackedOff(state)) return SyncOutcome.Skipped
            val outcome = runSync(credentials, state)
            last = outcome
            return outcome
        } finally {
            lock.unlock()
        }
    }

    private fun runSync(credentials: WebDavCredentials, initial: SyncState): SyncOutcome {
        var state = initial
        var attempt = 0
        var restarts = 0
        val warnings = mutableListOf<String>()

        while (attempt < maxAttempts) {
            attempt += 1
            val snapshot = local.snapshot()
            val localState = ReadingStateCodec.decode(snapshot.json)?.state
                ?: return fail(state, SyncException("本机进度文件无法读取", SyncErrorCategory.OTHER, "LOCAL_DECODE"))
            val remote = try {
                client.get(credentials)
            } catch (error: SyncException) {
                return fail(state, error)
            }
            val remoteState = remote?.let { content ->
                ReadingStateCodec.decode(content.text)?.state
                    ?: return fail(
                        state,
                        SyncException(
                            "远端文件不是有效的进度文件，已停止以免覆盖",
                            SyncErrorCategory.OTHER,
                            "REMOTE_DECODE",
                        ),
                    )
            }
            val base = baselineStore.read()?.let { ReadingStateCodec.decode(it)?.state }

            val localHash = hash(localState)
            val remoteHash = remoteState?.let(::hash)
            val localChanged = state.baselineHash == null || localHash != state.baselineHash
            val remoteChanged = when {
                remote == null -> false
                state.lastEtag != null && remote.etag != null ->
                    !Etag.equal(remote.etag, state.lastEtag)

                else -> remoteHash != state.lastRemoteHash
            }
            val hasSynced = state.baselineHash != null || state.lastSuccessAt != null
            val action = SyncDecision.decide(
                localChanged = localChanged,
                remoteExists = remote != null,
                remoteChanged = remoteChanged,
                restoreMissing = hasSynced,
                localBlank = isBlank(localState),
            )

            try {
                when (action) {
                    SyncAction.UP_TO_DATE -> {
                        val next = successState(
                            state = state,
                            etag = remote?.etag,
                            remoteHash = remoteHash,
                            localHash = localHash,
                        )
                        return success(SyncAction.UP_TO_DATE, next, warnings = warnings)
                    }

                    SyncAction.CREATE_REMOTE -> {
                        val written = client.put(credentials, snapshot.json, PutCondition.MustNotExist)
                        if (written.etag == null) warnings.add(WARNING_NO_ETAG)
                        baselineStore.write(snapshot.json)
                        val next = successState(
                            state = state,
                            etag = written.etag,
                            remoteHash = localHash,
                            localHash = localHash,
                        )
                        return success(SyncAction.CREATE_REMOTE, next, warnings = warnings)
                    }

                    SyncAction.UPLOAD -> {
                        val condition = writeCondition(remote, warnings)
                        val written = client.put(credentials, snapshot.json, condition)
                        if (written.etag == null) warnings.add(WARNING_NO_ETAG)
                        baselineStore.write(snapshot.json)
                        val next = successState(
                            state = state,
                            etag = written.etag,
                            remoteHash = localHash,
                            localHash = localHash,
                        )
                        return success(SyncAction.UPLOAD, next, warnings = warnings)
                    }

                    SyncAction.DOWNLOAD -> {
                        val adopted = remoteState!!.copy(
                            deviceId = localState.deviceId,
                            updatedAt = now().toString(),
                        )
                        if (!local.replaceIfUnchanged(snapshot, adopted)) {
                            restarts += 1
                            if (restarts > maxRestarts) return restartExhausted(state)
                            continue
                        }
                        val json = ReadingStateCodec.encode(adopted)
                        baselineStore.write(json)
                        val next = successState(
                            state = state,
                            etag = remote.etag,
                            remoteHash = remoteHash,
                            localHash = hash(adopted),
                        )
                        return success(
                            SyncAction.DOWNLOAD,
                            next,
                            localChanged = true,
                            warnings = warnings,
                            summary = null,
                        )
                    }

                    SyncAction.UPLOAD_MERGED -> {
                        val merged = StateMerge.merge(
                            local = localState,
                            remote = remoteState!!,
                            base = base,
                            now = now().toString(),
                            today = today(),
                        )
                        if (!local.replaceIfUnchanged(snapshot, merged.state)) {
                            restarts += 1
                            if (restarts > maxRestarts) return restartExhausted(state)
                            continue
                        }
                        val json = ReadingStateCodec.encode(merged.state)
                        val condition = writeCondition(remote, warnings)
                        val written = client.put(credentials, json, condition)
                        if (written.etag == null) warnings.add(WARNING_NO_ETAG)
                        baselineStore.write(json)
                        val next = successState(
                            state = state,
                            etag = written.etag,
                            remoteHash = hash(merged.state),
                            localHash = hash(merged.state),
                        )
                        return success(
                            SyncAction.UPLOAD_MERGED,
                            next,
                            localChanged = true,
                            warnings = warnings,
                            summary = merged.summary,
                        )
                    }
                }
            } catch (error: SyncException) {
                if (error.category == SyncErrorCategory.PRECONDITION) {
                    // Someone wrote between our GET and PUT. Restart the whole
                    // round: new ETag, new decision, new merge.
                    continue
                }
                return fail(state, error, attempts = attempt)
            }
        }
        return fail(
            state,
            SyncException(
                "远端一直在变，已自动重试 $maxAttempts 次，请稍后重试",
                SyncErrorCategory.PRECONDITION,
                "PRECONDITION_RETRIES_EXHAUSTED",
                412,
            ),
            attempts = attempt,
        )
    }

    private fun writeCondition(remote: RemoteContent?, warnings: MutableList<String>): PutCondition {
        val etag = remote?.etag
        if (etag != null) return PutCondition.IfMatch(etag)
        // No ETag: the server cannot make the write conditional. Keep a local
        // copy of what is about to be replaced, say so, and overwrite.
        if (remote != null) {
            runCatching { backupRemote(remote.text) }
            warnings.add(WARNING_REMOTE_BACKUP)
        }
        return PutCondition.Force
    }

    private fun backupRemote(text: String) {
        Files.createDirectories(backupDir)
        val name = "remote-${now().toEpochMilli()}.json"
        AtomicFiles.write(backupDir.resolve(name), text)
        val backups = backupDir.toFile()
            .listFiles { file -> file.name.startsWith("remote-") }
            ?.sortedByDescending { it.name }
            .orEmpty()
        backups.drop(MAX_REMOTE_BACKUPS).forEach { runCatching { it.delete() } }
    }

    private fun isBlank(state: ReadingState): Boolean =
        state.books.isEmpty() &&
            state.wordCards.isEmpty() &&
            state.dailyActivity.isEmpty() &&
            state.selfLevel == null &&
            state.levelScore == null &&
            state.recentAssessmentPapers.isEmpty() &&
            state.preferences == Preferences()

    private fun successState(
        state: SyncState,
        etag: String?,
        remoteHash: String?,
        localHash: String,
    ): SyncState = state.copy(
        lastEtag = etag,
        lastRemoteHash = remoteHash,
        baselineHash = localHash,
        lastSuccessAt = now().toString(),
        lastFailureCategory = null,
        lastFailureCode = null,
        lastFailureMessage = null,
        lastFailureAt = null,
        consecutiveFailures = 0,
        nextAttemptAt = null,
    )

    private fun success(
        action: SyncAction,
        state: SyncState,
        localChanged: Boolean = false,
        warnings: List<String> = emptyList(),
        summary: MergeSummary? = null,
    ): SyncOutcome.Success {
        stateStore.save(state)
        return SyncOutcome.Success(
            action = action,
            summary = summary,
            localChanged = localChanged,
            warnings = warnings,
            etag = state.lastEtag,
        )
    }

    private fun restartExhausted(state: SyncState): SyncOutcome {
        val error = SyncException(
            "同步期间本机又有新修改，已重试 $maxRestarts 次，请稍后再同步",
            SyncErrorCategory.PRECONDITION,
            "LOCAL_CHANGED_DURING_SYNC",
        )
        return failureOf(failState(state, error))
    }

    private fun fail(
        state: SyncState,
        error: SyncException,
        attempts: Int = 0,
    ): SyncOutcome.Failure {
        val failed = failState(state, error, attempts)
        stateStore.save(failed)
        return failureOf(failed)
    }

    private fun failState(
        state: SyncState,
        error: SyncException,
        attempts: Int = 0,
    ): SyncState {
        val failures = state.consecutiveFailures + 1
        val next = now().plusSeconds(backoffSeconds(failures))
        return state.copy(
            lastFailureCategory = error.category.name,
            lastFailureCode = error.code,
            lastFailureMessage = if (attempts > 1) "${error.message}（已重试 $attempts 次）" else error.message,
            lastFailureAt = now().toString(),
            consecutiveFailures = failures,
            nextAttemptAt = next.toString(),
        )
    }

    private fun failureOf(state: SyncState): SyncOutcome.Failure = SyncOutcome.Failure(
        category = state.lastFailureCategory?.let {
            runCatching { SyncErrorCategory.valueOf(it) }.getOrDefault(SyncErrorCategory.OTHER)
        } ?: SyncErrorCategory.OTHER,
        code = state.lastFailureCode ?: "SYNC_ERROR",
        message = state.lastFailureMessage ?: "同步失败",
        status = 0,
        nextAttemptAt = state.nextAttemptAt,
    )

    private fun isBackedOff(state: SyncState): Boolean {
        val next = StateMerge.instant(state.nextAttemptAt) ?: return false
        return next.isAfter(now())
    }

    private fun serverKey(credentials: WebDavCredentials): String =
        "${credentials.url}\n${credentials.username}"

    private fun hash(state: ReadingState): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest(ReadingStateCodec.encode(state).toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    companion object {
        private const val MAX_REMOTE_BACKUPS = 3
        private const val WARNING_NO_ETAG = "服务器不返回 ETag，下次同步改用内容比对"
        private const val WARNING_REMOTE_BACKUP = "服务器不支持条件写，已在本地保留覆盖前的远端副本"

        /** 30 -> 60 -> 120, then stay at two minutes; longer feels "never syncs". */
        fun backoffSeconds(failures: Int): Long {
            val step = (failures - 1).coerceIn(0, 2)
            return minOf(120L, 30L * (1L shl step))
        }

        fun backoffDuration(failures: Int): Duration = Duration.ofSeconds(backoffSeconds(failures))
    }
}
