package com.pagelence.uieink.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pagelence.uim3.Strings
import com.pagelence.uim3.model.BookChapterUi
import com.pagelence.uim3.model.BookDetailUi
import com.pagelence.uieink.components.InkButton
import com.pagelence.uieink.components.InkDivider
import com.pagelence.uieink.components.InkKeyValueRow
import com.pagelence.uieink.components.InkListRow
import com.pagelence.uieink.components.InkSectionLabel
import com.pagelence.uieink.components.InkText
import com.pagelence.uieink.components.InkTopBar
import com.pagelence.uieink.theme.LocalInkColors
import com.pagelence.uieink.theme.LocalInkTypography

/**
 * 书籍详情：排版封面 + 基本信息 + 简介 + 章节目录 + 加入书架 / 立即阅读。
 *
 * 墨屏不画封面图：深色封面块在墨水屏上会整块刷新且发虚，这里用描边框 + 书名
 * 与作者替代；其它信息与普通版同源。
 */
@Composable
fun EinkBookDetailScreen(
    strings: Strings,
    detail: BookDetailUi,
    onBack: () -> Unit,
    onToggleShelf: () -> Unit,
    onStartReading: () -> Unit,
    onOpenChapter: (BookChapterUi) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalInkColors.current
    val typography = LocalInkTypography.current
    Column(modifier.fillMaxSize()) {
        InkTopBar(title = strings.details, subtitle = detail.title, backLabel = strings.back, onBack = onBack)
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
                Box(
                    Modifier
                        .width(96.dp)
                        .height(132.dp)
                        .border(1.5.dp, colors.ink),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(Modifier.padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        InkText(detail.title, style = typography.caption, maxLines = 4)
                    }
                }
                Column(Modifier.padding(start = 14.dp)) {
                    InkText(detail.title, style = typography.title, maxLines = 3)
                    detail.author?.takeIf { it.isNotBlank() }?.let {
                        Spacer(Modifier.height(4.dp))
                        InkText(it, style = typography.caption, color = colors.inkMuted)
                    }
                    Spacer(Modifier.height(6.dp))
                    InkText(
                        listOfNotNull(
                            detail.level,
                            detail.category,
                            strings.unitCount.format(detail.unitCount),
                        ).filter { it.isNotBlank() }.joinToString(" · "),
                        style = typography.caption,
                        color = colors.inkSecondary,
                    )
                    if (detail.wordCount > 0) {
                        Spacer(Modifier.height(2.dp))
                        InkText(
                            "${strings.finishedWordsLabel} · ${detail.wordCount}",
                            style = typography.caption,
                            color = colors.inkMuted,
                        )
                    }
                }
            }
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                InkButton(
                    label = if (detail.onShelf) strings.viewInShelf else strings.addToShelf,
                    onClick = if (detail.onShelf) onBack else onToggleShelf,
                )
                Spacer(Modifier.width(10.dp))
                InkButton(
                    label = if (detail.readUnits > 0) strings.continueReading else strings.startReading,
                    onClick = onStartReading,
                    primary = true,
                )
            }
            Spacer(Modifier.height(6.dp))
            val description = detail.description
            if (description != null && description.isNotBlank()) {
                InkSectionLabel(strings.bookIntro)
                InkText(
                    description,
                    style = typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }
            InkSectionLabel(strings.bookInfo)
            InkKeyValueRow(strings.bookLevelLabel, detail.level ?: strings.bookInfoEmpty)
            InkKeyValueRow(strings.bookWordCount, detail.wordCount.toString())
            InkKeyValueRow(strings.bookChapters, detail.chapters.size.toString())
            detail.creator?.takeIf { it.isNotBlank() }?.let { InkKeyValueRow(strings.bookCreator, it) }
            detail.sourceLicense?.takeIf { it.isNotBlank() }?.let { InkKeyValueRow(strings.bookLicense, it) }
            InkDivider()
            InkSectionLabel(strings.toc)
            detail.chapters.forEach { chapter ->
                InkListRow(
                    title = chapter.title,
                    subtitle = strings.unitCount.format(chapter.unitCount),
                    trailing = chapter.number.toString(),
                    onClick = { onOpenChapter(chapter) },
                )
                InkDivider()
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
