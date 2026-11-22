package com.pagelence.uim3.screens

// ReaderSheets：从 ReaderScreen.kt 拆出；只做展示，不含业务规则。

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pagelence.core.prefs.GuideMode
import com.pagelence.uim3.Strings
import com.pagelence.uim3.components.MetaChip
import com.pagelence.uim3.components.SectionLabel
import com.pagelence.uim3.model.AnalysisUi
import com.pagelence.uim3.model.ReaderUiState
import com.pagelence.uim3.model.TocChapterUi
import com.pagelence.uim3.model.TocUnitUi

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AnalysisSheet(strings: Strings, analysis: AnalysisUi, onClose: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onClose,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() },
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp, vertical = 8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    strings.analysisTitle,
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.weight(1f),
                )
                MetaChip(analysis.level)
            }
            Spacer(Modifier.height(14.dp))
            SectionLabel(strings.analysisCore)
            Spacer(Modifier.height(6.dp))
            Text(analysis.core, style = MaterialTheme.typography.bodyLarge)
            if (analysis.parts.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                SectionLabel(strings.analysisParts)
                analysis.parts.forEach { part ->
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.Top) {
                        MetaChip(analysisRole(strings, part.role))
                        Spacer(Modifier.width(8.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                part.text,
                                style = MaterialTheme.typography.bodyMedium,
                                fontStyle = FontStyle.Italic,
                            )
                            part.description?.takeIf { it.isNotBlank() }?.let {
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    it,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
            analysis.order?.takeIf { it.isNotBlank() }?.let {
                Spacer(Modifier.height(16.dp))
                SectionLabel(strings.analysisOrder)
                Spacer(Modifier.height(6.dp))
                Text(it, style = MaterialTheme.typography.bodyLarge)
            }
            analysis.note?.takeIf { it.isNotBlank() }?.let {
                Spacer(Modifier.height(16.dp))
                SectionLabel(strings.analysisNote)
                Spacer(Modifier.height(6.dp))
                Text(it, style = MaterialTheme.typography.bodyLarge)
            }
            Spacer(Modifier.height(18.dp))
            TextButton(onClick = onClose) { Text(strings.analysisClose) }
            Spacer(Modifier.height(20.dp))
        }
    }
}

internal fun analysisRole(strings: Strings, role: String): String = when (role) {
    "subordinate" -> strings.analysisRoleSubordinate
    "relative" -> strings.analysisRoleRelative
    "nonfinite" -> strings.analysisRoleNonfinite
    "prepositional" -> strings.analysisRolePrepositional
    "appositive" -> strings.analysisRoleAppositive
    "parenthetical" -> strings.analysisRoleParenthetical
    else -> role
}

/** The 导读 text for the current mode: both languages, English only, or none. */
internal data class GuideCopy(val zh: String?, val en: String?) {
    val isEmpty: Boolean get() = zh.isNullOrBlank() && en.isNullOrBlank()
}

internal fun guideText(state: ReaderUiState): GuideCopy? = when (state.preference.guideMode) {
    GuideMode.BILINGUAL -> GuideCopy(state.introductionZh, state.introductionEn)
    GuideMode.EN -> GuideCopy(null, state.introductionEn ?: state.introductionZh)
    GuideMode.IMMERSIVE -> null
}?.takeUnless { it.isEmpty }

/** Contents sheet: every unit of the book, finished ones included. */
@Composable
internal fun TocSheet(
    strings: Strings,
    toc: List<TocChapterUi>,
    onOpen: (String) -> Unit,
) {
    Column(Modifier.fillMaxWidth().navigationBarsPadding()) {
        Column(Modifier.padding(horizontal = 22.dp)) {
            Text(strings.toc, style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(4.dp))
            Text(
                strings.tocHint,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(12.dp))
        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f, fill = false),
            contentPadding = PaddingValues(start = 14.dp, end = 14.dp, bottom = 20.dp),
        ) {
            toc.forEach { chapter ->
                if (chapter.title.isNotBlank()) {
                    item(key = "chapter:${chapter.id}") {
                        Text(
                            chapter.title,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 8.dp, top = 14.dp, bottom = 6.dp),
                        )
                    }
                }
                items(chapter.units, key = { it.id }) { unit ->
                    TocRow(strings, unit, onOpen)
                }
            }
        }
    }
}

@Composable
internal fun TocRow(
    strings: Strings,
    unit: TocUnitUi,
    onOpen: (String) -> Unit,
) {
    val background = if (unit.current) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceContainerLow
    }
    Surface(
        onClick = { onOpen(unit.id) },
        shape = MaterialTheme.shapes.medium,
        color = background,
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                unit.number.toString().padStart(2, '0'),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(30.dp),
            )
            Text(
                unit.title,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            when {
                unit.current -> MetaChip(strings.tocCurrent)
                unit.read -> MetaChip(strings.tocRead)
            }
        }
    }
}

internal fun levelSourceLabel(strings: Strings, source: String): String = when (source) {
    "aligned" -> strings.levelFromWordNet
    "derived" -> strings.levelFromRoot
    "tag" -> strings.levelFromExam
    "freq" -> strings.levelFromFrequency
    else -> ""
}
