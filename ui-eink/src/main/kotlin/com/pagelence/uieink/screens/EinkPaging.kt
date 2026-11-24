package com.pagelence.uieink.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.pagelence.core.prefs.FontSize
import com.pagelence.core.prefs.LineSpacing
import com.pagelence.core.prefs.PageMargin
import com.pagelence.core.prefs.ReadingFont
import com.pagelence.core.reading.MeasuredSentence
import com.pagelence.core.reading.SentencePage
import com.pagelence.core.reading.SentencePaginator
import com.pagelence.presentation.pagination.PaginationCache
import com.pagelence.presentation.pagination.PaginationFingerprint
import com.pagelence.presentation.pagination.PaginationKey
import com.pagelence.uim3.model.ParagraphUi
import com.pagelence.uim3.model.ReaderUiState
import com.pagelence.uieink.theme.einkReadingStyle

/** 最近翻过的单元，避免来回换单元时重新测量（与普通版同口径）。 */
private const val CacheEntries = 8
private val pageCache = PaginationCache<List<SentencePage>>(CacheEntries)

/**
 * 墨水屏正文分页：与普通版同一套 [:core] 分页器与同一份 `preferences`。
 *
 * 字号、行距、页边距跟普通版同步（T169），字体固定衬线；默认仍是 19sp /
 * 行高 1.74 / 左右各 22dp。分页缓存键带上这三项，改设置后按新档位重新测量。
 */
@Composable
internal fun rememberEinkTextPages(
    state: ReaderUiState,
    contentWidthPx: Int,
    pageHeightPx: Int,
    showAllTranslations: Boolean,
): List<SentencePage> {
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val preference = state.preference
    val readingStyle = einkReadingStyle(preference)
    val key = PaginationKey(
        bookId = state.bookId,
        unitId = state.unitId,
        contentWidthPx = contentWidthPx,
        pageHeightPx = pageHeightPx,
        fontSize = preference.fontSize,
        readingFont = ReadingFont.SERIF,
        lineSpacing = preference.lineSpacing,
        pageMargin = preference.pageMargin,
        showAllTranslations = showAllTranslations,
        language = state.language,
        density = density.density,
        fontScale = density.fontScale,
        fingerprint = PaginationFingerprint.of(state.paragraphs),
    )
    return remember(key) {
        pageCache.getOrPut(key) {
            // 页脚占一行，分页时先扣掉，避免最后一句被页脚压住。
            val usableHeight = with(density) { (pageHeightPx - 44.dp.toPx()).toInt() }
                .coerceAtLeast(200)
            val measured = state.paragraphs.flatMap { paragraph ->
                measureParagraph(paragraph, measurer, readingStyle, contentWidthPx, density, showAllTranslations)
            }
            SentencePaginator.paginate(measured, usableHeight)
        }
    }
}

private fun measureParagraph(
    paragraph: ParagraphUi,
    measurer: TextMeasurer,
    style: TextStyle,
    contentWidthPx: Int,
    density: Density,
    showAllTranslations: Boolean,
): List<MeasuredSentence> = with(density) {
    val width = contentWidthPx.coerceAtLeast(1)
    paragraph.sentences.mapIndexed { index, sentence ->
        val textHeight = measurer.measure(
            sentence.text,
            style = style,
            constraints = Constraints(maxWidth = width),
        ).size.height
        // 全部句译显示时，每句下面多一段中文；分页必须把它算进去，否则
        // 页脚会压住最后一句的译文。
        val zh = sentence.zh
        val translationHeight = if (showAllTranslations && !zh.isNullOrBlank()) {
            measurer.measure(
                zh,
                style = style.copy(fontSize = style.fontSize * 0.9f, lineHeight = style.lineHeight * 0.9f),
                constraints = Constraints(maxWidth = width),
            ).size.height + 4.dp.toPx()
        } else {
            0f
        }
        MeasuredSentence(
            paragraphId = paragraph.id,
            sentenceIndex = index,
            heightPx = (textHeight + translationHeight + 4.dp.toPx()).toInt(),
            leadingHeightPx = if (index == 0) 22.dp.toPx().toInt() else 0,
        )
    }
}
