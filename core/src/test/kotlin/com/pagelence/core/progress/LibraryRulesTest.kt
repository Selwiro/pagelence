package com.pagelence.core.progress

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LibraryRulesTest {
    private fun entry(
        id: String,
        readUnits: Int = 0,
        hasResume: Boolean = false,
        finished: Boolean = false,
        lastReadAt: String? = null,
        finishedAt: String? = null,
    ) = LibraryEntry(
        bookId = id,
        readUnits = readUnits,
        hasResume = hasResume,
        finished = finished,
        lastReadAt = lastReadAt,
        finishedAt = finishedAt,
    )

    @Test
    fun `an unstarted book added to the shelf shows up in reading`() {
        val sections = LibraryRules.sections(
            listOf(entry("alice-1866"), entry("doctor-dolittle-1920")),
            shelfIds = setOf("alice-1866"),
        )
        assertEquals(listOf("alice-1866"), sections.reading.map { it.bookId })
        assertTrue(sections.finished.isEmpty())
    }

    @Test
    fun `a started book shows up without being added`() {
        val sections = LibraryRules.sections(
            listOf(entry("alice-1866", readUnits = 2), entry("doctor-dolittle-1920")),
            shelfIds = emptySet(),
        )
        assertEquals(listOf("alice-1866"), sections.reading.map { it.bookId })
    }

    @Test
    fun `a resume position is enough to keep the book on the shelf`() {
        val sections = LibraryRules.sections(
            listOf(entry("alice-1866", hasResume = true)),
            shelfIds = emptySet(),
        )
        assertEquals(listOf("alice-1866"), sections.reading.map { it.bookId })
    }

    @Test
    fun `a finished book moves to the finished group instead of disappearing`() {
        val sections = LibraryRules.sections(
            listOf(
                entry(
                    "alice-1866",
                    readUnits = 104,
                    finished = true,
                    finishedAt = "2026-11-21T10:00:00Z",
                ),
                entry("doctor-dolittle-1920", readUnits = 3),
            ),
            shelfIds = setOf("alice-1866"),
        )
        assertEquals(listOf("doctor-dolittle-1920"), sections.reading.map { it.bookId })
        assertEquals(listOf("alice-1866"), sections.finished.map { it.bookId })
    }

    @Test
    fun `reading is newest first and finished is newest finish first`() {
        val sections = LibraryRules.sections(
            listOf(
                entry("old", readUnits = 1, lastReadAt = "2026-11-01T00:00:00Z"),
                entry("new", readUnits = 1, lastReadAt = "2026-11-20T00:00:00Z"),
                entry(
                    "early-finished",
                    readUnits = 100,
                    finished = true,
                    finishedAt = "2026-11-10T00:00:00Z",
                ),
                entry(
                    "late-finished",
                    readUnits = 100,
                    finished = true,
                    finishedAt = "2026-11-21T00:00:00Z",
                ),
                // Finished before `finishedAt` existed: falls back to lastReadAt.
                entry(
                    "legacy-finished",
                    readUnits = 100,
                    finished = true,
                    lastReadAt = "2026-11-15T00:00:00Z",
                ),
                entry("never-touched"),
            ),
            shelfIds = emptySet(),
        )
        assertEquals(listOf("new", "old"), sections.reading.map { it.bookId })
        assertEquals(
            listOf("late-finished", "legacy-finished", "early-finished"),
            sections.finished.map { it.bookId },
        )
    }

    @Test
    fun `only an empty shelf is empty`() {
        assertTrue(LibraryRules.sections(emptyList(), emptySet()).isEmpty)
        val finishedOnly = LibraryRules.sections(
            listOf(entry("alice-1866", readUnits = 104, finished = true)),
            shelfIds = emptySet(),
        )
        assertTrue(!finishedOnly.isEmpty)
    }
}
