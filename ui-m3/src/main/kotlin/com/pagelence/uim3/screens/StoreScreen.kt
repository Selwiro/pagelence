package com.pagelence.uim3.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pagelence.uim3.Strings
import com.pagelence.uim3.components.BookCover
import com.pagelence.uim3.components.rememberCoverPainter
import com.pagelence.uim3.components.EmptyState
import com.pagelence.uim3.components.MetaChip
import com.pagelence.uim3.components.PageTopBar
import com.pagelence.uim3.components.SectionLabel
import com.pagelence.uim3.model.BuiltInBookUi
import com.pagelence.uim3.model.StoreBookUi
import com.pagelence.uim3.model.StoreSourceUi
import com.pagelence.uim3.model.StoreUiState

/**
 * 书城：先展示随包内置的五档书目（可按 CEFR 筛选），再展示自定义源的可下载
 * 书目、已下载管理。默认源尚未托管，因此不在界面出现，也不会发起网络请求。
 */
@Composable
fun StoreScreen(
    strings: Strings,
    state: StoreUiState,
    onOpenBook: (String) -> Unit,
    onOpenDetail: (String) -> Unit,
    onRefresh: () -> Unit,
    onToggleLocalHttp: (Boolean) -> Unit,
    onAddSource: (String, String) -> Unit,
    onToggleSource: (String, Boolean) -> Unit,
    onDeleteSource: (String) -> Unit,
    onDownload: (String, String?) -> Unit,
    onRemoveDownloaded: (String) -> Unit,
    onImportLocal: () -> Unit,
    onImportOnline: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showMenu by rememberSaveable { mutableStateOf(false) }
    var showAddSource by rememberSaveable { mutableStateOf(false) }
    var showImportOnline by rememberSaveable { mutableStateOf(false) }
    var conflictBook by rememberSaveable { mutableStateOf<String?>(null) }
    var levelFilter by rememberSaveable { mutableStateOf<String?>(null) }

    val levels = (state.builtIn.mapNotNull { it.level } + state.books.mapNotNull { it.level })
        .distinct()
        .sorted()
    val builtIn = state.builtIn.filter { levelFilter == null || it.level == levelFilter }
    val builtInIds = state.builtIn.map { it.id }.toSet()
    // A custom source may offer a title that already ships with the app; the
    // built-in card is the copy the reader should use.
    val catalog = state.books.filter { it.id !in builtInIds && (levelFilter == null || it.level == levelFilter) }
    val conflict = catalog.firstOrNull { it.id == conflictBook }

    Scaffold(
        modifier = modifier,
        topBar = {
            PageTopBar(
                title = strings.store,
                actions = {
                    if (state.importing) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(12.dp))
                    }
                    Box {
                        IconButton(
                            onClick = { showMenu = true },
                            enabled = !state.importing,
                        ) {
                            Icon(Icons.Filled.MoreVert, contentDescription = strings.storeMenu)
                        }
                        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                            DropdownMenuItem(
                                text = { Text(strings.storeImportLocal) },
                                onClick = {
                                    showMenu = false
                                    onImportLocal()
                                },
                            )
                            DropdownMenuItem(
                                text = { Text(strings.storeImportOnline) },
                                onClick = {
                                    showMenu = false
                                    showImportOnline = true
                                },
                            )
                            DropdownMenuItem(
                                text = { Text(strings.storeAddSource) },
                                onClick = {
                                    showMenu = false
                                    showAddSource = true
                                },
                            )
                        }
                    }
                },
            )
        },
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "built-in-header") {
                Column {
                    SectionLabel(strings.storeBuiltInTitle)
                    Text(
                        strings.storeBuiltInHint,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (levels.isNotEmpty()) {
                item(key = "level-filter") {
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        FilterChip(
                            selected = levelFilter == null,
                            onClick = { levelFilter = null },
                            label = { Text(strings.storeFilterAll) },
                        )
                        levels.forEach { level ->
                            FilterChip(
                                selected = levelFilter == level,
                                onClick = { levelFilter = if (levelFilter == level) null else level },
                                label = { Text(level) },
                            )
                        }
                    }
                }
            }

            state.error?.let { message ->
                item(key = "error") {
                    NoticeSurface(message, MaterialTheme.colorScheme.errorContainer)
                }
            }
            state.notice?.let { message ->
                item(key = "notice") {
                    NoticeSurface(message, MaterialTheme.colorScheme.secondaryContainer)
                }
            }
            if (state.importing) {
                item(key = "importing") {
                    NoticeSurface(strings.storeImporting, MaterialTheme.colorScheme.secondaryContainer)
                }
            }

            if (builtIn.isEmpty() && catalog.isEmpty()) {
                item(key = "empty") {
                    EmptyState(
                        icon = Icons.Filled.ShoppingCart,
                        title = strings.storeEmptyCatalog,
                        body = strings.storeEmptyHint,
                    )
                }
            } else {
                items(builtIn, key = { "built-in-${it.id}" }) { book ->
                    BuiltInBookCard(
                        strings = strings,
                        book = book,
                        onRead = { onOpenBook(book.id) },
                        onOpenDetail = { onOpenDetail(book.id) },
                    )
                }
                if (catalog.isNotEmpty()) {
                    item(key = "custom-catalog-title") { SectionLabel(strings.storeCustomCatalogTitle) }
                    items(catalog, key = { it.id }) { book ->
                        BookCard(
                            strings = strings,
                            book = book,
                            onRead = { onOpenBook(book.id) },
                            onOpenDetail = { onOpenDetail(book.id) },
                            onDownload = {
                                if (book.sourceNames.size > 1) {
                                    conflictBook = book.id
                                } else {
                                    onDownload(book.id, null)
                                }
                            },
                        )
                    }
                }
            }

            if (state.installed.isNotEmpty()) {
                item(key = "installed-title") { SectionLabel(strings.storeInstalledTitle) }
                items(state.installed, key = { "installed-${it.id}" }) { installed ->
                    OutlinedCard(
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth().clickable { onOpenDetail(installed.id) },
                    ) {
                        Row(
                            modifier = Modifier.padding(start = 14.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(installed.title, style = MaterialTheme.typography.titleSmall)
                                Text(
                                    "${strings.unitCountText(installed.units)} · ${installed.sourceName}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            TextButton(onClick = { onOpenBook(installed.id) }) { Text(strings.readNow) }
                            IconButton(onClick = { onRemoveDownloaded(installed.id) }) {
                                Icon(Icons.Filled.Close, contentDescription = strings.storeRemove)
                            }
                        }
                    }
                }
            }

            item(key = "custom-title") { SectionLabel(strings.storeCustomSources) }
            items(state.sources.filterNot { it.builtIn }, key = { it.id }) { source ->
                CustomSourceRow(strings, source, onToggleSource, onDeleteSource)
            }
            item(key = "custom-actions") {
                Column {
                    state.sources.filterNot { it.builtIn }.takeIf { it.isNotEmpty() }?.let { sources ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TextButton(onClick = onRefresh, enabled = !state.refreshing) {
                                Text(strings.storeRefreshCustom)
                            }
                            if (state.refreshing) {
                                Spacer(Modifier.width(8.dp))
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                )
                            }
                            sources.firstOrNull()?.let { source ->
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    state.status ?: source.name,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(strings.storeLocalHttp, style = MaterialTheme.typography.bodyMedium)
                            Text(
                                strings.storeLocalHttpHint,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(checked = state.allowLocalHttp, onCheckedChange = onToggleLocalHttp)
                    }
                }
            }
        }
    }

    if (showAddSource) {
        AddSourceDialog(
            strings = strings,
            onDismiss = { showAddSource = false },
            onAdd = { name, url ->
                showAddSource = false
                onAddSource(name, url)
            },
        )
    }
    if (showImportOnline) {
        ImportOnlineDialog(
            strings = strings,
            onDismiss = { showImportOnline = false },
            onImport = { url ->
                showImportOnline = false
                onImportOnline(url)
            },
        )
    }
    conflict?.let { book ->
        AlertDialog(
            onDismissRequest = { conflictBook = null },
            title = { Text(strings.storeChooseSource) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    book.sourceNames.forEachIndexed { index, name ->
                        OutlinedCard(
                            onClick = {
                                onDownload(book.id, book.sourceIds.getOrNull(index))
                                conflictBook = null
                            },
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                name,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.padding(14.dp),
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { conflictBook = null }) { Text(strings.close) }
            },
        )
    }
}

@Composable
private fun NoticeSurface(message: String, color: androidx.compose.ui.graphics.Color) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = color,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(14.dp),
        )
    }
}

@Composable
private fun BuiltInBookCard(
    strings: Strings,
    book: BuiltInBookUi,
    onRead: () -> Unit,
    onOpenDetail: () -> Unit,
) {
    ElevatedCard(
        shape = MaterialTheme.shapes.large,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpenDetail),
    ) {
        Column(Modifier.padding(18.dp)) {
            Row {
                BookCover(
                    title = book.coverTitle,
                    author = book.author,
                    seed = book.id,
                    monogram = book.monogram,
                    cover = rememberCoverPainter(book.coverPath),
                    modifier = Modifier.width(64.dp),
                )
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        book.title,
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    book.author?.let {
                        Text(
                            it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        book.level?.let { MetaChip(it) }
                        MetaChip(
                            if (book.available) strings.storeBadgeBuiltIn else strings.storeComingSoon,
                        )
                    }
                }
            }
            book.description?.takeIf { it.isNotBlank() }?.let {
                Spacer(Modifier.height(8.dp))
                Text(
                    it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (book.available) strings.unitCountText(book.unitCount) else strings.storeComingSoonHint,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                if (book.available) {
                    Button(onClick = onRead) { Text(strings.readNow) }
                }
            }
        }
    }
}

@Composable
private fun BookCard(
    strings: Strings,
    book: StoreBookUi,
    onRead: () -> Unit,
    onOpenDetail: () -> Unit,
    onDownload: () -> Unit,
) {
    ElevatedCard(
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onOpenDetail),
    ) {
        Column(Modifier.padding(18.dp)) {
            Row {
                BookCover(
                    title = book.coverTitle,
                    author = book.author,
                    seed = book.id,
                    modifier = Modifier.width(64.dp),
                )
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        book.title,
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    book.author?.let {
                        Text(
                            it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        book.level?.let { MetaChip(it) }
                        MetaChip(strings.sourcesText(book.sourceNames.size))
                        if (book.installed) MetaChip(strings.storeInstalled)
                    }
                }
            }
            book.description?.takeIf { it.isNotBlank() }?.let {
                Spacer(Modifier.height(8.dp))
                Text(
                    it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    strings.storeSize.format(book.sizeLabel),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                when {
                    book.downloading -> {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(10.dp))
                        Text(strings.storeDownloading, style = MaterialTheme.typography.labelLarge)
                    }
                    book.installed && !book.updateAvailable -> {
                        Button(onClick = onRead) { Text(strings.readNow) }
                    }
                    else -> {
                        Button(onClick = onDownload) {
                            Text(if (book.installed) strings.storeUpdate else strings.storeDownload)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CustomSourceRow(
    strings: Strings,
    source: StoreSourceUi,
    onToggle: (String, Boolean) -> Unit,
    onDelete: (String) -> Unit,
) {
    OutlinedCard(shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(start = 14.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(source.name, style = MaterialTheme.typography.titleSmall)
                Text(
                    source.url,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Switch(checked = source.enabled, onCheckedChange = { onToggle(source.id, it) })
            IconButton(onClick = { onDelete(source.id) }) {
                Icon(Icons.Filled.Close, contentDescription = strings.storeRemove)
            }
        }
    }
}

@Composable
private fun AddSourceDialog(
    strings: Strings,
    onDismiss: () -> Unit,
    onAdd: (String, String) -> Unit,
) {
    var name by rememberSaveable { mutableStateOf("") }
    var url by rememberSaveable { mutableStateOf("") }
    val valid = url.trim().startsWith("https://") || url.trim().startsWith("http://")
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.storeAddSource) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(strings.storeSourceName) },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text(strings.storeSourceUrl) },
                    singleLine = true,
                )
                Text(
                    strings.storeLocalHttpHint,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onAdd(name.trim(), url.trim()) }, enabled = valid) {
                Text(strings.storeAdd)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(strings.close) }
        },
    )
}

@Composable
private fun ImportOnlineDialog(
    strings: Strings,
    onDismiss: () -> Unit,
    onImport: (String) -> Unit,
) {
    var url by rememberSaveable { mutableStateOf("") }
    val valid = url.trim().startsWith("https://") || url.trim().startsWith("http://")
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.storeImportOnlineTitle) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text(strings.storeImportUrl) },
                    singleLine = true,
                )
                Text(
                    strings.storeImportOnlineHint,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onImport(url.trim()) }, enabled = valid) {
                Text(strings.storeImport)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(strings.close) }
        },
    )
}
