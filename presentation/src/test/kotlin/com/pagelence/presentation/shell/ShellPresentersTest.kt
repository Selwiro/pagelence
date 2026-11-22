package com.pagelence.presentation.shell

import com.pagelence.core.content.FileContentRepository
import com.pagelence.core.prefs.FontSize
import com.pagelence.core.prefs.Language
import com.pagelence.core.prefs.Preferences
import com.pagelence.core.progress.BookProgress
import com.pagelence.core.progress.InMemoryProgressRepository
import com.pagelence.core.wordcards.InMemoryWordCardRepository
import com.pagelence.core.wordcards.CardMastery
import com.pagelence.core.wordcards.WordCard
import com.pagelence.uim3.UiLanguage
import java.nio.file.Path
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ShellPresentersTest {
    private val contentDir: Path = Path.of(
        System.getProperty("pagelence.contentDir") ?: "../content",
    ).toAbsolutePath().normalize()
    private val content = FileContentRepository(contentDir)
    private val bookId = "doctor-dolittle-1920"

    @Test
    fun `settings state carries preferences and maps the language`() {
        val prefs = Preferences(language = Language.EN, fontSize = FontSize.LARGE)

        val state = SettingsPresenter(prefs).state()

        assertEquals(UiLanguage.EN, state.language)
        assertEquals(FontSize.LARGE, state.fontSize)
    }

    @Test
    fun `only cards of shipped books are listed`() {
        val cards = InMemoryWordCardRepository()
        cards.add(card("kept", bookId))
        cards.add(card("ghost", "removed-book"))

        val rows = WordCardPresenter(content, cards, UiLanguage.ZH).cards()

        assertEquals(listOf("kept"), rows.map { it.term })
        assertEquals("怪医杜立德", rows.single().bookTitle)
    }

    @Test
    fun `review splits today's queue into due and new cards`() {
        val cards = InMemoryWordCardRepository()
        cards.add(card("fresh", bookId))
        cards.add(card("overdue", bookId).copy(dueAt = "2026-11-19", reviews = 2, intervalDays = 3))
        cards.add(card("today", bookId).copy(dueAt = "2026-11-22", reviews = 1, intervalDays = 1))
        cards.add(card("later", bookId).copy(dueAt = "2026-12-01", reviews = 3, intervalDays = 8))
        cards.add(card("deleted", bookId).copy(deletedAt = "2026-11-21T00:00:00Z"))

        val review = WordCardPresenter(content, cards, UiLanguage.ZH) { LocalDate.of(2026, 11, 22) }.review()

        assertEquals(2, review.dueCount)
        assertEquals(1, review.newCount)
        assertEquals(listOf("overdue", "today", "fresh"), review.queue.map { it.term })
        assertEquals(4, review.rows.size, "the list still shows the future card")
        assertTrue(review.hasWork)
        assertEquals(CardMastery.NEW, review.queue.last().mastery)
        assertEquals("2026-11-19", review.queue.first().dueAt)
    }

    @Test
    fun `a queue with nothing due shows no work`() {
        val cards = InMemoryWordCardRepository()
        cards.add(card("later", bookId).copy(dueAt = "2026-12-01", reviews = 3, intervalDays = 8))

        val review = WordCardPresenter(content, cards, UiLanguage.ZH) { LocalDate.of(2026, 11, 22) }.review()

        assertEquals(0, review.dueCount)
        assertEquals(0, review.newCount)
        assertFalse(review.hasWork)
        assertTrue(review.queue.isEmpty())
    }

    @Test
    fun `a finished book reports its counts and finish date`() {
        val index = assertNotNull(content.index(bookId))
        val progress = InMemoryProgressRepository(
            mapOf(
                bookId to BookProgress(
                    bookId = bookId,
                    readUnitIds = index.units.map { it.id },
                    finishedAt = "2026-11-22T02:00:00Z",
                ),
            ),
        )

        val page = assertNotNull(
            FinishedPresenter(content, progress, InMemoryWordCardRepository(), UiLanguage.ZH)
                .finished(bookId),
        )

        assertEquals(index.units.size, page.unitCount)
        assertTrue(page.wordCount > 0)
        assertTrue(!page.finishedOn.isNullOrBlank())
    }

    private fun card(term: String, bookId: String) = WordCard(
        id = "$bookId::$term",
        bookId = bookId,
        paragraphId = "p",
        hintId = "h",
        term = term,
        lemma = term,
        createdAt = "2026-11-22T00:00:00Z",
        updatedAt = "2026-11-22T00:00:00Z",
    )
}
