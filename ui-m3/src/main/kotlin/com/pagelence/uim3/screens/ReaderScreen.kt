package com.pagelence.uim3.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pagelence.core.prefs.DefinitionMode
import com.pagelence.core.prefs.FontSize
import com.pagelence.core.prefs.GuideMode
import com.pagelence.core.prefs.ReaderLayout
import com.pagelence.core.progress.ReaderPhase
import com.pagelence.core.reading.MeasuredSentence
import com.pagelence.core.reading.SentencePage
import com.pagelence.core.reading.SentencePaginator
import com.pagelence.core.reading.TextPageChunk
import com.pagelence.uim3.Strings
import com.pagelence.uim3.components.AnalysisIcon
import com.pagelence.uim3.components.GrammarCard
import com.pagelence.uim3.components.MetaChip
import com.pagelence.uim3.components.TranslateIcon
import com.pagelence.uim3.components.SectionLabel
import com.pagelence.uim3.model.AnalysisPartUi
import com.pagelence.uim3.model.AnalysisUi
import com.pagelence.uim3.model.GrammarUi
import com.pagelence.uim3.model.ParagraphUi
import com.pagelence.uim3.model.QuizQuestionUi
import com.pagelence.uim3.model.ReaderPreviewUi
import com.pagelence.uim3.model.ReaderUiState
import com.pagelence.uim3.model.SelectedWordUi
import com.pagelence.uim3.model.SenseUi
import com.pagelence.uim3.model.SentenceUi
import com.pagelence.uim3.model.TocChapterUi
import com.pagelence.uim3.model.TocUnitUi
import com.pagelence.uim3.model.WordPanelUi
import com.pagelence.uim3.theme.readingTextStyle
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

private val SentenceTranslateIconSize = 14.dp
private val SentenceAnalysisIconSize = 14.dp

/**
 * Page-turn tap zones live in the 22dp text margin only. A 48dp zone would
 * cover the last words of every line and steal their taps, so the zone matches
 * the margin; full height keeps it easy to hit.
 */
private val PageTurnZoneWidth = 22.dp

/**
 * Extra hit area around the 14dp inline icons. The placeholder stays 14dp so
 * pagination does not move; taps land in this slop instead of on a word.
 */
private val SentenceIconSlop = 14.dp

/**
 * Inline glyphs stay 14dp, but their placeholder is 28dp so the tap target is
 * bigger than the glyph. A 48dp inline box would change line height and
 * pagination; TalkBack reaches the same action through semantics.
 */
private val SentenceIconTouchSize = 24.dp
private const val SentenceTranslateInlineId = "sentence-translate"
private const val SentenceAnalysisInlineId = "sentence-analysis"

/** One page in the reading sequence: guide → text → quiz → summary → grammar → review. */
private enum class ProgressKind { GUIDE, TEXT, QUIZ, GRAMMAR }

private data class ProgressSegment(
    val kind: ProgressKind,
    val label: String,
    val value: Float,
    val active: Boolean,
)

private sealed interface ReaderPage {
    /** Transient page before the guide; swiping back opens the previous unit's grammar. */
    data object PreviousUnit : ReaderPage
    data object Guide : ReaderPage
    data class Text(val page: SentencePage) : ReaderPage
    data object TextScroll : ReaderPage
    data class Quiz(val index: Int) : ReaderPage
    data object Grammar : ReaderPage
    /** Transient page after the grammar; swiping on opens the next unit's guide. */
    data object NextUnit : ReaderPage
}

private data class PagePosition(
    val phase: ReaderPhase,
    val textPage: Int,
    val textProgress: Float,
)

private data class ExpandedTranslation(
    val paragraphId: String,
    val sentenceNumber: Int,
    val sentence: String,
    val zh: String,
)

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
    onWordClick: (paragraphId: String, sentenceNumber: Int, spelling: String, start: Int, end: Int) -> Unit,
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
    var textScrollProgress by remember(state.unitId) { mutableStateOf(state.resumeTextProgress) }
    var returnToTextAfterPreview by remember { mutableStateOf(false) }
    var forceTextTop by remember { mutableStateOf(false) }
    var textSession by remember { mutableIntStateOf(0) }

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
                    onOpenToc = { showToc = true },
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
                // The text column is min(screen, 760dp) minus its 22dp side padding.
                val textWidth = minOf(maxWidth, 760.dp) - 44.dp
                val contentWidthPx = with(density) { textWidth.roundToPx().coerceAtLeast(1) }
                val pageHeightPx = with(density) { (maxHeight - 4.dp).roundToPx().coerceAtLeast(1) }
                val textPages = if (paged) {
                    rememberTextPages(
                        state = state,
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
                    buildPages(state, textPages, hasGuide, paged)
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
                                    onTranslate = { paragraphId, sentence ->
                                        expandedTranslation = toggleTranslation(
                                            current = expandedTranslation,
                                            paragraphId = paragraphId,
                                            sentence = sentence,
                                        )
                                    },
                                )

                                ReaderPage.TextScroll -> key(textSession) {
                                    TextScrollView(
                                        strings = strings,
                                        state = state,
                                        startAtTop = forceTextTop,
                                        expandedTranslation = expandedTranslation,
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
                                        .width(PageTurnZoneWidth)
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
                                        .width(PageTurnZoneWidth)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AnalysisSheet(strings: Strings, analysis: AnalysisUi, onClose: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onClose,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() },
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp, vertical = 8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    strings.analysisTitle,
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.weight(1f),
                )
                MetaChip(analysis.level)
            }
            Spacer(Modifier.height(14.dp))
            SectionLabel(strings.analysisCore)
            Spacer(Modifier.height(6.dp))
            Text(analysis.core, style = MaterialTheme.typography.bodyLarge)
            if (analysis.parts.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                SectionLabel(strings.analysisParts)
                analysis.parts.forEach { part ->
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.Top) {
                        MetaChip(analysisRole(strings, part.role))
                        Spacer(Modifier.width(8.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                part.text,
                                style = MaterialTheme.typography.bodyMedium,
                                fontStyle = FontStyle.Italic,
                            )
                            part.description?.takeIf { it.isNotBlank() }?.let {
                                Spacer(Modifier.height(4.dp))
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
            analysis.order?.takeIf { it.isNotBlank() }?.let {
                Spacer(Modifier.height(16.dp))
                SectionLabel(strings.analysisOrder)
                Spacer(Modifier.height(6.dp))
                Text(it, style = MaterialTheme.typography.bodyLarge)
            }
            analysis.note?.takeIf { it.isNotBlank() }?.let {
                Spacer(Modifier.height(16.dp))
                SectionLabel(strings.analysisNote)
                Spacer(Modifier.height(6.dp))
                Text(it, style = MaterialTheme.typography.bodyLarge)
            }
            Spacer(Modifier.height(18.dp))
            TextButton(onClick = onClose) { Text(strings.analysisClose) }
            Spacer(Modifier.height(20.dp))
        }
    }
}

private fun analysisRole(strings: Strings, role: String): String = when (role) {
    "subordinate" -> strings.analysisRoleSubordinate
    "relative" -> strings.analysisRoleRelative
    "nonfinite" -> strings.analysisRoleNonfinite
    "prepositional" -> strings.analysisRolePrepositional
    "appositive" -> strings.analysisRoleAppositive
    "parenthetical" -> strings.analysisRoleParenthetical
    else -> role
}

/** The 导读 text for the current mode: both languages, English only, or none. */
private data class GuideCopy(val zh: String?, val en: String?) {
    val isEmpty: Boolean get() = zh.isNullOrBlank() && en.isNullOrBlank()
}

private fun guideText(state: ReaderUiState): GuideCopy? = when (state.preference.guideMode) {
    GuideMode.BILINGUAL -> GuideCopy(state.introductionZh, state.introductionEn)
    GuideMode.EN -> GuideCopy(null, state.introductionEn ?: state.introductionZh)
    GuideMode.IMMERSIVE -> null
}?.takeUnless { it.isEmpty }

private fun buildPages(
    state: ReaderUiState,
    textPages: List<SentencePage>,
    hasGuide: Boolean,
    paged: Boolean,
): List<ReaderPage> = buildList {
    // Paged layout gets one bridge page at each open edge: before the guide
    // when a previous unit exists, after the grammar when a next unit exists.
    if (paged && state.hasPreviousUnit) add(ReaderPage.PreviousUnit)
    if (hasGuide) add(ReaderPage.Guide)
    if (paged) {
        textPages.forEach { add(ReaderPage.Text(it)) }
    } else {
        add(ReaderPage.TextScroll)
    }
    state.quiz.indices.forEach { add(ReaderPage.Quiz(it)) }
    if (state.grammar != null) add(ReaderPage.Grammar)
    if (paged && state.hasNextUnit) add(ReaderPage.NextUnit)
}

@Composable
private fun rememberTextPages(
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

private fun measureSentences(
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
private fun measuredSentence(
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReaderTopBar(
    strings: Strings,
    state: ReaderUiState,
    onBack: () -> Unit,
    onOpenCards: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenToc: () -> Unit,
) {
    TopAppBar(
        title = {
            Column {
                Text(state.bookTitle, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    "${state.chapterTitle} · ${state.unitPosition}/${state.unitCount}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = strings.back)
            }
        },
        actions = {
            IconButton(onClick = onOpenToc) {
                Icon(
                    Icons.AutoMirrored.Filled.List,
                    contentDescription = strings.toc,
                )
            }
            MoreMenu(state.cardsCount, strings, onOpenCards, onOpenSettings)
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
    )
}

@Composable
private fun MoreMenu(
    cardsCount: Int,
    strings: Strings,
    onOpenCards: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(Icons.Filled.MoreVert, contentDescription = strings.more)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text("${strings.wordCards} ($cardsCount)") },
                leadingIcon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = null) },
                onClick = {
                    expanded = false
                    onOpenCards()
                },
            )
            DropdownMenuItem(
                text = { Text(strings.settings) },
                leadingIcon = { Icon(Icons.Filled.Settings, contentDescription = null) },
                onClick = {
                    expanded = false
                    onOpenSettings()
                },
            )
        }
    }
}

@Composable
private fun ReaderProgressBar(
    segments: List<ProgressSegment>,
    onSegmentClick: (ProgressKind) -> Unit,
) {
    if (segments.isEmpty()) return
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 6.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            segments.forEach { segment ->
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(9.dp))
                        .background(
                            if (segment.active) {
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                            } else {
                                androidx.compose.ui.graphics.Color.Transparent
                            },
                        )
                        .clickable(
                            role = Role.Button,
                            onClickLabel = segment.label,
                            onClick = { onSegmentClick(segment.kind) },
                        )
                        .padding(horizontal = 6.dp, vertical = 7.dp),
                ) {
                    Text(
                        segment.label,
                        style = MaterialTheme.typography.labelMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                        color = if (segment.active) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                    Spacer(Modifier.height(5.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(MaterialTheme.colorScheme.outlineVariant),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(segment.value.coerceIn(0f, 1f))
                                .fillMaxHeight()
                                .background(MaterialTheme.colorScheme.primary),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PageIndicator(
    strings: Strings,
    page: Int,
    total: Int,
    showNext: Boolean,
    nextLabel: String,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            shape = RoundedCornerShape(50),
            color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.94f),
            shadowElevation = 1.dp,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    strings.textProgressLabel.format(page, total),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (showNext) {
                    TextButton(
                        onClick = onNext,
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                    ) {
                        Text(nextLabel, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}

@Composable
private fun PreviewPageView(
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
private fun UnitBridgePage(label: String) {
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
private fun GuidePageView(
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
private fun TextPageView(
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
    onTranslate: (String, SentenceUi) -> Unit,
) {
    val scroll = rememberScrollState()
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
                    .padding(horizontal = 22.dp, vertical = 10.dp),
            ) {
                page.chunks.forEach { chunk ->
                    ParagraphChunkView(
                        strings = strings,
                        state = state,
                        chunk = chunk,
                        expandedTranslation = expandedTranslation,
                        onWordClick = onWordClick,
                        onAnalysis = onAnalysis,
                        onTranslate = onTranslate,
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
private fun ParagraphChunkView(
    strings: Strings,
    state: ReaderUiState,
    chunk: TextPageChunk,
    expandedTranslation: ExpandedTranslation?,
    onWordClick: (String, Int, String, Int, Int) -> Unit,
    onAnalysis: (SentenceUi) -> Unit,
    onTranslate: (String, SentenceUi) -> Unit,
) {
    val paragraph = state.paragraphs.firstOrNull { it.id == chunk.paragraphId } ?: return
    if (chunk.sentenceStart == 0) Spacer(Modifier.height(22.dp))
    val end = chunk.sentenceEndExclusive.coerceAtMost(paragraph.sentences.size)
    if (chunk.sentenceStart >= end) return
    val chunkSentences = paragraph.sentences.subList(chunk.sentenceStart, end)
    val expandedIndex = expandedTranslation
        ?.takeIf { it.paragraphId == paragraph.id }
        ?.let { expanded -> chunkSentences.indexOfFirst { it.number == expanded.sentenceNumber } }
        ?: -1
    if (expandedIndex < 0) {
        ParagraphText(
            strings = strings,
            paragraphId = paragraph.id,
            sentences = chunkSentences,
            fontSize = state.preference.fontSize,
            annotateHints = state.preference.annotateHints,
            selectedWord = state.selectedWord,
            onWordClick = onWordClick,
            onAnalysis = onAnalysis,
            onTranslate = onTranslate,
        )
    } else {
        ParagraphText(
            strings = strings,
            paragraphId = paragraph.id,
            sentences = chunkSentences.subList(0, expandedIndex + 1),
            fontSize = state.preference.fontSize,
            annotateHints = state.preference.annotateHints,
            selectedWord = state.selectedWord,
            onWordClick = onWordClick,
            onAnalysis = onAnalysis,
            onTranslate = onTranslate,
        )
        TranslationExpansion(expandedTranslation?.zh.orEmpty())
        if (expandedIndex + 1 < chunkSentences.size) {
            ParagraphText(
                strings = strings,
                paragraphId = paragraph.id,
                sentences = chunkSentences.subList(expandedIndex + 1, chunkSentences.size),
                fontSize = state.preference.fontSize,
                annotateHints = state.preference.annotateHints,
                selectedWord = state.selectedWord,
                onWordClick = onWordClick,
                onAnalysis = onAnalysis,
                onTranslate = onTranslate,
            )
        }
    }
}

@Composable
private fun TextScrollView(
    strings: Strings,
    state: ReaderUiState,
    startAtTop: Boolean,
    expandedTranslation: ExpandedTranslation?,
    enterLabel: String,
    onWordClick: (String, Int, String, Int, Int) -> Unit,
    onAnalysis: (SentenceUi) -> Unit,
    onTranslate: (String, SentenceUi) -> Unit,
    onEnterQuiz: () -> Unit,
    onProgress: (Float) -> Unit,
) {
    val scroll = rememberScrollState()
    var restored by remember { mutableStateOf(false) }
    var hasScrolled by remember { mutableStateOf(false) }

    LaunchedEffect(scroll.maxValue, startAtTop) {
        if (!restored && scroll.maxValue > 0) {
            val target = if (startAtTop) 0f else state.resumeTextProgress.coerceIn(0f, 1f)
            scroll.scrollTo((scroll.maxValue * target).toInt())
            restored = true
        }
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
                .padding(horizontal = 22.dp, vertical = 10.dp),
        ) {
            state.paragraphs.forEach { paragraph ->
            ParagraphBlock(
                strings = strings,
                paragraph = paragraph,
                fontSize = state.preference.fontSize,
                annotateHints = state.preference.annotateHints,
                selectedWord = state.selectedWord,
                expandedTranslation = expandedTranslation,
                onWordClick = onWordClick,
                onAnalysis = onAnalysis,
                onTranslate = onTranslate,
            )
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
private fun QuizPageView(
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
            if (question.sentenceText != null) {
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
                                question.sentenceText,
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

@Composable
private fun ParagraphBlock(
    strings: Strings,
    paragraph: ParagraphUi,
    fontSize: FontSize,
    annotateHints: Boolean,
    selectedWord: SelectedWordUi?,
    expandedTranslation: ExpandedTranslation?,
    onWordClick: (String, Int, String, Int, Int) -> Unit,
    onAnalysis: (SentenceUi) -> Unit,
    onTranslate: (String, SentenceUi) -> Unit,
) {
    Column {
        val expandedIndex = expandedTranslation
            ?.takeIf { it.paragraphId == paragraph.id }
            ?.let { expanded -> paragraph.sentences.indexOfFirst { it.number == expanded.sentenceNumber } }
            ?: -1
        if (expandedIndex < 0) {
            ParagraphText(
                strings = strings,
                paragraphId = paragraph.id,
                sentences = paragraph.sentences,
                fontSize = fontSize,
                annotateHints = annotateHints,
                selectedWord = selectedWord,
                onWordClick = onWordClick,
                onAnalysis = onAnalysis,
                onTranslate = onTranslate,
            )
        } else {
            ParagraphText(
                strings = strings,
                paragraphId = paragraph.id,
                sentences = paragraph.sentences.subList(0, expandedIndex + 1),
                fontSize = fontSize,
                annotateHints = annotateHints,
                selectedWord = selectedWord,
                onWordClick = onWordClick,
                onAnalysis = onAnalysis,
                onTranslate = onTranslate,
            )
            TranslationExpansion(expandedTranslation?.zh.orEmpty())
            if (expandedIndex + 1 < paragraph.sentences.size) {
                ParagraphText(
                    strings = strings,
                    paragraphId = paragraph.id,
                    sentences = paragraph.sentences.subList(expandedIndex + 1, paragraph.sentences.size),
                    fontSize = fontSize,
                    annotateHints = annotateHints,
                    selectedWord = selectedWord,
                    onWordClick = onWordClick,
                    onAnalysis = onAnalysis,
                    onTranslate = onTranslate,
                )
            }
        }
    }
}

@Composable
private fun TranslationExpansion(zh: String) {
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

private data class SentenceSpan(
    val sentence: SentenceUi,
    val start: Int,
    val end: Int,
)

private data class ParagraphTextData(
    val annotated: AnnotatedString,
    val spans: List<SentenceSpan>,
    val iconStarts: Map<Int, SentenceUi>,
    val analysisStarts: Map<Int, SentenceUi>,
    val selectionRanges: List<IntRange>,
    val hintRanges: List<IntRange>,
)

/**
 * One paragraph (or a page chunk of it) as a single text flow. Sentences are
 * joined with spaces, translation icons are inline footnote markers, and long
 * sentences are never split into separate Text blocks.
 */
@Composable
private fun ParagraphText(
    strings: Strings,
    paragraphId: String,
    sentences: List<SentenceUi>,
    fontSize: FontSize,
    annotateHints: Boolean,
    selectedWord: SelectedWordUi?,
    onWordClick: (String, Int, String, Int, Int) -> Unit,
    onAnalysis: (SentenceUi) -> Unit,
    onTranslate: (String, SentenceUi) -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = MaterialTheme.colorScheme.primary
    val selectionColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.66f)
    val selectionText = MaterialTheme.colorScheme.onPrimaryContainer
    val hintColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
    val data = remember(
        sentences,
        annotateHints,
        strings.translation,
        selectedWord?.paragraphId,
        selectedWord?.sentenceNumber,
        selectedWord?.spelling,
        selectedWord?.start,
        selectedWord?.end,
    ) {
        buildParagraphTextData(
            strings = strings,
            sentences = sentences,
            annotateHints = annotateHints,
            selectedWord = selectedWord,
            selectionText = selectionText,
        )
    }
    val density = LocalDensity.current
    val iconSizePx = with(density) { SentenceTranslateIconSize.toPx() }
    val iconSize = with(density) { iconSizePx.toSp() }
    val analysisSize = with(density) { SentenceAnalysisIconSize.toPx().toSp() }
    val inlineContent = buildMap {
        sentences.filter { !it.zh.isNullOrBlank() }.forEach { sentence ->
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
        sentences.filter { it.analysis != null }.forEach { sentence ->
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
        style = readingTextStyle(fontSize),
        onTextLayout = { layout = it },
        modifier = modifier
            .drawBehind {
                val result = layout ?: return@drawBehind
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
            }
            .pointerInput(data.annotated, data.spans, data.iconStarts, data.analysisStarts) {
                detectTapGestures { offset ->
                    val result = layout ?: return@detectTapGestures
                    val index = result.getOffsetForPosition(offset)
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
                }
            },
    )
}

private fun translateId(sentenceNumber: Int): String = "translate-$sentenceNumber"

private fun analysisId(sentenceNumber: Int): String = "analysis-$sentenceNumber"

private fun buildParagraphTextData(
    strings: Strings,
    sentences: List<SentenceUi>,
    annotateHints: Boolean,
    selectedWord: SelectedWordUi?,
    selectionText: androidx.compose.ui.graphics.Color,
): ParagraphTextData {
    val spans = mutableListOf<SentenceSpan>()
    val iconStarts = mutableMapOf<Int, SentenceUi>()
    val analysisStarts = mutableMapOf<Int, SentenceUi>()
    val builder = AnnotatedString.Builder()
    sentences.forEachIndexed { index, sentence ->
        if (index > 0) builder.append(" ")
        val start = builder.length
        builder.append(sentence.text)
        val end = builder.length
        spans += SentenceSpan(sentence, start, end)
        if (!sentence.zh.isNullOrBlank()) {
            builder.append(" ")
            val iconAt = builder.length
            builder.appendInlineContent(translateId(sentence.number), strings.translation)
            iconStarts[iconAt] = sentence
        }
        if (sentence.analysis != null) {
            builder.append(" ")
            val analysisAt = builder.length
            builder.appendInlineContent(analysisId(sentence.number), strings.analysisParse)
            analysisStarts[analysisAt] = sentence
        }
    }
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
    return ParagraphTextData(
        annotated = builder.toAnnotatedString(),
        spans = spans,
        iconStarts = iconStarts,
        analysisStarts = analysisStarts,
        selectionRanges = selectionRanges,
        hintRanges = hintRanges,
    )
}

private fun toggleTranslation(
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

private fun DrawScope.drawSelectionBackground(
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

private fun DrawScope.drawHintUnderline(
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

private inline fun forEachRangeLine(
    layout: TextLayoutResult,
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

private fun rangesOf(text: String, word: String): List<IntRange> {
    if (word.isBlank()) return emptyList()
    val pattern = Regex(
        "(?<![\\p{L}])" + Regex.escape(word) + "(?![\\p{L}])",
        RegexOption.IGNORE_CASE,
    )
    return pattern.findAll(text).map { it.range }.toList()
}

private data class WordHit(val text: String, val start: Int, val end: Int)

private fun wordAt(text: String, index: Int): WordHit? {
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

@Composable
private fun Cardish(title: String, content: @Composable () -> Unit) {
    ElevatedCard(shape = MaterialTheme.shapes.large, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(10.dp))
            content()
        }
    }
}

@Composable
private fun WordPanel(
    strings: Strings,
    panel: WordPanelUi,
    definitionMode: DefinitionMode,
    onToggleCard: () -> Unit,
    onClose: () -> Unit,
) {
    val zhBase = panel.senses.filter { it.language == "zh" && !it.fromSurface }.take(6)
    val zhExtra = panel.senses.filter { it.language == "zh" && it.fromSurface }.take(4)
    val enBase = panel.senses.filter { it.language == "en" && !it.fromSurface }.take(6)
    val enExtra = panel.senses.filter { it.language == "en" && it.fromSurface }.take(4)
    val showZh = definitionMode != DefinitionMode.EN
    val showEn = definitionMode != DefinitionMode.ZH
    val hasZh = zhBase.isNotEmpty() || zhExtra.isNotEmpty()
    val hasEn = enBase.isNotEmpty() || enExtra.isNotEmpty()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(start = 24.dp, end = 24.dp, bottom = 24.dp),
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(panel.term, style = MaterialTheme.typography.headlineMedium)
            panel.ipa?.takeIf { it.isNotBlank() }?.let {
                Spacer(Modifier.width(10.dp))
                Text(
                    "/$it/",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (panel.lemma.isNotBlank() && !panel.lemma.equals(panel.term, ignoreCase = true)) {
            Text(
                "${strings.lemmaBase} ${panel.lemma}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        panel.note?.takeIf { it.isNotBlank() }?.let {
            Text(
                it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            panel.pos?.takeIf { it.isNotBlank() }?.let { MetaChip(it) }
            panel.level?.let { MetaChip(if (panel.levelApprox) "≈$it" else it) }
        }
        // The word panel shows only the CEFR level and book occurrences; the
        // wordfreq band (高频/少见 …) is deliberately left out.
        val meta = buildList {
            if (panel.levelApprox) panel.levelSource?.let { add(levelSourceLabel(strings, it)) }
            panel.occurrences?.let { add("${strings.occurrences} $it") }
        }
        if (meta.isNotEmpty()) {
            Spacer(Modifier.height(6.dp))
            Text(
                meta.joinToString(" · "),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        panel.gloss?.takeIf { it.isNotBlank() }?.let { gloss ->
            Spacer(Modifier.height(14.dp))
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                    Text(
                        strings.sentenceMeaning,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f),
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        gloss,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                }
            }
        }
        // A word can have a Chinese sense but no English one (or the reverse).
        // If the selected meaning language has nothing, fall back to the other
        // language instead of rendering an apparently empty panel.
        val showZhSenses = showZh && hasZh
        val showEnSenses = showEn && hasEn
        if (showZhSenses) {
            SectionLabel(strings.definitionsZh)
            ZhSenseList(zhBase)
            if (zhExtra.isNotEmpty()) {
                OtherFormLabel(strings.otherFormSenses)
                ZhSenseList(zhExtra, muted = true)
            }
        }
        if (showEnSenses) {
            SectionLabel(strings.definitionsEn)
            EnSenseList(enBase)
            if (enExtra.isNotEmpty()) {
                OtherFormLabel(strings.otherFormSenses)
                EnSenseList(enExtra)
            }
        }
        if (!showZhSenses && !showEnSenses && panel.senses.isNotEmpty()) {
            val fallbackZh = hasZh
            SectionLabel(if (fallbackZh) strings.definitionsZh else strings.definitionsEn)
            Text(
                strings.definitionLanguageFallback,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(6.dp))
            if (fallbackZh) {
                ZhSenseList(zhBase)
                if (zhExtra.isNotEmpty()) {
                    OtherFormLabel(strings.otherFormSenses)
                    ZhSenseList(zhExtra, muted = true)
                }
            } else {
                EnSenseList(enBase)
                if (enExtra.isNotEmpty()) {
                    OtherFormLabel(strings.otherFormSenses)
                    EnSenseList(enExtra)
                }
            }
        }
        if (panel.senses.isEmpty()) {
            Text(
                panel.emptyLabel ?: strings.noDefinition,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        panel.exampleEn?.let { example ->
            Spacer(Modifier.height(14.dp))
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceContainer,
            ) {
                Column(Modifier.padding(12.dp)) {
                    Text(
                        strings.example,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(example, style = MaterialTheme.typography.bodyMedium, fontStyle = FontStyle.Italic)
                    panel.exampleZh?.let {
                        Text(
                            it,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
        if (panel.gloss.isNullOrBlank() && panel.senses.isEmpty() && !panel.sentenceZh.isNullOrBlank()) {
            Spacer(Modifier.height(12.dp))
            SectionLabel(strings.sentenceMeaning)
            Text(
                panel.sentenceZh.orEmpty(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(18.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onToggleCard, modifier = Modifier.weight(1f)) {
                Text(if (panel.collected) strings.removeCard else strings.collectCard)
            }
            TextButton(onClick = onClose) { Text(strings.close) }
        }
    }
}

@Composable
private fun ZhSenseList(senses: List<SenseUi>, muted: Boolean = false) {
    senses.forEach { sense ->
        Row(Modifier.padding(bottom = 4.dp)) {
            sense.pos?.takeIf { it.isNotBlank() }?.let {
                // Reserve a column wide enough for `conj.` / `abbr.` and let it
                // grow with the font scale instead of wrapping or clipping.
                Text(
                    it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (muted) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.widthIn(min = 36.dp),
                )
                Spacer(Modifier.width(8.dp))
            }
            Text(
                sense.definition,
                style = MaterialTheme.typography.bodyMedium,
                color = if (muted) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun EnSenseList(senses: List<SenseUi>) {
    senses.forEach { sense ->
        Text(
            listOfNotNull(sense.pos?.takeIf { it.isNotBlank() }, sense.definition)
                .joinToString(" "),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 4.dp),
        )
    }
}

@Composable
private fun OtherFormLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 4.dp, bottom = 2.dp),
    )
}

/** Contents sheet: every unit of the book, finished ones included. */
@Composable
private fun TocSheet(
    strings: Strings,
    toc: List<TocChapterUi>,
    onOpen: (String) -> Unit,
) {
    Column(Modifier.fillMaxWidth().navigationBarsPadding()) {
        Column(Modifier.padding(horizontal = 22.dp)) {
            Text(strings.toc, style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(4.dp))
            Text(
                strings.tocHint,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(12.dp))
        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f, fill = false),
            contentPadding = PaddingValues(start = 14.dp, end = 14.dp, bottom = 20.dp),
        ) {
            toc.forEach { chapter ->
                if (chapter.title.isNotBlank()) {
                    item(key = "chapter:${chapter.id}") {
                        Text(
                            chapter.title,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 8.dp, top = 14.dp, bottom = 6.dp),
                        )
                    }
                }
                items(chapter.units, key = { it.id }) { unit ->
                    TocRow(strings, unit, onOpen)
                }
            }
        }
    }
}

@Composable
private fun TocRow(
    strings: Strings,
    unit: TocUnitUi,
    onOpen: (String) -> Unit,
) {
    val background = if (unit.current) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceContainerLow
    }
    Surface(
        onClick = { onOpen(unit.id) },
        shape = MaterialTheme.shapes.medium,
        color = background,
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                unit.number.toString().padStart(2, '0'),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(30.dp),
            )
            Text(
                unit.title,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            when {
                unit.current -> MetaChip(strings.tocCurrent)
                unit.read -> MetaChip(strings.tocRead)
            }
        }
    }
}

private fun levelSourceLabel(strings: Strings, source: String): String = when (source) {
    "aligned" -> strings.levelFromWordNet
    "derived" -> strings.levelFromRoot
    "tag" -> strings.levelFromExam
    "freq" -> strings.levelFromFrequency
    else -> ""
}
