package com.pagelence.uieink.screens

// 阅读页底部菜单、目录页与书签笔记页（T169，对应普通版 T167）。
//
// 普通版用覆盖层 + 动效 + 底部抽屉；墨屏版改成同一块白底面板与整页列表：
// 无遮罩、无转场、无圆角，分层只靠 1px 线与字重。点正文空白处呼出菜单，
// 再点空白处或返回收起；面板本身不拦截正文区域的点击。

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pagelence.core.prefs.FontSize
import com.pagelence.core.prefs.LineSpacing
import com.pagelence.core.prefs.PageMargin
import com.pagelence.core.prefs.Preferences
import com.pagelence.core.progress.AnnotationKind
import com.pagelence.uim3.Strings
import com.pagelence.uim3.model.AnnotationUi
import com.pagelence.uim3.model.TocChapterUi
import com.pagelence.uieink.components.InkButton
import com.pagelence.uieink.components.InkDivider
import com.pagelence.uieink.components.InkListRow
import com.pagelence.uieink.components.InkOptionRow
import com.pagelence.uieink.components.InkSectionLabel
import com.pagelence.uieink.components.InkText
import com.pagelence.uieink.components.InkTextButton
import com.pagelence.uieink.components.InkTopBar
import com.pagelence.uieink.theme.LocalInkColors
import com.pagelence.uieink.theme.LocalInkTypography

/**
 * 底部菜单：四个入口 + 可展开的内联阅读设置。
 *
 * [settingsOpen] 为真时，面板向上长出字号/行距/页边距与「更多设置」；面板最高
 * 占屏幕 80%，超出部分滚动，四个入口永远留在底部。
 */
@Composable
internal fun EinkReaderDock(
    strings: Strings,
    preference: Preferences,
    annotationCount: Int,
    cardsCount: Int,
    settingsOpen: Boolean,
    onOpenToc: () -> Unit,
    onOpenAnnotations: () -> Unit,
    onToggleSettings: () -> Unit,
    onOpenCards: () -> Unit,
    onOpenSettings: () -> Unit,
    onFontSize: (FontSize) -> Unit,
    onLineSpacing: (LineSpacing) -> Unit,
    onPageMargin: (PageMargin) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalInkColors.current
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val panelMaxHeight = this.maxHeight * 0.8f
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(colors.paper),
        ) {
            Box(Modifier.fillMaxWidth().height(1.dp).background(colors.ink))
            if (settingsOpen) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(max = panelMaxHeight)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        InkText(
                            strings.readerSettings,
                            style = LocalInkTypography.current.uiStrong,
                            modifier = Modifier.weight(1f),
                        )
                        InkTextButton(strings.readerSettingsMore, onClick = onOpenSettings)
                    }
                    Spacer(Modifier.height(4.dp))
                    InkOptionRow(
                        label = strings.fontSize,
                        options = fontSizeOptions(),
                        selected = preference.fontSize,
                        onSelect = onFontSize,
                    )
                    InkOptionRow(
                        label = strings.lineSpacing,
                        options = listOf(
                            LineSpacing.COMPACT to strings.lineSpacingCompact,
                            LineSpacing.NORMAL to strings.lineSpacingNormal,
                            LineSpacing.RELAXED to strings.lineSpacingRelaxed,
                        ),
                        selected = preference.lineSpacing,
                        onSelect = onLineSpacing,
                    )
                    InkOptionRow(
                        label = strings.pageMargin,
                        options = listOf(
                            PageMargin.NARROW to strings.pageMarginNarrow,
                            PageMargin.NORMAL to strings.pageMarginNormal,
                            PageMargin.WIDE to strings.pageMarginWide,
                        ),
                        selected = preference.pageMargin,
                        onSelect = onPageMargin,
                    )
                    InkText(
                        strings.einkTypographyHint,
                        style = LocalInkTypography.current.caption,
                        color = colors.inkMuted,
                        modifier = Modifier.padding(top = 6.dp, bottom = 4.dp),
                    )
                }
                InkDivider()
            }
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                DockAction(
                    label = strings.toc,
                    onClick = onOpenToc,
                )
                DockAction(
                    label = strings.annotations,
                    count = annotationCount,
                    onClick = onOpenAnnotations,
                )
                DockAction(
                    label = strings.settings,
                    active = settingsOpen,
                    onClick = onToggleSettings,
                )
                DockAction(
                    label = strings.wordCards,
                    count = cardsCount,
                    onClick = onOpenCards,
                )
            }
        }
    }
}

/** 六档字号；顺序与普通版一致（15/17/19/22/25/28）。 */
internal fun fontSizeOptions(): List<Pair<FontSize, String>> = listOf(
    FontSize.XSMALL to "15",
    FontSize.SMALL to "17",
    FontSize.MEDIUM to "19",
    FontSize.LARGE to "22",
    FontSize.XLARGE to "25",
    FontSize.XXLARGE to "28",
)

@Composable
private fun RowScope.DockAction(
    label: String,
    onClick: () -> Unit,
    count: Int = 0,
    active: Boolean = false,
) {
    val colors = LocalInkColors.current
    val typography = LocalInkTypography.current
    val interaction = remember { MutableInteractionSource() }
    Column(
        modifier = Modifier
            .weight(1f)
            .defaultMinSize(minHeight = 56.dp)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = 2.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        InkText(
            if (count > 0) "$label $count" else label,
            style = if (active) typography.uiStrong else typography.ui,
            color = if (active) colors.ink else colors.inkSecondary,
            maxLines = 2,
            textAlign = TextAlign.Center,
        )
        if (active) {
            Spacer(Modifier.height(3.dp))
            Box(Modifier.fillMaxWidth(0.5f).height(2.dp).background(colors.ink))
        }
    }
}

/** 全书目录：按章分组，标「在读 / 已读」，点任意单元直接切过去。 */
@Composable
internal fun EinkTocPage(
    strings: Strings,
    toc: List<TocChapterUi>,
    onOpen: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalInkColors.current
    val typography = LocalInkTypography.current
    Column(modifier.fillMaxSize().background(colors.paper)) {
        InkTopBar(title = strings.toc, backLabel = strings.back, onBack = onBack)
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            InkText(
                strings.tocHint,
                style = typography.caption,
                color = colors.inkMuted,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
            toc.forEach { chapter ->
                InkSectionLabel(chapter.title)
                chapter.units.forEach { unit ->
                    val interaction = remember { MutableInteractionSource() }
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 48.dp)
                            .clickable(
                                interactionSource = interaction,
                                indication = null,
                                onClick = { onOpen(unit.id) },
                            )
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        InkText(
                            unit.number.toString().padStart(2, '0'),
                            style = typography.caption,
                            color = colors.inkMuted,
                            modifier = Modifier.width(32.dp),
                        )
                        InkText(
                            unit.title,
                            style = if (unit.current) typography.uiStrong else typography.ui,
                            maxLines = 2,
                            modifier = Modifier.weight(1f),
                        )
                        if (unit.current) {
                            InkText(strings.tocCurrent, style = typography.caption, color = colors.inkSecondary)
                        } else if (unit.read) {
                            InkText(strings.tocRead, style = typography.caption, color = colors.inkMuted)
                        }
                    }
                    InkDivider()
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

/** 本书的书签、划线与笔记：按单元分组，点一行跳回原句。 */
@Composable
internal fun EinkAnnotationListPage(
    strings: Strings,
    bookTitle: String,
    annotations: List<AnnotationUi>,
    onOpen: (AnnotationUi) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalInkColors.current
    val typography = LocalInkTypography.current
    Column(modifier.fillMaxSize().background(colors.paper)) {
        InkTopBar(
            title = strings.annotations,
            subtitle = if (annotations.isEmpty()) {
                bookTitle
            } else {
                "$bookTitle · ${strings.marksText(annotations.size)}"
            },
            backLabel = strings.back,
            onBack = onBack,
        )
        if (annotations.isEmpty()) {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
                InkText(strings.annotationEmptyTitle, style = typography.title)
                Spacer(Modifier.height(8.dp))
                InkText(strings.annotationEmptyBody, style = typography.body, color = colors.inkSecondary)
                Spacer(Modifier.height(8.dp))
                InkText(strings.annotationEmptyHint, style = typography.caption, color = colors.inkMuted)
            }
        } else {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                annotations.groupBy { it.unitId }.forEach { (_, rows) ->
                    val first = rows.first()
                    InkSectionLabel(
                        listOfNotNull(
                            first.chapterTitle.takeIf { it.isNotBlank() },
                            first.unitTitle.takeIf { it.isNotBlank() },
                        ).joinToString(" · "),
                    )
                    rows.forEach { annotation ->
                        InkListRow(
                            title = annotation.quote.ifBlank { strings.annotationAnchorMoved },
                            subtitle = buildString {
                                append(annotationKindLabel(strings, annotation.kind))
                                annotation.dateLabel?.let { append(" · $it") }
                                if (!annotation.anchorValid) append(" · ${strings.annotationAnchorMoved}")
                                annotation.note?.takeIf { it.isNotBlank() }?.let { append("\n$it") }
                            },
                            trailing = if (annotation.note.isNullOrBlank()) null else "✎",
                            onClick = { onOpen(annotation) },
                        )
                        InkDivider()
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

/** 标注类型标签；列表与复习栏共用同一套叫法。 */
internal fun annotationKindLabel(strings: Strings, kind: AnnotationKind): String = when (kind) {
    AnnotationKind.BOOKMARK -> strings.annotationBookmark
    AnnotationKind.HIGHLIGHT -> strings.annotationHighlight
    AnnotationKind.NOTE -> strings.annotationNote
}
