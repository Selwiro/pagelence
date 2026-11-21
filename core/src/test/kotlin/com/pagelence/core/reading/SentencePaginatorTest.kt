package com.pagelence.core.reading

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SentencePaginatorTest {
    private fun sentence(
        paragraph: String,
        index: Int,
        height: Int,
        leading: Int = 0,
    ) = MeasuredSentence(paragraph, index, height, leading)

    @Test
    fun `every sentence appears once in order`() {
        val source = listOf(
            sentence("p1", 0, 100, 40),
            sentence("p1", 1, 100),
            sentence("p2", 0, 120, 40),
            sentence("p2", 1, 80),
        )
        val pages = SentencePaginator.paginate(source, pageHeightPx = 350)
        val flattened = pages.flatMap { page ->
            page.chunks.flatMap { chunk ->
                (chunk.sentenceStart until chunk.sentenceEndExclusive).map { chunk.paragraphId to it }
            }
        }
        assertEquals(listOf("p1" to 0, "p1" to 1, "p2" to 0, "p2" to 1), flattened)
    }

    @Test
    fun `paragraphs may continue across pages`() {
        val source = listOf(
            sentence("p1", 0, 200, 30),
            sentence("p1", 1, 200),
            sentence("p1", 2, 200),
        )
        val pages = SentencePaginator.paginate(source, pageHeightPx = 300)
        assertEquals(3, pages.size)
        assertEquals(listOf(0, 1), pages[0].chunks.single().let { listOf(it.sentenceStart, it.sentenceEndExclusive) })
    }

    @Test
    fun `pages fill up to the viewport`() {
        val source = listOf(
            sentence("p1", 0, 100, 20),
            sentence("p1", 1, 100),
            sentence("p1", 2, 100),
            sentence("p1", 3, 100),
        )
        val pages = SentencePaginator.paginate(source, pageHeightPx = 320)
        assertEquals(2, pages.size)
        assertEquals(3, pages[0].chunks.single().sentenceEndExclusive)
    }

    @Test
    fun `a sentence taller than the viewport gets its own scrollable page`() {
        val pages = SentencePaginator.paginate(
            listOf(sentence("p1", 0, 500, 20), sentence("p1", 1, 100)),
            pageHeightPx = 300,
        )
        assertEquals(2, pages.size)
        assertTrue(pages[0].scrollable)
        assertEquals(1, pages[0].chunks.single().sentenceEndExclusive)
    }

    @Test
    fun `chunks merge within the same paragraph and split across pages`() {
        val source = listOf(
            sentence("p1", 0, 100, 20),
            sentence("p1", 1, 100),
            sentence("p1", 2, 100),
        )
        val pages = SentencePaginator.paginate(source, pageHeightPx = 250)
        assertEquals(1, pages[0].chunks.size)
        assertEquals(2, pages[0].chunks.single().sentenceEndExclusive)
    }

    @Test
    fun `empty input produces no pages`() {
        assertTrue(SentencePaginator.paginate(emptyList(), 500).isEmpty())
    }
}
