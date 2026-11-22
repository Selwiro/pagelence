package com.pagelence.data

import com.pagelence.core.content.PagelenceJson
import com.pagelence.core.lookup.LookupEntry
import com.pagelence.core.lookup.LookupHistoryRepository
import com.pagelence.core.lookup.LookupHistoryRules
import com.pagelence.core.prefs.Preferences
import com.pagelence.core.progress.Annotation
import com.pagelence.core.progress.AnnotationRepository
import com.pagelence.core.progress.AnnotationRules
import com.pagelence.core.progress.BookProgress
import com.pagelence.core.progress.ProgressRepository
import com.pagelence.core.progress.ReaderPosition
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
import java.time.Duration
import java.time.Instant
import java.util.UUID
import java.util.concurrent.CopyOnWriteArrayList
import kotlinx.serialization.Serializable

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

    /**
     * Page turns only need a few hundred bytes; writing the whole document on
     * every turn is what made the file grow with the card list. The journal is
     * replayed on load and folded into `state.json` on the next full write.
     */
    private val journalFile: File? = file.parentFile?.let { File(it, "reader-position.json") }

    /** The document as loaded, so a write can carry fields this build cannot see. */
    private var rawDocument: kotlinx.serialization.json.JsonObject? = null

    /** Version to write: this build's, unless the document came from a newer one. */
    private var documentVersion: Int = STATE_SCHEMA_VERSION

    /** True when the main file was unreadable and the backup was used instead. */
    @Volatile
    var recoveredFromBackup: Boolean = false
        private set

    /** True when both the main file and its backup were unreadable. */
    @Volatile
    var corruptWithoutBackup: Boolean = false
        private set

    private var cached: ReadingState = load().let(::applyJournal)
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
            schemaVersion = documentVersion,
            updatedAt = now(),
            deviceId = cached.deviceId,
        )
        persist(next)
        clearJournal()
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

    /**
     * Copy the current on-disk document to `state.json.bak` before a destructive
     * import overwrite. False means the copy failed, so the caller must abort
     * instead of replacing a state it could not back up.
     */
    fun backupNow(): Boolean = synchronized(lock) {
        val backup = backupFile ?: return@synchronized false
        if (!file.isFile) return@synchronized false
        try {
            file.copyTo(backup, overwrite = true)
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun commit(state: ReadingState) {
        val next = state.copy(schemaVersion = documentVersion)
        persist(next)
        clearJournal()
        cached = next
        revision += 1
        notifyWrite()
    }

    /**
     * Save just the reader's place. Small atomic write, no rewrite of the whole
     * document; the in-memory state already carries it, so sync/export see it.
     */
    fun saveReaderPoint(bookId: String, paragraphId: String?, position: ReaderPosition) {
        synchronized(lock) {
            val book = cached.books[bookId] ?: BookProgress(bookId = bookId)
            val next = cached.copy(
                books = cached.books + (
                    bookId to book.copy(
                        paragraphId = paragraphId ?: book.paragraphId,
                        reader = position,
                    )
                    ),
            )
            val journal = journalFile
            val written = journal != null && runCatching {
                writeAtomically(
                    journal,
                    PagelenceJson.encodeToString(
                        ReaderJournal.serializer(),
                        ReaderJournal(bookId, paragraphId, position, now()),
                    ),
                )
            }.isSuccess
            if (!written) {
                // No journal path or a failed write: fall back to the full document.
                commit(next)
                return
            }
            cached = next
            revision += 1
            notifyWrite()
        }
    }

    private fun clearJournal() {
        runCatching { journalFile?.delete() }
    }

    private fun applyJournal(state: ReadingState): ReadingState {
        val journal = readJournal() ?: return state
        if (journal.position.unitId.isBlank()) return state
        val book = state.books[journal.bookId] ?: run {
            clearJournal()
            return state
        }
        val resumed = book.copy(
            paragraphId = journal.paragraphId ?: book.paragraphId,
            reader = journal.position,
        )
        return state.copy(books = state.books + (journal.bookId to resumed))
    }

    private fun readJournal(): ReaderJournal? {
        val source = journalFile ?: return null
        if (!source.isFile) return null
        return runCatching {
            PagelenceJson.decodeFromString(ReaderJournal.serializer(), source.readText(Charsets.UTF_8))
        }.getOrNull()
    }

    /**
     * Drop data that only grows: tombstones older than [TOMBSTONE_RETENTION_DAYS]
     * and guide-visited ids of books that no longer have progress.
     */
    private fun compact(state: ReadingState): ReadingState {
        val cutoff = runCatching { Instant.parse(now()).minus(Duration.ofDays(TOMBSTONE_RETENTION_DAYS)) }
            .getOrNull()
        val cards = if (cutoff == null) {
            state.wordCards
        } else {
            state.wordCards.filterNot { card -> deletedBefore(card.deletedAt, cutoff) }
        }
        val annotations = if (cutoff == null) {
            state.annotations
        } else {
            state.annotations.filterNot { annotation -> deletedBefore(annotation.deletedAt, cutoff) }
        }
        val guides = state.preferences.seenGuideUnits.filter { it.substringBefore("::") in state.books.keys }
        val lookups = LookupHistoryRules.sanitize(state.lookupHistory)
        if (cards.size == state.wordCards.size &&
            annotations.size == state.annotations.size &&
            guides.size == state.preferences.seenGuideUnits.size &&
            lookups == state.lookupHistory
        ) {
            return state
        }
        return state.copy(
            wordCards = cards,
            annotations = annotations,
            preferences = state.preferences.copy(seenGuideUnits = guides),
            lookupHistory = lookups,
        )
    }

    private fun deletedBefore(deletedAt: String?, cutoff: Instant): Boolean {
        val deleted = deletedAt?.let { runCatching { Instant.parse(it) }.getOrNull() }
        return deleted != null && deleted.isBefore(cutoff)
    }

    private fun notifyWrite() {
        writeListeners.forEach { listener -> runCatching(listener) }
    }

    private fun load(): ReadingState {
        val main = decode(file)
        if (main != null) {
            adopt(main)
            val state = compact(main.state)
            // Upgrade v1 in place and apply one-time compaction; a document from
            // a newer build is read but never rewritten automatically, so its
            // unknown fields survive on disk.
            if (main.upgraded || state != main.state) persist(state)
            // Keep one known-good copy per process start, not per page turn.
            runCatching { backupFile?.let { file.copyTo(it, overwrite = true) } }
            return state
        }

        val backup = decode(backupFile)
        if (backup != null) {
            recoveredFromBackup = true
            adopt(backup)
            // Preserve the broken file for diagnosis, then heal the main file so
            // the notice is one-shot and the next launch starts normally.
            runCatching { corruptFile?.let { file.copyTo(it, overwrite = true) } }
            val state = compact(backup.state)
            runCatching { persist(state) }
            return state
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

    /** Remember the raw document and which schema version a later write should use. */
    private fun adopt(decoded: ReadingStateCodec.Decoded) {
        rawDocument = decoded.raw
        documentVersion = if (decoded.fromFuture) decoded.sourceVersion else STATE_SCHEMA_VERSION
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
        writeAtomically(file, ReadingStateCodec.encode(state, rawDocument))
    }

    /** Temp file + atomic rename; the fallback keeps working on filesystems without it. */
    private fun writeAtomically(target: File, text: String) {
        target.parentFile?.mkdirs()
        val temp = File(target.parentFile, "${target.name}.tmp")
        temp.writeText(text, Charsets.UTF_8)
        runCatching {
            Files.move(
                temp.toPath(),
                target.toPath(),
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.ATOMIC_MOVE,
            )
        }.onFailure {
            Files.move(temp.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING)
        }
    }

    /**
     * One queued page turn. `paragraphId` is the visible anchor, `position` the
     * phase/page/answers; saving both keeps a kill from losing either.
     */
    @Serializable
    private data class ReaderJournal(
        val bookId: String,
        val paragraphId: String? = null,
        val position: ReaderPosition,
        val savedAt: String,
    )

    private companion object {
        /** Deleted cards are kept long enough for every device to see the tombstone. */
        const val TOMBSTONE_RETENTION_DAYS = 180L
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

    override fun savePosition(bookId: String, paragraphId: String?, position: ReaderPosition) {
        store.saveReaderPoint(bookId, paragraphId, position)
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

/** Sentence-level marks persisted in [ReadingStateStore], tombstones included. */
class StateAnnotationRepository(private val store: ReadingStateStore) : AnnotationRepository {
    override fun list(bookId: String, includeDeleted: Boolean): List<Annotation> =
        store.state().annotations.filter { it.bookId == bookId && (includeDeleted || !it.isDeleted) }

    override fun all(includeDeleted: Boolean): List<Annotation> =
        store.state().annotations.filter { includeDeleted || !it.isDeleted }

    override fun get(id: String): Annotation? =
        store.state().annotations.firstOrNull { it.id == id }

    override fun upsert(annotation: Annotation): Annotation {
        store.update { state ->
            state.copy(annotations = AnnotationRules.upsert(state.annotations, annotation))
        }
        return get(annotation.id) ?: annotation
    }

    override fun remove(id: String, deletedAt: String): Boolean {
        val existing = get(id) ?: return false
        if (existing.isDeleted) return false
        store.update { state ->
            state.copy(annotations = AnnotationRules.remove(state.annotations, id, deletedAt))
        }
        return true
    }
}

/** Recent word lookups persisted in [ReadingStateStore], newest first. */
class StateLookupHistoryRepository(
    private val store: ReadingStateStore,
    private val now: () -> String = { Instant.now().toString() },
) : LookupHistoryRepository {
    override fun all(): List<LookupEntry> = store.state().lookupHistory

    override fun record(entry: LookupEntry): LookupEntry {
        val stamped = entry.copy(lookedUpAt = entry.lookedUpAt.ifBlank { now() })
        store.update { state ->
            state.copy(lookupHistory = LookupHistoryRules.record(state.lookupHistory, stamped))
        }
        return store.state().lookupHistory.firstOrNull { it.id == stamped.id } ?: stamped
    }

    override fun clear() {
        store.update { state -> state.copy(lookupHistory = LookupHistoryRules.clear()) }
    }
}

/** Preferences persisted in [ReadingStateStore]. */
class StatePreferencesRepository(private val store: ReadingStateStore) {    fun preferences(): Preferences = store.state().preferences

    fun save(preferences: Preferences): Preferences {
        store.update { it.copy(preferences = preferences) }
        return store.state().preferences
    }
}
