package com.pagelence.presentation.reader

import com.pagelence.core.prefs.Preferences
import com.pagelence.core.reading.SentencePage
import com.pagelence.uim3.UiLanguage
import com.pagelence.uim3.model.ReaderUiState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** The page order both shells rely on; measured text pages are passed in. */
class ReaderFlowTest {
    private fun state(
        hasPrevious: Boolean = false,
        hasNext: Boolean = false,
    ) = ReaderUiState(
        bookId = "doctor-dolittle-1920",
        unitId = "chapter-01-u01",
        bookTitle = "怪医杜立德",
        chapterTitle = "帕德尔比",
        unitTitle = "戴高礼帽的医生",
        chapterNumber = 1,
        unitNumber = 1,
        unitPosition = 0,
        unitCount = 103,
        introductionZh = null,
        introductionEn = null,
        paragraphs = emptyList(),
        startParagraphId = null,
        quiz = emptyList(),
        quizSubmitted = false,
        quizScore = 0,
        grammar = null,
        unitRead = false,
        hasNextUnit = hasNext,
        hasPreviousUnit = hasPrevious,
        preference = Preferences(),
        language = UiLanguage.ZH,
        wordPanel = null,
        cardsCount = 0,
    )

    private fun page() = SentencePage(emptyList(), 100, 100, scrollable = false)

    @Test
    fun `paged layout puts bridge pages at both open edges`() {
        val pages = ReaderFlow.pages(
            state = state(hasPrevious = true, hasNext = true),
            textPages = listOf(page(), page()),
            hasGuide = true,
            paged = true,
        )

        assertEquals(ReaderPage.PreviousUnit, pages.first())
        assertEquals(ReaderPage.Guide, pages[1])
        assertEquals(2, pages.count { it is ReaderPage.Text })
        assertEquals(ReaderPage.NextUnit, pages.last())
    }

    @Test
    fun `scroll layout has one text page and no bridge pages`() {
        val pages = ReaderFlow.pages(
            state = state(hasPrevious = true, hasNext = true),
            textPages = listOf(page(), page()),
            hasGuide = false,
            paged = false,
        )

        assertEquals(listOf<ReaderPage>(ReaderPage.TextScroll), pages)
        assertTrue(pages.none { it is ReaderPage.Text })
    }

    @Test
    fun `a unit without guide or neighbours is just its text pages`() {
        val pages = ReaderFlow.pages(
            state = state(),
            textPages = listOf(page(), page(), page()),
            hasGuide = false,
            paged = true,
        )

        assertEquals(3, pages.size)
        assertTrue(pages.all { it is ReaderPage.Text })
    }
}
