package com.pagelence.core.progress

import com.pagelence.core.ContentFixtures
import com.pagelence.core.content.FileContentRepository
import com.pagelence.core.content.PagelenceJson
import kotlinx.serialization.Serializable
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Cross-implementation check for read-unit counting and resume positions.
 *
 * The Web reader's `advance` / `restoreProgress` (`work/web/src/reading.mjs`)
 * are the reference. `progress-parity.json` is recorded from those rules over
 * the shipped book by `tools/migrate/record_progress_parity.py`; this test
 * replays the same sequences through [ProgressSemantics].
 */
class ProgressParityTest {
    private val NOW = "2026-11-21T10:00:00Z"
    private val repository = FileContentRepository(ContentFixtures.contentDir)
    private val fixture: Fixture = PagelenceJson.decodeFromString(
        ContentFixtures.resource("parity/progress-parity.json"),
    )

    @Test
    fun `fixture targets the shipped book`() {
        assertEquals(ContentFixtures.dolittle, fixture.bookId)
        assertEquals(103, fixture.unitCount)
        val index = assertNotNull(repository.index(fixture.bookId))
        assertEquals(fixture.unitCount, index.units.size)
    }

    @Test
    fun `sequential reading matches the web reader`() {
        val index = assertNotNull(repository.index(fixture.bookId))
        var progress = BookProgress(bookId = fixture.bookId)
        for (step in fixture.sequential) {
            progress = ProgressSemantics.advance(progress, index, step.after, NOW)
            assertEquals(
                step.readUnitCount,
                ProgressSemantics.readUnitCount(progress, index),
                "read-unit count after ${step.after}",
            )
            assertEquals(step.paragraphId, progress.paragraphId, "resume paragraph after ${step.after}")
        }
        assertTrue(ProgressSemantics.isComplete(progress, index))
    }

    @Test
    fun `shuffled reading keeps the same counts as the web reader`() {
        val index = assertNotNull(repository.index(fixture.bookId))
        val orders = fixture.shuffled.groupBy { it.order ?: "unknown" }
        assertEquals(2, orders.size)
        for ((order, steps) in orders) {
            var progress = BookProgress(bookId = fixture.bookId)
            for (step in steps) {
                progress = ProgressSemantics.advance(progress, index, step.after, NOW)
                assertEquals(
                    step.readUnitCount,
                    ProgressSemantics.readUnitCount(progress, index),
                    "$order: read-unit count after ${step.after}",
                )
                assertEquals(step.paragraphId, progress.paragraphId, "$order: resume paragraph")
            }
        }
    }

    @Test
    fun `restore drops unknown ids and duplicates like the web reader`() {
        val index = assertNotNull(repository.index(fixture.bookId))
        for (case in fixture.restore) {
            val saved = case.saved
            val restored = ProgressSemantics.restore(
                saved?.let {
                    BookProgress(
                        bookId = fixture.bookId,
                        paragraphId = it.paragraphId,
                        readUnitIds = it.readUnitIds,
                    )
                },
                index,
            )
            assertEquals(case.expectedReadUnitIds, restored.readUnitIds)
            assertEquals(case.expectedParagraphId, restored.paragraphId)
        }
    }

    @Serializable
    private data class Fixture(
        val schemaVersion: Int = 1,
        val bookId: String,
        val unitCount: Int,
        val sequential: List<Step> = emptyList(),
        val shuffled: List<Step> = emptyList(),
        val restore: List<RestoreCase> = emptyList(),
    )

    @Serializable
    private data class Step(
        val order: String? = null,
        val after: String,
        val readUnitCount: Int,
        val paragraphId: String? = null,
    )

    @Serializable
    private data class RestoreCase(
        val saved: SavedProgress? = null,
        val expectedParagraphId: String? = null,
        val expectedReadUnitIds: List<String> = emptyList(),
    )

    @Serializable
    private data class SavedProgress(
        val schemaVersion: Int = 1,
        val paragraphId: String? = null,
        val readUnitIds: List<String> = emptyList(),
    )
}
