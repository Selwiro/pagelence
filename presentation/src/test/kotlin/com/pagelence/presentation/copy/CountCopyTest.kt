package com.pagelence.presentation.copy

import com.pagelence.uim3.EnStrings
import com.pagelence.uim3.ZhStrings
import kotlin.test.Test
import kotlin.test.assertEquals

/** English count phrases take a singular form at one; Chinese stays unchanged. */
class CountCopyTest {
    @Test
    fun `english count phrases are singular at one`() {
        assertEquals("1 unit", EnStrings.unitCountText(1))
        assertEquals("2 units", EnStrings.unitCountText(2))
        assertEquals("1 unit", EnStrings.chapterUnitsText(1))
        assertEquals("1 of 1 unit read", EnStrings.readUnitsText(1, 1))
        assertEquals("1 of 2 units read", EnStrings.readUnitsText(1, 2))
        assertEquals("1 card", EnStrings.cardsText(1))
        assertEquals("1 mark", EnStrings.marksText(1))
        assertEquals("1 grammar point", EnStrings.grammarPointsText(1))
        assertEquals("1 unit finished today", EnStrings.todayUnitsText(1))
        assertEquals("1 source", EnStrings.sourcesText(1))
        assertEquals("Book", EnStrings.finishedBooksLabel(1))
        assertEquals("Books", EnStrings.finishedBooksLabel(0))
        assertEquals("Day", EnStrings.readingDaysLabel(1))
        assertEquals("Days", EnStrings.readingDaysLabel(3))
        assertEquals("Best 1 day", EnStrings.streakLongestText(1))
    }

    @Test
    fun `chinese keeps the same wording at any count`() {
        assertEquals("1 个单元", ZhStrings.unitCountText(1))
        assertEquals("2 个单元", ZhStrings.unitCountText(2))
        assertEquals("今日读完 1 个单元", ZhStrings.todayUnitsText(1))
        assertEquals("1 个来源", ZhStrings.sourcesText(1))
        assertEquals("读完", ZhStrings.finishedBooksLabel(1))
        assertEquals("阅读天数", ZhStrings.readingDaysLabel(1))
    }
}
