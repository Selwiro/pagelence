package com.pagelence.presentation.review

import com.pagelence.core.wordcards.ReviewGrade
import com.pagelence.uim3.model.ReviewSessionUi
import com.pagelence.uim3.model.WordCardUi

/**
 * One review round, as pure transitions over [ReviewSessionUi].
 *
 * The shell owns the clock and the store: it turns a tap on 忘了/模糊/记得 into
 * `Srs.grade` + a save, then hands the graded card back here to move on. Keeping
 * this separate means the round's flow (reveal before grading, no skipping,
 * three counters) is testable without Android or a file.
 */
object ReviewSessionRules {
    /** Start a round over a queue snapshot; an empty queue is a finished round. */
    fun start(queue: List<WordCardUi>): ReviewSessionUi = ReviewSessionUi(cards = queue)

    /** Reveal the answer of the current card (idempotent). */
    fun reveal(session: ReviewSessionUi): ReviewSessionUi =
        if (session.revealed || session.current == null) session else session.copy(revealed = true)

    /**
     * Record one grade and advance. Grading before the reveal is ignored, so a
     * double tap or a stray gesture cannot mark a card the reader never saw.
     */
    fun grade(session: ReviewSessionUi, grade: ReviewGrade): ReviewSessionUi {
        if (session.current == null || !session.revealed) return session
        return session.copy(
            index = session.index + 1,
            revealed = false,
            remembered = session.remembered + if (grade == ReviewGrade.GOOD) 1 else 0,
            unsure = session.unsure + if (grade == ReviewGrade.HARD) 1 else 0,
            forgotten = session.forgotten + if (grade == ReviewGrade.AGAIN) 1 else 0,
        )
    }
}
