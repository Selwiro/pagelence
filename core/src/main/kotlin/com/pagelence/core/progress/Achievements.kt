package com.pagelence.core.progress

import com.pagelence.core.prefs.DailyGoal
import java.time.LocalDate

/**
 * The achievement catalogue (T163).
 *
 * Achievements are **derived, never stored**: every item is computed from the
 * state that already exists (finished books, read units, the daily log, word
 * cards, annotations), so importing a state on a new device shows the same wall,
 * nothing has to be merged, and a hand-edited file cannot "grant" a badge it did
 * not earn. The copy lives in the UI layer; this file only knows ids, thresholds
 * and the date each item was reached.
 */
enum class AchievementId {
    FIRST_BOOK,
    THREE_BOOKS,
    HUNDRED_UNITS,
    STREAK_3,
    STREAK_7,
    STREAK_30,
    CARDS_50,
    NOTES_20,
    READING_HOURS_10,
    LEVEL_B1,
}

/** One evaluated achievement: current progress against its threshold. */
data class AchievementProgress(
    val id: AchievementId,
    val current: Int,
    val target: Int,
    val unlocked: Boolean,
    /** Local date the threshold was crossed; null when it cannot be dated. */
    val unlockedOn: LocalDate? = null,
) {
    /** 0..1 for the progress bar on the wall. */
    val fraction: Float
        get() = if (target <= 0) 1f else (current.toFloat() / target).coerceIn(0f, 1f)
}

/** A book the reader actually finished, oldest finish first. */
data class FinishedBookFact(
    val finishedOn: LocalDate?,
    /** Book level from its manifest, e.g. `A2`; null when the package omits it. */
    val level: String?,
    /** Units in the book, so a unit-threshold crossing can be attributed to it. */
    val units: Int = 0,
)

/**
 * Everything the catalogue needs. The shell fills it from `:data`; tests fill it
 * directly, which is why none of it is a repository or a clock.
 */
data class AchievementInput(
    val finishedBooks: List<FinishedBookFact> = emptyList(),
    /** Units read across every book, including books still in progress. */
    val totalUnitsRead: Int = 0,
    val activity: Map<String, DailyActivity> = emptyMap(),
    val goalMinutes: Int = DailyGoal.DEFAULT_MINUTES,
    /** Local dates of the active word cards (created), unsorted. */
    val wordCardDates: List<LocalDate> = emptyList(),
    /** Local dates of the active bookmarks/highlights/notes, unsorted. */
    val annotationDates: List<LocalDate> = emptyList(),
)

object Achievements {
    const val HUNDRED_UNITS = 100
    const val CARDS = 50
    const val NOTES = 20
    const val READING_HOURS = 10
    const val READING_SECONDS = READING_HOURS * 60 * 60

    /** Display order: the order the wall reads in. */
    val CATALOG: List<AchievementId> = AchievementId.entries

    fun evaluate(input: AchievementInput, today: LocalDate): List<AchievementProgress> {
        val books = input.finishedBooks
        val streak = ReadingStreakRules.summarize(input.activity, input.goalMinutes, today)
        val cards = input.wordCardDates.sorted()
        val notes = input.annotationDates.sorted()

        return CATALOG.map { id ->
            when (id) {
                AchievementId.FIRST_BOOK -> progress(
                    id,
                    current = books.size,
                    target = 1,
                    unlockedOn = books.getOrNull(0)?.finishedOn,
                )

                AchievementId.THREE_BOOKS -> progress(
                    id,
                    current = books.size,
                    target = 3,
                    unlockedOn = books.getOrNull(2)?.finishedOn,
                )

                // Units are read across books and the log only keeps the last 365
                // days, so the crossing date is not recoverable; show progress only.
                AchievementId.HUNDRED_UNITS -> progress(
                    id,
                    current = input.totalUnitsRead,
                    target = HUNDRED_UNITS,
                    unlockedOn = null,
                )

                AchievementId.STREAK_3 -> progress(
                    id,
                    current = streak.longestDays,
                    target = 3,
                    unlockedOn = streak.longestRunEndsOn,
                )

                AchievementId.STREAK_7 -> progress(
                    id,
                    current = streak.longestDays,
                    target = 7,
                    unlockedOn = streak.longestRunEndsOn,
                )

                AchievementId.STREAK_30 -> progress(
                    id,
                    current = streak.longestDays,
                    target = 30,
                    unlockedOn = streak.longestRunEndsOn,
                )

                AchievementId.CARDS_50 -> progress(
                    id,
                    current = cards.size,
                    target = CARDS,
                    unlockedOn = cards.getOrNull(CARDS - 1),
                )

                AchievementId.NOTES_20 -> progress(
                    id,
                    current = notes.size,
                    target = NOTES,
                    unlockedOn = notes.getOrNull(NOTES - 1),
                )

                AchievementId.READING_HOURS_10 -> progress(
                    id,
                    current = readingSeconds(input.activity),
                    target = READING_SECONDS,
                    unlockedOn = readingCrossing(input.activity, READING_SECONDS),
                )

                AchievementId.LEVEL_B1 -> {
                    val b1 = books.filter { it.level?.uppercase() == "B1" }
                    progress(
                        id,
                        current = b1.size,
                        target = 1,
                        unlockedOn = b1.getOrNull(0)?.finishedOn,
                    )
                }
            }
        }
    }

    /** Unlocked first (most recent achievement first), then closest to unlocking. */
    fun ordered(items: List<AchievementProgress>, today: LocalDate): List<AchievementProgress> =
        items.sortedWith(
            compareByDescending<AchievementProgress> { it.unlocked }
                .thenByDescending { it.unlockedOn ?: LocalDate.MIN }
                .thenByDescending { it.fraction }
                .thenBy { it.id.ordinal },
        )

    /**
     * Achievements this book's completion plausibly just reached: the finish
     * count, the unit threshold it crossed, its level, or a streak that ended
     * today. Used only to highlight a row on the ending page.
     */
    fun reachedBy(
        input: AchievementInput,
        book: FinishedBookFact,
        today: LocalDate,
    ): Set<AchievementId> {
        val items = evaluate(input, today).associateBy { it.id }
        val reached = LinkedHashSet<AchievementId>()
        val finished = input.finishedBooks.size
        if (items.getValue(AchievementId.FIRST_BOOK).unlocked && finished == 1) {
            reached += AchievementId.FIRST_BOOK
        }
        if (items.getValue(AchievementId.THREE_BOOKS).unlocked && finished == 3) {
            reached += AchievementId.THREE_BOOKS
        }
        if (items.getValue(AchievementId.LEVEL_B1).unlocked && book.level?.uppercase() == "B1") {
            reached += AchievementId.LEVEL_B1
        }
        // Only when this book is what pushed the cumulative count over 100.
        val hundreds = items.getValue(AchievementId.HUNDRED_UNITS)
        if (hundreds.unlocked && input.totalUnitsRead - book.units < Achievements.HUNDRED_UNITS) {
            reached += AchievementId.HUNDRED_UNITS
        }
        for (id in listOf(AchievementId.STREAK_3, AchievementId.STREAK_7, AchievementId.STREAK_30)) {
            val item = items.getValue(id)
            if (item.unlocked && item.unlockedOn == today) reached += id
        }
        return reached
    }

    private fun progress(
        id: AchievementId,
        current: Int,
        target: Int,
        unlockedOn: LocalDate?,
    ): AchievementProgress {
        val safeCurrent = current.coerceAtLeast(0)
        val unlocked = safeCurrent >= target
        return AchievementProgress(
            id = id,
            current = safeCurrent,
            target = target,
            unlocked = unlocked,
            unlockedOn = unlockedOn.takeIf { unlocked },
        )
    }

    private fun readingSeconds(activity: Map<String, DailyActivity>): Int =
        DailyActivityRules.totalReadingSeconds(activity)

    /** The local day cumulative reading time first reached [target] seconds. */
    private fun readingCrossing(activity: Map<String, DailyActivity>, target: Int): LocalDate? {
        var total = 0
        for (date in activity.keys.sorted()) {
            val parsed = runCatching { LocalDate.parse(date) }.getOrNull() ?: continue
            total += activity.getValue(date).readingSeconds.coerceAtLeast(0)
            if (total >= target) return parsed
        }
        return null
    }
}
