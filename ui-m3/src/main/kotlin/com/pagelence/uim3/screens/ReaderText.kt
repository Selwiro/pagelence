package com.pagelence.uim3.screens

// ReaderText：从 ReaderScreen.kt 拆出；只做展示，不含业务规则。

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import com.pagelence.core.prefs.FontSize
import com.pagelence.core.prefs.LineSpacing
import com.pagelence.core.prefs.ReadingFont
import com.pagelence.uim3.Strings
import com.pagelence.uim3.components.AnalysisIcon
import com.pagelence.uim3.components.TranslateIcon
import com.pagelence.uim3.model.ParagraphUi
import com.pagelence.uim3.model.SelectedWordUi
import com.pagelence.uim3.model.SentenceUi
import com.pagelence.uim3.theme.readingTextStyle

@Composable
internal fun ParagraphBlock(
    strings: Strings,
    paragraph: ParagraphUi,
    fontSize: FontSize,
    readingFont: ReadingFont,
    lineSpacing: LineSpacing,
    showAllTranslations: Boolean,
    annotateHints: Boolean,
    selectedWord: SelectedWordUi?,
    expandedTranslation: ExpandedTranslation?,
    focus: SentenceFocus?,
    onWordClick: (String, Int, String, Int, Int) -> Unit,
    onAnalysis: (SentenceUi) -> Unit,
    onTranslate: (String, SentenceUi) -> Unit,
    onSentenceLongPress: (String, SentenceUi) -> Unit,
) {
    Column {
        val expandedIndex = expandedTranslation
            ?.takeIf { !showAllTranslations && it.paragraphId == paragraph.id }
            ?.let { expanded -> paragraph.sentences.indexOfFirst { it.number == expanded.sentenceNumber } }
            ?: -1
        if (expandedIndex < 0) {
            ParagraphText(
                strings = strings,
                paragraphId = paragraph.id,
                sentences = paragraph.sentences,
                fontSize = fontSize,
                readingFont = readingFont,
                lineSpacing = lineSpacing,
                showAllTranslations = showAllTranslations,
                annotateHints = annotateHints,
                selectedWord = selectedWord,
                focus = focus,
                onWordClick = onWordClick,
                onAnalysis = onAnalysis,
                onTranslate = onTranslate,
                onSentenceLongPress = onSentenceLongPress,
            )
        } else {
            ParagraphText(
                strings = strings,
                paragraphId = paragraph.id,
                sentences = paragraph.sentences.subList(0, expandedIndex + 1),
                fontSize = fontSize,
                readingFont = readingFont,
                lineSpacing = lineSpacing,
                showAllTranslations = showAllTranslations,
                annotateHints = annotateHints,
                selectedWord = selectedWord,
                focus = focus,
                onWordClick = onWordClick,
                onAnalysis = onAnalysis,
                onTranslate = onTranslate,
                onSentenceLongPress = onSentenceLongPress,
            )
            TranslationExpansion(expandedTranslation?.zh.orEmpty())
            if (expandedIndex + 1 < paragraph.sentences.size) {
                ParagraphText(
                    strings = strings,
                    paragraphId = paragraph.id,
                    sentences = paragraph.sentences.subList(expandedIndex + 1, paragraph.sentences.size),
                    fontSize = fontSize,
                    readingFont = readingFont,
                    lineSpacing = lineSpacing,
                    showAllTranslations = showAllTranslations,
                    annotateHints = annotateHints,
                    selectedWord = selectedWord,
                    focus = focus,
                    onWordClick = onWordClick,
                    onAnalysis = onAnalysis,
                    onTranslate = onTranslate,
                    onSentenceLongPress = onSentenceLongPress,
                )
            }
        }
    }
}

@Composable
internal fun TranslationExpansion(zh: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 4.dp, end = 4.dp, bottom = 8.dp)
            .height(IntrinsicSize.Min),
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(2.dp)),
        )
        Spacer(Modifier.width(10.dp))
        Text(
            zh,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

internal data class SentenceSpan(
    val sentence: SentenceUi,
    val start: Int,
    val end: Int,
)

internal data class SentenceMarkSpan(
    val range: IntRange,
    val bookmark: Boolean,
    val note: Boolean,
)

internal fun translateId(sentenceNumber: Int): String = "translate-$sentenceNumber"

internal fun analysisId(sentenceNumber: Int): String = "analysis-$sentenceNumber"

/**
 * One paragraph (or a page chunk of it) as a single text flow. Sentences are
 * joined with spaces, translation icons are inline footnote markers, and long
 * sentences are never split into separate Text blocks.
 */
@Composable
internal fun ParagraphText(
    strings: Strings,
    paragraphId: String,
    sentences: List<SentenceUi>,
    fontSize: FontSize,
    readingFont: ReadingFont,
    lineSpacing: LineSpacing,
    showAllTranslations: Boolean,
    annotateHints: Boolean,
    selectedWord: SelectedWordUi?,
    focus: SentenceFocus?,
    onWordClick: (String, Int, String, Int, Int) -> Unit,
    onAnalysis: (SentenceUi) -> Unit,
    onTranslate: (String, SentenceUi) -> Unit,
    onSentenceLongPress: (String, SentenceUi) -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = MaterialTheme.colorScheme.primary
    val selectionColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.66f)
    val selectionText = MaterialTheme.colorScheme.onPrimaryContainer
    val hintColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
    val highlightColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
    val focusColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
    val markColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.78f)
    val translationColor = MaterialTheme.colorScheme.onSurfaceVariant
    val data = remember(
        sentences,
        showAllTranslations,
        annotateHints,
        strings.translation,
        translationColor,
        selectedWord?.paragraphId,
        selectedWord?.sentenceNumber,
        selectedWord?.spelling,
        selectedWord?.start,
        selectedWord?.end,
        focus,
    ) {
        buildParagraphTextData(
            strings = strings,
            paragraphId = paragraphId,
            sentences = sentences,
            showAllTranslations = showAllTranslations,
            annotateHints = annotateHints,
            selectedWord = selectedWord,
            focus = focus,
            selectionText = selectionText,
            translationText = translationColor,
        )
    }
    val density = LocalDensity.current
    val iconSizePx = with(density) { SentenceTranslateIconSize.toPx() }
    val iconSize = with(density) { iconSizePx.toSp() }
    val analysisSize = with(density) { SentenceAnalysisIconSize.toPx().toSp() }
    val inlineContent = buildMap<String, InlineTextContent> {
        data.iconStarts.values.distinct().forEach { sentence ->
            put(
                translateId(sentence.number),
                InlineTextContent(
                    placeholder = Placeholder(
                        width = iconSize,
                        height = iconSize,
                        placeholderVerticalAlign = PlaceholderVerticalAlign.Center,
                    ),
                ) {
                    Icon(
                        imageVector = TranslateIcon,
                        contentDescription = null,
                        tint = accent.copy(alpha = 0.55f),
                        modifier = Modifier
                            .size(SentenceTranslateIconSize)
                            .semantics {
                                contentDescription = strings.translation
                                onClick(label = strings.translation) {
                                    onTranslate(paragraphId, sentence)
                                    true
                                }
                            },
                    )
                },
            )
        }
        data.analysisStarts.values.distinct().forEach { sentence ->
            put(
                analysisId(sentence.number),
                InlineTextContent(
                    placeholder = Placeholder(
                        width = analysisSize,
                        height = analysisSize,
                        placeholderVerticalAlign = PlaceholderVerticalAlign.Center,
                    ),
                ) {
                    Icon(
                        imageVector = AnalysisIcon,
                        contentDescription = null,
                        tint = accent.copy(alpha = 0.7f),
                        modifier = Modifier
                            .size(SentenceAnalysisIconSize)
                            .semantics {
                                contentDescription = strings.analysisParse
                                onClick(label = strings.analysisParse) {
                                    onAnalysis(sentence)
                                    true
                                }
                            },
                    )
                },
            )
        }
    }

    var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
    Text(
        text = data.annotated,
        inlineContent = inlineContent,
        style = readingTextStyle(fontSize, readingFont, lineSpacing),
        onTextLayout = { layout = it },
        modifier = modifier
            .drawBehind {
                val result = layout ?: return@drawBehind
                data.focusRanges.forEach { range ->
                    drawSelectionBackground(
                        result,
                        range,
                        focusColor,
                        cornerPx = 3.dp.toPx(),
                        horizontalPadPx = 2.dp.toPx(),
                    )
                }
                data.highlightRanges.forEach { range ->
                    drawSelectionBackground(
                        result,
                        range,
                        highlightColor,
                        cornerPx = 3.dp.toPx(),
                        horizontalPadPx = 2.dp.toPx(),
                    )
                }
                data.selectionRanges.forEach { range ->
                    drawSelectionBackground(
                        result,
                        range,
                        selectionColor,
                        cornerPx = 4.dp.toPx(),
                        horizontalPadPx = 2.5.dp.toPx(),
                    )
                }
                data.hintRanges.forEach { range ->
                    drawHintUnderline(
                        result,
                        range,
                        hintColor,
                        strokeWidth = 1.dp.toPx(),
                        dash = 3.dp.toPx(),
                        gap = 3.dp.toPx(),
                    )
                }
                data.marks.forEach { mark ->
                    drawSentenceMarks(
                        layout = result,
                        range = mark.range,
                        bookmark = mark.bookmark,
                        note = mark.note,
                        color = markColor,
                    )
                }
            }
            .pointerInput(data.annotated, data.spans, data.iconStarts, data.analysisStarts) {
                detectTapGestures(
                    onLongPress = { offset ->
                        val result = layout ?: return@detectTapGestures
                        val index = result.getOffsetForPosition(offset)
                        if (data.translationRanges.any { index in it }) return@detectTapGestures
                        val span = data.spans.firstOrNull { index >= it.start && index < it.end }
                            ?: return@detectTapGestures
                        onSentenceLongPress(paragraphId, span.sentence)
                    },
                    onTap = { offset ->
                    val result = layout ?: return@detectTapGestures
                    val index = result.getOffsetForPosition(offset)
                    // Chinese translations are display-only; a tap on them must
                    // not be mistaken for a word lookup (T159).
                    if (data.translationRanges.any { index in it }) return@detectTapGestures
                    // 14dp glyphs are hard to hit with a thumb; accept a tap
                    // within a fingertip of the icon without changing layout.
                    val slop = SentenceIconSlop.toPx()
                    data.analysisStarts.entries.firstOrNull { (at, _) ->
                        val box = result.getBoundingBox(at)
                        offset.x in (box.left - slop)..(box.right + slop) &&
                            offset.y in (box.top - slop)..(box.bottom + slop)
                    }?.value?.let { sentence ->
                        onAnalysis(sentence)
                        return@detectTapGestures
                    }
                    data.iconStarts.entries.firstOrNull { (at, _) ->
                        val box = result.getBoundingBox(at)
                        offset.x in (box.left - slop)..(box.right + slop) &&
                            offset.y in (box.top - slop)..(box.bottom + slop)
                    }?.value?.let { sentence ->
                        onTranslate(paragraphId, sentence)
                        return@detectTapGestures
                    }
                    data.analysisStarts.entries.firstOrNull {
                        index == it.key || index == it.key + 1
                    }?.value?.let { sentence ->
                        onAnalysis(sentence)
                        return@detectTapGestures
                    }
                    data.iconStarts.entries.firstOrNull {
                        index == it.key || index == it.key + 1
                    }?.value?.let { sentence ->
                        onTranslate(paragraphId, sentence)
                        return@detectTapGestures
                    }
                    val span = data.spans.firstOrNull { index >= it.start && index < it.end }
                        ?: data.spans.lastOrNull { index >= it.start }
                        ?: return@detectTapGestures
                    if (span.sentence.text.isEmpty()) return@detectTapGestures
                    val local = (index - span.start).coerceIn(0, span.sentence.text.lastIndex)
                    wordAt(span.sentence.text, local)?.let { hit ->
                        onWordClick(
                            paragraphId,
                            span.sentence.number,
                            hit.text,
                            hit.start,
                            hit.end,
                        )
                    }
                    },
                )
            },
    )
}

internal data class ParagraphTextData(
    val annotated: AnnotatedString,
    val spans: List<SentenceSpan>,
    val iconStarts: Map<Int, SentenceUi>,
    val analysisStarts: Map<Int, SentenceUi>,
    val selectionRanges: List<IntRange>,
    val hintRanges: List<IntRange>,
    val highlightRanges: List<IntRange>,
    val translationRanges: List<IntRange>,
    val focusRanges: List<IntRange>,
    val marks: List<SentenceMarkSpan>,
)

/**
 * Sentence flow shared by rendering and paged measurement so both always agree
 * on what is on the page.
 *
 * - The per-sentence translate icon only appears when translations are hidden;
 *   with 全部句译 on, the Chinese follows on its own line instead (T159).
 * - [translationStyle] is a smaller, muted span; measurement only needs its
 *   size, rendering also uses its colour.
 */
internal data class SentenceFlowData(
    val annotated: AnnotatedString,
    val spans: List<SentenceSpan>,
    val iconStarts: Map<Int, SentenceUi>,
    val analysisStarts: Map<Int, SentenceUi>,
    val translationRanges: List<IntRange>,
)

internal fun buildSentenceFlow(
    strings: Strings,
    sentences: List<SentenceUi>,
    showAllTranslations: Boolean,
    translationStyle: SpanStyle,
): SentenceFlowData {
    val spans = mutableListOf<SentenceSpan>()
    val iconStarts = mutableMapOf<Int, SentenceUi>()
    val analysisStarts = mutableMapOf<Int, SentenceUi>()
    val translationRanges = mutableListOf<IntRange>()
    val builder = AnnotatedString.Builder()
    var previousTranslated = false
    sentences.forEachIndexed { index, sentence ->
        // A translation already ends on its own line; start the next English
        // sentence on a fresh line so the two languages never share one line
        // and look like a repeated fragment (T159).
        if (index > 0) builder.append(if (previousTranslated) "\n" else " ")
        val start = builder.length
        builder.append(sentence.text)
        val end = builder.length
        spans += SentenceSpan(sentence, start, end)
        if (!sentence.zh.isNullOrBlank()) {
            if (showAllTranslations) {
                builder.append("\n")
                val zhStart = builder.length
                builder.pushStyle(translationStyle)
                builder.append(sentence.zh)
                builder.pop()
                if (builder.length > zhStart) translationRanges += zhStart until builder.length
            } else {
                builder.append(" ")
                val iconAt = builder.length
                builder.appendInlineContent(translateId(sentence.number), strings.translation)
                iconStarts[iconAt] = sentence
            }
        }
        if (sentence.analysis != null) {
            builder.append(" ")
            val analysisAt = builder.length
            builder.appendInlineContent(analysisId(sentence.number), strings.analysisParse)
            analysisStarts[analysisAt] = sentence
        }
        previousTranslated = showAllTranslations && !sentence.zh.isNullOrBlank()
    }
    return SentenceFlowData(
        annotated = builder.toAnnotatedString(),
        spans = spans,
        iconStarts = iconStarts,
        analysisStarts = analysisStarts,
        translationRanges = translationRanges,
    )
}

internal fun buildParagraphTextData(
    strings: Strings,
    paragraphId: String,
    sentences: List<SentenceUi>,
    showAllTranslations: Boolean,
    annotateHints: Boolean,
    selectedWord: SelectedWordUi?,
    focus: SentenceFocus?,
    selectionText: androidx.compose.ui.graphics.Color,
    translationText: androidx.compose.ui.graphics.Color,
): ParagraphTextData {
    val flow = buildSentenceFlow(
        strings = strings,
        sentences = sentences,
        showAllTranslations = showAllTranslations,
        translationStyle = SpanStyle(color = translationText, fontSize = 0.86.em),
    )
    val spans = flow.spans
    val iconStarts = flow.iconStarts
    val analysisStarts = flow.analysisStarts
    val builder = AnnotatedString.Builder(flow.annotated)
    val selectionRanges = selectedWord?.let { selected ->
        spans.firstOrNull { it.sentence.number == selected.sentenceNumber }?.let { span ->
            val ranges = if (selected.start in 0..selected.end && selected.end < span.sentence.text.length) {
                listOf(selected.start..selected.end)
            } else {
                rangesOf(span.sentence.text, selected.spelling)
            }
            ranges.map { (span.start + it.first)..(span.start + it.last) }
        }
    }.orEmpty()
    selectionRanges.forEach { range ->
        builder.addStyle(
            SpanStyle(color = selectionText, fontWeight = FontWeight.SemiBold),
            range.first,
            range.last + 1,
        )
    }
    val hintRanges = if (annotateHints) {
        sentences.flatMap { sentence ->
            val span = spans.first { it.sentence === sentence }
            sentence.annotated.flatMap { word ->
                rangesOf(sentence.text, word).map { (span.start + it.first)..(span.start + it.last) }
            }
        }
    } else {
        emptyList()
    }
    val focusRanges = focus?.let { target ->
        spans.firstOrNull {
            paragraphId == target.paragraphId && it.sentence.number == target.sentenceNumber
        }?.takeIf { it.end > it.start }?.let { listOf(it.start..(it.end - 1)) }
    }.orEmpty()
    val highlightRanges = spans
        .filter { it.sentence.mark?.highlighted == true && it.end > it.start }
        .map { it.start..(it.end - 1) }
    val marks = spans.mapNotNull { span ->
        val mark = span.sentence.mark ?: return@mapNotNull null
        if (mark.isEmpty || span.end <= span.start) return@mapNotNull null
        // Sentence-end inline icons (translation, analysis) sit right after the
        // text. Anchor the marker after the last of them, otherwise it would be
        // painted on top of the icon.
        val inlineEnd = (iconStarts.entries + analysisStarts.entries)
            .filter { it.value.number == span.sentence.number }
            .maxOfOrNull { it.key }
        SentenceMarkSpan(
            range = span.start..maxOf(span.end - 1, inlineEnd ?: (span.end - 1)),
            bookmark = mark.bookmarked,
            note = mark.hasNote,
        )
    }
    return ParagraphTextData(
        annotated = builder.toAnnotatedString(),
        spans = spans,
        iconStarts = iconStarts,
        analysisStarts = analysisStarts,
        selectionRanges = selectionRanges,
        hintRanges = hintRanges,
        highlightRanges = highlightRanges,
        translationRanges = flow.translationRanges,
        focusRanges = focusRanges,
        marks = marks,
    )
}

internal fun toggleTranslation(
    current: ExpandedTranslation?,
    paragraphId: String,
    sentence: SentenceUi,
): ExpandedTranslation? {
    if (sentence.zh.isNullOrBlank()) return current
    if (current?.paragraphId == paragraphId && current.sentenceNumber == sentence.number) {
        return null
    }
    return ExpandedTranslation(
        paragraphId = paragraphId,
        sentenceNumber = sentence.number,
        sentence = sentence.text,
        zh = sentence.zh.orEmpty(),
    )
}

internal fun DrawScope.drawSelectionBackground(
    layout: TextLayoutResult,
    range: IntRange,
    color: androidx.compose.ui.graphics.Color,
    cornerPx: Float,
    horizontalPadPx: Float,
) {
    forEachRangeLine(layout, range) { line, from, to, left, right ->
        val top = layout.getBoundingBox(from).top
        val bottom = layout.getBoundingBox((to - 1).coerceAtLeast(from)).bottom
        val paddedLeft = left - horizontalPadPx
        val paddedRight = right + horizontalPadPx
        drawRoundRect(
            color = color,
            topLeft = Offset(paddedLeft, top),
            size = Size(
                (paddedRight - paddedLeft).coerceAtLeast(1f),
                (bottom - top).coerceAtLeast(1f),
            ),
            cornerRadius = CornerRadius(cornerPx),
        )
    }
}

internal fun DrawScope.drawHintUnderline(
    layout: TextLayoutResult,
    range: IntRange,
    color: androidx.compose.ui.graphics.Color,
    strokeWidth: Float,
    dash: Float,
    gap: Float,
) {
    forEachRangeLine(layout, range) { line, from, to, left, right ->
        val y = layout.getLineBaseline(line) + strokeWidth * 2.2f
        val path = Path().apply {
            moveTo(left, y)
            lineTo(right, y)
        }
        drawPath(
            path = path,
            color = color,
            style = Stroke(
                width = strokeWidth,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(dash, gap)),
            ),
        )
    }
}

/**
 * Sentence-end markers drawn after the text, not as inline content: adding or
 * removing a bookmark must never re-flow the page the reader is on.
 */
internal fun DrawScope.drawSentenceMarks(
    layout: TextLayoutResult,
    range: IntRange,
    bookmark: Boolean,
    note: Boolean,
    color: androidx.compose.ui.graphics.Color,
) {
    val textLength = layout.layoutInput.text.length
    if (textLength == 0 || range.isEmpty()) return
    val last = range.last.coerceIn(0, textLength - 1)
    val line = layout.getLineForOffset(last)
    val box = layout.getBoundingBox(last)
    val bookmarkWidth = 4.5.dp.toPx()
    val noteWidth = 6.dp.toPx()
    val markerHeight = 9.dp.toPx()
    val gap = 2.5.dp.toPx()
    val totalWidth = (if (bookmark) bookmarkWidth + gap else 0f) +
        (if (note) noteWidth else 0f)
    val top = layout.getLineTop(line) + 1.dp.toPx()
    val right = size.width - 1.dp.toPx()
    var x = box.right + gap
    // Near the right edge, draw the marker just before the last glyph instead
    // of clipping it out of the column.
    if (x + totalWidth > right) {
        x = (box.left - totalWidth - gap).coerceAtLeast(0f)
    }
    if (bookmark) {
        val path = Path().apply {
            moveTo(x, top)
            lineTo(x + bookmarkWidth, top)
            lineTo(x + bookmarkWidth, top + markerHeight)
            lineTo(x + bookmarkWidth / 2f, top + markerHeight - bookmarkWidth * 0.6f)
            lineTo(x, top + markerHeight)
            close()
        }
        drawPath(path = path, color = color)
        x += bookmarkWidth + gap
    }
    if (note) {
        // A tiny outlined page with two ruled lines, so a note is recognisable
        // at 6dp without moving the text or adding an inline placeholder.
        val noteTop = top + 1.dp.toPx()
        val noteHeight = markerHeight - 2.dp.toPx()
        drawRoundRect(
            color = color,
            topLeft = Offset(x, noteTop),
            size = Size(noteWidth, noteHeight),
            cornerRadius = CornerRadius(1.dp.toPx()),
            style = Stroke(width = 0.9.dp.toPx()),
        )
        val lineX = x + 1.4.dp.toPx()
        val lineRight = x + noteWidth - 1.4.dp.toPx()
        listOf(0.32f, 0.64f).forEach { fraction ->
            val y = noteTop + noteHeight * fraction
            drawLine(
                color = color,
                start = Offset(lineX, y),
                end = Offset(lineRight, y),
                strokeWidth = 0.8.dp.toPx(),
            )
        }
    }
}

internal inline fun forEachRangeLine(    layout: TextLayoutResult,
    range: IntRange,
    block: (line: Int, from: Int, to: Int, left: Float, right: Float) -> Unit,
) {
    val textLength = layout.layoutInput.text.length
    if (range.isEmpty() || textLength == 0) return
    val startLine = layout.getLineForOffset(range.first.coerceIn(0, textLength - 1))
    val endLine = layout.getLineForOffset(range.last.coerceIn(0, textLength - 1))
    for (line in startLine..endLine) {
        val lineStart = layout.getLineStart(line)
        val lineEnd = layout.getLineEnd(line)
        val from = maxOf(range.first, lineStart)
        val to = minOf(range.last + 1, lineEnd)
        if (to <= from) continue
        val left = layout.getHorizontalPosition(from, true)
        val right = if (to >= textLength) {
            layout.getBoundingBox(to - 1).right
        } else {
            layout.getHorizontalPosition(to, false)
        }
        block(line, from, to, left, right)
    }
}

internal fun rangesOf(text: String, word: String): List<IntRange> {
    if (word.isBlank()) return emptyList()
    val pattern = Regex(
        "(?<![\\p{L}])" + Regex.escape(word) + "(?![\\p{L}])",
        RegexOption.IGNORE_CASE,
    )
    return pattern.findAll(text).map { it.range }.toList()
}

internal data class WordHit(val text: String, val start: Int, val end: Int)

internal fun wordAt(text: String, index: Int): WordHit? {
    if (text.isEmpty()) return null
    fun isWordChar(c: Char) = c.isLetter() || c == '\'' || c == '’' || c == '-'
    var start = index.coerceIn(0, text.lastIndex)
    var end = start
    if (!isWordChar(text[start])) {
        if (start > 0 && isWordChar(text[start - 1])) {
            start -= 1
            end = start
        } else {
            return null
        }
    }
    while (start > 0 && isWordChar(text[start - 1])) start -= 1
    while (end < text.lastIndex && isWordChar(text[end + 1])) end += 1
    val word = text.substring(start, end + 1)
    return if (word.any { it.isLetter() }) WordHit(word, start, end) else null
}
