package com.pagelence.presentation.reader

import com.pagelence.core.reading.SentencePage
import com.pagelence.uim3.model.ReaderUiState

/**
 * One page in the reading sequence: guide → text → quiz → summary → grammar →
 * review, with bridge pages at the open edges of a unit.
 */
sealed interface ReaderPage {
    /** Transient page before the guide; swiping back opens the previous unit's grammar. */
    data object PreviousUnit : ReaderPage
    data object Guide : ReaderPage
    data class Text(val page: SentencePage) : ReaderPage
    data object TextScroll : ReaderPage
    data class Quiz(val index: Int) : ReaderPage
    data object Grammar : ReaderPage
    /** Transient page after the grammar; swiping on opens the next unit's guide. */
    data object NextUnit : ReaderPage
}

/**
 * Android-free page order for the reader.
 *
 * The shell measures sentence heights (it owns text measurement) and passes
 * [textPages]; this object only decides the sequence, so both the phone and an
 * e-ink shell build the same page list.
 */
object ReaderFlow {
    fun pages(
        state: ReaderUiState,
        textPages: List<SentencePage>,
        hasGuide: Boolean,
        paged: Boolean,
    ): List<ReaderPage> = buildList {
        // Paged layout gets one bridge page at each open edge: before the guide
        // when a previous unit exists, after the grammar when a next unit exists.
        if (paged && state.hasPreviousUnit) add(ReaderPage.PreviousUnit)
        if (hasGuide) add(ReaderPage.Guide)
        if (paged) {
            textPages.forEach { add(ReaderPage.Text(it)) }
        } else {
            add(ReaderPage.TextScroll)
        }
        state.quiz.indices.forEach { add(ReaderPage.Quiz(it)) }
        if (state.grammar != null) add(ReaderPage.Grammar)
        if (paged && state.hasNextUnit) add(ReaderPage.NextUnit)
    }
}
