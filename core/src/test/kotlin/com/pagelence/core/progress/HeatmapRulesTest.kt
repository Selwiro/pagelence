package com.pagelence.core.progress

import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class HeatmapRulesTest {
    private fun day(units: Int) = DailyActivity(completedUnits = units)

    @Test
    fun `grid ends today and spans 365 days`() {
        val today = LocalDate.parse("2026-11-19")
        val map = HeatmapRules.build(emptyMap(), today)
        assertEquals(today, map.lastDate)
        assertEquals(today.minusDays(364), map.firstDate)
        assertEquals(1, map.weeks.last().count { it?.date == today })
        assertTrue(map.isEmpty)
        // 365 days across 53 or 54 week columns, each 7 rows.
        assertTrue(map.weeks.size in 53..54)
        assertTrue(map.weeks.all { it.size == 7 })
    }

    @Test
    fun `cells follow local dates and levels`() {
        val today = LocalDate.parse("2026-11-19")
        val activity = mapOf(
            "2026-11-19" to day(2),
            "2026-11-18" to day(0),
            "2026-11-17" to day(1),
            "2026-11-16" to day(5),
        )
        val map = HeatmapRules.build(activity, today)
        assertEquals(4, map.activeDays)
        assertEquals(8, map.totalUnits)
        val cells = map.weeks.flatten().filterNotNull().associateBy { it.date.toString() }
        assertEquals(3, cells.getValue("2026-11-19").level)
        assertEquals(1, cells.getValue("2026-11-18").level)
        assertEquals(2, cells.getValue("2026-11-17").level)
        assertEquals(4, cells.getValue("2026-11-16").level)
        assertEquals(0, cells.getValue("2026-11-15").level)
    }

    @Test
    fun `cells start on monday and pad outside the window`() {
        val today = LocalDate.parse("2026-11-19") // Thursday
        val map = HeatmapRules.build(emptyMap(), today)
        val firstCell = map.weeks.first().firstOrNull { it != null }
        assertEquals(map.firstDate, firstCell?.date)
        val leading = map.weeks.first().takeWhile { it == null }
        assertEquals(map.firstDate.dayOfWeek.value - 1, leading.size)
        // The final week may end before Sunday, so trailing rows are null.
        val trailing = map.weeks.last().takeLastWhile { it == null }
        assertEquals(7 - today.dayOfWeek.value, trailing.size)
    }

    @Test
    fun `window crosses the year boundary and leap day`() {
        val today = LocalDate.parse("2028-03-01")
        val map = HeatmapRules.build(
            mapOf("2028-02-29" to day(3), "2027-12-31" to day(1)),
            today,
        )
        val cells = map.weeks.flatten().filterNotNull().associateBy { it.date }
        assertEquals(3, cells.getValue(LocalDate.parse("2028-02-29")).level)
        assertEquals(2, cells.getValue(LocalDate.parse("2027-12-31")).level)
        assertEquals(LocalDate.parse("2028-03-01"), map.lastDate)
        assertEquals(LocalDate.parse("2027-03-03"), map.firstDate)
    }

    @Test
    fun `days outside the window are dropped`() {
        val today = LocalDate.parse("2026-11-19")
        val map = HeatmapRules.build(
            mapOf("2025-01-01" to day(9), "2030-01-01" to day(2)),
            today,
        )
        assertTrue(map.isEmpty)
        assertTrue(map.weeks.flatten().filterNotNull().none { it.units > 0 })
    }

    @Test
    fun `merge uses the per-day maximum`() {
        val local = mapOf("2026-11-18" to day(2), "2026-11-19" to day(1))
        val remote = mapOf("2026-11-18" to day(3), "2026-11-20" to day(4))
        val merged = DailyActivityRules.mergeDayMax(local, remote, "2026-11-19")
        assertEquals(3, merged.getValue("2026-11-18").completedUnits)
        assertEquals(1, merged.getValue("2026-11-19").completedUnits)
        assertEquals(4, merged.getValue("2026-11-20").completedUnits)
    }

    @Test
    fun `levels are independent of color`() {
        assertEquals(0, HeatmapRules.level(0, recorded = false))
        assertEquals(1, HeatmapRules.level(0, recorded = true))
        assertEquals(2, HeatmapRules.level(1, recorded = true))
        assertEquals(3, HeatmapRules.level(2, recorded = true))
        assertEquals(3, HeatmapRules.level(3, recorded = true))
        assertEquals(4, HeatmapRules.level(4, recorded = true))
        assertEquals(4, HeatmapRules.level(40, recorded = true))
    }

    @Test
    fun `a single padded column has no leading cells`() {
        val today = LocalDate.parse("2026-11-16") // Monday
        val map = HeatmapRules.build(emptyMap(), today)
        assertEquals(today, map.weeks.last().first()?.date)
        assertNull(map.weeks.last().getOrNull(1))
    }
}
