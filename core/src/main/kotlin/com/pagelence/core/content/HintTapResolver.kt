package com.pagelence.core.content

/** A tap that lands inside a book hint; [start] and [end] are inclusive. */
data class HintTap(
    val target: String,
    /** Character range of [target] inside the sentence. */
    val start: Int,
    val end: Int,
    val hint: Hint,
)

/**
 * Promotes a word-level tap to the whole hint under the finger.
 *
 * The reader draws one dashed underline per `target`, but its tap handler can
 * only report a single word. Without this promotion a phrase such as
 * `by sight` opens `by` or `sight` and the book's explanation for the phrase is
 * unreachable. Matching therefore uses `target`, not `kind`: multi-word proper
 * names are stored as `kind: "word"` too.
 *
 * Hints with a [Hint.sentence] only apply to that sentence; a null sentence
 * applies to the whole paragraph. The longest target wins so a phrase beats a
 * word nested inside it; an exact-sentence hint wins over a paragraph-level one
 * when the targets are the same length.
 */
object HintTapResolver {
    /**
     * @param sentenceNumber 1-based sentence number inside the unit, the same
     *   index [Hint.sentence] and [QuizItem.sentence] use.
     * @param wordStart first character of the tapped word inside [sentenceText].
     * @param wordEnd last character of the tapped word inside [sentenceText].
     */
    fun resolve(
        sentenceText: String,
        sentenceNumber: Int,
        hints: List<Hint>,
        wordStart: Int,
        wordEnd: Int,
    ): HintTap? {
        if (sentenceText.isBlank()) return null
        if (wordStart < 0 || wordEnd < wordStart || wordEnd > sentenceText.lastIndex) return null

        return hints.asSequence()
            .filter { it.kind != "sentence" }
            .filter { it.sentence == null || it.sentence == sentenceNumber }
            .flatMap { hint ->
                occurrences(sentenceText, hint.target)
                    .filter { it.first <= wordStart && wordEnd <= it.last }
                    .asSequence()
                    .map { range -> HintTap(hint.target, range.first, range.last, hint) }
            }
            .sortedWith(HINT_ORDER)
            .firstOrNull()
    }

    private val HINT_ORDER: Comparator<HintTap> =
        compareByDescending<HintTap> { it.target.length }
            .thenByDescending { it.hint.sentence != null }
            .thenBy { it.start }
            .thenBy { it.hint.id }

    private fun occurrences(text: String, target: String): List<IntRange> {
        if (target.isBlank()) return emptyList()
        val pattern = Regex(
            "(?<![\\p{L}])" + Regex.escape(target) + "(?![\\p{L}])",
            RegexOption.IGNORE_CASE,
        )
        return pattern.findAll(text).map { it.range }.toList()
    }
}
