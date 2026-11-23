package com.pagelence.uieink.screens

import android.os.SystemClock
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.nativeKeyCode
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pagelence.core.prefs.GuideMode
import com.pagelence.core.progress.ReaderPhase
import com.pagelence.core.reading.SentencePage
import com.pagelence.presentation.eink.EinkAction
import com.pagelence.presentation.eink.EinkKeyMap
import com.pagelence.presentation.eink.EinkRefreshRules
import com.pagelence.presentation.eink.EinkRefreshSettings
import com.pagelence.presentation.eink.EinkRefreshState
import com.pagelence.presentation.reader.ReaderFlow
import com.pagelence.presentation.reader.ReaderPage
import com.pagelence.presentation.text.WordBoundary
import com.pagelence.uim3.Strings
import com.pagelence.uim3.model.GrammarUi
import com.pagelence.uim3.model.ReaderUiState
import com.pagelence.uim3.model.SelectedWordUi
import com.pagelence.uim3.model.SentenceUi
import com.pagelence.uieink.components.InkButton
import com.pagelence.uieink.components.InkChoiceRow
import com.pagelence.uieink.components.InkDivider
import com.pagelence.uieink.components.InkPageFooter
import com.pagelence.uieink.components.InkText
import com.pagelence.uieink.components.InkTextButton
import com.pagelence.uieink.components.InkTopBar
import com.pagelence.uieink.theme.LocalInkColors
import com.pagelence.uieink.theme.LocalInkTypography

/**
 * 墨水屏阅读页：导读 → 正文（翻页）→ 测验 → 语法，与普通版共用 [ReaderFlow]
 * 的页序和 `:core` 的分页器，但不含滚动阅读、动效、浮层与内联图标。
 *
 * 交互只有三种：左右（方向键/翻页键/音量键也算）翻页、点按按钮、返回键。
 * 正文左右留白里各有一条 22dp、整页高的翻页带；正文中间不拦截点击，留给下一
 * 增量的「点词」。
 */
@Composable
fun EinkReaderScreen(
    strings: Strings,
    state: ReaderUiState,
    onBack: () -> Unit,
    onPageChanged: (ReaderPhase, Int, Float, String?) -> Unit,
    onSelectOption: (questionIndex: Int, optionIndex: Int) -> Unit,
    onSubmitQuiz: () -> Unit,
    onNextUnit: () -> Unit,
    onPreviousUnit: () -> Unit,
    onFullRefresh: () -> Unit,
    onWordClick: (paragraphId: String, sentenceNumber: Int, spelling: String, start: Int, end: Int) -> Unit,
    onOpenSearch: () -> Unit,
    onToggleAllTranslations: () -> Unit,
    onToggleBookmark: (paragraphId: String, sentenceNumber: Int) -> Unit,
    onToggleHighlight: (paragraphId: String, sentenceNumber: Int) -> Unit,
    onSaveNote: (paragraphId: String, sentenceNumber: Int, note: String) -> Unit,
    onDeleteNote: (paragraphId: String, sentenceNumber: Int) -> Unit,
    onJumpHandled: () -> Unit,
    modifier: Modifier = Modifier,
    refreshSettings: EinkRefreshSettings = EinkRefreshSettings.Default,
) {
    val colors = LocalInkColors.current
    val typography = LocalInkTypography.current
    val guide = remember(state.unitId, state.preference.guideMode, state.introductionZh, state.introductionEn) {
        guideCopy(state)
    }
    val focusRequester = remember { FocusRequester() }
    var refreshState by remember(state.unitId) {
        mutableStateOf(EinkRefreshState.opened(SystemClock.elapsedRealtime()))
    }
    // 标记模式：开着时点句子进标注页，关着时点词查词。
    var markMode by remember(state.unitId) { mutableStateOf(false) }
    var actionTarget by remember(state.unitId) { mutableStateOf<Pair<String, Int>?>(null) }

    Column(modifier.fillMaxSize().background(colors.paper)) {
        InkTopBar(
            title = state.bookTitle,
            subtitle = "${state.chapterTitle} · ${state.unitTitle}",
            backLabel = strings.back,
            onBack = onBack,
        )
        ReaderToolbar(
            strings = strings,
            markMode = markMode,
            showAllTranslations = state.preference.showAllTranslations,
            onSearch = onOpenSearch,
            onToggleMarkMode = { markMode = !markMode; actionTarget = null },
            onToggleTranslations = onToggleAllTranslations,
        )
        Box(Modifier.weight(1f).fillMaxWidth()) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val density = LocalDensity.current
            val horizontalPadding = with(density) { 22.dp.roundToPx() }
            val contentWidthPx = (constraints.maxWidth - horizontalPadding * 2).coerceAtLeast(1)
            val textPages = rememberEinkTextPages(
                state = state,
                contentWidthPx = contentWidthPx,
                pageHeightPx = constraints.maxHeight,
                showAllTranslations = state.preference.showAllTranslations,
            )
            val pages = remember(
                state.unitId,
                textPages,
                guide != null,
                state.quiz.size,
                state.grammar,
                state.hasPreviousUnit,
                state.hasNextUnit,
            ) {
                ReaderFlow.pages(state, textPages, guide != null, paged = true)
            }
            var cursor by remember(state.unitId, pages.size) {
                mutableIntStateOf(initialCursor(state, pages))
            }
            LaunchedEffect(state.unitId) {
                val decision = EinkRefreshRules.onUnitChange(
                    refreshState,
                    refreshSettings,
                    SystemClock.elapsedRealtime(),
                )
                refreshState = decision.state
                if (decision.fullRefresh) onFullRefresh()
            }
            LaunchedEffect(cursor, pages.size, state.unitId) {
                val page = pages.getOrNull(cursor) ?: return@LaunchedEffect
                reportOf(page, pages)?.let { (phase, textPage, anchor) ->
                    onPageChanged(phase, textPage, 0f, anchor)
                }
                val decision = EinkRefreshRules.onPageTurn(
                    refreshState,
                    refreshSettings,
                    SystemClock.elapsedRealtime(),
                )
                refreshState = decision.state
                if (decision.fullRefresh) onFullRefresh()
            }
            // 搜索 / 笔记列表要求定位到某一句：找到它所在的正文页并翻过去。
            LaunchedEffect(state.jumpTarget, pages.size) {
                val target = state.jumpTarget ?: return@LaunchedEffect
                val textPages = pages.withIndex().filter { it.value is ReaderPage.Text }
                val hit = textPages.firstOrNull { (_, page) ->
                    val text = page as ReaderPage.Text
                    text.page.chunks.any { it.paragraphId == target.paragraphId }
                }
                if (hit != null) cursor = hit.index
                onJumpHandled()
            }
            LaunchedEffect(Unit) { focusRequester.requestFocus() }

            val turnNext: () -> Unit = {
                if (cursor < pages.lastIndex) cursor += 1 else onNextUnit()
            }
            val turnPrevious: () -> Unit = {
                when {
                    cursor > 0 -> cursor -= 1
                    state.hasPreviousUnit -> onPreviousUnit()
                    else -> onBack()
                }
            }
            Column(Modifier.fillMaxSize()) {
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(start = 22.dp, end = 22.dp, top = 10.dp, bottom = 4.dp)
                        .focusRequester(focusRequester)
                        .focusable()
                        .onPreviewKeyEvent { event ->
                            if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                            when (EinkKeyMap.action(event.key.nativeKeyCode)) {
                                EinkAction.NEXT_PAGE -> { turnNext(); true }
                                EinkAction.PREVIOUS_PAGE -> { turnPrevious(); true }
                                EinkAction.BACK -> { onBack(); true }
                                EinkAction.CONFIRM, EinkAction.NONE -> false
                            }
                        },
                ) {
                    val page = pages.getOrNull(cursor)
                    if (page == null) {
                        InkText(
                            strings.loading,
                            style = typography.ui,
                            color = colors.inkMuted,
                            modifier = Modifier.padding(16.dp),
                        )
                    } else {
                        when (page) {
                            is ReaderPage.PreviousUnit -> BridgePage(
                                label = strings.grammarPrevious,
                                onOpen = onPreviousUnit,
                            )
                            is ReaderPage.Guide -> GuidePage(
                                strings = strings,
                                state = state,
                                guide = guide,
                                onStart = {
                                    val first = pages.indexOfFirst { it is ReaderPage.Text }
                                    if (first >= 0) cursor = first
                                },
                            )
                            is ReaderPage.Text -> TextPageView(
                                strings = strings,
                                state = state,
                                page = page.page,
                                onPrevious = turnPrevious,
                                onNext = turnNext,
                                onWordClick = onWordClick,
                                markMode = markMode,
                                onSentenceTap = { paragraphId, sentenceNumber ->
                                    actionTarget = paragraphId to sentenceNumber
                                },
                            )
                            is ReaderPage.TextScroll -> TextPageView(
                                strings = strings,
                                state = state,
                                page = textPages.firstOrNull(),
                                onPrevious = turnPrevious,
                                onNext = turnNext,
                                onWordClick = onWordClick,
                                markMode = markMode,
                                onSentenceTap = { paragraphId, sentenceNumber ->
                                    actionTarget = paragraphId to sentenceNumber
                                },
                            )
                            is ReaderPage.Quiz -> QuizPageView(
                                strings = strings,
                                state = state,
                                index = page.index,
                                onSelect = onSelectOption,
                                onSubmit = onSubmitQuiz,
                                onNextUnit = onNextUnit,
                            )
                            is ReaderPage.Grammar -> GrammarPageView(
                                strings = strings,
                                grammar = state.grammar,
                                hasNextUnit = state.hasNextUnit,
                                onNextUnit = onNextUnit,
                            )
                            is ReaderPage.NextUnit -> BridgePage(
                                label = strings.nextUnitGuide,
                                onOpen = onNextUnit,
                            )
                        }
                    }
                }
                InkPageFooter(
                    left = "${state.unitPosition} / ${state.unitCount}",
                    center = pageLabel(strings, pages.getOrNull(cursor), cursor, pages, state),
                    right = if (markMode) strings.markSentences else "←  →",
                )
            }
        }
            actionTarget?.let { (paragraphId, sentenceNumber) ->
                val sentence = state.paragraphs
                    .firstOrNull { it.id == paragraphId }
                    ?.sentences
                    ?.firstOrNull { it.number == sentenceNumber }
                if (sentence != null) {
                    Box(Modifier.fillMaxSize().background(colors.paper)) {
                        EinkSentenceActionsPage(
                            strings = strings,
                            quote = sentence.text,
                            mark = sentence.mark,
                            onToggleBookmark = { onToggleBookmark(paragraphId, sentenceNumber) },
                            onToggleHighlight = { onToggleHighlight(paragraphId, sentenceNumber) },
                            onSaveNote = { note -> onSaveNote(paragraphId, sentenceNumber, note) },
                            onDeleteNote = { onDeleteNote(paragraphId, sentenceNumber) },
                            onClose = { actionTarget = null },
                        )
                    }
                }
            }
        }
    }
}

/** 页脚页码：正文按页数、测验按题号，其余显示环节名。 */
private fun pageLabel(
    strings: Strings,
    page: ReaderPage?,
    cursor: Int,
    pages: List<ReaderPage>,
    state: ReaderUiState,
): String = when (page) {
    is ReaderPage.Text -> {
        val textIndex = pages.take(cursor).count { it is ReaderPage.Text }
        strings.pageText.format(textIndex + 1, pages.count { it is ReaderPage.Text })
    }
    ReaderPage.Guide -> strings.pageGuide
    is ReaderPage.Quiz -> strings.pageQuiz.format(page.index + 1, state.quiz.size)
    ReaderPage.Grammar -> strings.pageGrammar
    else -> ""
}

@Composable
private fun GuidePage(
    strings: Strings,
    state: ReaderUiState,
    guide: GuideCopy?,
    onStart: () -> Unit,
) {
    val colors = LocalInkColors.current
    val typography = LocalInkTypography.current
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        InkText(strings.readingGuide, style = typography.caption, color = colors.inkMuted)
        Spacer(Modifier.height(6.dp))
        InkText(state.unitTitle, style = typography.title)
        Spacer(Modifier.height(14.dp))
        guide?.zh?.let {
            InkText(it, style = typography.body)
            Spacer(Modifier.height(14.dp))
        }
        guide?.en?.let {
            InkText(it, style = typography.bodySmall, color = colors.inkSecondary)
            Spacer(Modifier.height(14.dp))
        }
        InkDivider()
        Spacer(Modifier.height(14.dp))
        InkButton(strings.startReading, onClick = onStart, primary = true)
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun TextPageView(
    strings: Strings,
    state: ReaderUiState,
    page: SentencePage?,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onWordClick: (String, Int, String, Int, Int) -> Unit,
    markMode: Boolean,
    onSentenceTap: (String, Int) -> Unit,
) {
    val typography = LocalInkTypography.current
    val colors = LocalInkColors.current
    if (page == null) {
        InkText(
            strings.loading,
            style = typography.ui,
            color = colors.inkMuted,
            modifier = Modifier.padding(16.dp),
        )
        return
    }
    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
        ) {
            page.chunks.forEach { chunk ->
                val paragraph = state.paragraphs.firstOrNull { it.id == chunk.paragraphId } ?: return@forEach
                val sentenceCount = paragraph.sentences.size
                val start = chunk.sentenceStart.coerceIn(0, sentenceCount)
                val end = chunk.sentenceEndExclusive.coerceIn(start, sentenceCount)
                if (start == end) return@forEach
                paragraph.sentences.subList(start, end).forEachIndexed { index, sentence ->
                    // 段首留白与分页测量里的 22dp 对应，段内句子之间 4dp。
                    Spacer(Modifier.height(if (index == 0) 20.dp else 4.dp))
                    InkSentence(
                        sentence = sentence,
                        paragraphId = paragraph.id,
                        selected = state.selectedWord,
                        onWordClick = onWordClick,
                        markMode = markMode,
                        onSentenceTap = onSentenceTap,
                        showTranslation = state.preference.showAllTranslations,
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
        }
        // 翻页带只占左右留白，不遮正文中间，点词由正文自己处理。
        PageTurnZone(Modifier.align(Alignment.CenterStart), onClick = onPrevious)
        PageTurnZone(Modifier.align(Alignment.CenterEnd), onClick = onNext)
    }
}

/**
 * 一句正文：整句可点。
 *
 * - 普通模式：点哪查哪，选中词灰底高亮。
 * - 标记模式：点句子打开标注页（书签 / 划线 / 笔记）。
 * - 划线：整句浅灰底；书签：句首「▍」；笔记：句末「✎」。
 * - 「全部句译」打开时，句下跟一行中文。
 */
@Composable
private fun InkSentence(
    sentence: SentenceUi,
    paragraphId: String,
    selected: SelectedWordUi?,
    onWordClick: (String, Int, String, Int, Int) -> Unit,
    markMode: Boolean,
    onSentenceTap: (String, Int) -> Unit,
    showTranslation: Boolean,
) {
    val typography = LocalInkTypography.current
    val colors = LocalInkColors.current
    var layout by remember(sentence.text) { mutableStateOf<TextLayoutResult?>(null) }
    val mark = sentence.mark
    val highlight = selected
        ?.takeIf { it.paragraphId == paragraphId && it.sentenceNumber == sentence.number }
    val prefix = if (mark?.bookmarked == true) "▍ " else ""
    val suffix = if (mark?.hasNote == true) "  ✎" else ""
    val text = remember(sentence.text, prefix, suffix, highlight?.start, highlight?.end, mark?.highlighted) {
        buildAnnotatedString {
            append(prefix)
            val start = length
            append(sentence.text)
            append(suffix)
            if (mark?.highlighted == true) {
                addStyle(SpanStyle(background = colors.markFill), start, start + sentence.text.length)
            }
            if (highlight != null && highlight.start in 0..highlight.end && highlight.end < sentence.text.length) {
                addStyle(
                    SpanStyle(background = colors.fill, fontWeight = FontWeight.SemiBold),
                    start + highlight.start,
                    start + highlight.end + 1,
                )
            }
        }
    }
    Column {
        BasicText(
            text = text,
            modifier = Modifier
                .fillMaxWidth()
                .pointerInput(sentence.text, markMode) {
                    detectTapGestures { offset ->
                        if (markMode) {
                            onSentenceTap(paragraphId, sentence.number)
                            return@detectTapGestures
                        }
                        val result = layout ?: return@detectTapGestures
                        val index = result.getOffsetForPosition(offset)
                        WordBoundary.wordAt(sentence.text, index)?.let { hit ->
                            onWordClick(paragraphId, sentence.number, hit.text, hit.start, hit.end)
                        }
                    }
                },
            style = typography.body.merge(TextStyle(color = colors.ink)),
            onTextLayout = { layout = it },
        )
        val zh = sentence.zh
        if (showTranslation && !zh.isNullOrBlank()) {
            InkText(zh, style = typography.bodySmall, color = colors.inkSecondary)
        }
    }
}

/** 阅读页工具条：搜索、标记模式、全部句译。 */
@Composable
private fun ReaderToolbar(
    strings: Strings,
    markMode: Boolean,
    showAllTranslations: Boolean,
    onSearch: () -> Unit,
    onToggleMarkMode: () -> Unit,
    onToggleTranslations: () -> Unit,
) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
        InkTextButton(strings.inBookSearch, onClick = onSearch)
        InkTextButton(
            label = (if (markMode) "■ " else "□ ") + strings.markSentences,
            onClick = onToggleMarkMode,
        )
        InkTextButton(
            label = if (showAllTranslations) strings.hideAllTranslations else strings.showAllTranslations,
            onClick = onToggleTranslations,
        )
    }
    InkDivider()
}

@Composable
private fun PageTurnZone(modifier: Modifier, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier
            .width(22.dp)
            .fillMaxHeight()
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
    )
}

@Composable
private fun QuizPageView(
    strings: Strings,
    state: ReaderUiState,
    index: Int,
    onSelect: (Int, Int) -> Unit,
    onSubmit: () -> Unit,
    onNextUnit: () -> Unit,
) {
    val colors = LocalInkColors.current
    val typography = LocalInkTypography.current
    val question = state.quiz.getOrNull(index) ?: return
    val submitted = state.quizSubmitted
    val allAnswered = state.quiz.all { it.selected != null }
    val isLastQuiz = index == state.quiz.lastIndex
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        InkText(
            strings.pageQuiz.format(index + 1, state.quiz.size),
            style = typography.caption,
            color = colors.inkMuted,
        )
        Spacer(Modifier.height(8.dp))
        InkText("${index + 1}. ${question.question}", style = typography.uiStrong)
        Spacer(Modifier.height(12.dp))
        question.options.forEachIndexed { optionIndex, option ->
            val selected = question.selected == optionIndex
            InkChoiceRow(
                index = optionIndex,
                label = option,
                selected = selected,
                revealed = submitted && (selected || optionIndex == question.correct),
                correct = optionIndex == question.correct,
                enabled = !submitted,
                onClick = { onSelect(index, optionIndex) },
            )
        }
        if (submitted) {
            question.feedback?.let {
                Spacer(Modifier.height(10.dp))
                InkText(it, style = typography.bodySmall, color = colors.inkSecondary)
            }
            if (isLastQuiz) {
                Spacer(Modifier.height(14.dp))
                InkDivider()
                Spacer(Modifier.height(10.dp))
                InkText(
                    strings.quizScore.format(state.quizScore, state.quiz.size),
                    style = typography.uiStrong,
                )
                Spacer(Modifier.height(12.dp))
                InkButton(
                    label = if (state.hasNextUnit) strings.nextUnit else strings.finishBook,
                    onClick = onNextUnit,
                    primary = true,
                )
            }
        } else if (allAnswered) {
            Spacer(Modifier.height(14.dp))
            InkButton(strings.quizSubmit, onClick = onSubmit, primary = true)
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun GrammarPageView(
    strings: Strings,
    grammar: GrammarUi?,
    hasNextUnit: Boolean,
    onNextUnit: () -> Unit,
) {
    val colors = LocalInkColors.current
    val typography = LocalInkTypography.current
    if (grammar == null) return
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        InkText(strings.grammar, style = typography.caption, color = colors.inkMuted)
        Spacer(Modifier.height(6.dp))
        grammar.pointName?.let {
            InkText(it, style = typography.title)
            Spacer(Modifier.height(10.dp))
        }
        InkText(grammar.excerpt, style = typography.body)
        Spacer(Modifier.height(14.dp))
        InkDivider()
        Spacer(Modifier.height(12.dp))
        grammar.explain?.let {
            InkText(strings.grammarExplain, style = typography.uiStrong)
            InkText(it, style = typography.bodySmall, color = colors.inkSecondary)
            Spacer(Modifier.height(10.dp))
        }
        grammar.form?.let {
            InkText(strings.grammarForm, style = typography.uiStrong)
            InkText(it, style = typography.bodySmall, color = colors.inkSecondary)
            Spacer(Modifier.height(10.dp))
        }
        grammar.focus?.let {
            InkText(strings.grammarFocus, style = typography.uiStrong)
            InkText(it, style = typography.bodySmall, color = colors.inkSecondary)
            Spacer(Modifier.height(10.dp))
        }
        if (grammar.pitfalls.isNotEmpty()) {
            InkText(strings.grammarPitfalls, style = typography.uiStrong)
            grammar.pitfalls.forEach {
                InkText("· $it", style = typography.bodySmall, color = colors.inkSecondary)
            }
            Spacer(Modifier.height(10.dp))
        }
        if (grammar.extraPoints.isNotEmpty()) {
            InkText(strings.grammarAlsoUses, style = typography.uiStrong)
            grammar.extraPoints.forEach {
                InkText("· ${it.name}", style = typography.bodySmall, color = colors.inkSecondary)
            }
            Spacer(Modifier.height(10.dp))
        }
        Spacer(Modifier.height(14.dp))
        InkDivider()
        Spacer(Modifier.height(12.dp))
        InkButton(
            label = if (hasNextUnit) strings.nextUnit else strings.finishBook,
            onClick = onNextUnit,
            primary = true,
        )
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun BridgePage(label: String, onOpen: () -> Unit) {
    val typography = LocalInkTypography.current
    Column(
        Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        InkText(label, style = typography.title)
        Spacer(Modifier.height(16.dp))
        InkButton(label, onClick = onOpen, primary = true)
    }
}

private data class GuideCopy(val zh: String?, val en: String?) {
    val isEmpty: Boolean get() = zh.isNullOrBlank() && en.isNullOrBlank()
}

private fun guideCopy(state: ReaderUiState): GuideCopy? = when (state.preference.guideMode) {
    GuideMode.BILINGUAL -> GuideCopy(state.introductionZh, state.introductionEn)
    GuideMode.EN -> GuideCopy(null, state.introductionEn ?: state.introductionZh)
    GuideMode.IMMERSIVE -> null
}?.takeUnless { it.isEmpty }

private fun initialCursor(state: ReaderUiState, pages: List<ReaderPage>): Int {
    val firstText = pages.indexOfFirst { it is ReaderPage.Text }
    val firstQuiz = pages.indexOfFirst { it is ReaderPage.Quiz }
    val grammar = pages.indexOfFirst { it is ReaderPage.Grammar }
    val target = when (state.resumePhase) {
        ReaderPhase.GUIDE -> pages.indexOfFirst { it is ReaderPage.Guide }
        ReaderPhase.TEXT -> if (firstText < 0) 0 else firstText + state.resumeTextPage
        ReaderPhase.QUIZ -> if (firstQuiz < 0) 0 else firstQuiz
        ReaderPhase.GRAMMAR -> if (grammar < 0) 0 else grammar
    }
    return target.coerceIn(0, pages.lastIndex.coerceAtLeast(0))
}

private fun reportOf(page: ReaderPage, pages: List<ReaderPage>): Triple<ReaderPhase, Int, String?>? = when (page) {
    is ReaderPage.Text -> Triple(
        ReaderPhase.TEXT,
        pages.takeWhile { it !== page }.count { it is ReaderPage.Text },
        page.page.anchorParagraphId,
    )
    is ReaderPage.Guide -> Triple(ReaderPhase.GUIDE, 0, null)
    is ReaderPage.Quiz -> Triple(ReaderPhase.QUIZ, 0, null)
    is ReaderPage.Grammar -> Triple(ReaderPhase.GRAMMAR, 0, null)
    is ReaderPage.TextScroll -> Triple(ReaderPhase.TEXT, 0, null)
    is ReaderPage.PreviousUnit, is ReaderPage.NextUnit -> null
}
