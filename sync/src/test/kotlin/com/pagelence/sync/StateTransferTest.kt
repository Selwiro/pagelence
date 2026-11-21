package com.pagelence.sync

import com.pagelence.core.progress.ReadingStateCodec
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class StateTransferTest {
    @Test
    fun exportIsTheStateDocument() {
        val local = MemoryLocalState(Fixtures.state(books = mapOf("b" to Fixtures.book())))
        val exported = StateTransfer.export(local)
        assertEquals("b", ReadingStateCodec.decode(exported)?.state?.books?.keys?.first())
    }

    @Test
    fun importMergesInsteadOfReplacing() {
        val local = MemoryLocalState(
            Fixtures.state(books = mapOf("local" to Fixtures.book(id = "local"))),
        )
        val imported = ReadingStateCodec.encode(
            Fixtures.state(
                deviceId = "old-phone",
                books = mapOf("remote" to Fixtures.book(id = "remote")),
            ),
        )
        val result = StateTransfer.import(local, imported, Fixtures.NOW, Fixtures.TODAY)
        assertEquals(setOf("local", "remote"), result.state.books.keys)
        assertEquals(setOf("local", "remote"), local.current().books.keys)
    }

    @Test
    fun importRejectsUnreadableDocumentsWithoutTouchingLocalState() {
        val local = MemoryLocalState(Fixtures.state(books = mapOf("b" to Fixtures.book())))
        val before = local.current()
        assertFailsWith<SyncException> {
            StateTransfer.import(local, "<html>nope</html>", Fixtures.NOW, Fixtures.TODAY)
        }
        assertTrue(before === local.current())
    }
}
