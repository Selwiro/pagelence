package com.pagelence.uim3.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pagelence.uim3.Strings
import com.pagelence.uim3.components.BookCover
import com.pagelence.uim3.components.PageTopBar
import com.pagelence.uim3.components.wordCountParts
import com.pagelence.uim3.components.rememberCoverPainter
import com.pagelence.uim3.model.BookChapterUi
import com.pagelence.uim3.model.BookDetailUi

/**
 * The store's book page, modelled on the reference design: cover and identity,
 * a four-cell stat row, the shelf/read actions, an "about this book" row that
 * opens the info page, then the blurb and tags.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BookOverviewScreen(
    strings: Strings,
    state: BookDetailUi,
    onBack: () -> Unit,
    onToggleShelf: () -> Unit,
    onRead: () -> Unit,
    onReread: () -> Unit,
    onOpenFinished: () -> Unit,
    onDownload: (String?) -> Unit,
    onOpenInfo: () -> Unit,
    onOpenChapter: (BookChapterUi) -> Unit,
    modifier: Modifier = Modifier,
) {
    var pickSource by remember(state.bookId) { mutableStateOf(false) }
    val words = wordCountParts(state.wordCount, strings.uiLanguage)

    Scaffold(
        modifier = modifier,
        topBar = { PageTopBar(title = "", onBack = onBack) },
        bottomBar = {
            Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 3.dp) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(
                            onClick = onToggleShelf,
                            enabled = state.available,
                            modifier = Modifier.weight(1f).height(48.dp),
                        ) {
                            Text(if (state.onShelf) strings.onShelf else strings.addToShelf)
                        }
                        when {
                            state.available && state.finished -> Button(
                                onClick = onReread,
                                modifier = Modifier.weight(1f).height(48.dp),
                            ) {
                                Text(strings.readAgain)
                            }

                            state.available -> Button(
                                onClick = onRead,
                                modifier = Modifier.weight(1f).height(48.dp),
                            ) {
                                Text(strings.readNow)
                            }

                            state.downloadable -> Button(
                                onClick = {
                                    if (state.sourceIds.size > 1) pickSource = true
                                    else onDownload(state.sourceIds.firstOrNull())
                                },
                                enabled = !state.downloading,
                                modifier = Modifier.weight(1f).height(48.dp),
                            ) {
                                if (state.downloading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp,
                                    )
                                    Spacer(Modifier.width(8.dp))
                                }
                                Text(
                                    when {
                                        state.downloading -> strings.storeDownloading
                                        state.updateAvailable -> strings.storeUpdate
                                        else -> strings.storeDownload
                                    },
                                )
                            }

                            else -> Button(
                                onClick = {},
                                enabled = false,
                                modifier = Modifier.weight(1f).height(48.dp),
                            ) {
                                Text(strings.readNow)
                            }
                        }
                    }
                    if (!state.available && !state.downloadable) {
                        Text(
                            strings.bookPlannedHint,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        },
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item(key = "header") {
                Row {
                    BookCover(
                        title = state.coverTitle,
                        author = state.author,
                        seed = state.bookId,
                        monogram = state.monogram,
                        cover = rememberCoverPainter(state.coverPath),
                        modifier = Modifier.width(132.dp),
                    )
                    Spacer(Modifier.width(18.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            state.title,
                            style = MaterialTheme.typography.headlineSmall,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                        )
                        state.author?.let {
                            Spacer(Modifier.height(6.dp))
                            Text(
                                it,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        state.creator?.takeIf { it.isNotBlank() }?.let {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "${strings.bookCreator}${strings.labelSeparator}$it",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (state.finished) {
                            Spacer(Modifier.height(10.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Surface(
                                    shape = MaterialTheme.shapes.small,
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                ) {
                                    Text(
                                        strings.shelfFinished,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    )
                                }
                                state.finishedOn?.let { date ->
                                    Text(
                                        strings.finishedDate.format(date),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (state.finished) {
                item(key = "finished-summary") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onOpenFinished)
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            strings.finishedSummary,
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.weight(1f),
                        )
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            item(key = "stats") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    StatValueCell(
                        value = state.level ?: strings.bookInfoEmpty,
                        label = strings.bookDifficulty,
                        modifier = Modifier.weight(1f),
                    )
                    StatDivider()
                    StatIconCell(
                        icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = null) },
                        label = state.category ?: strings.bookInfoEmpty,
                        modifier = Modifier.weight(1f),
                    )
                    StatDivider()
                    StatValueCell(
                        value = words.value,
                        label = words.label,
                        modifier = Modifier.weight(1f),
                    )
                    StatDivider()
                    StatIconCell(
                        icon = { Icon(Icons.Filled.Home, contentDescription = null) },
                        label = sourceLabel(strings, state),
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            item(key = "about") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onOpenInfo)
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        strings.bookAbout,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f),
                    )
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            state.description?.takeIf { it.isNotBlank() }?.let { description ->
                item(key = "description") {
                    Text(description, style = MaterialTheme.typography.bodyLarge)
                }
            }

            if (state.tags.isNotEmpty()) {
                item(key = "tags") {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        state.tags.forEach { tag -> TagChip(tag) }
                    }
                }
            }

            item(key = "chapters-header") {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        strings.bookChapters,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    if (state.chapters.isNotEmpty()) {
                        Text(
                            strings.unitCountText(state.unitCount),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            if (state.chapters.isEmpty()) {
                item(key = "chapters-empty") {
                    Text(
                        if (state.downloadable && !state.available) strings.bookChaptersAfterDownload
                        else strings.bookNoChapters,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                items(state.chapters, key = { it.id }) { chapter ->
                    ChapterRow(
                        chapter = chapter,
                        unitLabel = strings.chapterUnitsText(chapter.unitCount),
                        enabled = state.available,
                        onClick = { onOpenChapter(chapter) },
                    )
                }
            }
        }
    }

    if (pickSource) {
        AlertDialog(
            onDismissRequest = { pickSource = false },
            title = { Text(strings.storeChooseSource) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.sourceNames.forEachIndexed { index, name ->
                        OutlinedCard(
                            onClick = {
                                pickSource = false
                                onDownload(state.sourceIds.getOrNull(index))
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
                TextButton(onClick = { pickSource = false }) { Text(strings.close) }
            },
        )
    }
}

private fun sourceLabel(strings: Strings, state: BookDetailUi): String = when {
    state.builtIn -> strings.bookSourceBuiltIn
    state.sourceNames.isNotEmpty() -> state.sourceNames.first()
    else -> strings.bookInfoEmpty
}

@Composable
private fun StatValueCell(value: String, label: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            value,
            style = MaterialTheme.typography.titleMedium.copy(fontSize = 20.sp),
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
    }
}

@Composable
private fun StatIconCell(
    icon: @Composable () -> Unit,
    label: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(modifier = Modifier.size(24.dp), contentAlignment = Alignment.Center) { icon() }
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun StatDivider() {
    Surface(
        color = MaterialTheme.colorScheme.outlineVariant,
        modifier = Modifier.width(1.dp).height(34.dp),
    ) {}
}

@Composable
private fun TagChip(text: String) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
        )
    }
}

@Composable
private fun ChapterRow(
    chapter: BookChapterUi,
    unitLabel: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.width(36.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            Text(
                chapter.number.toString(),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Column(Modifier.weight(1f)) {
            Text(
                chapter.title,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                unitLabel,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
