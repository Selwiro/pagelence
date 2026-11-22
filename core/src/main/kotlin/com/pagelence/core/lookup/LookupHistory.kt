package com.pagelence.core.lookup

import kotlinx.serialization.Serializable

/**
 * One word the reader looked up, with everything needed to reopen it (T161).
 *
 * [gloss] and [ipa] are a snapshot for the history list: the dictionary may be
 * updated or the book removed, but the row still reads sensibly. The position
 * fields let a tap jump back to the exact sentence and reopen the panel.
 */
@Serializable
data class LookupEntry(
    val id: String,
    val bookId: String,
    val unitId: String,
    val paragraphId: String,
    /** 1-based sentence number inside the unit. */
    val sentenceNumber: Int,
    val term: String,
    val lemma: String,
    /** Character range of the tapped word inside the sentence. */
    val start: Int = 0,
    val end: Int = 0,
    val gloss: String? = null,
    val ipa: String? = null,
    val lookedUpAt: String,
) {
    val isUsable: Boolean
        get() = id.isNotBlank() && bookId.isNotBlank() && unitId.isNotBlank() &&
            term.isNotBlank() && sentenceNumber > 0
}

/**
 * Rules for the lookup history: one row per word per book, newest first, capped
 * so a long reading life cannot grow `state.json` without bound.
 *
 * Re-looking up a word refreshes its timestamp and position instead of adding a
 * duplicate row; that is what makes the list read like "recent words" rather
 * than a log of every tap.
 */
object LookupHistoryRules {
    const val MAX_ENTRIES = 200

    fun idOf(bookId: String, lemma: String, term: String): String {
        val key = lemmaKey(lemma.ifBlank { term })
        return "$bookId::$key"
    }

    fun lemmaKey(value: String): String =
        value.trim().lowercase().replace(Regex("\\s+"), " ")

    /** Put [entry] at the top, replacing an earlier lookup of the same word. */
    fun record(
        current: List<LookupEntry>,
        entry: LookupEntry,
        max: Int = MAX_ENTRIES,
    ): List<LookupEntry> {
        if (!entry.isUsable || max <= 0) return sanitize(current, max)
        val rest = current.filterNot { it.id == entry.id }
        return sanitize(listOf(entry) + rest, max)
    }

    /**
     * Drop unusable rows, collapse duplicates by id (newest wins), sort newest
     * first and keep at most [max]. Used on load and after a sync merge.
     */
    fun sanitize(current: List<LookupEntry>, max: Int = MAX_ENTRIES): List<LookupEntry> {
        if (max <= 0) return emptyList()
        val best = LinkedHashMap<String, LookupEntry>()
        current.forEach { entry ->
            if (!entry.isUsable) return@forEach
            val existing = best[entry.id]
            if (existing == null || isLater(entry.lookedUpAt, existing.lookedUpAt)) {
                best[entry.id] = entry
            }
        }
        return best.values
            // Compare parsed instants, not strings: an imported document may
            // carry an offset (+08:00) that would sort incorrectly as text.
            .sortedWith(compareByDescending<LookupEntry> { instant(it.lookedUpAt) })
            .take(max)
    }

    /** Union by id; the later lookup wins, then newest first and capped. */
    fun merge(
        local: List<LookupEntry>,
        remote: List<LookupEntry>,
        max: Int = MAX_ENTRIES,
    ): List<LookupEntry> = sanitize(local + remote, max)

    fun clear(): List<LookupEntry> = emptyList()

    private fun isLater(candidate: String, current: String): Boolean {
        val a = instant(candidate) ?: return false
        val b = instant(current) ?: return true
        return a.isAfter(b)
    }

    private fun instant(value: String): java.time.Instant? =
        runCatching { java.time.Instant.parse(value) }.getOrNull()
        // Instant.parse handles offsets; fall back to 1970 for missing timestamps.
        ?: java.time.Instant.EPOCH
}

/** Local storage contract; implemented by `:data` over `state.json`. */
interface LookupHistoryRepository {
    fun all(): List<LookupEntry>

    fun record(entry: LookupEntry): LookupEntry

    fun clear()
}
