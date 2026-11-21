package com.pagelence.core.progress

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Heat-map grid over the v2 `dailyActivity` log.
 *
 * The grid is display-only: no streaks, no reminders, no push. Dates are local
 * dates (the same `YYYY-MM-DD` keys the store writes), so the app must pass
 * "today" from the device clock; DST and time-zone changes cannot shift a cell
 * because a day is a plain local date, not an instant.
 *
 * Gray levels keep the map readable on e-ink:
 *   0 = no record, 1 = opened a book but finished nothing,
 *   2 = 1 unit, 3 = 2–3 units, 4 = 4+ units.
 */
object HeatmapRules {
    const val DAYS = 365
    const val LEVELS = 4

    data class Cell(
        val date: LocalDate,
        val units: Int,
        val level: Int,
    )

    data class Heatmap(
        /** Columns are weeks (Monday first); null marks padding outside the window. */
        val weeks: List<List<Cell?>>,
        val firstDate: LocalDate,
        val lastDate: LocalDate,
        val activeDays: Int,
        val totalUnits: Int,
    ) {
        val isEmpty: Boolean get() = activeDays == 0
    }

    fun level(units: Int, recorded: Boolean): Int = when {
        !recorded -> 0
        units <= 0 -> 1
        units == 1 -> 2
        units <= 3 -> 3
        else -> 4
    }

    fun build(activity: Map<String, DailyActivity>, today: LocalDate): Heatmap {
        val last = today
        val first = today.minusDays((DAYS - 1).toLong())
        val gridStart = first.with(DayOfWeek.MONDAY)
        val columns = (ChronoUnit.DAYS.between(gridStart, last) / 7 + 1).toInt()
        val weeks = mutableListOf<List<Cell?>>()
        var activeDays = 0
        var totalUnits = 0
        for (column in 0 until columns) {
            val week = mutableListOf<Cell?>()
            for (row in 0 until 7) {
                val date = gridStart.plusDays(column * 7L + row)
                if (date.isBefore(first) || date.isAfter(last)) {
                    week += null
                    continue
                }
                val entry = activity[date.toString()]
                val units = entry?.completedUnits ?: 0
                if (entry != null) {
                    activeDays += 1
                    totalUnits += units
                }
                week += Cell(date = date, units = units, level = level(units, entry != null))
            }
            weeks += week
        }
        return Heatmap(
            weeks = weeks,
            firstDate = first,
            lastDate = last,
            activeDays = activeDays,
            totalUnits = totalUnits,
        )
    }
}
