package com.pagelence.core.wordcards

import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The schedule is the only part of a word card the app computes over time, so
 * it is tested as a pure function of (card, grade, today) — no clock, no store.
 */
class ReviewScheduleTest {
    private val today = LocalDate.of(2026, 11, 22)
    private val now = "2026-11-22T02:00:00Z"

    private fun card(
        term: String = "word",
        createdAt: String = "2026-11-20T00:00:00Z",
        dueAt: String? = null,
        intervalDays: Int = 0,
        ease: Double = Srs.DEFAULT_EASE,
        reviews: Int = 0,
        lapses: Int = 0,
        lastReviewedAt: String? = null,
        deletedAt: String? = null,
    ) = WordCard(
        id = "book::p1::$term",
        bookId = "book",
        paragraphId = "p1",
        hintId = term,
        term = term,
        lemma = term,
        createdAt = createdAt,
        updatedAt = createdAt,
        deletedAt = deletedAt,
        dueAt = dueAt,
        intervalDays = intervalDays,
        ease = ease,
        reviews = reviews,
        lapses = lapses,
        lastReviewedAt = lastReviewedAt,
    )

    // --- three grades ----------------------------------------------------

    @Test
    fun `good schedules the first review one day out`() {
        val graded = Srs.grade(card(), ReviewGrade.GOOD, today, now)
        assertEquals(1, graded.intervalDays)
        assertEquals("2026-11-23", graded.dueAt)
        assertEquals(1, graded.reviews)
        assertEquals(0, graded.lapses)
        assertEquals(Srs.DEFAULT_EASE, graded.ease)
        assertEquals(ReviewGrade.GOOD, graded.lastGrade)
        assertEquals(now, graded.lastReviewedAt)
        assertEquals(now, graded.updatedAt)
    }

    @Test
    fun `good schedules the second review three days out`() {
        val graded = Srs.grade(
            card(dueAt = "2026-11-22", intervalDays = 1, reviews = 1, lastReviewedAt = "2026-11-21T02:00:00Z"),
            ReviewGrade.GOOD,
            today,
            now,
        )
        assertEquals(3, graded.intervalDays)
        assertEquals("2026-11-25", graded.dueAt)
        assertEquals(2, graded.reviews)
    }

    @Test
    fun `later good reviews multiply by ease`() {
        val graded = Srs.grade(
            card(dueAt = "2026-11-22", intervalDays = 3, reviews = 2),
            ReviewGrade.GOOD,
            today,
            now,
        )
        assertEquals(8, graded.intervalDays, "ceil(3 × 2.5)")
        assertEquals("2026-11-30", graded.dueAt)
    }

    @Test
    fun `again resets the interval and counts a lapse`() {
        val graded = Srs.grade(
            card(dueAt = "2026-11-22", intervalDays = 20, ease = 2.5, reviews = 4),
            ReviewGrade.AGAIN,
            today,
            now,
        )
        assertEquals(0, graded.intervalDays)
        assertEquals("2026-11-22", graded.dueAt, "comes back later the same day")
        assertEquals(1, graded.lapses)
        assertEquals(5, graded.reviews)
        assertEquals(2.3, graded.ease, absoluteTolerance = 1e-9)
        assertTrue(Srs.isDue(graded, today))
        assertEquals(CardMastery.LEARNING, Srs.mastery(graded))
    }

    @Test
    fun `hard takes a small step and lowers ease less`() {
        val graded = Srs.grade(
            card(dueAt = "2026-11-22", intervalDays = 3, ease = 2.5, reviews = 2),
            ReviewGrade.HARD,
            today,
            now,
        )
        assertEquals(4, graded.intervalDays, "ceil(3 × 1.2)")
        assertEquals(2.35, graded.ease, absoluteTolerance = 1e-9)
        assertEquals(0, graded.lapses)
    }

    @Test
    fun `hard on a first review still waits a day`() {
        val graded = Srs.grade(card(), ReviewGrade.HARD, today, now)
        assertEquals(1, graded.intervalDays)
        assertEquals("2026-11-23", graded.dueAt)
    }

    @Test
    fun `hard never stalls at a one day interval`() {
        val first = Srs.grade(card(), ReviewGrade.GOOD, today, now)
        val second = Srs.grade(first, ReviewGrade.HARD, today, now)
        assertEquals(2, second.intervalDays, "ceil(1 × 1.2) rounds up, so the card moves on")
    }

    // --- bounds ----------------------------------------------------------

    @Test
    fun `ease never drops below the floor`() {
        val graded = Srs.grade(
            card(dueAt = "2026-11-22", intervalDays = 5, ease = 1.4, reviews = 3),
            ReviewGrade.AGAIN,
            today,
            now,
        )
        assertEquals(Srs.MIN_EASE, graded.ease, absoluteTolerance = 1e-9)
    }

    @Test
    fun `ease never rises above the ceiling and stored values are clamped`() {
        val inflated = card(ease = 9.0)
        assertEquals(Srs.MAX_EASE, Srs.easeOf(inflated), absoluteTolerance = 1e-9)
        val graded = Srs.grade(inflated, ReviewGrade.GOOD, today, now)
        assertEquals(Srs.MAX_EASE, graded.ease, absoluteTolerance = 1e-9)
        assertEquals(1, graded.intervalDays, "first review uses the 1-day step, not the inflated ease")
    }

    @Test
    fun `intervals stop at one year`() {
        val graded = Srs.grade(
            card(dueAt = "2026-11-22", intervalDays = 300, reviews = 6),
            ReviewGrade.GOOD,
            today,
            now,
        )
        assertEquals(Srs.MAX_INTERVAL_DAYS, graded.intervalDays)
        assertEquals("2027-11-22", graded.dueAt)
    }

    // --- due dates -------------------------------------------------------

    @Test
    fun `a card without a schedule is new and due immediately`() {
        val legacy = card()
        assertTrue(Srs.isNew(legacy))
        assertTrue(Srs.isDue(legacy, today))
        assertNull(Srs.dueOn(legacy))
    }

    @Test
    fun `due dates are compared by day not by clock`() {
        val scheduled = card(dueAt = "2026-11-22", reviews = 1, intervalDays = 1)
        assertTrue(Srs.isDue(scheduled, today), "due today is due")
        assertFalse(Srs.isDue(scheduled, today.minusDays(1)), "not due yesterday")
        val overdue = scheduled.copy(dueAt = "2026-11-18")
        assertTrue(Srs.isDue(overdue, today))
        assertFalse(Srs.isDue(scheduled.copy(dueAt = "2026-11-23"), today), "tomorrow is not due")
    }

    @Test
    fun `a reviewed card with an unreadable due date is due rather than lost`() {
        assertTrue(Srs.isDue(card(dueAt = "not-a-date", reviews = 2), today))
    }

    @Test
    fun `tombstones are never due`() {
        val deleted = card(deletedAt = "2026-11-21T00:00:00Z")
        assertFalse(Srs.isDue(deleted, today))
        assertTrue(Srs.queue(listOf(deleted), today).isEmpty)
    }

    @Test
    fun `queue holds overdue and new cards but not future ones`() {
        val fresh = card(term = "apple")
        val overdue = card(term = "pear", dueAt = "2026-11-20", reviews = 1, intervalDays = 1)
        val dueToday = card(term = "plum", dueAt = "2026-11-22", reviews = 2, intervalDays = 3)
        val later = card(term = "kiwi", dueAt = "2026-12-05", reviews = 2, intervalDays = 3)
        val queue = Srs.queue(listOf(fresh, later, dueToday, overdue), today)
        assertEquals(listOf("pear", "plum"), queue.due.map { it.term })
        assertEquals(listOf("apple"), queue.fresh.map { it.term })
        assertEquals(listOf("pear", "plum", "apple"), queue.cards.map { it.term })
        assertFalse(queue.isEmpty)
    }

    @Test
    fun `an empty card list makes an empty queue`() {
        assertTrue(Srs.queue(emptyList(), today).isEmpty)
    }

    @Test
    fun `grading after midnight counts from the new day`() {
        val first = Srs.grade(card(), ReviewGrade.GOOD, today, now)
        assertFalse(Srs.isDue(first, today), "scheduled for tomorrow")
        assertTrue(Srs.isDue(first, today.plusDays(1)), "due when the calendar turns")
        val second = Srs.grade(first, ReviewGrade.GOOD, today.plusDays(1), "2026-11-23T02:00:00Z")
        assertEquals("2026-11-26", second.dueAt, "three days from the day of the review")
    }

    // --- mastery ---------------------------------------------------------

    @Test
    fun `mastery follows the interval`() {
        assertEquals(CardMastery.NEW, Srs.mastery(card()))
        assertEquals(CardMastery.LEARNING, Srs.mastery(card(reviews = 2, intervalDays = 3)))
        assertEquals(CardMastery.MASTERED, Srs.mastery(card(reviews = 4, intervalDays = 21)))
        assertEquals(
            CardMastery.LEARNING,
            Srs.mastery(card(reviews = 5, intervalDays = 2, lapses = 3)),
            "a card that keeps being forgotten is not mastered",
        )
    }

    // --- merge -----------------------------------------------------------

    @Test
    fun `merge keeps the schedule of the later review`() {
        val ungraded = card(term = "boat")
        val graded = Srs.grade(ungraded, ReviewGrade.GOOD, today, now)
        val merged = Srs.mergeSchedule(ungraded, graded)
        assertEquals(graded.dueAt, merged.dueAt)
        assertEquals(graded.reviews, merged.reviews)
        assertEquals(ungraded.term, merged.term)
    }

    @Test
    fun `merge never drops a schedule for a side that has none`() {
        val graded = Srs.grade(card(term = "boat"), ReviewGrade.GOOD, today, now)
        val merged = Srs.mergeSchedule(graded, card(term = "boat"))
        assertEquals(graded.dueAt, merged.dueAt)
        assertEquals(graded.reviews, merged.reviews)
    }

    @Test
    fun `merge keeps the earlier review when the other side is older`() {
        val older = Srs.grade(card(term = "boat"), ReviewGrade.GOOD, today.minusDays(3), "2026-11-19T02:00:00Z")
        val newer = Srs.grade(older, ReviewGrade.GOOD, today, now)
        val merged = Srs.mergeSchedule(newer, older)
        assertEquals(newer.dueAt, merged.dueAt)
        assertEquals(newer.reviews, merged.reviews)
    }

    @Test
    fun `merge ignores an unparseable review timestamp`() {
        val base = Srs.grade(card(term = "boat"), ReviewGrade.GOOD, today, now)
        val broken = card(term = "boat").copy(
            dueAt = "2027-01-01",
            reviews = 9,
            lastReviewedAt = "yesterday-ish",
        )
        val merged = Srs.mergeSchedule(base, broken)
        assertEquals(base.dueAt, merged.dueAt)
        assertEquals(base.reviews, merged.reviews)
    }

    @Test
    fun `merge without any review keeps the base card`() {
        val base = card(term = "boat")
        val merged = Srs.mergeSchedule(base, card(term = "boat"))
        assertEquals(base, merged)
    }
}
