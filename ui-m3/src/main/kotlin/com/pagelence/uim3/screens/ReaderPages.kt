package com.pagelence.uim3.screens

// ReaderPages：从 ReaderScreen.kt 拆出；只做展示，不含业务规则。

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import com.pagelence.core.reading.SentencePage
import com.pagelence.core.reading.TextPageChunk
import com.pagelence.uim3.Strings
import com.pagelence.uim3.components.MetaChip
import com.pagelence.uim3.model.QuizQuestionUi
import com.pagelence.uim3.model.ReaderPreviewUi
import com.pagelence.uim3.model.ReaderUiState
import com.pagelence.uim3.model.SentenceUi
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first

@Composable
internal fun PreviewPageView(
    strings: Strings,
    state: ReaderUiState,
    preview: ReaderPreviewUi,
    onAnswer: (Boolean) -> Unit,
    onNext: () -> Unit,
    onFinish: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val word = preview.current
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 20.dp),
    ) {
        Text(strings.previewTitle, style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(6.dp))
        if (preview.words.isNotEmpty()) {
            Text(
                strings.previewProgress.format(
                    (preview.index + 1).coerceAtMost(preview.words.size),
                    preview.words.size,
                ),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.outlineVariant),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(
                            if (preview.words.isEmpty()) 0f
                            else preview.index.toFloat() / preview.words.size,
                        )
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.primary),
                )
            }
        }
        Spacer(Modifier.height(24.dp))

        when {
            preview.words.isEmpty() -> {
                Text(
                    strings.previewEmpty,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(24.dp))
                Button(onClick = onFinish, modifier = Modifier.fillMaxWidth()) {
                    Text(strings.startReading)
                }
            }

            preview.finished -> {
                Text(strings.previewDone, style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(8.dp))
                Text(
                    strings.previewDoneHint,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(24.dp))
                Button(onClick = onFinish, modifier = Modifier.fillMaxWidth()) {
                    Text(strings.startReading)
                }
            }

            word != null -> {
                Text(word.term, style = MaterialTheme.typography.displaySmall)
                word.ipa?.takeIf { it.isNotBlank() }?.let {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "/$it/",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    MetaChip(word.level)
                    Text(
                        strings.previewLevelHint.format(state.bookLevel.orEmpty()),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.height(28.dp))

                if (!preview.revealed) {
                    Text(strings.previewPrompt, style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Button(
                            onClick = { onAnswer(true) },
                            modifier = Modifier.weight(1f),
                        ) { Text(strings.previewKnown) }
                        OutlinedButton(
                            onClick = { onAnswer(false) },
                            modifier = Modifier.weight(1f),
                        ) { Text(strings.previewUnknown) }
                    }
                } else {
                    Surface(
                        shape = MaterialTheme.shapes.large,
                        color = MaterialTheme.colorScheme.surfaceContainer,
                    ) {
                        Column(Modifier.padding(18.dp)) {
                            word.zh?.takeIf { it.isNotBlank() }?.let {
                                Text(it, style = MaterialTheme.typography.bodyLarge)
                            }
                            word.en?.takeIf { it.isNotBlank() }?.let {
                                if (!word.zh.isNullOrBlank()) Spacer(Modifier.height(10.dp))
                                Text(
                                    it,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    Text(
                        when {
                            preview.lastKnown == false -> strings.previewAdded
                            word.inWordCards -> strings.previewInCards
                            else -> strings.previewKnownTag
                        },
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.height(18.dp))
                    Button(onClick = onNext, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            if (preview.index == preview.words.lastIndex) {
                                strings.previewDone
                            } else {
                                strings.previewNext
                            },
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        TextButton(onClick = onClose, modifier = Modifier.fillMaxWidth()) {
            Text(strings.back)
        }
    }
}

/**
 * Transient divider shown while a swipe crosses a unit boundary. The pager
 * settles here first; the screen then opens the adjacent unit's target page.
 */
@Composable
internal fun UnitBridgePage(label: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(22.dp),
            strokeWidth = 2.dp,
        )
        Spacer(Modifier.height(14.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
internal fun GuidePageView(
    strings: Strings,
    state: ReaderUiState,
    copy: GuideCopy,
    onStart: () -> Unit,
    onPreview: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 20.dp),
    ) {
        Text(
            state.chapterTitle,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(6.dp))
        Text(state.unitTitle, style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(20.dp))
        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.primaryContainer,
        ) {
            Column(Modifier.padding(20.dp)) {
                Text(
                    strings.readingGuide,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                copy.zh?.takeIf { it.isNotBlank() }?.let { zh ->
                    Spacer(Modifier.height(10.dp))
                    Text(
                        zh,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
                if (!copy.zh.isNullOrBlank() && !copy.en.isNullOrBlank()) {
                    Spacer(Modifier.height(14.dp))
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.18f),
                    )
                    Spacer(Modifier.height(14.dp))
                }
                copy.en?.takeIf { it.isNotBlank() }?.let { en ->
                    Text(
                        en,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.88f),
                    )
                }
            }
        }
        Spacer(Modifier.height(22.dp))
        Button(onClick = onStart, modifier = Modifier.fillMaxWidth()) {
            Text(strings.startReading)
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = onPreview, modifier = Modifier.fillMaxWidth()) {
            Text(strings.preview)
        }
    }
}

@Composable
internal fun TextPageView(
    strings: Strings,
    state: ReaderUiState,
    page: SentencePage,
    pageIndex: Int,
    pageCount: Int,
    hasNextPage: Boolean,
    onWordClick: (String, Int, String, Int, Int) -> Unit,
    onAnalysis: (SentenceUi) -> Unit,
    onNextPage: () -> Unit,
    onNextUnit: () -> Unit,
    expandedTranslation: ExpandedTranslation?,
    focus: SentenceFocus?,
    onTranslate: (String, SentenceUi) -> Unit,
    onSentenceLongPress: (String, SentenceUi) -> Unit,
) {
    val scroll = rememberScrollState()
    val horizontalPadding = state.preference.pageMargin.horizontalPadding()
    val atEnd by remember { derivedStateOf { scroll.value >= scroll.maxValue - 8 } }
    val scrollable = page.scrollable || expandedTranslation != null
    Box(Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 38.dp)
                .then(if (scrollable) Modifier.verticalScroll(scroll) else Modifier),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 760.dp)
                    .padding(horizontal = horizontalPadding, vertical = 10.dp),
            ) {
                page.chunks.forEach { chunk ->
                    ParagraphChunkView(
                        strings = strings,
                        state = state,
                        chunk = chunk,
                        expandedTranslation = expandedTranslation,
                        focus = focus,
                        onWordClick = onWordClick,
                        onAnalysis = onAnalysis,
                        onTranslate = onTranslate,
                        onSentenceLongPress = onSentenceLongPress,
                    )
                }
            }
        }
        if (scrollable && expandedTranslation == null) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 42.dp)
                    .fillMaxWidth()
                    .height(20.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                when {
                    atEnd && hasNextPage -> TextButton(
                        onClick = onNextPage,
                        contentPadding = PaddingValues(horizontal = 12.dp),
                    ) { Text(strings.nextPage) }

                    atEnd -> Text(
                        strings.scrollEnd,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    else -> Text(
                        strings.scrollHint,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        PageIndicator(
            strings = strings,
            page = pageIndex,
            total = pageCount,
            showNext = pageIndex == pageCount,
            nextLabel = when {
                state.quiz.isNotEmpty() -> strings.enterQuiz
                state.grammar != null -> strings.continueReading
                state.hasNextUnit -> strings.nextUnit
                else -> strings.finishBook
            },
            onNext = if (state.quiz.isEmpty() && state.grammar == null) onNextUnit else onNextPage,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 6.dp)
                .fillMaxWidth(),
        )
    }
}

@Composable
internal fun ParagraphChunkView(
    strings: Strings,
    state: ReaderUiState,
    chunk: TextPageChunk,
    expandedTranslation: ExpandedTranslation?,
    focus: SentenceFocus?,
    onWordClick: (String, Int, String, Int, Int) -> Unit,
    onAnalysis: (SentenceUi) -> Unit,
    onTranslate: (String, SentenceUi) -> Unit,
    onSentenceLongPress: (String, SentenceUi) -> Unit,
) {
    val paragraph = state.paragraphs.firstOrNull { it.id == chunk.paragraphId } ?: return
    if (chunk.sentenceStart == 0) Spacer(Modifier.height(22.dp))
    val end = chunk.sentenceEndExclusive.coerceAtMost(paragraph.sentences.size)
    if (chunk.sentenceStart >= end) return
    val chunkSentences = paragraph.sentences.subList(chunk.sentenceStart, end)
    val expandedIndex = expandedTranslation
        ?.takeIf { !state.preference.showAllTranslations && it.paragraphId == paragraph.id }
        ?.let { expanded -> chunkSentences.indexOfFirst { it.number == expanded.sentenceNumber } }
        ?: -1
    if (expandedIndex < 0) {
        ParagraphText(
            strings = strings,
            paragraphId = paragraph.id,
            sentences = chunkSentences,
            fontSize = state.preference.fontSize,
            readingFont = state.preference.readingFont,
            lineSpacing = state.preference.lineSpacing,
            showAllTranslations = state.preference.showAllTranslations,
            annotateHints = state.preference.annotateHints,
            selectedWord = state.selectedWord,
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
            sentences = chunkSentences.subList(0, expandedIndex + 1),
            fontSize = state.preference.fontSize,
            readingFont = state.preference.readingFont,
            lineSpacing = state.preference.lineSpacing,
            showAllTranslations = state.preference.showAllTranslations,
            annotateHints = state.preference.annotateHints,
            selectedWord = state.selectedWord,
            focus = focus,
            onWordClick = onWordClick,
            onAnalysis = onAnalysis,
            onTranslate = onTranslate,
            onSentenceLongPress = onSentenceLongPress,
        )
        TranslationExpansion(expandedTranslation?.zh.orEmpty())
        if (expandedIndex + 1 < chunkSentences.size) {
            ParagraphText(
                strings = strings,
                paragraphId = paragraph.id,
                sentences = chunkSentences.subList(expandedIndex + 1, chunkSentences.size),
                fontSize = state.preference.fontSize,
                readingFont = state.preference.readingFont,
                lineSpacing = state.preference.lineSpacing,
                showAllTranslations = state.preference.showAllTranslations,
                annotateHints = state.preference.annotateHints,
                selectedWord = state.selectedWord,
                focus = focus,
                onWordClick = onWordClick,
                onAnalysis = onAnalysis,
                onTranslate = onTranslate,
                onSentenceLongPress = onSentenceLongPress,
            )
        }
    }
}

@Composable
internal fun TextScrollView(
    strings: Strings,
    state: ReaderUiState,
    startAtTop: Boolean,
    expandedTranslation: ExpandedTranslation?,
    focus: SentenceFocus?,
    enterLabel: String,
    onWordClick: (String, Int, String, Int, Int) -> Unit,
    onAnalysis: (SentenceUi) -> Unit,
    onTranslate: (String, SentenceUi) -> Unit,
    onSentenceLongPress: (String, SentenceUi) -> Unit,
    onEnterQuiz: () -> Unit,
    onProgress: (Float) -> Unit,
    scrollToParagraphId: String? = null,
    onScrollHandled: () -> Unit = {},
) {
    val scroll = rememberScrollState()
    val horizontalPadding = state.preference.pageMargin.horizontalPadding()
    var restored by remember { mutableStateOf(false) }
    var hasScrolled by remember { mutableStateOf(false) }
    val paragraphOffsets = remember { mutableStateMapOf<String, Int>() }

    LaunchedEffect(scroll.maxValue, startAtTop) {
        if (!restored && scroll.maxValue > 0) {
            val target = if (startAtTop) 0f else state.resumeTextProgress.coerceIn(0f, 1f)
            scroll.scrollTo((scroll.maxValue * target).toInt())
            restored = true
        }
    }
    // A list row asked for one sentence: scroll its paragraph to the top. The
    // offset is published by layout, so this waits for the first real value.
    LaunchedEffect(scrollToParagraphId) {
        val paragraphId = scrollToParagraphId ?: return@LaunchedEffect
        val offset = snapshotFlow { paragraphOffsets[paragraphId] }
            .filterNotNull()
            .first()
        restored = true
        scroll.scrollTo(offset.coerceAtLeast(0))
        onScrollHandled()
    }
    LaunchedEffect(scroll) {
        snapshotFlow { scroll.isScrollInProgress }
            .distinctUntilChanged()
            .collect { inProgress ->
                if (inProgress) {
                    hasScrolled = true
                } else if (hasScrolled) {
                    onProgress(if (scroll.maxValue > 0) scroll.value.toFloat() / scroll.maxValue else 0f)
                }
            }
    }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = Modifier
                // Same reading column as the paged layout: on a tablet a full
                // width line is too long to read comfortably.
                .widthIn(max = 760.dp)
                .fillMaxWidth()
                .fillMaxHeight()
                .verticalScroll(scroll)
                .padding(horizontal = horizontalPadding, vertical = 10.dp),
        ) {
            state.paragraphs.forEach { paragraph ->
            Box(
                modifier = Modifier.onGloballyPositioned { coordinates ->
                    paragraphOffsets[paragraph.id] = coordinates.positionInParent().y.toInt()
                },
            ) {
                ParagraphBlock(
                    strings = strings,
                    paragraph = paragraph,
                    fontSize = state.preference.fontSize,
                    readingFont = state.preference.readingFont,
                    lineSpacing = state.preference.lineSpacing,
                    showAllTranslations = state.preference.showAllTranslations,
                    annotateHints = state.preference.annotateHints,
                    selectedWord = state.selectedWord,
                    expandedTranslation = expandedTranslation,
                    focus = focus,
                    onWordClick = onWordClick,
                    onAnalysis = onAnalysis,
                    onTranslate = onTranslate,
                    onSentenceLongPress = onSentenceLongPress,
                )
            }
            Spacer(Modifier.height(22.dp))
        }
        Spacer(Modifier.height(8.dp))
        Button(onClick = onEnterQuiz, modifier = Modifier.fillMaxWidth()) {
            Text(enterLabel)
        }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
internal fun QuizPageView(
    strings: Strings,
    question: QuizQuestionUi,
    index: Int,
    total: Int,
    allAnswered: Boolean,
    unansweredCount: Int,
    onSelect: (Int) -> Unit,
    onNext: () -> Unit,
) {
    var showSentence by rememberSaveable(index, question.selected) { mutableStateOf(false) }
    val answered = question.selected != null
    val correct = question.selected == question.correct
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 14.dp),
    ) {
        Text(
            strings.pageQuiz.format(index + 1, total),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(8.dp))
        Text("${index + 1}. ${question.question}", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(14.dp))
        question.options.forEachIndexed { optionIndex, option ->
            val selected = question.selected == optionIndex
            val border = when {
                answered && selected && !correct -> MaterialTheme.colorScheme.error
                selected -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.outlineVariant
            }
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .selectable(
                        selected = selected,
                        enabled = !answered,
                        onClick = { onSelect(optionIndex) },
                    ),
                shape = MaterialTheme.shapes.medium,
                color = if (selected) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceContainerLow
                },
                border = BorderStroke(1.dp, border),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(
                        selected = selected,
                        onClick = { onSelect(optionIndex) },
                        enabled = !answered,
                    )
                    Text(option, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        if (answered) {
            Spacer(Modifier.height(10.dp))
            Text(
                if (correct) strings.quizCorrect else strings.quizWrong,
                style = MaterialTheme.typography.labelLarge,
                color = if (correct) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
            )
            question.feedback?.takeIf { it.isNotBlank() }?.let {
                Spacer(Modifier.height(4.dp))
                Text(
                    it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            val sentenceText = question.sentenceText
            if (sentenceText != null) {
                TextButton(onClick = { showSentence = !showSentence }) {
                    Text(if (showSentence) strings.quizHideSentence else strings.quizSeeSentence)
                }
                if (showSentence) {
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.surfaceContainer,
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Text(
                                sentenceText,
                                style = MaterialTheme.typography.bodyMedium,
                                fontStyle = FontStyle.Italic,
                            )
                            question.sentenceZh?.takeIf { it.isNotBlank() }?.let {
                                Text(
                                    it,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(18.dp))
        Button(
            onClick = onNext,
            enabled = answered && (index != total - 1 || allAnswered),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                when {
                    index == total - 1 -> strings.continueReading
                    else -> strings.quizNextQuestion
                },
            )
        }
        if (index == total - 1 && !allAnswered) {
            Spacer(Modifier.height(8.dp))
            Text(
                strings.quizUnanswered.format(unansweredCount),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
