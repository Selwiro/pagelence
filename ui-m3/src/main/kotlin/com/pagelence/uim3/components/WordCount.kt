package com.pagelence.uim3.components

import com.pagelence.uim3.UiLanguage
import java.util.Locale

/** A word count split the way the book page prints it: big value, small unit. */
data class WordCountParts(
    val value: String,
    val label: String,
    /** One-line form for the info row; empty when the book has no word count. */
    val text: String,
)

/**
 * Book word counts for both interface languages.
 *
 * Chinese uses 万 (`2.5 万词`); English uses thousands (`25k words`) and a
 * comma-separated exact count for short books, so the interface never prints a
 * Chinese unit next to English copy.
 */
fun wordCountParts(words: Int, language: UiLanguage): WordCountParts = when {
    words >= 10_000 && language == UiLanguage.ZH -> {
        val wan = Math.round(words / 1_000.0).toDouble() / 10.0
        val value = if (wan == Math.floor(wan)) {
            wan.toInt().toString()
        } else {
            String.format(Locale.US, "%.1f", wan)
        }
        WordCountParts(value, "万词", "$value 万词")
    }

    words >= 10_000 -> {
        val thousands = Math.round(words / 1_000.0).toInt()
        WordCountParts(thousands.toString(), "k words", "${thousands}k words")
    }

    words > 0 && language == UiLanguage.ZH -> {
        WordCountParts(words.toString(), "词", "$words 词")
    }

    words > 0 -> {
        val formatted = String.format(Locale.US, "%,d", words)
        WordCountParts(formatted, "words", "$formatted words")
    }

    else -> WordCountParts(
        value = "—",
        label = if (language == UiLanguage.ZH) "词汇量" else "Word count",
        text = "",
    )
}

/** One-line form used by the info page; empty when there is no count. */
fun wordCountText(words: Int, language: UiLanguage): String = wordCountParts(words, language).text
