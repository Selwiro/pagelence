package com.pagelence.uim3.screens

import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.pagelence.sync.SyncProvider
import com.pagelence.uim3.Strings
import com.pagelence.uim3.components.PageTopBar
import com.pagelence.uim3.components.SectionLabel
import com.pagelence.uim3.model.SyncTone
import com.pagelence.uim3.model.SyncUiState

/**
 * WebDAV setup, sync status and local export/import. Default-off: nothing here
 * runs until the reader saves a file URL.
 */
@Composable
fun SyncScreen(
    strings: Strings,
    state: SyncUiState,
    onBack: () -> Unit,
    onProvider: (SyncProvider) -> Unit,
    onUrl: (String) -> Unit,
    onUsername: (String) -> Unit,
    onAllowHttp: (Boolean) -> Unit,
    onSave: (String) -> Unit,
    onSyncNow: () -> Unit,
    onDisable: () -> Unit,
    onExport: () -> Unit,
    onImport: () -> Unit,
    onDismissNotice: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val copy = strings.syncCopy
    var password by rememberSaveable { mutableStateOf("") }
    var confirmDisable by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        topBar = { PageTopBar(title = copy.title, onBack = onBack) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        ) {
            Text(
                copy.subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )

            Spacer(Modifier.height(14.dp))
            StatusCard(strings = strings, state = state)
            state.notice?.let { notice ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = notice,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (state.noticeIsError) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.primary
                        },
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = onDismissNotice) { Text(strings.close) }
                }
            }

            Spacer(Modifier.height(8.dp))
            SectionLabel(copy.sectionProvider)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ProviderChip(strings, SyncProvider.JIANGUOYUN, state, onProvider)
                ProviderChip(strings, SyncProvider.NEXTCLOUD, state, onProvider)
                ProviderChip(strings, SyncProvider.SYNOLOGY, state, onProvider)
                ProviderChip(strings, SyncProvider.INFINICLOUD, state, onProvider)
                ProviderChip(strings, SyncProvider.CUSTOM, state, onProvider)
            }
            Text(
                copy.providerHint,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp),
            )

            OutlinedTextField(
                value = state.url,
                onValueChange = onUrl,
                label = { Text(copy.url) },
                placeholder = { Text(state.templateUrl ?: copy.urlHint) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
            )
            OutlinedTextField(
                value = state.username,
                onValueChange = onUsername,
                label = { Text(copy.username) },
                placeholder = { Text(copy.usernameHint) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
            )
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text(copy.password) },
                visualTransformation = PasswordVisualTransformation(),
                supportingText = {
                    Text(if (state.hasPassword) copy.passwordStored else copy.passwordMissing)
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
            )
            Text(
                copy.passwordHint,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(copy.allowHttp, style = MaterialTheme.typography.titleSmall)
                    Text(
                        copy.allowHttpHint,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(checked = state.allowHttp, onCheckedChange = onAllowHttp)
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Button(
                    onClick = {
                        onSave(password)
                        password = ""
                    },
                    enabled = !state.busy && state.url.isNotBlank(),
                ) { Text(copy.saveAndTest) }
                OutlinedButton(
                    onClick = onSyncNow,
                    enabled = !state.busy && state.configured,
                ) { Text(copy.syncNow) }
            }
            if (state.configured) {
                TextButton(
                    onClick = { confirmDisable = true },
                    enabled = !state.busy,
                    modifier = Modifier.padding(top = 4.dp),
                ) { Text(copy.disable) }
            }

            Spacer(Modifier.height(8.dp))
            androidx.compose.material3.HorizontalDivider()
            SectionLabel(copy.sectionBackup)
            Text(
                copy.backupHint,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedButton(onClick = onExport, enabled = !state.busy) { Text(copy.export) }
                OutlinedButton(onClick = onImport, enabled = !state.busy) { Text(copy.import) }
            }

            Text(
                copy.help,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 12.dp, bottom = 28.dp),
            )
        }
    }

    if (confirmDisable) {
        AlertDialog(
            onDismissRequest = { confirmDisable = false },
            title = { Text(copy.disableTitle) },
            text = { Text(copy.disableBody) },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmDisable = false
                        onDisable()
                    },
                ) { Text(copy.disable) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDisable = false }) { Text(copy.cancel) }
            },
        )
    }
}

@Composable
private fun StatusCard(strings: Strings, state: SyncUiState) {
    val copy = strings.syncCopy
    val container = when (state.tone) {
        SyncTone.OK -> MaterialTheme.colorScheme.primaryContainer
        SyncTone.WARNING -> MaterialTheme.colorScheme.tertiaryContainer
        SyncTone.ERROR -> MaterialTheme.colorScheme.errorContainer
        SyncTone.IDLE -> MaterialTheme.colorScheme.surfaceContainer
    }
    val content = when (state.tone) {
        SyncTone.OK -> MaterialTheme.colorScheme.onPrimaryContainer
        SyncTone.WARNING -> MaterialTheme.colorScheme.onTertiaryContainer
        SyncTone.ERROR -> MaterialTheme.colorScheme.onErrorContainer
        SyncTone.IDLE -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = container,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (state.busy) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = content,
                )
            }
            Text(
                text = if (state.busy) copy.busy else state.status ?: copy.statusIdle,
                style = MaterialTheme.typography.bodyMedium,
                color = content,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun ProviderChip(
    strings: Strings,
    provider: SyncProvider,
    state: SyncUiState,
    onProvider: (SyncProvider) -> Unit,
) {
    val copy = strings.syncCopy
    val label = when (provider) {
        SyncProvider.JIANGUOYUN -> copy.providerJianguoyun
        SyncProvider.NEXTCLOUD -> copy.providerNextcloud
        SyncProvider.SYNOLOGY -> copy.providerSynology
        SyncProvider.INFINICLOUD -> copy.providerInfinicloud
        SyncProvider.CUSTOM -> copy.providerCustom
    }
    FilterChip(
        selected = state.provider == provider,
        onClick = { onProvider(provider) },
        label = { Text(label) },
    )
}
