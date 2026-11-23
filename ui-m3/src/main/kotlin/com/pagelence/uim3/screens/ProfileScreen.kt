package com.pagelence.uim3.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pagelence.core.prefs.DailyGoal
import com.pagelence.core.progress.CefrLevel
import com.pagelence.uim3.Strings
import com.pagelence.uim3.components.AchievementTrack
import com.pagelence.uim3.components.ChoiceSheet
import com.pagelence.uim3.components.LevelOptionRow
import com.pagelence.uim3.components.MetaChip
import com.pagelence.uim3.components.MetricItem
import com.pagelence.uim3.components.MetricRow
import com.pagelence.uim3.components.PageTopBar
import com.pagelence.uim3.components.PagelenceIcons
import com.pagelence.uim3.components.SettingDivider
import com.pagelence.uim3.components.SettingGroup
import com.pagelence.uim3.components.SettingRow
import com.pagelence.uim3.components.SheetOption
import com.pagelence.uim3.levelName
import com.pagelence.uim3.levelOptions
import com.pagelence.uim3.levelSourceLabel
import com.pagelence.uim3.model.AchievementWallUi
import com.pagelence.uim3.model.HeatmapCellUi
import com.pagelence.uim3.model.ProfileUiState

/**
 * 「我的」(T168): one page, one lead. Today is the only emphasized card; the
 * lifetime numbers live inside the record card together with the heat map;
 * achievements fold into a single card with a ten-slot track; the level moves
 * to a low-frequency "learning setup" group at the bottom.
 */
@Composable
fun ProfileScreen(
    strings: Strings,
    state: ProfileUiState,
    onBack: (() -> Unit)?,
    onOpenSettings: () -> Unit,
    onSelectLevel: (CefrLevel) -> Unit,
    onOpenAssessment: () -> Unit,
    onOpenAchievements: () -> Unit,
    onDailyGoal: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showLevelDialog by rememberSaveable { mutableStateOf(false) }
    var showGoalSheet by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        topBar = {
            PageTopBar(
                title = strings.profile,
                onBack = onBack,
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = strings.settings)
                    }
                },
            )
        },
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            item(key = "today") {
                TodayCard(strings, state) { showGoalSheet = true }
            }
            item(key = "record") { RecordCard(strings, state) }
            item(key = "achievements") {
                AchievementCard(strings, state.achievements, onOpenAchievements)
            }
            item(key = "study") {
                StudySettings(
                    strings = strings,
                    state = state,
                    onOpenLevel = { showLevelDialog = true },
                    onOpenAssessment = onOpenAssessment,
                )
            }
        }
    }

    if (showLevelDialog) {
        LevelDialog(
            strings = strings,
            current = state.level,
            onDismiss = { showLevelDialog = false },
            onConfirm = { level ->
                showLevelDialog = false
                onSelectLevel(level)
            },
        )
    }

    if (showGoalSheet) {
        ChoiceSheet(
            title = strings.dailyGoal,
            options = DailyGoal.OPTIONS.map { minutes ->
                SheetOption(minutes, strings.goalMinutesOption.format(minutes))
            },
            selected = DailyGoal.sanitize(state.dailyGoalMinutes),
            onDismiss = { showGoalSheet = false },
            onSelect = onDailyGoal,
            footer = strings.dailyGoalHint,
        )
    }
}

/**
 * 今日: the goal with one status line. Exceeding the goal is spelled out instead
 * of showing "15 / 10" next to a full bar, and the unit count only appears when
 * it is non-zero.
 */
@Composable
private fun TodayCard(strings: Strings, state: ProfileUiState, onEditGoal: () -> Unit) {
    val minutes = state.todaySeconds / 60
    val overMinutes = ((state.todaySeconds - state.dailyGoalMinutes * 60) / 60).coerceAtLeast(0)
    val remainingMinutes = ((state.secondsToGoal + 59) / 60).coerceAtLeast(1)
    val status = when {
        state.metToday && overMinutes > 0 -> strings.todayOverGoal.format(overMinutes)
        state.metToday -> strings.todayGoalMet
        else -> strings.todayGoalRemaining.format(remainingMinutes)
    }
    val detail = if (state.todayUnits > 0) {
        strings.joinDot.format(status, strings.todayUnitsText(state.todayUnits))
    } else {
        status
    }

    Surface(
        onClick = onEditGoal,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    strings.todayLabel,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                MetaChip(streakLabel(strings, state))
            }
            Spacer(Modifier.height(14.dp))
            Text(
                buildAnnotatedString {
                    withStyle(SpanStyle(fontSize = 38.sp, fontWeight = FontWeight.SemiBold)) {
                        append(minutes.toString())
                    }
                    withStyle(SpanStyle(fontSize = 17.sp)) {
                        append(strings.todayGoalSuffix.format(state.dailyGoalMinutes))
                    }
                },
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Spacer(Modifier.height(12.dp))
            GoalBar(fraction = state.goalFraction)
            Spacer(Modifier.height(10.dp))
            Text(
                detail,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
            )
        }
    }
}

@Composable
private fun GoalBar(fraction: Float) {
    val track = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.16f)
    Box(
        Modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(CircleShape)
            .background(track),
    ) {
        Box(
            Modifier
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .fillMaxHeight()
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary),
        )
    }
}

/**
 * 阅读记录: lifetime numbers are this card's masthead, the heat map is its body.
 * Keeping them in one container removes the bare number strip that used to sit
 * between the today card and the map.
 */
@Composable
private fun RecordCard(strings: Strings, state: ProfileUiState) {
    val weeks = state.heatmap.weeks.takeLast(HeatmapWeeks)
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    strings.readingRecord,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    strings.heatmapRange.format(HeatmapWeeks),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(14.dp))
            MetricRow(
                listOf(
                    MetricItem(
                        value = state.finishedBooks.toString(),
                        unit = if (state.finishedBooks == 1) strings.unitBook else strings.unitBooks,
                        label = strings.statFinished,
                    ),
                    MetricItem(
                        value = state.totalReadUnits.toString(),
                        unit = if (state.totalReadUnits == 1) strings.unitUnit else strings.unitUnits,
                        label = strings.unitsTotal.format(state.totalUnits),
                    ),
                    MetricItem(
                        value = state.readingDays.toString(),
                        unit = if (state.readingDays == 1) strings.unitDay else strings.unitDays,
                        label = strings.statReading,
                    ),
                ),
            )
            Spacer(Modifier.height(14.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(Modifier.height(14.dp))
            if (weeks.isEmpty() || state.heatmap.isEmpty) {
                Text(
                    strings.heatmapEmpty,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                HeatmapGrid(weeks)
            }
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    strings.heatmapCellHint,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    strings.heatmapLess,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.width(4.dp))
                repeat(5) { level -> HeatmapCellBox(level, 9.dp) }
                Spacer(Modifier.width(4.dp))
                Text(
                    strings.heatmapMore,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** 26 weeks (half a year) fit a phone width without horizontal scrolling. */
private const val HeatmapWeeks = 26

@Composable
private fun HeatmapGrid(weeks: List<List<HeatmapCellUi?>>) {
    val gap = 2.dp
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val cell = ((maxWidth - gap * (weeks.size - 1)) / weeks.size).coerceIn(4.dp, 12.dp)
        Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
            weeks.forEach { week ->
                Column(verticalArrangement = Arrangement.spacedBy(gap)) {
                    repeat(7) { day ->
                        val level = week.getOrNull(day)?.level
                        if (level == null) {
                            Box(Modifier.size(cell))
                        } else {
                            HeatmapCellBox(level, cell)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HeatmapCellBox(level: Int, size: Dp) {
    val color = if (level == 0) {
        MaterialTheme.colorScheme.surfaceContainerHighest
    } else {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.18f + 0.2f * (level - 1))
    }
    Surface(
        modifier = Modifier.size(size),
        shape = RoundedCornerShape(2.5.dp),
        color = color,
    ) {}
}

/**
 * 成就, folded: a header with the count, the ten-slot track and one line —
 * the latest unlock, or the rule while nothing is unlocked yet. The whole card
 * opens the full wall.
 */
@Composable
private fun AchievementCard(
    strings: Strings,
    wall: AchievementWallUi,
    onOpen: () -> Unit,
) {
    val latest = wall.items.firstOrNull { it.unlocked && it.unlockedOn != null }
    val line = if (wall.unlocked == 0 || latest == null) {
        strings.achievementWallEmpty
    } else {
        strings.achievementsRecent.format(latest.title)
    }
    Surface(
        onClick = onOpen,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    strings.achievements,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    strings.achievementCount.format(wall.unlocked, wall.total),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Icon(
                    imageVector = PagelenceIcons.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(start = 8.dp).size(18.dp),
                )
            }
            if (wall.total > 0) {
                Spacer(Modifier.height(12.dp))
                AchievementTrack(unlocked = wall.unlocked, total = wall.total)
            }
            Spacer(Modifier.height(10.dp))
            Text(
                line,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun StudySettings(
    strings: Strings,
    state: ProfileUiState,
    onOpenLevel: () -> Unit,
    onOpenAssessment: () -> Unit,
) {
    val levelValue = state.level
        ?.let { strings.levelValueWithSource.format(levelName(strings, it), levelSourceLabel(strings, state.levelSource)) }
        ?: strings.levelUnset
    SettingGroup(title = strings.studySettings) {
        SettingRow(label = strings.myLevel, value = levelValue, onClick = onOpenLevel)
        SettingDivider()
        SettingRow(
            label = strings.assessment,
            value = strings.assessmentMinutes,
            onClick = onOpenAssessment,
        )
    }
}

private fun streakLabel(strings: Strings, state: ProfileUiState): String = when {
    state.currentStreak > 0 -> strings.streakCurrent.format(state.currentStreak)
    state.longestStreak > 0 -> strings.streakRestart
    else -> strings.streakStart
}

@Composable
private fun LevelDialog(
    strings: Strings,
    current: CefrLevel?,
    onDismiss: () -> Unit,
    onConfirm: (CefrLevel) -> Unit,
) {
    var selected by rememberSaveable(current) { mutableStateOf(current?.name) }
    val selectedLevel = selected?.let { runCatching { CefrLevel.valueOf(it) }.getOrNull() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.myLevel) },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 560.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                levelOptions(strings).forEach { choice ->
                    LevelOptionRow(
                        choice = choice,
                        selected = selectedLevel == choice.level,
                        onClick = { selected = choice.level.name },
                    )
                }
                Text(
                    strings.levelAffectsHint,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { selectedLevel?.let(onConfirm) },
                enabled = selectedLevel != null,
            ) {
                Text(strings.setLevel)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(strings.close) }
        },
    )
}
