package com.pagelence.core.progress

import com.pagelence.core.prefs.DailyGoal
import java.time.LocalDate
import java.time.format.DateTimeParseException

/**
 * The "today" card on "我的": how long the reader has read today, whether the
 * daily goal is met, and the streak that follows from it (T162).
 *
 * Deliberately plain data — the screen formats it. Streaks never punish: a
 * missed day resets [currentDays] but keeps [longestDays] and [metDays], and
 * [restarting] lets the copy say "今天重新开始" instead of "断签".
 */
data class ReadingStreak(
    val goalMinutes: Int = DailyGoal.DEFAULT_MINUTES,
    val todaySeconds: Int = 0,
    val todayUnits: Int = 0,
    val metToday: Boolean = false,
    /** Seconds still needed today; 0 once the goal is met. */
    val secondsToGoal: Int = 0,
    /** Consecutive goal-met days ending today (or yesterday while today is open). */
    val currentDays: Int = 0,
    /** Longest run in the retained window; kept after a missed day. */
    val longestDays: Int = 0,
    /** Last day of the longest run, so an achievement can date itself. */
    val longestRunEndsOn: LocalDate? = null,
    val metDays: Int = 0,
) {
    val goalSeconds: Int get() = DailyGoal.secondsOf(goalMinutes)

    val hasHistory: Boolean get() = metDays > 0

    /** Had a goal streak before, has not read enough today: the gentle copy path. */
    val restarting: Boolean get() = !metToday && currentDays == 0 && longestDays > 0
}

/**
 * Turns the v2 `dailyActivity` log into a [ReadingStreak].
 *
 * A day counts as met when its `readingSeconds` reaches the goal. Days recorded
 * **before** reading time existed have no seconds but may have finished units;
 * those count as met once, so updating the app does not wipe an existing streak.
 * A day with seconds but not enough does not fall back to the unit rule — that
 * would make the goal meaningless (`1 单元` ≈ 5 分钟).
 *
 * Dates after [today] are ignored (a hand-edited file or a device clock jump).
 */
object ReadingStreakRules {
    /** A day is capped at 12 hours; a stuck clock cannot inflate the log. */
    const val MAX_SECONDS_PER_DAY = 12 * 60 * 60

    fun isGoalMet(day: DailyActivity?, goalMinutes: Int): Boolean {
        if (day == null) return false
        val seconds = day.readingSeconds.coerceIn(0, MAX_SECONDS_PER_DAY)
        if (seconds <= 0) return day.completedUnits > 0
        return seconds >= DailyGoal.secondsOf(goalMinutes)
    }

    fun summarize(
        activity: Map<String, DailyActivity>,
        goalMinutes: Int,
        today: LocalDate,
    ): ReadingStreak {
        val goal = DailyGoal.sanitize(goalMinutes)
        val goalSeconds = DailyGoal.secondsOf(goal)
        val todayKey = today.toString()

        val todayEntry = activity[todayKey]
        val todaySeconds = (todayEntry?.readingSeconds ?: 0).coerceIn(0, MAX_SECONDS_PER_DAY)
        val todayUnits = (todayEntry?.completedUnits ?: 0).coerceAtLeast(0)
        val metToday = isGoalMet(todayEntry, goal)

        val metDates = activity.keys
            .mapNotNull { key -> parse(key)?.takeIf { !it.isAfter(today) } }
            .filter { date -> isGoalMet(activity[date.toString()], goal) }
            .toSortedSet()

        val current = if (metToday) {
            daysEndingAt(metDates, today)
        } else {
            daysEndingAt(metDates, today.minusDays(1))
        }

        val longest = longestRun(metDates)

        return ReadingStreak(
            goalMinutes = goal,
            todaySeconds = todaySeconds,
            todayUnits = todayUnits,
            metToday = metToday,
            secondsToGoal = if (metToday) 0 else (goalSeconds - todaySeconds).coerceAtLeast(0),
            currentDays = current,
            longestDays = longest.days,
            longestRunEndsOn = longest.endsOn,
            metDays = metDates.size,
        )
    }

    private fun daysEndingAt(met: Set<LocalDate>, anchor: LocalDate): Int {
        var days = 0
        var date = anchor
        while (date in met) {
            days += 1
            date = date.minusDays(1)
        }
        return days
    }

    /** Longest run and the day it ended; ties keep the earliest run. */
    private fun longestRun(met: Set<LocalDate>): Run {
        var best = Run(0, null)
        var length = 0
        var previous: LocalDate? = null
        for (date in met) {
            length = if (previous != null && previous.plusDays(1) == date) length + 1 else 1
            if (length > best.days) best = Run(length, date)
            previous = date
        }
        return best
    }

    private data class Run(val days: Int, val endsOn: LocalDate?)

    private fun parse(key: String): LocalDate? = try {
        LocalDate.parse(key)
    } catch (_: DateTimeParseException) {
        null
    }
}
