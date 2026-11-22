package com.pagelence.presentation.reader

import com.pagelence.core.content.FileContentRepository
import com.pagelence.core.progress.CefrLevel
import com.pagelence.core.progress.InMemoryProgressRepository
import com.pagelence.core.progress.LevelSource
import com.pagelence.core.progress.ReaderPhase
import com.pagelence.core.progress.ReadingProfile
import com.pagelence.core.progress.ReadingProfileRepository
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ReaderSessionTest {
    private val contentDir: Path = Path.of(
        System.getProperty("pagelence.contentDir") ?: "../content",
    ).toAbsolutePath().normalize()

    private val bookId = "doctor-dolittle-1920"
    private val repository = FileContentRepository(contentDir)
    private val index = assertNotNull(repository.index(bookId))
    private val firstUnit = index.units.first()
    private val firstParagraph = firstUnit.paragraphs.first().id

    private class FakeProfile : ReadingProfileRepository {
        var completedUnits = 0
            private set
        private var current = ReadingProfile()

        override fun profile(): ReadingProfile = current

        override fun setLevel(
            level: CefrLevel?,
            source: LevelSource,
            score: Int?,
            updatedAt: String,
        ): ReadingProfile = current

        override fun recordReadingDay(date: String): ReadingProfile = current

        override fun recordCompletedUnit(date: String): ReadingProfile {
            completedUnits += 1
            return current
        }
    }

    private fun session(progress: InMemoryProgressRepository, profile: FakeProfile) =
        ReaderSession(progress = progress, profile = profile, now = { "2026-11-22T00:00:00Z" })

    private fun point(
        unitId: String = firstUnit.id,
        phase: ReaderPhase = ReaderPhase.TEXT,
        paragraphId: String? = firstParagraph,
    ) = ResumePoint(
        bookId = bookId,
        unitId = unitId,
        phase = phase,
        textPage = 1,
        textProgress = 0.5f,
        paragraphId = paragraphId,
    )

    @Test
    fun `a page turn saves the position without finishing the unit`() {
        val progress = InMemoryProgressRepository()
        val profile = FakeProfile()

        val outcome = session(progress, profile).save(point(), index)

        val saved = progress.progress(bookId)
        assertEquals(firstUnit.id, saved.reader?.unitId)
        assertEquals(1, saved.reader?.textPage)
        assertEquals(firstParagraph, saved.paragraphId)
        assertTrue(saved.readUnitIds.isEmpty())
        assertFalse(outcome.unitFinished)
        assertFalse(outcome.wroteFullDocument)
        assertEquals(0, profile.completedUnits)
    }

    @Test
    fun `reaching the grammar page finishes the unit and records the day`() {
        val progress = InMemoryProgressRepository()
        val profile = FakeProfile()

        val outcome = session(progress, profile)
            .save(point(phase = ReaderPhase.GRAMMAR), index, completedAt = "2026-11-22T01:00:00Z")

        val saved = progress.progress(bookId)
        assertTrue(firstUnit.id in saved.readUnitIds)
        assertTrue(saved.reader?.grammarReached == true)
        assertTrue(outcome.unitFinished)
        assertTrue(outcome.wroteFullDocument)
        assertEquals(1, profile.completedUnits)
        assertNull(saved.finishedAt, "one unit out of many is not a finished book")
    }

    @Test
    fun `a passive grammar save never stamps a finish date`() {
        val progress = InMemoryProgressRepository()
        val profile = FakeProfile()

        session(progress, profile).save(point(phase = ReaderPhase.GRAMMAR), index, completedAt = null)

        assertNull(progress.progress(bookId).finishedAt)
        assertTrue(progress.progress(bookId).readUnitIds.contains(firstUnit.id))
    }

    @Test
    fun `finishing the last unit marks the book complete`() {
        val progress = InMemoryProgressRepository(emptyMap())
        val profile = FakeProfile()
        val lastUnit = index.units.last()
        val before = index.units.dropLast(1).fold(progress.progress(bookId)) { acc, unit ->
            acc.copy(readUnitIds = acc.readUnitIds + unit.id)
        }
        progress.save(before)

        val outcome = session(progress, profile).save(
            point(unitId = lastUnit.id, phase = ReaderPhase.GRAMMAR, paragraphId = null),
            index,
            completedAt = "2026-11-22T02:00:00Z",
        )

        val saved = progress.progress(bookId)
        assertEquals("2026-11-22T02:00:00Z", saved.finishedAt)
        assertTrue(outcome.bookFinished)
    }
}
