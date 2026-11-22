package com.pagelence.presentation.pagination

import com.pagelence.core.prefs.FontSize
import com.pagelence.uim3.UiLanguage
import com.pagelence.uim3.model.ParagraphUi

/**
 * Every input that can change how one unit's text is paged (review item E-15).
 *
 * All fields are small values, so comparing two keys never walks the paragraph
 * text. That is the whole point: the reader recomposes on every page turn and
 * word tap, and the old `remember(state.paragraphs)` key deep-compared the
 * whole unit each time the state was rebuilt.
 *
 * [fingerprint] covers the text-related inputs; see [PaginationFingerprint].
 */
data class PaginationKey(
    val bookId: String,
    val unitId: String,
    val contentWidthPx: Int,
    val pageHeightPx: Int,
    val fontSize: FontSize,
    val language: UiLanguage,
    /** `Density.density`; measured dp sizes depend on it. */
    val density: Float,
    /** `Density.fontScale`; `sp` text sizes depend on it. */
    val fontScale: Float,
    val fingerprint: Long,
)

/**
 * 64-bit fingerprint of the fields that affect text layout: paragraph ids and
 * sentence text/translation/analysis. `String.hashCode` is cached by the JVM,
 * so one pass over a unit's sentences is a few hundred integer mixes.
 *
 * Deliberately excluded:
 *
 * - marks (bookmark/highlight/note): `ReaderText` draws them after the text as
 *   overlays, so they never reflow a page (T144). Including them would
 *   re-paginate the unit every time the reader adds a bookmark.
 * - quiz answers, cards, word panels: not part of the text at all.
 *
 * If a future sentence-end element becomes inline content (like the existing
 * translation/analysis icons), add it here.
 */
object PaginationFingerprint {
    // FNV-1a 64-bit offset basis and prime.
    private const val OFFSET = -3750763034362895579L
    private const val PRIME = 1099511628211L

    fun of(paragraphs: List<ParagraphUi>): Long {
        var hash = OFFSET
        paragraphs.forEach { paragraph ->
            hash = mix(hash, paragraph.id.hashCode().toLong())
            hash = mix(hash, paragraph.sentences.size.toLong())
            paragraph.sentences.forEach { sentence ->
                hash = mix(hash, sentence.number.toLong())
                hash = mix(hash, sentence.text.hashCode().toLong())
                hash = mix(hash, (sentence.zh?.hashCode() ?: 0).toLong())
                hash = mix(hash, (sentence.analysis?.parts?.size ?: -1).toLong())
            }
        }
        return hash
    }

    private fun mix(seed: Long, value: Long): Long = (seed xor value) * PRIME
}

/**
 * Small LRU cache for paged text, keyed by [PaginationKey].
 *
 * Keeps a handful of recent units so swiping back, or returning to a unit
 * after reading its quiz, reuses the measured pages instead of measuring again.
 * Generic over the page type so the ink screen shell can cache its own pages.
 */
class PaginationCache<V>(private val maxEntries: Int = 8) {
    init {
        require(maxEntries > 0) { "maxEntries must be positive" }
    }

    private val entries = object : LinkedHashMap<PaginationKey, V>(maxEntries, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<PaginationKey, V>): Boolean =
            size > maxEntries
    }

    val size: Int get() = entries.size

    @Synchronized
    fun get(key: PaginationKey): V? = entries[key]

    /** Returns the cached pages for [key], or runs [compute] once on a miss. */
    @Synchronized
    fun getOrPut(key: PaginationKey, compute: () -> V): V {
        entries[key]?.let { return it }
        val value = compute()
        entries[key] = value
        return value
    }

    @Synchronized
    fun clear() {
        entries.clear()
    }
}
