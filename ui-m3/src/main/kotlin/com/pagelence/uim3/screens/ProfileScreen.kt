package com.pagelence.uim3.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pagelence.uim3.model.HeatmapUi
import com.pagelence.core.progress.CefrLevel
import com.pagelence.uim3.Strings
import com.pagelence.uim3.components.LevelOptionRow
import com.pagelence.uim3.components.MetaChip
import com.pagelence.uim3.components.PageTopBar
import com.pagelence.uim3.levelDescription
import com.pagelence.uim3.levelName
import com.pagelence.uim3.levelOptions
import com.pagelence.uim3.levelSourceLabel
import com.pagelence.uim3.levelVocabulary
import com.pagelence.uim3.model.ProfileUiState

/** "我的": self-assessed level, aggregate stats and the learning heat map. */
@Composable
fun ProfileScreen(
    strings: Strings,
    state: ProfileUiState,
    onBack: (() -> Unit)?,
    onOpenSettings: () -> Unit,
    onSelectLevel: (CefrLevel) -> Unit,
    onOpenAssessment: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showLevelDialog by rememberSaveable { mutableStateOf(false) }

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
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item(key = "level") {
                LevelCard(
                    strings = strings,
                    state = state,
                    onSetLevel = { showLevelDialog = true },
                    onOpenAssessment = onOpenAssessment,
                )
            }
            item(key = "stats") { StatsRow(strings, state) }
            item(key = "heatmap") { HeatmapCard(strings, state.heatmap) }
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
}

@Composable
private fun LevelCard(
    strings: Strings,
    state: ProfileUiState,
    onSetLevel: () -> Unit,
    onOpenAssessment: () -> Unit,
) {
    val level = state.level
    ElevatedCard(shape = MaterialTheme.shapes.large, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    strings.myLevel,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f),
                )
                if (level != null) {
                    MetaChip(levelSourceLabel(strings, state.levelSource))
                }
            }
            Spacer(Modifier.height(10.dp))
            if (level == null) {
                Text(
                    strings.levelUnset,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    strings.levelAffectsHint,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Text(levelName(strings, level), style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.height(4.dp))
                Text(
                    levelDescription(strings, level),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    levelVocabulary(strings, level),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    strings.levelAffectsHint,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onSetLevel) {
                    Text(if (state.level == null) strings.setLevel else strings.changeLevel)
                }
                OutlinedButton(onClick = onOpenAssessment) {
                    Text(strings.assessment)
                }
            }
        }
    }
}

/**
 * 365-day heat map from `dailyActivity`. Only the gray level carries meaning, so
 * an e-ink screen reads the same as a phone; the grid scrolls horizontally and
 * opens on the newest weeks.
 */
@Composable
private fun HeatmapCard(strings: Strings, heatmap: HeatmapUi) {
    val scroll = rememberScrollState()
    LaunchedEffect(heatmap.weeks.size) {
        scroll.scrollTo(Int.MAX_VALUE)
    }
    ElevatedCard(shape = MaterialTheme.shapes.large, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(strings.heatmap, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Text(
                    strings.heatmapSummary.format(heatmap.activeDays, heatmap.totalUnits),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(10.dp))
            if (heatmap.isEmpty) {
                Text(
                    strings.heatmapEmpty,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Row(Modifier.fillMaxWidth().horizontalScroll(scroll)) {
                    heatmap.weeks.forEach { week ->
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            week.forEach { cell -> HeatmapCellBox(cell?.level ?: 0) }
                        }
                        Spacer(Modifier.width(2.dp))
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        strings.heatmapLess,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    repeat(5) { level -> HeatmapCellBox(level) }
                    Text(
                        strings.heatmapMore,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                strings.heatmapHint,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun HeatmapCellBox(level: Int) {
    val color = if (level == 0) {
        MaterialTheme.colorScheme.surfaceContainerHighest
    } else {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f + 0.18f * (level - 1))
    }
    Surface(
        modifier = Modifier.size(10.dp),
        shape = MaterialTheme.shapes.extraSmall,
        color = color,
    ) {}
}

@Composable
private fun StatsRow(strings: Strings, state: ProfileUiState) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        StatCard(
            value = state.finishedBooks.toString(),
            label = strings.booksFinishedShort,
            modifier = Modifier.weight(1f),
        )
        StatCard(
            value = "${state.totalReadUnits}/${state.totalUnits}",
            label = strings.unitsReadShort,
            modifier = Modifier.weight(1f),
        )
        StatCard(
            value = state.readingDays.toString(),
            label = strings.readingDaysShort,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun StatCard(value: String, label: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(value, style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(4.dp))
            Text(
                label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
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
