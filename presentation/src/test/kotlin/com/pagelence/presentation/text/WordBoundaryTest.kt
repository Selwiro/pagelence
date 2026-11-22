package com.pagelence.presentation.text

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class WordBoundaryTest {
    @Test
    fun expandsToWholeWord() {
        val text = "The doctor had a funny little house."
        val index = text.indexOf("doctor") + 2
        val hit = WordBoundary.wordAt(text, index)
        assertEquals("doctor", hit?.text)
        assertEquals(text.indexOf("doctor"), hit?.start)
        assertEquals(text.indexOf("doctor") + 5, hit?.end)
    }

    @Test
    fun keepsApostrophesAndHyphensInside() {
        val text = "It's a well-known place."
        assertEquals("It's", WordBoundary.wordAt(text, 1)?.text)
        assertEquals("well-known", WordBoundary.wordAt(text, text.indexOf("known"))?.text)
    }

    @Test
    fun tapsOnSpaceFallBackToOneChar() {
        val text = "one two"
        assertEquals("one", WordBoundary.wordAt(text, 3)?.text)
    }

    @Test
    fun punctuationOnlyReturnsNull() {
        assertNull(WordBoundary.wordAt("...", 1))
        assertNull(WordBoundary.wordAt(" —— ", 1))
    }

    @Test
    fun emptyTextReturnsNull() {
        assertNull(WordBoundary.wordAt("", 0))
    }

    @Test
    fun indexOutsideRangeIsClamped() {
        val text = "word"
        assertEquals("word", WordBoundary.wordAt(text, 99)?.text)
        assertEquals("word", WordBoundary.wordAt(text, -5)?.text)
    }
}
