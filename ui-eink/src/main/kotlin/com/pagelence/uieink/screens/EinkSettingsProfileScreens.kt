package com.pagelence.uieink.screens

// 我的、设置、成就、排版、词典与关于（T169，对齐普通版 T168 的信息架构）。
//
// 普通版用卡片、抽屉与底部弹层；墨屏版仍是一列白底行：分组标题 + 1px 分隔线，
// 需要选项时整页覆盖，不做浮层与动画。设置项本身的语义（分组、当前值、
// 子页面）与普通版一致。

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import com.pagelence.core.prefs.DefinitionMode
import com.pagelence.core.prefs.FontSize
import com.pagelence.core.prefs.GuideMode
import com.pagelence.core.prefs.LineSpacing
import com.pagelence.core.prefs.PageMargin
import com.pagelence.core.progress.CefrLevel
import com.pagelence.core.progress.LevelSource
import com.pagelence.presentation.eink.EinkRefreshPresets
import com.pagelence.presentation.eink.EinkRefreshSettings
import com.pagelence.uim3.AboutCopy
import com.pagelence.uim3.Strings
import com.pagelence.uim3.UiLanguage
import com.pagelence.uim3.model.AchievementUi
import com.pagelence.uim3.model.AchievementWallUi
import com.pagelence.uim3.model.DictionaryPhase
import com.pagelence.uim3.model.DictionaryUiState
import com.pagelence.uim3.model.HeatmapCellUi
import com.pagelence.uim3.model.ProfileUiState
import com.pagelence.uim3.model.SettingsUiState
import com.pagelence.uieink.components.InkButton
import com.pagelence.uieink.components.InkDivider
import com.pagelence.uieink.components.InkKeyValueRow
import com.pagelence.uieink.components.InkListRow
import com.pagelence.uieink.components.InkOptionRow
import com.pagelence.uieink.components.InkSectionLabel
import com.pagelence.uieink.components.InkSelectRow
import com.pagelence.uieink.components.InkText
import com.pagelence.uieink.components.InkTextButton
import com.pagelence.uieink.components.InkTopBar
import com.pagelence.uieink.theme.InkColors
import com.pagelence.uieink.theme.LocalInkColors
import com.pagelence.uieink.theme.LocalInkTypography
import com.pagelence.uieink.theme.einkPageMargin
import com.pagelence.uieink.theme.einkReadingStyle

/**
 * 设置（T168 结构）：阅读 / 目标与记录 / 外观 / 墨水屏 / 数据 / 关于。
 *
 * 「排版」与「完整词表」各自成页；词义语言、导读模式、每日目标与界面语言
 * 是整页单选。主题、阅读方式与标注提示不在墨屏出现：墨屏固定白底、翻页，
 * 标注由工具条的「标记句子」显式开启。
 */
@Composable
fun EinkSettingsScreen(
    strings: Strings,
    settings: SettingsUiState,
    dictionary: DictionaryUiState,
    refresh: EinkRefreshSettings,
    refreshBackend: String,
    forceMoaanRefresh: Boolean,
    appVersion: String,
    onBack: () -> Unit,
    onLanguage: (UiLanguage) -> Unit,
    onDailyGoal: (Int) -> Unit,
    onRefresh: (EinkRefreshSettings) -> Unit,
    onToggleForceMoaan: (Boolean) -> Unit,
    onTestFullRefresh: () -> Unit,
    onDefinitionMode: (DefinitionMode) -> Unit,
    onGuideMode: (GuideMode) -> Unit,
    onOpenAbout: () -> Unit,
    onOpenSync: () -> Unit,
    onOpenTypography: () -> Unit,
    onOpenDictionary: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalInkColors.current
    val typography = LocalInkTypography.current
    var sheet by remember { mutableStateOf<EinkSettingsSheet?>(null) }

    Box(modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            InkTopBar(title = strings.settings, backLabel = strings.back, onBack = onBack)
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                InkSectionLabel(strings.sectionReading)
                InkListRow(
                    title = strings.typography,
                    subtitle = strings.typographySummary.format(
                        "${fontSizeLabel(settings.fontSize)}sp",
                        strings.fontSerif,
                    ),
                    trailing = "›",
                    onClick = onOpenTypography,
                )
                InkDivider()
                InkListRow(
                    title = strings.definitionMode,
                    subtitle = definitionLabel(strings, settings),
                    trailing = "›",
                    onClick = { sheet = EinkSettingsSheet.DEFINITION },
                )
                InkDivider()
                InkListRow(
                    title = strings.guideMode,
                    subtitle = guideLabel(strings, settings),
                    trailing = "›",
                    onClick = { sheet = EinkSettingsSheet.GUIDE },
                )
                InkDivider()

                InkSectionLabel(strings.sectionGoalRecord)
                InkListRow(
                    title = strings.dailyGoal,
                    subtitle = strings.goalMinutesOption.format(settings.dailyGoalMinutes),
                    trailing = "›",
                    onClick = { sheet = EinkSettingsSheet.GOAL },
                )
                InkDivider()

                InkSectionLabel(strings.sectionAppearance)
                InkListRow(
                    title = strings.language,
                    subtitle = if (settings.language == UiLanguage.EN) strings.languageEn else strings.languageZh,
                    trailing = "›",
                    onClick = { sheet = EinkSettingsSheet.LANGUAGE },
                )
                InkDivider()

                InkSectionLabel(strings.refreshStrength)
                InkSelectRow(
                    label = strings.refreshLight,
                    selected = refresh == EinkRefreshPresets.Light,
                    onClick = { onRefresh(EinkRefreshPresets.Light) },
                )
                InkSelectRow(
                    label = strings.refreshStandard,
                    selected = refresh == EinkRefreshPresets.Standard,
                    onClick = { onRefresh(EinkRefreshPresets.Standard) },
                )
                InkSelectRow(
                    label = strings.refreshStrong,
                    selected = refresh == EinkRefreshPresets.Strong,
                    onClick = { onRefresh(EinkRefreshPresets.Strong) },
                )
                InkKeyValueRow(strings.refreshBackend, refreshBackend, emphasis = true)
                InkSelectRow(
                    label = strings.forceMoaanRefresh,
                    selected = forceMoaanRefresh,
                    onClick = { onToggleForceMoaan(!forceMoaanRefresh) },
                )
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)) {
                    InkButton(strings.testFullRefresh, onClick = onTestFullRefresh)
                }
                InkDivider()

                InkSectionLabel(strings.sectionData)
                InkListRow(
                    title = strings.fullDictionary,
                    subtitle = dictionarySummary(strings, dictionary),
                    trailing = "›",
                    onClick = onOpenDictionary,
                )
                InkDivider()
                InkListRow(
                    title = strings.syncCopy.title,
                    subtitle = strings.syncCopy.subtitle,
                    trailing = "›",
                    onClick = onOpenSync,
                )
                InkDivider()

                InkListRow(
                    title = strings.about,
                    subtitle = appVersion,
                    trailing = "›",
                    onClick = onOpenAbout,
                )
                Spacer(Modifier.height(28.dp))
            }
        }

        sheet?.let { current ->
            EinkChoicePage(
                backLabel = strings.back,
                title = when (current) {
                    EinkSettingsSheet.DEFINITION -> strings.definitionMode
                    EinkSettingsSheet.GUIDE -> strings.guideMode
                    EinkSettingsSheet.GOAL -> strings.dailyGoal
                    EinkSettingsSheet.LANGUAGE -> strings.language
                },
                onBack = { sheet = null },
            ) {
                when (current) {
                    EinkSettingsSheet.DEFINITION -> listOf(
                        DefinitionMode.ZH to strings.definitionModeZh,
                        DefinitionMode.MIXED to strings.definitionModeMixed,
                        DefinitionMode.EN to strings.definitionModeEn,
                    ).forEach { (mode, label) ->
                        InkSelectRow(
                            label = label,
                            selected = settings.definitionMode == mode,
                            onClick = {
                                onDefinitionMode(mode)
                                sheet = null
                            },
                        )
                    }

                    EinkSettingsSheet.GUIDE -> listOf(
                        GuideMode.BILINGUAL to strings.guideModeBilingual,
                        GuideMode.EN to strings.guideModeEn,
                        GuideMode.IMMERSIVE to strings.guideModeImmersive,
                    ).forEach { (mode, label) ->
                        InkSelectRow(
                            label = label,
                            selected = settings.guideMode == mode,
                            onClick = {
                                onGuideMode(mode)
                                sheet = null
                            },
                        )
                    }

                    EinkSettingsSheet.GOAL -> {
                        InkText(
                            strings.dailyGoalHint,
                            style = typography.caption,
                            color = colors.inkMuted,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                        )
                        listOf(5, 10, 15, 20, 30).forEach { minutes ->
                            InkSelectRow(
                                label = strings.goalMinutesOption.format(minutes),
                                selected = settings.dailyGoalMinutes == minutes,
                                onClick = {
                                    onDailyGoal(minutes)
                                    sheet = null
                                },
                            )
                        }
                    }

                    EinkSettingsSheet.LANGUAGE -> listOf(
                        UiLanguage.ZH to strings.languageZh,
                        UiLanguage.EN to strings.languageEn,
                    ).forEach { (language, label) ->
                        InkSelectRow(
                            label = label,
                            selected = settings.language == language,
                            onClick = {
                                onLanguage(language)
                                sheet = null
                            },
                        )
                    }
                }
            }
        }
    }
}

private enum class EinkSettingsSheet { DEFINITION, GUIDE, GOAL, LANGUAGE }

/** 排版：预览 + 字号六档 + 行距三档 + 页边距三档，改完即时生效。 */
@Composable
fun EinkTypographyScreen(
    strings: Strings,
    settings: SettingsUiState,
    onBack: () -> Unit,
    onFontSize: (FontSize) -> Unit,
    onLineSpacing: (LineSpacing) -> Unit,
    onPageMargin: (PageMargin) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalInkColors.current
    val typography = LocalInkTypography.current
    Column(modifier.fillMaxSize().background(colors.paper)) {
        InkTopBar(title = strings.typography, backLabel = strings.back, onBack = onBack)
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
            Spacer(Modifier.height(10.dp))
            InkText(strings.typographyPreview, style = typography.uiStrong, color = colors.inkSecondary)
            Spacer(Modifier.height(8.dp))
            Column(
                Modifier
                    .fillMaxWidth()
                    .border(1.dp, colors.rule)
                    .padding(horizontal = einkPageMargin(settings.pageMargin), vertical = 14.dp),
            ) {
                InkText(
                    strings.typographySample,
                    style = einkReadingStyle(settings.fontSize, settings.lineSpacing),
                )
                Spacer(Modifier.height(12.dp))
                InkDivider()
                Spacer(Modifier.height(10.dp))
                InkText(strings.typographySampleZh, style = typography.bodySmall, color = colors.inkSecondary)
            }
            Spacer(Modifier.height(16.dp))
            InkOptionRow(
                label = strings.fontSize,
                options = fontSizeOptions(),
                selected = settings.fontSize,
                onSelect = onFontSize,
            )
            InkOptionRow(
                label = strings.lineSpacing,
                options = listOf(
                    LineSpacing.COMPACT to strings.lineSpacingCompact,
                    LineSpacing.NORMAL to strings.lineSpacingNormal,
                    LineSpacing.RELAXED to strings.lineSpacingRelaxed,
                ),
                selected = settings.lineSpacing,
                onSelect = onLineSpacing,
            )
            InkOptionRow(
                label = strings.pageMargin,
                options = listOf(
                    PageMargin.NARROW to strings.pageMarginNarrow,
                    PageMargin.NORMAL to strings.pageMarginNormal,
                    PageMargin.WIDE to strings.pageMarginWide,
                ),
                selected = settings.pageMargin,
                onSelect = onPageMargin,
            )
            Spacer(Modifier.height(10.dp))
            InkText(strings.einkTypographyHint, style = typography.caption, color = colors.inkMuted)
            Spacer(Modifier.height(28.dp))
        }
    }
}

/** 完整词表：从设置首屏移出来的独立页。 */
@Composable
fun EinkDictionaryScreen(
    strings: Strings,
    dictionary: DictionaryUiState,
    onBack: () -> Unit,
    onDownload: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalInkColors.current
    val typography = LocalInkTypography.current
    Column(modifier.fillMaxSize().background(colors.paper)) {
        InkTopBar(title = strings.fullDictionary, backLabel = strings.back, onBack = onBack)
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            dictionary.status?.let {
                InkText(
                    it,
                    style = typography.ui,
                    color = colors.inkSecondary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                )
            }
            dictionary.version?.let {
                InkText(
                    strings.dictVersionLabel.format(it),
                    style = typography.ui,
                    color = colors.inkSecondary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }
            dictionary.sizeLabel?.let {
                InkText(
                    strings.dictSizeLabel.format(it),
                    style = typography.ui,
                    color = colors.inkSecondary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }
            dictionary.installedVersion?.let {
                InkText(
                    strings.dictInstalled,
                    style = typography.caption,
                    color = colors.inkMuted,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)) {
                if (dictionary.canDownload || dictionary.canResume || dictionary.canRetry) {
                    InkButton(
                        label = if (dictionary.canRetry) strings.dictRetry else strings.dictDownload,
                        onClick = onDownload,
                        primary = true,
                    )
                }
                if (dictionary.canDelete) {
                    InkTextButton(strings.dictDelete, onClick = onDelete)
                }
            }
            Spacer(Modifier.height(12.dp))
            InkText(
                strings.dictLicenseNote,
                style = typography.caption,
                color = colors.inkMuted,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            Spacer(Modifier.height(28.dp))
        }
    }
}

/** 我的（T168 结构）：今日、阅读记录、成就入口、学习设置。 */
@Composable
fun EinkProfileScreen(
    strings: Strings,
    profile: ProfileUiState,
    onOpenSettings: () -> Unit,
    onOpenAchievements: () -> Unit,
    onSelectLevel: (CefrLevel) -> Unit,
    onDailyGoal: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalInkColors.current
    val typography = LocalInkTypography.current
    var showGoal by remember { mutableStateOf(false) }
    var showLevel by remember { mutableStateOf(false) }

    Box(modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
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
                        InkText(
                            strings.todayUnitsLine.format(profile.todayUnits),
                            style = typography.caption,
                            color = colors.inkMuted,
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    InkTextButton(
                        label = strings.dailyGoal + " · " + strings.goalMinutesOption.format(profile.dailyGoalMinutes),
                        onClick = { showGoal = true },
                    )
                }
                InkDivider()

                InkSectionLabel(strings.readingRecord)
                InkKeyValueRow(
                    strings.statFinished,
                    "${profile.finishedBooks} " + countUnit(
                        profile.finishedBooks,
                        strings.unitBook,
                        strings.unitBooks,
                    ),
                )
                InkKeyValueRow(
                    strings.unitsTotal.format(profile.totalUnits),
                    "${profile.totalReadUnits} " + countUnit(
                        profile.totalReadUnits,
                        strings.unitUnit,
                        strings.unitUnits,
                    ),
                )
                InkKeyValueRow(
                    strings.statReading,
                    "${profile.readingDays} " + countUnit(
                        profile.readingDays,
                        strings.unitDay,
                        strings.unitDays,
                    ),
                )
                InkText(
                    strings.heatmapRange.format(HeatmapWeeks),
                    style = typography.caption,
                    color = colors.inkMuted,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
                if (!profile.heatmap.isEmpty) {
                    HeatmapGrid(profile.heatmap.weeks.takeLast(HeatmapWeeks))
                    HeatmapLegend(strings)
                } else {
                    InkText(
                        strings.heatmapEmpty,
                        style = typography.caption,
                        color = colors.inkMuted,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                    )
                }
                InkDivider()

                val latest = profile.achievements.items.firstOrNull { it.unlocked && it.unlockedOn != null }
                InkListRow(
                    title = strings.achievements,
                    subtitle = if (profile.achievements.unlocked == 0 || latest == null) {
                        strings.achievementWallEmpty
                    } else {
                        strings.achievementsRecent.format(latest.title)
                    },
                    trailing = strings.achievementCount.format(
                        profile.achievements.unlocked,
                        profile.achievements.total,
                    ),
                    onClick = onOpenAchievements,
                )
                InkDivider()

                InkSectionLabel(strings.studySettings)
                InkListRow(
                    title = strings.myLevel,
                    subtitle = profile.level
                        ?.let {
                            strings.levelValueWithSource.format(
                                levelName(strings, it),
                                levelSourceLabel(strings, profile.levelSource),
                            )
                        }
                        ?: strings.levelUnset,
                    trailing = "›",
                    onClick = { showLevel = true },
                )
                Spacer(Modifier.height(24.dp))
            }
        }

        if (showGoal) {
            EinkChoicePage(strings.dailyGoal, backLabel = strings.back, onBack = { showGoal = false }) {
                InkText(
                    strings.dailyGoalHint,
                    style = typography.caption,
                    color = colors.inkMuted,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                )
                listOf(5, 10, 15, 20, 30).forEach { minutes ->
                    InkSelectRow(
                        label = strings.goalMinutesOption.format(minutes),
                        selected = profile.dailyGoalMinutes == minutes,
                        onClick = {
                            onDailyGoal(minutes)
                            showGoal = false
                        },
                    )
                }
            }
        }
        if (showLevel) {
            EinkChoicePage(strings.myLevel, backLabel = strings.back, onBack = { showLevel = false }) {
                CefrLevel.entries.forEach { level ->
                    InkSelectRow(
                        label = levelName(strings, level),
                        description = levelDescription(strings, level),
                        alignTop = true,
                        selected = profile.level == level,
                        onClick = {
                            showLevel = false
                            onSelectLevel(level)
                        },
                    )
                }
            }
        }
    }
}

/** 成就墙：已解锁 / 未解锁两组；点一行看条件与解锁日期。 */
@Composable
fun EinkAchievementsScreen(
    strings: Strings,
    wall: AchievementWallUi,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalInkColors.current
    val typography = LocalInkTypography.current
    var selectedId by remember { mutableStateOf<String?>(null) }
    val selected = wall.items.firstOrNull { it.id == selectedId }

    Box(modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().background(colors.paper)) {
            InkTopBar(title = strings.achievements, backLabel = strings.back, onBack = onBack)
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                InkText(
                    strings.achievementWallHint,
                    style = typography.caption,
                    color = colors.inkMuted,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
                if (wall.isEmpty) {
                    InkText(
                        strings.achievementWallEmpty,
                        style = typography.body,
                        color = colors.inkSecondary,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    )
                }
                val unlocked = wall.items.filter { it.unlocked }
                val locked = wall.items.filterNot { it.unlocked }
                if (unlocked.isNotEmpty()) {
                    InkSectionLabel(
                        "${strings.achievementUnlocked}  ${unlocked.size}",
                    )
                    unlocked.forEach { item ->
                        AchievementRow(strings, item) { selectedId = item.id }
                    }
                }
                if (locked.isNotEmpty()) {
                    InkSectionLabel(
                        "${strings.achievementLocked}  ${locked.size}",
                    )
                    locked.forEach { item ->
                        AchievementRow(strings, item) { selectedId = item.id }
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }

        selected?.let { item ->
            Column(Modifier.fillMaxSize().background(colors.paper)) {
                InkTopBar(title = item.title, backLabel = strings.back, onBack = { selectedId = null })
                Column(
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                ) {
                InkText(item.description, style = typography.body)
                Spacer(Modifier.height(10.dp))
                InkText(
                    if (item.unlocked) {
                        strings.achievementUnlockedOn.format(item.unlockedOn.orEmpty())
                    } else {
                        item.progressLabel ?: strings.achievementWallHint
                    },
                    style = typography.caption,
                    color = colors.inkSecondary,
                )
                    Spacer(Modifier.height(20.dp))
                    InkButton(strings.close, onClick = { selectedId = null })
                }
            }
        }
    }
}

@Composable
private fun AchievementRow(
    strings: Strings,
    item: AchievementUi,
    onClick: () -> Unit,
) {
    val colors = LocalInkColors.current
    val typography = LocalInkTypography.current
    InkListRow(
        title = (if (item.unlocked) "✓ " else "· ") + item.title,
        subtitle = item.description,
        trailing = when {
            item.unlocked -> item.unlockedOn
            item.progressLabel != null -> item.progressLabel
            else -> null
        },
        onClick = onClick,
    )
    InkDivider()
}

/** 整页单选：白底、顶栏返回、选项行；用于设置与「我的」的选择。 */
@Composable
private fun EinkChoicePage(
    title: String,
    backLabel: String = "←",
    onBack: () -> Unit,
    content: @Composable () -> Unit,
) {
    val colors = LocalInkColors.current
    Column(Modifier.fillMaxSize().background(colors.paper)) {
        InkTopBar(title = title, backLabel = backLabel, onBack = onBack)
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            content()
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
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
        ) {
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

/** 26 周（半年）在半屏宽度内可辨；比 365 天整图更适合墨屏的 16 级灰。 */
private const val HeatmapWeeks = 26

/**
 * 阅读热力图：每周一列、周一在上，格子按 0–4 级灰阶填充。
 *
 * 手机宽度放不下 53 列；这里只画最近 26 周（T168 同口径），格宽按可用宽度反算。
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

@Composable
private fun HeatmapLegend(strings: Strings) {
    val colors = LocalInkColors.current
    val typography = LocalInkTypography.current
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        InkText(
            strings.heatmapCellHint,
            style = typography.caption,
            color = colors.inkMuted,
            modifier = Modifier.weight(1f),
        )
        InkText(strings.heatmapLess, style = typography.caption, color = colors.inkMuted)
        Spacer(Modifier.size(4.dp))
        repeat(5) { level ->
            Box(
                Modifier
                    .padding(horizontal = 1.dp)
                    .size(10.dp)
                    .background(heatColor(level, colors))
                    .then(if (level == 0) Modifier.border(0.5.dp, colors.rule) else Modifier),
            )
        }
        Spacer(Modifier.size(4.dp))
        InkText(strings.heatmapMore, style = typography.caption, color = colors.inkMuted)
    }
}

private fun heatColor(level: Int, colors: InkColors): Color = when (level) {
    0 -> colors.paper
    1 -> Color(0xFFE2E2E2)
    2 -> Color(0xFFBCBCBC)
    3 -> Color(0xFF8E8E8E)
    else -> Color(0xFF5E5E5E)
}

/** 「1 本 / 2 本」：英文界面用单复数，中文两档相同。 */
internal fun countUnit(count: Int, one: String, many: String): String =
    if (count == 1) one else many

internal fun fontSizeLabel(size: FontSize): String = when (size) {
    FontSize.XSMALL -> "15"
    FontSize.SMALL -> "17"
    FontSize.MEDIUM -> "19"
    FontSize.LARGE -> "22"
    FontSize.XLARGE -> "25"
    FontSize.XXLARGE -> "28"
}

private fun definitionLabel(strings: Strings, settings: SettingsUiState): String = when (settings.definitionMode) {
    DefinitionMode.ZH -> strings.definitionModeZh
    DefinitionMode.MIXED -> strings.definitionModeMixed
    DefinitionMode.EN -> strings.definitionModeEn
}

private fun guideLabel(strings: Strings, settings: SettingsUiState): String = when (settings.guideMode) {
    GuideMode.BILINGUAL -> strings.guideModeBilingual
    GuideMode.EN -> strings.guideModeEn
    GuideMode.IMMERSIVE -> strings.guideModeImmersive
}

fun levelName(strings: Strings, level: CefrLevel): String = when (level) {
    CefrLevel.A1 -> strings.levelA1Name
    CefrLevel.A2 -> strings.levelA2Name
    CefrLevel.B1 -> strings.levelB1Name
    CefrLevel.B2 -> strings.levelB2Name
    CefrLevel.C1 -> strings.levelC1Name
    CefrLevel.C2 -> strings.levelC2Name
}

private fun levelDescription(strings: Strings, level: CefrLevel): String = when (level) {
    CefrLevel.A1 -> strings.levelA1Desc
    CefrLevel.A2 -> strings.levelA2Desc
    CefrLevel.B1 -> strings.levelB1Desc
    CefrLevel.B2 -> strings.levelB2Desc
    CefrLevel.C1 -> strings.levelC1Desc
    CefrLevel.C2 -> strings.levelC2Desc
}

internal fun levelSourceLabel(strings: Strings, source: LevelSource): String = when (source) {
    LevelSource.SELF -> strings.levelSelf
    LevelSource.QUICK -> strings.levelQuick
    LevelSource.FULL -> strings.levelFull
}

private fun dictionarySummary(strings: Strings, dictionary: DictionaryUiState): String {
    val size = dictionary.sizeLabel ?: dictionary.installedVersion ?: dictionary.version ?: "—"
    return if (dictionary.phase == DictionaryPhase.INSTALLED) {
        strings.dictionaryInstalled.format(size)
    } else {
        strings.dictionaryNotInstalled.format(size)
    }
}
