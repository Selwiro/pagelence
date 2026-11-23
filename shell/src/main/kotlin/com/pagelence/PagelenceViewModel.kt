package com.pagelence

import android.net.Uri
import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pagelence.core.assessment.AssessmentMode
import com.pagelence.core.assessment.AssessmentPlan
import com.pagelence.core.assessment.AssessmentResult
import com.pagelence.core.assessment.AssessmentRules
import com.pagelence.core.assessment.AssessmentSkill
import com.pagelence.core.assessment.levelSource
import com.pagelence.core.content.Analysis
import com.pagelence.core.content.ContentIndex
import com.pagelence.core.content.BookSummary
import com.pagelence.core.content.ContentOutline
import com.pagelence.presentation.achievements.AchievementPresenter
import com.pagelence.presentation.annotations.AnnotationPresenter
import com.pagelence.presentation.library.LibraryPresenter
import com.pagelence.presentation.lookup.LookupHistoryPresenter
import com.pagelence.presentation.reader.PreviewPresenter
import com.pagelence.presentation.reader.ReadingClock
import com.pagelence.presentation.reader.ReaderPresenter
import com.pagelence.presentation.reader.WordPanelPresenter
import com.pagelence.presentation.review.ReviewSessionRules
import com.pagelence.presentation.assessment.AssessmentPresenter
import com.pagelence.presentation.book.BookDetailPresenter
import com.pagelence.presentation.dictionary.DictionaryPresenter
import com.pagelence.presentation.grammar.GrammarPresenter
import com.pagelence.presentation.search.SearchPresenter
import com.pagelence.presentation.store.StorePresenter
import com.pagelence.presentation.shell.FinishedPresenter
import com.pagelence.presentation.shell.SettingsPresenter
import com.pagelence.presentation.shell.WordCardPresenter
import com.pagelence.presentation.sync.SyncPresenter
import com.pagelence.presentation.store.formatBytes
import com.pagelence.presentation.store.storeErrorPriority
import com.pagelence.presentation.store.storeErrorText
import com.pagelence.presentation.reader.toSelectedUi
import com.pagelence.presentation.reader.toStripUi
import com.pagelence.core.content.StoreCatalogRules
import com.pagelence.core.content.Grammar
import com.pagelence.core.content.GrammarPoint
import com.pagelence.core.content.Hint
import com.pagelence.core.content.HintExample
import com.pagelence.core.content.HintTapResolver
import com.pagelence.core.content.BookLicense
import com.pagelence.core.content.LocalizedText
import com.pagelence.core.content.Paragraph
import com.pagelence.core.content.Sentence
import com.pagelence.core.content.StudyUnit
import com.pagelence.core.lexicon.WordLookup
import com.pagelence.core.lookup.LookupEntry
import com.pagelence.core.lookup.LookupHistoryRules
import com.pagelence.core.dict.DictInstallResult
import com.pagelence.core.dict.DictManifest
import com.pagelence.core.dict.DictRejection
import com.pagelence.core.dict.DictRules
import com.pagelence.core.prefs.DailyGoal
import com.pagelence.core.prefs.DefinitionMode
import com.pagelence.core.prefs.FontSize
import com.pagelence.core.prefs.LineSpacing
import com.pagelence.core.prefs.PageMargin
import com.pagelence.core.prefs.ReadingFont
import com.pagelence.core.prefs.GuideMode
import com.pagelence.core.prefs.LevelDefaults
import com.pagelence.core.prefs.LevelRecommendation
import com.pagelence.core.prefs.ThemeMode
import com.pagelence.core.prefs.Language
import com.pagelence.core.prefs.ReaderLayout
import com.pagelence.core.prefs.Preferences
import com.pagelence.core.progress.AnnotationKind
import com.pagelence.core.progress.AnnotationRules
import com.pagelence.core.progress.BookProgress
import com.pagelence.core.progress.CefrLevel
import com.pagelence.core.progress.LevelSource
import com.pagelence.core.progress.ReaderPhase
import com.pagelence.core.progress.ShelfRules
import com.pagelence.core.progress.ReaderPosition
import com.pagelence.core.progress.HeatmapRules
import com.pagelence.core.progress.LibraryEntry
import com.pagelence.core.progress.LibraryRules
import com.pagelence.core.progress.ProgressSemantics
import com.pagelence.core.progress.QuizGrading
import com.pagelence.core.progress.ReadingState
import com.pagelence.core.source.CatalogRules
import com.pagelence.core.source.CatalogSnapshot
import com.pagelence.core.source.ImportedSource
import com.pagelence.core.source.InstalledBook
import com.pagelence.core.source.SourceError
import com.pagelence.core.source.SourceLine
import com.pagelence.core.source.SourceResult
import com.pagelence.core.reading.SelectedTerm
import com.pagelence.core.reading.WordSelectionState
import com.pagelence.core.wordcards.ReviewGrade
import com.pagelence.core.wordcards.Srs
import com.pagelence.core.wordcards.WordCard
import com.pagelence.sync.StateTransfer
import com.pagelence.sync.SyncException
import com.pagelence.sync.SyncMode
import com.pagelence.sync.SyncOutcome
import com.pagelence.sync.SyncProvider
import com.pagelence.sync.SyncProviders
import com.pagelence.sync.WebDavCredentials
import com.pagelence.sync.WebDavUrl
import com.pagelence.uim3.Strings
import com.pagelence.uim3.SyncCopy
import com.pagelence.uim3.UiLanguage
import com.pagelence.uim3.model.AnnotationFilter
import com.pagelence.uim3.model.AnnotationUi
import com.pagelence.uim3.model.AssessmentQuestionUi
import com.pagelence.uim3.model.AssessmentResultUi
import com.pagelence.uim3.model.AssessmentSkillUi
import com.pagelence.uim3.model.AssessmentUiState
import com.pagelence.uim3.model.BuiltInBookUi
import com.pagelence.uim3.model.BookChapterUi
import com.pagelence.uim3.model.BookDetailUi
import com.pagelence.uim3.model.BookUi
import com.pagelence.uim3.model.FinishedBookUi
import com.pagelence.uim3.model.DictionaryPhase
import com.pagelence.uim3.model.DictionaryUiState
import com.pagelence.uim3.model.GrammarDetailUi
import com.pagelence.uim3.model.GrammarExampleUi
import com.pagelence.uim3.model.GrammarPointUi
import com.pagelence.uim3.model.GrammarTabUiState
import com.pagelence.uim3.model.AnalysisPartUi
import com.pagelence.uim3.model.AnalysisUi
import com.pagelence.uim3.model.GrammarRelatedUi
import com.pagelence.uim3.model.GrammarUi
import com.pagelence.uim3.model.ImportPreviewUi
import com.pagelence.uim3.model.LibraryUiState
import com.pagelence.uim3.model.LookupHistoryUi
import com.pagelence.uim3.model.LevelPromptUi
import com.pagelence.uim3.model.ParagraphUi
import com.pagelence.uim3.model.ProfileUiState
import com.pagelence.uim3.model.QuizQuestionUi
import com.pagelence.uim3.model.PreviewWordUi
import com.pagelence.uim3.model.ReaderPreviewUi
import com.pagelence.uim3.model.ReaderJumpUi
import com.pagelence.uim3.model.ReaderUiState
import com.pagelence.uim3.model.ReviewTab
import com.pagelence.uim3.model.SearchHitUi
import com.pagelence.uim3.model.SearchUiState
import com.pagelence.uim3.model.SelectedWordUi
import com.pagelence.uim3.model.SenseUi
import com.pagelence.uim3.model.TocChapterUi
import com.pagelence.uim3.model.TocUnitUi
import com.pagelence.uim3.model.SentenceUi
import com.pagelence.uim3.model.HeatmapCellUi
import com.pagelence.uim3.model.HeatmapUi
import com.pagelence.uim3.model.InstalledBookUi
import com.pagelence.uim3.model.SettingsUiState
import com.pagelence.uim3.model.StoreBookUi
import com.pagelence.uim3.model.StoreSourceUi
import com.pagelence.uim3.model.StoreUiState
import com.pagelence.uim3.model.SyncTone
import com.pagelence.uim3.model.SyncUiState
import com.pagelence.uim3.model.ReviewSessionUi
import com.pagelence.uim3.model.WordReviewUi
import com.pagelence.uim3.model.WordPanelUi
import com.pagelence.uim3.model.WordStripUi
import com.pagelence.uim3.stringsFor
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/** Screens are shell state; the reading session itself lives in [AppUiState.reader]. */
enum class Screen {
    ONBOARDING,
    LIBRARY,
    STORE,
    REVIEW,
    PROFILE,
    GRAMMAR_DETAIL,
    GRAMMAR_POINT,
    ASSESSMENT,
    READER,
    SETTINGS,
    SETTINGS_TYPOGRAPHY,
    SETTINGS_DICTIONARY,
    ACHIEVEMENTS,
    SYNC,
    ABOUT,
    ABOUT_PRIVACY,
    ABOUT_LICENSES,
    BOOK_DETAIL,
    BOOK_INFO,
    BOOK_FINISHED,
    SEARCH,
}

/** Four bottom-bar destinations; each keeps its own screen stack. */
enum class MainTab { SHELF, STORE, REVIEW, PROFILE }

/**
 * One-shot startup message: a recovered/damaged progress file, or content that
 * could not be read (whole package vs. a few books).
 */
enum class RecoveryNotice { FROM_BACKUP, CORRUPT, CONTENT_FAILED, CONTENT_PARTIAL }

fun rootScreen(tab: MainTab): Screen = when (tab) {
    MainTab.SHELF -> Screen.LIBRARY
    MainTab.STORE -> Screen.STORE
    MainTab.REVIEW -> Screen.REVIEW
    MainTab.PROFILE -> Screen.PROFILE
}

data class AppUiState(
    val screen: Screen = Screen.LIBRARY,
    val tab: MainTab = MainTab.SHELF,
    val library: LibraryUiState = LibraryUiState(),
    val reader: ReaderUiState? = null,
    /** 复习 → 单词：列表、今日到期/新卡计数与待复习队列。 */
    val review: WordReviewUi = WordReviewUi(),
    /** 正在进行的复习轮次；null 表示在列表或别的页面。 */
    val reviewSession: ReviewSessionUi? = null,
    /** Cross-book bookmarks/highlights/notes for 复习 → 笔记. */
    val annotations: List<AnnotationUi> = emptyList(),
    /** Recent word lookups for 复习 → 查词, newest first. */
    val lookupHistory: List<LookupHistoryUi> = emptyList(),
    val annotationFilter: AnnotationFilter = AnnotationFilter.ALL,
    val settings: SettingsUiState = SettingsUiState(
        language = UiLanguage.ZH,
        theme = ThemeMode.SYSTEM,
        fontSize = FontSize.MEDIUM,
        readerLayout = ReaderLayout.PAGED,
        annotateHints = true,
        definitionMode = DefinitionMode.ZH,
        guideMode = GuideMode.BILINGUAL,
    ),
    val profile: ProfileUiState = ProfileUiState(),
    val grammarTab: GrammarTabUiState = GrammarTabUiState(),
    val reviewTab: ReviewTab = ReviewTab.WORDS,
    val store: StoreUiState = StoreUiState(),
    val grammarDetail: GrammarDetailUi? = null,
    val grammarPoint: GrammarPointUi? = null,
    val assessment: AssessmentUiState = AssessmentUiState(),
    /** Opened from a store card; null while loading or after leaving the page. */
    val bookDetail: BookDetailUi? = null,
    /** The page shown right after the last unit; null everywhere else. */
    val finishedBook: FinishedBookUi? = null,
    val speechAvailable: Boolean = false,
    val readerExitConfirm: Boolean = false,
    /** The level prompt is open; nothing has been written to `state.json` yet. */
    val levelPrompt: LevelPromptUi? = null,
    /** WebDAV setup draft + persisted sync status. */
    val sync: SyncUiState = SyncUiState(),
    /** Settings → 词典 → 完整词表 state. */
    val dictionary: DictionaryUiState = DictionaryUiState(),
    /** Set when startup had to recover from a backup or start empty. */
    val recovery: RecoveryNotice? = null,
    /** Current in-book search; null outside the search screen. */
    val search: SearchUiState? = null,
)

/** A level waiting for the reader to accept, keep or edit the recommended modes. */
private data class PendingLevel(val level: CefrLevel, val source: LevelSource, val score: Int?)

/**
 * 壳的状态机：把 :core 的解析、进度、查词、卡片结果映射成 UI 模型，再把界面事件
 * 翻译成 :core 调用。业务规则都在 :core，本类只做编排与线程切换。
 */
class PagelenceViewModel(private val graph: AppGraph) : ViewModel() {
    private val _ui = MutableStateFlow(AppUiState())
    val ui: StateFlow<AppUiState> = _ui.asStateFlow()

    private var index: ContentIndex? = null
    private var quizAnswers: Map<Int, Int> = emptyMap()
    private var quizSubmitted = false
    private var quizCollected = 0
    private var storeSnapshots: List<CatalogSnapshot> = emptyList()
    private var assessmentPlan: AssessmentPlan? = null
    private var pendingLevel: PendingLevel? = null
    private val assessmentAnswers = mutableMapOf<String, Int>()
    private var assessmentResult: AssessmentResult? = null
    private var pendingPanel: WordPanelUi? = null
    private var wordSelection: WordSelectionState = WordSelectionState.Hidden
    /** Bumped for every annotation jump, so the reader can re-open the same row. */
    private var annotationJumpToken = 0L

    private var syncConfig: SyncConfig? = null
    private var syncDraft: SyncConfig = SyncConfig(SyncProvider.CUSTOM, "", "", false)
    private var syncHasPassword = false
    private var pendingImport: ReadingState? = null
    private var syncDebounceJob: Job? = null
    private var dictionaryDownloadJob: Job? = null
    private val syncDebounceLock = Any()
    /** Set while the engine writes local state, so its own write does not re-trigger sync. */
    private val syncWriteSuppressed = AtomicBoolean(false)

    /** Everything needed to rewrite one unit's resume point after a page turn. */
    private data class ReaderSavePoint(
        val bookId: String,
        val unitId: String,
        val phase: ReaderPhase,
        val textPage: Int,
        val textProgress: Float,
        /** Anchor paragraph of the visible page; null keeps the stored one. */
        val paragraphId: String?,
        val answers: Map<Int, Int>,
        val submitted: Boolean,
    )

    /**
     * The newest snapshot waiting to be written. Page turns are debounced and
     * coalesced here, and [progressWriteLock] serialises every write, so an
     * older snapshot can never land after a newer one.
     */
    private val pendingReaderSave = AtomicReference<ReaderSavePoint?>(null)
    private val progressWriteLock = Mutex()
    private var readerSaveJob: Job? = null

    /**
     * Reading-time accounting (T162). The clock only runs while the reader is on
     * screen and the app is in the foreground; [readingPendingSeconds] holds the
     * seconds not written yet and is flushed every [READING_TICK_MS] and when the
     * reader is left, so a process kill loses at most one tick.
     */
    private val readingClock = ReadingClock()
    private var readingTicker: Job? = null
    private var readingPendingSeconds = 0
    private var appInForeground = true

    private val stacks: MutableMap<MainTab, MutableList<Screen>> = MainTab.entries.associateWith {
        mutableListOf(rootScreen(it))
    }.toMutableMap()

    init {
        graph.speech.setOnReadyListener { available ->
            _ui.update { it.copy(speechAvailable = available) }
        }
        // Any local write (progress, card, preference, profile) schedules a
        // debounced sync; the store is the single choke point, so no call site
        // has to remember.
        graph.store.addWriteListener { onLocalStateWritten() }
        observeReadingTime()
        bootstrap()
    }

    override fun onCleared() {
        flushProgress()
        flushReadingSeconds(blocking = true)
        graph.speech.shutdown()
        super.onCleared()
    }

    // --- reading time ----------------------------------------------------

    /**
     * The reader screen and app foreground together decide whether time counts.
     * Watching [AppUiState.screen] instead of every navigation helper is what
     * keeps this correct for the many ways in and out of the reader.
     */
    private fun observeReadingTime() {
        viewModelScope.launch {
            _ui.map { it.screen }.distinctUntilChanged().collect {
                applyReadingClock()
            }
        }
    }

    private fun applyReadingClock() {
        val now = SystemClock.elapsedRealtime()
        val active = appInForeground && _ui.value.screen == Screen.READER
        if (active) {
            if (!readingClock.running) {
                readingClock.start(now)
                startReadingTicker()
            }
            return
        }
        if (readingClock.running) {
            queueReadingSeconds(readingClock.stop(now))
        }
        readingTicker?.cancel()
        readingTicker = null
        flushReadingSeconds()
    }

    private fun startReadingTicker() {
        readingTicker?.cancel()
        readingTicker = viewModelScope.launch(Dispatchers.IO) {
            while (isActive) {
                delay(READING_TICK_MS)
                val seconds = readingClock.tick(SystemClock.elapsedRealtime())
                if (seconds > 0) {
                    queueReadingSeconds(seconds)
                    flushReadingSeconds()
                }
            }
        }
    }

    private fun queueReadingSeconds(seconds: Int) {
        if (seconds > 0) readingPendingSeconds += seconds
    }

    /**
     * Write the pending seconds into today's log. Called from IO coroutines and
     * from `ON_STOP`; [blocking] is only for `onCleared`, where the process may
     * be gone right after.
     */
    private fun flushReadingSeconds(blocking: Boolean = false) {
        val seconds = readingPendingSeconds
        if (seconds <= 0) return
        readingPendingSeconds = 0
        val date = today()
        if (blocking) {
            runCatching {
                runBlocking {
                    withTimeoutOrNull(FLUSH_PROGRESS_TIMEOUT_MS) {
                        graph.profile.recordReadingSeconds(date, seconds)
                    }
                }
            }
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { graph.profile.recordReadingSeconds(date, seconds) }
                .getOrNull() ?: return@launch
            val profile = runCatching { buildProfile() }.getOrNull() ?: return@launch
            withContext(Dispatchers.Main) {
                _ui.update { it.copy(profile = profile) }
            }
        }
    }

    // --- startup ---------------------------------------------------------

    private fun bootstrap() {
        viewModelScope.launch(Dispatchers.IO) {
            val prefs = graph.preferences.preferences()
            val language = uiLanguage(prefs)
            val strings = stringsFor(language)
            val skipped = mutableListOf<String>()
            // A missing or unreadable manifest is the one failure that leaves the
            // shelf unusable; show a retry instead of crashing on launch.
            val library = runCatching { buildLibrary(strings, language, skipped) }
            val onboarding = !prefs.levelOnboardingDone
            val storedSync = graph.syncCredentials.config()
            syncConfig = storedSync
            syncDraft = storedSync ?: SyncConfig(SyncProvider.CUSTOM, "", "", false)
            syncHasPassword = graph.syncCredentials.hasPassword()

            if (library.isFailure) {
                _ui.update {
                    it.copy(
                        screen = Screen.LIBRARY,
                        tab = MainTab.SHELF,
                        settings = settingsOf(prefs, language),
                        recovery = RecoveryNotice.CONTENT_FAILED,
                    )
                }
                return@launch
            }

            // One broken book must not take the shelf, the profile or the store
            // down with it; each section degrades to its empty state.
            val profile = runCatching { buildProfile() }.getOrDefault(ProfileUiState())
            val annotations = runCatching { buildAnnotations(language) }.getOrDefault(emptyList())
            val grammarTab = runCatching { buildGrammarTab(strings, language) }
                .getOrDefault(GrammarTabUiState(loading = false))
            val store = runCatching { buildStoreState(strings, language) }
                .getOrDefault(StoreUiState(loading = false))
            val recovery = when {
                graph.store.recoveredFromBackup -> RecoveryNotice.FROM_BACKUP
                graph.store.corruptWithoutBackup -> RecoveryNotice.CORRUPT
                skipped.isNotEmpty() -> RecoveryNotice.CONTENT_PARTIAL
                else -> null
            }
            _ui.update {
                it.copy(
                    screen = if (onboarding) Screen.ONBOARDING else Screen.LIBRARY,
                    tab = MainTab.SHELF,
                    library = library.getOrDefault(LibraryUiState(loading = false)),
                    profile = profile,
                    annotations = annotations,
                    grammarTab = grammarTab,
                    store = store,
                    settings = settingsOf(prefs, language),
                    sync = syncUiState(language),
                    recovery = recovery,
                )
            }
            onAppForeground()
        }
    }

    /** Retry the content-dependent startup after a damaged package. */
    fun retryContent() {
        _ui.update { it.copy(recovery = null, library = LibraryUiState(loading = true)) }
        bootstrap()
    }

    fun refreshLibrary() {
        viewModelScope.launch(Dispatchers.IO) {
            val prefs = graph.preferences.preferences()
            val language = uiLanguage(prefs)
            val strings = stringsFor(language)
            val skipped = mutableListOf<String>()
            val library = runCatching { buildLibrary(strings, language, skipped) }
                .getOrDefault(LibraryUiState(loading = false))
            val profile = runCatching { buildProfile() }.getOrDefault(ProfileUiState())
            val annotations = runCatching { buildAnnotations(language) }.getOrDefault(emptyList())
            val grammarTab = runCatching { buildGrammarTab(strings, language) }
                .getOrDefault(GrammarTabUiState(loading = false))
            _ui.update {
                it.copy(
                    library = library,
                    profile = profile,
                    grammarTab = grammarTab,
                    settings = settingsOf(prefs, language),
                    recovery = if (skipped.isNotEmpty()) RecoveryNotice.CONTENT_PARTIAL else it.recovery,
                )
            }
        }
    }

    /**
     * Build the shelf from `books-index.json` + each book's `outline.json`.
     *
     * Deliberately no `index(bookId)` here: parsing 1 MB of prose per book on
     * every cold start is what made "add more books" expensive. A book whose
     * outline/body is unreadable is skipped and reported, never fatal.
     */
    private fun presenter(strings: Strings, language: UiLanguage) = LibraryPresenter(
        content = graph.content,
        progress = graph.progress,
        profiles = graph.profile,
        strings = strings,
        language = language,
    )

    private fun buildLibrary(
        strings: Strings,
        language: UiLanguage,
        skipped: MutableList<String> = mutableListOf(),
    ): LibraryUiState {
        val shelfIds = graph.preferences.preferences().shelfBookIds.toSet()
        return presenter(strings, language).shelf(shelfIds, skipped)
    }

    private fun buildProfile(): ProfileUiState {
        val prefs = graph.preferences.preferences()
        val language = uiLanguage(prefs)
        val strings = stringsFor(language)
        val base = presenter(strings, language).profile()
        return includePendingReading(base)
            .copy(achievements = achievementPresenter(strings, language).wall())
    }

    /**
     * Seconds the clock counted but the store has not written yet. Folding them
     * into the built profile keeps "今日" honest when a screen is rebuilt in the
     * same instant the last 15-second chunk is being flushed; the numbers are
     * recomputed from scratch after the write anyway.
     */
    private fun includePendingReading(profile: ProfileUiState): ProfileUiState {
        val pending = readingPendingSeconds
        if (pending <= 0) return profile
        val goalSeconds = profile.dailyGoalMinutes.coerceAtLeast(1) * 60
        val seconds = profile.todaySeconds + pending
        val met = seconds >= goalSeconds || profile.metToday
        return profile.copy(
            todaySeconds = seconds,
            metToday = met,
            secondsToGoal = if (met) 0 else (goalSeconds - seconds).coerceAtLeast(0),
        )
    }

    private fun achievementPresenter(strings: Strings, language: UiLanguage) = AchievementPresenter(
        content = graph.content,
        progress = graph.progress,
        profiles = graph.profile,
        cards = graph.cards,
        annotations = graph.annotations,
        strings = strings,
    )

    // --- reading ---------------------------------------------------------

    private fun currentStack(): MutableList<Screen> =
        stacks.getOrPut(_ui.value.tab) { mutableListOf(rootScreen(_ui.value.tab)) }

    private fun push(screen: Screen) {
        currentStack().add(screen)
        _ui.update { it.copy(screen = screen, readerExitConfirm = false) }
    }

    private fun popScreen(): Screen? {
        val stack = currentStack()
        if (stack.size <= 1) return null
        stack.removeAt(stack.lastIndex)
        return stack.last()
    }

    fun selectTab(tab: MainTab) {
        if (_ui.value.tab == tab) return
        graph.speech.stop()
        val target = stacks[tab]?.lastOrNull() ?: rootScreen(tab)
        _ui.update {
            it.copy(tab = tab, screen = target, readerExitConfirm = false)
        }
        if (tab == MainTab.STORE) refreshStore()
        if (tab == MainTab.REVIEW) refreshCards()
    }

    /** Back within the current tab; at a tab root the system handles Back. */
    fun back() {
        graph.speech.stop()
        if (_ui.value.screen == Screen.READER) {
            leaveReader()
            return
        }
        val next = popScreen() ?: return
        _ui.update {
            it.copy(
                screen = next,
                readerExitConfirm = false,
                finishedBook = null,
            )
        }
    }

    fun openBook(bookId: String) = openBookAt(bookId, unitId = null)

    /** Open a specific unit, used by the grammar detail's "回到本单元". */
    fun openUnit(bookId: String, unitId: String) = openBookAt(bookId, unitId)

    /**
     * Jump to any unit from the in-reader contents sheet. Unlike [openUnit] this
     * does not push another reader screen: the sheet is already inside the
     * reader, so the reader just swaps its unit and restarts at page 0.
     */
    fun openUnitFromReader(unitId: String) {
        val reader = _ui.value.reader ?: return
        openBookAt(reader.bookId, unitId, push = false)
    }

    /** Open 书内搜索 for the current book, keeping the last query (T160). */
    fun openBookSearch() {
        val reader = _ui.value.reader ?: return
        val bookIndex = index?.takeIf { it.book.id == reader.bookId } ?: return
        val presenter = SearchPresenter(_ui.value.settings.language)
        val previous = _ui.value.search?.takeIf { it.bookId == reader.bookId }
        push(Screen.SEARCH)
        _ui.update {
            it.copy(
                search = if (previous == null) {
                    presenter.initial(bookIndex)
                } else {
                    presenter.search(bookIndex, previous.query)
                },
            )
        }
    }

    /** Keystroke handler; scanning one book is cheap enough to run inline. */
    fun updateSearchQuery(query: String) {
        val current = _ui.value.search ?: return
        val bookIndex = index?.takeIf { it.book.id == current.bookId } ?: return
        val next = SearchPresenter(_ui.value.settings.language).search(bookIndex, query)
        _ui.update { it.copy(search = next) }
    }

    /** Jump to a result: same book reuses the reader, another unit swaps it. */
    fun openSearchHit(hit: SearchHitUi) {
        val reader = _ui.value.reader
        val jump = ReaderJumpUi(
            paragraphId = hit.paragraphId,
            sentenceNumber = hit.sentenceNumber,
            token = ++annotationJumpToken,
        )
        val next = popScreen() ?: return
        _ui.update { it.copy(screen = next, search = null) }
        when {
            reader != null && reader.bookId == hit.bookId && reader.unitId == hit.unitId ->
                _ui.update { state ->
                    state.reader?.let { state.copy(reader = it.copy(jumpTarget = jump)) } ?: state
                }

            reader != null && reader.bookId == hit.bookId ->
                openBookAt(hit.bookId, hit.unitId, push = false, jump = jump)

            else -> openBookAt(hit.bookId, hit.unitId, push = true, jump = jump)
        }
    }

    private fun openBookAt(
        bookId: String,
        unitId: String?,
        push: Boolean = true,
        jump: ReaderJumpUi? = null,
        lookup: LookupHistoryUi? = null,
    ) {
        clearPendingReaderSave()
        viewModelScope.launch(Dispatchers.IO) {
            graph.speech.stop()
            val bookIndex = runCatching { graph.content.index(bookId) }.getOrNull()
            if (bookIndex == null) {
                withContext(Dispatchers.Main) {
                    _ui.update { it.copy(recovery = RecoveryNotice.CONTENT_FAILED) }
                }
                return@launch
            }
            val saved = graph.progress.progress(bookId)
            val unit = unitId?.let(bookIndex::unit)
                ?: saved.paragraphId?.let(bookIndex::unitOf)
                ?: bookIndex.units.firstOrNull()
                ?: return@launch
            // Opening a unit from the grammar tab makes it the resume position
            // even before the first page turn.
            val progress = if (unitId != null) {
                unit.paragraphs.firstOrNull()?.let { first ->
                    ProgressSemantics.setPosition(saved, bookIndex, first.id).also { graph.progress.save(it) }
                } ?: saved
            } else {
                saved
            }
            index = bookIndex
            val resume = progress.reader?.takeIf { it.unitId == unit.id }
            quizAnswers = resume?.quizAnswers ?: emptyMap()
            quizSubmitted = resume?.quizSubmitted ?: false
            quizCollected = 0
            pendingPanel = null
            wordSelection = WordSelectionState.clear()
            graph.profile.recordReadingDay(today())
            val reader = buildReader(
                bookIndex = bookIndex,
                unit = unit,
                progress = progress,
                startAtText = jump != null,
                jump = jump,
            )
            val profile = buildProfile()
            withContext(Dispatchers.Main) {
                if (push) currentStack().add(Screen.READER)
                _ui.update {
                    it.copy(
                        screen = Screen.READER,
                        reader = reader,
                        readerExitConfirm = false,
                        profile = profile,
                    )
                }
                // Persist the entry point immediately: a kill before the first
                // page turn should still resume at this unit and phase.
                saveReaderPosition(reader, immediate = true)
            }
            // Reopening a history row also opens that word's panel; the reader
            // state above is committed before the lookup runs.
            lookup?.let {
                onWordClick(it.paragraphId, it.sentenceNumber, it.term, it.start, it.end)
            }
        }
    }

    /** Top-bar/back request: confirm first when quiz answers are still unsent. */
    fun requestCloseReader() {
        graph.speech.stop()
        val reader = _ui.value.reader
        val inProgress = reader != null && !reader.quizSubmitted && reader.quiz.any { it.selected != null }
        if (inProgress) {
            _ui.update { it.copy(readerExitConfirm = true) }
        } else {
            closeReader()
        }
    }

    fun dismissReaderExit() {
        _ui.update { it.copy(readerExitConfirm = false) }
    }

    fun dismissRecovery() {
        _ui.update { it.copy(recovery = null) }
    }

    fun closeReader() {
        graph.speech.stop()
        pendingPanel = null
        wordSelection = WordSelectionState.clear()
        if (_ui.value.screen == Screen.READER) leaveReader() else back()
    }

    /**
     * Leave the reader. Saving on the last unit's grammar page is the third
     * finish path: the book becomes complete and the ending page replaces the
     * reader instead of dropping straight back to the shelf.
     */
    private fun leaveReader() {
        val reader = _ui.value.reader
        val bookIndex = index
        val saved = reader?.let { graph.progress.progress(it.bookId) }
        // The passive save on entering the grammar page already marks the unit
        // read, so completeness alone cannot tell whether the ending was shown.
        // The finish date is the transition marker: null before the first
        // completion, kept on every later re-read.
        val completes = reader != null && bookIndex != null && saved != null &&
            reader.resumePhase == ReaderPhase.GRAMMAR &&
            bookIndex.nextUnit(reader.unitId) == null &&
            saved.finishedAt == null
        if (reader == null || bookIndex == null || !completes) {
            _ui.value.reader?.let { saveReaderPosition(it, immediate = true) }
            val next = popScreen() ?: rootScreen(_ui.value.tab)
            _ui.update { it.copy(screen = next, reader = null, readerExitConfirm = false) }
            refreshLibrary()
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            progressWriteLock.withLock {
                persistReaderSavePoint(
                    savePointOf(reader, answers = quizAnswers, submitted = quizSubmitted),
                    now = Instant.now().toString(),
                )
            }
            showFinished(reader.bookId, fromReader = true)
        }
    }

    fun backToReader() = back()

    fun closeCards() = back()

    fun openProfile() = selectTab(MainTab.PROFILE)

    fun closeProfile() = selectTab(MainTab.SHELF)

    fun openStore() = selectTab(MainTab.STORE)

    fun openGrammar() = openReview(ReviewTab.GRAMMAR)

    /** Switch to the review tab and pick its words or grammar sub-page. */
    fun openReview(tab: ReviewTab) {
        _ui.update { it.copy(reviewTab = tab) }
        selectTab(MainTab.REVIEW)
        refreshCards()
    }

    fun selectReviewTab(tab: ReviewTab) {
        _ui.update { it.copy(reviewTab = tab) }
    }

    private fun refreshCards() {
        viewModelScope.launch(Dispatchers.IO) {
            val review = currentReview()
            val language = uiLanguage(graph.preferences.preferences())
            val annotations = buildAnnotations(language)
            val lookups = currentLookupHistory(language)
            withContext(Dispatchers.Main) {
                _ui.update {
                    it.copy(review = review, annotations = annotations, lookupHistory = lookups)
                }
            }
        }
    }

    private fun currentLookupHistory(language: UiLanguage): List<LookupHistoryUi> =
        LookupHistoryPresenter(
            content = graph.content,
            lookups = graph.lookups,
            language = language,
        ).list()

    private fun buildAnnotations(language: UiLanguage): List<AnnotationUi> =
        annotationPresenter(language).all(graph.annotations.all())

    private fun annotationPresenter(language: UiLanguage): AnnotationPresenter =
        AnnotationPresenter(content = graph.content, language = language)

    /** Level picked in onboarding or "我的"; the reading modes are confirmed first. */
    // --- book detail ------------------------------------------------------

    /** Open the detail page for a book card in the store or library. */
    fun openBookDetail(bookId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val language = uiLanguage(graph.preferences.preferences())
            val detail = buildBookDetail(bookId, language) ?: return@launch
            withContext(Dispatchers.Main) {
                _ui.update { it.copy(bookDetail = detail) }
                push(Screen.BOOK_DETAIL)
            }
        }
    }

    /** Open the "详细信息" page from the "关于此书" row. */
    fun openBookInfo() {
        if (_ui.value.bookDetail == null) return
        push(Screen.BOOK_INFO)
    }

    /** Add or remove the book from the shelf; progress is not touched. */
    fun toggleShelf(bookId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val prefs = graph.preferences.preferences()
            val shelf = ShelfRules.toggle(prefs.shelfBookIds, bookId)
            graph.preferences.save(prefs.copy(shelfBookIds = shelf))
            val language = uiLanguage(prefs)
            val library = buildLibrary(stringsFor(language), language)
            withContext(Dispatchers.Main) {
                _ui.update {
                    it.copy(
                        library = library,
                        bookDetail = it.bookDetail?.let { detail ->
                            if (detail.bookId == bookId) detail.copy(onShelf = bookId in shelf) else detail
                        },
                    )
                }
            }
        }
    }

    fun startBookFromDetail() {
        val detail = _ui.value.bookDetail ?: return
        if (!detail.available) return
        openBookAt(detail.bookId, unitId = null)
    }

    /** Jump into a chapter from its detail row. */
    fun openChapterFromDetail(bookId: String, chapterId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val chapter = graph.content.index(bookId)?.book?.chapters
                ?.firstOrNull { it.id == chapterId } ?: return@launch
            val unitId = chapter.units.firstOrNull()?.id ?: return@launch
            withContext(Dispatchers.Main) { openBookAt(bookId, unitId) }
        }
    }

    private fun buildBookDetail(bookId: String, language: UiLanguage): BookDetailUi? {
        val prefs = graph.preferences.preferences()
        return BookDetailPresenter(
            content = graph.content,
            sources = graph.sourceManager,
            catalog = graph.builtInCatalog,
            progressRepo = graph.progress,
            preferences = prefs,
            snapshots = storeSnapshots,
            downloadingId = _ui.value.store.downloadingId,
            strings = stringsFor(language),
            language = language,
        ).detail(bookId)
    }




    private suspend fun refreshBookDetail(bookId: String) {
        val detail = buildBookDetail(bookId, uiLanguage(graph.preferences.preferences()))
            ?: return
        withContext(Dispatchers.Main) {
            _ui.update {
                if (it.bookDetail?.bookId == bookId) it.copy(bookDetail = detail) else it
            }
        }
    }

    fun selectLevel(level: CefrLevel) = requestLevel(level, LevelSource.SELF, null)

    /**
     * A level from the self-assessment or a test. When its recommended modes
     * differ from the current ones, the reader decides first: nothing is
     * written to `state.json` until the prompt is answered.
     */
    private fun requestLevel(level: CefrLevel, source: LevelSource, score: Int?) {
        pendingLevel = PendingLevel(level, source, score)
        viewModelScope.launch(Dispatchers.IO) {
            val prefs = graph.preferences.preferences()
            val changes = LevelDefaults.changes(prefs, level)
            if (changes.isEmpty) {
                applyLevel(level, source, score, LevelRecommendation())
                return@launch
            }
            val prompt = LevelPromptUi(
                level = level,
                source = source,
                currentLanguage = uiLanguage(prefs),
                currentDefinition = prefs.definitionMode,
                currentGuide = prefs.guideMode,
                changes = changes,
            )
            withContext(Dispatchers.Main) { _ui.update { it.copy(levelPrompt = prompt) } }
        }
    }

    fun acceptLevelPrompt() {
        val pending = pendingLevel ?: return
        val changes = _ui.value.levelPrompt?.changes ?: return
        viewModelScope.launch(Dispatchers.IO) {
            applyLevel(pending.level, pending.source, pending.score, changes)
        }
    }

    fun keepLevelPrompt() {
        val pending = pendingLevel ?: return
        viewModelScope.launch(Dispatchers.IO) {
            applyLevel(pending.level, pending.source, pending.score, LevelRecommendation())
        }
    }

    fun applyLevelPrompt(language: UiLanguage, definitionMode: DefinitionMode, guideMode: GuideMode) {
        val pending = pendingLevel ?: return
        viewModelScope.launch(Dispatchers.IO) {
            applyLevel(
                level = pending.level,
                source = pending.source,
                score = pending.score,
                recommendation = LevelRecommendation(
                    language = if (language == UiLanguage.EN) Language.EN else Language.ZH,
                    definitionMode = definitionMode,
                    guideMode = guideMode,
                ),
            )
        }
    }

    /** Dismissing the prompt keeps the old level and the old settings. */
    fun dismissLevelPrompt() {
        pendingLevel = null
        _ui.update { it.copy(levelPrompt = null) }
    }

    /** Writes the level and only the modes the reader agreed to. */
    private suspend fun applyLevel(
        level: CefrLevel,
        source: LevelSource,
        score: Int?,
        recommendation: LevelRecommendation,
    ) {
        graph.profile.setLevel(level, source, score, graph.store.timestamp())
        val prefs = LevelDefaults.applyRecommendation(graph.preferences.preferences(), recommendation)
            .copy(levelOnboardingDone = true)
        graph.preferences.save(prefs)
        val language = uiLanguage(prefs)
        val strings = stringsFor(language)
        val profile = buildProfile()
        val library = buildLibrary(strings, language)
        val grammarTab = buildGrammarTab(strings, language)
        val leavingAssessment = _ui.value.screen == Screen.ASSESSMENT
        withContext(Dispatchers.Main) {
            pendingLevel = null
            _ui.update {
                it.copy(
                    screen = if (it.screen == Screen.ONBOARDING) Screen.LIBRARY else it.screen,
                    levelPrompt = null,
                    settings = settingsOf(prefs, language),
                    profile = profile,
                    library = library,
                    grammarTab = grammarTab,
                )
            }
            if (leavingAssessment) finishAssessment()
        }
    }

    fun skipOnboarding() {
        viewModelScope.launch(Dispatchers.IO) {
            val prefs = graph.preferences.preferences().copy(levelOnboardingDone = true)
            graph.preferences.save(prefs)
            _ui.update {
                it.copy(
                    screen = if (it.screen == Screen.ONBOARDING) Screen.LIBRARY else it.screen,
                    settings = settingsOf(prefs, uiLanguage(prefs)),
                )
            }
        }
    }

    // --- store / book sources --------------------------------------------

    fun refreshStore() {
        viewModelScope.launch(Dispatchers.IO) {
            val strings = stringsFor(uiLanguage(graph.preferences.preferences()))
            _ui.update { it.copy(store = it.store.copy(refreshing = true, error = null, notice = null)) }
            val results = graph.sourceManager.refreshAll()
            val ok = results.mapNotNull { (_, result) -> (result as? SourceResult.Ok)?.value }
            val failures = results.mapNotNull { (_, result) -> (result as? SourceResult.Err)?.error }
            storeSnapshots = ok
            val store = buildStoreState(
                strings = strings,
                language = uiLanguage(graph.preferences.preferences()),
                refreshing = false,
                error = if (ok.isEmpty() && failures.isNotEmpty()) {
                    storeErrorText(strings, failures.minByOrNull(::storeErrorPriority)!!)
                } else {
                    null
                },
                notice = if (ok.isNotEmpty() && failures.isNotEmpty()) strings.storePartialError else null,
            )
            _ui.update { it.copy(store = store) }
        }
    }

    fun setSourceLine(line: SourceLine) {
        graph.sourceManager.setLine(line)
        storeSnapshots = emptyList()
        _ui.update {
            it.copy(store = buildStoreState(stringsFor(uiLanguage(graph.preferences.preferences())), uiLanguage(graph.preferences.preferences())))
        }
        refreshStore()
    }

    fun setAllowLocalHttp(allowed: Boolean) {
        graph.sourceManager.setAllowLocalHttp(allowed)
        _ui.update {
            it.copy(store = it.store.copy(allowLocalHttp = allowed))
        }
    }

    fun addCustomSource(name: String, url: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val strings = stringsFor(uiLanguage(graph.preferences.preferences()))
            when (val result = graph.sourceManager.addCustomSource(name, url)) {
                is SourceResult.Ok -> {
                    _ui.update { it.copy(store = buildStoreState(strings, uiLanguage(graph.preferences.preferences()))) }
                    refreshStore()
                }
                is SourceResult.Err -> _ui.update {
                    it.copy(store = it.store.copy(error = storeErrorText(strings, result.error)))
                }
            }
        }
    }

    fun toggleCustomSource(id: String, enabled: Boolean) {
        graph.sourceManager.setCustomSourceEnabled(id, enabled)
        _ui.update {
            it.copy(store = buildStoreState(stringsFor(uiLanguage(graph.preferences.preferences())), uiLanguage(graph.preferences.preferences())))
        }
        if (enabled) refreshStore()
    }

    fun deleteCustomSource(id: String) {
        graph.sourceManager.removeCustomSource(id)
        storeSnapshots = storeSnapshots.filterNot { it.sourceId == id }
        _ui.update {
            it.copy(store = buildStoreState(stringsFor(uiLanguage(graph.preferences.preferences())), uiLanguage(graph.preferences.preferences())))
        }
    }

    /** Install a `.pagelence` package the user picked from local storage. */
    fun importLocalPackage(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val strings = stringsFor(uiLanguage(graph.preferences.preferences()))
            _ui.update { it.copy(store = it.store.copy(importing = true, error = null, notice = null)) }
            val bytes = graph.readPackageFile(uri)
            val result = if (bytes == null) {
                SourceResult.Err(SourceError.UnreadableFile(uri.toString()))
            } else {
                graph.sourceManager.importPackage(bytes, ImportedSource.LOCAL)
            }
            finishImport(result, strings)
        }
    }

    /** Download and install one `.pagelence` file from a direct HTTPS link. */
    fun importOnlinePackage(url: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val strings = stringsFor(uiLanguage(graph.preferences.preferences()))
            _ui.update { it.copy(store = it.store.copy(importing = true, error = null, notice = null)) }
            finishImport(graph.sourceManager.importFromUrl(url), strings)
        }
    }

    private fun finishImport(result: SourceResult<InstalledBook>, strings: Strings) {
        val language = uiLanguage(graph.preferences.preferences())
        when (result) {
            is SourceResult.Ok -> {
                graph.refreshContent()
                _ui.update {
                    it.copy(
                        store = buildStoreState(
                            strings = strings,
                            language = language,
                            importing = false,
                            notice = strings.storeImported.format(titleFor(result.value.title, language)),
                        ),
                    )
                }
                refreshLibrary()
            }
            is SourceResult.Err -> _ui.update {
                it.copy(store = it.store.copy(importing = false, error = storeErrorText(strings, result.error)))
            }
        }
    }

    /** Download and install one catalog book; [sourceId] null means the first source. */
    fun downloadBook(bookId: String, sourceId: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            val strings = stringsFor(uiLanguage(graph.preferences.preferences()))
            val entry = CatalogRules.merge(storeSnapshots).firstOrNull { it.book.id == bookId }
                ?: return@launch
            val source = sourceId ?: entry.sources.firstOrNull() ?: return@launch
            _ui.update { it.copy(store = it.store.copy(downloadingId = bookId, error = null, notice = null)) }
            refreshBookDetail(bookId)
            when (val result = graph.sourceManager.download(entry.book, source)) {
                is SourceResult.Ok -> {
                    graph.refreshContent()
                    val installed = buildStoreState(
                        strings = strings,
                        language = uiLanguage(graph.preferences.preferences()),
                        notice = strings.storeDownloaded,
                        downloadingId = null,
                    )
                    _ui.update { it.copy(store = installed) }
                    refreshLibrary()
                    refreshBookDetail(bookId)
                }
                is SourceResult.Err -> {
                    _ui.update {
                        it.copy(
                            store = it.store.copy(
                                downloadingId = null,
                                error = storeErrorText(strings, result.error),
                            ),
                        )
                    }
                    refreshBookDetail(bookId)
                }
            }
        }
    }

    /** Download the book whose detail page is open, from a source or the first one. */
    fun downloadFromDetail(sourceId: String?) {
        val detail = _ui.value.bookDetail ?: return
        downloadBook(detail.bookId, sourceId)
    }

    fun removeDownloadedBook(bookId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val strings = stringsFor(uiLanguage(graph.preferences.preferences()))
            graph.sourceManager.removeInstalled(bookId)
            graph.refreshContent()
            _ui.update {
                it.copy(store = buildStoreState(strings, uiLanguage(graph.preferences.preferences())))
            }
            refreshLibrary()
            refreshBookDetail(bookId)
        }
    }

    private fun storePresenter(strings: Strings, language: UiLanguage) = StorePresenter(
        content = graph.content,
        sources = graph.sourceManager,
        catalog = graph.builtInCatalog,
        strings = strings,
        language = language,
    )

    private fun buildStoreState(
        strings: Strings,
        language: UiLanguage,
        refreshing: Boolean = false,
        error: String? = null,
        notice: String? = null,
        downloadingId: String? = _ui.value.store.downloadingId,
        importing: Boolean = _ui.value.store.importing,
    ): StoreUiState = storePresenter(strings, language).store(
        snapshots = storeSnapshots,
        refreshing = refreshing,
        error = error,
        notice = notice,
        downloadingId = downloadingId,
        importing = importing,
        finishedIds = _ui.value.library.finished.mapTo(mutableSetOf()) { it.id },
    )

    // --- assessment ------------------------------------------------------

    fun openAssessment() {
        _ui.update { it.copy(assessment = AssessmentUiState()) }
        push(Screen.ASSESSMENT)
    }

    /** Draw a quick/full plan from the shared bank and show the first question. */
    fun startAssessment(mode: AssessmentMode) {
        viewModelScope.launch(Dispatchers.IO) {
            val profile = graph.profile.profile()
            val bank = graph.assessment.bank()
            if (bank.questions.isEmpty()) return@launch
            val level = profile.selfLevel ?: CefrLevel.B1
            val recent = graph.profile.recentAssessmentIds()
            val plan = AssessmentRules.plan(bank, mode, level, recent = recent)
            // A question enters the window as soon as it is shown, so exiting and
            // restarting cannot reroll the same paper.
            graph.profile.recordAssessmentIds(plan.questions.map { it.id })
            val strings = stringsFor(uiLanguage(graph.preferences.preferences()))
            assessmentPlan = plan
            assessmentAnswers.clear()
            assessmentResult = null
            val questions = AssessmentPresenter(strings).questions(plan)
            withContext(Dispatchers.Main) {
                _ui.update {
                    it.copy(
                        assessment = it.assessment.copy(
                            active = true,
                            mode = mode,
                            questions = questions,
                            index = 0,
                            submitted = false,
                            result = null,
                        ),
                    )
                }
            }
        }
    }

    fun selectAssessmentOption(option: Int) {
        val current = _ui.value.assessment.current ?: return
        val question = assessmentPlan?.questions?.firstOrNull { it.id == current.id } ?: return
        if (option !in question.options.indices) return
        assessmentAnswers[current.id] = option
        _ui.update { state ->
            val assessment = state.assessment
            state.copy(
                assessment = assessment.copy(
                    questions = assessment.questions.map { item ->
                        if (item.id == current.id) item.copy(selected = option, skipped = false) else item
                    },
                ),
            )
        }
    }

    /**
     * "Not sure" is a first-class answer: it clears any picked option, records
     * no guess, counts as wrong in scoring, and moves on immediately.
     */
    fun skipAssessmentQuestion() {
        val current = _ui.value.assessment.current ?: return
        assessmentPlan?.questions?.firstOrNull { it.id == current.id } ?: return
        assessmentAnswers.remove(current.id)
        _ui.update { state ->
            val assessment = state.assessment
            state.copy(
                assessment = assessment.copy(
                    questions = assessment.questions.map { item ->
                        if (item.id == current.id) item.copy(selected = null, skipped = true) else item
                    },
                ),
            )
        }
        nextAssessmentQuestion()
    }

    fun nextAssessmentQuestion() {
        val assessment = _ui.value.assessment
        if (!assessment.active) return
        if (assessment.index < assessment.questions.lastIndex) {
            _ui.update { it.copy(assessment = it.assessment.copy(index = it.assessment.index + 1)) }
        } else {
            submitAssessment()
        }
    }

    fun previousAssessmentQuestion() {
        _ui.update { state ->
            val assessment = state.assessment
            if (!assessment.active || assessment.index == 0) {
                state
            } else {
                state.copy(assessment = assessment.copy(index = assessment.index - 1))
            }
        }
    }

    /** Back from the test: confirm when answers would be lost. */
    fun requestCloseAssessment() {
        val assessment = _ui.value.assessment
        if (assessment.active && assessment.answeredCount > 0) {
            _ui.update { it.copy(assessment = it.assessment.copy(exitConfirm = true)) }
        } else {
            closeAssessment()
        }
    }

    fun dismissAssessmentExit() {
        _ui.update { it.copy(assessment = it.assessment.copy(exitConfirm = false)) }
    }

    fun closeAssessment() {
        finishAssessment()
    }

    private fun submitAssessment() {
        val plan = assessmentPlan ?: return
        val result = AssessmentRules.evaluate(plan, assessmentAnswers)
        assessmentResult = result
        val strings = stringsFor(uiLanguage(graph.preferences.preferences()))
        _ui.update {
            it.copy(
                assessment = it.assessment.copy(
                    active = false,
                    submitted = true,
                    result = AssessmentPresenter(strings).result(result),
                    index = it.assessment.questions.lastIndex.coerceAtLeast(0),
                ),
            )
        }
    }

    /** Accept the suggested level; the mode changes are confirmed in the same prompt. */
    fun acceptAssessmentResult() {
        val result = assessmentResult ?: return
        requestLevel(result.suggested, result.mode.levelSource(), result.scorePercent)
    }

    /** Keep the current level but record the score and time. */
    fun keepAssessmentResult() {
        val result = assessmentResult ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val profile = graph.profile.profile()
            graph.profile.setLevel(
                profile.selfLevel,
                profile.levelSource,
                result.scorePercent,
                Instant.now().toString(),
            )
            withContext(Dispatchers.Main) { finishAssessment() }
        }
    }

    private fun finishAssessment() {
        assessmentPlan = null
        assessmentAnswers.clear()
        assessmentResult = null
        back()
        refreshLibrary()
    }

    // --- grammar tab -----------------------------------------------------

    fun openGrammarPoint(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val language = uiLanguage(graph.preferences.preferences())
            val all = graph.content.grammarPoints()
            val point = all.firstOrNull { it.id == id } ?: return@launch
            val ui = GrammarPresenter(all, language).point(point)
            withContext(Dispatchers.Main) {
                _ui.update { it.copy(grammarPoint = ui) }
                push(Screen.GRAMMAR_POINT)
            }
        }
    }


    fun openGrammarDetail(bookId: String, unitId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val prefs = graph.preferences.preferences()
            val language = uiLanguage(prefs)
            val detail = buildGrammarDetail(bookId, unitId, language) ?: return@launch
            withContext(Dispatchers.Main) {
                _ui.update { it.copy(grammarDetail = detail) }
                push(Screen.GRAMMAR_DETAIL)
            }
        }
    }

    private fun buildGrammarDetail(bookId: String, unitId: String, language: UiLanguage): GrammarDetailUi? {
        val bookIndex = graph.content.index(bookId) ?: return null
        val unit = bookIndex.unit(unitId) ?: return null
        val grammar = unit.grammar ?: return null
        val chapter = bookIndex.chapterByUnitId[unit.id]
        return GrammarDetailUi(
            bookId = bookId,
            bookTitle = titleFor(bookIndex.book.title, language),
            unitId = unit.id,
            unitTitle = unit.title?.let { titleFor(it, language) } ?: unit.id,
            chapterTitle = chapter?.title?.let { titleFor(it, language) } ?: "",
            grammar = readerPresenter(bookId).grammar(grammar),
        )
    }

    private fun buildGrammarTab(strings: Strings, language: UiLanguage): GrammarTabUiState {
        val all = graph.content.grammarPoints()
        return GrammarTabUiState(
            points = GrammarPresenter(all, language).tab().points,
            loading = false,
        )
    }

    fun onWordClick(
        paragraphId: String,
        sentenceNumber: Int,
        spelling: String,
        start: Int,
        end: Int,
    ) {
        val reader = _ui.value.reader ?: return
        val bookIndex = index ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val language = uiLanguage(graph.preferences.preferences())
            val result = WordPanelPresenter(
                content = graph.content,
                strings = stringsFor(language),
            ).open(
                index = bookIndex,
                bookId = reader.bookId,
                unitId = reader.unitId,
                paragraphId = paragraphId,
                sentenceNumber = sentenceNumber,
                spelling = spelling,
                start = start,
                end = end,
            ) ?: return@launch
            pendingPanel = result.panel
            wordSelection = WordSelectionState.select(result.selected)
            recordLookup(reader.bookId, reader.unitId, paragraphId, sentenceNumber, result.panel, start, end)
            _ui.update { state ->
                state.reader?.let { current ->
                    state.copy(
                        reader = current.copy(
                            // Tapping a word opens its entry right away; the old
                            // flow required a second tap on the top strip, which
                            // read as "nothing happened".
                            wordPanel = result.panel,
                            wordStrip = result.selected.toStripUi(),
                            selectedWord = result.selected.toSelectedUi(),
                        ),
                    )
                } ?: state
            }
        }
    }

    /** Open the full sheet for the word currently shown in the top strip. */
    fun openWordPanel() {
        if (wordSelection is WordSelectionState.Hidden) return
        val panel = pendingPanel ?: return
        wordSelection = WordSelectionState.openDetails(wordSelection)
        _ui.update { state ->
            state.reader?.let { state.copy(reader = it.copy(wordPanel = panel)) } ?: state
        }
    }

    fun closeWordPanel() {
        wordSelection = WordSelectionState.clear()
        _ui.update { state ->
            state.reader?.let {
                state.copy(
                    reader = it.copy(
                        wordPanel = null,
                        wordStrip = null,
                        selectedWord = null,
                    ),
                )
            } ?: state
        }
    }

    /** Remember this lookup for 复习 → 查词; failures never break the panel (T161). */
    private fun recordLookup(
        bookId: String,
        unitId: String,
        paragraphId: String,
        sentenceNumber: Int,
        panel: WordPanelUi,
        start: Int,
        end: Int,
    ) {
        runCatching {
            val gloss = panel.gloss ?: panel.senses
                .firstOrNull { it.language == "zh" && !it.fromSurface }
                ?.definition
            graph.lookups.record(
                LookupEntry(
                    id = LookupHistoryRules.idOf(bookId, panel.lemma, panel.term),
                    bookId = bookId,
                    unitId = unitId,
                    paragraphId = paragraphId,
                    sentenceNumber = sentenceNumber,
                    term = panel.term,
                    lemma = panel.lemma,
                    start = start,
                    end = end,
                    gloss = gloss,
                    ipa = panel.ipa,
                    lookedUpAt = Instant.now().toString(),
                ),
            )
        }
    }

    /**
     * History row → the same sentence with its word panel open (T161). A removed
     * book is not tappable (the presenter marks it), so this only runs for books
     * still installed.
     */
    fun openLookup(entry: LookupHistoryUi) {
        if (!entry.available) return
        if (runCatching { graph.content.index(entry.bookId) }.getOrNull() == null) return
        val reader = _ui.value.reader
        val jump = ReaderJumpUi(
            paragraphId = entry.paragraphId,
            sentenceNumber = entry.sentenceNumber,
            token = ++annotationJumpToken,
        )
        when {
            reader != null && reader.bookId == entry.bookId && reader.unitId == entry.unitId -> {
                _ui.update { state ->
                    state.reader?.let { state.copy(reader = it.copy(jumpTarget = jump)) } ?: state
                }
                onWordClick(entry.paragraphId, entry.sentenceNumber, entry.term, entry.start, entry.end)
            }

            reader != null && reader.bookId == entry.bookId ->
                openBookAt(entry.bookId, entry.unitId, push = false, jump = jump, lookup = entry)

            else -> openBookAt(entry.bookId, entry.unitId, push = true, jump = jump, lookup = entry)
        }
    }

    fun clearLookups() {
        viewModelScope.launch(Dispatchers.IO) {
            graph.lookups.clear()
            val lookups = currentLookupHistory(uiLanguage(graph.preferences.preferences()))
            withContext(Dispatchers.Main) { _ui.update { it.copy(lookupHistory = lookups) } }
        }
    }

    fun toggleCard() {
        val reader = _ui.value.reader ?: return
        val panel = reader.wordPanel ?: return
        val bookIndex = index ?: return
        val unit = bookIndex.unit(reader.unitId) ?: return
        val paragraph = bookIndex.paragraphById[panel.paragraphId] ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val hint = paragraph.hints.firstOrNull { it.target.equals(panel.term, ignoreCase = true) }
            val hintId = hint?.id ?: lookupHintId(panel.term)
            val existing = graph.cards.find(reader.bookId, panel.lemma, panel.term)
            if (panel.collected) {
                val cardId = existing?.id ?: WordCard.idOf(reader.bookId, paragraph.id, hintId)
                graph.cards.remove(cardId, Instant.now().toString())
            } else if (existing == null) {
                val now = Instant.now().toString()
                val baseZh = panel.senses.firstOrNull { it.language == "zh" && !it.fromSurface }
                val baseEn = panel.senses.firstOrNull { it.language == "en" && !it.fromSurface }
                graph.cards.add(
                    WordCard(
                        id = WordCard.idOf(reader.bookId, paragraph.id, hintId),
                        bookId = reader.bookId,
                        paragraphId = paragraph.id,
                        hintId = hintId,
                        unitId = unit.id,
                        term = panel.term,
                        lemma = panel.lemma,
                        pos = panel.pos,
                        zh = hint?.zh ?: panel.gloss ?: baseZh?.definition,
                        en = hint?.en ?: baseEn?.definition,
                        ipa = panel.ipa,
                        example = hint?.example,
                        createdAt = now,
                        updatedAt = now,
                    ),
                )
            }
            val counts = graph.cards.list(reader.bookId).size
            val collected = graph.cards.find(reader.bookId, panel.lemma, panel.term) != null
            pendingPanel = pendingPanel?.copy(collected = collected)
            _ui.update { state ->
                state.reader?.let { current ->
                    state.copy(
                        reader = current.copy(
                            cardsCount = counts,
                            wordPanel = current.wordPanel?.copy(collected = collected),
                        ),
                    )
                } ?: state
            }
        }
    }

    fun speakWord() {
        val term = pendingPanel?.term ?: WordSelectionState.word(wordSelection)?.term
        if (term.isNullOrBlank()) return
        speakText(term)
    }

    fun speakSentence(paragraphId: String, sentenceNumber: Int) {
        val reader = _ui.value.reader ?: return
        val bookIndex = index ?: return
        val sentence = bookIndex.sentence(reader.unitId, sentenceNumber)?.text ?: return
        speakText(sentence)
    }

    fun stopSpeaking() {
        graph.speech.stop()
    }

    private fun speakText(text: String) {
        val prefs = graph.preferences.preferences()
        val strings = stringsFor(uiLanguage(prefs))
        val notice = when (graph.speech.speak(text, prefs.speechRate)) {
            SpeechController.Result.SPOKEN -> null
            SpeechController.Result.NO_ENGLISH_ENGINE -> strings.speechEngineMissing
            SpeechController.Result.NOT_READY -> strings.speechNotReady
            SpeechController.Result.EMPTY -> null
        } ?: return
        _ui.update { state ->
            state.reader?.let { state.copy(reader = it.copy(speechNotice = notice)) } ?: state
        }
    }

    fun speechNoticeShown() {
        _ui.update { state ->
            state.reader?.let { state.copy(reader = it.copy(speechNotice = null)) } ?: state
        }
    }

    /** A page turn clears the temporary word strip and stops speech. */
    /**
     * Called whenever the reader changes phase or text page. Also closes the
     * word panel and saves the resume point, so backing out returns to the same
     * screen next time.
     */
    fun onReaderPageChanged(
        phase: ReaderPhase,
        textPage: Int,
        textProgress: Float,
        anchorParagraphId: String? = null,
    ) {
        graph.speech.stop()
        val reader = _ui.value.reader ?: return
        pendingPanel = null
        wordSelection = WordSelectionState.clear()
        val updated = reader.copy(
            resumePhase = phase,
            resumeTextPage = textPage,
            resumeTextProgress = textProgress,
            wordPanel = null,
            wordStrip = null,
            selectedWord = null,
        )
        _ui.update { it.copy(reader = updated) }
        saveReaderPosition(updated, paragraphId = anchorParagraphId)
    }

    /** Continuous scroll mode reports its normalised offset when scrolling stops. */
    fun onReaderTextProgress(progress: Float) {
        val reader = _ui.value.reader ?: return
        val value = progress.coerceIn(0f, 1f)
        if (kotlin.math.abs(reader.resumeTextProgress - value) < 0.002f) return
        val updated = reader.copy(resumeTextProgress = value)
        _ui.update { it.copy(reader = updated) }
        saveReaderPosition(updated)
    }

    private fun savePointOf(
        reader: ReaderUiState,
        paragraphId: String? = null,
        answers: Map<Int, Int> = quizAnswers,
        submitted: Boolean = quizSubmitted,
    ): ReaderSavePoint = ReaderSavePoint(
        bookId = reader.bookId,
        unitId = reader.unitId,
        phase = reader.resumePhase,
        textPage = reader.resumeTextPage,
        textProgress = reader.resumeTextProgress,
        paragraphId = paragraphId,
        answers = answers,
        submitted = submitted,
    )

    /**
     * Queue the current reader position. Page turns use the debounce, while
     * answers and exits ask for an immediate write. The snapshot is taken now
     * because switching units replaces the globals.
     */
    private fun saveReaderPosition(
        reader: ReaderUiState,
        immediate: Boolean = false,
        paragraphId: String? = null,
    ) {
        pendingReaderSave.set(savePointOf(reader, paragraphId))
        readerSaveJob?.cancel()
        readerSaveJob = viewModelScope.launch(Dispatchers.IO) {
            if (!immediate) delay(SAVE_DEBOUNCE_MS)
            progressWriteLock.withLock {
                pendingReaderSave.getAndSet(null)?.let(::persistReaderSavePoint)
            }
        }
    }

    /**
     * Write anything still queued. Called from `ON_STOP` and `onCleared`, so it
     * briefly blocks the caller: the state document is small and losing the
     * last page turn is worse than a few milliseconds on the way out.
     */
    fun flushProgress() {
        val point = pendingReaderSave.getAndSet(null) ?: return
        readerSaveJob?.cancel()
        runCatching {
            runBlocking {
                withTimeoutOrNull(FLUSH_PROGRESS_TIMEOUT_MS) {
                    progressWriteLock.withLock { persistReaderSavePoint(point) }
                }
            }
        }
    }

    private fun clearPendingReaderSave() {
        pendingReaderSave.set(null)
        readerSaveJob?.cancel()
    }

    /**
     * Write one resume point with the answers captured at schedule time.
     *
     * [now] is passed only when the caller is finishing the unit (leaving the
     * reader or moving on); a page turn or quiz answer passes null so browsing
     * the last grammar page never stamps a finish date.
     */
    private fun persistReaderSavePoint(point: ReaderSavePoint, now: String? = null) {
        // A save from the previous book may still be queued after a switch.
        val bookIndex = index?.takeIf { it.book.id == point.bookId } ?: return
        graph.readerSession.save(
            com.pagelence.presentation.reader.ResumePoint(
                bookId = point.bookId,
                unitId = point.unitId,
                phase = point.phase,
                textPage = point.textPage,
                textProgress = point.textProgress,
                paragraphId = point.paragraphId,
                answers = point.answers,
                submitted = point.submitted,
            ),
            bookIndex,
            completedAt = now,
        )
    }

    fun selectOption(questionIndex: Int, optionIndex: Int) {
        if (quizSubmitted) return
        quizAnswers = quizAnswers + (questionIndex to optionIndex)
        rebuildQuiz()
        _ui.value.reader?.let { saveReaderPosition(it, immediate = true) }
    }

    fun submitQuiz() {
        val reader = _ui.value.reader ?: return
        if (quizSubmitted || quizAnswers.size != reader.quiz.size) return
        quizSubmitted = true
        quizCollected = 0
        rebuildQuiz()
        _ui.value.reader?.let { saveReaderPosition(it, immediate = true) }
    }

    /** Clear the missed answers only; correct ones stay so the retry is short. */
    fun redoWrongQuiz() {
        val reader = _ui.value.reader ?: return
        if (!quizSubmitted) return
        val correctOptions = reader.quiz.map { it.correct }
        if (QuizGrading.grade(quizAnswers, correctOptions).allCorrect) return
        quizAnswers = QuizGrading.redoWrong(quizAnswers, correctOptions)
        quizSubmitted = false
        quizCollected = 0
        rebuildQuiz()
        _ui.value.reader?.let { saveReaderPosition(it, immediate = true) }
    }

    fun collectQuizWords() {
        val reader = _ui.value.reader ?: return
        val bookIndex = index ?: return
        val unit = bookIndex.unit(reader.unitId) ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val seen = mutableSetOf<String>()
            val candidates = readerPresenter(reader.bookId).quizCandidates(unit)
                .filter { candidate ->
                    graph.cards.find(reader.bookId, candidate.lemma, candidate.term) == null &&
                        seen.add(WordCard.lemmaKey(candidate.lemma))
                }
            if (candidates.isEmpty()) return@launch
            val now = Instant.now().toString()
            candidates.forEach { candidate ->
                graph.cards.add(
                    WordCard(
                        id = WordCard.idOf(reader.bookId, candidate.paragraphId, candidate.hintId),
                        bookId = reader.bookId,
                        paragraphId = candidate.paragraphId,
                        hintId = candidate.hintId,
                        unitId = unit.id,
                        term = candidate.term,
                        lemma = candidate.lemma,
                        pos = candidate.pos,
                        zh = candidate.zh,
                        en = candidate.en,
                        ipa = candidate.ipa,
                        example = candidate.example,
                        createdAt = now,
                        updatedAt = now,
                    ),
                )
            }
            quizCollected += candidates.size
            val counts = graph.cards.list(reader.bookId).size
            _ui.update { state ->
                state.reader?.let { current ->
                    state.copy(
                        reader = current.copy(
                            quizCollectedCount = quizCollected,
                            cardsCount = counts,
                        ),
                    )
                } ?: state
            }
        }
    }

    private fun rebuildQuiz() {
        val reader = _ui.value.reader ?: return
        val score = QuizGrading.grade(quizAnswers, reader.quiz.map { it.correct }).correctCount
        val questions = reader.quiz.mapIndexed { index, question ->
            question.copy(
                selected = quizAnswers[index],
                revealed = quizSubmitted,
            )
        }
        _ui.update {
            it.copy(
                reader = it.reader?.copy(
                    quiz = questions,
                    quizSubmitted = quizSubmitted,
                    quizScore = score,
                    quizCollectedCount = quizCollected,
                ),
            )
        }
    }

    fun nextUnit() {
        graph.speech.stop()
        val reader = _ui.value.reader ?: return
        val bookIndex = index ?: return
        clearPendingReaderSave()
        viewModelScope.launch(Dispatchers.IO) {
            var justFinished = false
            val advanced = progressWriteLock.withLock {
                val current = graph.progress.progress(reader.bookId)
                val wasRead = reader.unitId in current.readUnitIds
                val next = ProgressSemantics.advance(
                    current,
                    bookIndex,
                    reader.unitId,
                    Instant.now().toString(),
                )
                graph.progress.save(next)
                if (!wasRead) graph.profile.recordCompletedUnit(today())
                justFinished = ProgressSemantics.justFinished(current, next)
                next
            }
            if (justFinished) {
                showFinished(reader.bookId, fromReader = true)
                return@launch
            }
            val next = bookIndex.nextUnit(reader.unitId)
            if (next == null) {
                withContext(Dispatchers.Main) { leaveReader() }
                return@launch
            }
            quizAnswers = emptyMap()
            quizSubmitted = false
            quizCollected = 0
            pendingPanel = null
            wordSelection = WordSelectionState.clear()
            val nextReader = buildReader(bookIndex, next, advanced, startAtGuide = true)
            withContext(Dispatchers.Main) {
                _ui.update { it.copy(screen = Screen.READER, reader = nextReader) }
            }
        }
    }

    /**
     * Swipe back from a unit's first page: save where this unit stopped, then
     * open the previous unit at its grammar page. The first unit has no
     * previous target and never reaches here.
     */
    fun previousUnit() {
        graph.speech.stop()
        val reader = _ui.value.reader ?: return
        val bookIndex = index ?: return
        val answers = quizAnswers
        val submitted = quizSubmitted
        clearPendingReaderSave()
        viewModelScope.launch(Dispatchers.IO) {
            val previous = bookIndex.previousUnit(reader.unitId) ?: return@launch
            progressWriteLock.withLock {
                persistReaderSavePoint(savePointOf(reader, answers = answers, submitted = submitted))
            }
            val progress = graph.progress.progress(reader.bookId)
            val resume = progress.reader?.takeIf { it.unitId == previous.id }
            quizAnswers = resume?.quizAnswers ?: emptyMap()
            quizSubmitted = resume?.quizSubmitted ?: false
            quizCollected = 0
            pendingPanel = null
            wordSelection = WordSelectionState.clear()
            val previousReader = buildReader(
                bookIndex = bookIndex,
                unit = previous,
                progress = progress,
                startAtGrammar = true,
            )
            withContext(Dispatchers.Main) {
                _ui.update { it.copy(screen = Screen.READER, reader = previousReader) }
            }
        }
    }

    fun stopHere() {
        val reader = _ui.value.reader ?: return
        val bookIndex = index ?: return
        clearPendingReaderSave()
        viewModelScope.launch(Dispatchers.IO) {
            var justFinished = false
            progressWriteLock.withLock {
                val current = graph.progress.progress(reader.bookId)
                val wasRead = reader.unitId in current.readUnitIds
                val next = ProgressSemantics.markUnitRead(
                    current,
                    bookIndex,
                    reader.unitId,
                    Instant.now().toString(),
                )
                graph.progress.save(next)
                if (!wasRead) graph.profile.recordCompletedUnit(today())
                justFinished = ProgressSemantics.justFinished(current, next)
            }
            if (justFinished) showFinished(reader.bookId, fromReader = true)
        }
    }

    // --- finished book ----------------------------------------------------

    /** Build and show the ending page; from the reader it replaces that screen. */
    private suspend fun showFinished(bookId: String, fromReader: Boolean) {
        val finished = buildFinishedBook(bookId)
        if (finished == null) {
            if (fromReader) leaveReaderWithoutFinishing()
            return
        }
        val profile = buildProfile()
        withContext(Dispatchers.Main) {
            graph.speech.stop()
            clearPendingReaderSave()
            pendingPanel = null
            wordSelection = WordSelectionState.clear()
            val stack = currentStack()
            if (fromReader && stack.lastOrNull() == Screen.READER) stack.removeAt(stack.lastIndex)
            if (stack.lastOrNull() != Screen.BOOK_FINISHED) stack.add(Screen.BOOK_FINISHED)
            _ui.update {
                it.copy(
                    screen = Screen.BOOK_FINISHED,
                    reader = null,
                    readerExitConfirm = false,
                    finishedBook = finished,
                    profile = profile,
                )
            }
        }
        refreshLibrary()
    }

    private fun leaveReaderWithoutFinishing() {
        _ui.value.reader?.let { saveReaderPosition(it, immediate = true) }
        val next = popScreen() ?: rootScreen(_ui.value.tab)
        _ui.update { it.copy(screen = next, reader = null, readerExitConfirm = false) }
        refreshLibrary()
    }

    private fun buildFinishedBook(bookId: String): FinishedBookUi? {
        val language = uiLanguage(graph.preferences.preferences())
        val strings = stringsFor(language)
        val base = FinishedPresenter(
            content = graph.content,
            progress = graph.progress,
            cards = graph.cards,
            language = language,
        ).finished(bookId) ?: return null
        val achievements = achievementPresenter(strings, language).forBook(bookId)
        return base.copy(
            noteCount = runCatching { graph.annotations.list(bookId).size }.getOrDefault(0),
            achievements = achievements.wall,
            newAchievementIds = achievements.newIds,
        )
    }


    fun openFinishedSummary() {
        val detail = _ui.value.bookDetail ?: return
        viewModelScope.launch(Dispatchers.IO) { showFinished(detail.bookId, fromReader = false) }
    }

    /** Leave the ending page for the shelf, wherever the book was opened from. */
    fun backToShelf() {
        val stack = currentStack()
        if (stack.lastOrNull() == Screen.BOOK_FINISHED) stack.removeAt(stack.lastIndex)
        if (_ui.value.tab == MainTab.SHELF) {
            // A detail page may sit under the ending page; the button promises
            // the shelf itself, not one page back.
            while (stack.size > 1) stack.removeAt(stack.lastIndex)
            _ui.update {
                it.copy(screen = Screen.LIBRARY, finishedBook = null, readerExitConfirm = false)
            }
            refreshLibrary()
        } else {
            _ui.update { it.copy(finishedBook = null, readerExitConfirm = false) }
            selectTab(MainTab.SHELF)
        }
    }

    /** Leave the ending page for the achievements wall in "我的". */
    fun openProfileFromFinished() {
        val stack = currentStack()
        if (stack.lastOrNull() == Screen.BOOK_FINISHED) stack.removeAt(stack.lastIndex)
        if (_ui.value.tab == MainTab.PROFILE) {
            while (stack.size > 1) stack.removeAt(stack.lastIndex)
            _ui.update {
                it.copy(
                    screen = Screen.PROFILE,
                    finishedBook = null,
                    readerExitConfirm = false,
                )
            }
        } else {
            _ui.update { it.copy(finishedBook = null, readerExitConfirm = false) }
            selectTab(MainTab.PROFILE)
        }
    }

    /** Leave the ending page for the store. */
    fun openStoreFromFinished() {
        val stack = currentStack()
        if (stack.lastOrNull() == Screen.BOOK_FINISHED) stack.removeAt(stack.lastIndex)
        if (_ui.value.tab == MainTab.STORE) {
            _ui.update {
                it.copy(
                    screen = stack.lastOrNull() ?: Screen.STORE,
                    finishedBook = null,
                    readerExitConfirm = false,
                )
            }
        } else {
            _ui.update { it.copy(finishedBook = null, readerExitConfirm = false) }
            selectTab(MainTab.STORE)
        }
    }

    /**
     * Start over from the first unit while keeping everything already read and
     * the original finish date: the shelf stays in "已读完", "我的" does not move.
     */
    fun rereadBook(bookId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val bookIndex = runCatching { graph.content.index(bookId) }.getOrNull()
            if (bookIndex == null) {
                withContext(Dispatchers.Main) {
                    _ui.update { it.copy(recovery = RecoveryNotice.CONTENT_FAILED) }
                }
                return@launch
            }
            val first = bookIndex.units.firstOrNull() ?: return@launch
            val stored = graph.progress.progress(bookId)
            graph.progress.save(
                stored.copy(
                    paragraphId = first.paragraphs.firstOrNull()?.id,
                    reader = null,
                ),
            )
            withContext(Dispatchers.Main) {
                val stack = currentStack()
                if (stack.lastOrNull() == Screen.BOOK_FINISHED) stack.removeAt(stack.lastIndex)
                _ui.update { it.copy(finishedBook = null) }
                openBookAt(bookId, unitId = first.id)
            }
        }
    }

    // --- cards -----------------------------------------------------------

    fun openCards() = openReview(ReviewTab.WORDS)

    fun deleteCard(cardId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            graph.cards.remove(cardId, Instant.now().toString())
            val review = currentReview()
            val reader = _ui.value.reader
            withContext(Dispatchers.Main) {
                _ui.update {
                    it.copy(
                        review = review,
                        reviewSession = null,
                        reader = reader?.copy(cardsCount = graph.cards.list(reader.bookId).size),
                    )
                }
            }
        }
    }

    private fun currentReview(): WordReviewUi = WordCardPresenter(
        content = graph.content,
        cards = graph.cards,
        language = uiLanguage(graph.preferences.preferences()),
    ).review()

    // --- spaced repetition (T143) ----------------------------------------

    /** Start a round over the queue the page just showed. */
    fun startReview() {
        val review = _ui.value.review
        if (!review.hasWork) return
        _ui.update { it.copy(reviewSession = ReviewSessionRules.start(review.queue)) }
    }

    fun revealCard() {
        _ui.update { state ->
            state.copy(reviewSession = state.reviewSession?.let(ReviewSessionRules::reveal))
        }
    }

    /**
     * Apply one grade and move on. The card is written immediately (one atomic
     * `state.json` write per grade), so killing the app mid-round keeps every
     * card already graded and leaves the rest due. The round itself is not
     * persisted: restarting recomputes the queue from the saved schedule.
     */
    fun gradeCard(grade: ReviewGrade) {
        val session = _ui.value.reviewSession ?: return
        if (!session.revealed) return
        val cardId = session.current?.id ?: return
        _ui.update { it.copy(reviewSession = ReviewSessionRules.grade(session, grade)) }
        val now = Instant.now().toString()
        val today = LocalDate.now()
        viewModelScope.launch(Dispatchers.IO) {
            val card = graph.cards.get(cardId)?.takeIf { !it.isDeleted } ?: return@launch
            graph.cards.add(Srs.grade(card, grade, today, now))
        }
    }

    /** Leave the round (or its summary) and refresh the counters. */
    fun closeReview() {
        _ui.update { it.copy(reviewSession = null) }
        refreshCards()
    }

    // --- bookmarks, highlights and notes ---------------------------------

    fun selectAnnotationFilter(filter: AnnotationFilter) {
        _ui.update { it.copy(annotationFilter = filter) }
    }

    /**
     * Jump from a list row to the sentence. Same book + same unit reuses the open
     * reader; another unit of the same book swaps the unit without pushing a
     * second reader; another book opens it normally (the review tab is a valid
     * place to start reading from). A book that is no longer installed sends the
     * reader to the store instead of showing a misleading "content damaged".
     */
    fun openAnnotation(annotation: AnnotationUi) {
        if (runCatching { graph.content.index(annotation.bookId) }.getOrNull() == null) {
            selectTab(MainTab.STORE)
            return
        }
        val reader = _ui.value.reader
        val jump = ReaderJumpUi(
            paragraphId = annotation.paragraphId,
            sentenceNumber = annotation.sentenceNumber,
            token = ++annotationJumpToken,
        )
        when {
            reader != null && reader.bookId == annotation.bookId && reader.unitId == annotation.unitId ->
                _ui.update { state ->
                    state.reader?.let { state.copy(reader = it.copy(jumpTarget = jump)) } ?: state
                }

            reader != null && reader.bookId == annotation.bookId ->
                openBookAt(annotation.bookId, annotation.unitId, push = false, jump = jump)

            else -> openBookAt(annotation.bookId, annotation.unitId, push = true, jump = jump)
        }
    }

    /** The reader scrolled/landed on the requested sentence; do not repeat it. */
    fun annotationJumpHandled() {
        _ui.update { state ->
            state.reader?.let { state.copy(reader = it.copy(jumpTarget = null)) } ?: state
        }
    }

    fun toggleBookmark(paragraphId: String, sentenceNumber: Int) =
        toggleAnnotation(AnnotationKind.BOOKMARK, paragraphId, sentenceNumber)

    fun toggleHighlight(paragraphId: String, sentenceNumber: Int) =
        toggleAnnotation(AnnotationKind.HIGHLIGHT, paragraphId, sentenceNumber)

    private fun toggleAnnotation(
        kind: AnnotationKind,
        paragraphId: String,
        sentenceNumber: Int,
    ) {
        val reader = _ui.value.reader ?: return
        val bookIndex = index?.takeIf { it.book.id == reader.bookId } ?: return
        val unit = bookIndex.unit(reader.unitId) ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val id = AnnotationRules.idOf(reader.bookId, paragraphId, sentenceNumber, kind)
            val existing = graph.annotations.get(id)
            val now = Instant.now().toString()
            if (existing != null && !existing.isDeleted) {
                graph.annotations.remove(id, now)
            } else {
                val quote = AnnotationRules.sentenceText(bookIndex, unit.id, sentenceNumber)
                    ?: existing?.quote.orEmpty()
                graph.annotations.upsert(
                    AnnotationRules.create(
                        bookId = reader.bookId,
                        unitId = unit.id,
                        paragraphId = paragraphId,
                        sentenceNumber = sentenceNumber,
                        kind = kind,
                        quote = quote,
                        now = now,
                        previous = existing,
                    ),
                )
            }
            afterAnnotationChanged()
        }
    }

    fun saveNote(paragraphId: String, sentenceNumber: Int, note: String) {
        val text = AnnotationRules.normalizeNote(note) ?: return
        val reader = _ui.value.reader ?: return
        val bookIndex = index?.takeIf { it.book.id == reader.bookId } ?: return
        val unit = bookIndex.unit(reader.unitId) ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val id = AnnotationRules.idOf(reader.bookId, paragraphId, sentenceNumber, AnnotationKind.NOTE)
            val existing = graph.annotations.get(id)
            val quote = AnnotationRules.sentenceText(bookIndex, unit.id, sentenceNumber)
                ?: existing?.quote.orEmpty()
            graph.annotations.upsert(
                AnnotationRules.create(
                    bookId = reader.bookId,
                    unitId = unit.id,
                    paragraphId = paragraphId,
                    sentenceNumber = sentenceNumber,
                    kind = AnnotationKind.NOTE,
                    quote = quote,
                    now = Instant.now().toString(),
                    note = text,
                    previous = existing,
                ),
            )
            afterAnnotationChanged()
        }
    }

    fun deleteNote(paragraphId: String, sentenceNumber: Int) {
        val reader = _ui.value.reader ?: return
        viewModelScope.launch(Dispatchers.IO) {
            graph.annotations.remove(
                AnnotationRules.idOf(
                    reader.bookId,
                    paragraphId,
                    sentenceNumber,
                    AnnotationKind.NOTE,
                ),
                Instant.now().toString(),
            )
            afterAnnotationChanged()
        }
    }

    /**
     * Rebuild the live rows and per-sentence marks after a local edit. Marks are
     * drawn outside the text flow, so the page the reader is on does not move.
     */
    private fun afterAnnotationChanged() {
        val language = uiLanguage(graph.preferences.preferences())
        val presenter = annotationPresenter(language)
        val all = presenter.all(graph.annotations.all())
        val reader = _ui.value.reader
        val bookIndex = reader?.let { current -> index?.takeIf { it.book.id == current.bookId } }
        if (reader == null || bookIndex == null) {
            _ui.update { it.copy(annotations = all) }
            return
        }
        val stored = graph.annotations.list(reader.bookId)
        val marks = presenter.marks(stored, reader.bookId, reader.unitId)
        val paragraphs = reader.paragraphs.map { paragraph ->
            paragraph.copy(
                sentences = paragraph.sentences.map { sentence ->
                    sentence.copy(
                        mark = marks[AnnotationRules.sentenceKey(paragraph.id, sentence.number)],
                    )
                },
            )
        }
        _ui.update { state ->
            state.copy(
                annotations = all,
                reader = state.reader?.copy(
                    annotations = presenter.book(stored, reader.bookId),
                    paragraphs = paragraphs,
                ),
            )
        }
    }


    // --- settings --------------------------------------------------------

    fun openSettings() {
        val prefs = graph.preferences.preferences()
        _ui.update { it.copy(settings = settingsOf(prefs, uiLanguage(prefs))) }
        push(Screen.SETTINGS)
        refreshDictionary()
    }

    fun backFromSettings() = back()

    /** 设置 → 排版：four reading-surface choices with a live preview (T168). */
    fun openTypography() = push(Screen.SETTINGS_TYPOGRAPHY)

    /** 设置 → 完整词表：the download block moved off the settings home. */
    fun openDictionarySettings() {
        refreshDictionary()
        push(Screen.SETTINGS_DICTIONARY)
    }

    /** 我的 → 成就：the full wall behind the folded card. */
    fun openAchievements() = push(Screen.ACHIEVEMENTS)

    /** 读完页的「查看全部」直达成就页，而不是停在「我的」首屏。 */
    fun openAchievementsFromFinished() {
        val stack = currentStack()
        if (stack.lastOrNull() == Screen.BOOK_FINISHED) stack.removeAt(stack.lastIndex)
        if (_ui.value.tab == MainTab.PROFILE) {
            while (stack.size > 1) stack.removeAt(stack.lastIndex)
            _ui.update {
                it.copy(screen = Screen.PROFILE, finishedBook = null, readerExitConfirm = false)
            }
        } else {
            _ui.update { it.copy(finishedBook = null, readerExitConfirm = false) }
            selectTab(MainTab.PROFILE)
        }
        push(Screen.ACHIEVEMENTS)
    }

    /**
     * 恢复默认设置：only reading, typography, goal and appearance. Progress,
     * cards, notes, the full dictionary, sync and the interface language stay.
     */
    fun restoreDefaultSettings() {
        val prefs = graph.preferences.preferences().copy(
            fontSize = FontSize.MEDIUM,
            readingFont = ReadingFont.SANS,
            lineSpacing = LineSpacing.NORMAL,
            pageMargin = PageMargin.NORMAL,
            readerLayout = ReaderLayout.PAGED,
            readerLayoutChosen = true,
            definitionMode = DefinitionMode.ZH,
            guideMode = GuideMode.BILINGUAL,
            annotateHints = true,
            theme = ThemeMode.SYSTEM,
            dailyGoalMinutes = com.pagelence.core.prefs.DailyGoal.DEFAULT_MINUTES,
        )
        savePreferences(prefs)
    }

    // --- optional full dictionary ---------------------------------------

    /** Recompute the settings row from the manifest, partial and installed files. */
    fun refreshDictionary() {
        viewModelScope.launch(Dispatchers.IO) {
            val state = buildDictionaryState()
            _ui.update { it.copy(dictionary = state) }
        }
    }

    /** Download or resume; asks first when the active network is metered. */
    fun downloadDictionary() {
        val manifest = graph.fullDictManifest ?: return
        if (!graph.config.fullDictEnabled) return
        if (!graph.dictionaryNetworkUnmetered()) {
            _ui.update { it.copy(dictionary = it.dictionary.copy(confirmMetered = true)) }
            return
        }
        startDictionaryDownload(manifest)
    }

    fun confirmMeteredDictionaryDownload() {
        val manifest = graph.fullDictManifest ?: return
        _ui.update { it.copy(dictionary = it.dictionary.copy(confirmMetered = false)) }
        startDictionaryDownload(manifest)
    }

    fun dismissMeteredDictionaryDownload() {
        _ui.update { it.copy(dictionary = it.dictionary.copy(confirmMetered = false)) }
    }

    /** Pause keeps the partial file; the next tap resumes from that byte. */
    fun pauseDictionaryDownload() {
        graph.fullDictInstaller.cancel()
    }

    fun deleteFullDictionary() {
        viewModelScope.launch(Dispatchers.IO) {
            graph.fullDictInstaller.remove()
            graph.fullDictionary.close()
            graph.refreshContent()
            val state = buildDictionaryState()
            _ui.update { it.copy(dictionary = state) }
        }
    }

    private fun startDictionaryDownload(manifest: DictManifest) {
        if (dictionaryDownloadJob?.isActive == true) return
        dictionaryDownloadJob = viewModelScope.launch(Dispatchers.IO) {
            val strings = stringsFor(uiLanguage(graph.preferences.preferences()))
            var lastPercent = -1
            _ui.update {
                it.copy(
                    dictionary = it.dictionary.copy(
                        phase = DictionaryPhase.DOWNLOADING,
                        progress = 0f,
                        status = strings.dictDownloading.format(0),
                        confirmMetered = false,
                        canDownload = false,
                        canResume = false,
                        canPause = true,
                    ),
                )
            }
            val result = graph.fullDictInstaller.install(manifest, graph.config.fullDictEntryUrl) { done, total ->
                val percent = if (total <= 0) 0 else ((done * 100) / total).toInt().coerceIn(0, 100)
                if (percent != lastPercent) {
                    lastPercent = percent
                    _ui.update {
                        it.copy(
                            dictionary = it.dictionary.copy(
                                phase = DictionaryPhase.DOWNLOADING,
                                progress = percent / 100f,
                                status = strings.dictDownloading.format(percent),
                            ),
                        )
                    }
                }
            }
            // The job is finished from here on; clear it before building the
            // state, otherwise `isActive` still reports "downloading".
            dictionaryDownloadJob = null
            when (result) {
                DictInstallResult.Installed -> {
                    graph.fullDictionary.close()
                    // Drop cached WordQuery instances so lookups see the new table.
                    graph.refreshContent()
                    val state = buildDictionaryState()
                    _ui.update { it.copy(dictionary = state) }
                }

                DictInstallResult.Cancelled -> {
                    val state = buildDictionaryState()
                    _ui.update { it.copy(dictionary = state) }
                }

                is DictInstallResult.Failed -> {
                    val reason = dictionaryFailureText(result)
                    val state = buildDictionaryState().copy(
                        phase = DictionaryPhase.FAILED,
                        status = strings.dictFailed.format(reason),
                        canDownload = false,
                        canRetry = true,
                    )
                    _ui.update { it.copy(dictionary = state) }
                }
            }
        }
    }

    private fun dictionaryFailureText(failure: DictInstallResult.Failed): String =
        when (val rejection = failure.rejection) {
            is DictRejection.Schema -> "schema ${rejection.found} != ${rejection.supported}"
            is DictRejection.AppTooOld -> "app ${rejection.current} < ${rejection.required}"
            is DictRejection.Malformed -> rejection.field
            is DictRejection.SizeMismatch -> "size ${rejection.found} != ${rejection.expected}"
            DictRejection.ChecksumMismatch -> "sha256"
            null -> failure.reason ?: "unknown"
        }

    private fun buildDictionaryState(): DictionaryUiState {
        val language = uiLanguage(graph.preferences.preferences())
        return DictionaryPresenter(
            strings = stringsFor(language),
            appVersionCode = graph.config.appVersionCode,
            hostingEnabled = graph.config.fullDictEnabled,
        ).state(
            manifest = graph.fullDictManifest,
            installer = graph.fullDictInstaller,
            downloading = dictionaryDownloadJob?.isActive == true,
            progress = _ui.value.dictionary.progress,
        )
    }


    fun openAbout() = push(Screen.ABOUT)

    fun openAboutPrivacy() = push(Screen.ABOUT_PRIVACY)

    fun openAboutLicenses() = push(Screen.ABOUT_LICENSES)

    fun setLanguage(language: UiLanguage) {
        val prefs = graph.preferences.preferences()
            .copy(language = if (language == UiLanguage.EN) Language.EN else Language.ZH)
        savePreferences(prefs)
    }

    fun setDefinitionMode(mode: DefinitionMode) {
        savePreferences(graph.preferences.preferences().copy(definitionMode = mode))
    }

    fun setGuideMode(mode: GuideMode) {
        savePreferences(graph.preferences.preferences().copy(guideMode = mode))
    }

    fun setTheme(theme: ThemeMode) {
        savePreferences(graph.preferences.preferences().copy(theme = theme))
    }

    fun setFontSize(size: FontSize) {
        savePreferences(graph.preferences.preferences().copy(fontSize = size))
    }

    fun setReadingFont(font: ReadingFont) {
        savePreferences(graph.preferences.preferences().copy(readingFont = font))
    }

    fun setLineSpacing(spacing: LineSpacing) {
        savePreferences(graph.preferences.preferences().copy(lineSpacing = spacing))
    }

    fun setPageMargin(margin: PageMargin) {
        savePreferences(graph.preferences.preferences().copy(pageMargin = margin))
    }

    /** 全部句译: one switch for every sentence translation in the current unit (T159). */
    fun toggleAllTranslations() {
        val prefs = graph.preferences.preferences()
        savePreferences(prefs.copy(showAllTranslations = !prefs.showAllTranslations))
    }

    fun setReaderLayout(layout: ReaderLayout) {
        savePreferences(
            graph.preferences.preferences().copy(
                readerLayout = layout,
                readerLayoutChosen = true,
            ),
        )
    }

    fun setAnnotateHints(enabled: Boolean) {
        savePreferences(graph.preferences.preferences().copy(annotateHints = enabled))
    }

    fun setSpeechRate(rate: Float) {
        savePreferences(
            graph.preferences.preferences().copy(speechRate = rate.coerceIn(0.7f, 1.3f)),
        )
    }

    fun setDailyGoal(minutes: Int) {
        val prefs = graph.preferences.preferences().copy(dailyGoalMinutes = DailyGoal.sanitize(minutes))
        savePreferences(prefs)
    }

    private fun savePreferences(prefs: Preferences) {
        viewModelScope.launch(Dispatchers.IO) {
            val previousLanguage = _ui.value.settings.language
            val previousTheme = _ui.value.settings.theme
            graph.preferences.save(prefs)
            // A cold start paints the Android starting window from the manifest
            // theme before Compose reads the preference; mirror a changed
            // theme at the platform level so later launches open with a
            // matching background. Runs after the save so a configuration
            // change cannot recreate the activity with the old value.
            if (prefs.theme != previousTheme) {
                runCatching { applyApplicationNightMode(graph.context, prefs.theme) }
            }
            val language = uiLanguage(prefs)
            val reader = _ui.value.reader
            val refreshed = if (reader != null && reader.language != language) {
                val bookIndex = index
                val unit = bookIndex?.unit(reader.unitId)
                if (bookIndex != null && unit != null) {
                    buildReader(bookIndex, unit, graph.progress.progress(reader.bookId))
                } else {
                    reader.copy(preference = prefs, language = language)
                }
            } else {
                reader?.copy(preference = prefs, language = language)
            }
            pendingPanel = null
            wordSelection = WordSelectionState.clear()
            _ui.update {
                it.copy(settings = settingsOf(prefs, language), reader = refreshed)
            }
            refreshLibrary()
            // Screens built earlier keep their formatted copy; a language
            // change must rebuild the two whose rows are not recomputed on
            // every visit (store titles/blurbs and the dictionary status).
            if (language != previousLanguage) {
                refreshStore()
                refreshDictionary()
            }
        }
    }

    // --- sync ------------------------------------------------------------

    fun openSync() {
        syncDraft = graph.syncCredentials.config() ?: SyncConfig(SyncProvider.CUSTOM, "", "", false)
        syncHasPassword = graph.syncCredentials.hasPassword()
        val language = _ui.value.settings.language
        _ui.update { it.copy(sync = syncUiState(language)) }
        push(Screen.SYNC)
    }

    /**
     * Returning to the foreground clears the wait and asks for one auto round.
     * A recent success/failure within [FOREGROUND_SYNC_MIN_INTERVAL_SECONDS] is
     * skipped: `onStart` also fires when the document picker closes, and a sync
     * there would wipe the export/import notice the reader just got.
     */
    fun onAppForeground() {
        appInForeground = true
        applyReadingClock()
        viewModelScope.launch(Dispatchers.IO) {
            graph.syncEngine.clearBackoff()
            val config = graph.syncCredentials.config() ?: return@launch
            if (!config.enabled) return@launch
            syncConfig = config
            syncHasPassword = graph.syncCredentials.hasPassword()
            val status = graph.syncEngine.status()
            val last = listOfNotNull(status.lastSuccessAt, status.lastFailureAt)
                .mapNotNull(::parseInstant)
                .maxOrNull()
            if (last != null &&
                Duration.between(last, Instant.now()).seconds < FOREGROUND_SYNC_MIN_INTERVAL_SECONDS
            ) {
                return@launch
            }
            performSync(config, SyncMode.AUTO)
        }
    }

    /** The app went to the background: stop counting and write what we have. */
    fun onAppBackground() {
        appInForeground = false
        applyReadingClock()
    }

    fun setSyncProvider(provider: SyncProvider) {
        updateSyncDraft { draft ->
            val url = if (draft.url.isBlank()) {
                SyncProviders.defaultUrl(provider) ?: draft.url
            } else {
                draft.url
            }
            draft.copy(provider = provider, url = url)
        }
    }

    fun setSyncUrl(url: String) = updateSyncDraft { it.copy(url = url) }

    fun setSyncUsername(username: String) = updateSyncDraft { it.copy(username = username) }

    fun setSyncAllowHttp(allowed: Boolean) = updateSyncDraft { it.copy(allowHttp = allowed) }

    private fun updateSyncDraft(transform: (SyncConfig) -> SyncConfig) {
        syncDraft = transform(syncDraft)
        val language = _ui.value.settings.language
        _ui.update { it.copy(sync = syncUiState(language)) }
    }

    /**
     * Validate, persist (password into the Keystore), test the connection and,
     * when the probe succeeds, run one manual round so setup ends with a
     * visible result instead of an untested "saved".
     */
    fun saveSyncConfig(password: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val language = uiLanguage(graph.preferences.preferences())
            val copy = stringsFor(language).syncCopy
            val draft = syncDraft
            val validated = runCatching { WebDavUrl.validateFileUrl(draft.url, draft.allowHttp) }
            val invalid = validated.exceptionOrNull()
            if (invalid != null) {
                updateSyncUi(language, notice = copy.errorFor("URL"), noticeIsError = true)
                return@launch
            }
            val config = draft.copy(url = validated.getOrThrow())
            graph.syncCredentials.save(config)
            val passwordStored = graph.syncCredentials.savePassword(password)
            syncConfig = config
            syncDraft = config
            syncHasPassword = graph.syncCredentials.hasPassword()
            if (!passwordStored) {
                updateSyncUi(language, notice = copy.savedNoPassword, noticeIsError = true)
                return@launch
            }
            val credentials = runCatching { credentialsOf(config) }.getOrElse { error ->
                updateSyncUi(language, notice = error.message ?: copy.passwordMissing, noticeIsError = true)
                return@launch
            }
            val probe = runCatching { graph.syncEngine.probe(credentials) }
            val probeError = probe.exceptionOrNull()
            if (probeError != null) {
                val category = (probeError as? SyncException)?.category
                updateSyncUi(
                    language,
                    notice = copy.testFailed + "：" + copy.errorFor(category?.name),
                    noticeIsError = true,
                )
                return@launch
            }
            val exists = probe.getOrNull()?.exists == true
            updateSyncUi(language, notice = if (exists) copy.testOk else copy.testEmpty)
            performSync(config, SyncMode.MANUAL)
        }
    }

    fun syncNow() {
        val config = syncConfig ?: graph.syncCredentials.config()
        if (config == null || !config.enabled) {
            val copy = stringsFor(_ui.value.settings.language).syncCopy
            updateSyncUi(_ui.value.settings.language, notice = copy.statusIdle, noticeIsError = true)
            return
        }
        viewModelScope.launch(Dispatchers.IO) { performSync(config, SyncMode.MANUAL) }
    }

    fun disableSync() {
        viewModelScope.launch(Dispatchers.IO) {
            syncDebounceJob?.cancel()
            graph.syncCredentials.clear()
            graph.syncEngine.reset()
            syncConfig = null
            syncDraft = SyncConfig(SyncProvider.CUSTOM, "", "", false)
            syncHasPassword = false
            updateSyncUi(uiLanguage(graph.preferences.preferences()))
        }
    }

    fun dismissSyncNotice() {
        _ui.update { it.copy(sync = it.sync.copy(notice = null, noticeIsError = false)) }
    }

    fun exportState(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val language = uiLanguage(graph.preferences.preferences())
            val copy = stringsFor(language).syncCopy
            val ok = graph.writeTextDocument(uri, StateTransfer.export(graph.store))
            updateSyncUi(
                language,
                notice = if (ok) copy.exportDone else copy.exportFailed,
                noticeIsError = !ok,
            )
        }
    }

    /**
     * Step one of an import: read (≤5 MB), validate and show the preview. No
     * write happens until [confirmImport], so a corrupt or future-version file
     * cannot touch the live document.
     */
    fun importState(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val language = uiLanguage(graph.preferences.preferences())
            val copy = stringsFor(language).syncCopy
            val text = when (val read = graph.readTextDocument(uri)) {
                TextDocumentRead.TooLarge -> {
                    showImportError(language, copy.importTooLarge)
                    return@launch
                }

                TextDocumentRead.Failed -> {
                    showImportError(language, copy.importUnreadable)
                    return@launch
                }

                is TextDocumentRead.Ok -> read.text
            }
            val inspected = StateTransfer.inspect(text)
            when (inspected) {
                StateTransfer.Inspection.Invalid -> showImportError(language, copy.importInvalid)
                StateTransfer.Inspection.FutureVersion -> showImportError(language, copy.importFuture)
                is StateTransfer.Inspection.Ready -> {
                    pendingImport = inspected.document
                    val preview = inspected.preview
                    updateSyncUi(
                        language,
                        importPreview = ImportPreviewUi(
                            books = preview.books,
                            readUnits = preview.readUnits,
                            cards = preview.cards,
                            level = preview.level?.name,
                            updatedAt = formatSyncTime(preview.updatedAt),
                        ),
                    )
                }
            }
        }
    }

    /** Step two: apply the previewed file, merging by default or replacing. */
    fun confirmImport(overwrite: Boolean) {
        val document = pendingImport ?: return
        pendingImport = null
        viewModelScope.launch(Dispatchers.IO) {
            val language = uiLanguage(graph.preferences.preferences())
            val copy = stringsFor(language).syncCopy
            if (overwrite && !graph.store.backupNow()) {
                updateSyncUi(language, notice = copy.importBackupFailed, noticeIsError = true)
                return@launch
            }
            val mode = if (overwrite) {
                StateTransfer.ImportMode.OVERWRITE
            } else {
                StateTransfer.ImportMode.MERGE
            }
            val imported = runCatching {
                StateTransfer.import(graph.store, document, mode, Instant.now().toString(), today())
            }.getOrNull()
            if (imported == null) {
                updateSyncUi(language, notice = copy.importInvalid, noticeIsError = true)
                return@launch
            }
            refreshLibrary()
            refreshCards()
            val notice = if (imported.mode == StateTransfer.ImportMode.OVERWRITE) {
                copy.overwriteDone
            } else {
                val summary = imported.summary
                val changes = (summary?.booksAdded ?: 0) + (summary?.booksUpdated ?: 0) +
                    (summary?.cardsAdded ?: 0) + (summary?.cardsUpdated ?: 0) +
                    (summary?.cardsDeleted ?: 0) + (summary?.activityDaysChanged ?: 0) +
                    (summary?.annotationsAdded ?: 0) + (summary?.annotationsUpdated ?: 0) +
                    (summary?.annotationsDeleted ?: 0) +
                    (if (summary?.preferencesFromRemote == true) 1 else 0) +
                    (if (summary?.levelFromRemote == true) 1 else 0)
                if (changes == 0) copy.importNothing else copy.importDone
            }
            updateSyncUi(language, notice = notice)
            scheduleAutoSync()
        }
    }

    fun cancelImport() {
        pendingImport = null
        updateSyncUi(uiLanguage(graph.preferences.preferences()))
    }

    private fun showImportError(language: UiLanguage, message: String) {
        pendingImport = null
        updateSyncUi(language, notice = message, noticeIsError = true)
    }

    private fun onLocalStateWritten() {
        if (syncWriteSuppressed.get()) return
        scheduleAutoSync()
    }

    /** Debounced auto sync: finishing a page does not hit the network each turn. */
    private fun scheduleAutoSync() {
        val config = syncConfig ?: runCatching { graph.syncCredentials.config() }.getOrNull() ?: return
        if (!config.enabled) return
        synchronized(syncDebounceLock) {
            syncDebounceJob?.cancel()
            syncDebounceJob = viewModelScope.launch(Dispatchers.IO) {
                delay(AUTO_SYNC_DEBOUNCE_MS)
                performSync(config, SyncMode.AUTO)
            }
        }
    }

    private fun performSync(config: SyncConfig, mode: SyncMode) {
        val language = uiLanguage(graph.preferences.preferences())
        val copy = stringsFor(language).syncCopy
        _ui.update { it.copy(sync = it.sync.copy(busy = true, notice = null, noticeIsError = false)) }
        val credentials = runCatching { credentialsOf(config) }.getOrElse { error ->
            updateSyncUi(
                language,
                notice = error.message ?: copy.passwordMissing,
                noticeIsError = true,
            )
            return
        }
        syncWriteSuppressed.set(true)
        val outcome = try {
            graph.syncEngine.sync(credentials, mode)
        } catch (error: SyncException) {
            SyncOutcome.Failure(
                category = error.category,
                code = error.code,
                message = error.message.orEmpty(),
                status = error.status,
            )
        } finally {
            syncWriteSuppressed.set(false)
        }
        // A download or merge can change preferences (including the UI
        // language), so resolve the copy again before writing the result.
        val resultLanguage = uiLanguage(graph.preferences.preferences())
        val resultCopy = stringsFor(resultLanguage).syncCopy
        when (outcome) {
            is SyncOutcome.Success -> {
                if (outcome.localChanged) {
                    refreshLibrary()
                    refreshCards()
                }
                updateSyncUi(resultLanguage, notice = successNotice(resultCopy, outcome))
            }

            is SyncOutcome.Failure -> {
                updateSyncUi(
                    resultLanguage,
                    notice = resultCopy.errorFor(outcome.category.name),
                    noticeIsError = true,
                )
            }

            SyncOutcome.Busy, SyncOutcome.Skipped -> updateSyncUi(resultLanguage)
        }
    }

    private fun successNotice(copy: SyncCopy, outcome: SyncOutcome.Success): String? {
        val summary = outcome.summary
        val base = when (outcome.action) {
            com.pagelence.sync.SyncAction.UP_TO_DATE -> null
            com.pagelence.sync.SyncAction.CREATE_REMOTE,
            com.pagelence.sync.SyncAction.UPLOAD,
            -> copy.syncUploaded

            com.pagelence.sync.SyncAction.DOWNLOAD -> copy.syncDownloaded
            com.pagelence.sync.SyncAction.UPLOAD_MERGED -> copy.syncMerged.format(
                (summary?.booksAdded ?: 0) + (summary?.booksUpdated ?: 0),
                (summary?.cardsAdded ?: 0) + (summary?.cardsUpdated ?: 0),
                summary?.cardsDeleted ?: 0,
            )
        }
        val warning = outcome.warnings.takeIf { it.isNotEmpty() }
            ?.joinToString("；") { it }
            ?.let { copy.warningPrefix + it }
        return listOfNotNull(base, warning).takeIf { it.isNotEmpty() }?.joinToString("\n")
    }

    private fun credentialsOf(config: SyncConfig): WebDavCredentials {
        val password = graph.syncCredentials.password()
        if (password == null && graph.syncCredentials.hasPassword()) {
            throw SyncException(
                "无法读取已保存的密码，请重新输入",
                com.pagelence.sync.SyncErrorCategory.AUTH,
                "PASSWORD_UNREADABLE",
            )
        }
        return WebDavCredentials(
            url = config.url,
            username = config.username,
            password = password.orEmpty(),
            allowHttp = config.allowHttp,
        )
    }

    private fun updateSyncUi(
        language: UiLanguage,
        notice: String? = null,
        noticeIsError: Boolean = false,
        importPreview: ImportPreviewUi? = null,
    ) {
        _ui.update {
            it.copy(
                sync = syncUiState(
                    language = language,
                    notice = notice,
                    noticeIsError = noticeIsError,
                    importPreview = importPreview,
                ),
            )
        }
    }

    private fun syncUiState(
        language: UiLanguage,
        busy: Boolean = false,
        notice: String? = null,
        noticeIsError: Boolean = false,
        importPreview: ImportPreviewUi? = null,
    ): SyncUiState {
        val stored = syncConfig ?: graph.syncCredentials.config()
        val status = graph.syncEngine.status()
        return SyncPresenter(stringsFor(language).syncCopy).state(
            configured = stored?.enabled == true,
            provider = stored?.provider ?: syncDraft.provider,
            url = syncDraft.url,
            username = syncDraft.username,
            allowHttp = syncDraft.allowHttp,
            hasPassword = syncHasPassword,
            lastSuccessAt = status.lastSuccessAt,
            lastFailureAt = status.lastFailureAt,
            lastFailureCategory = status.lastFailureCategory,
            busy = busy,
            notice = notice,
            noticeIsError = noticeIsError,
            importPreview = importPreview,
        )
    }


    private fun parseInstant(value: String): Instant? =
        runCatching { Instant.parse(value) }.getOrNull()

    /** Local `yyyy-MM-dd` for a stored ISO instant; null when absent/unparseable. */
    private fun formatDate(value: String?): String? {
        val instant = value?.let(::parseInstant) ?: return null
        return DateTimeFormatter.ofPattern("yyyy-MM-dd")
            .withZone(ZoneId.systemDefault())
            .format(instant)
    }

    private fun formatSyncTime(value: String?): String {
        val instant = value?.let(::parseInstant) ?: return "—"
        return DateTimeFormatter.ofPattern("MM-dd HH:mm")
            .withZone(ZoneId.systemDefault())
            .format(instant)
    }

    // --- mapping ---------------------------------------------------------

    private fun readerPresenter(bookId: String): ReaderPresenter {
        val prefs = graph.preferences.preferences()
        val language = uiLanguage(prefs)
        return ReaderPresenter(
            content = graph.content,
            cards = graph.cards,
            strings = stringsFor(language),
            language = language,
            preferences = prefs,
            bookLevel = bookLevel(bookId),
            annotations = graph.annotations,
        )
    }

    private fun buildReader(
        bookIndex: ContentIndex,
        unit: StudyUnit,
        progress: BookProgress,
        startAtText: Boolean = false,
        startAtGuide: Boolean = false,
        startAtGrammar: Boolean = false,
        jump: ReaderJumpUi? = null,
    ): ReaderUiState = readerPresenter(bookIndex.book.id).unit(
        bookIndex = bookIndex,
        unit = unit,
        progress = progress,
        quizAnswers = quizAnswers,
        quizSubmitted = quizSubmitted,
        quizCollected = quizCollected,
        startAtText = startAtText,
        startAtGuide = startAtGuide,
        startAtGrammar = startAtGrammar,
    ).copy(jumpTarget = jump)


    private fun bookLevel(bookId: String): String? {
        graph.builtInCatalog.books().firstOrNull { it.id == bookId }?.level?.let { return it }
        storeSnapshots.forEach { snapshot ->
            snapshot.catalog.books.firstOrNull { it.id == bookId }?.level?.let { return it }
        }
        return graph.profile.profile().selfLevel?.name
    }


    fun onGuideStarted() {
        val reader = _ui.value.reader ?: return
        val key = "${reader.bookId}::${reader.unitId}"
        viewModelScope.launch(Dispatchers.IO) {
            val prefs = graph.preferences.preferences()
            if (key in prefs.seenGuideUnits) return@launch
            graph.preferences.save(prefs.copy(seenGuideUnits = prefs.seenGuideUnits + key))
        }
    }

    // --- pre-reading vocabulary preview ---------------------------------

    fun startPreview() {
        val reader = _ui.value.reader ?: return
        if (reader.preview != null) return
        val bookIndex = index ?: return
        val unit = bookIndex.unit(reader.unitId) ?: return
        val level = reader.bookLevel
        viewModelScope.launch(Dispatchers.IO) {
            val words = previewWords(bookIndex, unit, level)
            withContext(Dispatchers.Main) {
                _ui.update { state ->
                    state.reader?.let { current ->
                        state.copy(reader = current.copy(preview = ReaderPreviewUi(words)))
                    } ?: state
                }
            }
        }
    }

    fun previewAnswer(known: Boolean) {
        val reader = _ui.value.reader ?: return
        val preview = reader.preview ?: return
        if (preview.revealed || preview.finished) return
        val word = preview.current ?: return
        val updated = preview.copy(
            revealed = true,
            lastKnown = known,
            known = preview.known + if (known) 1 else 0,
        )
        _ui.update { state ->
            state.reader?.let { state.copy(reader = it.copy(preview = updated)) } ?: state
        }
        if (!known && !word.inWordCards) {
            viewModelScope.launch(Dispatchers.IO) {
                addPreviewCard(reader, word)
            }
        }
    }

    fun previewNext() {
        val reader = _ui.value.reader ?: return
        val preview = reader.preview ?: return
        if (!preview.revealed) return
        val updated = preview.copy(index = preview.index + 1, revealed = false, lastKnown = null)
        _ui.update { state ->
            state.reader?.let { state.copy(reader = it.copy(preview = updated)) } ?: state
        }
    }

    fun finishPreview() {
        val reader = _ui.value.reader ?: return
        _ui.update { state ->
            state.reader?.let {
                state.copy(
                    reader = it.copy(
                        preview = null,
                        resumePhase = ReaderPhase.TEXT,
                        resumeTextPage = 0,
                        resumeTextProgress = 0f,
                    ),
                )
            } ?: state
        }
        onGuideStarted()
    }

    fun closePreview() {
        _ui.update { state ->
            state.reader?.let { state.copy(reader = it.copy(preview = null)) } ?: state
        }
    }

    private fun previewWords(
        bookIndex: ContentIndex,
        unit: StudyUnit,
        bookLevel: String?,
    ): List<PreviewWordUi> = PreviewPresenter(graph.content, graph.cards)
        .words(bookIndex, unit, bookLevel)


    private suspend fun addPreviewCard(reader: ReaderUiState, word: PreviewWordUi) {
        val now = Instant.now().toString()
        val paragraphId = "preview-${reader.unitId}"
        val hintId = "preview-${word.lemma}"
        graph.cards.add(
            WordCard(
                id = WordCard.idOf(reader.bookId, paragraphId, hintId),
                bookId = reader.bookId,
                paragraphId = paragraphId,
                hintId = hintId,
                unitId = reader.unitId,
                term = word.term,
                lemma = word.lemma,
                pos = word.pos,
                zh = word.zh,
                en = word.en,
                ipa = word.ipa,
                example = word.exampleEn?.let { HintExample(it, word.exampleZh) },
                createdAt = now,
                updatedAt = now,
            ),
        )
        val count = graph.cards.list(reader.bookId).size
        withContext(Dispatchers.Main) {
            _ui.update { state ->
                val current = state.reader
                val preview = current?.preview
                if (current == null || preview == null) {
                    state
                } else {
                    val words = preview.words.map {
                        if (WordCard.lemmaKey(it.lemma) == WordCard.lemmaKey(word.lemma)) {
                            it.copy(inWordCards = true)
                        } else {
                            it
                        }
                    }
                    state.copy(
                        reader = current.copy(
                            preview = preview.copy(words = words, added = preview.added + 1),
                            cardsCount = count,
                        ),
                    )
                }
            }
        }
    }


    private fun levelRank(level: String?): Int? = when (level) {
        "A1" -> 0
        "A2" -> 1
        "B1" -> 2
        "B2" -> 3
        "C1" -> 4
        "C2" -> 5
        else -> null
    }







    private fun settingsOf(prefs: Preferences, language: UiLanguage): SettingsUiState =
        SettingsPresenter(prefs).state()


    private fun uiLanguage(prefs: Preferences): UiLanguage =
        if (prefs.language == Language.EN) UiLanguage.EN else UiLanguage.ZH

    private fun titleFor(title: LocalizedText, language: UiLanguage): String = when (language) {
        UiLanguage.ZH -> title.zh ?: title.en ?: ""
        UiLanguage.EN -> title.en ?: title.zh ?: ""
    }

    private fun textFor(text: LocalizedText, language: UiLanguage): String? = when (language) {
        UiLanguage.ZH -> text.zh ?: text.en
        UiLanguage.EN -> text.en ?: text.zh
    }?.takeIf { it.isNotBlank() }

    private fun today(): String = LocalDate.now().toString()

    private fun lookupHintId(term: String): String =
        "lookup-" + term.lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-')

    companion object {
        /** Coalesce a burst of page turns into one write. */
        private const val SAVE_DEBOUNCE_MS = 300L
        /** Page turns and card taps coalesce; active reading never syncs per turn. */
        private const val AUTO_SYNC_DEBOUNCE_MS = 30_000L
        /** Do not sync again on every onStart (e.g. returning from the file picker). */
        private const val FOREGROUND_SYNC_MIN_INTERVAL_SECONDS = 60L

        /** Upper bound on the blocking flush during ON_STOP. */
        private const val FLUSH_PROGRESS_TIMEOUT_MS = 1_000L

        /** How often reading time is written while the reader is open. */
        private const val READING_TICK_MS = 15_000L

        fun factory(graph: AppGraph): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = PagelenceViewModel(graph) as T
        }
    }
}
