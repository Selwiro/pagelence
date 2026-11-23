package com.pagelence.presentation.library

import com.pagelence.core.content.BookSummary
import com.pagelence.core.content.ContentOutline
import com.pagelence.core.content.ContentRepository
import com.pagelence.core.content.LocalizedText
import com.pagelence.core.progress.BookProgress
import com.pagelence.core.progress.HeatmapRules
import com.pagelence.core.progress.LibraryEntry
import com.pagelence.core.progress.LibraryRules
import com.pagelence.core.progress.ProgressRepository
import com.pagelence.core.progress.ReadingProfile
import com.pagelence.core.progress.ReadingProfileRepository
import com.pagelence.core.progress.ReadingStreakRules
import com.pagelence.uim3.Strings
import com.pagelence.uim3.UiLanguage
import com.pagelence.uim3.model.BookUi
import com.pagelence.uim3.model.HeatmapCellUi
import com.pagelence.uim3.model.HeatmapUi
import com.pagelence.uim3.model.LibraryUiState
import com.pagelence.uim3.model.ProfileUiState
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Turns content manifests, the outline and progress into the shelf and the
 * "my" page. Android-free: the phone shell and an e-ink shell share the same
 * cards, ordering and statistics.
 */
class LibraryPresenter(
    private val content: ContentRepository,
    private val progress: ProgressRepository,
    private val profiles: ReadingProfileRepository,
    private val strings: Strings,
    private val language: UiLanguage,
    private val today: () -> LocalDate = { LocalDate.now() },
) {
    /**
     * Build the two shelf groups. A book whose outline/body cannot be read is
     * added to [skipped] instead of failing the whole shelf.
     */
    fun shelf(
        shelfIds: Set<String>,
        skipped: MutableList<String> = mutableListOf(),
    ): LibraryUiState {
        val entries = content.library().mapNotNull { summary ->
            val outline = runCatching {
                content.outline(summary.id) ?: content.index(summary.id)?.let(ContentOutline::from)
            }.getOrNull()
            if (outline == null) {
                skipped += summary.id
                return@mapNotNull null
            }
            val saved = progress.progress(summary.id)
            val ui = card(summary, outline, saved)
            LibraryEntry(
                bookId = ui.id,
                readUnits = ui.readUnits,
                hasResume = ui.readingUnitTitle != null,
                finished = ui.finished,
                lastReadAt = saved.lastReadAt,
                finishedAt = saved.finishedAt,
            ) to ui
        }
        val sections = LibraryRules.sections(entries.map { it.first }, shelfIds)
        val byId = entries.associate { it.first.bookId to it.second }
        return LibraryUiState(
            books = sections.reading.mapNotNull { byId[it.bookId] },
            finished = sections.finished.mapNotNull { byId[it.bookId] },
            loading = false,
        )
    }

    /** Totals plus the heat map for the "my" page. */
    fun profile(): ProfileUiState {
        var totalUnits = 0
        var totalReadUnits = 0
        var finishedBooks = 0
        content.library().forEach { summary ->
            val outline = runCatching {
                content.outline(summary.id) ?: content.index(summary.id)?.let(ContentOutline::from)
            }.getOrNull() ?: return@forEach
            val saved = progress.progress(summary.id)
            val read = saved.readUnitIds.count { it in outline.unitIds }
            totalUnits += outline.units.size
            totalReadUnits += read
            if (outline.units.isNotEmpty() && read == outline.units.size) finishedBooks += 1
        }
        val profile = profiles.profile()
        val streak = ReadingStreakRules.summarize(
            activity = profile.dailyActivity,
            goalMinutes = profile.dailyGoalMinutes,
            today = today(),
        )
        return ProfileUiState(
            level = profile.selfLevel,
            levelSource = profile.levelSource,
            levelScore = profile.levelScore,
            levelUpdatedAt = profile.levelUpdatedAt,
            totalUnits = totalUnits,
            totalReadUnits = totalReadUnits,
            finishedBooks = finishedBooks,
            readingDays = profile.readingDays,
            heatmap = heatmap(profile),
            dailyGoalMinutes = streak.goalMinutes,
            todaySeconds = streak.todaySeconds,
            todayUnits = streak.todayUnits,
            metToday = streak.metToday,
            secondsToGoal = streak.secondsToGoal,
            currentStreak = streak.currentDays,
            longestStreak = streak.longestDays,
            metDays = streak.metDays,
        )
    }

    private fun card(
        summary: BookSummary,
        outline: ContentOutline,
        saved: BookProgress,
    ): BookUi {
        val title = outline.title ?: summary.title ?: LocalizedText(zh = summary.id, en = summary.id)
        val author = outline.author
        val read = saved.readUnitIds.count { it in outline.unitIds }
        // Prefer the paragraph anchor; a finished unit has no anchor, so fall
        // back to the resume point's unit id.
        val readingUnit = saved.paragraphId?.let(outline::unitOfParagraph)
            ?: saved.reader?.unitId?.let(outline::unit)
        val subtitle = buildList {
            author?.let(::add)
            outline.edition?.let(::add)
            add(strings.unitCountText(outline.units.size))
        }.joinToString(" · ")
        return BookUi(
            id = summary.id,
            title = titleFor(title),
            monogram = (title.zh ?: title.en ?: summary.id).take(1),
            author = author,
            coverTitle = title.en ?: title.zh ?: summary.id,
            subtitle = subtitle,
            unitCount = outline.units.size,
            readUnits = read,
            readingUnitTitle = readingUnit?.title?.let { titleFor(it) },
            finished = outline.units.isNotEmpty() && read == outline.units.size,
            finishedOn = formatDate(saved.finishedAt),
            coverPath = content.coverPath(summary.id),
        )
    }

    private fun heatmap(profile: ReadingProfile): HeatmapUi {
        val map = HeatmapRules.build(profile.dailyActivity, today())
        return HeatmapUi(
            weeks = map.weeks.map { week ->
                week.map { cell -> cell?.let { HeatmapCellUi(it.date.toString(), it.level) } }
            },
            activeDays = map.activeDays,
            totalUnits = map.totalUnits,
        )
    }

    private fun titleFor(title: LocalizedText): String = when (language) {
        UiLanguage.ZH -> title.zh ?: title.en ?: ""
        UiLanguage.EN -> title.en ?: title.zh ?: ""
    }

    private fun formatDate(value: String?): String? {
        val instant = value?.let { runCatching { Instant.parse(it) }.getOrNull() } ?: return null
        return DateTimeFormatter.ofPattern("yyyy-MM-dd")
            .withZone(ZoneId.systemDefault())
            .format(instant)
    }
}
