package com.pagelence.core.progress

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ShelfRulesTest {
    @Test
    fun `toggle adds a new book and removes an existing one`() {
        val added = ShelfRules.toggle(emptyList(), "alice-1866")
        assertEquals(listOf("alice-1866"), added)
        assertTrue(ShelfRules.contains(added, "alice-1866"))

        val removed = ShelfRules.toggle(added, "alice-1866")
        assertEquals(emptyList(), removed)
        assertFalse(ShelfRules.contains(removed, "alice-1866"))
    }

    @Test
    fun `toggle keeps the order of the other books`() {
        val shelf = listOf("doctor-dolittle-1920", "alice-1866")
        assertEquals(
            listOf("doctor-dolittle-1920", "alice-1866", "sign-of-four-1890"),
            ShelfRules.toggle(shelf, "sign-of-four-1890"),
        )
        assertEquals(
            listOf("alice-1866"),
            ShelfRules.toggle(shelf, "doctor-dolittle-1920"),
        )
    }
}
