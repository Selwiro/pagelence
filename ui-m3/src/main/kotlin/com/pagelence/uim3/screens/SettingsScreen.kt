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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pagelence.core.prefs.DefinitionMode
import com.pagelence.core.prefs.FontSize
import com.pagelence.core.prefs.LineSpacing
import com.pagelence.core.prefs.PageMargin
import com.pagelence.core.prefs.ReadingFont
import com.pagelence.core.prefs.GuideMode
import com.pagelence.core.prefs.ReaderLayout
import com.pagelence.core.prefs.ThemeMode
import com.pagelence.uim3.Strings
import com.pagelence.uim3.UiLanguage
import com.pagelence.uim3.components.PageTopBar
import com.pagelence.uim3.components.ChoiceRow
import com.pagelence.uim3.components.PagelenceIcons
import com.pagelence.uim3.components.SectionLabel
import com.pagelence.uim3.model.DictionaryPhase
import com.pagelence.uim3.model.DictionaryUiState
import com.pagelence.uim3.model.SettingsUiState

/** 设置：阅读、外观、语言与词典、关于。偏好只写 `state.json`，不动进度。 */
@Composable
fun SettingsScreen(
    strings: Strings,
    state: SettingsUiState,
    dictionary: DictionaryUiState,
    appVersion: String,
    onBack: () -> Unit,
    onOpenSync: () -> Unit,
    onOpenAbout: () -> Unit,
    onDictionaryDownload: () -> Unit,
    onDictionaryPause: () -> Unit,
    onDictionaryDelete: () -> Unit,
    onDictionaryMeteredConfirm: () -> Unit,
    onDictionaryMeteredDismiss: () -> Unit,
    onLanguage: (UiLanguage) -> Unit,
    onTheme: (ThemeMode) -> Unit,
    onFontSize: (FontSize) -> Unit,
    onReadingFont: (ReadingFont) -> Unit,
    onLineSpacing: (LineSpacing) -> Unit,
    onPageMargin: (PageMargin) -> Unit,
    onReaderLayout: (ReaderLayout) -> Unit,
    onAnnotateHints: (Boolean) -> Unit,
    onDefinitionMode: (DefinitionMode) -> Unit,
    onGuideMode: (GuideMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    var confirmDictionaryDelete by remember { mutableStateOf(false) }
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
            SettingBlock(strings.readingFont) {
                ChoiceRow(
                    options = listOf(
                        ReadingFont.SANS to strings.fontSans,
                        ReadingFont.SERIF to strings.fontSerif,
                    ),
                    selected = state.readingFont,
                    onSelect = onReadingFont,
                )
            }
            SettingBlock(strings.lineSpacing) {
                ChoiceRow(
                    options = listOf(
                        LineSpacing.COMPACT to strings.lineSpacingCompact,
                        LineSpacing.NORMAL to strings.lineSpacingNormal,
                        LineSpacing.RELAXED to strings.lineSpacingRelaxed,
                    ),
                    selected = state.lineSpacing,
                    onSelect = onLineSpacing,
                )
            }
            SettingBlock(strings.pageMargin) {
                ChoiceRow(
                    options = listOf(
                        PageMargin.NARROW to strings.pageMarginNarrow,
                        PageMargin.NORMAL to strings.pageMarginNormal,
                        PageMargin.WIDE to strings.pageMarginWide,
                    ),
                    selected = state.pageMargin,
                    onSelect = onPageMargin,
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
            SectionLabel(strings.sectionDictionary)
            SettingBlock(strings.fullDictionary) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val meta = listOfNotNull(
                        dictionary.version?.let { strings.dictVersionLabel.format(it) },
                        dictionary.sizeLabel?.let { strings.dictSizeLabel.format(it) },
                    ).joinToString(" · ")
                    if (meta.isNotEmpty()) {
                        Text(
                            meta,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    dictionary.status?.let {
                        Text(it, style = MaterialTheme.typography.bodyMedium)
                    }
                    if (dictionary.phase == DictionaryPhase.DOWNLOADING) {
                        LinearProgressIndicator(
                            progress = { dictionary.progress },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        when {
                            dictionary.canDownload -> Button(onClick = onDictionaryDownload) {
                                Text(strings.dictDownload)
                            }

                            dictionary.canResume -> Button(onClick = onDictionaryDownload) {
                                Text(strings.dictResume)
                            }

                            dictionary.canRetry -> Button(onClick = onDictionaryDownload) {
                                Text(strings.dictRetry)
                            }
                        }
                        if (dictionary.canPause) {
                            OutlinedButton(onClick = onDictionaryPause) {
                                Text(strings.dictPause)
                            }
                        }
                        if (dictionary.canDelete) {
                            TextButton(onClick = { confirmDictionaryDelete = true }) {
                                Text(strings.dictDelete)
                            }
                        }
                    }
                    Text(
                        strings.dictLicenseNote,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
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
                onClick = onOpenSync,
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceContainer,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Icon(
                        imageVector = PagelenceIcons.Globe,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp),
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(strings.syncCopy.title, style = MaterialTheme.typography.titleMedium)
                        Text(
                            strings.syncCopy.subtitle,
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

    if (dictionary.confirmMetered) {
        AlertDialog(
            onDismissRequest = onDictionaryMeteredDismiss,
            title = { Text(strings.dictMeteredTitle) },
            text = {
                Text(strings.dictMeteredBody.format(dictionary.sizeLabel ?: "—"))
            },
            confirmButton = {
                TextButton(onClick = onDictionaryMeteredConfirm) { Text(strings.dictContinue) }
            },
            dismissButton = {
                TextButton(onClick = onDictionaryMeteredDismiss) { Text(strings.close) }
            },
        )
    }

    if (confirmDictionaryDelete) {
        AlertDialog(
            onDismissRequest = { confirmDictionaryDelete = false },
            title = { Text(strings.dictDelete) },
            text = { Text(strings.dictDeleteConfirm) },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmDictionaryDelete = false
                        onDictionaryDelete()
                    },
                ) { Text(strings.dictDelete) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDictionaryDelete = false }) { Text(strings.close) }
            },
        )
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
