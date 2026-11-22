package com.pagelence.uim3.screens

// ReaderPaging：从 ReaderScreen.kt 拆出；只做展示，不含业务规则。

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import com.pagelence.core.reading.MeasuredSentence
import com.pagelence.core.reading.SentencePage
import com.pagelence.core.reading.SentencePaginator
import com.pagelence.uim3.Strings
import com.pagelence.presentation.pagination.PaginationCache
import com.pagelence.presentation.pagination.PaginationFingerprint
import com.pagelence.presentation.pagination.PaginationKey
import com.pagelence.uim3.model.ParagraphUi
import com.pagelence.uim3.model.ReaderUiState
import com.pagelence.uim3.model.SentenceUi
import com.pagelence.uim3.theme.readingTextStyle

/** Recent units kept across page turns, guides and quiz trips (E-15). */
private const val TextPageCacheEntries = 8

/**
 * Process-wide so returning to a unit does not measure it again. Keys are
 * cheap, so the LRU lookup is safe to do during composition.
 */
private val textPageCache = PaginationCache<List<SentencePage>>(TextPageCacheEntries)

@Composable
internal fun rememberTextPages(
    state: ReaderUiState,
    strings: Strings,
    contentWidthPx: Int,
    pageHeightPx: Int,
): List<SentencePage> {
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val readingStyle = readingTextStyle(
        fontSize = state.preference.fontSize,
        readingFont = state.preference.readingFont,
        lineSpacing = state.preference.lineSpacing,
    )
    // E-15: key on small values plus a text fingerprint instead of the
    // paragraph list; the old key deep-compared the whole unit on every state
    // rebuild. Bookmark/highlight/note changes do not invalidate the cache
    // because the reader draws those markers after the text (no reflow).
    val key = PaginationKey(
        bookId = state.bookId,
        unitId = state.unitId,
        contentWidthPx = contentWidthPx,
        pageHeightPx = pageHeightPx,
        fontSize = state.preference.fontSize,
        readingFont = state.preference.readingFont,
        lineSpacing = state.preference.lineSpacing,
        pageMargin = state.preference.pageMargin,
        showAllTranslations = state.preference.showAllTranslations,
        language = state.language,
        density = density.density,
        fontScale = density.fontScale,
        fingerprint = PaginationFingerprint.of(state.paragraphs),
    )
    return remember(key) {
        textPageCache.getOrPut(key) {
            // Leave room for the page indicator chip at the bottom of a text page.
            val usableHeight = with(density) { (pageHeightPx - 42.dp.toPx()).toInt() }
                .coerceAtLeast(200)
            val measured = state.paragraphs.flatMap { paragraph ->
                measureSentences(
                    paragraph = paragraph,
                    measurer = measurer,
                    strings = strings,
                    readingStyle = readingStyle,
                    contentWidthPx = contentWidthPx,
                    density = density,
                    showAllTranslations = state.preference.showAllTranslations,
                )
            }
            SentencePaginator.paginate(measured, usableHeight)
        }
    }
}

internal fun measureSentences(
    paragraph: ParagraphUi,
    measurer: TextMeasurer,
    strings: Strings,
    readingStyle: TextStyle,
    contentWidthPx: Int,
    density: Density,
    showAllTranslations: Boolean,
): List<MeasuredSentence> = with(density) {
    val sentenceWidth = contentWidthPx.coerceAtLeast(1)
    paragraph.sentences.mapIndexed { index, sentence ->
        val measured = measuredSentence(sentence, strings, density, showAllTranslations)
        val textHeight = measurer.measure(
            measured.first,
            style = readingStyle,
            placeholders = measured.second,
            constraints = Constraints(maxWidth = sentenceWidth),
        ).size.height
        val height = textHeight.toFloat() + 4.dp.toPx()
        val leading = if (index == 0) 22.dp.toPx() else 0f
        MeasuredSentence(
            paragraphId = paragraph.id,
            sentenceIndex = index,
            heightPx = height.toInt(),
            leadingHeightPx = leading.toInt(),
        )
    }
}

/**
 * Sentence text plus the inline translation icon placeholder. Returns the
 * annotated text and the placeholder ranges the measurer needs.
 */
internal fun measuredSentence(
    sentence: SentenceUi,
    strings: Strings,
    density: Density,
    showAllTranslations: Boolean,
): Pair<AnnotatedString, List<AnnotatedString.Range<Placeholder>>> {
    val hidesIcons = sentence.zh.isNullOrBlank() && sentence.analysis == null
    if (hidesIcons) return AnnotatedString(sentence.text) to emptyList()
    val flow = buildSentenceFlow(
        strings = strings,
        sentences = listOf(sentence),
        showAllTranslations = showAllTranslations,
        translationStyle = SpanStyle(fontSize = 0.86.em),
    )
    val ranges = mutableListOf<AnnotatedString.Range<Placeholder>>()
    if (flow.iconStarts.isNotEmpty()) {
        val sizePx = with(density) { SentenceTranslateIconSize.toPx() }
        val size = with(density) { sizePx.toSp() }
        flow.iconStarts.keys.forEach { at ->
            ranges += AnnotatedString.Range(
                Placeholder(size, size, PlaceholderVerticalAlign.Center),
                at,
                at + 1,
            )
        }
    }
    if (flow.analysisStarts.isNotEmpty()) {
        val sizePx = with(density) { SentenceAnalysisIconSize.toPx() }
        val size = with(density) { sizePx.toSp() }
        flow.analysisStarts.keys.forEach { at ->
            ranges += AnnotatedString.Range(
                Placeholder(size, size, PlaceholderVerticalAlign.Center),
                at,
                at + 1,
            )
        }
    }
    return flow.annotated to ranges
}
