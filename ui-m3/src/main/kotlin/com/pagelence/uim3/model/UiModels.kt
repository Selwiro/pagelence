package com.pagelence.uim3.model

import com.pagelence.core.assessment.AssessmentMode
import com.pagelence.core.prefs.DefinitionMode
import com.pagelence.core.prefs.FontSize
import com.pagelence.core.prefs.ReaderLayout
import com.pagelence.core.prefs.GuideMode
import com.pagelence.core.prefs.LevelRecommendation
import com.pagelence.core.prefs.ThemeMode
import com.pagelence.core.prefs.Preferences
import com.pagelence.core.progress.CefrLevel
import com.pagelence.core.progress.LevelSource
import com.pagelence.core.progress.ReaderPhase
import com.pagelence.sync.SyncProvider
import com.pagelence.uim3.UiLanguage

/**
 * Presentation models. They are built by the app shell from `:core` data and
 * contain no rules: screens render them and report events back.
 */

data class BookUi(
    val id: String,
    val title: String,
    /** One character used as the cover watermark; always taken from the Chinese title. */
    val monogram: String,
    val author: String?,
    /** English title printed on the cover, regardless of the UI language. */
    val coverTitle: String,
    val subtitle: String,
    val unitCount: Int,
    val readUnits: Int,
    val readingUnitTitle: String?,
    val finished: Boolean,
    /** Display date of the finish, e.g. `2026-11-21`; null for old finishes. */
    val finishedOn: String? = null,
    /** Package `cover.jpg`; null falls back to the typographic cover. */
    val coverPath: String? = null,
)

data class LibraryUiState(
    /** Books in progress, newest read first. */
    val books: List<BookUi> = emptyList(),
    /** Finished books, newest finish first; finishing never removes a book. */
    val finished: List<BookUi> = emptyList(),
    val loading: Boolean = true,
    val error: String? = null,
)

/** One downloadable book in the store, merged across sources. */
data class StoreBookUi(
    val id: String,
    val title: String,
    /** English title printed on the cover, regardless of the UI language. */
    val coverTitle: String,
    val author: String?,
    val level: String?,
    val description: String?,
    val sizeLabel: String,
    val sourceNames: List<String>,
    val sourceIds: List<String>,
    val installed: Boolean,
    val updateAvailable: Boolean,
    val downloading: Boolean = false,
)

/** One chapter row on the book detail page. */
data class BookChapterUi(
    val id: String,
    val number: Int,
    val title: String,
    val unitCount: Int,
)

/** Everything the store shows on one book's detail page. */
data class BookDetailUi(
    val bookId: String,
    val title: String,
    val coverTitle: String,
    val monogram: String,
    val author: String?,
    val level: String?,
    val wordCount: Int,
    val category: String?,
    val tags: List<String>,
    val description: String?,
    val creator: String?,
    val chapters: List<BookChapterUi>,
    val unitCount: Int,
    /** Readable on this device (built-in or downloaded). */
    val available: Boolean,
    /** Shipped with the app rather than downloaded from a source. */
    val builtIn: Boolean,
    /** A source catalog offers this book, whether or not it is installed yet. */
    val downloadable: Boolean,
    val downloading: Boolean,
    val updateAvailable: Boolean,
    val sizeLabel: String?,
    val sourceNames: List<String>,
    val sourceIds: List<String>,
    /** Publication facts for the info page; null when the package omits them. */
    val publisher: String? = null,
    val edition: String? = null,
    /** Display-ready licence summaries, already localised. */
    val sourceLicense: String? = null,
    val sourceLicenseNote: String? = null,
    val creatorLicense: String? = null,
    val onShelf: Boolean,
    val readUnits: Int,
    /** Complete: every unit was read at least once. */
    val finished: Boolean = false,
    /** Display date of the finish, e.g. `2026-11-21`; null for old finishes. */
    val finishedOn: String? = null,
    val coverPath: String?,
)

/**
 * The page shown after the last unit of a book: one small ending, then the
 * book's numbers and the ways back into the shelf.
 */
data class FinishedBookUi(
    val bookId: String,
    val title: String,
    val coverTitle: String,
    val monogram: String,
    val author: String?,
    val unitCount: Int,
    /** Words of the original text, counted from the book itself. */
    val wordCount: Int,
    /** Words the reader collected from this book, tombstones excluded. */
    val cardCount: Int,
    /** Display date of the finish; null for a book finished before T139. */
    val finishedOn: String?,
    val coverPath: String?,
)

/** One built-in title on the store shelf; planned titles are visible but not readable yet. */
data class BuiltInBookUi(
    val id: String,
    val title: String,
    val coverTitle: String,
    val monogram: String,
    val author: String?,
    val level: String?,
    val description: String?,
    val unitCount: Int,
    val available: Boolean,
    val order: Int = 0,
    val status: String = "available",
    /** Package `cover.jpg`; null falls back to the typographic cover. */
    val coverPath: String? = null,
)

data class StoreSourceUi(
    val id: String,
    val name: String,
    val url: String,
    val builtIn: Boolean,
    val enabled: Boolean,
)

data class InstalledBookUi(
    val id: String,
    val title: String,
    val units: Int,
    val sourceName: String,
)

/** 书城：内置书架、可选等级筛选、自定义源书目、已下载与自定义源。 */
data class StoreUiState(
    val loading: Boolean = false,
    val refreshing: Boolean = false,
    val allowLocalHttp: Boolean = false,
    val books: List<StoreBookUi> = emptyList(),
    val builtIn: List<BuiltInBookUi> = emptyList(),
    val sources: List<StoreSourceUi> = emptyList(),
    val installed: List<InstalledBookUi> = emptyList(),
    val downloadingId: String? = null,
    /** A local or online package is being read, validated and installed. */
    val importing: Boolean = false,
    val status: String? = null,
    val error: String? = null,
    val notice: String? = null,
)

/** One heat-map cell; [level] 0–4 maps to a gray step, not a hue. */
data class HeatmapCellUi(val date: String, val level: Int)

data class HeatmapUi(
    /** Columns are weeks (Monday first); null pads outside the 365-day window. */
    val weeks: List<List<HeatmapCellUi?>> = emptyList(),
    val activeDays: Int = 0,
    val totalUnits: Int = 0,
) {
    val isEmpty: Boolean get() = activeDays == 0
}

/** "我的": level, cross-book progress and recent reading. */
data class ProfileUiState(
    val level: CefrLevel? = null,
    val levelSource: LevelSource = LevelSource.SELF,
    val levelScore: Int? = null,
    val levelUpdatedAt: String? = null,
    val totalUnits: Int = 0,
    val totalReadUnits: Int = 0,
    val finishedBooks: Int = 0,
    val readingDays: Int = 0,
    val heatmap: HeatmapUi = HeatmapUi(),
)

/** One page of a placement test; reading questions carry their passage. */
data class AssessmentQuestionUi(
    val id: String,
    val level: String,
    val skillLabel: String,
    val prompt: String,
    val options: List<String>,
    val selected: Int?,
    /** The reader marked this one as "not sure" and moved on; counts as wrong. */
    val skipped: Boolean = false,
    val passageTitle: String? = null,
    val passageText: String? = null,
)

data class AssessmentSkillUi(val label: String, val percent: Int)

data class AssessmentResultUi(
    val mode: AssessmentMode,
    val scorePercent: Int,
    val correct: Int,
    val total: Int,
    /** "A2–B1" or "B1". */
    val band: String,
    /** The level the test suggests, shown with the band copy. */
    val suggested: String,
    val singleLevel: Boolean,
    val skills: List<AssessmentSkillUi>,
)

/** Placement-test screen state: intro, one question per page, result. */
data class AssessmentUiState(
    val active: Boolean = false,
    val mode: AssessmentMode = AssessmentMode.QUICK,
    val questions: List<AssessmentQuestionUi> = emptyList(),
    val index: Int = 0,
    val submitted: Boolean = false,
    val result: AssessmentResultUi? = null,
    /** Back pressed with answers already given: ask before throwing them away. */
    val exitConfirm: Boolean = false,
) {
    val current: AssessmentQuestionUi? get() = questions.getOrNull(index)
    val answeredCount: Int get() = questions.count { it.selected != null || it.skipped }
}

/** One entry of the shared CEFR-J grammar library, list + detail. */
data class GrammarPointUi(
    val id: String,
    val name: String,
    val levelStart: String,
    val levelEnd: String?,
    /** `A2.1–B1.2`, or one band. */
    val levelLabel: String,
    val explain: String,
    val form: String,
    val pitfalls: List<String>,
    val examples: List<GrammarExampleUi>,
    val related: List<GrammarRelatedUi> = emptyList(),
    val previousId: String? = null,
    val nextId: String? = null,
)

data class GrammarRelatedUi(val id: String, val name: String)

data class GrammarTabUiState(
    val points: List<GrammarPointUi> = emptyList(),
    val loading: Boolean = false,
)

data class GrammarDetailUi(
    val bookId: String,
    val bookTitle: String,
    val unitId: String,
    val unitTitle: String,
    val chapterTitle: String,
    val grammar: GrammarUi,
)

data class SentenceUi(
    /** 1-based sentence number inside the unit; used for lookups. */
    val number: Int,
    /** A display fragment; long sentences are split at word boundaries. */
    val text: String,
    val zh: String?,
    /** Spellings the book annotates with a hint or gloss. */
    val annotated: Set<String>,
    /** Long-sentence analysis; null hides the sentence-end 「解析」 marker. */
    val analysis: AnalysisUi? = null,
)

data class AnalysisUi(
    val level: String,
    val core: String,
    val parts: List<AnalysisPartUi>,
    val order: String?,
    val note: String?,
)

data class AnalysisPartUi(
    /** Verbatim text from the sentence. */
    val text: String,
    /** `subordinate`, `relative`, `nonfinite`, `prepositional`, `appositive`, `parenthetical`. */
    val role: String,
    /** Part explanation; null in English when the book has no `en`. */
    val description: String?,
)

data class ParagraphUi(
    val id: String,
    val sentences: List<SentenceUi>,
)

data class QuizQuestionUi(
    val question: String,
    val options: List<String>,
    val selected: Int?,
    val correct: Int,
    val feedback: String?,
    val revealed: Boolean,
    /** 1-based sentence inside the unit, as stored by the content. */
    val sentenceNumber: Int? = null,
    val sentenceText: String? = null,
    val sentenceZh: String? = null,
)

data class GrammarExampleUi(
    val text: String,
    val zh: String?,
    val fromBook: Boolean = false,
)

/**
 * One grammar card in "point → examples → analysis" order. Every field is
 * optional except [excerpt]; the screen hides empty sections, so an older
 * two-part unit still renders.
 */
data class GrammarUi(
    val excerpt: String,
    val topic: String?,
    /** The primary (first) point; null for a library-outside grammar. */
    val pointId: String? = null,
    val pointName: String? = null,
    val levelLabel: String? = null,
    val explain: String? = null,
    val form: String? = null,
    val pitfalls: List<String> = emptyList(),
    val focus: String? = null,
    /** Remaining `pointIds` shown as "这一句还用到" links. */
    val extraPoints: List<GrammarRelatedUi> = emptyList(),
)

data class SenseUi(
    val pos: String?,
    val definition: String,
    /** `zh` or `en`; the panel groups senses by this. */
    val language: String = "zh",
    /** A meaning of the tapped form itself (`left` as "左边的"), not its lemma. */
    val fromSurface: Boolean = false,
)

data class WordPanelUi(
    val term: String,
    val lemma: String,
    val pos: String?,
    val ipa: String?,
    val level: String?,
    /** True when the level is inferred and should read as `≈B2`. */
    val levelApprox: Boolean = false,
    val levelSource: String?,
    val gloss: String?,
    /** `人名` / `地名` / `专名` / `未收录释义`, when there is no dictionary sense. */
    val emptyLabel: String? = null,
    /** Contraction/compound note, e.g. 「缩合词：she + will」. */
    val note: String? = null,
    val exampleEn: String?,
    val exampleZh: String?,
    /** Chinese of the sentence the word was tapped in, used when no sense exists. */
    val sentenceZh: String? = null,
    val zipf: Double?,
    /** `high` / `common` / `normal` / `low`, when the word has a frequency. */
    val frequencyBand: String? = null,
    val occurrences: Int?,
    val senses: List<SenseUi>,
    val collected: Boolean,
    val paragraphId: String,
    val sentenceNumber: Int,
    val hintId: String?,
)

/** The word currently highlighted in the text. */
data class SelectedWordUi(
    val paragraphId: String,
    val sentenceNumber: Int,
    val spelling: String,
    val start: Int = -1,
    val end: Int = -1,
)

/** The temporary one-line word entry in the reader top bar. */
data class WordStripUi(
    val paragraphId: String,
    val sentenceNumber: Int,
    val spelling: String,
    val term: String,
    val ipa: String?,
    val gloss: String?,
)

data class ReaderUiState(
    val bookId: String,
    val unitId: String,
    val bookTitle: String,
    val chapterTitle: String,
    val unitTitle: String,
    val chapterNumber: Int,
    val unitNumber: Int,
    val unitPosition: Int,
    val unitCount: Int,
    val introductionZh: String?,
    val introductionEn: String?,
    val showGuideInitially: Boolean = false,
    val paragraphs: List<ParagraphUi>,
    val startParagraphId: String?,
    val quiz: List<QuizQuestionUi>,
    val quizSubmitted: Boolean,
    val quizScore: Int,
    val quizCollectibleCount: Int = 0,
    val quizCollectedCount: Int = 0,
    val grammar: GrammarUi?,
    val unitRead: Boolean,
    val hasNextUnit: Boolean,
    val hasPreviousUnit: Boolean = false,
    /** Where the last session stopped inside this unit. */
    val resumePhase: ReaderPhase = ReaderPhase.TEXT,
    val resumeTextPage: Int = 0,
    val resumeTextProgress: Float = 0f,
    /** Book CEFR band from the store; used by the pre-reading vocabulary list. */
    val bookLevel: String? = null,
    /** Non-null while the guide's 预习 flow is open. */
    val preview: ReaderPreviewUi? = null,
    val preference: Preferences,
    val language: UiLanguage,
    val wordPanel: WordPanelUi?,
    val wordStrip: WordStripUi? = null,
    val selectedWord: SelectedWordUi? = null,
    val cardsCount: Int,
    val speechNotice: String? = null,
    /** Chapters and units of this book, for the in-reader contents sheet. */
    val toc: List<TocChapterUi> = emptyList(),
)

/** Sub-pages of the 复习 tab. */
enum class ReviewTab { WORDS, GRAMMAR }

/** One unit row in the reader's contents sheet. */
data class TocUnitUi(
    val id: String,
    val number: Int,
    val title: String,
    val read: Boolean,
    val current: Boolean,
)

/** One chapter group in the reader's contents sheet. */
data class TocChapterUi(
    val id: String,
    val number: Int,
    val title: String,
    val units: List<TocUnitUi>,
)

data class WordCardUi(
    val id: String,
    val term: String,
    val lemma: String,
    val ipa: String?,
    val zh: String?,
    val en: String?,
    val exampleEn: String?,
    val exampleZh: String?,
    val bookId: String,
    val bookTitle: String,
)

data class PreviewWordUi(
    val term: String,
    val lemma: String,
    val ipa: String? = null,
    val pos: String? = null,
    val level: String,
    val zh: String? = null,
    val en: String? = null,
    /** First sentence where the word appears, used for the generated card. */
    val exampleEn: String? = null,
    val exampleZh: String? = null,
    /** Already in the word-card list, so the preview must not add a duplicate. */
    val inWordCards: Boolean = false,
)

/**
 * State of the guide's pre-reading vocabulary flow. [index] == [words].size
 * means every word was shown; [revealed] means the definition is visible.
 */
data class ReaderPreviewUi(
    val words: List<PreviewWordUi>,
    val index: Int = 0,
    val revealed: Boolean = false,
    /** True/False after the reader answers the current word. */
    val lastKnown: Boolean? = null,
    val known: Int = 0,
    val added: Int = 0,
) {
    val current: PreviewWordUi? get() = words.getOrNull(index)
    val finished: Boolean get() = index >= words.size
}

/** Which optional-dictionary action the row should offer. */
enum class DictionaryPhase { UNAVAILABLE, NOT_INSTALLED, DOWNLOADING, PAUSED, INSTALLED, UPDATE_AVAILABLE, FAILED }

/**
 * Settings → 词典 → 完整词表. The manifest and download rules live in `:core`;
 * this model only carries what the row renders.
 */
data class DictionaryUiState(
    val phase: DictionaryPhase = DictionaryPhase.UNAVAILABLE,
    /** Version pinned by the manifest compiled into this build. */
    val version: String? = null,
    /** Published size, already formatted (e.g. `82.0 MB`). */
    val sizeLabel: String? = null,
    val installedVersion: String? = null,
    /** One line under the title: not installed / progress / installed / error. */
    val status: String? = null,
    /** 0f–1f while downloading. */
    val progress: Float = 0f,
    /** The metered-network confirmation dialog is open. */
    val confirmMetered: Boolean = false,
    val canDownload: Boolean = false,
    val canPause: Boolean = false,
    val canResume: Boolean = false,
    val canDelete: Boolean = false,
    val canRetry: Boolean = false,
)

data class SettingsUiState(
    val language: UiLanguage,
    val theme: ThemeMode,
    val fontSize: FontSize,
    val readerLayout: ReaderLayout = ReaderLayout.PAGED,
    val annotateHints: Boolean,
    val definitionMode: DefinitionMode,
    val guideMode: GuideMode,
    val speechRate: Float = 1.0f,
)

/**
 * A level just picked (self-assessment or test result) before the app changes
 * the reading modes. [changes] holds only the modes that differ from the
 * current settings, so the reader sees exactly what would change.
 */
data class LevelPromptUi(
    val level: CefrLevel,
    val source: LevelSource,
    val currentLanguage: UiLanguage,
    val currentDefinition: DefinitionMode,
    val currentGuide: GuideMode,
    val changes: LevelRecommendation,
)

/** Visual weight of the sync status line. */
enum class SyncTone { IDLE, OK, WARNING, ERROR }

/** What an imported progress file contains, shown before anything is written. */
data class ImportPreviewUi(
    val books: Int,
    val readUnits: Int,
    val cards: Int,
    val level: String?,
    val updatedAt: String,
)

/**
 * Draft + status for the sync screen. The password is never part of this
 * model: the screen keeps it in local Compose state and hands it to the save
 * callback, so it does not sit in a StateFlow.
 */
data class SyncUiState(
    val configured: Boolean = false,
    val provider: SyncProvider = SyncProvider.CUSTOM,
    val url: String = "",
    val username: String = "",
    val allowHttp: Boolean = false,
    val hasPassword: Boolean = false,
    val templateUrl: String? = null,
    val busy: Boolean = false,
    val tone: SyncTone = SyncTone.IDLE,
    val status: String? = null,
    val notice: String? = null,
    val noticeIsError: Boolean = false,
    val importPreview: ImportPreviewUi? = null,
)
