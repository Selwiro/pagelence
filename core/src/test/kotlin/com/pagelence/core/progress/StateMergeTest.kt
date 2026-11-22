package com.pagelence.core.progress

import com.pagelence.core.lookup.LookupEntry
import com.pagelence.core.lookup.LookupHistoryRules
import com.pagelence.core.prefs.FontSize
import com.pagelence.core.prefs.Preferences
import com.pagelence.core.wordcards.WordCard
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Merge rules live in `:core` because both WebDAV sync and the local
 * export/import path must apply exactly the same table.
 */
class StateMergeTest {
    private object Fixture {
        const val NOW = "2026-11-21T10:00:00Z"
        const val TODAY = "2026-11-21"

        fun state(
            deviceId: String = "device-a",
            updatedAt: String = "2026-11-20T10:00:00Z",
            books: Map<String, BookProgress> = emptyMap(),
            cards: List<WordCard> = emptyList(),
            activity: Map<String, DailyActivity> = emptyMap(),
            selfLevel: CefrLevel? = null,
            levelUpdatedAt: String? = null,
            lookups: List<LookupEntry> = emptyList(),
        ): ReadingState = ReadingState(
            deviceId = deviceId,
            updatedAt = updatedAt,
            books = books,
            wordCards = cards,
            dailyActivity = activity,
            selfLevel = selfLevel,
            levelUpdatedAt = levelUpdatedAt,
            lookupHistory = lookups,
        )

        fun lookup(term: String, at: String, sentenceNumber: Int = 1): LookupEntry = LookupEntry(
            id = LookupHistoryRules.idOf("book", term, term),
            bookId = "book",
            unitId = "u1",
            paragraphId = "p1",
            sentenceNumber = sentenceNumber,
            term = term,
            lemma = term,
            lookedUpAt = at,
        )

        fun book(
            id: String = "book",
            units: List<String> = listOf("u1"),
            paragraphId: String? = "p1",
            lastReadAt: String? = "2026-11-20T09:00:00Z",
            finishedAt: String? = null,
        ): BookProgress = BookProgress(
            bookId = id,
            paragraphId = paragraphId,
            readUnitIds = units,
            lastReadAt = lastReadAt,
            finishedAt = finishedAt,
        )

        fun card(
            id: String = "book::p1::h1",
            updatedAt: String = "2026-11-20T09:00:00Z",
            deletedAt: String? = null,
            term: String = "word",
        ): WordCard = WordCard(
            id = id,
            bookId = id.substringBefore("::"),
            paragraphId = "p1",
            hintId = "h1",
            term = term,
            lemma = term.lowercase(),
            createdAt = "2026-11-19T09:00:00Z",
            updatedAt = updatedAt,
            deletedAt = deletedAt,
        )
    }

    @Test
    fun readUnitsAreUnitedKeepingLocalOrder() {
        val local = Fixture.state(books = mapOf("b" to Fixture.book(units = listOf("u2", "u1"))))
        val remote = Fixture.state(
            deviceId = "device-b",
            books = mapOf("b" to Fixture.book(units = listOf("u1", "u3"), lastReadAt = "2026-11-20T08:00:00Z")),
        )
        val merged = StateMerge.merge(local, remote, base = null, Fixture.NOW, Fixture.TODAY).state
        assertEquals(listOf("u2", "u1", "u3"), merged.books.getValue("b").readUnitIds)
    }

    @Test
    fun finishDateKeepsTheEarliestNonNull() {
        val local = Fixture.state(
            books = mapOf(
                "b" to Fixture.book(
                    units = listOf("u1", "u2"),
                    lastReadAt = "2026-11-21T09:00:00Z",
                    finishedAt = "2026-11-21T08:00:00Z",
                ),
            ),
        )
        val remote = Fixture.state(
            deviceId = "device-b",
            books = mapOf(
                "b" to Fixture.book(
                    units = listOf("u1"),
                    lastReadAt = "2026-11-20T09:00:00Z",
                    finishedAt = "2026-11-20T08:00:00Z",
                ),
            ),
        )
        val merged = StateMerge.merge(local, remote, base = null, Fixture.NOW, Fixture.TODAY).state
        assertEquals("2026-11-20T08:00:00Z", merged.books.getValue("b").finishedAt)

        val oneSide = StateMerge.merge(
            local.copy(books = mapOf("b" to Fixture.book(finishedAt = null))),
            remote.copy(books = mapOf("b" to Fixture.book(finishedAt = "2026-11-20T08:00:00Z"))),
            base = null,
            Fixture.NOW,
            Fixture.TODAY,
        ).state
        assertEquals("2026-11-20T08:00:00Z", oneSide.books.getValue("b").finishedAt)
    }

    @Test
    fun laterReadPositionWins() {
        val local = Fixture.state(
            books = mapOf("b" to Fixture.book(units = listOf("u1"), paragraphId = "p1", lastReadAt = "2026-11-20T09:00:00Z")),
        )
        val remote = Fixture.state(
            deviceId = "device-b",
            books = mapOf("b" to Fixture.book(units = listOf("u2"), paragraphId = "p9", lastReadAt = "2026-11-21T09:00:00Z")),
        )
        val merged = StateMerge.merge(local, remote, base = null, Fixture.NOW, Fixture.TODAY).state
        val book = merged.books.getValue("b")
        assertEquals("p9", book.paragraphId)
        assertEquals("2026-11-21T09:00:00Z", book.lastReadAt)
        assertEquals(listOf("u1", "u2"), book.readUnitIds)
    }

    @Test
    fun newerCardWinsAndTombstonePropagates() {
        val local = Fixture.state(
            cards = listOf(
                Fixture.card(id = "b::p1::h1", updatedAt = "2026-11-20T09:00:00Z"),
                Fixture.card(id = "b::p1::h2", updatedAt = "2026-11-20T09:00:00Z", term = "gone"),
            ),
        )
        val remote = Fixture.state(
            deviceId = "device-b",
            cards = listOf(
                Fixture.card(id = "b::p1::h1", updatedAt = "2026-11-21T09:00:00Z", term = "newer"),
                Fixture.card(
                    id = "b::p1::h2",
                    updatedAt = "2026-11-21T08:00:00Z",
                    deletedAt = "2026-11-21T08:00:00Z",
                    term = "gone",
                ),
            ),
        )
        val result = StateMerge.merge(local, remote, base = null, Fixture.NOW, Fixture.TODAY)
        val cards = result.state.wordCards.associateBy { it.id }
        assertEquals("newer", cards.getValue("b::p1::h1").term)
        assertTrue(cards.getValue("b::p1::h2").isDeleted)
        assertEquals(1, result.summary.cardsDeleted)
    }

    @Test
    fun tombstoneWinsEvenWhenTheOtherSideEditedLater() {
        val local = Fixture.state(
            cards = listOf(
                Fixture.card(
                    id = "b::p1::h2",
                    updatedAt = "2026-11-20T09:00:00Z",
                    deletedAt = "2026-11-20T09:00:00Z",
                ),
            ),
        )
        val remote = Fixture.state(
            deviceId = "device-b",
            cards = listOf(
                Fixture.card(id = "b::p1::h2", updatedAt = "2026-11-21T09:00:00Z", term = "resurrected"),
            ),
        )
        val merged = StateMerge.merge(local, remote, base = null, Fixture.NOW, Fixture.TODAY).state
        assertTrue(merged.wordCards.single().isDeleted)
    }

    @Test
    fun dailyActivityTakesPerDayMaximum() {
        val local = Fixture.state(activity = mapOf("2026-11-20" to DailyActivity(1)))
        val remote = Fixture.state(
            deviceId = "device-b",
            activity = mapOf("2026-11-20" to DailyActivity(3), "2026-11-19" to DailyActivity(2)),
        )
        val merged = StateMerge.merge(local, remote, base = null, Fixture.NOW, Fixture.TODAY).state
        assertEquals(3, merged.dailyActivity.getValue("2026-11-20").completedUnits)
        assertEquals(2, merged.dailyActivity.getValue("2026-11-19").completedUnits)
    }

    @Test
    fun levelUsesLevelUpdatedAtNotTheDocumentTimestamp() {
        val local = Fixture.state(
            updatedAt = "2026-11-21T09:00:00Z",
            selfLevel = CefrLevel.B1,
            levelUpdatedAt = "2026-11-20T09:00:00Z",
        )
        val remote = Fixture.state(
            deviceId = "device-b",
            updatedAt = "2026-11-20T09:00:00Z",
            selfLevel = CefrLevel.C1,
            levelUpdatedAt = "2026-11-21T08:00:00Z",
        )
        val result = StateMerge.merge(local, remote, base = null, Fixture.NOW, Fixture.TODAY)
        assertEquals(CefrLevel.C1, result.state.selfLevel)
        assertTrue(result.summary.levelFromRemote)
    }

    @Test
    fun levelFallsBackToTheDocumentWhenTimestampsAreMissing() {
        val local = Fixture.state(updatedAt = "2026-11-21T09:00:00Z", selfLevel = CefrLevel.A2)
        val remote = Fixture.state(
            deviceId = "device-b",
            updatedAt = "2026-11-20T09:00:00Z",
            selfLevel = CefrLevel.B2,
        )
        val merged = StateMerge.merge(local, remote, base = null, Fixture.NOW, Fixture.TODAY).state
        assertEquals(CefrLevel.A2, merged.selfLevel)
    }

    @Test
    fun aFreshDocumentTakesTheImportedPreferencesEvenWhenItIsNewer() {
        val local = Fixture.state(updatedAt = "2026-11-21T09:00:00Z")
        val remote = Fixture.state(deviceId = "device-b", updatedAt = "2026-11-20T09:00:00Z")
            .copy(preferences = Preferences(fontSize = FontSize.LARGE))
        val merged = StateMerge.merge(local, remote, base = null, Fixture.NOW, Fixture.TODAY).state
        assertEquals(FontSize.LARGE, merged.preferences.fontSize)
    }

    @Test
    fun levelRestoresWhenOnlyTheOtherSideHasOneAndTimestampsAreMissing() {
        val local = Fixture.state(updatedAt = "2026-11-21T09:00:00Z")
        val remote = Fixture.state(
            deviceId = "device-b",
            updatedAt = "2026-11-20T09:00:00Z",
            selfLevel = CefrLevel.B2,
        )
        val merged = StateMerge.merge(local, remote, base = null, Fixture.NOW, Fixture.TODAY).state
        assertEquals(CefrLevel.B2, merged.selfLevel)
    }

    @Test
    fun preferencesUseBaseToTellWhichSideChanged() {
        val base = Fixture.state(updatedAt = "2026-11-19T00:00:00Z")
        val localPrefs = base.preferences.copy(fontSize = FontSize.LARGE)
        val local = base.copy(preferences = localPrefs, updatedAt = "2026-11-20T00:00:00Z")
        val remote = base.copy(
            deviceId = "device-b",
            preferences = base.preferences,
            updatedAt = "2026-11-20T00:00:00Z",
        )
        val merged = StateMerge.merge(local, remote, base, Fixture.NOW, Fixture.TODAY).state
        assertEquals(localPrefs, merged.preferences)
    }

    @Test
    fun bothPreferencesChangedLaterDocumentWins() {
        val base = Fixture.state(updatedAt = "2026-11-19T00:00:00Z")
        val local = base.copy(
            preferences = base.preferences.copy(fontSize = FontSize.LARGE),
            updatedAt = "2026-11-20T00:00:00Z",
        )
        val remote = base.copy(
            deviceId = "device-b",
            preferences = base.preferences.copy(fontSize = FontSize.SMALL),
            updatedAt = "2026-11-21T00:00:00Z",
        )
        val merged = StateMerge.merge(local, remote, base, Fixture.NOW, Fixture.TODAY).state
        assertEquals(FontSize.SMALL, merged.preferences.fontSize)
    }

    @Test
    fun noContentChangeKeepsLocalUpdatedAt() {
        val local = Fixture.state(books = mapOf("b" to Fixture.book()))
        val remote = Fixture.state(deviceId = "device-b", books = mapOf("b" to Fixture.book()))
        val result = StateMerge.merge(local, remote, base = null, Fixture.NOW, Fixture.TODAY)
        assertEquals(local.updatedAt, result.state.updatedAt)
    }

    @Test
    fun annotationsMergeByTombstoneThenUpdatedAt() {
        val localNote = Annotation(
            id = "book::p1::1::note",
            bookId = "book",
            unitId = "u1",
            paragraphId = "p1",
            sentenceNumber = 1,
            kind = AnnotationKind.NOTE,
            note = "旧",
            quote = "Old.",
            createdAt = "2026-11-20T09:00:00Z",
            updatedAt = "2026-11-20T09:00:00Z",
        )
        val remoteNote = localNote.copy(note = "新", updatedAt = "2026-11-21T09:00:00Z")
        val localBookmark = Annotation(
            id = "book::p1::2::bookmark",
            bookId = "book",
            unitId = "u1",
            paragraphId = "p1",
            sentenceNumber = 2,
            kind = AnnotationKind.BOOKMARK,
            quote = "Second.",
            createdAt = "2026-11-20T09:00:00Z",
            updatedAt = "2026-11-20T09:00:00Z",
            deletedAt = "2026-11-21T09:00:00Z",
        )
        val remoteBookmark = localBookmark.copy(deletedAt = null, updatedAt = "2026-11-22T09:00:00Z")
        val result = StateMerge.merge(
            Fixture.state().copy(annotations = listOf(localNote, localBookmark)),
            Fixture.state(deviceId = "device-b").copy(annotations = listOf(remoteNote, remoteBookmark)),
            base = null,
            Fixture.NOW,
            Fixture.TODAY,
        )
        val merged = result.state.annotations.associateBy { it.id }
        assertEquals("新", merged.getValue(localNote.id).note)
        // The tombstone wins even though the other side edited it later.
        assertEquals("2026-11-21T09:00:00Z", merged.getValue(localBookmark.id).deletedAt)
        assertEquals(1, result.summary.annotationsUpdated)
        assertEquals(0, result.summary.annotationsAdded)
        assertEquals(0, result.summary.annotationsDeleted)
    }

    @Test
    fun `lookup history merges by word with the later lookup winning`() {
        val local = Fixture.state(
            deviceId = "device-a",
            lookups = listOf(
                Fixture.lookup("sea", "2026-11-20T08:00:00Z"),
                Fixture.lookup("boat", "2026-11-20T09:00:00Z"),
            ),
        )
        val remote = Fixture.state(
            deviceId = "device-b",
            lookups = listOf(
                Fixture.lookup("sea", "2026-11-20T11:00:00Z", sentenceNumber = 7),
                Fixture.lookup("bird", "2026-11-20T07:00:00Z"),
            ),
        )

        val result = StateMerge.merge(local, remote, base = null, now = Fixture.NOW, today = Fixture.TODAY)

        assertEquals(listOf("sea", "boat", "bird"), result.state.lookupHistory.map { it.term })
        assertEquals(7, result.state.lookupHistory.first().sentenceNumber)
        assertEquals(2, result.summary.lookupsChanged)
    }
}
