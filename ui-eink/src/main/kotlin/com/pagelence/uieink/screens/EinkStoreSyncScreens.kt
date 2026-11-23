package com.pagelence.uieink.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pagelence.sync.SyncProvider
import com.pagelence.uim3.Strings
import com.pagelence.uim3.model.BuiltInBookUi
import com.pagelence.uim3.model.ImportPreviewUi
import com.pagelence.uim3.model.InstalledBookUi
import com.pagelence.uim3.model.StoreBookUi
import com.pagelence.uim3.model.StoreSourceUi
import com.pagelence.uim3.model.StoreUiState
import com.pagelence.uim3.model.SyncUiState
import com.pagelence.uieink.components.InkButton
import com.pagelence.uieink.components.InkDivider
import com.pagelence.uieink.components.InkListRow
import com.pagelence.uieink.components.InkNotice
import com.pagelence.uieink.components.InkSectionLabel
import com.pagelence.uieink.components.InkSelectRow
import com.pagelence.uieink.components.InkText
import com.pagelence.uieink.components.InkTextField
import com.pagelence.uieink.components.InkTextButton
import com.pagelence.uieink.components.InkTopBar
import com.pagelence.uieink.theme.LocalInkColors
import com.pagelence.uieink.theme.LocalInkTypography

/**
 * 书城：随包书单、自定义源书目、已下载管理与书源设置。
 *
 * 墨屏版不做封面墙与等级筛选（书少、翻页贵），重点是「能读 / 能下载 / 能删」。
 */
@Composable
fun EinkStoreScreen(
    strings: Strings,
    store: StoreUiState,
    onRefresh: () -> Unit,
    onOpenBook: (String) -> Unit,
    onOpenDetail: (String) -> Unit,
    onDownload: (bookId: String, sourceId: String?) -> Unit,
    onRemoveDownloaded: (String) -> Unit,
    onImportLocal: () -> Unit,
    onAddSource: (name: String, url: String) -> Unit,
    onToggleSource: (id: String, enabled: Boolean) -> Unit,
    onDeleteSource: (id: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalInkColors.current
    val typography = LocalInkTypography.current
    var showAddSource by remember { mutableStateOf(false) }
    var sourceName by remember { mutableStateOf("") }
    var sourceUrl by remember { mutableStateOf("") }
    Column(modifier.fillMaxSize()) {
        InkTopBar(
            title = strings.store,
            rightLabel = strings.refreshStore,
            onRight = onRefresh,
        )
        if (store.refreshing) InkNotice(strings.loading)
        store.error?.let { InkNotice(it) }
        store.notice?.let { InkNotice(it) }
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            InkSectionLabel(strings.builtIn)
            store.builtIn.forEach { book ->
                BuiltInRow(strings, book, onOpenBook, onOpenDetail)
                InkDivider()
            }

            if (store.books.isNotEmpty()) {
                InkSectionLabel(strings.store)
                store.books.forEach { book ->
                    RemoteBookRow(strings, store, book, onDownload, onOpenBook, onOpenDetail)
                    InkDivider()
                }
            }

            if (store.installed.isNotEmpty()) {
                InkSectionLabel(strings.downloadedBooks)
                store.installed.forEach { book ->
                    InstalledRow(strings, book, onOpenBook, onRemoveDownloaded)
                    InkDivider()
                }
            }

            InkSectionLabel(strings.storeCustomSources)
            store.sources.forEach { source ->
                SourceRow(strings, source, onToggleSource, onDeleteSource)
                InkDivider()
            }
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                InkButton(strings.storeAddSource, onClick = { showAddSource = !showAddSource })
            }
            if (showAddSource) {
                InkTextField(strings.storeSourceName, sourceName, { sourceName = it })
                InkTextField(strings.storeSourceUrl, sourceUrl, { sourceUrl = it })
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)) {
                    InkButton(
                        label = strings.annotationSave,
                        onClick = {
                            onAddSource(sourceName.trim(), sourceUrl.trim())
                            sourceName = ""
                            sourceUrl = ""
                            showAddSource = false
                        },
                        primary = true,
                        enabled = sourceUrl.isNotBlank(),
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                InkButton(strings.importBook, onClick = onImportLocal)
            }
            InkText(
                strings.storeSourceInvalid,
                style = typography.caption,
                color = colors.inkMuted,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun BuiltInRow(
    strings: Strings,
    book: BuiltInBookUi,
    onOpenBook: (String) -> Unit,
    onOpenDetail: (String) -> Unit,
) {
    val colors = LocalInkColors.current
    val typography = LocalInkTypography.current
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
        InkText(book.title, style = typography.title, maxLines = 2)
        Spacer(Modifier.height(4.dp))
        val meta = listOfNotNull(
            book.level,
            strings.unitCount.format(book.unitCount),
            if (book.available) null else strings.storeComingSoon,
        ).joinToString(" · ")
        InkText(meta, style = typography.caption, color = colors.inkMuted)
        Spacer(Modifier.height(8.dp))
        if (book.available) {
            Row {
                InkButton(strings.readNow, onClick = { onOpenBook(book.id) }, primary = true)
                InkTextButton(strings.details, onClick = { onOpenDetail(book.id) })
            }
        } else {
            InkText(book.description.orEmpty(), style = typography.bodySmall, color = colors.inkMuted)
        }
    }
}

@Composable
private fun RemoteBookRow(
    strings: Strings,
    store: StoreUiState,
    book: StoreBookUi,
    onDownload: (String, String?) -> Unit,
    onOpenBook: (String) -> Unit,
    onOpenDetail: (String) -> Unit,
) {
    val colors = LocalInkColors.current
    val typography = LocalInkTypography.current
    val downloading = store.downloadingId == book.id
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
        InkText(book.title, style = typography.title, maxLines = 2)
        Spacer(Modifier.height(4.dp))
        InkText(
            listOfNotNull(book.level, book.sizeLabel, book.sourceNames.joinToString("/"))
                .filter { it.isNotBlank() }
                .joinToString(" · "),
            style = typography.caption,
            color = colors.inkMuted,
        )
        Spacer(Modifier.height(8.dp))
        Row {
            when {
                downloading -> InkText(strings.loading, style = typography.ui, color = colors.inkMuted)
                book.installed -> InkButton(strings.readNow, onClick = { onOpenBook(book.id) }, primary = true)
                else -> InkButton(
                    label = strings.download,
                    onClick = { onDownload(book.id, book.sourceIds.firstOrNull()) },
                    primary = true,
                )
            }
            InkTextButton(strings.details, onClick = { onOpenDetail(book.id) })
        }
    }
}

@Composable
private fun InstalledRow(
    strings: Strings,
    book: InstalledBookUi,
    onOpenBook: (String) -> Unit,
    onRemove: (String) -> Unit,
) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        InkTextButton(strings.readNow, onClick = { onOpenBook(book.id) })
        InkListRow(
            title = book.title,
            subtitle = "${book.units} · ${book.sourceName}",
            modifier = Modifier.weight(1f),
        )
        InkTextButton(strings.delete, onClick = { onRemove(book.id) })
    }
}

@Composable
private fun SourceRow(
    strings: Strings,
    source: StoreSourceUi,
    onToggle: (String, Boolean) -> Unit,
    onDelete: (String) -> Unit,
) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        InkTextButton(
            label = if (source.enabled) "■" else "□",
            onClick = { onToggle(source.id, !source.enabled) },
        )
        InkListRow(
            title = source.name,
            subtitle = source.url,
            modifier = Modifier.weight(1f),
        )
        if (!source.builtIn) {
            InkTextButton(strings.delete, onClick = { onDelete(source.id) })
        }
    }
}

/**
 * 同步与备份：WebDAV 配置、立即同步、导出/导入。
 *
 * 密码不进入 UI 状态模型（明文只在本页的 Compose 状态里），保存时一次性交给
 * ViewModel 写入 Keystore。
 */
@Composable
fun EinkSyncScreen(
    strings: Strings,
    sync: SyncUiState,
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
    onConfirmImport: (Boolean) -> Unit,
    onCancelImport: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val copy = strings.syncCopy
    val colors = LocalInkColors.current
    val typography = LocalInkTypography.current
    var password by remember { mutableStateOf("") }
    Column(modifier.fillMaxSize()) {
        InkTopBar(title = copy.title, subtitle = copy.subtitle, backLabel = strings.back, onBack = onBack)
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            if (sync.configured) {
                InkSectionLabel(copy.sectionProvider)
                InkText(
                    sync.status ?: copy.statusConfigured,
                    style = typography.ui,
                    color = if (sync.tone == com.pagelence.uim3.model.SyncTone.ERROR) colors.ink else colors.inkSecondary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                    InkButton(strings.syncCopy.syncNow, onClick = onSyncNow, primary = true, enabled = !sync.busy)
                }
            }
            sync.notice?.let { InkNotice(it) }
            InkSegment(strings, sync, onProvider)
            InkTextField(copy.url, sync.url, onUrl, placeholder = copy.urlHint)
            InkTextField(copy.username, sync.username, onUsername, placeholder = copy.usernameHint)
            InkTextField(
                label = copy.password,
                value = password,
                onValueChange = { password = it },
                placeholder = if (sync.hasPassword) copy.passwordStored else copy.passwordHint,
            )
            InkSelectRow(copy.allowHttp, sync.allowHttp, { onAllowHttp(!sync.allowHttp) }, description = copy.allowHttpHint)
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                InkButton(
                    label = copy.saveAndTest,
                    onClick = { onSave(password); password = "" },
                    primary = true,
                    enabled = !sync.busy,
                )
            }
            if (sync.configured) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
                    InkTextButton(copy.disable, onClick = onDisable)
                }
            }
            InkDivider()
            InkSectionLabel(copy.sectionBackup)
            InkText(copy.backupHint, style = typography.caption, color = colors.inkMuted, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                InkButton(copy.export, onClick = onExport)
                Spacer(Modifier.height(0.dp))
                InkTextButton(copy.import, onClick = onImport)
            }
            sync.importPreview?.let { preview ->
                ImportPreviewBlock(strings, preview, onConfirmImport, onCancelImport)
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun InkSegment(
    strings: Strings,
    sync: SyncUiState,
    onProvider: (SyncProvider) -> Unit,
) {
    val copy = strings.syncCopy
    InkSectionLabel(copy.sectionProvider)
    listOf(
        SyncProvider.JIANGUOYUN to copy.providerJianguoyun,
        SyncProvider.NEXTCLOUD to copy.providerNextcloud,
        SyncProvider.SYNOLOGY to copy.providerSynology,
        SyncProvider.INFINICLOUD to copy.providerInfinicloud,
        SyncProvider.CUSTOM to copy.providerCustom,
    ).forEach { (provider, label) ->
        InkSelectRow(label, sync.provider == provider, { onProvider(provider) })
    }
    InkText(copy.providerHint, style = LocalInkTypography.current.caption, color = LocalInkColors.current.inkMuted, modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp))
    InkDivider()
}

@Composable
private fun ImportPreviewBlock(
    strings: Strings,
    preview: ImportPreviewUi,
    onConfirm: (Boolean) -> Unit,
    onCancel: () -> Unit,
) {
    val copy = strings.syncCopy
    val colors = LocalInkColors.current
    val typography = LocalInkTypography.current
    InkDivider()
    Column(Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
        InkText(copy.importPreviewTitle, style = typography.uiStrong)
        Spacer(Modifier.height(6.dp))
        InkText("${copy.importPreviewBooks}: ${preview.books}", style = typography.ui)
        InkText("${copy.importPreviewUnits}: ${preview.readUnits}", style = typography.ui)
        InkText("${copy.importPreviewCards}: ${preview.cards}", style = typography.ui)
        preview.level?.let { InkText("${copy.importPreviewLevel}: $it", style = typography.ui) }
        InkText("${copy.importPreviewUpdatedAt}: ${preview.updatedAt}", style = typography.ui)
        Spacer(Modifier.height(6.dp))
        InkText(copy.importPreviewHint, style = typography.caption, color = colors.inkMuted)
        Spacer(Modifier.height(8.dp))
        InkButton(copy.importMerge, onClick = { onConfirm(false) }, primary = true)
        Spacer(Modifier.height(8.dp))
        InkButton(copy.importOverwrite, onClick = { onConfirm(true) })
        Spacer(Modifier.height(8.dp))
        InkTextButton(copy.cancel, onClick = onCancel)
    }
}
