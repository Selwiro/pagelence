package com.pagelence.core.lexicon

import com.pagelence.core.ContentFixtures
import com.pagelence.core.content.FileContentRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Guards the "tap a word, see nothing" regression and the general-core promise.
 *
 * The shipped `ecdict.json` is no longer trimmed to the books: it is a general
 * core (BNC/COCA rank, exam tags, Oxford 3000, CEFR levels) plus every form the
 * bundled books use. Coverage is therefore checked against the whole paragraph
 * text, including skeleton units that have no `sentences` yet, and a handful of
 * words that appear in neither book prove the table is not a per-book glossary.
 */
class DictionaryCoverageTest {
    private val token = Regex("[A-Za-z][A-Za-z'’-]*")

    @Test
    fun `bundled book words resolve to a gloss, a hint or a dictionary entry`() {
        val repository = FileContentRepository(ContentFixtures.contentDir)

        var totalTokens = 0
        var missingTokens = 0
        val missingTypes = mutableSetOf<String>()
        for (summary in repository.library()) {
            val index = repository.index(summary.id) ?: continue
            for (unit in index.units) {
                for (paragraph in unit.paragraphs) {
                    val annotated = paragraph.hints
                        .map { WordQuery.normalize(it.target) }
                        .toSet()
                    for (match in token.findAll(paragraph.text)) {
                        val word = WordQuery.normalizeSurface(match.value)
                        if (word.length < 2) continue
                        totalTokens += 1
                        val covered = word in annotated ||
                            ecdict.entries.containsKey(word)
                        if (!covered) {
                            missingTokens += 1
                            missingTypes += word
                        }
                    }
                }
            }
        }

        val ratio = if (totalTokens == 0) 0.0 else 1.0 - missingTokens.toDouble() / totalTokens
        assertTrue(
            ratio >= 0.97,
            "dictionary covers ${(ratio * 100).toInt()}% of book tokens; missing: ${missingTypes.take(30)}",
        )
    }

    @Test
    fun `every inflected entry points at a lemma with definitions`() {
        assertEquals("dog", ecdict.entries["dogs"]?.lemma)
        assertEquals("daisy", ecdict.entries["daisies"]?.lemma)
        assertEquals("go", ecdict.entries["went"]?.lemma)
        assertEquals("child", ecdict.entries["children"]?.lemma)

        val dangling = ecdict.entries.filter { (_, entry) ->
            val lemma = entry.lemma ?: return@filter false
            val base = ecdict.entries[lemma] ?: return@filter true
            base.senses.isEmpty() && base.en.isEmpty()
        }.keys
        assertTrue(dangling.isEmpty(), "inflections without a defined lemma: ${dangling.take(30)}")
    }

    @Test
    fun `common words carry a Chinese sense and an English sense`() {
        for (word in listOf("town", "doctor", "little", "people", "house")) {
            val entry = ecdict.entries[word]
            assertTrue(entry != null, "$word is not in the dictionary")
            assertTrue(entry.senses.isNotEmpty(), "$word has no Chinese sense")
            assertTrue(entry.en.isNotEmpty(), "$word has no English sense")
        }
    }

    @Test
    fun `general core words are available without a book`() {
        // None of these occurs in Doctor Dolittle or Alice; they prove the table
        // is a general dictionary rather than a corpus glossary.
        for (word in listOf(
            "government",
            "environment",
            "computer",
            "photosynthesis",
            "restaurant",
            "democracy",
            "vaccine",
            "ecosystem",
            "algorithm",
            "podcast",
        )) {
            val entry = ecdict.entries[word]
            assertTrue(entry != null, "$word is not in the general core dictionary")
            assertTrue(entry.senses.isNotEmpty(), "$word has no Chinese sense")
            assertTrue(entry.en.isNotEmpty(), "$word has no English sense")
        }
    }

    private companion object {
        val ecdict: DictionaryFile by lazy {
            WordQuery.parseDictionary(ContentFixtures.read("third-party/dictionaries/ecdict.json"))
        }
    }
}
