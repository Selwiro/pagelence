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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.pagelence.core.reading.MeasuredSentence
import com.pagelence.core.reading.SentencePage
import com.pagelence.core.reading.SentencePaginator
import com.pagelence.uim3.model.ParagraphUi
import com.pagelence.uim3.model.ReaderUiState
import com.pagelence.uim3.model.SentenceUi
import com.pagelence.uim3.theme.readingTextStyle

@Composable
internal fun rememberTextPages(
    state: ReaderUiState,
    contentWidthPx: Int,
    pageHeightPx: Int,
): List<SentencePage> {
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val readingStyle = readingTextStyle(state.preference.fontSize)
    return remember(
        state.unitId,
        contentWidthPx,
        pageHeightPx,
        state.preference.fontSize,
        state.preference.language,
        state.paragraphs,
    ) {
        // Leave room for the page indicator chip at the bottom of a text page.
        val usableHeight = with(density) { (pageHeightPx - 42.dp.toPx()).toInt() }
            .coerceAtLeast(200)
        val measured = state.paragraphs.flatMap { paragraph ->
            measureSentences(
                paragraph = paragraph,
                measurer = measurer,
                readingStyle = readingStyle,
                contentWidthPx = contentWidthPx,
                density = density,
            )
        }
        SentencePaginator.paginate(measured, usableHeight)
    }
}

internal fun measureSentences(
    paragraph: ParagraphUi,
    measurer: TextMeasurer,
    readingStyle: TextStyle,
    contentWidthPx: Int,
    density: Density,
): List<MeasuredSentence> = with(density) {
    val sentenceWidth = contentWidthPx.coerceAtLeast(1)
    paragraph.sentences.mapIndexed { index, sentence ->
        val measured = measuredSentence(sentence, density)
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
    density: Density,
): Pair<AnnotatedString, List<AnnotatedString.Range<Placeholder>>> {
    if (sentence.zh.isNullOrBlank() && sentence.analysis == null) {
        return AnnotatedString(sentence.text) to emptyList()
    }
    val ranges = mutableListOf<AnnotatedString.Range<Placeholder>>()
    val iconSizePx = with(density) { SentenceTranslateIconSize.toPx() }
    val iconSize = with(density) { iconSizePx.toSp() }
    val analysisSize = with(density) { SentenceAnalysisIconSize.toPx().toSp() }
    val text = buildAnnotatedString {
        append(sentence.text)
        if (!sentence.zh.isNullOrBlank()) {
            append(" ")
            val at = length
            appendInlineContent(SentenceTranslateInlineId)
            ranges += AnnotatedString.Range(
                Placeholder(iconSize, iconSize, PlaceholderVerticalAlign.Center),
                at,
                at + 1,
            )
        }
        if (sentence.analysis != null) {
            append(" ")
            val at = length
            appendInlineContent(SentenceAnalysisInlineId)
            ranges += AnnotatedString.Range(
                Placeholder(analysisSize, analysisSize, PlaceholderVerticalAlign.Center),
                at,
                at + 1,
            )
        }
    }
    return text to ranges
}
