package com.pagelence.core.prefs

/**
 * The daily reading goal: how many minutes the reader wants to spend in a book
 * each day (T162).
 *
 * Minutes, not units: a unit is roughly five minutes but not exactly, and the
 * reader actually experiences "读了一会儿". The goal is a nudge, never a debt —
 * a missed day resets the current streak but keeps the best one, and nothing is
 * ever locked away (see [com.pagelence.core.progress.ReadingStreakRules]).
 *
 * Only the values in [OPTIONS] are accepted; anything else (a hand-edited file,
 * a newer build's custom value) falls back to [DEFAULT_MINUTES] instead of
 * failing the load.
 */
object DailyGoal {
    const val DEFAULT_MINUTES = 10

    /** What the settings screen offers, smallest first. */
    val OPTIONS = listOf(5, 10, 15, 20, 30)

    fun sanitize(minutes: Int?): Int =
        if (minutes != null && minutes in OPTIONS) minutes else DEFAULT_MINUTES

    fun secondsOf(minutes: Int): Int = sanitize(minutes) * 60
}
