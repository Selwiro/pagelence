package com.pagelence.core.progress

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DailyActivityRulesTest {
    @Test
    fun `a reading day is added once and counts as one day`() {
        val first = DailyActivityRules.recordReadingDay(emptyMap(), "2026-11-19")
        assertEquals(1, DailyActivityRules.readingDays(first))
        assertEquals(0, first.getValue("2026-11-19").completedUnits)

        val same = DailyActivityRules.recordReadingDay(first, "2026-11-19")
        assertEquals(first, same)
        assertEquals(1, DailyActivityRules.readingDays(same))
    }

    @Test
    fun `completed units accumulate per day`() {
        var activity = DailyActivityRules.recordReadingDay(emptyMap(), "2026-11-19")
        activity = DailyActivityRules.recordCompletedUnit(activity, "2026-11-19")
        activity = DailyActivityRules.recordCompletedUnit(activity, "2026-11-19")
        activity = DailyActivityRules.recordCompletedUnit(activity, "2026-11-20")

        assertEquals(2, activity.getValue("2026-11-19").completedUnits)
        assertEquals(1, activity.getValue("2026-11-20").completedUnits)
        assertEquals(3, DailyActivityRules.totalCompletedUnits(activity))
        assertEquals(2, DailyActivityRules.readingDays(activity))
    }

    @Test
    fun `activity keeps only the last 365 days and drops malformed keys`() {
        var activity = mapOf("bad" to DailyActivity(2))
        activity = DailyActivityRules.recordReadingDay(activity, "2025-01-01")
        activity = DailyActivityRules.recordReadingDay(activity, "2025-11-20")
        activity = DailyActivityRules.recordReadingDay(activity, "2026-11-19")

        val pruned = DailyActivityRules.prune(activity, "2026-11-19")
        assertFalse("bad" in pruned)
        assertFalse("2025-01-01" in pruned, "older than 365 days")
        assertTrue("2025-11-20" in pruned, "within 365 days")
        assertTrue("2026-11-19" in pruned)
        assertEquals(listOf("2025-11-20", "2026-11-19"), pruned.keys.toList())
    }

    @Test
    fun `merge takes the per-day maximum and unions the days`() {
        val local = mapOf(
            "2026-11-18" to DailyActivity(2),
            "2026-11-19" to DailyActivity(1),
        )
        val remote = mapOf(
            "2026-11-19" to DailyActivity(4),
            "2026-11-20" to DailyActivity(0),
        )
        val merged = DailyActivityRules.mergeDayMax(local, remote, "2026-11-19")
        assertEquals(2, merged.getValue("2026-11-18").completedUnits)
        assertEquals(4, merged.getValue("2026-11-19").completedUnits)
        assertTrue("2026-11-20" in merged)
        assertEquals(3, merged.size)
    }

    @Test
    fun `sanitize drops malformed dates and negative counts`() {
        val sanitized = DailyActivityRules.sanitize(
            mapOf(
                "2026-11-19" to DailyActivity(-3),
                "19-11-2026" to DailyActivity(2),
            ),
        )
        assertEquals(setOf("2026-11-19"), sanitized.keys)
        assertEquals(0, sanitized.getValue("2026-11-19").completedUnits)
    }

    @Test
    fun `reading seconds accumulate per day and ignore non-positive input`() {
        var activity = DailyActivityRules.recordReadingSeconds(emptyMap(), "2026-11-19", 90)
        activity = DailyActivityRules.recordReadingSeconds(activity, "2026-11-19", 30)
        activity = DailyActivityRules.recordReadingSeconds(activity, "2026-11-19", 0)
        activity = DailyActivityRules.recordReadingSeconds(activity, "2026-11-19", -60)

        assertEquals(120, activity.getValue("2026-11-19").readingSeconds)
        assertEquals(120, DailyActivityRules.totalReadingSeconds(activity))
    }

    @Test
    fun `a day of reading is capped instead of trusting the clock`() {
        var activity = emptyMap<String, DailyActivity>()
        repeat(6) {
            activity = DailyActivityRules.recordReadingSeconds(activity, "2026-11-19", 10_000)
        }

        assertEquals(
            ReadingStreakRules.MAX_SECONDS_PER_DAY,
            activity.getValue("2026-11-19").readingSeconds,
        )
    }

    @Test
    fun `merge keeps the larger reading time for the same day`() {
        val local = mapOf("2026-11-19" to DailyActivity(completedUnits = 1, readingSeconds = 600))
        val remote = mapOf("2026-11-19" to DailyActivity(completedUnits = 0, readingSeconds = 900))

        val merged = DailyActivityRules.mergeDayMax(local, remote, "2026-11-19")

        assertEquals(1, merged.getValue("2026-11-19").completedUnits)
        assertEquals(900, merged.getValue("2026-11-19").readingSeconds)
    }

    @Test
    fun `sanitize drops negative and oversized reading seconds`() {
        val sanitized = DailyActivityRules.sanitize(
            mapOf(
                "2026-11-19" to DailyActivity(readingSeconds = -90),
                "2026-11-20" to DailyActivity(readingSeconds = 999_999),
            ),
        )

        assertEquals(0, sanitized.getValue("2026-11-19").readingSeconds)
        assertEquals(
            ReadingStreakRules.MAX_SECONDS_PER_DAY,
            sanitized.getValue("2026-11-20").readingSeconds,
        )
    }
}
