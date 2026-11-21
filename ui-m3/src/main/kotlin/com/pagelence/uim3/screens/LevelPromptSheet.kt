package com.pagelence.uim3.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pagelence.core.prefs.DefinitionMode
import com.pagelence.core.prefs.GuideMode
import com.pagelence.core.prefs.Language
import com.pagelence.uim3.Strings
import com.pagelence.uim3.UiLanguage
import com.pagelence.uim3.components.ChoiceRow
import com.pagelence.uim3.levelName
import com.pagelence.uim3.levelSourceLabel
import com.pagelence.uim3.model.LevelPromptUi

/**
 * Shown after a level is picked, before the app touches the interface
 * language, the meaning language or the guide mode. The reader either accepts
 * the recommendation, keeps the current settings, or adjusts the three modes
 * right here. Dismissing the sheet changes nothing.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LevelPromptSheet(
    strings: Strings,
    prompt: LevelPromptUi,
    onAccept: () -> Unit,
    onKeep: () -> Unit,
    onApply: (UiLanguage, DefinitionMode, GuideMode) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() },
    ) {
        var editing by remember(prompt) { mutableStateOf(false) }
        var language by remember(prompt) {
            mutableStateOf(prompt.changes.language?.toUi() ?: prompt.currentLanguage)
        }
        var definition by remember(prompt) {
            mutableStateOf(prompt.changes.definitionMode ?: prompt.currentDefinition)
        }
        var guide by remember(prompt) {
            mutableStateOf(prompt.changes.guideMode ?: prompt.currentGuide)
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = 24.dp, end = 24.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                stringFormat(strings.levelPromptTitle, levelName(strings, prompt.level)),
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                levelSourceLabel(strings, prompt.source),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                strings.levelPromptBody,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (editing) {
                ModeBlock(strings.language) {
                    ChoiceRow(
                        options = listOf(
                            UiLanguage.ZH to strings.languageZh,
                            UiLanguage.EN to strings.languageEn,
                        ),
                        selected = language,
                        onSelect = { language = it },
                    )
                }
                ModeBlock(strings.definitionMode) {
                    ChoiceRow(
                        options = listOf(
                            DefinitionMode.ZH to strings.definitionModeZh,
                            DefinitionMode.MIXED to strings.definitionModeMixed,
                            DefinitionMode.EN to strings.definitionModeEn,
                        ),
                        selected = definition,
                        onSelect = { definition = it },
                    )
                }
                ModeBlock(strings.guideMode) {
                    ChoiceRow(
                        options = listOf(
                            GuideMode.BILINGUAL to strings.guideModeBilingual,
                            GuideMode.EN to strings.guideModeEn,
                            GuideMode.IMMERSIVE to strings.guideModeImmersive,
                        ),
                        selected = guide,
                        onSelect = { guide = it },
                    )
                }
                Button(
                    onClick = { onApply(language, definition, guide) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(strings.levelPromptApply)
                }
                TextButton(onClick = onKeep, modifier = Modifier.fillMaxWidth()) {
                    Text(strings.levelPromptKeep)
                }
                TextButton(onClick = { editing = false }, modifier = Modifier.fillMaxWidth()) {
                    Text(strings.levelPromptBack)
                }
            } else {
                prompt.changes.language?.let { recommended ->
                    ModeChangeRow(
                        label = strings.language,
                        from = languageLabel(strings, prompt.currentLanguage),
                        to = languageLabel(strings, recommended.toUi()),
                    )
                }
                prompt.changes.definitionMode?.let { recommended ->
                    ModeChangeRow(
                        label = strings.definitionMode,
                        from = definitionLabel(strings, prompt.currentDefinition),
                        to = definitionLabel(strings, recommended),
                    )
                }
                prompt.changes.guideMode?.let { recommended ->
                    ModeChangeRow(
                        label = strings.guideMode,
                        from = guideLabel(strings, prompt.currentGuide),
                        to = guideLabel(strings, recommended),
                    )
                }
                Button(onClick = onAccept, modifier = Modifier.fillMaxWidth()) {
                    Text(strings.levelPromptAccept)
                }
                OutlinedButton(onClick = onKeep, modifier = Modifier.fillMaxWidth()) {
                    Text(strings.levelPromptKeep)
                }
                TextButton(onClick = { editing = true }, modifier = Modifier.fillMaxWidth()) {
                    Text(strings.levelPromptAdjust)
                }
            }

            Text(
                strings.levelPromptHint,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ModeChangeRow(label: String, from: String, to: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge,
        )
        Text(
            from,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            "→",
            modifier = Modifier.padding(horizontal = 8.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            to,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun ModeBlock(title: String, content: @Composable () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        content()
    }
}

private fun Language.toUi(): UiLanguage =
    if (this == Language.EN) UiLanguage.EN else UiLanguage.ZH

private fun languageLabel(strings: Strings, language: UiLanguage): String =
    if (language == UiLanguage.EN) strings.languageEn else strings.languageZh

private fun definitionLabel(strings: Strings, mode: DefinitionMode): String = when (mode) {
    DefinitionMode.ZH -> strings.definitionModeZh
    DefinitionMode.MIXED -> strings.definitionModeMixed
    DefinitionMode.EN -> strings.definitionModeEn
}

private fun guideLabel(strings: Strings, mode: GuideMode): String = when (mode) {
    GuideMode.BILINGUAL -> strings.guideModeBilingual
    GuideMode.EN -> strings.guideModeEn
    GuideMode.IMMERSIVE -> strings.guideModeImmersive
}

private fun stringFormat(template: String, value: String): String =
    String.format(template, value)
