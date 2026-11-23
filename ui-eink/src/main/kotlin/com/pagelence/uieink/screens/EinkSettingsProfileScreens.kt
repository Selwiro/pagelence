package com.pagelence.uieink.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.pagelence.core.prefs.DefinitionMode
import com.pagelence.core.prefs.GuideMode
import com.pagelence.core.progress.CefrLevel
import com.pagelence.core.progress.LevelSource
import com.pagelence.presentation.eink.EinkRefreshPresets
import com.pagelence.presentation.eink.EinkRefreshSettings
import com.pagelence.uim3.AboutCopy
import com.pagelence.uim3.Strings
import com.pagelence.uim3.UiLanguage
import com.pagelence.uim3.model.DictionaryUiState
import com.pagelence.uim3.model.HeatmapCellUi
import com.pagelence.uim3.model.ProfileUiState
import com.pagelence.uim3.model.SettingsUiState
import com.pagelence.uieink.components.InkButton
import com.pagelence.uieink.components.InkDivider
import com.pagelence.uieink.components.InkKeyValueRow
import com.pagelence.uieink.components.InkListRow
import com.pagelence.uieink.components.InkSectionLabel
import com.pagelence.uieink.components.InkSelectRow
import com.pagelence.uieink.components.InkText
import com.pagelence.uieink.components.InkTextButton
import com.pagelence.uieink.components.InkTopBar
import com.pagelence.uieink.theme.InkColors
import com.pagelence.uieink.theme.LocalInkColors
import com.pagelence.uieink.theme.LocalInkTypography

/**
 * 设定：语言、每日目标、刷新强度、词义/导读语言、完整词表、关于。
 *
 * 墨屏版不提供主题、字号、行距、页边距——正文固定为 19sp 衬线，这是设备形态的
 * 一部分；「刷新强度」则是墨屏独有、且只影响本机的设置。
 */
@Composable
fun EinkSettingsScreen(
    strings: Strings,
    settings: SettingsUiState,
    dictionary: DictionaryUiState,
    refresh: EinkRefreshSettings,
    appVersion: String,
    onBack: () -> Unit,
    onLanguage: (UiLanguage) -> Unit,
    onDailyGoal: (Int) -> Unit,
    onRefresh: (EinkRefreshSettings) -> Unit,
    onDefinitionMode: (DefinitionMode) -> Unit,
    onGuideMode: (GuideMode) -> Unit,
    onOpenAbout: () -> Unit,
    onOpenSync: () -> Unit,
    onDownloadDictionary: () -> Unit,
    onDeleteDictionary: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalInkColors.current
    val typography = LocalInkTypography.current
    Column(modifier.fillMaxSize()) {
        InkTopBar(title = strings.settings, backLabel = strings.back, onBack = onBack)
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            InkListRow(
                title = strings.syncCopy.title,
                subtitle = strings.syncCopy.subtitle,
                onClick = onOpenSync,
            )
            InkDivider()
            InkSectionLabel(strings.language)
            InkSelectRow(strings.languageZh, settings.language == UiLanguage.ZH, { onLanguage(UiLanguage.ZH) })
            InkSelectRow(strings.languageEn, settings.language == UiLanguage.EN, { onLanguage(UiLanguage.EN) })
            InkDivider()

            InkSectionLabel(strings.sectionReading)
            InkText(
                strings.dailyGoalHint,
                style = typography.caption,
                color = colors.inkMuted,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
            listOf(5, 10, 15, 20, 30).forEach { minutes ->
                InkSelectRow(
                    label = strings.goalMinutesOption.format(minutes),
                    selected = settings.dailyGoalMinutes == minutes,
                    onClick = { onDailyGoal(minutes) },
                )
            }
            InkDivider()

            InkSectionLabel(strings.refreshStrength)
            InkSelectRow(strings.refreshLight, refresh == EinkRefreshPresets.Light, { onRefresh(EinkRefreshPresets.Light) })
            InkSelectRow(strings.refreshStandard, refresh == EinkRefreshPresets.Standard, { onRefresh(EinkRefreshPresets.Standard) })
            InkSelectRow(strings.refreshStrong, refresh == EinkRefreshPresets.Strong, { onRefresh(EinkRefreshPresets.Strong) })
            InkDivider()

            InkSectionLabel(strings.definitionMode)
            InkSelectRow(strings.definitionModeZh, settings.definitionMode == DefinitionMode.ZH, { onDefinitionMode(DefinitionMode.ZH) })
            InkSelectRow(strings.definitionModeMixed, settings.definitionMode == DefinitionMode.MIXED, { onDefinitionMode(DefinitionMode.MIXED) })
            InkSelectRow(strings.definitionModeEn, settings.definitionMode == DefinitionMode.EN, { onDefinitionMode(DefinitionMode.EN) })
            InkDivider()

            InkSectionLabel(strings.guideMode)
            InkSelectRow(strings.guideModeBilingual, settings.guideMode == GuideMode.BILINGUAL, { onGuideMode(GuideMode.BILINGUAL) })
            InkSelectRow(strings.guideModeEn, settings.guideMode == GuideMode.EN, { onGuideMode(GuideMode.EN) })
            InkSelectRow(strings.guideModeImmersive, settings.guideMode == GuideMode.IMMERSIVE, { onGuideMode(GuideMode.IMMERSIVE) })
            InkDivider()

            InkSectionLabel(strings.fullDictionary)
            dictionary.status?.let {
                InkText(it, style = typography.caption, color = colors.inkSecondary, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
            }
            dictionary.sizeLabel?.let {
                InkKeyValueRow(strings.dictSizeLabel, it)
            }
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)) {
                if (dictionary.canDownload || dictionary.canResume || dictionary.canRetry) {
                    InkButton(
                        label = if (dictionary.canRetry) strings.dictRetry else strings.dictDownload,
                        onClick = onDownloadDictionary,
                        primary = true,
                    )
                }
                if (dictionary.canDelete) {
                    Spacer(Modifier.height(0.dp))
                    InkTextButton(strings.dictDelete, onClick = onDeleteDictionary)
                }
            }
            InkDivider()

            InkListRow(
                title = strings.about,
                subtitle = appVersion,
                onClick = onOpenAbout,
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

/** 关于：版本、简介、隐私与许可摘要；链接在墨屏版里不做跳转。 */
@Composable
fun EinkAboutScreen(
    strings: Strings,
    appVersion: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalInkColors.current
    val typography = LocalInkTypography.current
    val copy: AboutCopy = strings.aboutCopy
    Column(modifier.fillMaxSize()) {
        InkTopBar(title = copy.title, backLabel = strings.back, onBack = onBack)
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 12.dp)) {
            InkText(copy.tagline, style = typography.body)
            Spacer(Modifier.height(6.dp))
            InkText(copy.versionFormat.format(appVersion), style = typography.caption, color = colors.inkMuted)
            Spacer(Modifier.height(16.dp))
            InkText(copy.privacy, style = typography.title)
            copy.privacySections.forEach { section ->
                Spacer(Modifier.height(10.dp))
                InkText(section.title, style = typography.uiStrong)
                section.paragraphs.forEach {
                    InkText(it, style = typography.bodySmall, color = colors.inkSecondary)
                }
            }
            Spacer(Modifier.height(16.dp))
            InkText(copy.licenses, style = typography.title)
            copy.licenseSections.forEach { section ->
                Spacer(Modifier.height(10.dp))
                InkText(section.title, style = typography.uiStrong)
                section.paragraphs.forEach {
                    InkText(it, style = typography.bodySmall, color = colors.inkSecondary)
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

/** 我的：今日、等级、统计、成就。热力图在墨屏上不画格子，只给累计数字。 */
@Composable
fun EinkProfileScreen(
    strings: Strings,
    profile: ProfileUiState,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalInkColors.current
    val typography = LocalInkTypography.current
    Column(modifier.fillMaxSize()) {
        InkTopBar(
            title = strings.profile,
            rightLabel = strings.settings,
            onRight = onOpenSettings,
        )
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            InkSectionLabel(strings.todayLabel)
            Column(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                InkText(
                    strings.todayProgress.format(profile.todaySeconds / 60, profile.dailyGoalMinutes),
                    style = typography.display,
                )
                Spacer(Modifier.height(6.dp))
                InkText(
                    if (profile.metToday) {
                        strings.todayGoalMet
                    } else {
                        strings.todayGoalRemaining.format(((profile.secondsToGoal + 59) / 60).coerceAtLeast(0))
                    },
                    style = typography.ui,
                    color = colors.inkSecondary,
                )
                Spacer(Modifier.height(4.dp))
                InkText(
                    when {
                        profile.currentStreak > 0 -> strings.streakCurrent.format(profile.currentStreak)
                        profile.longestStreak > 0 -> strings.streakRestart
                        else -> strings.streakStart
                    } + " · " + strings.streakLongest.format(profile.longestStreak),
                    style = typography.caption,
                    color = colors.inkMuted,
                )
                if (profile.todayUnits > 0) {
                    Spacer(Modifier.height(4.dp))
                    InkText(strings.todayUnitsLine.format(profile.todayUnits), style = typography.caption, color = colors.inkMuted)
                }
            }
            InkDivider()

            InkSectionLabel(strings.myLevel)
            Column(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                val level = profile.level
                InkText(
                    level?.name ?: strings.levelUnset,
                    style = typography.title,
                )
                Spacer(Modifier.height(4.dp))
                InkText(
                    when (profile.levelSource) {
                        LevelSource.SELF -> strings.levelSelf
                        LevelSource.QUICK -> strings.levelQuick
                        LevelSource.FULL -> strings.levelFull
                    },
                    style = typography.caption,
                    color = colors.inkMuted,
                )
            }
            InkDivider()

            InkSectionLabel(strings.sectionReading)
            InkText(
                strings.unitsReadTotal.format(profile.totalReadUnits, profile.totalUnits),
                style = typography.ui,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
            )
            InkKeyValueRow(strings.booksFinishedShort, profile.finishedBooks.toString())
            InkKeyValueRow(strings.readingDaysShort, profile.readingDays.toString())
            InkKeyValueRow(strings.dailyGoal, strings.goalMinutesOption.format(profile.dailyGoalMinutes))
            if (!profile.heatmap.isEmpty) {
                HeatmapGrid(profile.heatmap.weeks)
            }
            InkDivider()

            InkSectionLabel("${strings.achievements}  ${profile.achievements.unlocked} / ${profile.achievements.total}")
            profile.achievements.items.forEach { item ->
                InkListRow(
                    title = item.title,
                    subtitle = item.description,
                    trailing = when {
                        item.unlocked -> item.unlockedOn ?: "✓"
                        item.progressLabel != null -> item.progressLabel
                        else -> null
                    },
                )
                InkDivider()
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

/**
 * 365 天阅读热力图：每周一列、周一在上，格子按 0–4 级灰阶填充。
 *
 * 手机宽度放不下 53 列 × 14dp，所以按可用宽度反算格边（通常 6–7dp）；
 * 墨屏不画颜色图例，只保留灰阶梯度——宋体正文旁边一块色卡反而更乱。
 */
@Composable
private fun HeatmapGrid(weeks: List<List<HeatmapCellUi?>>) {
    if (weeks.isEmpty()) return
    val colors = LocalInkColors.current
    BoxWithConstraints(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        val columns = weeks.size
        val gap = 2f
        val available = (maxWidth.value - gap * (columns - 1)).coerceAtLeast(4f)
        val cell = (available / columns).coerceAtLeast(4f).dp
        Column {
            for (row in 0..6) {
                Row {
                    weeks.forEach { week ->
                        val cellUi = week.getOrNull(row)
                        Box(
                            Modifier
                                .padding(end = gap.dp)
                                .size(cell)
                                .background(
                                    if (cellUi == null) Color.Transparent else heatColor(cellUi.level, colors),
                                )
                                .then(
                                    if (cellUi != null && cellUi.level == 0) {
                                        Modifier.border(0.5.dp, colors.rule)
                                    } else {
                                        Modifier
                                    },
                                ),
                        )
                    }
                }
                Spacer(Modifier.height(gap.dp))
            }
        }
    }
}

private fun heatColor(level: Int, colors: InkColors): Color = when (level) {
    0 -> colors.paper
    1 -> Color(0xFFE2E2E2)
    2 -> Color(0xFFBCBCBC)
    3 -> Color(0xFF8E8E8E)
    else -> Color(0xFF5E5E5E)
}
