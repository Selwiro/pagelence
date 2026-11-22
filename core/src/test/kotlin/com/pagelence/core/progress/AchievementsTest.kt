package com.pagelence.core.progress

import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AchievementsTest {
    private val today = LocalDate.of(2026, 11, 22)

    private fun byId(items: List<AchievementProgress>) = items.associateBy { it.id }

    private fun book(date: String?, level: String? = null) =
        FinishedBookFact(finishedOn = date?.let(LocalDate::parse), level = level)

    private fun day(seconds: Int) = DailyActivity(readingSeconds = seconds)

    @Test
    fun `an empty state unlocks nothing and does not crash`() {
        val items = Achievements.evaluate(AchievementInput(), today)

        assertEquals(AchievementId.entries.size, items.size)
        assertTrue(items.none { it.unlocked })
        assertTrue(items.all { it.unlockedOn == null })
        assertEquals(0f, items.first().fraction.coerceAtLeast(0f))
    }

    @Test
    fun `finishing books unlocks first and third with their dates`() {
        val one = Achievements.evaluate(
            AchievementInput(finishedBooks = listOf(book("2026-11-20"))),
            today,
        )
        val first = byId(one).getValue(AchievementId.FIRST_BOOK)
        assertTrue(first.unlocked)
        assertEquals(LocalDate.of(2026, 11, 20), first.unlockedOn)
        assertFalse(byId(one).getValue(AchievementId.THREE_BOOKS).unlocked)

        val three = Achievements.evaluate(
            AchievementInput(
                finishedBooks = listOf(
                    book("2026-11-18"),
                    book("2026-11-19"),
                    book("2026-11-21"),
                ),
            ),
            today,
        )
        val third = byId(three).getValue(AchievementId.THREE_BOOKS)
        assertTrue(third.unlocked)
        assertEquals(LocalDate.of(2026, 11, 21), third.unlockedOn)
    }

    @Test
    fun `one hundred units unlocks exactly at the threshold`() {
        val under = byId(Achievements.evaluate(AchievementInput(totalUnitsRead = 99), today))
            .getValue(AchievementId.HUNDRED_UNITS)
        assertFalse(under.unlocked)
        assertEquals(99, under.current)
        assertEquals(100, under.target)

        val exact = byId(Achievements.evaluate(AchievementInput(totalUnitsRead = 100), today))
            .getValue(AchievementId.HUNDRED_UNITS)
        assertTrue(exact.unlocked)
        assertNull(exact.unlockedOn, "per-unit dates are not stored")

        val over = byId(Achievements.evaluate(AchievementInput(totalUnitsRead = 208), today))
            .getValue(AchievementId.HUNDRED_UNITS)
        assertTrue(over.unlocked)
        assertEquals(1f, over.fraction)
    }

    @Test
    fun `streak achievements follow the longest run and its end date`() {
        val activity = mapOf(
            "2026-11-19" to day(900),
            "2026-11-20" to day(900),
            "2026-11-21" to day(900),
            "2026-11-22" to day(60),
        )

        val items = byId(
            Achievements.evaluate(
                AchievementInput(activity = activity, goalMinutes = 10),
                today,
            ),
        )

        val three = items.getValue(AchievementId.STREAK_3)
        assertTrue(three.unlocked)
        assertEquals(LocalDate.of(2026, 11, 21), three.unlockedOn)
        assertFalse(items.getValue(AchievementId.STREAK_7).unlocked)
        assertEquals(3, items.getValue(AchievementId.STREAK_7).current)
    }

    @Test
    fun `cards and marks unlock on the last dated item`() {
        val cardDates = (1..50).map { LocalDate.of(2026, 9, 1).plusDays(it.toLong()) }
        val noteDates = (1..19).map { LocalDate.of(2026, 10, 1).plusDays(it.toLong()) }

        val items = byId(
            Achievements.evaluate(
                AchievementInput(wordCardDates = cardDates, annotationDates = noteDates),
                today,
            ),
        )

        val cards = items.getValue(AchievementId.CARDS_50)
        assertTrue(cards.unlocked)
        assertEquals(cardDates.last(), cards.unlockedOn)
        assertFalse(items.getValue(AchievementId.NOTES_20).unlocked)
        assertEquals(19, items.getValue(AchievementId.NOTES_20).current)
    }

    @Test
    fun `ten hours unlocks on the day the total crossed`() {
        val activity = mapOf(
            "2026-11-18" to day(5 * 3600),
            "2026-11-20" to day(4 * 3600),
            "2026-11-21" to day(2 * 3600),
        )

        val item = byId(
            Achievements.evaluate(AchievementInput(activity = activity), today),
        ).getValue(AchievementId.READING_HOURS_10)

        assertTrue(item.unlocked)
        assertEquals(LocalDate.of(2026, 11, 21), item.unlockedOn)
    }

    @Test
    fun `a B1 finish unlocks the level achievement`() {
        val items = byId(
            Achievements.evaluate(
                AchievementInput(finishedBooks = listOf(book("2026-11-22", "B1"))),
                today,
            ),
        )

        assertTrue(items.getValue(AchievementId.LEVEL_B1).unlocked)
        assertEquals(LocalDate.of(2026, 11, 22), items.getValue(AchievementId.LEVEL_B1).unlockedOn)
    }

    @Test
    fun `ordering puts unlocked first and the closest next`() {
        val items = Achievements.evaluate(
            AchievementInput(
                finishedBooks = listOf(book("2026-11-20")),
                totalUnitsRead = 99,
            ),
            today,
        )

        val ordered = Achievements.ordered(items, today)
        assertTrue(ordered.first().unlocked)
        assertTrue(ordered.drop(1).none { it.unlocked })
        assertEquals(AchievementId.HUNDRED_UNITS, ordered[1].id, "closest to unlocking")
    }

    @Test
    fun `the unit achievement is highlighted only by the book that crossed it`() {
        val firstBook = FinishedBookFact(finishedOn = LocalDate.of(2026, 11, 20), level = "B1", units = 104)
        val crossed = Achievements.reachedBy(
            AchievementInput(finishedBooks = listOf(firstBook), totalUnitsRead = 104),
            firstBook,
            today,
        )
        assertTrue(AchievementId.HUNDRED_UNITS in crossed)

        val thirdBook = FinishedBookFact(finishedOn = LocalDate.of(2026, 11, 22), level = "B1", units = 104)
        val later = Achievements.reachedBy(
            AchievementInput(
                finishedBooks = listOf(firstBook, firstBook, thirdBook),
                totalUnitsRead = 208,
            ),
            thirdBook,
            today,
        )
        assertFalse(AchievementId.HUNDRED_UNITS in later, "100 was crossed long before this book")
    }

    @Test
    fun `reachedBy reports only what this finish plausibly crossed`() {
        val input = AchievementInput(finishedBooks = listOf(book("2026-11-22", "B1")))
        val reached = Achievements.reachedBy(input, book("2026-11-22", "B1"), today)

        assertTrue(AchievementId.FIRST_BOOK in reached)
        assertTrue(AchievementId.LEVEL_B1 in reached)
        assertFalse(AchievementId.THREE_BOOKS in reached)
    }
}
