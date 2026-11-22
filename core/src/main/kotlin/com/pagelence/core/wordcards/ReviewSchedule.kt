package com.pagelence.core.wordcards

import java.time.Instant
import java.time.LocalDate
import kotlin.math.ceil
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * How the reader rated a card after revealing the answer (T143).
 *
 * Three buttons instead of Anki's four: the product asks a learner of English,
 * not a power user of a flashcard app, and a fourth middle option only adds
 * hesitation. [HARD] is "模糊" — recalled with effort — and shortens the step.
 */
@Serializable
enum class ReviewGrade {
    @SerialName("again") AGAIN,
    @SerialName("hard") HARD,
    @SerialName("good") GOOD,
}

/**
 * Coarse bucket shown as a chip in the card list.
 *
 * Derived from the schedule rather than stored, so a merged or imported card can
 * never carry a status that contradicts its own interval:
 * - [NEW]: never reviewed (`reviews == 0`);
 * - [MASTERED]: the current interval is at least [Srs.MASTERED_INTERVAL_DAYS],
 *   i.e. the reader still remembered it after three weeks;
 * - [LEARNING]: everything else, including cards that keep being forgotten.
 */
enum class CardMastery { NEW, LEARNING, MASTERED }

/** Cards that need attention today, already split for the header line. */
data class ReviewQueue(
    /** Reviewed before and due today or earlier, oldest due date first. */
    val due: List<WordCard>,
    /** Never reviewed; collected but not scheduled yet. */
    val fresh: List<WordCard>,
) {
    /** The order a round runs in: overdue cards first, then new ones. */
    val cards: List<WordCard> get() = due + fresh

    val isEmpty: Boolean get() = due.isEmpty() && fresh.isEmpty()
}

/**
 * Simplified SM-2 with three grades, kept a pure function of the card, the
 * grade and `today`.
 *
 * Intervals: the first "remembered" review schedules 1 day, the second 3 days,
 * and every later one multiplies the previous interval by the card's ease.
 * [ReviewGrade.HARD] uses ×1.2 instead, so a card you barely recall comes back
 * much sooner than one you know. [ReviewGrade.AGAIN] resets the interval to 0 —
 * due again today — drops ease by 0.2 and counts a lapse.
 *
 * Ease only ever decreases (by 0.15 for HARD, 0.2 for AGAIN), matching the
 * "penalty only" reading of the task: a card that is always remembered keeps
 * 2.5 and its interval doubles-ish until the 365-day cap. The 1.3–2.8 range is
 * enforced on every read *and* write, so a hand-edited or imported ease can
 * never poison an interval.
 *
 * Dates are local calendar dates (`YYYY-MM-DD`), the same clock the daily
 * activity log uses: "due today" means the reader's today, not UTC. `today` is
 * injected so tests can move the calendar without touching the system clock.
 * `lastReviewedAt` / `updatedAt` stay ISO-8601 UTC instants, like every other
 * timestamp in `state.json`.
 */
object Srs {
    const val DEFAULT_EASE = 2.5
    const val MIN_EASE = 1.3
    const val MAX_EASE = 2.8
    const val MAX_INTERVAL_DAYS = 365

    /** An interval this long means the word survived three weeks unmasked. */
    const val MASTERED_INTERVAL_DAYS = 21

    private const val HARD_FACTOR = 1.2
    private const val AGAIN_PENALTY = 0.2
    private const val HARD_PENALTY = 0.15

    /**
     * Stored ease clamped into the supported range. The field defaults to
     * [DEFAULT_EASE], so a card written before T143 reads as a fresh card.
     */
    fun easeOf(card: WordCard): Double = card.ease.coerceIn(MIN_EASE, MAX_EASE)

    /** The stored due date, or null when the card has none or an unreadable one. */
    fun dueOn(card: WordCard): LocalDate? =
        card.dueAt?.takeIf { it.isNotBlank() }?.let { runCatching { LocalDate.parse(it) }.getOrNull() }

    /** True until the first review; older cards without the fields count as new. */
    fun isNew(card: WordCard): Boolean = card.reviews <= 0

    /**
     * True when the card belongs in today's queue: a new card, or one whose due
     * date is today or past. A missing due date counts as due, so cards written
     * before T143 are reviewed immediately instead of never.
     */
    fun isDue(card: WordCard, today: LocalDate): Boolean {
        if (card.isDeleted) return false
        if (isNew(card)) return true
        val due = dueOn(card) ?: return true
        return !due.isAfter(today)
    }

    /** Split [cards] into due and new, skipping tombstones and future dates. */
    fun queue(cards: List<WordCard>, today: LocalDate): ReviewQueue {
        val live = cards.filterNot { it.isDeleted }
        val fresh = live.filter { isNew(it) }
            .sortedWith(compareBy({ it.createdAt }, { it.term }))
        val due = live.filter { !isNew(it) && isDue(it, today) }
            .sortedWith(compareBy({ dueOn(it)?.toEpochDay() ?: Long.MIN_VALUE }, { it.term }))
        return ReviewQueue(due = due, fresh = fresh)
    }

    /**
     * Apply one grade. Returns a new card; the caller persists it.
     *
     * [now] is the ISO-8601 UTC instant written to `lastReviewedAt` /
     * `updatedAt`, [today] the local date the next due date is counted from.
     */
    fun grade(card: WordCard, rating: ReviewGrade, today: LocalDate, now: String): WordCard {
        val ease = adjustedEase(easeOf(card), rating)
        val interval = intervalFor(card, rating, ease)
        return card.copy(
            dueAt = today.plusDays(interval.toLong()).toString(),
            intervalDays = interval,
            ease = ease,
            reviews = card.reviews + 1,
            lapses = card.lapses + if (rating == ReviewGrade.AGAIN) 1 else 0,
            lastReviewedAt = now,
            lastGrade = rating,
            updatedAt = now,
        )
    }

    /** Mastery bucket for the list; see [CardMastery]. */
    fun mastery(card: WordCard): CardMastery = when {
        isNew(card) -> CardMastery.NEW
        card.intervalDays >= MASTERED_INTERVAL_DAYS -> CardMastery.MASTERED
        else -> CardMastery.LEARNING
    }

    /**
     * Overlay [other]'s schedule onto [base] when [other] was reviewed later.
     *
     * `state.json` merges cards by `updatedAt`, but the schedule has its own
     * clock: one device may grade a card while the other only re-collects it.
     * The side with the later `lastReviewedAt` owns every schedule field; the
     * side without one keeps the schedule it already had. Content fields
     * (term, meaning, example) are never touched here — the caller already
     * picked the newer card as [base].
     */
    fun mergeSchedule(base: WordCard, other: WordCard): WordCard {
        val mine = instant(base.lastReviewedAt)
        val theirs = instant(other.lastReviewedAt)
        val winner = when {
            theirs == null -> return base
            mine != null && !theirs.isAfter(mine) -> return base
            else -> other
        }
        return base.copy(
            dueAt = winner.dueAt,
            intervalDays = winner.intervalDays,
            ease = winner.ease,
            reviews = winner.reviews,
            lapses = winner.lapses,
            lastReviewedAt = winner.lastReviewedAt,
            lastGrade = winner.lastGrade,
        )
    }

    private fun adjustedEase(ease: Double, rating: ReviewGrade): Double {
        val next = when (rating) {
            ReviewGrade.AGAIN -> ease - AGAIN_PENALTY
            ReviewGrade.HARD -> ease - HARD_PENALTY
            ReviewGrade.GOOD -> ease
        }
        return next.coerceIn(MIN_EASE, MAX_EASE)
    }

    private fun intervalFor(card: WordCard, rating: ReviewGrade, ease: Double): Int {
        if (rating == ReviewGrade.AGAIN) return 0
        val previous = card.intervalDays.coerceAtLeast(0)
        val reviews = card.reviews.coerceAtLeast(0)
        return when (rating) {
            ReviewGrade.GOOD -> when (reviews) {
                0 -> 1
                1 -> 3
                else -> scaled(previous, ease)
            }

            else -> when (reviews) {
                0 -> 1
                else -> scaled(previous.coerceAtLeast(1), HARD_FACTOR)
            }
        }
    }

    /** Multiply and round up, so a hard step never stalls at the same interval. */
    private fun scaled(days: Int, factor: Double): Int =
        ceil(days * factor).toInt().coerceIn(1, MAX_INTERVAL_DAYS)

    private fun instant(value: String?): Instant? {
        if (value.isNullOrBlank()) return null
        return runCatching { Instant.parse(value) }.getOrNull()
    }
}
