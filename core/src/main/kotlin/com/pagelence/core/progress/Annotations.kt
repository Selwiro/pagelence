package com.pagelence.core.progress

import com.pagelence.core.content.ContentIndex
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A reader's mark on one sentence. The first release is deliberately
 * sentence-level: no arbitrary character ranges, no drawing, no rich text.
 *
 * All three kinds share one record shape so the list, the sync merge and the
 * tombstone rules only have to be written once. `kind` says how the reader sees
 * it: a bookmark in the margin, a highlighted sentence, or a note attached to
 * the sentence.
 */
@Serializable
enum class AnnotationKind(val id: String) {
    @SerialName("bookmark") BOOKMARK("bookmark"),
    @SerialName("highlight") HIGHLIGHT("highlight"),
    @SerialName("note") NOTE("note"),
    ;

    companion object {
        /** Lenient lookup used when an imported document carries an unknown kind. */
        fun fromId(value: String): AnnotationKind? = entries.firstOrNull { it.id == value }
    }
}

/**
 * One sentence-level mark stored in `state.json`.
 *
 * [quote] is a snapshot of the sentence text. When the book content changes and
 * [paragraphId] / [sentenceNumber] no longer resolve, the list still shows the
 * snapshot instead of dropping the reader's note.
 */
@Serializable
data class Annotation(
    val id: String,
    val bookId: String,
    val unitId: String,
    val paragraphId: String,
    /** 1-based sentence number inside the unit, same numbering as the reader. */
    val sentenceNumber: Int,
    val kind: AnnotationKind,
    val note: String? = null,
    val quote: String = "",
    val createdAt: String,
    val updatedAt: String,
    val deletedAt: String? = null,
) {
    val isDeleted: Boolean get() = deletedAt != null
}

/** Where a stored annotation currently resolves in the book, quote included. */
data class AnnotationAnchor(
    /** False when the unit/paragraph/sentence is no longer in the book. */
    val valid: Boolean,
    val unitId: String,
    val paragraphId: String,
    val sentenceNumber: Int,
    val quote: String,
)

/**
 * Rules for annotation identity, toggling, tombstones and sync merge. They live
 * in `:core` so the phone shell, the e-ink shell and the import path agree.
 */
object AnnotationRules {
    /**
     * Stable identity: one mark of each kind per sentence. Two devices editing
     * the same sentence therefore collide on the same id and merge by time
     * instead of producing duplicates.
     */
    fun idOf(
        bookId: String,
        paragraphId: String,
        sentenceNumber: Int,
        kind: AnnotationKind,
    ): String = "$bookId::$paragraphId::$sentenceNumber::${kind.id}"

    fun sentenceKey(paragraphId: String, sentenceNumber: Int): String =
        "$paragraphId::$sentenceNumber"

    /** Notes are stored trimmed; an all-blank note is the same as no note. */
    fun normalizeNote(note: String?): String? = note?.trim()?.takeIf { it.isNotEmpty() }

    /** Build a fresh (or resurrected) annotation for [kind] on one sentence. */
    fun create(
        bookId: String,
        unitId: String,
        paragraphId: String,
        sentenceNumber: Int,
        kind: AnnotationKind,
        quote: String,
        now: String,
        note: String? = null,
        previous: Annotation? = null,
    ): Annotation = Annotation(
        id = idOf(bookId, paragraphId, sentenceNumber, kind),
        bookId = bookId,
        unitId = unitId,
        paragraphId = paragraphId,
        sentenceNumber = sentenceNumber,
        kind = kind,
        note = if (kind == AnnotationKind.NOTE) normalizeNote(note) else null,
        quote = quote,
        createdAt = previous?.createdAt ?: now,
        updatedAt = now,
        deletedAt = null,
    )

    /** Replace the record with the same id, or append a new one. */
    fun upsert(annotations: List<Annotation>, annotation: Annotation): List<Annotation> {
        val position = annotations.indexOfFirst { it.id == annotation.id }
        if (position < 0) return annotations + annotation
        return annotations.toMutableList().also { it[position] = annotation }
    }

    /** Write a tombstone; false when the id is unknown or already deleted. */
    fun remove(annotations: List<Annotation>, id: String, now: String): List<Annotation> {
        val position = annotations.indexOfFirst { it.id == id && !it.isDeleted }
        if (position < 0) return annotations
        return annotations.toMutableList().also {
            it[position] = it[position].copy(deletedAt = now, updatedAt = now)
        }
    }

    /** Live (non-tombstone) marks, optionally for one book or unit. */
    fun live(
        annotations: List<Annotation>,
        bookId: String? = null,
        unitId: String? = null,
    ): List<Annotation> = annotations.filter {
        !it.isDeleted &&
            (bookId == null || it.bookId == bookId) &&
            (unitId == null || it.unitId == unitId)
    }

    fun forSentence(
        annotations: List<Annotation>,
        bookId: String,
        paragraphId: String,
        sentenceNumber: Int,
    ): List<Annotation> = annotations.filter {
        !it.isDeleted &&
            it.bookId == bookId &&
            it.paragraphId == paragraphId &&
            it.sentenceNumber == sentenceNumber
    }

    /**
     * Union by id. A tombstone beats a live copy on the other side because the
     * reader already acted on the deletion; two live copies resolve by the
     * later `updatedAt`, ties keep the local one like the word-card merge.
     */
    fun merge(local: List<Annotation>, remote: List<Annotation>): List<Annotation> {
        val localById = local.associateBy { it.id }
        val remoteById = remote.associateBy { it.id }
        val ids = LinkedHashSet<String>(local.size + remote.size)
        ids += local.map { it.id }
        ids += remote.map { it.id }
        return ids.mapNotNull { id ->
            val mine = localById[id]
            val theirs = remoteById[id]
            when {
                mine == null -> theirs
                theirs == null -> mine
                mine.isDeleted -> if (theirs.isDeleted) later(mine, theirs) else mine
                theirs.isDeleted -> theirs
                else -> later(mine, theirs)
            }
        }
    }

    private fun later(local: Annotation, remote: Annotation): Annotation {
        val localAt = StateMerge.instant(local.updatedAt)
        val remoteAt = StateMerge.instant(remote.updatedAt)
        return if (remoteAt != null && (localAt == null || remoteAt.isAfter(localAt))) remote else local
    }

    /**
     * Drop unreadable records and collapse duplicate ids (last wins) before the
     * document is used. A record whose sentence number is not positive cannot
     * be rendered, so it is dropped rather than kept as a permanent zombie.
     */
    fun sanitize(annotations: List<Annotation>): List<Annotation> {
        val seen = LinkedHashMap<String, Annotation>(annotations.size)
        annotations.forEach { annotation ->
            if (annotation.id.isBlank() ||
                annotation.bookId.isBlank() ||
                annotation.paragraphId.isBlank() ||
                annotation.sentenceNumber < 1
            ) {
                return@forEach
            }
            val normalized = annotation.copy(
                note = if (annotation.kind == AnnotationKind.NOTE) normalizeNote(annotation.note) else null,
                quote = annotation.quote.trim(),
            )
            val previous = seen[normalized.id]
            seen[normalized.id] = if (previous == null) normalized else later(previous, normalized)
        }
        return seen.values.toList()
    }

    /**
     * Resolve an annotation against the current book. An invalid anchor keeps
     * the stored ids and falls back to the quote snapshot, so the list can show
     * "内容已更新" instead of crashing or hiding the note.
     */
    fun anchor(index: ContentIndex, annotation: Annotation): AnnotationAnchor {
        val unit = index.unit(annotation.unitId)
        val paragraph = index.paragraphById[annotation.paragraphId]
        val inUnit = paragraph != null && unit?.paragraphs?.any { it.id == annotation.paragraphId } == true
        val sentence = if (inUnit) {
            sentenceText(index, annotation.unitId, annotation.sentenceNumber)
        } else {
            null
        }
        val valid = inUnit && sentence != null
        return AnnotationAnchor(
            valid = valid,
            unitId = annotation.unitId,
            paragraphId = annotation.paragraphId,
            sentenceNumber = annotation.sentenceNumber,
            quote = sentence?.takeIf { it.isNotBlank() }
                ?: annotation.quote.takeIf { it.isNotBlank() }
                ?: "",
        )
    }

    /** The sentence text at a unit-wide 1-based sentence number. */
    fun sentenceText(index: ContentIndex, unitId: String, sentenceNumber: Int): String? {
        val unit = index.unit(unitId) ?: return null
        var position = 0
        for (paragraph in unit.paragraphs) {
            for (sentence in index.sentences(paragraph)) {
                position += 1
                if (position == sentenceNumber) return sentence.text
            }
        }
        return null
    }
}

/**
 * Storage contract for annotations. `:data` implements it over the same
 * `state.json`; the app only reads and writes through this interface.
 */
interface AnnotationRepository {
    fun list(bookId: String, includeDeleted: Boolean = false): List<Annotation>

    fun all(includeDeleted: Boolean = false): List<Annotation>

    fun get(id: String): Annotation?

    /** Insert or replace one record (re-adding a deleted sentence clears the tombstone). */
    fun upsert(annotation: Annotation): Annotation

    /** Write a tombstone; false when the id is unknown or already deleted. */
    fun remove(id: String, deletedAt: String): Boolean
}

class InMemoryAnnotationRepository(
    initial: List<Annotation> = emptyList(),
) : AnnotationRepository {
    private val annotations = initial.toMutableList()

    override fun list(bookId: String, includeDeleted: Boolean): List<Annotation> =
        annotations.filter { it.bookId == bookId && (includeDeleted || !it.isDeleted) }

    override fun all(includeDeleted: Boolean): List<Annotation> =
        annotations.filter { includeDeleted || !it.isDeleted }

    override fun get(id: String): Annotation? = annotations.firstOrNull { it.id == id }

    override fun upsert(annotation: Annotation): Annotation {
        val position = annotations.indexOfFirst { it.id == annotation.id }
        if (position < 0) annotations += annotation else annotations[position] = annotation
        return annotation
    }

    override fun remove(id: String, deletedAt: String): Boolean {
        val position = annotations.indexOfFirst { it.id == id && !it.isDeleted }
        if (position < 0) return false
        annotations[position] = annotations[position].copy(deletedAt = deletedAt, updatedAt = deletedAt)
        return true
    }
}
