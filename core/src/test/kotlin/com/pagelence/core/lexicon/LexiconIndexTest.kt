package com.pagelence.core.lexicon

import com.pagelence.core.ContentFixtures
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * The shipped `lexicon.bin` must be byte-for-byte equivalent to the three JSON
 * tables it replaces: the app no longer ships those tables (T156), so a stale
 * or misencoded index would quietly change every word entry.
 */
class LexiconIndexTest {
    private val index = LexiconIndex.parse(ContentFixtures.readBytes("third-party/lexicon.bin"))
    private val levels = WordQuery.parseLevels(ContentFixtures.read("third-party/word-levels.json"))
    private val frequencies = WordQuery.parseWordFrequencies(
        ContentFixtures.read("third-party/word-frequencies.json"),
    )
    private val ecdict = WordQuery.parseDictionary(
        ContentFixtures.read("third-party/dictionaries/ecdict.json"),
    )

    @Test
    fun `sizes match the source tables`() {
        assertEquals(38_229, index.dictionarySize)
        assertEquals(33_210, index.levelSize)
        assertEquals(37_897, index.frequencySize)
        assertEquals(ecdict.entries.size, index.dictionarySize)
        assertEquals(levels.levels.size, index.levelSize)
        assertEquals(frequencies.entries.size, index.frequencySize)
    }

    @Test
    fun `dictionary entries match the JSON table`() {
        // Every 199th key plus the boundary rows: a full comparison decodes all
        // 38k entries and makes the test slower than the build it guards.
        var checked = 0
        ecdict.entries.keys.forEachIndexed { position, word ->
            if (position % 199 != 0) return@forEachIndexed
            assertEquals(ecdict.entries[word], index.entry(word), "entry for $word")
            checked++
        }
        assertTrue(checked > 150, "sampled $checked entries")
        val first = ecdict.entries.keys.minOf { it }
        val last = ecdict.entries.keys.maxOf { it }
        assertEquals(ecdict.entries[first], index.entry(first))
        assertEquals(ecdict.entries[last], index.entry(last))
        assertEquals(null, index.entry("zzzzzz-not-a-word"))
    }

    @Test
    fun `level slots and sources match the JSON table`() {
        var checked = 0
        levels.levels.keys.forEachIndexed { position, word ->
            if (position % 199 != 0) return@forEachIndexed
            assertEquals(levels.lookup(word), index.lookup(word), "levels for $word")
            checked++
        }
        assertTrue(checked > 150, "sampled $checked words")
        assertEquals(levels.lookup("very"), index.lookup("very"))
        assertEquals(null, index.lookup("zzzzzz-not-a-word"))
    }

    @Test
    fun `frequency values and cut points match the JSON table`() {
        var checked = 0
        frequencies.entries.keys.forEachIndexed { position, word ->
            if (position % 199 != 0) return@forEachIndexed
            assertEquals(frequencies.zipf(word), index.zipf(word), "zipf for $word")
            checked++
        }
        assertTrue(checked > 150, "sampled $checked words")
        assertEquals(frequencies.zipf("the"), index.zipf("the"))
        assertEquals(null, index.zipf("zzzzzz-not-a-word"))
        for (sample in listOf(0.5, 2.9, 3.5, 4.3, 7.4)) {
            assertEquals(frequencies.bandOf(sample), index.bandOf(sample), "band at $sample")
        }
    }

    @Test
    fun `words in the books keep the same lookup through the index`() {
        val repository = com.pagelence.core.content.FileContentRepository(
            ContentFixtures.contentDir,
            lexicon = { index },
        )
        val plain = com.pagelence.core.content.FileContentRepository(ContentFixtures.contentDir)
        val book = assertNotNull(repository.book(ContentFixtures.dolittle))
        var checked = 0
        for (chapter in book.chapters) {
            for (unit in chapter.units) {
                for (paragraph in unit.paragraphs) {
                    val hint = paragraph.hints.firstOrNull() ?: continue
                    val sentence = hint.sentence ?: 1
                    val indexed = repository.lookup(book.id, unit.id, paragraph.id, sentence, hint.target)
                        ?: continue
                    val expected = plain.lookup(book.id, unit.id, paragraph.id, sentence, hint.target)
                    assertEquals(expected, indexed, "lookup for ${hint.target}")
                    checked++
                }
            }
        }
        assertTrue(checked > 50, "checked $checked hint lookups")
    }

    @Test
    fun `a truncated file is rejected`() {
        assertFailsWith<IllegalArgumentException> { LexiconIndex.parse(byteArrayOf(1, 2, 3)) }
        val valid = ContentFixtures.readBytes("third-party/lexicon.bin")
        assertFailsWith<IllegalArgumentException> { LexiconIndex.parse(valid.copyOf(valid.size / 2)) }
    }
}
