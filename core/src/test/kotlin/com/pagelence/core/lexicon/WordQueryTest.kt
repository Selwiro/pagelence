package com.pagelence.core.lexicon

import com.pagelence.core.ContentFixtures
import com.pagelence.core.content.FileContentRepository
import com.pagelence.core.content.HintTapResolver
import com.pagelence.core.wordcards.InMemoryWordCardRepository
import com.pagelence.core.wordcards.WordCard
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class WordQueryTest {
    private val repository = FileContentRepository(ContentFixtures.contentDir)
    private val index = assertNotNull(repository.index(ContentFixtures.dolittle))
    private val levels = WordQuery.parseLevels(ContentFixtures.read("third-party/word-levels.json"))
    private val ecdict = WordQuery.parseDictionary(ContentFixtures.read("third-party/dictionaries/ecdict.json"))

    private fun query(cards: InMemoryWordCardRepository? = null) = WordQuery(
        index = index,
        levels = levels,
        ecdict = ecdict,
        cards = cards,
    )

    @Test
    fun `sentence hint wins over the generic tables`() {
        val (unitId, paragraphId, sentenceIndex) = locate("folks")
        val lookup = assertNotNull(
            query().lookup(unitId, paragraphId, sentenceIndex, "folks"),
        )
        assertEquals("folks", lookup.lemma)
        assertEquals("noun", lookup.pos)
        assertEquals("人们（folks = people）。", lookup.glossZh)
        assertNull(lookup.zipf)
        assertNull(lookup.bookOccurrences)
    }

    @Test
    fun `paragraph-level hint supplies the meaning`() {
        val (unitId, paragraphId, sentenceIndex) = locate("Puddleby-on-the-Marsh")
        val lookup = assertNotNull(
            query().lookup(unitId, paragraphId, sentenceIndex, "Puddleby-on-the-Marsh"),
        )
        assertTrue(lookup.glossZh.orEmpty().contains("帕德尔比"), "book translation is shown")
        assertNull(lookup.level)
    }

    @Test
    fun `multi-word hint opens as one term`() {
        val (unitId, paragraphId, sentenceIndex) = locateHint("by sight")
        val lookup = assertNotNull(
            query().lookup(unitId, paragraphId, sentenceIndex, "by sight"),
        )
        assertEquals("by sight", lookup.term)
        assertTrue(lookup.glossZh.orEmpty().contains("凭长相"), "book translation is shown")
        assertTrue(lookup.definitions.any { it.language == "zh" && it.definition.contains("凭外貌") })
    }

    @Test
    fun `tapping either word of a book phrase reaches its hint`() {
        val unit = assertNotNull(index.unit("chapter-01-u01"))
        val paragraph = assertNotNull(unit.paragraphs.firstOrNull { it.id == "chapter-01-p002" })
        val sentence = assertNotNull(index.sentence(unit.id, 4))
        val start = sentence.text.indexOf("sight")

        val tap = assertNotNull(
            HintTapResolver.resolve(
                sentenceText = sentence.text,
                sentenceNumber = 4,
                hints = paragraph.hints,
                wordStart = start,
                wordEnd = start + "sight".lastIndex,
            ),
        )
        assertEquals("by sight", tap.target)
        assertEquals("chapter-01-p002-by-sight", tap.hint.id)

        val lookup = assertNotNull(query().lookup(unit.id, paragraph.id, 4, tap.target))
        assertTrue(lookup.glossZh.orEmpty().contains("凭长相"))
    }

    @Test
    fun `punctuated hint opens as one term`() {
        val (unitId, paragraphId, sentenceIndex) = locateHint("M.D.")
        val lookup = assertNotNull(
            query().lookup(unitId, paragraphId, sentenceIndex, "M.D."),
        )
        assertEquals("M.D.", lookup.term)
        assertTrue(lookup.glossZh.orEmpty().contains("医学博士"))
        assertTrue(lookup.definitions.any { it.definition.contains("医学博士") })
    }

    @Test
    fun `words without a book hint fall back to levels and dictionaries`() {
        val (unitId, paragraphId, sentenceIndex) = locate("people")
        val lookup = assertNotNull(
            query().lookup(unitId, paragraphId, sentenceIndex, "people"),
        )
        assertNull(lookup.glossZh)
        assertEquals("A1", lookup.level)
        assertEquals("list", lookup.levelSource)
        assertFalse(lookup.levelApprox)
        assertTrue(lookup.definitions.isNotEmpty())
    }

    @Test
    fun `unknown part of speech picks the lower CEFR sense`() {
        val (unitId, paragraphId, sentenceIndex) = locate("very")
        val lookup = assertNotNull(
            query().lookup(unitId, paragraphId, sentenceIndex, "very"),
        )
        assertEquals("A1", lookup.level)
    }

    @Test
    fun `contractions resolve to their stem with a note`() {
        val alice = assertNotNull(repository.index(ContentFixtures.alice))
        val (unitId, paragraphId, sentenceIndex) = locateIn(alice, "she'll")
        val lookup = assertNotNull(
            WordQuery(index = alice, levels = levels, ecdict = ecdict)
                .lookup(unitId, paragraphId, sentenceIndex, "she'll"),
        )
        assertEquals("she", lookup.lemma)
        assertTrue(lookup.note.orEmpty().contains("缩合词"))
        assertTrue(lookup.definitions.isNotEmpty())
    }

    @Test
    fun `hyphenated compounds resolve when both parts exist`() {
        val alice = assertNotNull(repository.index(ContentFixtures.alice))
        val (unitId, paragraphId, sentenceIndex) = locateIn(alice, "waistcoat-pocket")
        val lookup = assertNotNull(
            WordQuery(index = alice, levels = levels, ecdict = ecdict)
                .lookup(unitId, paragraphId, sentenceIndex, "waistcoat-pocket"),
        )
        assertTrue(lookup.note.orEmpty().contains("复合词"))
        assertTrue(lookup.definitions.isNotEmpty())
    }

    @Test
    fun `form notes are removed from the shipped dictionary`() {
        val left = assertNotNull(ecdict.entries["left"])
        assertTrue(left.senses.none { it.getOrNull(1)?.contains("过去式") == true })
        val glasses = assertNotNull(ecdict.entries["glasses"])
        assertTrue(glasses.senses.none { it.getOrNull(1)?.contains("复数形式") == true })
    }

    @Test
    fun `lookups outside the sentence or book return null`() {
        assertNull(query().lookup("chapter-01-u01", "chapter-01-p002", 1, "herring"))
        assertNull(query().lookup("chapter-01-u01", "chapter-01-p002", 99, "folks"))
        assertNull(query().lookup("ghost-unit", "chapter-01-p002", 1, "folks"))
        assertNull(query().lookup("chapter-01-u01", "chapter-01-p002", 1, "  "))
    }

    @Test
    fun `collected cards are flagged in the lookup`() {
        val cards = InMemoryWordCardRepository()
        val unit = assertNotNull(index.unit("chapter-01-u01"))
        val paragraph = assertNotNull(unit.paragraphs.firstOrNull { it.id == "chapter-01-p002" })
        val hint = assertNotNull(paragraph.hints.first { it.target == "folks" })
        cards.add(WordCard.from(ContentFixtures.dolittle, unit, paragraph, hint, "2026-11-19T00:00:00Z"))

        val (locatedUnit, paragraphId, sentenceIndex) = locate("folks")
        assertTrue(assertNotNull(query(cards).lookup(locatedUnit, paragraphId, sentenceIndex, "folks")).inWordCards)
        assertFalse(assertNotNull(query().lookup(locatedUnit, paragraphId, sentenceIndex, "folks")).inWordCards)
    }

    @Test
    fun `inflected words resolve to their base entry`() {
        val (unitId, paragraphId, sentenceIndex) = locate("dogs")
        val lookup = assertNotNull(query().lookup(unitId, paragraphId, sentenceIndex, "dogs"))

        assertEquals("dogs", lookup.term)
        assertEquals("dog", lookup.lemma)
        assertTrue(
            lookup.definitions.any { it.language == "zh" && it.definition.contains("狗") },
            "the base entry's Chinese sense is shown",
        )
        assertTrue(
            lookup.definitions.none { it.definition.contains("的复数") || it.definition.contains("复数形式") },
            "a regular plural does not show a form-note stub",
        )
    }

    @Test
    fun `irregular forms resolve too`() {
        assertEquals("child", lookup("children").lemma)
        assertEquals("go", lookup("went").lemma)
        assertEquals("be", lookup("are").lemma)
    }

    @Test
    fun `a lemma that never appears in the book is still defined`() {
        val alice = assertNotNull(
            FileContentRepository(ContentFixtures.contentDir).index(ContentFixtures.alice),
        )
        val (unitId, paragraphId, sentenceIndex) = locateIn(alice, "daisies")
        val query = WordQuery(
            index = alice,
            levels = levels,
            ecdict = ecdict,
            )
        val lookup = assertNotNull(query.lookup(unitId, paragraphId, sentenceIndex, "daisies"))
        assertEquals("daisy", lookup.lemma)
        assertTrue(lookup.definitions.any { it.language == "zh" && it.definition.contains("雏菊") })
        assertTrue(
            lookup.definitions.none { it.definition.contains("的复数") || it.definition.contains("复数形式") },
            "a regular plural adds no form-note stub",
        )
    }

    @Test
    fun `an inflected form keeps the meanings of its own`() {
        val (unitId, paragraphId, sentenceIndex) = locate("left")
        val lookup = assertNotNull(query().lookup(unitId, paragraphId, sentenceIndex, "left"))
        assertEquals("leave", lookup.lemma)
        assertTrue(
            lookup.definitions.any { it.fromSurface && it.definition.contains("左") },
            "left as 左边的 is kept next to leave",
        )
    }

    @Test
    fun `reference tables parse without their licence fields`() {
        assertEquals(33210, levels.levels.size)
        assertTrue(ecdict.entries.isNotEmpty())
        assertTrue(levels.sources.isNotEmpty())
    }

    @Test
    fun `inferred levels read as approximate`() {
        val frequencies = WordQuery.parseWordFrequencies(
            ContentFixtures.read("third-party/word-frequencies.json"),
        )
        val occurrences = BookOccurrences(WordQuery.countWords(index.book))
        val query = WordQuery(
            index = index,
            levels = levels,
            frequencies = frequencies,
            occurrences = occurrences,
            ecdict = ecdict,
        )
        val (unitId, paragraphId, sentenceIndex) = locate("people")
        val lookup = assertNotNull(
            query.lookup(unitId, paragraphId, sentenceIndex, "people"),
        )
        assertNotNull(lookup.frequencyBand)
        assertNotNull(lookup.zipf)
        assertTrue((lookup.bookOccurrences ?: 0) > 0, "the book scan counts the word")

        val folks = assertNotNull(query.lookup(unitId, paragraphId, sentenceIndex, "people"))
        assertEquals("A1", folks.level)
    }

    @Test
    fun `a word graded by an inferred layer is marked approximate`() {
        val (unitId, paragraphId, sentenceIndex) = locate("folks")
        val lookup = assertNotNull(query().lookup(unitId, paragraphId, sentenceIndex, "folks"))
        assertEquals("B2", lookup.level)
        assertEquals("aligned", lookup.levelSource)
        assertTrue(lookup.levelApprox)
    }

    @Test
    fun `frequency bands follow the shipped cut points`() {
        val table = WordQuery.parseWordFrequencies(
            ContentFixtures.read("third-party/word-frequencies.json"),
        )
        assertEquals(FrequencyBand.HIGH, table.bandOf(table.bands.high))
        assertEquals(FrequencyBand.COMMON, table.bandOf(table.bands.common))
        assertEquals(FrequencyBand.NORMAL, table.bandOf(table.bands.normal))
        assertEquals(FrequencyBand.LOW, table.bandOf(0.5))
    }

    private fun lookup(word: String): WordLookup {
        val (unitId, paragraphId, sentenceIndex) = locate(word)
        return assertNotNull(query().lookup(unitId, paragraphId, sentenceIndex, word))
    }

    /** Find the unit, paragraph and 1-based unit sentence index of the first [word]. */
    private fun locate(word: String): Triple<String, String, Int> = locateIn(index, word)

    /** Find the paragraph hint for [target] and the sentence it is attached to. */
    private fun locateHint(target: String): Triple<String, String, Int> {
        for (unit in index.units) {
            for (paragraph in unit.paragraphs) {
                val hint = paragraph.hints.firstOrNull {
                    it.kind != "sentence" && it.target.equals(target, ignoreCase = true)
                } ?: continue
                val sentence = hint.sentence ?: continue
                return Triple(unit.id, paragraph.id, sentence)
            }
        }
        error("hint $target not found in ${index.book.id}")
    }

    private fun locateIn(
        index: com.pagelence.core.content.ContentIndex,
        word: String,
    ): Triple<String, String, Int> {
        for (unit in index.units) {
            var number = 0
            for (paragraph in unit.paragraphs) {
                for (sentence in paragraph.sentences) {
                    number += 1
                    if (sentence.text.contains(word)) return Triple(unit.id, paragraph.id, number)
                }
            }
        }
        error("word $word not found in ${index.book.id}")
    }
}
