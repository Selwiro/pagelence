package com.pagelence.uim3.screens

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pagelence.uim3.Strings
import com.pagelence.uim3.components.EmptyState
import com.pagelence.uim3.components.BookCover
import com.pagelence.uim3.components.rememberCoverPainter
import com.pagelence.uim3.components.ReadingProgressBar
import com.pagelence.uim3.model.BookUi
import com.pagelence.uim3.model.LibraryUiState

/**
 * 书库：Material 3 大标题随滚动收起。书分两组——「在读」继续读，
 * 「已读完」保留位置、显示读完日期，并可直接再读一遍。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    strings: Strings,
    state: LibraryUiState,
    onOpenBook: (String) -> Unit,
    onOpenBookDetail: (String) -> Unit,
    onReread: (String) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenProfile: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
        topBar = {
            LargeTopAppBar(
                title = { Text(strings.appName) },
                actions = {
                    IconButton(onClick = onOpenProfile) {
                        Icon(Icons.Filled.Person, contentDescription = strings.profile)
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = strings.settings)
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
            )
        },
    ) { padding ->
        when {
            state.loading -> Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(16.dp))
                    Text(strings.loading, style = MaterialTheme.typography.bodyMedium)
                }
            }

            state.error != null -> EmptyState(
                icon = Icons.AutoMirrored.Filled.List,
                title = strings.library,
                body = state.error.orEmpty(),
                modifier = Modifier.fillMaxSize().padding(padding),
            )

            state.books.isEmpty() && state.finished.isEmpty() -> EmptyState(
                icon = Icons.AutoMirrored.Filled.List,
                title = strings.emptyShelf,
                body = strings.emptyShelfHint,
                modifier = Modifier.fillMaxSize().padding(padding),
            )

            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                item(key = "hint") {
                    Text(
                        strings.libraryHint,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 4.dp),
                    )
                }
                if (state.books.isNotEmpty()) {
                    if (state.finished.isNotEmpty()) {
                        item(key = "section-reading") { ShelfSectionLabel(strings.shelfReading) }
                    }
                    items(state.books, key = { "reading-${it.id}" }) { book ->
                        ReadingBookCard(strings, book, onOpenBook)
                    }
                }
                if (state.finished.isNotEmpty()) {
                    item(key = "section-finished") { ShelfSectionLabel(strings.shelfFinished) }
                    items(state.finished, key = { "finished-${it.id}" }) { book ->
                        FinishedBookCard(
                            strings = strings,
                            book = book,
                            onOpen = { onOpenBookDetail(book.id) },
                            onReread = { onReread(book.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ShelfSectionLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 6.dp),
    )
}

@Composable
private fun ReadingBookCard(strings: Strings, book: BookUi, onOpenBook: (String) -> Unit) {
    val progress = if (book.unitCount == 0) 0f else book.readUnits.toFloat() / book.unitCount
    ElevatedCard(
        onClick = { onOpenBook(book.id) },
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            BookHeader(strings, book, progress)
            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    book.readingUnitTitle?.let { title ->
                        Text(
                            "${strings.lastRead} $title",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                Button(onClick = { onOpenBook(book.id) }) {
                    Text(
                        if (book.readUnits > 0 || book.readingUnitTitle != null) {
                            strings.continueReading
                        } else {
                            strings.startReading
                        },
                    )
                    Spacer(Modifier.width(6.dp))
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun FinishedBookCard(
    strings: Strings,
    book: BookUi,
    onOpen: () -> Unit,
    onReread: () -> Unit,
) {
    ElevatedCard(
        onClick = onOpen,
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            BookHeader(strings, book, progress = 1f)
            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    book.finishedOn?.let { date ->
                        Text(
                            strings.finishedDate.format(date),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Button(onClick = onReread) {
                    Text(strings.readAgain)
                }
            }
        }
    }
}

@Composable
private fun BookHeader(strings: Strings, book: BookUi, progress: Float) {
    Row {
        BookCover(
            title = book.coverTitle,
            author = book.author,
            seed = book.id,
            monogram = book.monogram,
            cover = rememberCoverPainter(book.coverPath),
            modifier = Modifier.width(76.dp),
        )
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    book.title,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (book.finished) {
                    Spacer(Modifier.width(8.dp))
                    FinishedBadge(strings.shelfFinished)
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(
                book.subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(10.dp))
            ReadingProgressBar(progress)
            Spacer(Modifier.height(6.dp))
            Text(
                strings.readUnits.format(book.readUnits, book.unitCount),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun FinishedBadge(text: String) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.primaryContainer,
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
        )
    }
}
