package com.pagelence.presentation.achievements

import com.pagelence.core.content.FileContentRepository
import com.pagelence.core.progress.AchievementId
import com.pagelence.core.progress.BookProgress
import com.pagelence.core.progress.CefrLevel
import com.pagelence.core.progress.DailyActivity
import com.pagelence.core.progress.InMemoryAnnotationRepository
import com.pagelence.core.progress.InMemoryProgressRepository
import com.pagelence.core.progress.LevelSource
import com.pagelence.core.progress.ReadingProfile
import com.pagelence.core.progress.ReadingProfileRepository
import com.pagelence.core.wordcards.InMemoryWordCardRepository
import com.pagelence.uim3.UiLanguage
import com.pagelence.uim3.stringsFor
import java.nio.file.Path
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AchievementPresenterTest {
    private val contentDir: Path = Path.of(
        System.getProperty("pagelence.contentDir") ?: "../content",
    ).toAbsolutePath().normalize()

    private val content = FileContentRepository(contentDir)
    private val aliceId = "alice-1866"
    private val today = LocalDate.of(2026, 11, 22)

    private fun profileRepo(
        activity: Map<String, DailyActivity> = emptyMap(),
        goalMinutes: Int = 10,
    ): ReadingProfileRepository = object : ReadingProfileRepository {
        override fun profile() = ReadingProfile(
            dailyActivity = activity,
            dailyGoalMinutes = goalMinutes,
        )

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
        profiles: ReadingProfileRepository = profileRepo(),
        cards: InMemoryWordCardRepository = InMemoryWordCardRepository(),
        annotations: InMemoryAnnotationRepository = InMemoryAnnotationRepository(),
    ) = AchievementPresenter(
        content = content,
        progress = progress,
        profiles = profiles,
        cards = cards,
        annotations = annotations,
        strings = stringsFor(UiLanguage.ZH),
        today = { today },
    )

    private fun finishedAlice() = InMemoryProgressRepository(
        mapOf(
            aliceId to BookProgress(
                bookId = aliceId,
                readUnitIds = content.index(aliceId)!!.units.map { it.id },
                finishedAt = "2026-11-22T02:00:00Z",
            ),
        ),
    )

    @Test
    fun `an empty state shows the whole catalogue locked`() {
        val wall = presenter().wall()

        assertEquals(AchievementId.entries.size, wall.total)
        assertEquals(0, wall.unlocked)
        assertTrue(wall.items.none { it.unlocked })
        assertTrue(wall.items.all { it.title.isNotBlank() && it.description.isNotBlank() })
    }

    @Test
    fun `a finished B1 book unlocks first book and the level achievement`() {
        val wall = presenter(progress = finishedAlice()).wall()
        val unlocked = wall.items.filter { it.unlocked }.map { it.id }

        assertTrue(AchievementId.FIRST_BOOK.name in unlocked)
        assertTrue(AchievementId.LEVEL_B1.name in unlocked)
        assertEquals("2026-11-22", wall.items.first { it.id == AchievementId.FIRST_BOOK.name }.unlockedOn)
    }

    @Test
    fun `forBook highlights what this finish reached`() {
        val result = presenter(progress = finishedAlice()).forBook(aliceId)

        assertTrue(AchievementId.FIRST_BOOK.name in result.newIds)
        assertTrue(AchievementId.LEVEL_B1.name in result.newIds)
        assertTrue(AchievementId.THREE_BOOKS.name !in result.newIds)
        assertEquals(AchievementId.entries.size, result.wall.total)
    }

    @Test
    fun `a book still in progress does not count as finished`() {
        val partial = InMemoryProgressRepository(
            mapOf(
                aliceId to BookProgress(
                    bookId = aliceId,
                    readUnitIds = content.index(aliceId)!!.units.take(3).map { it.id },
                ),
            ),
        )

        val wall = presenter(progress = partial).wall()

        assertTrue(wall.items.none { it.id == AchievementId.FIRST_BOOK.name && it.unlocked })
        assertEquals("3 / 100", wall.items.first { it.id == AchievementId.HUNDRED_UNITS.name }.progressLabel)
    }

    @Test
    fun `locked items carry a progress label`() {
        val wall = presenter().wall()
        val units = wall.items.first { it.id == AchievementId.HUNDRED_UNITS.name }

        assertEquals("0 / 100", units.progressLabel)
        assertEquals(
            "0.0 / 10 小时",
            wall.items.first { it.id == AchievementId.READING_HOURS_10.name }.progressLabel,
        )
    }
}
