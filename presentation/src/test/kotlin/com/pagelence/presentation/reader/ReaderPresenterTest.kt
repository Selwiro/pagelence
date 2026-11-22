package com.pagelence.presentation.reader

import com.pagelence.core.content.FileContentRepository
import com.pagelence.core.prefs.Preferences
import com.pagelence.core.progress.BookProgress
import com.pagelence.core.progress.ReaderPhase
import com.pagelence.core.progress.ReaderPosition
import com.pagelence.core.wordcards.InMemoryWordCardRepository
import com.pagelence.uim3.UiLanguage
import com.pagelence.uim3.stringsFor
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ReaderPresenterTest {
    private val contentDir: Path = Path.of(
        System.getProperty("pagelence.contentDir") ?: "../content",
    ).toAbsolutePath().normalize()

    private val bookId = "doctor-dolittle-1920"
    private val content = FileContentRepository(contentDir)
    private val index = assertNotNull(content.index(bookId))
    private val firstUnit = index.units.first()

    private fun presenter(preferences: Preferences = Preferences()) = ReaderPresenter(
        content = content,
        cards = InMemoryWordCardRepository(),
        strings = stringsFor(UiLanguage.ZH),
        language = UiLanguage.ZH,
        preferences = preferences,
        bookLevel = "A2",
    )

    @Test
    fun `a unit becomes readable state with paragraphs quiz and table of contents`() {
        val state = presenter().unit(
            bookIndex = index,
            unit = firstUnit,
            progress = BookProgress(bookId = bookId),
            quizAnswers = emptyMap(),
            quizSubmitted = false,
            quizCollected = 0,
        )

        assertEquals(bookId, state.bookId)
        assertNotNull(state.unitTitle)
        assertEquals(1, state.unitPosition)
        assertEquals(index.units.size, state.unitCount)
        assertTrue(state.paragraphs.isNotEmpty())
        assertTrue(state.quiz.isNotEmpty())
        assertTrue(state.toc.isNotEmpty())
        assertEquals(0, state.quizScore)
        assertTrue(state.quizCollectibleCount <= state.quiz.size)
    }

    @Test
    fun `a stored resume point decides the phase and keeps the answers`() {
        val progress = BookProgress(
            bookId = bookId,
            readUnitIds = listOf(firstUnit.id),
            reader = ReaderPosition(
                unitId = firstUnit.id,
                phase = ReaderPhase.QUIZ,
                textPage = 2,
                quizAnswers = mapOf(0 to 1),
                quizSubmitted = false,
            ),
        )

        val state = presenter().unit(
            bookIndex = index,
            unit = firstUnit,
            progress = progress,
            quizAnswers = mapOf(0 to 1),
            quizSubmitted = false,
            quizCollected = 0,
        )

        assertEquals(ReaderPhase.QUIZ, state.resumePhase)
        assertEquals(2, state.resumeTextPage)
        assertEquals(1, state.quiz.first().selected)
        assertTrue(state.unitRead)
    }

    @Test
    fun `the guide shows only for a fresh unit that has one`() {
        val state = presenter().unit(
            bookIndex = index,
            unit = firstUnit,
            progress = BookProgress(bookId = bookId),
            quizAnswers = emptyMap(),
            quizSubmitted = false,
            quizCollected = 0,
        )

        val hasIntroduction = firstUnit.introduction?.let {
            !it.zh.isNullOrBlank() || !it.en.isNullOrBlank()
        } == true
        assertEquals(hasIntroduction, state.showGuideInitially)
        assertEquals(
            if (hasIntroduction) ReaderPhase.GUIDE else ReaderPhase.TEXT,
            state.resumePhase,
        )
    }
}
