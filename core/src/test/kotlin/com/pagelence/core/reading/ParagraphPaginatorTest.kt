package com.pagelence.core.reading

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ParagraphPaginatorTest {
    private fun paragraphs(vararg heights: Int): List<MeasuredParagraph> =
        heights.mapIndexed { index, height -> MeasuredParagraph("p${index + 1}", height) }

    @Test
    fun `every paragraph appears exactly once in order`() {
        val source = paragraphs(100, 200, 300, 400, 500)
        val pages = ParagraphPaginator.paginate(source, pageHeightPx = 400, maxPageHeightPx = 800)
        val flattened = pages.flatMap { it.paragraphIds }
        assertEquals(source.map { it.id }, flattened)
        assertTrue(pages.none { it.paragraphIds.isEmpty() })
    }

    @Test
    fun `multi paragraph pages stay within the max height`() {
        val source = paragraphs(100, 200, 300, 400, 500)
        val pages = ParagraphPaginator.paginate(source, pageHeightPx = 400, maxPageHeightPx = 800)
        pages.filter { it.paragraphIds.size > 1 }.forEach { page ->
            assertTrue(page.heightPx <= 800, "${page.paragraphIds} is ${page.heightPx}")
        }
        assertTrue(pages.any { it.paragraphIds.size > 1 })
    }

    @Test
    fun `a paragraph taller than the max gets its own page`() {
        val source = paragraphs(200, 1200, 300)
        val pages = ParagraphPaginator.paginate(source, pageHeightPx = 500, maxPageHeightPx = 1000)
        assertEquals(3, pages.size)
        assertEquals(listOf("p2"), pages[1].paragraphIds)
        assertTrue(pages[1].heightPx > 1000)
        assertTrue(pages[1].scrollable)
    }

    @Test
    fun `a page taller than one viewport is scrollable`() {
        val pages = ParagraphPaginator.paginate(
            paragraphs(400, 400),
            pageHeightPx = 500,
            maxPageHeightPx = 1000,
        )
        assertEquals(1, pages.size)
        assertEquals(800, pages[0].heightPx)
        assertTrue(pages[0].scrollable)
    }

    @Test
    fun `a short page is not scrollable`() {
        val pages = ParagraphPaginator.paginate(
            paragraphs(200, 200),
            pageHeightPx = 500,
            maxPageHeightPx = 1000,
        )
        assertEquals(1, pages.size)
        assertFalse(pages[0].scrollable)
    }

    @Test
    fun `page anchors use the first paragraph`() {
        val pages = ParagraphPaginator.paginate(
            paragraphs(600, 200, 300),
            pageHeightPx = 500,
            maxPageHeightPx = 1000,
        )
        assertEquals("p1", pages[0].anchorParagraphId)
        assertEquals("p3", pages[1].anchorParagraphId)
    }

    @Test
    fun `buckets pick the signed max scale`() {
        assertEquals(ScreenBucket.COMPACT, ScreenBucket.of(360))
        assertEquals(ScreenBucket.MEDIUM, ScreenBucket.of(600))
        assertEquals(ScreenBucket.LARGE, ScreenBucket.of(906))
        assertEquals(ScreenBucket.XLARGE, ScreenBucket.of(1280))
        assertEquals(750, ParagraphPaginator.maxPageHeight(500, ScreenBucket.MEDIUM))
        assertEquals(1000, ParagraphPaginator.maxPageHeight(500, ScreenBucket.LARGE))
    }

    @Test
    fun `empty input produces no pages`() {
        assertTrue(ParagraphPaginator.paginate(emptyList(), 500, 1000).isEmpty())
    }
}
