package com.pagelence.presentation.achievements

import com.pagelence.core.content.ContentOutline
import com.pagelence.core.content.ContentRepository
import com.pagelence.core.progress.AchievementId
import com.pagelence.core.progress.AchievementInput
import com.pagelence.core.progress.AchievementProgress
import com.pagelence.core.progress.Achievements
import com.pagelence.core.progress.AnnotationRepository
import com.pagelence.core.progress.FinishedBookFact
import com.pagelence.core.progress.ProgressRepository
import com.pagelence.core.progress.ReadingProfileRepository
import com.pagelence.core.wordcards.WordCardRepository
import com.pagelence.uim3.Strings
import com.pagelence.uim3.achievementDescription
import com.pagelence.uim3.achievementTitle
import com.pagelence.uim3.model.AchievementUi
import com.pagelence.uim3.model.AchievementWallUi
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeParseException

/** The wall plus the ids this book's completion just reached. */
data class AchievementResult(
    val wall: AchievementWallUi,
    val newIds: List<String> = emptyList(),
)

/**
 * Turns the state that already exists into the achievement wall (T163).
 *
 * Nothing is stored: the catalogue is re-evaluated on every read, so the same
 * `state.json` yields the same wall on a new device and an uninstalled/reinstalled
 * app cannot gain or lose a badge. The presenter only gathers inputs (finished
 * books with their dates and levels, the daily log, cards, annotations) and
 * localises the rules' output.
 */
class AchievementPresenter(
    private val content: ContentRepository,
    private val progress: ProgressRepository,
    private val profiles: ReadingProfileRepository,
    private val cards: WordCardRepository,
    private val annotations: AnnotationRepository,
    private val strings: Strings,
    private val today: () -> LocalDate = { LocalDate.now() },
) {
    fun wall(): AchievementWallUi = snapshot().first

    fun forBook(bookId: String): AchievementResult {
        val (wall, input) = snapshot()
        val fact = finishedFacts().firstOrNull { it.first == bookId }?.second ?: return AchievementResult(wall)
        val newIds = Achievements.reachedBy(input, fact, today())
            .map { it.name }
        return AchievementResult(wall, newIds)
    }

    private fun snapshot(): Pair<AchievementWallUi, AchievementInput> {
        val profile = profiles.profile()
        val input = AchievementInput(
            finishedBooks = finishedFacts().map { it.second },
            totalUnitsRead = totalReadUnits(),
            activity = profile.dailyActivity,
            goalMinutes = profile.dailyGoalMinutes,
            wordCardDates = cards.all().mapNotNull { parseDate(it.createdAt) },
            annotationDates = annotations.all().mapNotNull { parseDate(it.createdAt) },
        )
        val items = Achievements.ordered(Achievements.evaluate(input, today()), today())
        val wall = AchievementWallUi(
            items = items.map(::toUi),
            unlocked = items.count { it.unlocked },
            total = items.size,
        )
        return wall to input
    }

    /** Finished books, oldest finish first, with the fact the rules need. */
    private fun finishedFacts(): List<Pair<String, FinishedBookFact>> =
        content.library().mapNotNull { summary ->
            val outline = runCatching {
                content.outline(summary.id) ?: content.index(summary.id)?.let(ContentOutline::from)
            }.getOrNull() ?: return@mapNotNull null
            val saved = progress.progress(summary.id)
            val read = saved.readUnitIds.count { it in outline.unitIds }
            if (outline.units.isEmpty() || read != outline.units.size) return@mapNotNull null
            summary.id to FinishedBookFact(
                finishedOn = parseInstantDate(saved.finishedAt),
                level = summary.level,
                units = outline.units.size,
            )
        }.sortedWith(
            compareBy({ it.second.finishedOn ?: LocalDate.MAX }, { it.first }),
        )

    private fun totalReadUnits(): Int = content.library().sumOf { summary ->
        val outline = runCatching {
            content.outline(summary.id) ?: content.index(summary.id)?.let(ContentOutline::from)
        }.getOrNull() ?: return@sumOf 0
        val saved = progress.progress(summary.id)
        saved.readUnitIds.count { it in outline.unitIds }
    }

    private fun toUi(item: AchievementProgress): AchievementUi = AchievementUi(
        id = item.id.name,
        title = achievementTitle(strings, item.id),
        description = achievementDescription(strings, item.id),
        unlocked = item.unlocked,
        unlockedOn = item.unlockedOn?.toString(),
        progressLabel = when {
            item.unlocked || item.target <= 1 -> null
            // Seconds read better as hours: "0.1 / 10 小时" beats "135 / 36000".
            item.id == AchievementId.READING_HOURS_10 ->
                strings.achievementProgressHours.format(
                    item.current / 3600f,
                    item.target / 3600,
                )

            else -> strings.achievementProgress.format(item.current, item.target)
        },
        fraction = item.fraction,
    )

    private fun parseDate(value: String?): LocalDate? {
        val text = value?.takeIf { it.isNotBlank() } ?: return null
        runCatching { LocalDate.parse(text) }.getOrNull()?.let { return it }
        return parseInstantDate(text)
    }

    private fun parseInstantDate(value: String?): LocalDate? {
        val text = value?.takeIf { it.isNotBlank() } ?: return null
        return try {
            Instant.parse(text).atZone(ZoneId.systemDefault()).toLocalDate()
        } catch (_: DateTimeParseException) {
            null
        }
    }
}
