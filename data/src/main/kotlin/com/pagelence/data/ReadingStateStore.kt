package com.pagelence.data

import com.pagelence.core.prefs.Preferences
import com.pagelence.core.progress.BookProgress
import com.pagelence.core.progress.ProgressRepository
import com.pagelence.core.progress.ReadingState
import com.pagelence.core.progress.ReadingStateCodec
import com.pagelence.core.progress.STATE_SCHEMA_VERSION
import com.pagelence.core.wordcards.WordCard
import com.pagelence.core.wordcards.WordCardRepository
import com.pagelence.sync.LocalSnapshot
import com.pagelence.sync.LocalState
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.time.Instant
import java.util.UUID
import java.util.concurrent.CopyOnWriteArrayList

/**
 * The single local `state.json`, frozen by `docs/progress-format.md`.
 *
 * One file holds progress, word cards and preferences, so export/import and
 * WebDAV can reuse it verbatim. Mutations are serialised and written
 * atomically (temp file + rename); a corrupt or missing file falls back to an
 * empty state instead of crashing.
 *
 * This is the M2 storage adapter. It implements the same `:core` repository
 * interfaces Room would, so replacing it later does not touch UI or rules.
 */
class ReadingStateStore(
    private val file: File,
    private val now: () -> String = { Instant.now().toString() },
    private val newDeviceId: () -> String = { UUID.randomUUID().toString() },
) : LocalState {
    private val lock = Any()
    private val backupFile: File? = file.parentFile?.let { File(it, "${file.name}.bak") }
    private val corruptFile: File? = file.parentFile?.let { File(it, "${file.name}.corrupt") }

    /** True when the main file was unreadable and the backup was used instead. */
    @Volatile
    var recoveredFromBackup: Boolean = false
        private set

    /** True when both the main file and its backup were unreadable. */
    @Volatile
    var corruptWithoutBackup: Boolean = false
        private set

    private var cached: ReadingState = load()
    private var revision = 0L
    private val writeListeners = CopyOnWriteArrayList<() -> Unit>()

    init {
        if (cached.deviceId.isBlank()) {
            cached = cached.copy(deviceId = newDeviceId(), updatedAt = now())
            persist(cached)
        }
    }

    fun state(): ReadingState = synchronized(lock) { cached }

    override fun current(): ReadingState = state()

    /** The current document, exactly as export/WebDAV would upload it. */
    fun snapshotJson(): String = synchronized(lock) { ReadingStateCodec.encode(cached) }

    /** Consistent document + revision, used by the sync engine. */
    override fun snapshot(): LocalSnapshot = synchronized(lock) {
        LocalSnapshot(ReadingStateCodec.encode(cached), revision)
    }

    /** Called after every write, including sync write-backs, so sync can debounce. */
    fun addWriteListener(listener: () -> Unit) {
        writeListeners.add(listener)
    }

    /** ISO-8601 timestamp used for `updatedAt` and per-book `lastReadAt`. */
    fun timestamp(): String = now()

    fun update(transform: (ReadingState) -> ReadingState): ReadingState = synchronized(lock) {
        val next = transform(cached).copy(
            schemaVersion = STATE_SCHEMA_VERSION,
            updatedAt = now(),
            deviceId = cached.deviceId,
        )
        persist(next)
        cached = next
        revision += 1
        notifyWrite()
        next
    }

    /**
     * Write a document produced by sync only if nothing was saved locally since
     * [snapshot]; false means the caller must recompute instead of clobbering
     * a fresh card or page turn.
     */
    override fun replaceIfUnchanged(snapshot: LocalSnapshot, state: ReadingState): Boolean =
        synchronized(lock) {
            if (snapshot.revision != revision) return false
            commit(state)
            true
        }

    /** Unconditional replace, used by local import after an explicit merge. */
    override fun replace(state: ReadingState) {
        synchronized(lock) { commit(state) }
    }

    private fun commit(state: ReadingState) {
        val next = state.copy(schemaVersion = STATE_SCHEMA_VERSION)
        persist(next)
        cached = next
        revision += 1
        notifyWrite()
    }

    private fun notifyWrite() {
        writeListeners.forEach { listener -> runCatching(listener) }
    }

    private fun load(): ReadingState {
        val main = decode(file)
        if (main != null) {
            // Upgrade v1 in place; a document from a newer build is read but never
            // rewritten automatically, so unknown fields survive on disk.
            if (main.upgraded) persist(main.state)
            // Keep one known-good copy per process start, not per page turn.
            runCatching { backupFile?.let { file.copyTo(it, overwrite = true) } }
            return main.state
        }

        val backup = decode(backupFile)
        if (backup != null) {
            recoveredFromBackup = true
            // Preserve the broken file for diagnosis, then heal the main file so
            // the notice is one-shot and the next launch starts normally.
            runCatching { corruptFile?.let { file.copyTo(it, overwrite = true) } }
            runCatching { persist(backup.state) }
            return backup.state
        }

        if (file.isFile) {
            corruptWithoutBackup = true
            runCatching { corruptFile?.let { file.copyTo(it, overwrite = true) } }
            val empty = emptyState()
            runCatching { persist(empty) }
            return empty
        }
        return emptyState()
    }

    /** Decode one file; null means missing or unreadable. */
    private fun decode(source: File?): ReadingStateCodec.Decoded? {
        if (source == null || !source.isFile) return null
        return runCatching {
            ReadingStateCodec.decode(source.readText(Charsets.UTF_8))
        }.getOrNull()
    }

    private fun emptyState(): ReadingState = ReadingState(
        schemaVersion = STATE_SCHEMA_VERSION,
        deviceId = newDeviceId(),
        updatedAt = now(),
    )

    private fun persist(state: ReadingState) {
        file.parentFile?.mkdirs()
        val temp = File(file.parentFile, "${file.name}.tmp")
        temp.writeText(ReadingStateCodec.encode(state), Charsets.UTF_8)
        runCatching {
            Files.move(
                temp.toPath(),
                file.toPath(),
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.ATOMIC_MOVE,
            )
        }.onFailure {
            Files.move(temp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING)
        }
    }
}

/** Progress persisted in [ReadingStateStore]. */
class StateProgressRepository(private val store: ReadingStateStore) : ProgressRepository {
    override fun progress(bookId: String): BookProgress = store.state().progress(bookId)

    override fun save(progress: BookProgress): BookProgress {
        val current = store.state().progress(progress.bookId)
        // Page turns and phase changes save repeatedly with the same content;
        // stamping every call would make two devices look "both changed" all
        // the time. Only a real change refreshes lastReadAt (and the document).
        if (progress.copy(lastReadAt = current.lastReadAt) == current) return current
        val stamped = progress.copy(lastReadAt = store.timestamp())
        store.update { state ->
            state.copy(books = state.books + (stamped.bookId to stamped))
        }
        return store.state().progress(stamped.bookId)
    }
}

/** Word cards persisted in [ReadingStateStore], tombstones included. */
class StateWordCardRepository(private val store: ReadingStateStore) : WordCardRepository {
    override fun list(bookId: String, includeDeleted: Boolean): List<WordCard> =
        store.state().wordCards.filter { it.bookId == bookId && (includeDeleted || !it.isDeleted) }

    override fun all(includeDeleted: Boolean): List<WordCard> =
        store.state().wordCards.filter { includeDeleted || !it.isDeleted }

    override fun get(cardId: String): WordCard? = store.state().wordCards.firstOrNull { it.id == cardId }

    override fun find(bookId: String, lemma: String, term: String): WordCard? {
        val wantedLemma = WordCard.lemmaKey(lemma)
        val wantedTerm = WordCard.lemmaKey(term)
        return store.state().wordCards.firstOrNull { card ->
            card.bookId == bookId && !card.isDeleted &&
                (WordCard.lemmaKey(card.lemma) == wantedLemma ||
                    WordCard.lemmaKey(card.term) == wantedTerm)
        }
    }

    override fun add(card: WordCard): WordCard {
        val merged = find(card.bookId, card.lemma, card.term)
        if (merged != null && merged.id != card.id) return merged
        store.update { state ->
            val existing = state.wordCards.firstOrNull { it.id == card.id }
            val stored = if (existing == null) card else card.copy(createdAt = existing.createdAt)
            val cards = if (existing == null) {
                state.wordCards + stored
            } else {
                state.wordCards.map { if (it.id == card.id) stored else it }
            }
            state.copy(wordCards = cards)
        }
        return get(card.id) ?: card
    }

    override fun remove(cardId: String, deletedAt: String): Boolean {
        val existing = get(cardId) ?: return false
        store.update { state ->
            state.copy(
                wordCards = state.wordCards.map {
                    if (it.id == cardId) it.copy(deletedAt = deletedAt, updatedAt = deletedAt) else it
                },
            )
        }
        return true
    }

    override fun isCollected(bookId: String, paragraphId: String, hintId: String): Boolean =
        get(WordCard.idOf(bookId, paragraphId, hintId))?.isDeleted == false
}

/** Preferences persisted in [ReadingStateStore]. */
class StatePreferencesRepository(private val store: ReadingStateStore) {
    fun preferences(): Preferences = store.state().preferences

    fun save(preferences: Preferences): Preferences {
        store.update { it.copy(preferences = preferences) }
        return store.state().preferences
    }
}
