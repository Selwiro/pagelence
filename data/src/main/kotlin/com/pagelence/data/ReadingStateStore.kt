package com.pagelence.data

import com.pagelence.core.prefs.Preferences
import com.pagelence.core.progress.BookProgress
import com.pagelence.core.progress.ProgressRepository
import com.pagelence.core.progress.ReadingState
import com.pagelence.core.progress.ReadingStateCodec
import com.pagelence.core.progress.STATE_SCHEMA_VERSION
import com.pagelence.core.wordcards.WordCard
import com.pagelence.core.wordcards.WordCardRepository
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.time.Instant
import java.util.UUID

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
) {
    private val lock = Any()
    private var cached: ReadingState = load()

    init {
        if (cached.deviceId.isBlank()) {
            cached = cached.copy(deviceId = newDeviceId(), updatedAt = now())
            persist(cached)
        }
    }

    fun state(): ReadingState = synchronized(lock) { cached }

    /** The current document, exactly as export/WebDAV would upload it. */
    fun snapshotJson(): String = synchronized(lock) { ReadingStateCodec.encode(cached) }

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
        next
    }

    private fun load(): ReadingState {
        if (!file.isFile) return emptyState()
        val decoded = runCatching {
            ReadingStateCodec.decode(file.readText(Charsets.UTF_8))
        }.getOrNull() ?: return emptyState()
        // Upgrade v1 in place; a document from a newer build is read but never
        // rewritten automatically, so unknown fields survive on disk.
        if (decoded.upgraded) persist(decoded.state)
        return decoded.state
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
