package com.pagelence.uim3

/** English copy; same getter shape as [ZhStrings]. */
object EnStrings : Strings {
    override val uiLanguage: UiLanguage get() = UiLanguage.EN
    override val appName: String get() = "Pagelence"
    override val library: String get() = "Library"
    override val settings: String get() = "Settings"
    override val wordCards: String get() = "Word cards"
    override val back: String get() = "Back"
    override val loading: String get() = "Loading content…"
    override val retry: String get() = "Retry"
    override val recoveryFromBackupTitle: String get() = "Recovered from backup"
    override val recoveryFromBackupBody: String get() =
        "The previous progress file could not be read, so the backup was used. " +
            "The damaged file is kept as state.json.corrupt for diagnosis."
    override val recoveryCorruptTitle: String get() = "Progress file damaged"
    override val recoveryCorruptBody: String get() =
        "Neither the progress file nor its backup could be read, so the app started with empty " +
            "progress. The damaged file is kept as state.json.corrupt for diagnosis."
    override val recoveryOk: String get() = "Got it"
    override val startReading: String get() = "Start reading"
    override val continueReading: String get() = "Continue"
    override val readUnits: String get() = "%1\$d of %2\$d units read"
    override val unitCount: String get() = "%d units"
    override val noCards: String get() = "No word cards yet"
    override val noCardsHint: String get() = "Tap a word while reading to keep it here."
    override val delete: String get() = "Delete"
    override val cardCollected: String get() = "In word cards"
    override val collectCard: String get() = "Add to word cards"
    override val removeCard: String get() = "Remove from word cards"
    override val translation: String get() = "ZH"
    override val analysisTitle: String get() = "Sentence analysis"
    override val analysisParse: String get() = "Parse"
    override val analysisCore: String get() = "Main clause"
    override val analysisParts: String get() = "Parts"
    override val analysisOrder: String get() = "Word order"
    override val analysisNote: String get() = "Hint"
    override val analysisClose: String get() = "Collapse"
    override val analysisRoleSubordinate: String get() = "subordinate clause"
    override val analysisRoleRelative: String get() = "relative clause"
    override val analysisRoleNonfinite: String get() = "non-finite"
    override val analysisRolePrepositional: String get() = "prepositional phrase"
    override val analysisRoleAppositive: String get() = "appositive"
    override val analysisRoleParenthetical: String get() = "parenthetical"
    override val noTranslation: String get() = "Translation pending"
    override val readingGuide: String get() = "Before reading"
    override val quiz: String get() = "Quiz"
    override val quizSubmit: String get() = "Check answers"
    override val quizScore: String get() = "Score %1\$d / %2\$d"
    override val quizResult: String get() = "Result"
    override val grammar: String get() = "Grammar here"
    override val grammarForm: String get() = "Form"
    override val grammarExplain: String get() = "Explanation"
    override val grammarRelated: String get() = "Related"
    override val grammarAlsoUses: String get() = "This sentence also uses"
    override val grammarPrevious: String get() = "Previous"
    override val grammarNext: String get() = "Next"
    override val grammarFocus: String get() = "This sentence"
    override val nextUnit: String get() = "Next section"
    override val nextUnitGuide: String get() = "Next section · Guide"
    override val previousUnitGrammar: String get() = "Previous section · Grammar"
    override val finishBook: String get() = "Finish book"
    override val sectionDictionary: String get() = "Dictionary"
    override val fullDictionary: String get() = "Full dictionary"
    override val dictVersionLabel: String get() = "Version %s"
    override val dictSizeLabel: String get() = "About %s"
    override val dictNotInstalled: String get() = "Not installed; lookups use the core dictionary"
    override val dictDownloading: String get() = "Downloading %d%%"
    override val dictPaused: String get() = "Paused · %s downloaded"
    override val dictInstalled: String get() = "Installed; lookups use core then full"
    override val dictUpdateAvailable: String get() = "Update %s available (installed %s)"
    override val dictHostingPending: String get() = "Download hosting is not live yet"
    override val dictNeedsNewerApp: String get() = "This dictionary needs a newer app"
    override val dictFailed: String get() = "Download failed: %s"
    override val dictDownload: String get() = "Download"
    override val dictPause: String get() = "Pause"
    override val dictResume: String get() = "Resume"
    override val dictRetry: String get() = "Retry"
    override val dictDelete: String get() = "Delete"
    override val dictDeleteConfirm: String get() = "The app falls back to the core dictionary; progress and cards are untouched."
    override val dictMeteredTitle: String get() = "Not on Wi-Fi"
    override val dictMeteredBody: String get() = "The full dictionary is about %s; continuing uses mobile data."
    override val dictContinue: String get() = "Continue"
    override val dictLicenseNote: String get() = "ECDICT (MIT); see About for the licence and source."
    override val shelfReading: String get() = "In progress"
    override val shelfFinished: String get() = "Finished"
    override val finishedDate: String get() = "Finished %s"
    override val readAgain: String get() = "Read again"
    override val finishedTitle: String get() = "Book finished"
    override val finishedSubtitle: String get() = "One book down. The next one is waiting."
    override val finishedUnitsLabel: String get() = "units"
    override val finishedWordsLabel: String get() = "Original words"
    override val finishedCardsLabel: String get() = "Saved words"
    override val finishedSummary: String get() = "Finished summary"
    override val backToShelf: String get() = "Back to shelf"
    override val browseStore: String get() = "Browse the store"
    override val language: String get() = "Interface language"
    override val languageZh: String get() = "中文"
    override val languageEn: String get() = "English"
    override val definitionMode: String get() = "Meaning language"
    override val definitionModeZh: String get() = "Chinese"
    override val definitionModeMixed: String get() = "Bilingual"
    override val definitionModeEn: String get() = "English"
    override val guideMode: String get() = "Guide mode"
    override val guideModeBilingual: String get() = "Bilingual guide"
    override val guideModeEn: String get() = "English guide"
    override val guideModeImmersive: String get() = "Immersive"
    override val toc: String get() = "Contents"
    override val tocHint: String get() = "Pick any unit; finished units can be reopened"
    override val tocCurrent: String get() = "Reading"
    override val tocRead: String get() = "Read"
    override val definitionsZh: String get() = "Chinese"
    override val definitionsEn: String get() = "English"
    override val lemmaBase: String get() = "Base form"
    override val otherFormSenses: String get() = "This form also means"
    override val noDefinition: String get() = "This word is not in the offline dictionary yet; the sentence meaning is shown instead."
    override val properName: String get() = "Name"
    override val properPlace: String get() = "Place"
    override val properNoun: String get() = "Proper noun"
    override val definitionLanguageFallback: String get() = "No sense in the selected meaning language; showing the available language instead."
    override val sentenceMeaning: String get() = "Sentence meaning"
    override val reviewWords: String get() = "Words"
    override val reviewGrammar: String get() = "Grammar"
    override val level: String get() = "CEFR"
    override val frequency: String get() = "Frequency"
    override val frequencyBandHigh: String get() = "Very common"
    override val frequencyBandCommon: String get() = "Common"
    override val frequencyBandNormal: String get() = "Average"
    override val frequencyBandLow: String get() = "Rare"
    override val levelFromWordNet: String get() = "WordNet-aligned"
    override val levelFromRoot: String get() = "From the root word"
    override val levelFromExam: String get() = "From exam word lists"
    override val levelFromFrequency: String get() = "Estimated from frequency"
    override val occurrences: String get() = "In this book"
    override val definitions: String get() = "Dictionary"
    override val example: String get() = "Example"
    override val collectedCount: String get() = "Cards %d"
    override val about: String get() = "About"
    override val aboutCopy: AboutCopy get() = EnAboutCopy
    override val syncCopy: SyncCopy get() = EnSyncCopy
    override val close: String get() = "Close"
    override val chapter: String get() = "Chapter %d"
    override val unit: String get() = "Section %d"
    override val emptyLibrary: String get() = "No books available"
    override val emptyShelf: String get() = "Nothing in progress"
    override val emptyShelfHint: String get() = "Pick a book in the store; it appears here once you start reading."
    override val cardRemoved: String get() = "Removed from word cards"
    override val libraryHint: String get() = "A few sections a day, one book at a time."
    override val lastRead: String get() = "Last read"
    override val sectionReading: String get() = "Reading"
    override val sectionAppearance: String get() = "Appearance"
    override val theme: String get() = "Theme"
    override val themeSystem: String get() = "System"
    override val themeLight: String get() = "Light"
    override val themeDark: String get() = "Dark"
    override val fontSize: String get() = "Text size"
    override val fontSmall: String get() = "Small"
    override val fontMedium: String get() = "Default"
    override val fontLarge: String get() = "Large"
    override val fontXLarge: String get() = "Extra large"
    override val contentLicense: String get() = "Content licence"
    override val annotateHints: String get() = "Annotate hints"
    override val annotateHintsHint: String get() = "Dashed underline on words and phrases that have a book definition"
    override val profile: String get() = "Me"
    override val myLevel: String get() = "English level"
    override val levelUnset: String get() = "Not set"
    override val levelSelf: String get() = "Self-assessment"
    override val levelQuick: String get() = "Quick test"
    override val levelFull: String get() = "Full test"
    override val setLevel: String get() = "Set level"
    override val changeLevel: String get() = "Change level"
    override val levelAffectsHint: String get() =
        "Only changes the default interface language, meaning language and guide mode; content stays open."
    override val levelUnsure: String get() = "Not sure? Start with A2 or B1 and adjust after one book."
    override val onboardingTitle: String get() = "Choose your English level"
    override val onboardingBody: String get() =
        "It sets the default interface language, meaning language and guide mode. You can change it later in Me."
    override val onboardingStart: String get() = "Start reading"
    override val onboardingSkip: String get() = "Maybe later"
    override val levelA1Name: String get() = "A1 Beginner"
    override val levelA1Desc: String get() = "Can read short phrases and familiar words; a whole paragraph still needs Chinese."
    override val levelA2Name: String get() = "A2 Elementary"
    override val levelA2Desc: String get() = "Can finish a page of simple story; too many new words break the thread."
    override val levelB1Name: String get() = "B1 Intermediate"
    override val levelB1Desc: String get() = "Can finish a chapter of clear narrative; long sentences need a second pass."
    override val levelB2Name: String get() = "B2 Upper intermediate"
    override val levelB2Desc: String get() = "Can finish a mainstream novel; the odd phrasing still trips you up."
    override val levelC1Name: String get() = "C1 Advanced"
    override val levelC1Desc: String get() = "Can read literary originals; the difficulty is implication, not vocabulary."
    override val levelC2Name: String get() = "C2 Proficient"
    override val levelC2Desc: String get() = "Can read almost anything; you only notice how well it is written."
    override val levelPromptTitle: String get() = "Reading modes for %s"
    override val levelPromptBody: String get() =
        "This level would change these settings. Accept the recommendation, keep yours, or pick now."
    override val levelPromptAccept: String get() = "Accept recommendation"
    override val levelPromptKeep: String get() = "Keep current settings"
    override val levelPromptAdjust: String get() = "Adjust now"
    override val levelPromptApply: String get() = "Apply"
    override val levelPromptBack: String get() = "Back to recommendation"
    override val levelPromptHint: String get() = "Reading aids only, never content. You can change them in Settings later."
    override val levelVocabLine: String get() = "Vocabulary: %s words"
    override val levelA1Vocab: String get() = "up to 1,000"
    override val levelA2Vocab: String get() = "1,000–2,000"
    override val levelB1Vocab: String get() = "2,000–4,000"
    override val levelB2Vocab: String get() = "4,000–6,000"
    override val levelC1Vocab: String get() = "6,000–10,000"
    override val levelC2Vocab: String get() = "10,000+"
    override val booksFinished: String get() = "%d books finished"
    override val booksFinishedShort: String get() = "Books"
    override val unitsReadTotal: String get() = "%1\$d of %2\$d units read"
    override val unitsReadShort: String get() = "Units"
    override val readingDays: String get() = "%d reading days"
    override val readingDaysShort: String get() = "Days"
    override val grammarPoint: String get() = "Point"
    override val grammarExamples: String get() = "Examples"
    override val grammarPitfalls: String get() = "Common mistakes"
    override val grammarBookExample: String get() = "From the book"
    override val quizCorrect: String get() = "Correct"
    override val quizWrong: String get() = "Incorrect"
    override val quizSeeSentence: String get() = "See sentence"
    override val quizHideSentence: String get() = "Hide sentence"
    override val quizExitTitle: String get() = "Quiz not submitted"
    override val quizExitBody: String get() = "Leaving now discards the answers you picked."
    override val quizExitConfirm: String get() = "Leave"
    override val quizStay: String get() = "Keep answering"
    override val more: String get() = "More"
    override val wordDetails: String get() = "Details"
    override val pronounce: String get() = "Pronounce"
    override val pronounceComing: String get() = "Pronunciation is coming in a later version"
    override val tabShelf: String get() = "Shelf"
    override val tabStore: String get() = "Store"
    override val tabReview: String get() = "Review"
    override val tabMe: String get() = "Me"
    override val store: String get() = "Store"
    override val storeHint: String get() = "Built-in books are ready to read; import a package or add a source from the menu."
    override val builtIn: String get() = "Built in"
    override val importBook: String get() = "Import a book"
    override val importComing: String get() = "Import a .pagelence package (coming soon)"
    override val viewInShelf: String get() = "View in shelf"
    override val readNow: String get() = "Read now"
    override val bookGrammar: String get() = "Grammar review"
    override val grammarEmpty: String get() = "Grammar notes are coming"
    override val grammarAll: String get() = "Grammar book"
    override val grammarPointsCount: String get() = "%d grammar points"
    override val openUnit: String get() = "Open this section"
    override val pageGuide: String get() = "Guide"
    override val pageText: String get() = "Text %1\$d / %2\$d"
    override val pageQuiz: String get() = "Quiz %1\$d / %2\$d"
    override val pageGrammar: String get() = "Grammar"
    override val pageReview: String get() = "Review"
    override val nextPage: String get() = "Next page"
    override val previousPage: String get() = "Previous page"
    override val scrollHint: String get() = "This page is long — scroll down"
    override val scrollEnd: String get() = "End of page"
    override val quizNextQuestion: String get() = "Next question"
    override val quizFeedbackGeneric: String get() = "Answer recorded."
    override val quizUnanswered: String get() = "Unanswered: %d"
    override val speechRate: String get() = "Speech rate"
    override val speechSlow: String get() = "Slow"
    override val speechFast: String get() = "Fast"
    override val speechEngineMissing: String get() = "No English speech engine is available. Install or choose one in system settings."
    override val speechNotReady: String get() = "The speech engine is still getting ready. Try again in a moment."
    override val assessment: String get() = "Placement test"
    override val assessmentIntro: String get() = "A short set of questions estimates your current reading level. The result is a reference only and never limits what you can read."
    override val assessmentQuick: String get() = "Quick test"
    override val assessmentQuickMeta: String get() = "12–15 questions · about 5 minutes"
    override val assessmentQuickBody: String get() = "Vocabulary and grammar around your level and its neighbours; gives a level band."
    override val assessmentFull: String get() = "Full test"
    override val assessmentFullMeta: String get() = "30–40 questions · 15–20 minutes"
    override val assessmentFullBody: String get() = "Adds a short passage and reports vocabulary, grammar and reading separately."
    override val assessmentStart: String get() = "Start test"
    override val assessmentQuestion: String get() = "Question %1\$d / %2\$d"
    override val assessmentNext: String get() = "Next question"
    override val assessmentSeeResult: String get() = "See result"
    override val assessmentSkip: String get() = "Skip this question"
    override val assessmentSkipHint: String get() = "Skip anything you are unsure about; skipped questions count as incorrect."
    override val assessmentSkipped: String get() = "Skipped; this question counts as incorrect."
    override val assessmentResult: String get() = "Result"
    override val assessmentReference: String get() = "Reference rating"
    override val assessmentBand: String get() = "Band %s"
    override val assessmentSingle: String get() = "Suggested level %s"
    override val assessmentScore: String get() = "%1\$d of %2\$d correct"
    override val assessmentAccept: String get() = "Accept new level"
    override val assessmentKeep: String get() = "Keep current level"
    override val assessmentAccepted: String get() = "Updated to %s; reading-aid defaults now follow it."
    override val assessmentKept: String get() = "Current level kept; you can retest at any time."
    override val assessmentSkillVocab: String get() = "Vocabulary"
    override val assessmentSkillGrammar: String get() = "Grammar"
    override val assessmentSkillReading: String get() = "Reading"
    override val assessmentHint: String get() = "A test only changes the default reading aids, never what you can read. You can retake it at any time."
    override val assessmentUnavailable: String get() = "The question bank is not available yet."
    override val storeBuiltInTitle: String get() = "Built-in books"
    override val storeBuiltInHint: String get() = "Included with the app and ready to read; unfinished titles are marked in production."
    override val storeFilterAll: String get() = "All"
    override val storeComingSoon: String get() = "In production"
    override val storeComingSoonHint: String get() = "Text and study notes are still being prepared; this title will arrive in an update."
    override val storeCustomCatalogTitle: String get() = "From custom sources"
    override val storeRefreshCustom: String get() = "Refresh custom sources"
    override val storeRefresh: String get() = "Refresh catalog"
    override val storeStatusUpdated: String get() = "Catalog updated · %s"
    override val storeStatusCached: String get() = "Cached catalog · %s"
    override val storeEmptyCatalog: String get() = "No downloadable books yet"
    override val storeEmptyHint: String get() = "Installed books are on the shelf; add a custom source to see the books it offers here."
    override val storeCustomSources: String get() = "Custom sources"
    override val storeAddSource: String get() = "Add source"
    override val storeSourceName: String get() = "Name (optional)"
    override val storeSourceUrl: String get() = "Catalog URL (HTTPS)"
    override val storeAdd: String get() = "Add"
    override val storeLocalHttp: String get() = "Allow LAN HTTP (debug only)"
    override val storeLocalHttpHint: String get() = "Only affects localhost and 10.0.2.2. Content is provided by the source and not reviewed."
    override val storeDownload: String get() = "Download"
    override val storeUpdate: String get() = "Update"
    override val storeInstalled: String get() = "Installed"
    override val storeRemove: String get() = "Remove"
    override val storeDownloading: String get() = "Downloading…"
    override val storeDownloaded: String get() = "Downloaded; ready in the library"
    override val storeSourcesCount: String get() = "%d source(s)"
    override val storeChooseSource: String get() = "Choose download source"
    override val storeInstalledTitle: String get() = "Downloaded"
    override val storeSize: String get() = "about %s"
    override val storePartialError: String get() = "Some sources were unreachable; showing the catalogs that loaded."
    override val storeErrorNetwork: String get() = "Network unreachable or timed out. Try another line or later."
    override val storeErrorHttp: String get() = "Server returned HTTP %d."
    override val storeErrorHttps: String get() = "Only HTTPS URLs are allowed (or enable LAN HTTP below)."
    override val storeErrorTooLarge: String get() = "The file exceeds the size limit and was rejected."
    override val storeErrorBadCatalog: String get() = "Catalog is invalid: %s"
    override val storeErrorBadPackage: String get() = "Book package is invalid: %s"
    override val storeErrorChecksum: String get() = "Checksum mismatch; the file may be incomplete."
    override val storeSourceInvalid: String get() = "The URL must be HTTPS (or LAN HTTP must be enabled)."
    override val storeBadgeBuiltIn: String get() = "Built in"
    override val storeMenu: String get() = "Store menu"
    override val storeImportLocal: String get() = "Import local book package"
    override val storeImportOnline: String get() = "Import online book package"
    override val storeImportOnlineTitle: String get() = "Import online book package"
    override val storeImportOnlineHint: String get() = "Enter an HTTPS link to a .pagelence package. Only this book is downloaded and the URL is not saved as a source; the app does not review the content."
    override val storeImportUrl: String get() = "Package link (HTTPS)"
    override val storeImport: String get() = "Import"
    override val storeImporting: String get() = "Importing…"
    override val storeImported: String get() = "Imported \"%s\"; it is ready in the library"
    override val storeImportedLocal: String get() = "Imported from file"
    override val storeImportedOnline: String get() = "Imported from link"
    override val storeErrorBuiltIn: String get() = "This book is built into the app; no import is needed."
    override val storeErrorFile: String get() = "Could not read the selected file. Check that it is a .pagelence package and try again."
    override val bookDetail: String get() = "Book details"
    override val bookWords: String get() = "about %s words"
    override val bookCategory: String get() = "Genre"
    override val bookTags: String get() = "Tags"
    override val bookCreator: String get() = "Made by"
    override val labelSeparator: String get() = ": "
    override val bookIntro: String get() = "About this book"
    override val bookChapters: String get() = "Chapters"
    override val bookChapterUnits: String get() = "%d units"
    override val addToShelf: String get() = "Add to shelf"
    override val onShelf: String get() = "On shelf"
    override val bookPlannedHint: String get() = "This book is still being made and is not readable yet."
    override val bookNoChapters: String get() = "Chapters are being prepared"
    override val bookAbout: String get() = "About this book"
    override val bookDifficulty: String get() = "Level"
    override val bookWordsLabel: String get() = "Words"
    override val bookInfoTitle: String get() = "Details"
    override val bookInfo: String get() = "Book information"
    override val bookAuthor: String get() = "Author"
    override val bookWordCount: String get() = "Word count"
    override val bookLevelLabel: String get() = "Level"
    override val bookLicense: String get() = "Licences"
    override val bookLicenseSource: String get() = "Source text"
    override val bookLicenseCreator: String get() = "This edition"
    override val bookPublish: String get() = "Publication"
    override val bookLanguage: String get() = "Language"
    override val bookLanguageEn: String get() = "English"
    override val bookChaptersCount: String get() = "Chapters"
    override val bookUnitsCount: String get() = "Units"
    override val bookInfoEmpty: String get() = "—"
    override val bookSourceBuiltIn: String get() = "Built-in"
    override val bookSources: String get() = "Sources"
    override val bookChaptersAfterDownload: String get() = "Chapters appear after download"
    override val heatmap: String get() = "Study heat map"
    override val heatmapEmpty: String get() = "No study record yet. Finish a unit and it will show up here."
    override val heatmapHint: String get() = "Units finished and reading days are recorded per day for up to 365 days, on this device only. No reminders, no streak penalties."
    override val heatmapLess: String get() = "Less"
    override val heatmapMore: String get() = "More"
    override val heatmapSummary: String get() = "%1\$d reading days · %2\$d units finished"
    override val readingLayout: String get() = "Reading layout"
    override val layoutPaged: String get() = "Pages"
    override val layoutScroll: String get() = "Scroll"
    override val preview: String get() = "Preview"
    override val previewTitle: String get() = "Preview this section"
    override val previewPrompt: String get() = "Do you know this word?"
    override val previewKnown: String get() = "I know it"
    override val previewUnknown: String get() = "I don't"
    override val previewNext: String get() = "Next"
    override val previewFinish: String get() = "Start reading"
    override val previewEmpty: String get() = "No words above this book's level in this section."
    override val previewProgress: String get() = "%1\$d / %2\$d"
    override val previewLevelHint: String get() = "Above this book (%1\$s)"
    override val previewInCards: String get() = "In word cards"
    override val previewAdded: String get() = "Added to word cards"
    override val previewKnownTag: String get() = "Known"
    override val previewUnknownTag: String get() = "New"
    override val previewDone: String get() = "Preview complete"
    override val previewDoneHint: String get() = "You'll meet these words again in the text."
    override val enterQuiz: String get() = "Start quiz"
    override val textProgressLabel: String get() = "%1\$d / %2\$d"
    override val progressGuide: String get() = "Guide"
    override val progressText: String get() = "Text"
    override val progressQuiz: String get() = "Quiz"
    override val progressGrammar: String get() = "Grammar"
    override val studyWords: String get() = "Word review"
    override val studyStart: String get() = "Start reviewing"
    override val studyPrompt: String get() = "Recall the meaning, then reveal the answer."
    override val studyShowAnswer: String get() = "Show answer"
    override val studyNext: String get() = "Next"
    override val studyPrevious: String get() = "Previous"
    override val studyExit: String get() = "Exit review"
    override val studyDone: String get() = "Round complete"
    override val studyProgress: String get() = "%1\$d / %2\$d"
    override val studyCards: String get() = "%d cards"
}

private val EnAboutCopy: AboutCopy = AboutCopy(
    title = "About",
    tagline = "A local-first reader for English originals: five-minute sections that help you finish " +
        "one book after another, with progress kept on this device.",
    settingsSubtitle = "Version v%s · Privacy & data · Licences",
    versionFormat = "v%s (%d)",
    checkUpdate = "Check for updates",
    checkUpdateSubtitle = "Current %s · development build",
    website = "Website",
    github = "GitHub repository",
    privacy = "Privacy & data",
    privacySubtitle = "Data, permissions, network",
    licenses = "Open-source licences",
    licensesSubtitle = "App and third-party licences",
    pendingLink = "Address not public yet",
    linkUnavailableTitle = "Not public yet",
    openFailedTitle = "Could not open link",
    openFailedBody = "Please try again, or open it manually in a browser.",
    linkUnavailableBody = "%s does not have a public address yet; this row will open it once it does.",
    updateTitle = "Development build",
    updateBody = "This is version %s. New versions are published on GitHub Releases, and the app never checks " +
        "for updates in the background.",
    privacySections = listOf(
        AboutSection(
            title = "Where your data lives",
            paragraphs = listOf(
                "Reading progress, word cards and preferences are stored only in this app's private storage " +
                    "on this device, mainly in a single state.json file. Book packages you download yourself " +
                    "live in the same private directory. There are no accounts and no developer server, so your " +
                    "reading records are never sent to us.",
                "Uninstalling the app or clearing its data removes this. Whether Android system backup includes " +
                    "app data is up to your device settings.",
            ),
        ),
        AboutSection(
            title = "Permissions",
            paragraphs = listOf(
                "The app requests network access and network-state access: the first is for book sources and " +
                    "the optional full dictionary, the second only tells Wi-Fi from mobile data so the download " +
                    "can ask first. It does not request storage, camera, location, contacts or notification " +
                    "permissions.",
                "Reading works offline: both built-in books ship with the app, and lookup and progress stay on " +
                    "this device.",
            ),
        ),
        AboutSection(
            title = "When the app connects",
            paragraphs = listOf(
                "The app contacts a book source only when you tap Refresh or Download, over HTTPS. The request " +
                    "includes that source's address; reading progress and word cards are never uploaded.",
                "The default source stays hidden and is not requested until its hosting is live. Check for " +
                    "updates does not use the network at present.",
            ),
        ),
        AboutSection(
            title = "What is not collected",
            paragraphs = listOf(
                "There are no ads, analytics or crash-reporting SDKs, and no device identifiers, location or " +
                    "usage behaviour are collected.",
            ),
        ),
        AboutSection(
            title = "Your controls",
            paragraphs = listOf(
                "Clear app data in Android Settings → Apps → Pagelence → Storage, or remove word cards in the " +
                    "app. Uninstalling also removes the data on this device.",
                "This page is an in-app explanation, not a formal legal document. Formal terms will be " +
                    "published with the public repository.",
            ),
        ),
    ),
    licenseSections = listOf(
        AboutSection(
            title = "This app",
            paragraphs = listOf(
                "The source code is licensed under the Apache License 2.0: you may use, modify and redistribute " +
                    "it as long as copyright and licence notices are kept. The name “Pagelence” and the icon are " +
                    "not licensed with the code; derived versions should use a different name.",
                "Guides, sentence translations, quizzes, grammar notes and interface copy written for this " +
                    "project are licensed under CC BY-SA 4.0. Book texts and third-party data are not covered by " +
                    "either licence.",
            ),
        ),
        AboutSection(
            title = "Third-party components",
            paragraphs = listOf(
                "Kotlin, kotlinx.coroutines, kotlinx.serialization, AndroidX, Jetpack Compose and " +
                    "OkHttp: Apache License 2.0.",
                "Material Icons: Apache License 2.0. The Android SDK and Android runtime are provided under " +
                    "their own terms.",
            ),
            bullets = true,
        ),
        AboutSection(
            title = "Third-party data",
            paragraphs = listOf(
                "ECDICT (pinned at 39cfb96): MIT, English–Chinese definitions and phonetics.",
                "Princeton WordNet 3.0: WordNet License.",
                "CEFR-J Wordlist v1.6: free for research, education and commercial use with the official " +
                    "attribution format.",
                "Octanove Vocabulary Profile C1/C2 1.0: CC BY-SA 4.0.",
                "CEFR-Annotated WordNet (2025): annotations CC BY 4.0; sense keys under the WordNet 3.0 " +
                    "licence. Used as a build input only and not redistributed.",
                "wordfreq 3.0.2: CC BY-SA 4.0, frequency data.",
                "The five-layer word-level table and in-book occurrence counts are project-made and licensed " +
                    "under CC BY-SA 4.0.",
            ),
            bullets = true,
        ),
        AboutSection(
            title = "Book texts",
            paragraphs = listOf(
                "The Story of Doctor Dolittle: Project Gutenberg #501, 1920 edition, public domain.",
                "Alice's Adventures in Wonderland: Wikisource transcription of the Macmillan 1866 printing; " +
                    "the text is public domain and the Tenniel illustrations are not distributed.",
            ),
            bullets = true,
        ),
        AboutSection(
            title = "Notes",
            paragraphs = listOf(
                "This page summarises the main licences only. Full licence texts, NOTICE and the third-party " +
                    "data list will be provided with the public repository.",
            ),
        ),
    ),
)
