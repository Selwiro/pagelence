package com.pagelence.uim3.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pagelence.core.prefs.DailyGoal
import com.pagelence.core.prefs.DefinitionMode
import com.pagelence.core.prefs.FontSize
import com.pagelence.core.prefs.GuideMode
import com.pagelence.core.prefs.ReaderLayout
import com.pagelence.core.prefs.ReadingFont
import com.pagelence.core.prefs.ThemeMode
import com.pagelence.uim3.Strings
import com.pagelence.uim3.UiLanguage
import com.pagelence.uim3.components.ChoiceSheet
import com.pagelence.uim3.components.PageTopBar
import com.pagelence.uim3.components.SettingDivider
import com.pagelence.uim3.components.SettingGroup
import com.pagelence.uim3.components.SettingRow
import com.pagelence.uim3.components.SheetOption
import com.pagelence.uim3.components.SwitchRow
import com.pagelence.uim3.components.fontSizeLabel
import com.pagelence.uim3.model.DictionaryPhase
import com.pagelence.uim3.model.DictionaryUiState
import com.pagelence.uim3.model.SettingsUiState

/**
 * 设置 (T168): five groups, twelve rows, one screen. Every choice is a row that
 * states its current value and opens a single-choice sheet; only the toggle is
 * a toggle. Typography and the full dictionary live on sub-pages so a page a
 * reader visits rarely does not weigh as much as the one they read in.
 */
@Composable
fun SettingsScreen(
    strings: Strings,
    state: SettingsUiState,
    dictionary: DictionaryUiState,
    appVersion: String,
    syncConfigured: Boolean,
    onBack: () -> Unit,
    onOpenTypography: () -> Unit,
    onOpenDictionary: () -> Unit,
    onOpenSync: () -> Unit,
    onOpenAbout: () -> Unit,
    onRestoreDefaults: () -> Unit,
    onLanguage: (UiLanguage) -> Unit,
    onTheme: (ThemeMode) -> Unit,
    onReaderLayout: (ReaderLayout) -> Unit,
    onAnnotateHints: (Boolean) -> Unit,
    onDefinitionMode: (DefinitionMode) -> Unit,
    onGuideMode: (GuideMode) -> Unit,
    onDailyGoal: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var sheet by remember { mutableStateOf<SettingsSheet?>(null) }
    var confirmRestore by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        topBar = { PageTopBar(title = strings.settings, onBack = onBack) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            SettingGroup(title = strings.sectionReading) {
                SettingRow(
                    label = strings.typography,
                    value = strings.typographySummary.format(
                        fontSizeLabel(strings, state.fontSize),
                        readingFontLabel(strings, state),
                    ),
                    onClick = onOpenTypography,
                )
                SettingDivider()
                SettingRow(
                    label = strings.readingLayout,
                    value = if (state.readerLayout == ReaderLayout.PAGED) {
                        strings.layoutPaged
                    } else {
                        strings.layoutScroll
                    },
                    onClick = { sheet = SettingsSheet.LAYOUT },
                )
                SettingDivider()
                SettingRow(
                    label = strings.definitionMode,
                    value = definitionLabel(strings, state),
                    onClick = { sheet = SettingsSheet.DEFINITION },
                )
                SettingDivider()
                SettingRow(
                    label = strings.guideMode,
                    value = guideLabel(strings, state),
                    onClick = { sheet = SettingsSheet.GUIDE },
                )
                SettingDivider()
                SwitchRow(
                    label = strings.annotateHints,
                    checked = state.annotateHints,
                    onCheckedChange = onAnnotateHints,
                )
            }

            SettingGroup(title = strings.sectionGoalRecord) {
                SettingRow(
                    label = strings.dailyGoal,
                    value = strings.goalMinutesOption.format(DailyGoal.sanitize(state.dailyGoalMinutes)),
                    onClick = { sheet = SettingsSheet.GOAL },
                )
            }

            SettingGroup(title = strings.sectionAppearance) {
                SettingRow(
                    label = strings.theme,
                    value = themeLabel(strings, state),
                    onClick = { sheet = SettingsSheet.THEME },
                )
                SettingDivider()
                SettingRow(
                    label = strings.language,
                    value = if (state.language == UiLanguage.EN) strings.languageEn else strings.languageZh,
                    onClick = { sheet = SettingsSheet.LANGUAGE },
                )
            }

            SettingGroup(title = strings.sectionData) {
                SettingRow(
                    label = strings.fullDictionary,
                    value = dictionarySummary(strings, dictionary),
                    onClick = onOpenDictionary,
                )
                SettingDivider()
                SettingRow(
                    label = strings.syncCopy.title,
                    value = if (syncConfigured) strings.syncOn else strings.syncOff,
                    onClick = onOpenSync,
                )
            }

            SettingGroup(title = strings.about) {
                SettingRow(
                    label = strings.about,
                    value = appVersion,
                    onClick = onOpenAbout,
                )
                SettingDivider()
                SettingRow(
                    label = strings.restoreDefaults,
                    onClick = { confirmRestore = true },
                    showChevron = false,
                    destructive = true,
                )
            }
            Spacer(Modifier.height(28.dp))
        }
    }

    when (sheet) {
        SettingsSheet.LAYOUT -> ChoiceSheet(
            title = strings.readingLayout,
            options = listOf(
                SheetOption(ReaderLayout.PAGED, strings.layoutPaged),
                SheetOption(ReaderLayout.SCROLL, strings.layoutScroll),
            ),
            selected = state.readerLayout,
            onDismiss = { sheet = null },
            onSelect = onReaderLayout,
        )

        SettingsSheet.DEFINITION -> ChoiceSheet(
            title = strings.definitionMode,
            options = listOf(
                SheetOption(DefinitionMode.ZH, strings.definitionModeZh),
                SheetOption(DefinitionMode.MIXED, strings.definitionModeMixed),
                SheetOption(DefinitionMode.EN, strings.definitionModeEn),
            ),
            selected = state.definitionMode,
            onDismiss = { sheet = null },
            onSelect = onDefinitionMode,
        )

        SettingsSheet.GUIDE -> ChoiceSheet(
            title = strings.guideMode,
            options = listOf(
                SheetOption(GuideMode.BILINGUAL, strings.guideModeBilingual),
                SheetOption(GuideMode.EN, strings.guideModeEn),
                SheetOption(GuideMode.IMMERSIVE, strings.guideModeImmersive),
            ),
            selected = state.guideMode,
            onDismiss = { sheet = null },
            onSelect = onGuideMode,
        )

        SettingsSheet.GOAL -> ChoiceSheet(
            title = strings.dailyGoal,
            options = DailyGoal.OPTIONS.map { minutes ->
                SheetOption(minutes, strings.goalMinutesOption.format(minutes))
            },
            selected = DailyGoal.sanitize(state.dailyGoalMinutes),
            onDismiss = { sheet = null },
            onSelect = onDailyGoal,
            footer = strings.dailyGoalHint,
        )

        SettingsSheet.THEME -> ChoiceSheet(
            title = strings.theme,
            options = listOf(
                SheetOption(ThemeMode.SYSTEM, strings.themeSystem),
                SheetOption(ThemeMode.LIGHT, strings.themeLight),
                SheetOption(ThemeMode.DARK, strings.themeDark),
            ),
            selected = state.theme,
            onDismiss = { sheet = null },
            onSelect = onTheme,
        )

        SettingsSheet.LANGUAGE -> ChoiceSheet(
            title = strings.language,
            options = listOf(
                SheetOption(UiLanguage.ZH, strings.languageZh),
                SheetOption(UiLanguage.EN, strings.languageEn),
            ),
            selected = state.language,
            onDismiss = { sheet = null },
            onSelect = onLanguage,
        )

        null -> Unit
    }

    if (confirmRestore) {
        AlertDialog(
            onDismissRequest = { confirmRestore = false },
            title = { Text(strings.restoreDefaults) },
            text = { Text(strings.restoreDefaultsBody) },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmRestore = false
                        onRestoreDefaults()
                    },
                ) { Text(strings.restoreDefaults) }
            },
            dismissButton = {
                TextButton(onClick = { confirmRestore = false }) { Text(strings.close) }
            },
        )
    }
}

private enum class SettingsSheet { LAYOUT, DEFINITION, GUIDE, GOAL, THEME, LANGUAGE }

private fun readingFontLabel(strings: Strings, state: SettingsUiState): String =
    when (state.readingFont) {
        ReadingFont.SANS -> strings.fontSans
        ReadingFont.SERIF -> strings.fontSerif
    }

private fun definitionLabel(strings: Strings, state: SettingsUiState): String =
    when (state.definitionMode) {
        DefinitionMode.ZH -> strings.definitionModeZh
        DefinitionMode.MIXED -> strings.definitionModeMixed
        DefinitionMode.EN -> strings.definitionModeEn
    }

private fun guideLabel(strings: Strings, state: SettingsUiState): String =
    when (state.guideMode) {
        GuideMode.BILINGUAL -> strings.guideModeBilingual
        GuideMode.EN -> strings.guideModeEn
        GuideMode.IMMERSIVE -> strings.guideModeImmersive
    }

private fun themeLabel(strings: Strings, state: SettingsUiState): String =
    when (state.theme) {
        ThemeMode.SYSTEM -> strings.themeSystem
        ThemeMode.LIGHT -> strings.themeLight
        ThemeMode.DARK -> strings.themeDark
    }

private fun dictionarySummary(strings: Strings, dictionary: DictionaryUiState): String {
    val size = dictionary.sizeLabel ?: dictionary.installedVersion ?: dictionary.version ?: "—"
    return if (dictionary.phase == DictionaryPhase.INSTALLED) {
        strings.dictionaryInstalled.format(size)
    } else {
        strings.dictionaryNotInstalled.format(size)
    }
}
