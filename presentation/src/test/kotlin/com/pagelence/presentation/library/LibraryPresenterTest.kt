package com.pagelence.presentation.library

import com.pagelence.core.content.Book
import com.pagelence.core.content.BookSummary
import com.pagelence.core.content.ContentIndex
import com.pagelence.core.content.ContentRepository
import com.pagelence.core.content.FileContentRepository
import com.pagelence.core.content.GrammarPoint
import com.pagelence.core.progress.BookProgress
import com.pagelence.core.progress.CefrLevel
import com.pagelence.core.progress.InMemoryProgressRepository
import com.pagelence.core.progress.LevelSource
import com.pagelence.core.progress.ReadingProfile
import com.pagelence.core.progress.ReadingProfileRepository
import com.pagelence.uim3.UiLanguage
import com.pagelence.uim3.stringsFor
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class LibraryPresenterTest {
    private val contentDir: Path = Path.of(
        System.getProperty("pagelence.contentDir") ?: "../content",
    ).toAbsolutePath().normalize()

    private val bookId = "doctor-dolittle-1920"
    private val content = FileContentRepository(contentDir)

    private val emptyProfile = object : ReadingProfileRepository {
        override fun profile() = ReadingProfile()

        override fun setLevel(
            level: CefrLevel?,
            source: LevelSource,
            score: Int?,
            updatedAt: String,
        ) = profile()

        override fun recordReadingDay(date: String) = profile()

        override fun recordCompletedUnit(date: String) = profile()
    }

    private fun presenter(
        progress: InMemoryProgressRepository = InMemoryProgressRepository(),
        repository: ContentRepository = content,
    ) = LibraryPresenter(
        content = repository,
        progress = progress,
        profiles = emptyProfile,
        strings = stringsFor(UiLanguage.ZH),
        language = UiLanguage.ZH,
    )

    @Test
    fun `the shelf shows a started book with its resume title`() {
        val progress = InMemoryProgressRepository(
            mapOf(
                bookId to BookProgress(
                    bookId = bookId,
                    paragraphId = "chapter-01-p003",
                    readUnitIds = listOf("chapter-01-u01"),
                ),
            ),
        )

        val shelf = presenter(progress).shelf(emptySet())

        val card = assertNotNull(shelf.books.firstOrNull { it.id == bookId })
        assertEquals(1, card.readUnits)
        assertNotNull(card.readingUnitTitle)
        assertEquals(103, card.unitCount)
    }

    @Test
    fun `a book with a broken package is skipped instead of failing the shelf`() {
        val summary = BookSummary(id = "broken", path = "books/broken/book.json", units = 1)
        val broken = object : ContentRepository {
            override fun library() = listOf(summary)
            override fun book(bookId: String): Book? = error("broken book")
            override fun index(bookId: String): ContentIndex? = error("broken book")
            override fun outline(bookId: String) = null
            override fun grammarPoint(id: String): GrammarPoint? = null
            override fun grammarPoints(): List<GrammarPoint> = emptyList()
            override fun lookup(
                bookId: String,
                unitId: String,
                paragraphId: String,
                sentenceIndex: Int,
                spelling: String,
            ) = null
        }
        val skipped = mutableListOf<String>()

        val shelf = presenter(repository = broken).shelf(emptySet(), skipped)

        assertTrue(shelf.books.isEmpty())
        assertEquals(listOf("broken"), skipped)
    }

    @Test
    fun `profile totals count shipped units and read units`() {
        val progress = InMemoryProgressRepository(
            mapOf(
                bookId to BookProgress(
                    bookId = bookId,
                    readUnitIds = listOf("chapter-01-u01", "chapter-01-u02"),
                ),
            ),
        )

        val profile = presenter(progress).profile()

        assertEquals(207, profile.totalUnits)
        assertEquals(2, profile.totalReadUnits)
        assertEquals(0, profile.finishedBooks)
    }
}
