package com.pagelence.core.content

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BookSearchTest {
    private val index: ContentIndex = ContentIndex(
        Book(
            id = "test-book",
            title = LocalizedText(zh = "测试", en = "Test"),
            chapters = listOf(
                Chapter(
                    id = "c1",
                    number = 1,
                    title = LocalizedText(zh = "第一章", en = "Chapter One"),
                    units = listOf(
                        StudyUnit(
                            id = "u1",
                            number = 1,
                            title = LocalizedText(zh = "第一单元", en = "Unit One"),
                            paragraphs = listOf(
                                Paragraph(
                                    id = "p1",
                                    text = "The sea was calm. The old man rowed.",
                                    sentences = listOf(
                                        Sentence("The sea was calm.", zh = "海面平静。"),
                                        Sentence("The old man rowed.", zh = "老人划着船。"),
                                    ),
                                ),
                                Paragraph(
                                    id = "p2",
                                    text = "A bird flew over the sea.",
                                    sentences = listOf(
                                        Sentence("A bird flew over the sea.", zh = "一只鸟飞过海面。"),
                                    ),
                                ),
                            ),
                        ),
                        StudyUnit(
                            id = "u2",
                            number = 2,
                            paragraphs = listOf(
                                Paragraph(
                                    id = "p3",
                                    text = "Sea and sky.",
                                    sentences = listOf(Sentence("Sea and sky.", zh = "海与天空。")),
                                ),
                            ),
                        ),
                    ),
                ),
            ),
        ),
    )

    @Test
    fun `english matches are case insensitive and keep document order`() {
        val results = BookSearch.search(index, "SEA")

        assertEquals(listOf("p1", "p2", "p3"), results.hits.map { it.paragraphId })
        assertEquals(listOf(1, 3, 1), results.hits.map { it.sentenceNumber })
        assertEquals(listOf("u1", "u1", "u2"), results.hits.map { it.unitId })
        assertTrue(results.hits.all { it.field == SearchField.TEXT })
    }

    @Test
    fun `existing translations are searchable too`() {
        val results = BookSearch.search(index, "海面")

        assertEquals(2, results.hits.size)
        assertTrue(results.hits.all { it.field == SearchField.TRANSLATION })
        assertEquals("p1", results.hits.first().paragraphId)
        assertEquals("海面平静。", results.hits.first().translation)
        assertEquals(0, results.hits.first().matchStart)
    }

    @Test
    fun `english wins over a translation hit in the same sentence`() {
        val results = BookSearch.search(index, "sea")

        assertEquals(1, results.hits.count { it.paragraphId == "p1" && it.sentenceNumber == 1 })
        assertEquals(SearchField.TEXT, results.hits.first().field)
    }

    @Test
    fun `limit reports truncation only when more matches exist`() {
        val cut = BookSearch.search(index, "sea", limit = 2)
        assertEquals(2, cut.hits.size)
        assertTrue(cut.truncated)

        val exact = BookSearch.search(index, "sea", limit = 3)
        assertEquals(3, exact.hits.size)
        assertFalse(exact.truncated)

        val empty = BookSearch.search(index, "   ")
        assertTrue(empty.isEmpty)
        assertFalse(empty.truncated)
    }

    @Test
    fun `snippet keeps the match and marks a cut`() {
        val long = "It was a long calm night and the old man watched the sea for hours without a word."
        val at = long.indexOf("sea")
        val snippet = snippetOf(long, at, 3, radius = 10)

        assertTrue(snippet.text.startsWith("…"), snippet.text)
        assertTrue(snippet.text.endsWith("…"), snippet.text)
        assertEquals("sea", snippet.text.substring(snippet.matchStart, snippet.matchStart + snippet.matchLength))
        assertTrue(snippet.text.length < long.length)
    }

    @Test
    fun `skeleton paragraphs fall back to the frozen splitter`() {
        val book = Book(
            id = "skeleton",
            title = LocalizedText(zh = "骨架"),
            chapters = listOf(
                Chapter(
                    id = "c1",
                    number = 1,
                    units = listOf(
                        StudyUnit(
                            id = "u1",
                            number = 1,
                            paragraphs = listOf(Paragraph(id = "p1", text = "One sea. Two seas.")),
                        ),
                    ),
                ),
            ),
        )

        val results = BookSearch.search(ContentIndex(book), "sea")
        assertEquals(2, results.hits.size)
        assertEquals(listOf(1, 2), results.hits.map { it.sentenceNumber })
    }
}
