package com.pagelence

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pagelence.uim3.Strings
import com.pagelence.uim3.screens.AboutArticleScreen
import com.pagelence.uim3.screens.AboutScreen
import com.pagelence.uim3.screens.AssessmentScreen
import com.pagelence.uim3.screens.BookInfoScreen
import com.pagelence.uim3.screens.BookOverviewScreen
import com.pagelence.uim3.screens.BookSearchScreen
import com.pagelence.uim3.screens.FinishedBookScreen
import com.pagelence.uim3.screens.GrammarDetailScreen
import com.pagelence.uim3.screens.GrammarPointScreen
import com.pagelence.uim3.screens.LevelOnboardingScreen
import com.pagelence.uim3.screens.LevelPromptSheet
import com.pagelence.uim3.screens.LibraryScreen
import com.pagelence.uim3.screens.ProfileScreen
import com.pagelence.uim3.screens.ReviewScreen
import com.pagelence.uim3.screens.ReaderScreen
import com.pagelence.uim3.screens.SettingsScreen
import com.pagelence.uim3.screens.StoreScreen
import com.pagelence.uim3.screens.SyncScreen
import com.pagelence.uim3.stringsFor
import com.pagelence.uim3.theme.PagelenceTheme
import com.pagelence.uim3.theme.isDark
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * 根组件：只做屏幕切换与事件转发。任何业务判断都在 ViewModel / :core。
 */
@Composable
fun PagelenceApp(
    viewModel: PagelenceViewModel,
    appVersion: String,
    appVersionCode: Int,
) {
    val state by viewModel.ui.collectAsStateWithLifecycle()
    val dark = isDark(state.settings.theme)
    val view = LocalView.current
    SideEffect {
        val window = (view.context as Activity).window
        WindowCompat.getInsetsController(window, view).apply {
            isAppearanceLightStatusBars = !dark
            isAppearanceLightNavigationBars = !dark
        }
    }
    PagelenceTheme(darkTheme = dark) {
        AppContent(viewModel, state, appVersion, appVersionCode)
    }
}

@Composable
private fun AppContent(
    viewModel: PagelenceViewModel,
    state: AppUiState,
    appVersion: String,
    appVersionCode: Int,
) {
    val strings = stringsFor(state.settings.language)

    state.recovery?.let { recovery ->
        val retryable = recovery == RecoveryNotice.CONTENT_FAILED
        AlertDialog(
            onDismissRequest = viewModel::dismissRecovery,
            title = {
                Text(
                    when (recovery) {
                        RecoveryNotice.FROM_BACKUP -> strings.recoveryFromBackupTitle
                        RecoveryNotice.CORRUPT -> strings.recoveryCorruptTitle
                        RecoveryNotice.CONTENT_FAILED -> strings.recoveryContentFailedTitle
                        RecoveryNotice.CONTENT_PARTIAL -> strings.recoveryContentPartialTitle
                    },
                )
            },
            text = {
                Text(
                    when (recovery) {
                        RecoveryNotice.FROM_BACKUP -> strings.recoveryFromBackupBody
                        RecoveryNotice.CORRUPT -> strings.recoveryCorruptBody
                        RecoveryNotice.CONTENT_FAILED -> strings.recoveryContentFailedBody
                        RecoveryNotice.CONTENT_PARTIAL -> strings.recoveryContentPartialBody
                    },
                )
            },
            confirmButton = {
                if (retryable) {
                    TextButton(onClick = viewModel::retryContent) { Text(strings.recoveryRetry) }
                } else {
                    TextButton(onClick = viewModel::dismissRecovery) { Text(strings.recoveryOk) }
                }
            },
            dismissButton = if (retryable) {
                {
                    TextButton(onClick = viewModel::dismissRecovery) { Text(strings.recoveryLater) }
                }
            } else {
                null
            },
        )
    }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri -> uri?.let(viewModel::exportState) }
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri -> uri?.let(viewModel::importState) }
    val atTabRoot = state.screen == rootScreen(state.tab)
    BackHandler(
        enabled = state.levelPrompt != null || state.screen == Screen.ONBOARDING || !atTabRoot,
    ) {
        when {
            state.levelPrompt != null -> viewModel.dismissLevelPrompt()
            state.screen == Screen.ONBOARDING -> viewModel.skipOnboarding()
            state.screen == Screen.ASSESSMENT -> viewModel.requestCloseAssessment()
            state.reader?.preview != null -> viewModel.closePreview()
            else -> viewModel.back()
        }
    }

    when (state.screen) {
        Screen.ONBOARDING -> LevelOnboardingScreen(
            strings = strings,
            onSelect = viewModel::selectLevel,
            onSkip = viewModel::skipOnboarding,
        )

        Screen.LIBRARY, Screen.STORE, Screen.REVIEW, Screen.PROFILE -> MainTabsShell(
            viewModel = viewModel,
            state = state,
            strings = strings,
        )

        Screen.READER -> {
            val reader = state.reader
            if (reader == null) {
                MainTabsShell(viewModel = viewModel, state = state, strings = strings)
            } else {
                ReaderScreen(
                    strings = strings,
                    state = reader,
                    onBack = viewModel::requestCloseReader,
                    onOpenCards = viewModel::openCards,
                    onOpenSettings = viewModel::openSettings,
                    onOpenSearch = viewModel::openBookSearch,
                    onWordClick = viewModel::onWordClick,
                    onToggleAllTranslations = viewModel::toggleAllTranslations,
                    onOpenGrammarPoint = viewModel::openGrammarPoint,
                    onCloseWordPanel = viewModel::closeWordPanel,
                    onToggleCard = viewModel::toggleCard,
                    onSelectOption = viewModel::selectOption,
                    onSubmitQuiz = viewModel::submitQuiz,
                    onNextUnit = viewModel::nextUnit,
                    onPreviousUnit = viewModel::previousUnit,
                    onOpenUnit = viewModel::openUnitFromReader,
                    onStopSpeaking = viewModel::stopSpeaking,
                    onPageChanged = viewModel::onReaderPageChanged,
                    onTextProgress = viewModel::onReaderTextProgress,
                    onToggleBookmark = viewModel::toggleBookmark,
                    onToggleHighlight = viewModel::toggleHighlight,
                    onSaveNote = viewModel::saveNote,
                    onDeleteNote = viewModel::deleteNote,
                    onOpenAnnotation = viewModel::openAnnotation,
                    onJumpHandled = viewModel::annotationJumpHandled,
                    onGuideStarted = viewModel::onGuideStarted,
                    onStartPreview = viewModel::startPreview,
                    onPreviewAnswer = viewModel::previewAnswer,
                    onPreviewNext = viewModel::previewNext,
                    onPreviewFinish = viewModel::finishPreview,
                    onClosePreview = viewModel::closePreview,
                    onSpeechNoticeShown = viewModel::speechNoticeShown,
                    exitConfirm = state.readerExitConfirm,
                    onDismissExit = viewModel::dismissReaderExit,
                    onConfirmExit = viewModel::closeReader,
                )
            }
        }

        Screen.SETTINGS -> SettingsScreen(
            strings = strings,
            state = state.settings,
            dictionary = state.dictionary,
            appVersion = appVersion,
            onBack = viewModel::backFromSettings,
            onOpenSync = viewModel::openSync,
            onOpenAbout = viewModel::openAbout,
            onDictionaryDownload = viewModel::downloadDictionary,
            onDictionaryPause = viewModel::pauseDictionaryDownload,
            onDictionaryDelete = viewModel::deleteFullDictionary,
            onDictionaryMeteredConfirm = viewModel::confirmMeteredDictionaryDownload,
            onDictionaryMeteredDismiss = viewModel::dismissMeteredDictionaryDownload,
            onLanguage = viewModel::setLanguage,
            onTheme = viewModel::setTheme,
            onFontSize = viewModel::setFontSize,
            onReadingFont = viewModel::setReadingFont,
            onLineSpacing = viewModel::setLineSpacing,
            onPageMargin = viewModel::setPageMargin,
            onReaderLayout = viewModel::setReaderLayout,
            onAnnotateHints = viewModel::setAnnotateHints,
            onDefinitionMode = viewModel::setDefinitionMode,
            onGuideMode = viewModel::setGuideMode,
        )

        Screen.SYNC -> SyncScreen(
            strings = strings,
            state = state.sync,
            onBack = viewModel::back,
            onProvider = viewModel::setSyncProvider,
            onUrl = viewModel::setSyncUrl,
            onUsername = viewModel::setSyncUsername,
            onAllowHttp = viewModel::setSyncAllowHttp,
            onSave = viewModel::saveSyncConfig,
            onSyncNow = viewModel::syncNow,
            onDisable = viewModel::disableSync,
            onExport = { exportLauncher.launch(backupFileName()) },
            onImport = { importLauncher.launch(arrayOf("application/json", "text/plain", "*/*")) },
            onConfirmImport = viewModel::confirmImport,
            onCancelImport = viewModel::cancelImport,
            onDismissNotice = viewModel::dismissSyncNotice,
        )

        Screen.ABOUT -> AboutScreen(
            strings = strings,
            appVersion = appVersion,
            appVersionCode = appVersionCode,
            onBack = viewModel::back,
            onOpenPrivacy = viewModel::openAboutPrivacy,
            onOpenLicenses = viewModel::openAboutLicenses,
        )

        Screen.ABOUT_PRIVACY -> AboutArticleScreen(
            title = strings.aboutCopy.privacy,
            sections = strings.aboutCopy.privacySections,
            onBack = viewModel::back,
        )

        Screen.ABOUT_LICENSES -> AboutArticleScreen(
            title = strings.aboutCopy.licenses,
            sections = strings.aboutCopy.licenseSections,
            onBack = viewModel::back,
        )

        Screen.BOOK_DETAIL -> {
            val detail = state.bookDetail
            if (detail == null) {
                MainTabsShell(viewModel = viewModel, state = state, strings = strings)
            } else {
                BookOverviewScreen(
                    strings = strings,
                    state = detail,
                    onBack = viewModel::back,
                    onToggleShelf = { viewModel.toggleShelf(detail.bookId) },
                    onRead = viewModel::startBookFromDetail,
                    onReread = { viewModel.rereadBook(detail.bookId) },
                    onOpenFinished = viewModel::openFinishedSummary,
                    onDownload = viewModel::downloadFromDetail,
                    onOpenInfo = viewModel::openBookInfo,
                    onOpenChapter = { viewModel.openChapterFromDetail(detail.bookId, it.id) },
                )
            }
        }

        Screen.SEARCH -> BookSearchScreen(
            strings = strings,
            state = state.search,
            onBack = viewModel::back,
            onQueryChange = viewModel::updateSearchQuery,
            onOpenHit = viewModel::openSearchHit,
        )

        Screen.BOOK_FINISHED -> {
            val finished = state.finishedBook
            if (finished == null) {
                MainTabsShell(viewModel = viewModel, state = state, strings = strings)
            } else {
                FinishedBookScreen(
                    strings = strings,
                    state = finished,
                    onReread = { viewModel.rereadBook(finished.bookId) },
                    onBackToShelf = viewModel::backToShelf,
                    onBrowseStore = viewModel::openStoreFromFinished,
                )
            }
        }

        Screen.BOOK_INFO -> {
            val detail = state.bookDetail
            if (detail == null) {
                MainTabsShell(viewModel = viewModel, state = state, strings = strings)
            } else {
                BookInfoScreen(
                    strings = strings,
                    state = detail,
                    onBack = viewModel::back,
                )
            }
        }

        Screen.ASSESSMENT -> AssessmentScreen(
            strings = strings,
            state = state.assessment,
            onBack = viewModel::requestCloseAssessment,
            onDismissExit = viewModel::dismissAssessmentExit,
            onConfirmExit = viewModel::closeAssessment,
            onStart = viewModel::startAssessment,
            onSelectOption = viewModel::selectAssessmentOption,
            onSkip = viewModel::skipAssessmentQuestion,
            onNext = viewModel::nextAssessmentQuestion,
            onPrevious = viewModel::previousAssessmentQuestion,
            onAccept = viewModel::acceptAssessmentResult,
            onKeep = viewModel::keepAssessmentResult,
        )

        Screen.GRAMMAR_POINT -> {
            val point = state.grammarPoint
            if (point == null) {
                MainTabsShell(viewModel = viewModel, state = state, strings = strings)
            } else {
                GrammarPointScreen(
                    strings = strings,
                    point = point,
                    onBack = viewModel::back,
                    onOpenPoint = viewModel::openGrammarPoint,
                )
            }
        }

        Screen.GRAMMAR_DETAIL -> {
            val detail = state.grammarDetail
            if (detail == null) {
                MainTabsShell(viewModel = viewModel, state = state, strings = strings)
            } else {
                GrammarDetailScreen(
                    strings = strings,
                    state = detail,
                    onBack = viewModel::back,
                    onOpenUnit = { viewModel.openUnit(detail.bookId, detail.unitId) },
                    onOpenPoint = viewModel::openGrammarPoint,
                )
            }
        }
    }

    state.levelPrompt?.let { prompt ->
        LevelPromptSheet(
            strings = strings,
            prompt = prompt,
            onAccept = viewModel::acceptLevelPrompt,
            onKeep = viewModel::keepLevelPrompt,
            onApply = viewModel::applyLevelPrompt,
            onDismiss = viewModel::dismissLevelPrompt,
        )
    }
}

@Composable
private fun navColors() = NavigationBarItemDefaults.colors(
    selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
    selectedTextColor = MaterialTheme.colorScheme.primary,
    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
)

@Composable
private fun MainTabsShell(
    viewModel: PagelenceViewModel,
    state: AppUiState,
    strings: Strings,
) {
    // Local packages come through the system document picker; the app reads the
    // chosen document itself, so no storage permission is needed.
    val packagePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(viewModel::importLocalPackage)
    }
    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 0.dp,
            ) {
                NavigationBarItem(
                    selected = state.tab == MainTab.SHELF,
                    onClick = { viewModel.selectTab(MainTab.SHELF) },
                    icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = strings.tabShelf) },
                    label = { Text(strings.tabShelf) },
                    colors = navColors(),
                )
                NavigationBarItem(
                    selected = state.tab == MainTab.STORE,
                    onClick = { viewModel.selectTab(MainTab.STORE) },
                    icon = { Icon(Icons.Filled.ShoppingCart, contentDescription = strings.tabStore) },
                    label = { Text(strings.tabStore) },
                    colors = navColors(),
                )
                NavigationBarItem(
                    selected = state.tab == MainTab.REVIEW,
                    onClick = { viewModel.selectTab(MainTab.REVIEW) },
                    icon = { Icon(Icons.Filled.Refresh, contentDescription = strings.tabReview) },
                    label = { Text(strings.tabReview) },
                    colors = navColors(),
                )
                NavigationBarItem(
                    selected = state.tab == MainTab.PROFILE,
                    onClick = { viewModel.selectTab(MainTab.PROFILE) },
                    icon = { Icon(Icons.Filled.Person, contentDescription = strings.tabMe) },
                    label = { Text(strings.tabMe) },
                    colors = navColors(),
                )
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (state.screen) {
                Screen.LIBRARY -> LibraryScreen(
                    strings = strings,
                    state = state.library,
                    onOpenBook = viewModel::openBook,
                    onOpenBookDetail = viewModel::openBookDetail,
                    onReread = viewModel::rereadBook,
                    onOpenSettings = viewModel::openSettings,
                    onOpenProfile = viewModel::openProfile,
                )

                Screen.STORE -> StoreScreen(
                    strings = strings,
                    state = state.store,
                    onOpenBook = viewModel::openBook,
                    onOpenDetail = viewModel::openBookDetail,
                    onRefresh = viewModel::refreshStore,
                    onToggleLocalHttp = viewModel::setAllowLocalHttp,
                    onAddSource = viewModel::addCustomSource,
                    onToggleSource = viewModel::toggleCustomSource,
                    onDeleteSource = viewModel::deleteCustomSource,
                    onDownload = viewModel::downloadBook,
                    onRemoveDownloaded = viewModel::removeDownloadedBook,
                    onImportLocal = { packagePicker.launch(arrayOf("*/*")) },
                    onImportOnline = viewModel::importOnlinePackage,
                )

                Screen.REVIEW -> ReviewScreen(
                    strings = strings,
                    tab = state.reviewTab,
                    cards = state.cards,
                    grammar = state.grammarTab,
                    annotations = state.annotations,
                    annotationFilter = state.annotationFilter,
                    lookups = state.lookupHistory,
                    onSelectTab = viewModel::selectReviewTab,
                    onSelectAnnotationFilter = viewModel::selectAnnotationFilter,
                    onOpenAnnotation = viewModel::openAnnotation,
                    onOpenLookup = viewModel::openLookup,
                    onClearLookups = viewModel::clearLookups,
                    onOpenPoint = viewModel::openGrammarPoint,
                    onDeleteCard = viewModel::deleteCard,
                )

                Screen.PROFILE -> ProfileScreen(
                    strings = strings,
                    state = state.profile,
                    onBack = null,
                    onOpenSettings = viewModel::openSettings,
                    onSelectLevel = viewModel::selectLevel,
                    onOpenAssessment = viewModel::openAssessment,
                )

                else -> Unit
            }
        }
    }
}

/** Default SAF name: chronological file names make backups sortable. */
private fun backupFileName(): String {
    val stamp = DateTimeFormatter.ofPattern("yyyyMMdd-HHmm").format(LocalDateTime.now())
    return "pagelence-backup-$stamp.json"
}
