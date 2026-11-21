package com.pagelence.core.reading

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class WordSelectionStateTest {
    private val word = SelectedTerm(
        paragraphId = "chapter-01-p002",
        sentenceNumber = 4,
        spelling = "folks",
        term = "folks",
        ipa = "fəʊks",
        gloss = "人们",
    )
    private val other = word.copy(spelling = "skiff", term = "skiff")

    @Test
    fun `tapping a word shows the strip and no sheet`() {
        val state = WordSelectionState.select(word)
        assertTrue(state is WordSelectionState.Shown)
        assertEquals(word, WordSelectionState.word(state))
        assertFalse(WordSelectionState.isDetails(state))
    }

    @Test
    fun `details open from the strip and close back to it`() {
        val shown = WordSelectionState.select(word)
        val details = WordSelectionState.openDetails(shown)
        assertTrue(WordSelectionState.isDetails(details))
        assertEquals(word, WordSelectionState.word(details))

        val closed = WordSelectionState.closeDetails(details)
        assertTrue(closed is WordSelectionState.Shown)
        assertEquals(word, WordSelectionState.word(closed))
    }

    @Test
    fun `details cannot open while hidden and hidden has no word`() {
        val hidden = WordSelectionState.clear()
        assertTrue(hidden is WordSelectionState.Hidden)
        assertNull(WordSelectionState.word(hidden))
        assertTrue(WordSelectionState.openDetails(hidden) is WordSelectionState.Hidden)
        assertTrue(WordSelectionState.closeDetails(hidden) is WordSelectionState.Hidden)
    }

    @Test
    fun `tapping another word replaces a shown or open selection`() {
        val replaced = WordSelectionState.select(other)
        assertEquals(other, WordSelectionState.word(replaced))
        assertFalse(WordSelectionState.isDetails(replaced))

        val open = WordSelectionState.openDetails(WordSelectionState.select(word))
        val replacedOpen = WordSelectionState.select(other)
        assertEquals(other, WordSelectionState.word(replacedOpen))
        assertFalse(WordSelectionState.isDetails(replacedOpen), "a new word returns to the strip")
        assertTrue(open is WordSelectionState.Details)
    }

    @Test
    fun `leaving the reader clears the selection`() {
        val state = WordSelectionState.openDetails(WordSelectionState.select(word))
        assertTrue(WordSelectionState.clear() is WordSelectionState.Hidden)
        assertTrue(state is WordSelectionState.Details)
    }
}
