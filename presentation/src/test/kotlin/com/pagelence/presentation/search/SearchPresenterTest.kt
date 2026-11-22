package com.pagelence.presentation.search

import com.pagelence.core.content.Book
import com.pagelence.core.content.Chapter
import com.pagelence.core.content.ContentIndex
import com.pagelence.core.content.LocalizedText
import com.pagelence.core.content.Paragraph
import com.pagelence.core.content.Sentence
import com.pagelence.core.content.StudyUnit
import com.pagelence.uim3.UiLanguage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SearchPresenterTest {
    private val index = ContentIndex(
        Book(
            id = "book",
            title = LocalizedText(zh = "测试书", en = "Test Book"),
            chapters = listOf(
                Chapter(
                    id = "c1",
                    number = 2,
                    title = LocalizedText(zh = "第二章", en = "Chapter Two"),
                    units = listOf(
                        StudyUnit(
                            id = "u1",
                            number = 3,
                            title = LocalizedText(zh = "第三单元", en = "Unit Three"),
                            paragraphs = listOf(
                                Paragraph(
                                    id = "p1",
                                    text = "The sea was calm.",
                                    sentences = listOf(Sentence("The sea was calm.", zh = "海面平静。")),
                                ),
                            ),
                        ),
                    ),
                ),
            ),
        ),
    )

    @Test
    fun `english hit maps location and labels`() {
        val state = SearchPresenter(UiLanguage.ZH).search(index, "sea")

        assertEquals("测试书", state.bookTitle)
        assertTrue(state.hasSearched)
        val hit = state.hits.single()
        assertEquals("第二章", hit.chapterLabel)
        assertEquals("第三单元", hit.unitLabel)
        assertEquals("p1", hit.paragraphId)
        assertEquals(1, hit.sentenceNumber)
        assertFalse(hit.matchedTranslation)
        assertEquals("sea", hit.snippet.substring(hit.matchStart, hit.matchStart + hit.matchLength))
    }

    @Test
    fun `translation hit keeps the english sentence as context`() {
        val state = SearchPresenter(UiLanguage.ZH).search(index, "海面")

        val hit = state.hits.single()
        assertTrue(hit.matchedTranslation)
        assertEquals("The sea was calm.", hit.sentence)
        assertEquals("海面", hit.snippet.substring(hit.matchStart, hit.matchStart + hit.matchLength))
    }

    @Test
    fun `blank query returns the prompt state`() {
        val state = SearchPresenter(UiLanguage.EN).search(index, "   ")

        assertFalse(state.hasSearched)
        assertTrue(state.hits.isEmpty())
        assertEquals("Test Book", state.bookTitle)
    }
}
