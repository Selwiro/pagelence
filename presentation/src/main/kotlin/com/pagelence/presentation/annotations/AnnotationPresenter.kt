package com.pagelence.presentation.annotations

import com.pagelence.core.content.ContentIndex
import com.pagelence.core.content.ContentRepository
import com.pagelence.core.content.LocalizedText
import com.pagelence.core.progress.Annotation
import com.pagelence.core.progress.AnnotationRules
import com.pagelence.uim3.UiLanguage
import com.pagelence.uim3.model.AnnotationUi
import com.pagelence.uim3.model.SentenceMarkUi
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Android-free mapping from stored [Annotation] rows to list rows and sentence
 * marks. Titles, unit positions and quote fallbacks come from the installed
 * content; a book that is no longer installed still yields rows (title falls
 * back to its id) so a reader never loses sight of an old note.
 */
class AnnotationPresenter(
    private val content: ContentRepository,
    private val language: UiLanguage,
) {
    /** Marks of one book, for the reader's per-sentence indicators. */
    fun marks(annotations: List<Annotation>, bookId: String, unitId: String): Map<String, SentenceMarkUi> {
        val bySentence = LinkedHashMap<String, SentenceMarkUi>()
        AnnotationRules.live(annotations, bookId = bookId, unitId = unitId).forEach { annotation ->
            val key = AnnotationRules.sentenceKey(annotation.paragraphId, annotation.sentenceNumber)
            val current = bySentence[key] ?: SentenceMarkUi()
            bySentence[key] = when (annotation.kind) {
                com.pagelence.core.progress.AnnotationKind.BOOKMARK -> current.copy(bookmarked = true)
                com.pagelence.core.progress.AnnotationKind.HIGHLIGHT -> current.copy(highlighted = true)
                com.pagelence.core.progress.AnnotationKind.NOTE ->
                    current.copy(note = AnnotationRules.normalizeNote(annotation.note))
            }
        }
        return bySentence
    }

    /** Live annotations of [bookId], ordered by chapter/unit then sentence. */
    fun book(annotations: List<Annotation>, bookId: String): List<AnnotationUi> {
        val index = runCatching { content.index(bookId) }.getOrNull()
        val stored = AnnotationRules.live(annotations, bookId = bookId)
        return if (index == null) {
            stored.sortedByDescending { it.updatedAt }.map { fallbackRow(it) }
        } else {
            val unitPositions = index.units.mapIndexed { position, unit -> unit.id to position + 1 }.toMap()
            stored
                .map { row(it, index, unitPositions[it.unitId] ?: 0) }
                .sortedWith(
                    compareBy<AnnotationUi> { it.unitNumber.takeIf { number -> number > 0 } ?: Int.MAX_VALUE }
                        .thenBy { it.sentenceNumber }
                        .thenBy { it.kind.ordinal },
                )
        }
    }

    /** Every live annotation across books, recent edits first (review tab). */
    fun all(annotations: List<Annotation>): List<AnnotationUi> {
        val live = AnnotationRules.live(annotations)
        return live.groupBy { it.bookId }
            .entries
            .sortedByDescending { (_, rows) -> rows.maxOfOrNull { it.updatedAt } ?: "" }
            .flatMap { (bookId, rows) -> book(rows, bookId) }
    }

    private fun row(annotation: Annotation, index: ContentIndex, unitPosition: Int): AnnotationUi {
        val anchor = AnnotationRules.anchor(index, annotation)
        val unit = index.unit(annotation.unitId)
        val chapter = index.chapterByUnitId[annotation.unitId]
        return AnnotationUi(
            id = annotation.id,
            bookId = annotation.bookId,
            bookTitle = title(index.book.title) ?: annotation.bookId,
            unitId = annotation.unitId,
            unitTitle = unit?.title?.let(::title) ?: unit?.number?.toString().orEmpty(),
            chapterTitle = chapter?.title?.let(::title).orEmpty(),
            unitNumber = unitPosition,
            paragraphId = annotation.paragraphId,
            sentenceNumber = annotation.sentenceNumber,
            kind = annotation.kind,
            note = AnnotationRules.normalizeNote(annotation.note),
            quote = anchor.quote,
            createdAt = annotation.createdAt,
            updatedAt = annotation.updatedAt,
            dateLabel = displayDate(annotation.updatedAt),
            anchorValid = anchor.valid,
        )
    }

    /**
     * A book that is not installed has no index, so titles, chapter and unit
     * numbers are unavailable. Keep the row usable with the stored quote.
     */
    private fun fallbackRow(annotation: Annotation): AnnotationUi = AnnotationUi(
        id = annotation.id,
        bookId = annotation.bookId,
        bookTitle = content.library().firstOrNull { it.id == annotation.bookId }?.title?.let(::title)
            ?: annotation.bookId,
        unitId = annotation.unitId,
        unitTitle = annotation.unitId,
        chapterTitle = "",
        unitNumber = 0,
        paragraphId = annotation.paragraphId,
        sentenceNumber = annotation.sentenceNumber,
        kind = annotation.kind,
        note = AnnotationRules.normalizeNote(annotation.note),
        quote = annotation.quote,
        createdAt = annotation.createdAt,
        updatedAt = annotation.updatedAt,
        dateLabel = displayDate(annotation.updatedAt),
        anchorValid = false,
    )

    private fun title(text: LocalizedText): String? = when (language) {
        UiLanguage.ZH -> text.zh ?: text.en
        UiLanguage.EN -> text.en ?: text.zh
    }?.takeIf { it.isNotBlank() }

    private fun displayDate(value: String?): String? {
        if (value.isNullOrBlank()) return null
        val instant = runCatching { Instant.parse(value) }.getOrNull() ?: return null
        return DateTimeFormatter.ofPattern("yyyy-MM-dd")
            .withZone(ZoneId.systemDefault())
            .format(instant)
    }
}
