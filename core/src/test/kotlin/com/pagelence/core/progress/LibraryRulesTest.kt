package com.pagelence.core.progress

import kotlin.test.Test
import kotlin.test.assertEquals

class LibraryRulesTest {
    private fun entry(
        id: String,
        readUnits: Int = 0,
        hasResume: Boolean = false,
        finished: Boolean = false,
    ) = LibraryEntry(id, readUnits, hasResume, finished)

    @Test
    fun `an unstarted book added to the shelf shows up`() {
        val visible = LibraryRules.visible(
            listOf(entry("alice-1866"), entry("doctor-dolittle-1920")),
            shelfIds = setOf("alice-1866"),
        )
        assertEquals(listOf("alice-1866"), visible.map { it.bookId })
    }

    @Test
    fun `a started book shows up without being added`() {
        val visible = LibraryRules.visible(
            listOf(entry("alice-1866", readUnits = 2), entry("doctor-dolittle-1920")),
            shelfIds = emptySet(),
        )
        assertEquals(listOf("alice-1866"), visible.map { it.bookId })
    }

    @Test
    fun `a resume position is enough to keep the book on the shelf`() {
        val visible = LibraryRules.visible(
            listOf(entry("alice-1866", hasResume = true)),
            shelfIds = emptySet(),
        )
        assertEquals(listOf("alice-1866"), visible.map { it.bookId })
    }

    @Test
    fun `finished books leave the shelf even when added`() {
        val visible = LibraryRules.visible(
            listOf(
                entry("alice-1866", readUnits = 104, finished = true),
                entry("doctor-dolittle-1920", readUnits = 3),
            ),
            shelfIds = setOf("alice-1866"),
        )
        assertEquals(listOf("doctor-dolittle-1920"), visible.map { it.bookId })
    }
}
