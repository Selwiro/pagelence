package com.pagelence.core.lookup

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LookupHistoryTest {
    private fun entry(
        term: String,
        at: String,
        bookId: String = "book",
        lemma: String = term,
        sentenceNumber: Int = 1,
    ) = LookupEntry(
        id = LookupHistoryRules.idOf(bookId, lemma, term),
        bookId = bookId,
        unitId = "u1",
        paragraphId = "p1",
        sentenceNumber = sentenceNumber,
        term = term,
        lemma = lemma,
        start = 0,
        end = term.length,
        gloss = "释义",
        lookedUpAt = at,
    )

    @Test
    fun `id ignores case and surrounding spaces`() {
        assertEquals(
            LookupHistoryRules.idOf("book", "  Dogs ", "dogs"),
            LookupHistoryRules.idOf("book", "dogs", "DOGS"),
        )
        assertEquals("book::old man", LookupHistoryRules.idOf("book", "Old   Man", "old man"))
    }

    @Test
    fun `recording the same word moves it to the top without duplicating`() {
        val first = entry("sea", "2026-11-22T08:00:00Z")
        val other = entry("boat", "2026-11-22T09:00:00Z")
        val again = first.copy(gloss = "大海", lookedUpAt = "2026-11-22T10:00:00Z")

        val list = LookupHistoryRules.record(listOf(other, first), again)

        assertEquals(listOf("sea", "boat"), list.map { it.term })
        assertEquals("大海", list.first().gloss)
        assertEquals(2, list.size)
    }

    @Test
    fun `history is capped`() {
        var list = emptyList<LookupEntry>()
        repeat(5) { index ->
            list = LookupHistoryRules.record(
                list,
                entry("w$index", "2026-11-22T0$index:00:00Z"),
                max = 3,
            )
        }

        assertEquals(listOf("w4", "w3", "w2"), list.map { it.term })
    }

    @Test
    fun `sanitize drops unusable rows and keeps the newest duplicate`() {
        val old = entry("sea", "2026-11-22T08:00:00Z", sentenceNumber = 2)
        val new = entry("sea", "2026-11-22T10:00:00Z", sentenceNumber = 4)
        val broken = entry("", "2026-11-22T11:00:00Z")

        val list = LookupHistoryRules.sanitize(listOf(old, broken, new))

        assertEquals(1, list.size)
        assertEquals(4, list.single().sentenceNumber)
    }

    @Test
    fun `merge keeps the later lookup on each device`() {
        val local = listOf(entry("sea", "2026-11-22T08:00:00Z"), entry("boat", "2026-11-22T09:00:00Z"))
        val remote = listOf(
            entry("sea", "2026-11-22T10:00:00Z", sentenceNumber = 7),
            entry("bird", "2026-11-22T07:00:00Z"),
        )

        val merged = LookupHistoryRules.merge(local, remote)

        assertEquals(listOf("sea", "boat", "bird"), merged.map { it.term })
        assertEquals(7, merged.first().sentenceNumber)
        assertTrue(merged.size == 3)
    }
}
