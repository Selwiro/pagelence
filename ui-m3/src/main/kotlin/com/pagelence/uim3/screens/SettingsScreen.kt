package com.pagelence.uim3.screens

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.Icons
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pagelence.core.prefs.DefinitionMode
import com.pagelence.core.prefs.FontSize
import com.pagelence.core.prefs.GuideMode
import com.pagelence.core.prefs.ReaderLayout
import com.pagelence.core.prefs.ThemeMode
import com.pagelence.uim3.Strings
import com.pagelence.uim3.UiLanguage
import com.pagelence.uim3.components.PageTopBar
import com.pagelence.uim3.components.ChoiceRow
import com.pagelence.uim3.components.PagelenceIcons
import com.pagelence.uim3.components.SectionLabel
import com.pagelence.uim3.model.SettingsUiState

/** 设置：阅读、外观、语言与词典、关于。偏好只写 `state.json`，不动进度。 */
@Composable
fun SettingsScreen(
    strings: Strings,
    state: SettingsUiState,
    appVersion: String,
    onBack: () -> Unit,
    onOpenAbout: () -> Unit,
    onLanguage: (UiLanguage) -> Unit,
    onTheme: (ThemeMode) -> Unit,
    onFontSize: (FontSize) -> Unit,
    onReaderLayout: (ReaderLayout) -> Unit,
    onAnnotateHints: (Boolean) -> Unit,
    onDefinitionMode: (DefinitionMode) -> Unit,
    onGuideMode: (GuideMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = { PageTopBar(title = strings.settings, onBack = onBack) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        ) {
            SectionLabel(strings.sectionReading)
            SettingBlock(strings.fontSize) {
                ChoiceRow(
                    options = listOf(
                        FontSize.SMALL to strings.fontSmall,
                        FontSize.MEDIUM to strings.fontMedium,
                        FontSize.LARGE to strings.fontLarge,
                        FontSize.XLARGE to strings.fontXLarge,
                    ),
                    selected = state.fontSize,
                    onSelect = onFontSize,
                )
            }
            SettingBlock(strings.readingLayout) {
                ChoiceRow(
                    options = listOf(
                        ReaderLayout.PAGED to strings.layoutPaged,
                        ReaderLayout.SCROLL to strings.layoutScroll,
                    ),
                    selected = state.readerLayout,
                    onSelect = onReaderLayout,
                )
            }
            SettingBlock(strings.definitionMode) {
                ChoiceRow(
                    options = listOf(
                        DefinitionMode.ZH to strings.definitionModeZh,
                        DefinitionMode.MIXED to strings.definitionModeMixed,
                        DefinitionMode.EN to strings.definitionModeEn,
                    ),
                    selected = state.definitionMode,
                    onSelect = onDefinitionMode,
                )
            }
            SettingBlock(strings.guideMode) {
                ChoiceRow(
                    options = listOf(
                        GuideMode.BILINGUAL to strings.guideModeBilingual,
                        GuideMode.EN to strings.guideModeEn,
                        GuideMode.IMMERSIVE to strings.guideModeImmersive,
                    ),
                    selected = state.guideMode,
                    onSelect = onGuideMode,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(strings.annotateHints, style = MaterialTheme.typography.titleMedium)
                    Text(
                        strings.annotateHintsHint,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(checked = state.annotateHints, onCheckedChange = onAnnotateHints)
            }

            Spacer(Modifier.height(8.dp))
            HorizontalDivider()
            SectionLabel(strings.sectionAppearance)
            SettingBlock(strings.theme) {
                ChoiceRow(
                    options = listOf(
                        ThemeMode.SYSTEM to strings.themeSystem,
                        ThemeMode.LIGHT to strings.themeLight,
                        ThemeMode.DARK to strings.themeDark,
                    ),
                    selected = state.theme,
                    onSelect = onTheme,
                )
            }
            SettingBlock(strings.language) {
                ChoiceRow(
                    options = listOf(
                        UiLanguage.ZH to strings.languageZh,
                        UiLanguage.EN to strings.languageEn,
                    ),
                    selected = state.language,
                    onSelect = onLanguage,
                )
            }

            Spacer(Modifier.height(8.dp))
            HorizontalDivider()
            Surface(
                onClick = onOpenAbout,
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 28.dp),
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceContainer,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Icon(
                        imageVector = PagelenceIcons.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp),
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(strings.about, style = MaterialTheme.typography.titleMedium)
                        Text(
                            strings.aboutCopy.settingsSubtitle.format(appVersion),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Icon(
                        imageVector = PagelenceIcons.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingBlock(title: String, content: @Composable () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        content()
    }
}
