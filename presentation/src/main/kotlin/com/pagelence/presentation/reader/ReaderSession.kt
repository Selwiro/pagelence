package com.pagelence.presentation.reader

import com.pagelence.core.content.ContentIndex
import com.pagelence.core.progress.ProgressRepository
import com.pagelence.core.progress.ProgressSemantics
import com.pagelence.core.progress.ReaderPhase
import com.pagelence.core.progress.ReaderPosition
import com.pagelence.core.progress.ReadingProfileRepository

/**
 * Where the reader is, captured at the moment a save is scheduled.
 *
 * Page turns, phase switches and quiz answers all go through [ReaderSession];
 * the shell only supplies the values it just rendered.
 */
data class ResumePoint(
    val bookId: String,
    val unitId: String,
    val phase: ReaderPhase,
    val textPage: Int,
    val textProgress: Float,
    /** Visible page anchor; null keeps the paragraph already stored. */
    val paragraphId: String? = null,
    val answers: Map<Int, Int> = emptyMap(),
    val submitted: Boolean = false,
)

/** What one save did, so the shell can react without re-reading the state. */
data class SaveOutcome(
    val unitFinished: Boolean,
    /** The save completed the last unit, so the reader may show the ending page. */
    val bookFinished: Boolean,
    /** A full document write happened (unit finished); false for a position-only save. */
    val wroteFullDocument: Boolean,
)

/**
 * Android-free reading-session rules: turn a [ResumePoint] into persisted
 * progress, deciding between a page-turn save and finishing the unit.
 *
 * `:app-phone` delegates its save path here; an e-ink shell can reuse the same
 * class, and its behaviour is unit-tested on a plain JVM. Page rendering and
 * text mapping stay in the shells.
 */
class ReaderSession(
    private val progress: ProgressRepository,
    private val profile: ReadingProfileRepository,
    private val now: () -> String = { java.time.Instant.now().toString() },
    private val today: () -> String = { java.time.LocalDate.now().toString() },
) {
    /**
     * Persist [point] inside [index].
     *
     * [completedAt] is the completion timestamp for "this save finished the
     * unit", or null for a passive save (page turn / phase switch) that must not
     * stamp a finish. Returns whether the unit/book just finished.
     */
    fun save(point: ResumePoint, index: ContentIndex, completedAt: String? = null): SaveOutcome {
        val stored = progress.progress(point.bookId)
        val current = point.paragraphId
            ?.takeIf { it in index.paragraphById }
            ?.let { stored.copy(paragraphId = it) }
            ?: stored

        val previous = current.reader?.takeIf { it.unitId == point.unitId }
        val grammarReached = previous?.grammarReached == true || point.phase == ReaderPhase.GRAMMAR
        val position = ReaderPosition(
            unitId = point.unitId,
            phase = point.phase,
            textPage = point.textPage.coerceAtLeast(0),
            textProgress = point.textProgress.coerceIn(0f, 1f),
            quizAnswers = point.answers,
            quizSubmitted = point.submitted,
            grammarReached = grammarReached,
        )

        val wasRead = point.unitId in current.readUnitIds
        if (grammarReached && !wasRead) {
            val next = ProgressSemantics.markUnitRead(current, index, point.unitId, completedAt)
            progress.save(next.copy(reader = position))
            profile.recordCompletedUnit(today())
            return SaveOutcome(
                unitFinished = true,
                bookFinished = next.finishedAt != null && current.finishedAt == null,
                wroteFullDocument = true,
            )
        }

        progress.savePosition(point.bookId, point.paragraphId, position)
        return SaveOutcome(unitFinished = false, bookFinished = false, wroteFullDocument = false)
    }

    /** Completion timestamp for saves that mean "I finished this unit". */
    fun completionStamp(): String = now()
}
