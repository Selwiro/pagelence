package com.pagelence.presentation.annotations

import com.pagelence.core.content.FileContentRepository
import com.pagelence.core.progress.Annotation
import com.pagelence.core.progress.AnnotationKind
import com.pagelence.core.progress.AnnotationRules
import com.pagelence.uim3.UiLanguage
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class AnnotationPresenterTest {
    private val contentDir: Path = Path.of(
        System.getProperty("pagelence.contentDir") ?: "../content",
    ).toAbsolutePath().normalize()

    private val bookId = "doctor-dolittle-1920"
    private val content = FileContentRepository(contentDir)
    private val index = assertNotNull(content.index(bookId))
    private val unit = index.units.first()
    private val paragraph = unit.paragraphs.first()

    private fun annotation(
        kind: AnnotationKind,
        sentence: Int = 1,
        paragraphId: String = paragraph.id,
        note: String? = null,
        quote: String = "quote",
        updatedAt: String = "2026-11-22T10:00:00Z",
    ) = Annotation(
        id = AnnotationRules.idOf(bookId, paragraphId, sentence, kind),
        bookId = bookId,
        unitId = unit.id,
        paragraphId = paragraphId,
        sentenceNumber = sentence,
        kind = kind,
        note = note,
        quote = quote,
        createdAt = "2026-11-22T09:00:00Z",
        updatedAt = updatedAt,
    )

    private fun presenter() = AnnotationPresenter(content, UiLanguage.ZH)

    @Test
    fun `marks merge the three kinds per sentence`() {
        val annotations = listOf(
            annotation(AnnotationKind.BOOKMARK),
            annotation(AnnotationKind.HIGHLIGHT),
            annotation(AnnotationKind.NOTE, note = "  注意这里  "),
        )
        val marks = presenter().marks(annotations, bookId, unit.id)
        val mark = assertNotNull(marks[AnnotationRules.sentenceKey(paragraph.id, 1)])
        assertTrue(mark.bookmarked)
        assertTrue(mark.highlighted)
        assertEquals("注意这里", mark.note)
    }

    @Test
    fun `book rows use the current text and keep the order`() {
        val rows = presenter().book(
            listOf(
                annotation(AnnotationKind.NOTE, sentence = 2, note = "第二句"),
                annotation(AnnotationKind.BOOKMARK, sentence = 1),
            ),
            bookId,
        )
        assertEquals(2, rows.size)
        assertEquals(1, rows.first().sentenceNumber)
        assertEquals(unit.id, rows.first().unitId)
        assertTrue(rows.first().anchorValid)
        assertTrue(rows.first().unitNumber > 0)
        assertTrue(rows.first().bookTitle.isNotBlank())
    }

    @Test
    fun `an invalid anchor falls back to the stored quote`() {
        val rows = presenter().book(
            listOf(annotation(AnnotationKind.NOTE, paragraphId = "gone-p", quote = "旧句子", note = "留着")),
            bookId,
        )
        assertEquals(1, rows.size)
        assertFalse(rows.single().anchorValid)
        assertEquals("旧句子", rows.single().quote)
        assertEquals("留着", rows.single().note)
    }

    @Test
    fun `all groups books and hides tombstones`() {
        val live = annotation(AnnotationKind.BOOKMARK)
        val deleted = annotation(AnnotationKind.NOTE, sentence = 2).copy(
            deletedAt = "2026-11-22T11:00:00Z",
            updatedAt = "2026-11-22T11:00:00Z",
        )
        val rows = presenter().all(listOf(live, deleted))
        assertEquals(1, rows.size)
        assertEquals(bookId, rows.single().bookId)
    }
}
