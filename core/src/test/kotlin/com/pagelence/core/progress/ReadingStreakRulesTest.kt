package com.pagelence.core.progress

import com.pagelence.core.prefs.DailyGoal
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ReadingStreakRulesTest {
    private val today = LocalDate.of(2026, 11, 22)

    private fun day(seconds: Int, units: Int = 0) =
        DailyActivity(completedUnits = units, readingSeconds = seconds)

    @Test
    fun `reaching the goal exactly marks the day`() {
        val streak = ReadingStreakRules.summarize(
            mapOf("2026-11-22" to day(600)),
            goalMinutes = 10,
            today = today,
        )

        assertTrue(streak.metToday)
        assertEquals(0, streak.secondsToGoal)
        assertEquals(1, streak.currentDays)
        assertEquals(600, streak.todaySeconds)
    }

    @Test
    fun `one second short is not met and the remainder is reported`() {
        val streak = ReadingStreakRules.summarize(
            mapOf("2026-11-22" to day(599)),
            goalMinutes = 10,
            today = today,
        )

        assertFalse(streak.metToday)
        assertEquals(1, streak.secondsToGoal)
        assertEquals(0, streak.currentDays)
        assertFalse(streak.restarting, "no history yet")
    }

    @Test
    fun `a legacy day with a finished unit counts as met, a short day does not`() {
        assertTrue(ReadingStreakRules.isGoalMet(day(0, units = 1), 10))
        assertFalse(ReadingStreakRules.isGoalMet(day(0, units = 0), 10))
        assertFalse(
            ReadingStreakRules.isGoalMet(day(120, units = 1), 10),
            "time wins once it exists: finishing one unit in 2 minutes is not the goal",
        )
    }

    @Test
    fun `yesterday met and today open keeps the run visible`() {
        val activity = mapOf(
            "2026-11-20" to day(600),
            "2026-11-21" to day(900),
            "2026-11-22" to day(60),
        )

        val streak = ReadingStreakRules.summarize(activity, 10, today)

        assertFalse(streak.metToday)
        assertEquals(2, streak.currentDays)
        assertEquals(540, streak.secondsToGoal)
        assertEquals(2, streak.metDays)
        assertFalse(streak.restarting)
    }

    @Test
    fun `a missed day resets the current run but keeps the best one`() {
        val activity = mapOf(
            "2026-11-18" to day(600),
            "2026-11-19" to day(600),
            "2026-11-20" to day(120),
            "2026-11-22" to day(120),
        )

        val streak = ReadingStreakRules.summarize(activity, 10, today)

        assertEquals(0, streak.currentDays)
        assertEquals(2, streak.longestDays)
        assertEquals(2, streak.metDays)
        assertTrue(streak.restarting)
    }

    @Test
    fun `today met extends the run that ended yesterday`() {
        val activity = mapOf(
            "2026-11-20" to day(600),
            "2026-11-21" to day(600),
            "2026-11-22" to day(600),
        )

        val streak = ReadingStreakRules.summarize(activity, 10, today)

        assertEquals(3, streak.currentDays)
        assertEquals(3, streak.longestDays)
    }

    @Test
    fun `the goal is sanitized and unsupported values fall back to ten minutes`() {
        assertEquals(10, ReadingStreakRules.summarize(emptyMap(), 7, today).goalMinutes)
        assertEquals(20, ReadingStreakRules.summarize(emptyMap(), 20, today).goalMinutes)

        val streak = ReadingStreakRules.summarize(mapOf("2026-11-22" to day(600)), 30, today)
        assertFalse(streak.metToday, "600 seconds is short of a 30-minute goal")
        assertEquals(1200, streak.secondsToGoal)
    }

    @Test
    fun `dates after today are ignored and empty input is safe`() {
        val empty = ReadingStreakRules.summarize(emptyMap(), DailyGoal.DEFAULT_MINUTES, today)
        assertEquals(0, empty.currentDays)
        assertEquals(0, empty.longestDays)
        assertEquals(0, empty.metDays)
        assertFalse(empty.hasHistory)
        assertFalse(empty.restarting)

        val future = ReadingStreakRules.summarize(mapOf("2026-11-25" to day(900)), 10, today)
        assertEquals(0, future.metDays)
    }

    @Test
    fun `absurd day totals are capped at twelve hours`() {
        val streak = ReadingStreakRules.summarize(
            mapOf("2026-11-22" to day(Int.MAX_VALUE)),
            30,
            today,
        )

        assertEquals(ReadingStreakRules.MAX_SECONDS_PER_DAY, streak.todaySeconds)
        assertTrue(streak.metToday)
    }

    @Test
    fun `malformed keys are skipped`() {
        val streak = ReadingStreakRules.summarize(
            mapOf("not-a-date" to day(900), "2026-11-22" to day(600)),
            10,
            today,
        )

        assertEquals(1, streak.metDays)
        assertEquals(1, streak.currentDays)
    }
}
