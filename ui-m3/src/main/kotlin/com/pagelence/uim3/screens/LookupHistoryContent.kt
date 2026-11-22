package com.pagelence.uim3.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pagelence.uim3.Strings
import com.pagelence.uim3.model.LookupHistoryUi

/** 复习 → 查词：最近查过的词，点一条回到原句并重新打开词义（T161）。 */
@Composable
internal fun LookupHistoryContent(
    strings: Strings,
    lookups: List<LookupHistoryUi>,
    onOpen: (LookupHistoryUi) -> Unit,
    onClear: () -> Unit,
) {
    var confirmClear by remember { mutableStateOf(false) }
    if (lookups.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            Column(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 40.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(strings.lookupEmptyTitle, style = MaterialTheme.typography.titleMedium)
                Text(
                    strings.lookupEmptyHint,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        return
    }
    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                strings.lookupCount.format(lookups.size),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = { confirmClear = true }) {
                Text(strings.lookupClear)
            }
        }
        LazyColumn(Modifier.fillMaxSize()) {
            items(lookups, key = { it.id }) { entry ->
                LookupRow(strings = strings, entry = entry, onClick = { onOpen(entry) })
            }
            item { Spacer(Modifier.height(20.dp)) }
        }
    }
    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text(strings.lookupClearTitle) },
            text = { Text(strings.lookupClearBody) },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmClear = false
                        onClear()
                    },
                ) { Text(strings.lookupClear) }
            },
            dismissButton = {
                TextButton(onClick = { confirmClear = false }) {
                    Text(strings.annotationCancel)
                }
            },
        )
    }
}

@Composable
private fun LookupRow(
    strings: Strings,
    entry: LookupHistoryUi,
    onClick: () -> Unit,
) {
    val enabled = entry.available
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                entry.term,
                style = MaterialTheme.typography.titleMedium,
                color = if (enabled) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
            entry.ipa?.takeIf { it.isNotBlank() }?.let { ipa ->
                Text(
                    "  /$ipa/",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (entry.lemma.isNotBlank() && !entry.lemma.equals(entry.term, ignoreCase = true)) {
                Text(
                    "  ${strings.lemmaBase} ${entry.lemma}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
        }
        entry.gloss?.takeIf { it.isNotBlank() }?.let { gloss ->
            Text(
                gloss,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            listOfNotNull(
                entry.bookTitle,
                entry.unitLabel.takeIf { it.isNotBlank() },
                if (enabled) entry.lookedUpAtLabel else strings.lookupBookRemoved,
            ).joinToString(" · "),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
}
