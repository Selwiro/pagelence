package com.pagelence.presentation.shell

import com.pagelence.core.content.FileContentRepository
import com.pagelence.core.prefs.FontSize
import com.pagelence.core.prefs.Language
import com.pagelence.core.prefs.Preferences
import com.pagelence.core.progress.BookProgress
import com.pagelence.core.progress.InMemoryProgressRepository
import com.pagelence.core.wordcards.InMemoryWordCardRepository
import com.pagelence.core.wordcards.WordCard
import com.pagelence.uim3.UiLanguage
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
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
