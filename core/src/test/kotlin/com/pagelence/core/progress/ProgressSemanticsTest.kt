package com.pagelence.core.progress

import com.pagelence.core.ContentFixtures
import com.pagelence.core.content.Book
import com.pagelence.core.content.Chapter
import com.pagelence.core.content.ContentIndex
import com.pagelence.core.content.LocalizedText
import com.pagelence.core.content.Paragraph
import com.pagelence.core.content.PagelenceJson
import com.pagelence.core.content.StudyUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ProgressSemanticsTest {
    private val NOW = "2026-11-21T10:00:00Z"
    private val book = Book(
        id = "test-book",
        title = LocalizedText(zh = "测试书", en = "Test"),
        chapters = listOf(
            Chapter(
                id = "c1",
                number = 1,
                units = listOf(
                    unit("u1", 1),
                    unit("u2", 2),
                ),
            ),
            Chapter(
                id = "c2",
                number = 2,
                units = listOf(unit("u3", 1)),
            ),
        ),
    )
    private val index = ContentIndex(book)

    private fun unit(id: String, number: Int) = StudyUnit(
        id = id,
        number = number,
        title = LocalizedText(zh = id),
        paragraphs = listOf(Paragraph(id = "$id-p1", text = "One. Two.")),
    )

    @Test
    fun `restore drops unknown ids and duplicates, keeping order`() {
        val restored = ProgressSemantics.restore(
            BookProgress(
                bookId = "test-book",
                paragraphId = "ghost-p",
                readUnitIds = listOf("u2", "ghost", "u2", "u1"),
                lastReadAt = "2026-11-19T01:00:00Z",
            ),
            index,
        )
        assertEquals(listOf("u2", "u1"), restored.readUnitIds)
        assertNull(restored.paragraphId)
        assertEquals("2026-11-19T01:00:00Z", restored.lastReadAt)
    }

    @Test
    fun `restore keeps a valid resume paragraph`() {
        val restored = ProgressSemantics.restore(
            BookProgress(bookId = "test-book", paragraphId = "u3-p1"),
            index,
        )
        assertEquals("u3-p1", restored.paragraphId)
        assertTrue(restored.readUnitIds.isEmpty())
    }

    @Test
    fun `advance marks the unit and moves to the next unit`() {
        val start = BookProgress(bookId = "test-book")
        val after = ProgressSemantics.advance(start, index, "u1", NOW)
        assertEquals(listOf("u1"), after.readUnitIds)
        assertEquals("u2-p1", after.paragraphId)

        val again = ProgressSemantics.advance(after, index, "u1", NOW)
        assertEquals(listOf("u1"), again.readUnitIds, "the unit is only marked once")
    }

    @Test
    fun `advancing the last unit keeps the resume paragraph`() {
        val progress = BookProgress(bookId = "test-book", paragraphId = "u3-p1")
        val after = ProgressSemantics.advance(progress, index, "u3", NOW)
        assertEquals(listOf("u3"), after.readUnitIds)
        assertEquals("u3-p1", after.paragraphId)
    }

    @Test
    fun `advancing an unknown unit is a no-op`() {
        val progress = BookProgress(bookId = "test-book", readUnitIds = listOf("u1"))
        assertEquals(progress, ProgressSemantics.advance(progress, index, "ghost", NOW))
    }

    @Test
    fun `markUnitRead does not move the position`() {
        val progress = BookProgress(bookId = "test-book", paragraphId = "u1-p1")
        val after = ProgressSemantics.markUnitRead(progress, index, "u2", NOW)
        assertEquals(listOf("u2"), after.readUnitIds)
        assertEquals("u1-p1", after.paragraphId)
        assertEquals(after, ProgressSemantics.markUnitRead(after, index, "ghost", NOW))
    }

    @Test
    fun `setPosition ignores paragraphs that are not in the book`() {
        val progress = BookProgress(bookId = "test-book")
        assertEquals("u2-p1", ProgressSemantics.setPosition(progress, index, "u2-p1").paragraphId)
        assertNull(ProgressSemantics.setPosition(progress, index, "ghost-p").paragraphId)
    }

    @Test
    fun `completion means every unit is read`() {
        val partial = BookProgress(bookId = "test-book", readUnitIds = listOf("u1", "u2"))
        assertFalse(ProgressSemantics.isComplete(partial, index))
        val complete = ProgressSemantics.markUnitRead(partial, index, "u3", NOW)
        assertTrue(ProgressSemantics.isComplete(complete, index))
        assertEquals(3, ProgressSemantics.readUnitCount(complete, index))
    }

    @Test
    fun `state document round-trips with schema version device and timestamp`() {
        val state = ReadingState(
            deviceId = "device-1",
            updatedAt = "2026-11-19T01:00:00Z",
            books = mapOf(
                "test-book" to BookProgress(
                    bookId = "test-book",
                    paragraphId = "u2-p1",
                    readUnitIds = listOf("u1"),
                ),
            ),
        )
        val json = PagelenceJson.encodeToString(ReadingState.serializer(), state)
        assertTrue(json.contains("\"schemaVersion\":2"))
        assertTrue(json.contains("\"deviceId\":\"device-1\""))
        assertEquals(state, PagelenceJson.decodeFromString(json))
    }

    @Test
    fun `shipped book exposes a first paragraph`() {
        val repository = com.pagelence.core.content.FileContentRepository(ContentFixtures.contentDir)
        val book = indexOf(repository, ContentFixtures.dolittle)
        assertEquals("chapter-01-p001", book.firstParagraphId)
    }

    @Test
    fun `finishing the last unit stamps the finish date once`() {
        val partial = BookProgress(bookId = "test-book", readUnitIds = listOf("u1", "u2"))
        val finished = ProgressSemantics.markUnitRead(partial, index, "u3", NOW)
        assertEquals(NOW, finished.finishedAt)

        val again = ProgressSemantics.advance(finished, index, "u3", "2026-12-01T00:00:00Z")
        assertEquals(NOW, again.finishedAt, "a re-read never rewrites the finish date")
    }

    @Test
    fun `a passive save leaves the finish date alone`() {
        val partial = BookProgress(bookId = "test-book", readUnitIds = listOf("u1", "u2"))
        val browsed = ProgressSemantics.markUnitRead(partial, index, "u3", now = null)
        assertNull(browsed.finishedAt, "browsing the last grammar page is not finishing the book")

        val existing = partial.copy(finishedAt = NOW, readUnitIds = listOf("u1", "u2", "u3"))
        assertEquals(NOW, ProgressSemantics.advance(existing, index, "u3", now = null).finishedAt)
    }

    @Test
    fun `an incomplete book loses a stale finish date`() {
        val stale = BookProgress(
            bookId = "test-book",
            readUnitIds = listOf("u1", "u2"),
            finishedAt = NOW,
        )
        val advanced = ProgressSemantics.advance(stale, index, "u1", now = null)
        assertNull(advanced.finishedAt)
    }

    @Test
    fun `restore keeps the finish date only while the book is complete`() {
        val complete = ProgressSemantics.restore(
            BookProgress(
                bookId = "test-book",
                readUnitIds = listOf("u1", "u2", "u3"),
                finishedAt = NOW,
            ),
            index,
        )
        assertEquals(NOW, complete.finishedAt)

        val missingUnit = ProgressSemantics.restore(
            BookProgress(
                bookId = "test-book",
                readUnitIds = listOf("u1", "u2"),
                finishedAt = NOW,
            ),
            index,
        )
        assertNull(missingUnit.finishedAt, "removing a unit puts the book back in progress")

        val unknownOnly = ProgressSemantics.restore(
            BookProgress(bookId = "test-book", readUnitIds = listOf("ghost"), finishedAt = NOW),
            index,
        )
        assertNull(unknownOnly.finishedAt)
    }

    @Test
    fun `justFinished only fires on the transition`() {
        val before = BookProgress(bookId = "test-book", readUnitIds = listOf("u1", "u2"))
        val after = ProgressSemantics.markUnitRead(before, index, "u3", NOW)
        assertTrue(ProgressSemantics.justFinished(before, after))
        assertFalse(ProgressSemantics.justFinished(after, after))
        assertFalse(ProgressSemantics.justFinished(after, ProgressSemantics.advance(after, index, "u3", NOW)))
    }

    private fun indexOf(
        repository: com.pagelence.core.content.FileContentRepository,
        bookId: String,
    ): ContentIndex = requireNotNull(repository.index(bookId))
}
