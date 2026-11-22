package com.pagelence.presentation.review

import com.pagelence.core.wordcards.ReviewGrade
import com.pagelence.uim3.model.ReviewSessionUi
import com.pagelence.uim3.model.WordCardUi
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** The round flow is pure UI state, so it is tested without a store or Android. */
class ReviewSessionRulesTest {
    private fun card(term: String) = WordCardUi(
        id = "book::p1::$term",
        term = term,
        lemma = term,
        ipa = null,
        zh = "释义",
        en = null,
        exampleEn = null,
        exampleZh = null,
        bookId = "book",
        bookTitle = "书",
    )

    @Test
    fun `an empty queue is a finished round`() {
        val session = ReviewSessionRules.start(emptyList())
        assertTrue(session.finished)
        assertNull(session.current)
        assertEquals(0, session.reviewed)
    }

    @Test
    fun `the round starts on the first card with the answer hidden`() {
        val session = ReviewSessionRules.start(listOf(card("alpha"), card("beta")))
        assertEquals("alpha", session.current?.term)
        assertFalse(session.revealed)
        assertEquals(0, session.index)
    }

    @Test
    fun `grading before the reveal is ignored`() {
        val session = ReviewSessionRules.start(listOf(card("alpha")))
        val graded = ReviewSessionRules.grade(session, ReviewGrade.GOOD)
        assertEquals(session, graded, "a stray tap must not skip a card the reader never saw")
        assertEquals(0, graded.reviewed)
    }

    @Test
    fun `reveal is idempotent`() {
        val session = ReviewSessionRules.reveal(ReviewSessionRules.start(listOf(card("alpha"))))
        assertTrue(session.revealed)
        assertEquals(session, ReviewSessionRules.reveal(session))
    }

    @Test
    fun `each grade advances and counts its bucket`() {
        var session = ReviewSessionRules.start(
            listOf(card("a"), card("b"), card("c"), card("d")),
        )
        session = ReviewSessionRules.grade(ReviewSessionRules.reveal(session), ReviewGrade.GOOD)
        session = ReviewSessionRules.grade(ReviewSessionRules.reveal(session), ReviewGrade.HARD)
        session = ReviewSessionRules.grade(ReviewSessionRules.reveal(session), ReviewGrade.AGAIN)
        assertEquals(3, session.index)
        assertEquals("d", session.current?.term)
        assertEquals(1, session.remembered)
        assertEquals(1, session.unsure)
        assertEquals(1, session.forgotten)
        assertEquals(3, session.reviewed)
        assertFalse(session.revealed)
        assertFalse(session.finished)
    }

    @Test
    fun `grading the last card finishes the round with the counts`() {
        var session = ReviewSessionRules.start(listOf(card("a"), card("b")))
        session = ReviewSessionRules.grade(ReviewSessionRules.reveal(session), ReviewGrade.GOOD)
        session = ReviewSessionRules.grade(ReviewSessionRules.reveal(session), ReviewGrade.AGAIN)
        assertTrue(session.finished)
        assertNull(session.current)
        assertEquals(2, session.reviewed)
        assertEquals(1, session.remembered)
        assertEquals(1, session.forgotten)
        assertEquals(0, session.unsure)
    }

    @Test
    fun `grading a finished round changes nothing`() {
        val done = ReviewSessionUi(cards = listOf(card("a")), index = 1, revealed = true)
        assertEquals(done, ReviewSessionRules.grade(done, ReviewGrade.GOOD))
    }
}
