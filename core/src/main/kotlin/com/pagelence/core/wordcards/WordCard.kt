package com.pagelence.core.wordcards

import com.pagelence.core.content.Hint
import com.pagelence.core.content.HintExample
import com.pagelence.core.content.Paragraph
import com.pagelence.core.content.StudyUnit
import kotlinx.serialization.Serializable

/**
 * One collected word card, frozen at version 1.
 *
 * The id is `bookId::paragraphId::hintId`, which is stable inside this app and
 * survives unit renumbering. Removing a card writes a tombstone ([deletedAt])
 * instead of dropping the row, so a later sync cannot resurrect it.
 */
@Serializable
data class WordCard(
    val id: String,
    val bookId: String,
    val paragraphId: String,
    val hintId: String,
    val unitId: String? = null,
    val term: String,
    val lemma: String,
    val pos: String? = null,
    val zh: String? = null,
    val en: String? = null,
    val ipa: String? = null,
    val example: HintExample? = null,
    val createdAt: String,
    val updatedAt: String,
    val deletedAt: String? = null,
    /**
     * Spaced-repetition schedule (T143). Optional on v2, so cards written before
     * it decode as new and immediately due: `reviews == 0`, no `dueAt`.
     * `dueAt` is a local calendar date (`YYYY-MM-DD`); [lastReviewedAt] stays an
     * ISO-8601 UTC instant like the other timestamps. Rules in [Srs].
     */
    val dueAt: String? = null,
    val intervalDays: Int = 0,
    val ease: Double = Srs.DEFAULT_EASE,
    val reviews: Int = 0,
    val lapses: Int = 0,
    val lastReviewedAt: String? = null,
    val lastGrade: ReviewGrade? = null,
) {
    val isDeleted: Boolean get() = deletedAt != null

    companion object {
        fun idOf(bookId: String, paragraphId: String, hintId: String): String =
            "$bookId::$paragraphId::$hintId"

        /** Case/possessive-insensitive key used to keep one card per base word. */
        fun lemmaKey(lemma: String): String {
            val value = lemma.trim().lowercase().replace('’', '\'')
            return if (value.length > 2 && value.endsWith("'s")) value.dropLast(2) else value
        }

        /** Build a card from the hint that was tapped. */
        fun from(
            bookId: String,
            unit: StudyUnit,
            paragraph: Paragraph,
            hint: Hint,
            now: String,
        ): WordCard = WordCard(
            id = idOf(bookId, paragraph.id, hint.id),
            bookId = bookId,
            paragraphId = paragraph.id,
            hintId = hint.id,
            unitId = unit.id,
            term = hint.target,
            lemma = hint.lemma ?: hint.target.lowercase(),
            pos = hint.pos,
            zh = hint.zh,
            en = hint.en,
            ipa = hint.ipa,
            example = hint.example,
            createdAt = now,
            updatedAt = now,
        )
    }
}

/** Storage contract for cards; Room implements this in the app modules. */
interface WordCardRepository {
    /** Live cards of one book unless [includeDeleted]. */
    fun list(bookId: String, includeDeleted: Boolean = false): List<WordCard>

    /** Live cards across all books, newest last. */
    fun all(includeDeleted: Boolean = false): List<WordCard>

    fun get(cardId: String): WordCard?

    /**
     * The live card for a base word, matched by lemma first and by the surface
     * form second (cards collected before lemma resolution stored the surface).
     */
    fun find(bookId: String, lemma: String, term: String): WordCard?

    /** Insert or replace a card; keeps [WordCard.createdAt] of an existing row. */
    fun add(card: WordCard): WordCard

    /** Tombstone a card. Returns false when the id is unknown. */
    fun remove(cardId: String, deletedAt: String): Boolean

    fun isCollected(bookId: String, paragraphId: String, hintId: String): Boolean
}

/** In-memory implementation used by `core` tests and previews. */
class InMemoryWordCardRepository : WordCardRepository {
    private val cards = LinkedHashMap<String, WordCard>()

    override fun list(bookId: String, includeDeleted: Boolean): List<WordCard> =
        cards.values.filter { it.bookId == bookId && (includeDeleted || !it.isDeleted) }

    override fun all(includeDeleted: Boolean): List<WordCard> =
        cards.values.filter { includeDeleted || !it.isDeleted }

    override fun get(cardId: String): WordCard? = cards[cardId]

    override fun find(bookId: String, lemma: String, term: String): WordCard? {
        val wantedLemma = WordCard.lemmaKey(lemma)
        val wantedTerm = WordCard.lemmaKey(term)
        return cards.values.firstOrNull { card ->
            card.bookId == bookId && !card.isDeleted &&
                (WordCard.lemmaKey(card.lemma) == wantedLemma ||
                    WordCard.lemmaKey(card.term) == wantedTerm)
        }
    }

    override fun add(card: WordCard): WordCard {
        val merged = find(card.bookId, card.lemma, card.term)
        if (merged != null && merged.id != card.id) return merged
        val existing = cards[card.id]
        val stored = if (existing == null) card else card.copy(createdAt = existing.createdAt)
        cards[card.id] = stored
        return stored
    }

    override fun remove(cardId: String, deletedAt: String): Boolean {
        val existing = cards[cardId] ?: return false
        cards[cardId] = existing.copy(deletedAt = deletedAt, updatedAt = deletedAt)
        return true
    }

    override fun isCollected(bookId: String, paragraphId: String, hintId: String): Boolean =
        cards[WordCard.idOf(bookId, paragraphId, hintId)]?.isDeleted == false
}
