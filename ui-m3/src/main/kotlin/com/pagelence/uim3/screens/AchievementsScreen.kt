package com.pagelence.uim3.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.unit.dp
import com.pagelence.uim3.Strings
import com.pagelence.uim3.components.PageTopBar
import com.pagelence.uim3.model.AchievementUi
import com.pagelence.uim3.model.AchievementWallUi

/**
 * 成就 (T168): the full wall behind the folded card on 「我的」. Unlocked items
 * come first, then the ones still in progress; tapping a cell repeats the rule
 * and the crossing date.
 */
@Composable
fun AchievementsScreen(
    strings: Strings,
    wall: AchievementWallUi,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    val selected = wall.items.firstOrNull { it.id == selectedId }
    val unlocked = wall.items.filter { it.unlocked }
    val locked = wall.items.filterNot { it.unlocked }

    Scaffold(
        modifier = modifier,
        topBar = { PageTopBar(title = strings.achievements, onBack = onBack) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item(key = "hint") {
                Text(
                    strings.achievementWallHint,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }
            if (wall.isEmpty) {
                item(key = "empty") {
                    Text(
                        strings.achievementWallEmpty,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (unlocked.isNotEmpty()) {
                item(key = "unlocked-header") {
                    GroupHeader(strings.achievementUnlocked, unlocked.size)
                }
                items(
                    items = unlocked.chunked(2),
                    key = { row -> row.joinToString("|") { it.id } },
                ) { row -> AchievementRow(strings, row) { selectedId = it } }
            }
            if (locked.isNotEmpty()) {
                item(key = "locked-header") {
                    GroupHeader(strings.achievementLocked, locked.size)
                }
                items(
                    items = locked.chunked(2),
                    key = { row -> row.joinToString("|") { it.id } },
                ) { row -> AchievementRow(strings, row) { selectedId = it } }
            }
        }
    }

    if (selected != null) {
        AchievementDialog(strings, selected) { selectedId = null }
    }
}

@Composable
private fun GroupHeader(label: String, count: Int) {
    Row(
        Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        Text(
            count.toString(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun AchievementRow(
    strings: Strings,
    items: List<AchievementUi>,
    onSelect: (String) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items.forEach { item ->
            AchievementCell(
                strings = strings,
                item = item,
                modifier = Modifier.weight(1f),
                onClick = { onSelect(item.id) },
            )
        }
        if (items.size == 1) Spacer(Modifier.weight(1f))
    }
}

@Composable
private fun AchievementCell(
    strings: Strings,
    item: AchievementUi,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        color = if (item.unlocked) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainer
        },
    ) {
        Column(Modifier.padding(12.dp).fillMaxWidth()) {
            Text(
                item.title,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 2,
            )
            Spacer(Modifier.height(4.dp))
            if (item.unlocked) {
                Text(
                    item.unlockedOn?.let { strings.achievementUnlockedOn.format(it) }
                        ?: strings.achievementUnlocked,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                LinearProgressIndicator(
                    progress = { item.fraction },
                    modifier = Modifier.fillMaxWidth().height(4.dp),
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    item.progressLabel ?: strings.achievementLocked,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun AchievementDialog(strings: Strings, item: AchievementUi, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(item.title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(item.description, style = MaterialTheme.typography.bodyMedium)
                Text(
                    if (item.unlocked) {
                        item.unlockedOn?.let { strings.achievementUnlockedOn.format(it) }
                            ?: strings.achievementUnlocked
                    } else {
                        item.progressLabel?.let { "${strings.achievementLocked} · $it" }
                            ?: strings.achievementLocked
                    },
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (!item.unlocked) {
                    LinearProgressIndicator(
                        progress = { item.fraction },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(strings.close) }
        },
    )
}
