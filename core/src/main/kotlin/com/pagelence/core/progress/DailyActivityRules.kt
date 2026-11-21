package com.pagelence.core.progress

import java.time.LocalDate
import java.time.format.DateTimeParseException

/**
 * Rules for the v2 daily activity log.
 *
 * A day is present once the reader opened a book (reading day) and carries the
 * number of units finished that day. Only the last [RETAIN_DAYS] local dates are
 * kept; malformed keys are dropped instead of failing a load.
 *
 * Sync merge (T069): take the per-day maximum of `completedUnits` and union the
 * keys. Both devices add to the same local day, so summing would double count;
 * buckets by device would show the same day twice in the heat map. The max rule
 * is intentionally lossy when one device reads more on the same day after the
 * other synced, which is acceptable for a "did I read today" display and keeps
 * the merge conflict-free.
 */
object DailyActivityRules {
    const val RETAIN_DAYS = 365

    fun recordReadingDay(
        activity: Map<String, DailyActivity>,
        date: String,
    ): Map<String, DailyActivity> {
        if (date in activity) return activity
        return prune(activity + (date to DailyActivity()), date)
    }

    fun recordCompletedUnit(
        activity: Map<String, DailyActivity>,
        date: String,
    ): Map<String, DailyActivity> {
        val current = activity[date] ?: DailyActivity()
        return prune(
            activity + (date to current.copy(completedUnits = current.completedUnits + 1)),
            date,
        )
    }

    fun readingDays(activity: Map<String, DailyActivity>): Int =
        activity.keys.count { parse(it) != null }

    fun totalCompletedUnits(activity: Map<String, DailyActivity>): Int =
        activity.values.sumOf { it.completedUnits }

    /** Drop malformed keys and sort the remaining dates; used when reading state. */
    fun sanitize(activity: Map<String, DailyActivity>): Map<String, DailyActivity> =
        activity.entries
            .mapNotNull { (key, value) ->
                if (parse(key) == null) null else key to value.copy(completedUnits = value.completedUnits.coerceAtLeast(0))
            }
            .sortedBy { it.first }
            .toMap()

    /** Drop malformed keys and dates older than [RETAIN_DAYS] before [today]. */
    fun prune(
        activity: Map<String, DailyActivity>,
        today: String,
    ): Map<String, DailyActivity> {
        val todayDate = parse(today)
        if (todayDate == null) return activity.filterKeys { parse(it) != null }
        val oldest = todayDate.minusDays((RETAIN_DAYS - 1).toLong())
        return activity.entries
            .mapNotNull { (key, value) ->
                val date = parse(key) ?: return@mapNotNull null
                if (date.isBefore(oldest)) null else key to value
            }
            .sortedBy { it.first }
            .toMap()
    }

    /** Conflict-free merge used by WebDAV/local import: per-day max, union of keys. */
    fun mergeDayMax(
        local: Map<String, DailyActivity>,
        remote: Map<String, DailyActivity>,
        today: String,
    ): Map<String, DailyActivity> {
        val keys = local.keys + remote.keys
        val merged = keys.associateWith { key ->
            val a = local[key]?.completedUnits ?: 0
            val b = remote[key]?.completedUnits ?: 0
            DailyActivity(completedUnits = maxOf(a, b))
        }
        return prune(merged, today)
    }

    private fun parse(date: String): LocalDate? = try {
        LocalDate.parse(date)
    } catch (_: DateTimeParseException) {
        null
    }
}
