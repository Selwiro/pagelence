package com.pagelence.core.content

/**
 * The frozen sentence-splitting rule.
 *
 * The Python migration and validator (`work/android/tools/lib/content.py`) use
 * the same rule; a unit test re-splits every migrated paragraph and compares it
 * with the stored sentences, so the two implementations cannot drift.
 *
 * Rules:
 *  1. A boundary is `.`, `!` or `?` (plus an optional closing quote) followed
 *     by whitespace and an uppercase letter or digit, or the end of the text.
 *  2. A boundary after a single-letter initial (`J.`) is not a sentence end.
 *  3. Whatever follows the last boundary is the final sentence.
 *
 * Known limitation, frozen on purpose: a dotted abbreviation followed by a
 * capital (`Mr. Smith`) *is* treated as a sentence end. The existing Web
 * translations were authored against this behaviour, so changing the rule now
 * would invalidate them; content authoring must follow the rule as written.
 * A future rule version (with re-translation) can revisit it.
 */
object SentenceSplitter {
    private val BOUNDARY = Regex("[.!?][”’\"]?(?=\\s+(?:[“\"]?[A-Z0-9]))")
    private val TRAILING_DOT_TOKEN = Regex("([A-Za-z](?:[A-Za-z.]*)\\.)[”’\"]?$")
    private val INITIAL = Regex("[a-z]\\.")

    fun split(text: String): List<String> {
        val sentences = mutableListOf<String>()
        var start = 0
        for (match in BOUNDARY.findAll(text)) {
            val end = match.range.last + 1
            val before = text.substring(start, end).trimEnd()
            val tail = TRAILING_DOT_TOKEN.find(before)
            if (tail != null && INITIAL.matches(tail.groupValues[1].lowercase())) continue
            val sentence = text.substring(start, end).trim()
            if (sentence.isNotEmpty()) sentences += sentence
            start = end
        }
        val rest = text.substring(start).trim()
        if (rest.isNotEmpty()) sentences += rest
        return sentences
    }

    /** Normalised word count used by manifests and unit statistics. */
    fun words(text: String): Int = WORD.findAll(text).count()

    private val WORD = Regex("[A-Za-z]+(?:['’][A-Za-z]+)?")
}
