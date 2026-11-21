package com.pagelence.core.lexicon

import com.pagelence.core.content.Book
import com.pagelence.core.content.ContentIndex
import com.pagelence.core.content.PagelenceJson
import com.pagelence.core.wordcards.WordCardRepository
import kotlinx.serialization.Serializable

/**
 * Read-only word tables shipped with the app (five-layer CEFR levels, the
 * wordfreq table, per-book occurrence counts scanned at runtime) plus the
 * offline dictionary. None of them is part of the sync document.
 */

@Serializable
data class WordLevelTable(
    val levels: Map<String, Map<String, String>> = emptyMap(),
    /** Per slot provenance: `list` / `aligned` / `derived` / `tag` / `freq`. */
    val sources: Map<String, Map<String, String>> = emptyMap(),
) {
    fun sourceOf(word: String, pos: String): String? = sources[word]?.get(pos)
}

@Serializable
data class FrequencyBands(
    val high: Double = 4.5,
    val common: Double = 3.5,
    val normal: Double = 2.5,
)

enum class FrequencyBand { HIGH, COMMON, NORMAL, LOW }

/** Global lemma → zipf table from wordfreq 3.0.2; bands cut at build time. */
@Serializable
data class WordFrequencyTable(
    val bands: FrequencyBands = FrequencyBands(),
    val entries: Map<String, Double> = emptyMap(),
) {
    fun bandOf(zipf: Double): FrequencyBand = when {
        zipf >= bands.high -> FrequencyBand.HIGH
        zipf >= bands.common -> FrequencyBand.COMMON
        zipf >= bands.normal -> FrequencyBand.NORMAL
        else -> FrequencyBand.LOW
    }
}

/** Word counts for one book, produced by scanning its text once on open. */
data class BookOccurrences(val counts: Map<String, Int> = emptyMap())

@Serializable
data class DictionaryEntry(
    val phonetic: String? = null,
    /** Chinese senses, each `[part of speech, definition]`. */
    val senses: List<List<String>> = emptyList(),
    /** English senses, each `[part of speech, definition]`. */
    val en: List<List<String>> = emptyList(),
    /** Base form for an inflected entry (`peeped` → `peep`), when known. */
    val lemma: String? = null,
    /** ECDICT `exchange` `1:` marker (`s`, `s3`, `pd`, `r`, …). */
    val form: String? = null,
    /** `name` / `place` for an `extra.json` proper noun; null for ECDICT rows. */
    val kind: String? = null,
    /** Why an `extra.json` entry exists; not shown as a dictionary sense. */
    val note: String? = null,
)

@Serializable
data class DictionaryFile(
    val id: String? = null,
    val termCount: Int = 0,
    val entries: Map<String, DictionaryEntry> = emptyMap(),
)

/** One lookup result: everything the tap-a-word panel needs. */
data class WordLookup(
    val term: String,
    val lemma: String,
    val pos: String? = null,
    val ipa: String? = null,
    /** Contextual meaning from the book, if any. */
    val glossZh: String? = null,
    /** CEFR level, e.g. `B1`. */
    val level: String? = null,
    /** True when the level was inferred (`aligned` / `derived` / `tag` / `freq`). */
    val levelApprox: Boolean = false,
    /** `list` / `aligned` / `derived` / `tag` / `freq`; null when no level. */
    val levelSource: String? = null,
    /** Zipf frequency from the app-wide wordfreq table. */
    val zipf: Double? = null,
    /** High / common / normal / low, from the shipped cut points. */
    val frequencyBand: FrequencyBand? = null,
    /** How many times the term appears in this book. */
    val bookOccurrences: Int? = null,
    /** Senses from the one ECDICT table, Chinese and English. */
    val definitions: List<DictionarySense> = emptyList(),
    /** `人名` / `地名` / `专名` / `未收录释义`, when there is no dictionary entry. */
    val label: String? = null,
    /** One line about a contraction or compound, e.g. 「缩合词：she + will」. */
    val note: String? = null,
    val inWordCards: Boolean = false,
)

data class DictionarySense(
    val pos: String?,
    val definition: String,
    /** `zh` or `en`: which column of the entry this sense came from. */
    val language: String = "zh",
    /** True for a meaning that belongs to the inflected form, not its lemma. */
    val fromSurface: Boolean = false,
)

/**
 * Lookup order, frozen with the content schema:
 *
 * 1. the sentence `hint` whose `sentence` matches the tap (book context),
 * 2. any paragraph `hint` for the same target (curated explanation),
 * 3. the word-form → lemma link shipped in ECDICT (plus a small rule fallback
 *    for forms the table does not contain),
 * 4. the CEFR level table (CEFR-J, then Octanove for C1/C2),
 * 5. wordfreq's per-paragraph frequency,
 * 6. how often the term occurs in this book,
 * 7. ECDICT — looked up by lemma, so `dogs` reads `dog`; contractions and
 *    hyphenated compounds are split only when every part resolves.
 */
class WordQuery(
    private val index: ContentIndex,
    private val levels: WordLevelTable = WordLevelTable(),
    private val frequencies: WordFrequencyTable? = null,
    private val occurrences: BookOccurrences? = null,
    private val ecdict: DictionaryFile? = null,
    private val cards: WordCardRepository? = null,
) {
    private val bookId: String = index.book.id

    /**
     * @param sentenceIndex 1-based sentence number inside the unit, the same
     *   index [com.pagelence.core.content.QuizItem.sentence] uses.
     */
    fun lookup(
        unitId: String,
        paragraphId: String,
        sentenceIndex: Int,
        spelling: String,
    ): WordLookup? {
        val unit = index.unit(unitId) ?: return null
        val paragraph = unit.paragraphs.firstOrNull { it.id == paragraphId } ?: return null
        val sentence = index.sentence(unitId, sentenceIndex) ?: return null
        if (sentence.text.isBlank()) return null

        val surface = spelling.trim()
        if (surface.isEmpty()) return null
        if (!sentence.text.contains(surface, ignoreCase = true)) return null
        val normalized = normalize(surface)

        val matches = paragraph.hints.filter { normalize(it.target) == normalized }
        val hint = matches.firstOrNull { it.sentence == sentenceIndex }
            ?: matches.firstOrNull { it.sentence == null }

        var lemma = hint?.lemma?.takeIf { it.isNotBlank() }
            ?: resolveLemma(surface)
            ?: normalizeSurface(surface)
        val pos = hint?.pos
        var definitions = definitionsFor(lemma, surface, pos)
        var label: String? = null
        var note: String? = null
        if (definitions.isEmpty()) {
            val fallback = fallbackLookup(surface, pos)
            if (fallback != null) {
                lemma = fallback.lemma
                definitions = fallback.definitions
                label = fallback.label
                note = fallback.note
            }
        }
        if (definitions.isEmpty() && label == null) {
            label = if (surface.firstOrNull()?.isUpperCase() == true) "专名" else "未收录释义"
        }
        val level = levelOf(lemma, pos)
        val zipf = frequencyOf(normalized, normalizeSurface(lemma))
        val card = cards?.find(bookId, lemma, surface)

        return WordLookup(
            term = surface,
            lemma = lemma,
            pos = pos,
            ipa = hint?.ipa ?: phoneticOf(lemma, surface),
            glossZh = hint?.zh,
            level = level?.level,
            levelApprox = level?.approx ?: false,
            levelSource = level?.source,
            zipf = zipf,
            frequencyBand = zipf?.let { frequencies?.bandOf(it) },
            bookOccurrences = occurrencesOf(normalized, normalizeSurface(surface)),
            definitions = definitions,
            label = label,
            note = note,
            inWordCards = card != null,
        )
    }

    private data class LevelHit(val level: String, val source: String) {
        val approx: Boolean get() = source != "list"
    }

    private fun levelOf(lemma: String, pos: String?): LevelHit? {
        val key = normalizeSurface(lemma)
        val entry = levels.levels[key] ?: return null
        val chosen: Pair<String, String>? = if (pos == null) {
            // Without a part of speech, do not promote a common word just
            // because one of its rarer senses is graded higher (`very` is A1
            // as an adverb and B2 as an adjective). The lowest sense is the
            // safe default for a reading aid.
            entry.entries.minByOrNull { levelRank(it.value) }?.let { it.key to it.value }
        } else {
            posAliases(pos).mapNotNull { alias -> entry[alias]?.let { alias to it } }
                .minByOrNull { levelRank(it.second) }
        }
        if (chosen == null) return null
        val source = levels.sourceOf(key, chosen.first) ?: "list"
        return LevelHit(chosen.second, source)
    }

    private fun levelRank(level: String): Int = when (level) {
        "A1" -> 0
        "A2" -> 1
        "B1" -> 2
        "B2" -> 3
        "C1" -> 4
        "C2" -> 5
        else -> 99
    }

    private fun frequencyOf(normalized: String, lemma: String): Double? {
        val table = frequencies?.entries ?: return null
        return table[normalized] ?: table[lemma]
    }

    private fun occurrencesOf(normalized: String, lemma: String): Int? {
        val counts = occurrences?.counts ?: return null
        return counts[normalized] ?: counts[lemma]
    }

    /**
     * Chinese and English senses both come from the one ECDICT table.
     *
     * When an inflected form keeps a meaning of its own (`glasses`, `left`,
     * `found`), those senses follow the lemma's senses and are flagged with
     * [DictionarySense.fromSurface]. A plain plural (`dogs`) adds nothing, so the
     * panel reads as the base word's entry.
     */
    private fun definitionsFor(lemma: String, surface: String, pos: String?): List<DictionarySense> {
        val zh = sensesOf(ecdict, lemma, pos, "zh")
        val en = sensesOf(ecdict, lemma, pos, "en").distinctBy { it.definition.lowercase() }
        if (normalizeSurface(lemma) == normalizeSurface(surface)) return zh + en
        val surfaceEntry = entryOf(surface) ?: return zh + en
        if (!hasOwnSenses(surfaceEntry)) return zh + en

        val zhSegments = zh.flatMap { segments(it.definition) }
        val enDefinitions = en.map { normalizeDefinition(it.definition) }.toSet()
        val extraZh = surfaceSenses(surfaceEntry, pos, "zh", zhSegments)
        val extraEn = surfaceSenses(surfaceEntry, pos, "en", emptyList())
            .filterNot { normalizeDefinition(it.definition) in enDefinitions }
        return zh + extraZh + en + extraEn
    }

    /**
     * A regular plural's own row only repeats the lemma (`dogs`, `children`), so
     * it is not worth a second block. Other inflections can be homographs
     * (`left`, `found`, `bit`) and keep their own senses.
     */
    private fun hasOwnSenses(entry: DictionaryEntry): Boolean {
        if (entry.senses.isEmpty() && entry.en.isEmpty()) return false
        val kind = entry.form.orEmpty()
        val pluralOnly = kind.isNotEmpty() && kind.contains('s') && kind.all { it == 's' || it == '3' }
        if (pluralOnly && entry.senses.any { sense ->
                sense.getOrNull(0)?.trim() == "pl." ||
                    PLURAL_NOTE.containsMatchIn(sense.getOrNull(1).orEmpty())
            }
        ) {
            return false
        }
        return true
    }

    private fun surfaceSenses(
        entry: DictionaryEntry,
        pos: String?,
        language: String,
        baseSegments: List<String>,
    ): List<DictionarySense> {
        val lines = if (language == "en") entry.en else entry.senses
        val senses = lines.mapNotNull { sense ->
            val rawPos = sense.getOrNull(0)?.trim().orEmpty()
            if (rawPos == "pl." || rawPos == "abbr.") return@mapNotNull null
            val definition = sense.getOrNull(1) ?: return@mapNotNull null
            val cleaned = cleanSurfaceDefinition(definition, baseSegments, language)
                ?: return@mapNotNull null
            DictionarySense(
                pos = rawPos.takeIf { it.isNotBlank() },
                definition = cleaned,
                language = language,
                fromSurface = true,
            )
        }
        return if (pos == null) senses else senses.sortedByDescending { it.pos == pos }
    }

    private fun cleanSurfaceDefinition(
        definition: String,
        baseSegments: List<String>,
        language: String,
    ): String? {
        val withoutNotes = PAREN_NOTE.replace(definition, " ")
        val parts = withoutNotes.split(';', '；')
            .map { it.trim() }
            .filter { it.isNotEmpty() && !NOTE.containsMatchIn(it) }
        val kept = if (language == "zh") {
            parts.filterNot { part ->
                val normalized = normalizeDefinition(part)
                normalized.isEmpty() || baseSegments.any { base ->
                    base.isNotEmpty() && (base.contains(normalized) || normalized.contains(base))
                }
            }
        } else {
            parts
        }
        return kept.takeIf { it.isNotEmpty() }
            ?.joinToString(if (language == "zh") "；" else " ") { it }
    }

    private fun resolveLemma(surface: String): String? {
        val entry = entryOf(surface)
        entry?.lemma?.takeIf { it.isNotBlank() }?.let { lemma ->
            if (hasDefinitions(lemma)) return lemma
        }
        // Only stem a word that the dictionary does not know; a known headword
        // may simply be its own base form (`series`, `news`).
        if (entry != null && (entry.senses.isNotEmpty() || entry.en.isNotEmpty())) return null
        return guessLemma(normalizeSurface(surface))?.takeIf { hasDefinitions(it) }
    }

    private fun entryOf(term: String): DictionaryEntry? {
        val key = normalizeSurface(term)
        return ecdict?.entries?.get(key) ?: ecdict?.entries?.get(normalize(term))
    }

    private fun hasDefinitions(term: String): Boolean {
        val entry = entryOf(term) ?: return false
        return entry.senses.isNotEmpty() || entry.en.isNotEmpty()
    }

    private fun phoneticOf(lemma: String, surface: String): String? =
        entryOf(lemma)?.phonetic?.takeIf { it.isNotBlank() }
            ?: entryOf(surface)?.phonetic?.takeIf { it.isNotBlank() }

    private fun sensesOf(
        table: DictionaryFile?,
        lemma: String,
        pos: String?,
        language: String,
    ): List<DictionarySense> {
        val entry = table?.entries?.get(lemma)
            ?: table?.entries?.get(normalize(lemma))
            ?: return emptyList()
        val lines = if (language == "en") entry.en else entry.senses
        val senses = lines.mapNotNull { sense ->
            val definition = sense.getOrNull(1)?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            DictionarySense(
                pos = sense.getOrNull(0)?.takeIf { it.isNotBlank() },
                definition = definition,
                language = language,
            )
        }
        return if (pos == null) senses else senses.sortedByDescending { it.pos == pos }
    }

    private data class Fallback(
        val lemma: String,
        val definitions: List<DictionarySense>,
        val label: String? = null,
        val note: String? = null,
    )

    /** Contraction → hyphen compound → proper noun, in the spec's order. */
    private fun fallbackLookup(surface: String, pos: String?): Fallback? {
        contraction(surface)?.let { (stem, word) ->
            if (hasDefinitions(stem)) {
                return Fallback(
                    lemma = stem,
                    definitions = definitionsFor(stem, stem, pos),
                    note = "缩合词：$stem + $word",
                )
            }
        }
        if (surface.contains('-')) {
            val parts = surface.split('-').filter { it.length >= 2 }
            if (parts.size >= 2 && parts.all(::hasDefinitions)) {
                return Fallback(
                    lemma = normalizeSurface(surface),
                    definitions = parts.flatMap { definitionsFor(it, it, null) },
                    note = "复合词，按两部分查",
                )
            }
        }
        entryOf(surface)?.kind?.takeIf { it.isNotBlank() }?.let { kind ->
            return Fallback(
                lemma = normalizeSurface(surface),
                definitions = emptyList(),
                label = when (kind) {
                    "name" -> "人名"
                    "place" -> "地名"
                    else -> "专名"
                },
            )
        }
        return null
    }

    private fun contraction(surface: String): Pair<String, String>? {
        val value = normalize(surface)
        val suffixes = listOf(
            "'ll" to "will", "'re" to "are", "'ve" to "have",
            "'d" to "would", "'m" to "am", "n't" to "not",
        )
        for ((suffix, word) in suffixes) {
            if (value.endsWith(suffix) && value.length > suffix.length) {
                return value.dropLast(suffix.length) to word
            }
        }
        if (value.endsWith("'") && value.length > 2) return value.dropLast(1) to ""
        return null
    }

    private fun segments(value: String): List<String> =
        value.split(';', '；', ',', '，')
            .map { normalizeDefinition(it) }
            .filter { it.isNotEmpty() }

    private fun normalizeDefinition(value: String): String =
        value.lowercase().replace(PUNCTUATION, "")

    private fun guessLemma(term: String): String? {
        if (term.length < 3 || term.any { it == '-' || it == '\'' || it == ' ' }) return null
        IRREGULAR[term]?.let { return it }
        val candidates = mutableListOf<String>()
        fun add(value: String) {
            if (value.length >= 2) candidates += value
        }
        when {
            term.endsWith("ies") && term.length > 4 -> add(term.dropLast(3) + "y")
            term.endsWith("ves") -> {
                add(term.dropLast(3) + "f")
                add(term.dropLast(3) + "fe")
            }
            term.endsWith("ches") || term.endsWith("shes") ||
                term.endsWith("xes") || term.endsWith("zes") || term.endsWith("ses") ->
                add(term.dropLast(2))
            term.endsWith("es") && term.length > 3 -> {
                add(term.dropLast(2))
                add(term.dropLast(1))
            }
            term.endsWith("s") && !term.endsWith("ss") && !term.endsWith("us") && !term.endsWith("is") ->
                add(term.dropLast(1))
        }
        when {
            term.endsWith("ied") -> add(term.dropLast(3) + "y")
            term.endsWith("ed") && term.length > 4 -> {
                add(term.dropLast(2))
                add(term.dropLast(1))
                if (term[term.length - 3] == term[term.length - 4]) add(term.dropLast(3))
            }
            term.endsWith("ing") && term.length > 5 -> {
                add(term.dropLast(3))
                add(term.dropLast(3) + "e")
                if (term[term.length - 4] == term[term.length - 5]) add(term.dropLast(4))
            }
            term.endsWith("ier") -> add(term.dropLast(3) + "y")
            term.endsWith("est") -> {
                add(term.dropLast(3))
                add(term.dropLast(2))
            }
            term.endsWith("er") -> {
                add(term.dropLast(2))
                add(term.dropLast(1))
            }
        }
        return candidates.firstOrNull { hasDefinitions(it) }
    }

    companion object {
        fun normalize(term: String): String = term.trim().lowercase().replace('’', '\'')

        /** Table-key normalisation: lower case plus a simple possessive strip. */
        fun normalizeSurface(term: String): String {
            val value = normalize(term)
            return if (value.length > 2 && value.endsWith("'s")) value.dropLast(2) else value
        }

        private val PUNCTUATION = Regex("[\\s,，;；.。·\\-—()（）]")

        // ECDICT notes an inflected row as `dog的复数`, `be的现在时复数`,
        // `daisy的名词复数`, `（glass的复数形式）` and so on.
        private const val NOTE_BODY =
            "(?:[a-z'’-]+的(?:现在时|过去式|过去分词|现在分词|第三人称单数|名词复数|复数|比较级|最高级)(?:和(?:过去式|过去分词|现在分词|名词复数|复数))?" +
                "|[a-z'’-]+复数|名词复数|动词复数|形容词复数|复数形式)"
        private val NOTE = Regex(NOTE_BODY)
        private val PLURAL_NOTE =
            Regex("(?:[a-z'’-]+的(?:现在时)?复数|[a-z'’-]+复数|名词复数|动词复数|复数形式)")
        private val PAREN_NOTE = Regex("[（(][^）)]*$NOTE_BODY[^）)]*[）)]")

        /** Fallbacks for the few irregulars ECDICT's `exchange` does not link. */
        private val IRREGULAR = mapOf(
            "went" to "go",
            "gone" to "go",
            "children" to "child",
            "men" to "man",
            "women" to "woman",
            "mice" to "mouse",
            "teeth" to "tooth",
            "feet" to "foot",
            "geese" to "goose",
            "better" to "good",
            "best" to "good",
            "worse" to "bad",
            "worst" to "bad",
            "further" to "far",
            "farther" to "far",
        )

        private fun posAliases(pos: String): List<String> = when (pos) {
            "verb" -> listOf("verb", "be-verb", "do-verb", "have-verb")
            "auxiliary" -> listOf("be-verb", "do-verb", "have-verb", "verb")
            "modal" -> listOf("modal-auxiliary", "modal", "verb")
            "determiner" -> listOf("determiner", "article")
            "conjunction" -> listOf("conjunction", "preposition")
            else -> listOf(pos)
        }

        /** Parse a third-party table; malformed files yield an empty table. */
        fun parseLevels(json: String): WordLevelTable =
            runCatching { PagelenceJson.decodeFromString<WordLevelTable>(json) }
                .getOrDefault(WordLevelTable())

        fun parseWordFrequencies(json: String): WordFrequencyTable =
            runCatching { PagelenceJson.decodeFromString<WordFrequencyTable>(json) }
                .getOrDefault(WordFrequencyTable())

        private val BOOK_WORD = Regex("[a-z][a-z'’\\-]*")

        /**
         * Count every token of a whole book once. The table is cached in memory
         * for the session; nothing is written back into the book or state.
         */
        fun countWords(book: Book): Map<String, Int> {
            val counts = HashMap<String, Int>(1 shl 12)
            for (chapter in book.chapters) {
                for (unit in chapter.units) {
                    for (paragraph in unit.paragraphs) {
                        for (match in BOOK_WORD.findAll(paragraph.text.lowercase())) {
                            val word = match.value.replace('’', '\'')
                            counts[word] = (counts[word] ?: 0) + 1
                        }
                    }
                }
            }
            return counts
        }

        fun parseDictionary(json: String): DictionaryFile =
            runCatching { PagelenceJson.decodeFromString<DictionaryFile>(json) }
                .getOrDefault(DictionaryFile())
    }
}
