package com.pagelence.sync

import com.pagelence.core.prefs.FontSize
import com.pagelence.core.prefs.Preferences
import com.pagelence.core.progress.DailyActivity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class StateMergeTest {
    @Test
    fun readUnitsAreUnitedKeepingLocalOrder() {
        val local = Fixtures.state(books = mapOf("b" to Fixtures.book(units = listOf("u2", "u1"))))
        val remote = Fixtures.state(
            deviceId = "device-b",
            books = mapOf("b" to Fixtures.book(units = listOf("u1", "u3"), lastReadAt = "2026-11-20T08:00:00Z")),
        )
        val merged = StateMerge.merge(local, remote, base = null, Fixtures.NOW, Fixtures.TODAY).state
        assertEquals(listOf("u2", "u1", "u3"), merged.books.getValue("b").readUnitIds)
    }

    @Test
    fun laterReadPositionWins() {
        val local = Fixtures.state(
            books = mapOf("b" to Fixtures.book(units = listOf("u1"), paragraphId = "p1", lastReadAt = "2026-11-20T09:00:00Z")),
        )
        val remote = Fixtures.state(
            deviceId = "device-b",
            books = mapOf("b" to Fixtures.book(units = listOf("u2"), paragraphId = "p9", lastReadAt = "2026-11-21T09:00:00Z")),
        )
        val merged = StateMerge.merge(local, remote, base = null, Fixtures.NOW, Fixtures.TODAY).state
        val book = merged.books.getValue("b")
        assertEquals("p9", book.paragraphId)
        assertEquals("2026-11-21T09:00:00Z", book.lastReadAt)
        assertEquals(listOf("u1", "u2"), book.readUnitIds)
    }

    @Test
    fun newerCardWinsAndTombstonePropagates() {
        val local = Fixtures.state(
            cards = listOf(
                Fixtures.card(id = "b::p1::h1", updatedAt = "2026-11-20T09:00:00Z"),
                Fixtures.card(id = "b::p1::h2", updatedAt = "2026-11-20T09:00:00Z", term = "gone"),
            ),
        )
        val remote = Fixtures.state(
            deviceId = "device-b",
            cards = listOf(
                Fixtures.card(id = "b::p1::h1", updatedAt = "2026-11-21T09:00:00Z", term = "newer"),
                Fixtures.card(
                    id = "b::p1::h2",
                    updatedAt = "2026-11-21T08:00:00Z",
                    deletedAt = "2026-11-21T08:00:00Z",
                    term = "gone",
                ),
            ),
        )
        val result = StateMerge.merge(local, remote, base = null, Fixtures.NOW, Fixtures.TODAY)
        val cards = result.state.wordCards.associateBy { it.id }
        assertEquals("newer", cards.getValue("b::p1::h1").term)
        assertTrue(cards.getValue("b::p1::h2").isDeleted)
        assertEquals(1, result.summary.cardsDeleted)
    }

    @Test
    fun dailyActivityTakesPerDayMaximum() {
        val local = Fixtures.state(activity = mapOf("2026-11-20" to DailyActivity(1)))
        val remote = Fixtures.state(
            deviceId = "device-b",
            activity = mapOf("2026-11-20" to DailyActivity(3), "2026-11-19" to DailyActivity(2)),
        )
        val merged = StateMerge.merge(local, remote, base = null, Fixtures.NOW, Fixtures.TODAY).state
        assertEquals(3, merged.dailyActivity.getValue("2026-11-20").completedUnits)
        assertEquals(2, merged.dailyActivity.getValue("2026-11-19").completedUnits)
    }

    @Test
    fun preferencesUseBaseToTellWhichSideChanged() {
        val base = Fixtures.state(updatedAt = "2026-11-19T00:00:00Z")
        val localPrefs = base.preferences.copy(fontSize = FontSize.LARGE)
        val local = base.copy(preferences = localPrefs, updatedAt = "2026-11-20T00:00:00Z")
        val remote = base.copy(
            deviceId = "device-b",
            preferences = base.preferences,
            updatedAt = "2026-11-20T00:00:00Z",
        )
        val merged = StateMerge.merge(local, remote, base, Fixtures.NOW, Fixtures.TODAY).state
        assertEquals(localPrefs, merged.preferences)
    }

    @Test
    fun bothPreferencesChangedLaterDocumentWins() {
        val base = Fixtures.state(updatedAt = "2026-11-19T00:00:00Z")
        val local = base.copy(
            preferences = base.preferences.copy(fontSize = FontSize.LARGE),
            updatedAt = "2026-11-20T00:00:00Z",
        )
        val remote = base.copy(
            deviceId = "device-b",
            preferences = base.preferences.copy(fontSize = FontSize.SMALL),
            updatedAt = "2026-11-21T00:00:00Z",
        )
        val merged = StateMerge.merge(local, remote, base, Fixtures.NOW, Fixtures.TODAY).state
        assertEquals(FontSize.SMALL, merged.preferences.fontSize)
    }

    @Test
    fun noContentChangeKeepsLocalUpdatedAt() {
        val local = Fixtures.state(books = mapOf("b" to Fixtures.book()))
        val remote = Fixtures.state(deviceId = "device-b", books = mapOf("b" to Fixtures.book()))
        val result = StateMerge.merge(local, remote, base = null, Fixtures.NOW, Fixtures.TODAY)
        assertEquals(local.updatedAt, result.state.updatedAt)
    }

}
