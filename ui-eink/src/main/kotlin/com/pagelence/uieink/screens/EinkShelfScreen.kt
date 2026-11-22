package com.pagelence.uieink.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pagelence.uim3.Strings
import com.pagelence.uim3.model.BookUi
import com.pagelence.uim3.model.BuiltInBookUi
import com.pagelence.uim3.model.LibraryUiState
import com.pagelence.uieink.components.InkButton
import com.pagelence.uieink.components.InkDivider
import com.pagelence.uieink.components.InkNotice
import com.pagelence.uieink.components.InkSectionLabel
import com.pagelence.uieink.components.InkText
import com.pagelence.uieink.components.InkTopBar
import com.pagelence.uieink.theme.LocalInkColors
import com.pagelence.uieink.theme.LocalInkTypography

/**
 * 墨水屏书架：只有「在读 / 已读完」两段列表，不做封面墙——墨水屏上封面缩略图
 * 既费刷新又看不清，用书名 + 进度 + 继续阅读足够。
 */
@Composable
fun EinkShelfScreen(
    strings: Strings,
    library: LibraryUiState,
    onOpenBook: (String) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    notice: String? = null,
    builtIn: List<BuiltInBookUi> = emptyList(),
) {
    val knownIds = (library.books + library.finished).map { it.id }.toSet()
    val unreadBuiltIn = builtIn.filter { it.available && it.id !in knownIds }
    val colors = LocalInkColors.current
    val typography = LocalInkTypography.current
    Column(modifier.fillMaxSize()) {
        InkTopBar(title = strings.appName, subtitle = strings.library)
        if (notice != null) {
            InkNotice(notice)
        }
        when {
            library.loading && library.books.isEmpty() -> {
                InkText(
                    strings.loading,
                    modifier = Modifier.padding(16.dp),
                    style = typography.ui,
                    color = colors.inkMuted,
                )
            }
            library.error != null && library.books.isEmpty() -> {
                InkText(library.error ?: "", modifier = Modifier.padding(16.dp), style = typography.ui)
                InkButton(
                    label = strings.retry,
                    onClick = onRetry,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
            library.books.isEmpty() && library.finished.isEmpty() && unreadBuiltIn.isEmpty() -> {
                Column(Modifier.padding(16.dp)) {
                    InkText(strings.emptyShelf, style = typography.title)
                    Spacer(Modifier.height(8.dp))
                    InkText(strings.emptyShelfHint, style = typography.ui, color = colors.inkMuted)
                }
            }
            else -> {
                LazyColumn(Modifier.fillMaxSize()) {
                    if (library.books.isNotEmpty()) {
                        item { InkSectionLabel(strings.shelfReading) }
                        items(library.books, key = { it.id }) { book ->
                            ShelfRow(
                                strings = strings,
                                book = book,
                                actionLabel = if (book.readUnits > 0) strings.continueReading else strings.startReading,
                                onOpen = { onOpenBook(book.id) },
                            )
                            InkDivider()
                        }
                    }
                    if (unreadBuiltIn.isNotEmpty()) {
                        item { InkSectionLabel(strings.shelfAvailable) }
                        items(unreadBuiltIn, key = { it.id }) { book ->
                            BuiltInRow(
                                strings = strings,
                                book = book,
                                onOpen = { onOpenBook(book.id) },
                            )
                            InkDivider()
                        }
                    }
                    if (library.finished.isNotEmpty()) {
                        item { InkSectionLabel(strings.shelfFinished) }
                        items(library.finished, key = { it.id }) { book ->
                            ShelfRow(
                                strings = strings,
                                book = book,
                                actionLabel = strings.readAgain,
                                onOpen = { onOpenBook(book.id) },
                            )
                            InkDivider()
                        }
                    }
                    item { Spacer(Modifier.height(24.dp)) }
                }
            }
        }
    }
}

@Composable
private fun ShelfRow(
    strings: Strings,
    book: BookUi,
    actionLabel: String,
    onOpen: () -> Unit,
) {
    val colors = LocalInkColors.current
    val typography = LocalInkTypography.current
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp)) {
        InkText(book.title, style = typography.title, maxLines = 2)
        if (book.subtitle.isNotBlank()) {
            Spacer(Modifier.height(4.dp))
            InkText(book.subtitle, style = typography.caption, color = colors.inkMuted, maxLines = 2)
        }
        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            InkText(
                strings.readUnits.format(book.readUnits, book.unitCount),
                style = typography.caption,
                color = colors.inkSecondary,
            )
            book.finishedOn?.let {
                InkText(it, style = typography.caption, color = colors.inkMuted)
            }
        }
        if (book.readingUnitTitle != null && !book.finished) {
            Spacer(Modifier.height(4.dp))
            InkText(
                "${strings.lastRead} · ${book.readingUnitTitle}",
                style = typography.caption,
                color = colors.inkMuted,
                maxLines = 1,
            )
        }
        Spacer(Modifier.height(10.dp))
        InkButton(label = actionLabel, onClick = onOpen, primary = !book.finished)
    }
}

@Composable
private fun BuiltInRow(
    strings: Strings,
    book: BuiltInBookUi,
    onOpen: () -> Unit,
) {
    val colors = LocalInkColors.current
    val typography = LocalInkTypography.current
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp)) {
        InkText(book.title, style = typography.title, maxLines = 2)
        book.description?.takeIf { it.isNotBlank() }?.let {
            Spacer(Modifier.height(4.dp))
            InkText(it, style = typography.caption, color = colors.inkMuted, maxLines = 3)
        }
        Spacer(Modifier.height(6.dp))
        val level = book.level?.takeIf { it.isNotBlank() }?.let { "$it · " }.orEmpty()
        InkText(
            level + strings.unitCount.format(book.unitCount),
            style = typography.caption,
            color = colors.inkSecondary,
        )
        Spacer(Modifier.height(10.dp))
        InkButton(label = strings.startReading, onClick = onOpen, primary = true)
    }
}
