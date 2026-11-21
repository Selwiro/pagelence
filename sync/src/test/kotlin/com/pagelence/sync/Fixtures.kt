package com.pagelence.sync

import com.pagelence.core.progress.BookProgress
import com.pagelence.core.progress.DailyActivity
import com.pagelence.core.progress.ReadingState
import com.pagelence.core.wordcards.WordCard

/** Small builders so each test states only the fields it cares about. */
object Fixtures {
    const val NOW = "2026-11-21T10:00:00Z"
    const val TODAY = "2026-11-21"

    fun state(
        deviceId: String = "device-a",
        updatedAt: String = "2026-11-20T10:00:00Z",
        books: Map<String, BookProgress> = emptyMap(),
        cards: List<WordCard> = emptyList(),
        activity: Map<String, DailyActivity> = emptyMap(),
    ): ReadingState = ReadingState(
        deviceId = deviceId,
        updatedAt = updatedAt,
        books = books,
        wordCards = cards,
        dailyActivity = activity,
    )

    fun book(
        id: String = "book",
        units: List<String> = listOf("u1"),
        paragraphId: String? = "p1",
        lastReadAt: String? = "2026-11-20T09:00:00Z",
    ): BookProgress = BookProgress(
        bookId = id,
        paragraphId = paragraphId,
        readUnitIds = units,
        lastReadAt = lastReadAt,
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
