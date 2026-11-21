package com.pagelence.core.reading

/**
 * The reader's temporary word selection state machine:
 *
 * ```
 * Hidden --tap word--> Shown(strip) --details--> Details(panel)
 *    ^                    ^                         |
 *    +--back/unit change--+-------close details-----+
 * ```
 *
 * Tapping another word replaces the selection and returns to `Shown`, so the
 * sheet does not linger over a different word. Page/section changes and leaving
 * the reader clear it. The UI maps the state to the top strip and sheet; the
 * rules are here so they can be tested without Compose.
 */
sealed interface WordSelectionState {
    data object Hidden : WordSelectionState

    data class Shown(val word: SelectedTerm) : WordSelectionState

    data class Details(val word: SelectedTerm) : WordSelectionState

    companion object {
        fun select(word: SelectedTerm): WordSelectionState = Shown(word)

        fun openDetails(state: WordSelectionState): WordSelectionState = when (state) {
            is Shown -> Details(state.word)
            is Details, Hidden -> state
        }

        fun closeDetails(state: WordSelectionState): WordSelectionState = when (state) {
            is Details -> Shown(state.word)
            is Shown, Hidden -> state
        }

        fun clear(): WordSelectionState = Hidden

        /** The word is still shown after closing the sheet. */
        fun word(state: WordSelectionState): SelectedTerm? = when (state) {
            is Shown -> state.word
            is Details -> state.word
            Hidden -> null
        }

        fun isDetails(state: WordSelectionState): Boolean = state is Details
    }
}

/** Everything the top strip needs; the sheet's payload stays in the shell. */
data class SelectedTerm(
    val paragraphId: String,
    val sentenceNumber: Int,
    val spelling: String,
    val term: String,
    val ipa: String? = null,
    val gloss: String? = null,
    /** Character range of the tapped occurrence inside the sentence. */
    val start: Int = -1,
    val end: Int = -1,
)
