package com.pagelence.core.wordcards

import com.pagelence.core.ContentFixtures
import com.pagelence.core.content.FileContentRepository
import com.pagelence.core.content.Hint
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class WordCardTest {
    private val repository = FileContentRepository(ContentFixtures.contentDir)
    private val index = assertNotNull(repository.index(ContentFixtures.dolittle))
    private val unit = assertNotNull(index.unit("chapter-01-u01"))
    private val paragraph = assertNotNull(unit.paragraphs.firstOrNull { it.id == "chapter-01-p002" })
    private val hint = assertNotNull(paragraph.hints.firstOrNull { it.target == "folks" })
    private val card = WordCard.from(ContentFixtures.dolittle, unit, paragraph, hint, "2026-11-19T01:00:00Z")

    @Test
    fun `card id is book paragraph and hint`() {
        assertEquals(
            "doctor-dolittle-1920::chapter-01-p002::chapter-01-p002-folks",
            card.id,
        )
        assertEquals(WordCard.idOf("b", "p", "h"), "b::p::h")
    }

    @Test
    fun `card carries the tap context`() {
        assertEquals("folks", card.term)
        assertEquals("folks", card.lemma)
        assertEquals("noun", card.pos)
        assertEquals("人们（folks = people）。", card.zh)
        assertEquals("chapter-01-u01", card.unitId)
        assertNull(card.example)
        assertNull(card.ipa)
        assertFalse(card.isDeleted)
    }

    @Test
    fun `repository collects, replaces and tombstones cards`() {
        val store = InMemoryWordCardRepository()
        store.add(card)
        assertTrue(store.isCollected(ContentFixtures.dolittle, "chapter-01-p002", "chapter-01-p002-folks"))

        val edited = card.copy(zh = "人", updatedAt = "2026-11-19T02:00:00Z")
        val stored = store.add(edited)
        assertEquals("2026-11-19T01:00:00Z", stored.createdAt, "createdAt survives an edit")
        assertEquals("人", store.get(card.id)?.zh)

        assertTrue(store.remove(card.id, "2026-11-19T03:00:00Z"))
        assertTrue(assertNotNull(store.get(card.id)).isDeleted)
        assertTrue(store.list(ContentFixtures.dolittle).isEmpty(), "tombstones are hidden")
        assertEquals(1, store.list(ContentFixtures.dolittle, includeDeleted = true).size)
        assertFalse(store.isCollected(ContentFixtures.dolittle, "chapter-01-p002", "chapter-01-p002-folks"))
        assertFalse(store.remove("missing", "2026-11-19T03:00:00Z"))
    }

    @Test
    fun `a card built without an example still works`() {
        val synthetic = Hint(id = "x", target = "boat", zh = "船", en = "A boat.")
        val built = WordCard.from(ContentFixtures.dolittle, unit, paragraph, synthetic, "now")
        assertNull(built.example)
        assertEquals("boat", built.lemma)
        assertNull(built.pos)
    }

    @Test
    fun `cards merge by lemma across paragraphs`() {
        val store = InMemoryWordCardRepository()
        val dogs = card.copy(
            id = "book::p1::h1",
            paragraphId = "p1",
            hintId = "h1",
            term = "dogs",
            lemma = "dog",
        )
        val dog = card.copy(
            id = "book::p2::h2",
            paragraphId = "p2",
            hintId = "h2",
            term = "dog",
            lemma = "dog",
        )

        assertEquals(dogs.id, store.add(dogs).id)
        assertEquals(dogs.id, store.add(dog).id, "a second inflection returns the first card")
        assertEquals(1, store.all().size)
        assertEquals(dogs.id, store.find(ContentFixtures.dolittle, "dog", "dog")?.id)

        store.remove(dogs.id, "2026-11-19T04:00:00Z")
        assertNull(store.find(ContentFixtures.dolittle, "dog", "dog"))
        assertEquals(dog.id, store.add(dog).id, "removing one base word reopens collection")
        assertEquals(1, store.all().size)
    }

    @Test
    fun `a card collected before lemma resolution is still found by its surface`() {
        val store = InMemoryWordCardRepository()
        val legacy = card.copy(
            id = "book::p1::h1",
            paragraphId = "p1",
            hintId = "h1",
            term = "dogs",
            lemma = "dogs",
        )
        store.add(legacy)

        assertEquals(legacy.id, store.find(ContentFixtures.dolittle, "dog", "dogs")?.id)
    }

    @Test
    fun `serialised cards keep the tombstone field`() {
        val json = com.pagelence.core.content.PagelenceJson.encodeToString(
            WordCard.serializer(),
            card.copy(deletedAt = "2026-11-20T00:00:00Z"),
        )
        assertTrue(json.contains("\"deletedAt\":\"2026-11-20T00:00:00Z\""))
        assertEquals(
            "2026-11-20T00:00:00Z",
            com.pagelence.core.content.PagelenceJson.decodeFromString<WordCard>(json).deletedAt,
        )
    }
}
