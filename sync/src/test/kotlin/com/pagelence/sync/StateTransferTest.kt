package com.pagelence.sync

import com.pagelence.core.progress.BookProgress
import com.pagelence.core.progress.CefrLevel
import com.pagelence.core.progress.ReadingStateCodec
import com.pagelence.core.wordcards.WordCard
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class StateTransferTest {
    private fun card(id: String, deleted: Boolean): WordCard = WordCard(
        id = id,
        bookId = id.substringBefore("::"),
        paragraphId = "p1",
        hintId = "h1",
        term = "word",
        lemma = "word",
        createdAt = "2026-11-19T09:00:00Z",
        updatedAt = "2026-11-20T09:00:00Z",
        deletedAt = if (deleted) "2026-11-20T09:00:00Z" else null,
    )

    private fun document() = Fixtures.state(
        deviceId = "device-b",
        updatedAt = "2026-11-20T10:00:00Z",
        books = mapOf(
            "a" to BookProgress(bookId = "a", readUnitIds = listOf("a1", "a2")),
            "b" to BookProgress(bookId = "b", readUnitIds = listOf("b1")),
        ),
        cards = listOf(card("a::p1::h1", deleted = false), card("a::p1::h2", deleted = true)),
    ).copy(selfLevel = CefrLevel.B1, levelUpdatedAt = "2026-11-20T09:00:00Z")

    @Test
    fun inspectShowsCountsBeforeWritingAnything() {
        val inspection = StateTransfer.inspect(ReadingStateCodec.encode(document()))
        val ready = assertIs<StateTransfer.Inspection.Ready>(inspection)
        assertEquals(2, ready.preview.books)
        assertEquals(3, ready.preview.readUnits)
        assertEquals(1, ready.preview.cards)
        assertEquals(CefrLevel.B1, ready.preview.level)
        assertEquals("2026-11-20T10:00:00Z", ready.preview.updatedAt)
    }

    @Test
    fun inspectRejectsGarbage() {
        assertEquals(StateTransfer.Inspection.Invalid, StateTransfer.inspect("not json"))
        assertEquals(StateTransfer.Inspection.Invalid, StateTransfer.inspect("{}"))
    }

    @Test
    fun inspectRejectsDocumentsFromANewerBuild() {
        val future = document().copy(schemaVersion = 99)
        assertEquals(
            StateTransfer.Inspection.FutureVersion,
            StateTransfer.inspect(ReadingStateCodec.encode(future)),
        )
    }

    @Test
    fun mergeKeepsBothSidesAndReportsASummary() {
        val local = MemoryLocalState(Fixtures.state(books = mapOf("a" to Fixtures.book(units = listOf("a3")))))
        val result = StateTransfer.import(
            local,
            document(),
            StateTransfer.ImportMode.MERGE,
            Fixtures.NOW,
            Fixtures.TODAY,
        )
        assertEquals(listOf("a3", "a1", "a2"), result.state.books.getValue("a").readUnitIds)
        assertTrue(result.state.books.containsKey("b"))
        assertEquals(StateTransfer.ImportMode.MERGE, result.mode)
        assertTrue((result.summary?.booksAdded ?: 0) >= 1)
    }

    @Test
    fun overwriteReplacesTheDocumentButKeepsTheLocalDeviceId() {
        val local = MemoryLocalState(
            Fixtures.state(
                deviceId = "device-a",
                books = mapOf("old" to Fixtures.book(id = "old")),
            ),
        )
        val result = StateTransfer.import(
            local,
            document(),
            StateTransfer.ImportMode.OVERWRITE,
            Fixtures.NOW,
            Fixtures.TODAY,
        )
        assertEquals("device-a", result.state.deviceId)
        assertEquals(setOf("a", "b"), result.state.books.keys)
        assertNull(result.summary)
        assertEquals("device-a", local.current().deviceId)
    }

    @Test
    fun exportIsThePlainDocument() {
        val local = MemoryLocalState(document())
        val decoded = ReadingStateCodec.decode(StateTransfer.export(local))
        assertEquals(document().books, decoded?.state?.books)
        assertEquals(document().wordCards, decoded?.state?.wordCards)
    }
}
