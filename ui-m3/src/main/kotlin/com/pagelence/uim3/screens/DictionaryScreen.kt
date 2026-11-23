package com.pagelence.uim3.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pagelence.uim3.Strings
import com.pagelence.uim3.components.PageTopBar
import com.pagelence.uim3.model.DictionaryPhase
import com.pagelence.uim3.model.DictionaryUiState

/**
 * 完整词表 (T168): the download used to be the heaviest block on the settings
 * page for something most readers touch once. It moved here whole — state,
 * actions, licence note — so the settings home only shows a summary row.
 */
@Composable
fun DictionaryScreen(
    strings: Strings,
    dictionary: DictionaryUiState,
    onBack: () -> Unit,
    onDownload: () -> Unit,
    onPause: () -> Unit,
    onDelete: () -> Unit,
    onMeteredConfirm: () -> Unit,
    onMeteredDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var confirmDelete by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        topBar = { PageTopBar(title = strings.fullDictionary, onBack = onBack) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Surface(
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
                        Text(it, style = MaterialTheme.typography.bodyLarge)
                    }
                    if (dictionary.phase == DictionaryPhase.DOWNLOADING) {
                        LinearProgressIndicator(
                            progress = { dictionary.progress },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        when {
                            dictionary.canDownload -> Button(onClick = onDownload) {
                                Text(strings.dictDownload)
                            }

                            dictionary.canResume -> Button(onClick = onDownload) {
                                Text(strings.dictResume)
                            }

                            dictionary.canRetry -> Button(onClick = onDownload) {
                                Text(strings.dictRetry)
                            }
                        }
                        if (dictionary.canPause) {
                            OutlinedButton(onClick = onPause) { Text(strings.dictPause) }
                        }
                        if (dictionary.canDelete) {
                            TextButton(onClick = { confirmDelete = true }) { Text(strings.dictDelete) }
                        }
                    }
                    Text(
                        strings.dictLicenseNote,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }

    if (dictionary.confirmMetered) {
        AlertDialog(
            onDismissRequest = onMeteredDismiss,
            title = { Text(strings.dictMeteredTitle) },
            text = { Text(strings.dictMeteredBody.format(dictionary.sizeLabel ?: "—")) },
            confirmButton = {
                TextButton(onClick = onMeteredConfirm) { Text(strings.dictContinue) }
            },
            dismissButton = {
                TextButton(onClick = onMeteredDismiss) { Text(strings.close) }
            },
        )
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(strings.dictDelete) },
            text = { Text(strings.dictDeleteConfirm) },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmDelete = false
                        onDelete()
                    },
                ) { Text(strings.dictDelete) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text(strings.close) }
            },
        )
    }
}
