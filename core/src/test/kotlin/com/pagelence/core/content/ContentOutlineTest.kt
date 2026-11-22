package com.pagelence.core.content

import com.pagelence.core.ContentFixtures
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** `outline.json` must stay a faithful, cheaper copy of the parts the shelf needs. */
class ContentOutlineTest {
    private val repository = FileContentRepository(ContentFixtures.contentDir)

    /** Fails the test's premise if the shelf path ever parses a book body. */
    private class CountingSource(private val inner: ContentSource) : ContentSource {
        var bookReads = 0
            private set

        override fun exists(path: String): Boolean = inner.exists(path)

        override fun readText(path: String): String {
            if (path.endsWith("/book.json")) bookReads += 1
            return inner.readText(path)
        }

        override fun filePath(path: String): String? = inner.filePath(path)
    }

    @Test
    fun `shelf metadata never parses a book body`() {
        val source = CountingSource(FileContentSource(ContentFixtures.contentDir))
        val repository = FileContentRepository(source)
        val manifest = FileContentRepository.loadManifest(source)

        repository.library()
        manifest.books.forEach { summary ->
            repository.outline(summary.id)
            repository.coverPath(summary.id)
        }

        assertEquals(0, source.bookReads)
    }

    @Test
    fun `outline exists for every shipped book and lists its units`() {
        val manifest = FileContentRepository.loadManifest(ContentFixtures.contentDir)

        for (summary in manifest.books) {
            val outline = assertNotNull(repository.outline(summary.id), "outline for ${summary.id}")
            assertEquals(summary.id, outline.bookId)
            assertEquals(summary.units, outline.units.size)
            assertTrue(outline.units.all { it.paragraphIds.isNotEmpty() })
        }
    }

    @Test
    fun `outline maps a paragraph back to its unit`() {
        val outline = assertNotNull(repository.outline(ContentFixtures.dolittle))
        val unit = outline.units.first()

        assertEquals(unit, outline.unitOfParagraph(unit.paragraphIds.first()))
        assertNull(outline.unitOfParagraph("not-a-paragraph"))
    }

    @Test
    fun `outline matches the parsed book`() {
        val index = assertNotNull(repository.index(ContentFixtures.alice))
        val outline = assertNotNull(repository.outline(ContentFixtures.alice))

        // Generated from the same book by the content scripts; a mismatch means
        // someone edited one side by hand.
        assertEquals(ContentOutline.from(index), outline)
    }
}
