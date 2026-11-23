package com.pagelence.uim3.screens

// 阅读页：导读、正文（翻页或上下滑动）、逐题测验、本轮回顾、语法与回顾。
// 页面编排与回调转发留在这里；顶栏、分页、正文、词条、目录分别在同目录的
// ReaderChrome / ReaderPaging / ReaderText / ReaderWordPanel / ReaderSheets。

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.unit.dp
import com.pagelence.core.prefs.ReaderLayout
import com.pagelence.core.progress.ReaderPhase
import com.pagelence.uim3.Strings
import com.pagelence.uim3.components.GrammarCard
import com.pagelence.uim3.model.AnnotationUi
import com.pagelence.uim3.model.ReaderUiState
import com.pagelence.uim3.model.SentenceMarkUi
import com.pagelence.presentation.reader.ReaderFlow
import com.pagelence.presentation.reader.ReaderPage
import com.pagelence.uim3.model.SentenceUi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 阅读页：导读、正文（翻页或上下滑动）、逐题测验、本轮回顾、语法与回顾。
 * 本文件只做展示与事件上报；句级分页由 TextMeasurer 测量，规则在 :core。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    strings: Strings,
    state: ReaderUiState,
    onBack: () -> Unit,
    onOpenCards: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenSearch: () -> Unit,
    onWordClick: (paragraphId: String, sentenceNumber: Int, spelling: String, start: Int, end: Int) -> Unit,
    onToggleAllTranslations: () -> Unit,
    onOpenGrammarPoint: (String) -> Unit,
    onCloseWordPanel: () -> Unit,
    onToggleCard: () -> Unit,
    onSelectOption: (questionIndex: Int, optionIndex: Int) -> Unit,
    onSubmitQuiz: () -> Unit,
    onNextUnit: () -> Unit,
    onPreviousUnit: () -> Unit,
    onOpenUnit: (String) -> Unit,
    onStopSpeaking: () -> Unit,
    onPageChanged: (ReaderPhase, Int, Float, String?) -> Unit,
    onTextProgress: (Float) -> Unit,
    onToggleBookmark: (paragraphId: String, sentenceNumber: Int) -> Unit,
    onToggleHighlight: (paragraphId: String, sentenceNumber: Int) -> Unit,
    onSaveNote: (paragraphId: String, sentenceNumber: Int, note: String) -> Unit,
    onDeleteNote: (paragraphId: String, sentenceNumber: Int) -> Unit,
    onOpenAnnotation: (AnnotationUi) -> Unit,
    onJumpHandled: () -> Unit,
    onGuideStarted: () -> Unit,
    onStartPreview: () -> Unit,
    onPreviewAnswer: (Boolean) -> Unit,
    onPreviewNext: () -> Unit,
    onPreviewFinish: () -> Unit,
    onClosePreview: () -> Unit,
    onSpeechNoticeShown: () -> Unit,
    exitConfirm: Boolean,
    onDismissExit: () -> Unit,
    onConfirmExit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val guideText = guideText(state)
    val hasGuide = guideText != null
    val paged = state.preference.readerLayout != ReaderLayout.SCROLL
    val preview = state.preview
    var analysisSentence by remember { mutableStateOf<SentenceUi?>(null) }
    val openAnalysis: (SentenceUi) -> Unit = { analysisSentence = it }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showToc by rememberSaveable { mutableStateOf(false) }
    var progressSegments by remember { mutableStateOf<List<ProgressSegment>>(emptyList()) }
    var progressTargets by remember { mutableStateOf<Map<ProgressKind, Int>>(emptyMap()) }
    var pagerStateRef by remember { mutableStateOf<androidx.compose.foundation.pager.PagerState?>(null) }
    var expandedTranslation by remember(state.unitId) { mutableStateOf<ExpandedTranslation?>(null) }
    // Brief highlight after a list/search jump so the target sentence is findable.
    var focusSentence by remember(state.unitId) { mutableStateOf<SentenceFocus?>(null) }
    var textScrollProgress by remember(state.unitId) { mutableStateOf(state.resumeTextProgress) }
    var returnToTextAfterPreview by remember { mutableStateOf(false) }
    var forceTextTop by remember { mutableStateOf(false) }
    var textSession by remember { mutableIntStateOf(0) }
    var actionTarget by remember { mutableStateOf<SentenceActionTarget?>(null) }
    var noteTarget by remember { mutableStateOf<SentenceActionTarget?>(null) }
    var showAnnotations by rememberSaveable { mutableStateOf(false) }
    var pendingScrollParagraph by remember { mutableStateOf<String?>(null) }

    fun markOf(target: SentenceActionTarget?): SentenceMarkUi? = target?.let { wanted ->
        state.paragraphs
            .firstOrNull { it.id == wanted.paragraphId }
            ?.sentences
            ?.firstOrNull { it.number == wanted.sentenceNumber }
            ?.mark
    }

    LaunchedEffect(state.unitId) { textScrollProgress = state.resumeTextProgress }

    LaunchedEffect(state.speechNotice) {
        val notice = state.speechNotice ?: return@LaunchedEffect
        snackbar.showSnackbar(notice)
        onSpeechNoticeShown()
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            Column {
                ReaderTopBar(
                    strings = strings,
                    state = state,
                    onBack = {
                        if (preview != null) onClosePreview() else onBack()
                    },
                    onOpenCards = onOpenCards,
                    onOpenSettings = onOpenSettings,
                    onOpenSearch = onOpenSearch,
                    onOpenToc = { showToc = true },
                    onOpenAnnotations = { showAnnotations = true },
                    annotationCount = state.annotations.size,
                    onToggleTranslations = onToggleAllTranslations,
                )
                if (preview == null) {
                    ReaderProgressBar(
                        segments = progressSegments,
                        onSegmentClick = { kind ->
                            val target = progressTargets[kind] ?: return@ReaderProgressBar
                            pagerStateRef?.let { pager ->
                                scope.launch { pager.animateScrollToPage(target) }
                            }
                        },
                    )
                }
            }
        },
    ) { padding ->
        if (preview != null) {
            PreviewPageView(
                strings = strings,
                state = state,
                preview = preview,
                onAnswer = onPreviewAnswer,
                onNext = onPreviewNext,
                onFinish = {
                    returnToTextAfterPreview = true
                    forceTextTop = true
                    textScrollProgress = 0f
                    textSession += 1
                    onPreviewFinish()
                },
                onClose = onClosePreview,
                modifier = Modifier.fillMaxSize().padding(padding).navigationBarsPadding(),
            )
        } else {
            BoxWithConstraints(Modifier.fillMaxSize().padding(padding).navigationBarsPadding()) {
                val density = LocalDensity.current
                // The text column is min(screen, 760dp) minus the reader's
                // chosen page margins (T158); page-turn zones shrink with the
                // margin so they never cover a word.
                val pagePadding = state.preference.pageMargin.horizontalPadding()
                val textWidth = minOf(maxWidth, 760.dp) - pagePadding * 2
                val pageTurnZone = minOf(pagePadding, PageTurnZoneWidth)
                val contentWidthPx = with(density) { textWidth.roundToPx().coerceAtLeast(1) }
                val pageHeightPx = with(density) { (maxHeight - 4.dp).roundToPx().coerceAtLeast(1) }
                val textPages = if (paged) {
                    rememberTextPages(
                        state = state,
                        strings = strings,
                        contentWidthPx = contentWidthPx,
                        pageHeightPx = pageHeightPx,
                    )
                } else {
                    emptyList()
                }
                val pages = remember(
                    state.quizSubmitted,
                    state.quiz.size,
                    state.grammar,
                    state.hasPreviousUnit,
                    state.hasNextUnit,
                    textPages,
                    hasGuide,
                    paged,
                ) {
                    ReaderFlow.pages(state, textPages, hasGuide, paged)
                }

                if (pages.isEmpty()) {
                    Text(strings.emptyLibrary, modifier = Modifier.padding(24.dp))
                    return@BoxWithConstraints
                }

                // Page indices include the two cross-unit bridge pages, so the
                // guide, text and quiz offsets are looked up instead of assumed.
                val guideIndex = pages.indexOf(ReaderPage.Guide)
                val textStart = pages.indexOfFirst {
                    it is ReaderPage.Text || it == ReaderPage.TextScroll
                }
                val quizStart = pages.indexOfFirst { it is ReaderPage.Quiz }
                val grammarIndex = pages.indexOf(ReaderPage.Grammar)
                val initialPage = when (state.resumePhase) {
                    ReaderPhase.GUIDE -> if (hasGuide) guideIndex else textStart
                    ReaderPhase.TEXT -> if (paged) {
                        textStart + state.resumeTextPage.coerceIn(0, maxOf(0, textPages.lastIndex))
                    } else {
                        textStart
                    }
                    ReaderPhase.QUIZ -> quizStart
                    ReaderPhase.GRAMMAR -> grammarIndex.takeIf { it >= 0 } ?: quizStart
                }.coerceIn(0, pages.lastIndex)

                key(state.unitId, paged) {
                    val pagerState = rememberPagerState(initialPage = initialPage, pageCount = { pages.size })
                    SideEffect {
                        pagerStateRef = pagerState
                        progressTargets = buildMap {
                            if (hasGuide) put(ProgressKind.GUIDE, guideIndex)
                            val textTarget = if (paged) {
                                textStart + state.resumeTextPage.coerceIn(
                                    0,
                                    maxOf(0, textPages.lastIndex),
                                )
                            } else {
                                textStart
                            }
                            put(ProgressKind.TEXT, textTarget)
                            if (state.quiz.isNotEmpty()) put(ProgressKind.QUIZ, quizStart)
                            if (grammarIndex >= 0) put(ProgressKind.GRAMMAR, grammarIndex)
                        }
                    }
                    var lastReportedPage by remember { mutableIntStateOf(-1) }
                    var initialApplied by remember { mutableStateOf(false) }
                    var wasSubmitted by remember { mutableStateOf(state.quizSubmitted) }

                    fun goTo(page: Int) {
                        scope.launch {
                            pagerState.animateScrollToPage(page.coerceIn(0, pages.lastIndex))
                        }
                    }

                    fun positionOf(page: ReaderPage): PagePosition = when (page) {
                        ReaderPage.Guide -> PagePosition(ReaderPhase.GUIDE, 0, 0f)
                        is ReaderPage.Text -> {
                            val index = textPages.indexOf(page.page)
                            PagePosition(
                                ReaderPhase.TEXT,
                                index.coerceAtLeast(0),
                                if (textPages.isEmpty()) 0f else (index + 1f) / textPages.size,
                            )
                        }
                        ReaderPage.TextScroll -> PagePosition(ReaderPhase.TEXT, 0, textScrollProgress)
                        is ReaderPage.Quiz -> PagePosition(
                            ReaderPhase.QUIZ,
                            state.resumeTextPage.coerceIn(0, maxOf(0, textPages.lastIndex)),
                            1f,
                        )
                        ReaderPage.Grammar -> PagePosition(
                            ReaderPhase.GRAMMAR,
                            state.resumeTextPage.coerceIn(0, maxOf(0, textPages.lastIndex)),
                            1f,
                        )
                        // Bridge pages never report a position; they only load
                        // the adjacent unit once the pager settles on them.
                        ReaderPage.PreviousUnit -> PagePosition(ReaderPhase.GUIDE, 0, 0f)
                        ReaderPage.NextUnit -> PagePosition(ReaderPhase.GRAMMAR, 0, 1f)
                    }

                    fun segmentsFor(page: ReaderPage): List<ProgressSegment> {
                        val textProgress = when (page) {
                            ReaderPage.Guide -> 0f
                            is ReaderPage.Text -> {
                                val index = textPages.indexOf(page.page)
                                if (textPages.isEmpty()) 0f else (index + 1f) / textPages.size
                            }
                            ReaderPage.TextScroll -> textScrollProgress
                            else -> 1f
                        }
                        val answered = state.quiz.count { it.selected != null }
                        val quizProgress = when {
                            state.quiz.isEmpty() -> null
                            state.quizSubmitted -> 1f
                            else -> answered.toFloat() / state.quiz.size
                        }
                        return buildList {
                            if (hasGuide) {
                                add(
                                    ProgressSegment(
                                        ProgressKind.GUIDE,
                                        strings.progressGuide,
                                        if (page == ReaderPage.Guide) 0f else 1f,
                                        active = page == ReaderPage.Guide,
                                    ),
                                )
                            }
                            add(
                                ProgressSegment(
                                    ProgressKind.TEXT,
                                    strings.progressText,
                                    textProgress.coerceIn(0f, 1f),
                                    active = page is ReaderPage.Text || page == ReaderPage.TextScroll,
                                ),
                            )
                            quizProgress?.let {
                                add(
                                    ProgressSegment(
                                        ProgressKind.QUIZ,
                                        strings.progressQuiz,
                                        it.coerceIn(0f, 1f),
                                        active = page is ReaderPage.Quiz,
                                    ),
                                )
                            }
                            if (state.grammar != null) {
                                add(
                                    ProgressSegment(
                                        ProgressKind.GRAMMAR,
                                        strings.progressGrammar,
                                        if (page == ReaderPage.Grammar) 1f else 0f,
                                        active = page == ReaderPage.Grammar,
                                    ),
                                )
                            }
                        }
                    }

                    fun updateProgress(page: ReaderPage) {
                        progressSegments = segmentsFor(page)
                    }

                    LaunchedEffect(returnToTextAfterPreview) {
                        if (returnToTextAfterPreview && pages.isNotEmpty()) {
                            pagerState.scrollToPage(textStart)
                            returnToTextAfterPreview = false
                        }
                    }

                    LaunchedEffect(pages.size, initialPage) {
                        if (!initialApplied && pages.isNotEmpty()) {
                            pagerState.scrollToPage(initialPage)
                            lastReportedPage = initialPage
                            initialApplied = true
                            updateProgress(pages[initialPage])
                        }
                    }

                    // A list row asked to show one sentence. Paged layout lands on
                    // the page that contains it; scrolling mode first switches to
                    // the text page, then the scroll view aligns the paragraph.
                    LaunchedEffect(state.jumpTarget?.token, pages) {
                        val target = state.jumpTarget ?: return@LaunchedEffect
                        if (paged) {
                            val paragraphIndex = state.paragraphs.indexOfFirst {
                                it.id == target.paragraphId
                            }
                            val sentenceIndex = state.paragraphs.getOrNull(paragraphIndex)
                                ?.sentences
                                ?.indexOfFirst { it.number == target.sentenceNumber }
                                ?: -1
                            val pageIndex = if (paragraphIndex >= 0 && sentenceIndex >= 0) {
                                textPages.indexOfFirst { page ->
                                    page.chunks.any { chunk ->
                                        chunk.paragraphId == target.paragraphId &&
                                            sentenceIndex >= chunk.sentenceStart &&
                                            sentenceIndex < chunk.sentenceEndExclusive
                                    }
                                }
                            } else {
                                -1
                            }
                            if (pageIndex >= 0) {
                                pagerState.scrollToPage(textStart + pageIndex)
                                lastReportedPage = textStart + pageIndex
                                focusSentence = SentenceFocus(target.paragraphId, target.sentenceNumber)
                            }
                            onJumpHandled()
                        } else if (state.paragraphs.any { it.id == target.paragraphId }) {
                            pagerState.scrollToPage(textStart)
                            pendingScrollParagraph = target.paragraphId
                            focusSentence = SentenceFocus(target.paragraphId, target.sentenceNumber)
                        } else {
                            // The paragraph moved or disappeared; the list already
                            // showed the stored quote, so there is nothing to scroll to.
                            onJumpHandled()
                        }
                    }

                    LaunchedEffect(focusSentence) {
                        if (focusSentence == null) return@LaunchedEffect
                        delay(2600)
                        focusSentence = null
                    }

                    LaunchedEffect(pages.size) {
                        if (pagerState.currentPage > pages.lastIndex) {
                            pagerState.scrollToPage(pages.lastIndex)
                        }
                    }

                    // A bridge page only exists to carry the swipe past the unit
                    // edge. Load the adjacent unit once the pager has settled so
                    // a small drag that springs back never switches units.
                    var boundaryNavigating by remember { mutableStateOf(false) }
                    LaunchedEffect(pagerState, pages) {
                        snapshotFlow { pagerState.settledPage }.collect { settled ->
                            when (pages.getOrNull(settled)) {
                                ReaderPage.PreviousUnit -> if (!boundaryNavigating) {
                                    boundaryNavigating = true
                                    onPreviousUnit()
                                }
                                ReaderPage.NextUnit -> if (!boundaryNavigating) {
                                    boundaryNavigating = true
                                    onNextUnit()
                                }
                                else -> boundaryNavigating = false
                            }
                        }
                    }

                    LaunchedEffect(pagerState.currentPage, pages.size) {
                        val page = pages.getOrNull(pagerState.currentPage) ?: return@LaunchedEffect
                        if (page == ReaderPage.PreviousUnit || page == ReaderPage.NextUnit) {
                            return@LaunchedEffect
                        }
                        val position = positionOf(page)
                        updateProgress(page)
                        if (pagerState.currentPage != lastReportedPage) {
                            lastReportedPage = pagerState.currentPage
                            expandedTranslation = null
                            if (page != ReaderPage.Guide) onGuideStarted()
                            onStopSpeaking()
                            val anchorParagraphId = when (page) {
                                is ReaderPage.Text -> page.page.anchorParagraphId
                                ReaderPage.TextScroll -> state.paragraphs.firstOrNull()?.id
                                else -> null
                            }
                            onPageChanged(
                                position.phase,
                                position.textPage,
                                position.textProgress,
                                anchorParagraphId,
                            )
                        }
                    }

                    LaunchedEffect(state.quiz, state.quizSubmitted, textScrollProgress, pagerState.currentPage) {
                        pages.getOrNull(pagerState.currentPage)
                            ?.takeUnless { it == ReaderPage.PreviousUnit || it == ReaderPage.NextUnit }
                            ?.let(::updateProgress)
                    }

                    LaunchedEffect(state.quizSubmitted) {
                        val grammarIndex = pages.indexOf(ReaderPage.Grammar)
                        val firstQuizIndex = pages.indexOfFirst { it is ReaderPage.Quiz }
                        if (state.quizSubmitted && !wasSubmitted) {
                            // No summary page: the last answer goes straight to
                            // this unit's grammar, or to the next unit's guide.
                            if (grammarIndex >= 0) {
                                pagerState.animateScrollToPage(grammarIndex)
                            } else {
                                onNextUnit()
                            }
                        } else if (!state.quizSubmitted && wasSubmitted && firstQuizIndex >= 0) {
                            pagerState.animateScrollToPage(firstQuizIndex)
                        }
                        wasSubmitted = state.quizSubmitted
                    }

                    Box(Modifier.fillMaxSize()) {
                        HorizontalPager(
                            state = pagerState,
                            userScrollEnabled = paged,
                            modifier = Modifier.fillMaxSize(),
                        ) { index ->
                            when (val page = pages[index]) {
                                ReaderPage.PreviousUnit -> UnitBridgePage(
                                    label = strings.previousUnitGrammar,
                                )

                                ReaderPage.NextUnit -> UnitBridgePage(
                                    label = strings.nextUnitGuide,
                                )

                                ReaderPage.Guide -> GuidePageView(
                                    strings = strings,
                                    state = state,
                                    copy = guideText ?: GuideCopy(null, null),
                                    onStart = {
                                        onGuideStarted()
                                        forceTextTop = true
                                        textScrollProgress = 0f
                                        onPageChanged(ReaderPhase.TEXT, 0, 0f, state.paragraphs.firstOrNull()?.id)
                                        textSession += 1
                                        goTo(textStart)
                                    },
                                    onPreview = onStartPreview,
                                )

                                is ReaderPage.Text -> TextPageView(
                                    strings = strings,
                                    state = state,
                                    page = page.page,
                                    pageIndex = textPages.indexOf(page.page) + 1,
                                    pageCount = textPages.size,
                                    hasNextPage = index < pages.lastIndex,
                                    onWordClick = onWordClick,
                                    onAnalysis = openAnalysis,
                                    onNextPage = { goTo(index + 1) },
                                    onNextUnit = onNextUnit,
                                    expandedTranslation = expandedTranslation,
                                    focus = focusSentence,
                                    onTranslate = { paragraphId, sentence ->
                                        expandedTranslation = toggleTranslation(
                                            current = expandedTranslation,
                                            paragraphId = paragraphId,
                                            sentence = sentence,
                                        )
                                    },
                                    onSentenceLongPress = { paragraphId, sentence ->
                                        actionTarget = SentenceActionTarget(
                                            paragraphId,
                                            sentence.number,
                                            sentence.text,
                                        )
                                    },
                                )

                                ReaderPage.TextScroll -> key(textSession) {
                                    TextScrollView(
                                        strings = strings,
                                        state = state,
                                        startAtTop = forceTextTop,
                                        expandedTranslation = expandedTranslation,
                                        focus = focusSentence,
                                        enterLabel = when {
                                            state.quiz.isNotEmpty() -> strings.enterQuiz
                                            state.grammar != null -> strings.continueReading
                                            state.hasNextUnit -> strings.nextUnit
                                            else -> strings.finishBook
                                        },
                                        onWordClick = onWordClick,
                                        onAnalysis = openAnalysis,
                                        onTranslate = { paragraphId, sentence ->
                                            expandedTranslation = toggleTranslation(
                                                current = expandedTranslation,
                                                paragraphId = paragraphId,
                                                sentence = sentence,
                                            )
                                        },
                                        onSentenceLongPress = { paragraphId, sentence ->
                                            actionTarget = SentenceActionTarget(
                                                paragraphId,
                                                sentence.number,
                                                sentence.text,
                                            )
                                        },
                                        scrollToParagraphId = pendingScrollParagraph,
                                        onScrollHandled = {
                                            pendingScrollParagraph = null
                                            onJumpHandled()
                                        },
                                        onEnterQuiz = {
                                            if (state.quiz.isEmpty() && state.grammar == null) {
                                                onNextUnit()
                                            } else {
                                                goTo(quizStart)
                                            }
                                        },
                                        onProgress = {
                                            textScrollProgress = it
                                            onTextProgress(it)
                                        },
                                    )
                                }

                                is ReaderPage.Quiz -> QuizPageView(
                                    strings = strings,
                                    question = state.quiz[page.index],
                                    index = page.index,
                                    total = state.quiz.size,
                                    allAnswered = state.quiz.all { it.selected != null },
                                    unansweredCount = state.quiz.count { it.selected == null },
                                    onSelect = { option -> onSelectOption(page.index, option) },
                                    onNext = {
                                        if (page.index == state.quiz.lastIndex) {
                                            onSubmitQuiz()
                                        } else {
                                            goTo(index + 1)
                                        }
                                    },
                                )

                                ReaderPage.Grammar -> Column(
                                    Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                                        .padding(horizontal = 20.dp, vertical = 10.dp),
                                ) {
                                    state.grammar?.let {
                                        GrammarCard(strings, it, onOpenPoint = onOpenGrammarPoint)
                                    }
                                    Spacer(Modifier.height(18.dp))
                                    Button(
                                        onClick = onNextUnit,
                                        modifier = Modifier.fillMaxWidth(),
                                    ) {
                                        Text(if (state.hasNextUnit) strings.nextUnit else strings.finishBook)
                                    }
                                    Spacer(Modifier.height(24.dp))
                                }
                            }
                        }

                        if (paged) {
                            val currentPage = pages.getOrNull(pagerState.currentPage)
                            if (pagerState.currentPage > 0) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.CenterStart)
                                        .fillMaxHeight()
                                        .width(pageTurnZone)
                                        .semantics { contentDescription = strings.previousPage }
                                        .clickable(
                                            onClickLabel = strings.previousPage,
                                            onClick = { goTo(pagerState.currentPage - 1) },
                                        ),
                                )
                            }
                            if (pagerState.currentPage < pages.lastIndex) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.CenterEnd)
                                        .fillMaxHeight()
                                        .width(pageTurnZone)
                                        .semantics { contentDescription = strings.nextPage }
                                        .clickable(
                                            onClickLabel = strings.nextPage,
                                            onClick = { goTo(pagerState.currentPage + 1) },
                                        ),
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (exitConfirm) {
        AlertDialog(
            onDismissRequest = onDismissExit,
            title = { Text(strings.quizExitTitle) },
            text = { Text(strings.quizExitBody) },
            confirmButton = {
                TextButton(onClick = onConfirmExit) { Text(strings.quizExitConfirm) }
            },
            dismissButton = {
                TextButton(onClick = onDismissExit) { Text(strings.quizStay) }
            },
        )
    }

    if (showToc) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showToc = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            dragHandle = { BottomSheetDefaults.DragHandle() },
        ) {
            TocSheet(
                strings = strings,
                toc = state.toc,
                onOpen = { unitId ->
                    showToc = false
                    onOpenUnit(unitId)
                },
            )
        }
    }

    if (showAnnotations) {
        AnnotationSheet(
            strings = strings,
            bookTitle = state.bookTitle,
            annotations = state.annotations,
            onOpen = { annotation ->
                showAnnotations = false
                onOpenAnnotation(annotation)
            },
            onDismiss = { showAnnotations = false },
        )
    }

    val activeTarget = actionTarget
    if (activeTarget != null) {
        SentenceActionSheet(
            strings = strings,
            target = activeTarget,
            mark = markOf(activeTarget),
            onToggleBookmark = { onToggleBookmark(activeTarget.paragraphId, activeTarget.sentenceNumber) },
            onToggleHighlight = { onToggleHighlight(activeTarget.paragraphId, activeTarget.sentenceNumber) },
            onEditNote = { noteTarget = activeTarget },
            onDeleteNote = {
                onDeleteNote(activeTarget.paragraphId, activeTarget.sentenceNumber)
            },
            onDismiss = { actionTarget = null },
        )
    }

    val editingTarget = noteTarget
    if (editingTarget != null) {
        NoteEditorDialog(
            strings = strings,
            initial = markOf(editingTarget)?.note.orEmpty(),
            onSave = { note ->
                onSaveNote(editingTarget.paragraphId, editingTarget.sentenceNumber, note)
                noteTarget = null
                actionTarget = null
            },
            onDismiss = { noteTarget = null },
        )
    }

    state.wordPanel?.let { panel ->
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = onCloseWordPanel,
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            dragHandle = { BottomSheetDefaults.DragHandle() },
        ) {
            WordPanel(strings, panel, state.preference.definitionMode, onToggleCard, onCloseWordPanel)
        }
    }

    analysisSentence?.let { sentence ->
        sentence.analysis?.let { analysis ->
            AnalysisSheet(strings, analysis, onClose = { analysisSentence = null })
        }
    }
}
