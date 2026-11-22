package com.pagelence.core.progress

import com.pagelence.core.content.Book
import com.pagelence.core.content.Chapter
import com.pagelence.core.content.ContentIndex
import com.pagelence.core.content.LocalizedText
import com.pagelence.core.content.Paragraph
import com.pagelence.core.content.Sentence
import com.pagelence.core.content.StudyUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * T144: annotation identity, tombstones, sync merge and anchor resolution. The
 * rules live in `:core` so import/sync and both shells share one behaviour.
 */
class AnnotationsTest {
    private val now = "2026-11-22T10:00:00Z"
    private val earlier = "2026-11-21T10:00:00Z"

    private val index = ContentIndex(
        Book(
            id = "book",
            title = LocalizedText(zh = "书", en = "Book"),
            chapters = listOf(
                Chapter(
                    id = "c1",
                    number = 1,
                    title = LocalizedText(zh = "第一章", en = "Chapter 1"),
                    units = listOf(
                        StudyUnit(
                            id = "u1",
                            number = 1,
                            title = LocalizedText(zh = "第一段", en = "Unit 1"),
                            paragraphs = listOf(
                                Paragraph(
                                    id = "u1-p1",
                                    text = "One. Two.",
                                    sentences = listOf(Sentence("One."), Sentence("Two.")),
                                ),
                            ),
                        ),
                    ),
                ),
            ),
        ),
    )

    private fun annotation(
        kind: AnnotationKind = AnnotationKind.BOOKMARK,
        bookId: String = "book",
        paragraphId: String = "u1-p1",
        sentence: Int = 1,
        updatedAt: String = now,
        deletedAt: String? = null,
        note: String? = null,
        quote: String = "One.",
    ) = Annotation(
        id = AnnotationRules.idOf(bookId, paragraphId, sentence, kind),
        bookId = bookId,
        unitId = "u1",
        paragraphId = paragraphId,
        sentenceNumber = sentence,
        kind = kind,
        note = note,
        quote = quote,
        createdAt = earlier,
        updatedAt = updatedAt,
        deletedAt = deletedAt,
    )

    @Test
    fun `id keeps one record per sentence and kind`() {
        val bookmark = annotation(kind = AnnotationKind.BOOKMARK)
        val highlight = annotation(kind = AnnotationKind.HIGHLIGHT)
        val note = annotation(kind = AnnotationKind.NOTE, note = "注意时态")

        assertEquals("book::u1-p1::1::bookmark", bookmark.id)
        assertEquals(3, setOf(bookmark.id, highlight.id, note.id).size)
        assertEquals(bookmark.id, AnnotationRules.idOf("book", "u1-p1", 1, AnnotationKind.BOOKMARK))
    }

    @Test
    fun `remove writes a tombstone and upsert resurrects the same id`() {
        val live = annotation()
        val removed = AnnotationRules.remove(listOf(live), live.id, now)

        assertEquals(1, removed.size)
        assertTrue(removed.single().isDeleted)
        assertEquals(live.createdAt, removed.single().createdAt)
        assertEquals(now, removed.single().updatedAt)
        assertTrue(AnnotationRules.live(removed).isEmpty())

        val restored = AnnotationRules.create(
            bookId = "book",
            unitId = "u1",
            paragraphId = "u1-p1",
            sentenceNumber = 1,
            kind = AnnotationKind.BOOKMARK,
            quote = "One.",
            now = "2026-11-22T11:00:00Z",
            previous = removed.single(),
        )
        assertNull(restored.deletedAt)
        assertEquals(earlier, restored.createdAt)
        assertEquals(restored.id, live.id)
    }

    @Test
    fun `merge keeps tombstones and resolves live copies by updatedAt`() {
        val localDeleted = annotation(deletedAt = "2026-11-22T09:00:00Z", updatedAt = now)
        val remoteLive = annotation(updatedAt = "2026-11-22T11:00:00Z")
        val merged = AnnotationRules.merge(listOf(localDeleted), listOf(remoteLive))
        assertTrue(merged.single().isDeleted)

        val localNote = annotation(kind = AnnotationKind.NOTE, note = "旧", updatedAt = earlier)
        val remoteNote = annotation(kind = AnnotationKind.NOTE, note = "新", updatedAt = now)
        val notes = AnnotationRules.merge(listOf(localNote), listOf(remoteNote))
        assertEquals("新", notes.single().note)
        assertEquals(1, notes.size)
    }

    @Test
    fun `anchor resolves the current sentence and falls back to the quote`() {
        val valid = AnnotationRules.anchor(index, annotation(sentence = 2, quote = "Two."))
        assertTrue(valid.valid)
        assertEquals("Two.", valid.quote)

        val moved = AnnotationRules.anchor(
            index,
            annotation(paragraphId = "gone-p", quote = "旧句子"),
        )
        assertFalse(moved.valid)
        assertEquals("旧句子", moved.quote)

        val noQuote = AnnotationRules.anchor(index, annotation(paragraphId = "gone-p", quote = ""))
        assertFalse(noQuote.valid)
        assertEquals("", noQuote.quote)
    }

    @Test
    fun `sanitize drops unreadable rows and collapses duplicate ids`() {
        val good = annotation(updatedAt = earlier)
        val newer = good.copy(note = null, updatedAt = now)
        val invalid = good.copy(id = " ", paragraphId = "")
        val rows = AnnotationRules.sanitize(listOf(good, invalid, newer))

        assertEquals(1, rows.size)
        assertEquals(now, rows.single().updatedAt)
    }

    @Test
    fun `in-memory repository hides tombstones by default`() {
        val repository = InMemoryAnnotationRepository()
        val bookmark = annotation()
        repository.upsert(bookmark)
        assertEquals(1, repository.list("book").size)

        assertTrue(repository.remove(bookmark.id, now))
        assertFalse(repository.remove(bookmark.id, now))
        assertTrue(repository.list("book").isEmpty())
        assertEquals(1, repository.list("book", includeDeleted = true).size)
        assertNotNull(repository.get(bookmark.id)?.deletedAt)
    }
}
