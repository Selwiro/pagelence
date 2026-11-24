package com.pagelence.eink

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.pagelence.MainTab
import com.pagelence.PagelenceViewModel
import com.pagelence.RecoveryNotice
import com.pagelence.Screen
import com.pagelence.presentation.eink.EinkRefreshSettings
import com.pagelence.uim3.Strings
import com.pagelence.uim3.model.FinishedBookUi
import com.pagelence.uim3.model.ReviewTab
import com.pagelence.uim3.stringsFor
import com.pagelence.uieink.components.InkButton
import com.pagelence.uieink.components.InkConfirmPage
import com.pagelence.uieink.components.InkNotice
import com.pagelence.uieink.components.InkTabBar
import com.pagelence.uieink.components.InkText
import com.pagelence.uieink.components.InkTextButton
import com.pagelence.uieink.components.InkTopBar
import com.pagelence.uieink.screens.EinkAboutScreen
import com.pagelence.uieink.screens.EinkAchievementsScreen
import com.pagelence.uieink.screens.EinkBookDetailScreen
import com.pagelence.uieink.screens.EinkDictionaryScreen
import com.pagelence.uieink.screens.EinkGrammarPointScreen
import com.pagelence.uieink.screens.EinkProfileScreen
import com.pagelence.uieink.screens.EinkReaderScreen
import com.pagelence.uieink.screens.EinkReviewData
import com.pagelence.uieink.screens.EinkReviewScreen
import com.pagelence.uieink.screens.EinkSearchScreen
import com.pagelence.uieink.screens.EinkSettingsScreen
import com.pagelence.uieink.screens.EinkShelfScreen
import com.pagelence.uieink.screens.levelName
import com.pagelence.uieink.screens.EinkStoreScreen
import com.pagelence.uieink.screens.EinkSyncScreen
import com.pagelence.uieink.screens.EinkTypographyScreen
import com.pagelence.uieink.screens.EinkWordPanelScreen
import com.pagelence.uieink.theme.InkTheme
import com.pagelence.uieink.theme.LocalInkColors
import com.pagelence.uieink.theme.LocalInkTypography

/**
 * 墨水屏壳根组件：屏幕切换、四栏导航与事件转发，与普通版 `PagelenceApp` 同构。
 *
 * 覆盖：书架、书城、复习（四子页）、我的、设置、同步、阅读、读完页、语法点、
 * 关于与首次启动。业务判断全部在 `:shell` 的 ViewModel / `:core`。
 */
@Composable
fun EinkApp(
    viewModel: PagelenceViewModel,
    appVersion: String,
    onFullRefresh: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.ui.collectAsState()
    val strings = stringsFor(state.settings.language)
    val context = LocalContext.current
    var refreshSettings by remember { mutableStateOf(EinkDeviceSettings.refresh(context)) }
    var forceMoaanRefresh by remember { mutableStateOf(EinkDeviceSettings.forceMoaanBroadcast(context)) }
    val refreshBackend = remember(forceMoaanRefresh) {
        when (EinkRefreshers.create(context, forceMoaanRefresh).backend) {
            RefreshBackend.MOAAN_BROADCAST -> strings.refreshBackendMoaan
            RefreshBackend.NONE -> strings.refreshBackendNone
        }
    }

    // 书城：导入本地书籍包；同步：导出/导入整份进度。两条都用 SAF，不申请存储权限。
    val importBookLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri -> uri?.let(viewModel::importLocalPackage) }
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri -> uri?.let(viewModel::exportState) }
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri -> uri?.let(viewModel::importState) }

    val root = state.screen == Screen.LIBRARY && state.tab == MainTab.SHELF ||
        state.screen == Screen.STORE && state.tab == MainTab.STORE ||
        state.screen == Screen.REVIEW && state.tab == MainTab.REVIEW ||
        state.screen == Screen.PROFILE && state.tab == MainTab.PROFILE

    BackHandler(enabled = state.reader?.wordPanel != null) { viewModel.closeWordPanel() }
    BackHandler(enabled = state.reader?.wordPanel == null && !root) {
        when (state.screen) {
            Screen.READER -> viewModel.requestCloseReader()
            Screen.SETTINGS -> viewModel.backFromSettings()
            else -> viewModel.back()
        }
    }

    InkTheme {
        Column(modifier.fillMaxSize()) {
            state.recovery?.let { recovery ->
                RecoveryBar(strings, recovery, viewModel::retryContent, viewModel::dismissRecovery)
            }
            Box(Modifier.weight(1f).fillMaxWidth()) {
                when (state.screen) {
                    Screen.LIBRARY -> {
                        val needsBuiltIn = state.library.books.isEmpty() &&
                            state.library.finished.isEmpty() &&
                            state.store.builtIn.isEmpty() &&
                            !state.store.refreshing
                        LaunchedEffect(needsBuiltIn) {
                            if (needsBuiltIn) viewModel.refreshStore()
                        }
                        EinkShelfScreen(
                            strings = strings,
                            library = state.library,
                            onOpenBook = viewModel::openBook,
                            onRetry = viewModel::refreshLibrary,
                            onOpenDetail = viewModel::openBookDetail,
                            builtIn = state.store.builtIn,
                        )
                    }
                    Screen.STORE -> EinkStoreScreen(
                        strings = strings,
                        store = state.store,
                        onRefresh = viewModel::refreshStore,
                        onOpenBook = viewModel::openBook,
                        onOpenDetail = viewModel::openBookDetail,
                        onDownload = viewModel::downloadBook,
                        onRemoveDownloaded = viewModel::removeDownloadedBook,
                        onImportLocal = { importBookLauncher.launch(arrayOf("application/json", "application/octet-stream", "*/*")) },
                        onAddSource = viewModel::addCustomSource,
                        onToggleSource = viewModel::toggleCustomSource,
                        onDeleteSource = viewModel::deleteCustomSource,
                    )
                    Screen.REVIEW -> EinkReviewScreen(
                        strings = strings,
                        data = EinkReviewData(
                            tab = state.reviewTab,
                            words = state.review,
                            session = state.reviewSession,
                            grammar = state.grammarTab,
                            annotations = state.annotations,
                            annotationFilter = state.annotationFilter,
                            lookups = state.lookupHistory,
                        ),
                        onSelectTab = viewModel::selectReviewTab,
                        onStartReview = viewModel::startReview,
                        onReveal = viewModel::revealCard,
                        onGrade = viewModel::gradeCard,
                        onCloseSession = viewModel::closeReview,
                        onDeleteCard = viewModel::deleteCard,
                        onOpenGrammarPoint = viewModel::openGrammarPoint,
                        onOpenAnnotation = viewModel::openAnnotation,
                        onSelectFilter = viewModel::selectAnnotationFilter,
                        onOpenLookup = viewModel::openLookup,
                        onClearLookups = viewModel::clearLookups,
                    )
                    Screen.PROFILE -> EinkProfileScreen(
                        strings = strings,
                        profile = state.profile,
                        onOpenSettings = viewModel::openSettings,
                        onOpenAchievements = viewModel::openAchievements,
                        onSelectLevel = viewModel::selectLevel,
                        onDailyGoal = viewModel::setDailyGoal,
                    )
                    Screen.SETTINGS -> EinkSettingsScreen(
                        strings = strings,
                        settings = state.settings,
                        dictionary = state.dictionary,
                        refresh = refreshSettings,
                        refreshBackend = refreshBackend,
                        forceMoaanRefresh = forceMoaanRefresh,
                        appVersion = appVersion,
                        onBack = viewModel::backFromSettings,
                        onLanguage = viewModel::setLanguage,
                        onDailyGoal = viewModel::setDailyGoal,
                        onRefresh = { settings ->
                            refreshSettings = settings
                            EinkDeviceSettings.saveRefresh(context, settings)
                        },
                        onToggleForceMoaan = { force ->
                            forceMoaanRefresh = force
                            EinkDeviceSettings.saveForceMoaanBroadcast(context, force)
                        },
                        onTestFullRefresh = onFullRefresh,
                        onDefinitionMode = viewModel::setDefinitionMode,
                        onGuideMode = viewModel::setGuideMode,
                        onOpenAbout = viewModel::openAbout,
                        onOpenSync = viewModel::openSync,
                        onOpenTypography = viewModel::openTypography,
                        onOpenDictionary = viewModel::openDictionarySettings,
                    )
                    Screen.SETTINGS_TYPOGRAPHY -> EinkTypographyScreen(
                        strings = strings,
                        settings = state.settings,
                        onBack = viewModel::back,
                        onFontSize = viewModel::setFontSize,
                        onLineSpacing = viewModel::setLineSpacing,
                        onPageMargin = viewModel::setPageMargin,
                    )
                    Screen.SETTINGS_DICTIONARY -> EinkDictionaryScreen(
                        strings = strings,
                        dictionary = state.dictionary,
                        onBack = viewModel::back,
                        onDownload = viewModel::downloadDictionary,
                        onDelete = viewModel::deleteFullDictionary,
                    )
                    Screen.ACHIEVEMENTS -> EinkAchievementsScreen(
                        strings = strings,
                        wall = state.profile.achievements,
                        onBack = viewModel::back,
                    )
                    Screen.ABOUT -> EinkAboutScreen(
                        strings = strings,
                        appVersion = appVersion,
                        onBack = viewModel::back,
                    )
                    Screen.SEARCH -> {
                        val search = state.search
                        if (search == null) {
                            Loading(strings)
                        } else {
                            EinkSearchScreen(
                                strings = strings,
                                search = search,
                                onQuery = viewModel::updateSearchQuery,
                                onOpenHit = viewModel::openSearchHit,
                                onBack = viewModel::back,
                            )
                        }
                    }
                    Screen.BOOK_DETAIL -> {
                        val detail = state.bookDetail
                        if (detail == null) {
                            Loading(strings)
                        } else {
                            EinkBookDetailScreen(
                                strings = strings,
                                detail = detail,
                                onBack = viewModel::back,
                                onToggleShelf = { viewModel.toggleShelf(detail.bookId) },
                                onStartReading = viewModel::startBookFromDetail,
                                onOpenChapter = { chapter ->
                                    viewModel.openChapterFromDetail(detail.bookId, chapter.id)
                                },
                            )
                        }
                    }
                    Screen.GRAMMAR_POINT -> {
                        val point = state.grammarPoint
                        if (point == null) {
                            Loading(strings)
                        } else {
                            EinkGrammarPointScreen(strings, point, viewModel::back)
                        }
                    }
                    Screen.SYNC -> EinkSyncScreen(
                        strings = strings,
                        sync = state.sync,
                        onBack = viewModel::back,
                        onProvider = viewModel::setSyncProvider,
                        onUrl = viewModel::setSyncUrl,
                        onUsername = viewModel::setSyncUsername,
                        onAllowHttp = viewModel::setSyncAllowHttp,
                        onSave = viewModel::saveSyncConfig,
                        onSyncNow = viewModel::syncNow,
                        onDisable = viewModel::disableSync,
                        onExport = { exportLauncher.launch("pagelence-state.json") },
                        onImport = { importLauncher.launch(arrayOf("application/json", "application/octet-stream", "*/*")) },
                        onConfirmImport = viewModel::confirmImport,
                        onCancelImport = viewModel::cancelImport,
                    )
                    Screen.READER -> {
                        val reader = state.reader
                        if (reader == null) {
                            Loading(strings)
                        } else {
                            EinkReaderScreen(
                                strings = strings,
                                state = reader,
                                onBack = viewModel::requestCloseReader,
                                onPageChanged = viewModel::onReaderPageChanged,
                                onSelectOption = viewModel::selectOption,
                                onSubmitQuiz = viewModel::submitQuiz,
                                onNextUnit = viewModel::nextUnit,
                                onPreviousUnit = viewModel::previousUnit,
                                onFullRefresh = onFullRefresh,
                                onWordClick = viewModel::onWordClick,
                                onOpenSearch = viewModel::openBookSearch,
                                onToggleAllTranslations = viewModel::toggleAllTranslations,
                                onToggleBookmark = viewModel::toggleBookmark,
                                onToggleHighlight = viewModel::toggleHighlight,
                                onSaveNote = viewModel::saveNote,
                                onDeleteNote = viewModel::deleteNote,
                                onJumpHandled = viewModel::annotationJumpHandled,
                                onOpenSettings = viewModel::openSettings,
                                onOpenUnit = { unitId -> viewModel.openUnit(reader.bookId, unitId) },
                                onOpenCards = viewModel::openCards,
                                onOpenAnnotation = viewModel::openAnnotation,
                                onFontSize = viewModel::setFontSize,
                                onLineSpacing = viewModel::setLineSpacing,
                                onPageMargin = viewModel::setPageMargin,
                                refreshSettings = refreshSettings,
                            )
                        }
                    }
                    Screen.BOOK_FINISHED -> {
                        val finished = state.finishedBook
                        if (finished == null) {
                            Loading(strings)
                        } else {
                            FinishedPage(
                                strings = strings,
                                finished = finished,
                                onReread = { viewModel.rereadBook(finished.bookId) },
                                onBackToShelf = viewModel::backToShelf,
                                onOpenAchievements = viewModel::openAchievementsFromFinished,
                            )
                        }
                    }
                    Screen.ONBOARDING -> OnboardingPage(strings, viewModel::skipOnboarding)
                    else -> PlaceholderPage(strings, viewModel::back)
                }

                // 词条：整页覆盖，保留原页状态；返回键先关词条。
                val panel = state.reader?.wordPanel
                if (panel != null && state.screen == Screen.READER) {
                    Box(Modifier.fillMaxSize().background(LocalInkColors.current.paper)) {
                        EinkWordPanelScreen(
                            strings = strings,
                            panel = panel,
                            definitionMode = state.settings.definitionMode,
                            onToggleCard = viewModel::toggleCard,
                            onClose = viewModel::closeWordPanel,
                        )
                    }
                }
                if (state.readerExitConfirm) {
                    Overlay {
                        InkConfirmPage(
                            title = strings.quizExitTitle,
                            body = strings.quizExitBody,
                            confirmLabel = strings.quizExitConfirm,
                            onConfirm = viewModel::closeReader,
                            cancelLabel = strings.quizStay,
                            onCancel = viewModel::dismissReaderExit,
                        )
                    }
                }
                state.levelPrompt?.let { prompt ->
                    Overlay {
                        InkConfirmPage(
                            title = strings.levelPromptTitle.format(
                                levelName(strings, prompt.level),
                            ),
                            body = strings.levelPromptBody,
                            confirmLabel = strings.levelPromptAccept,
                            onConfirm = viewModel::acceptLevelPrompt,
                            cancelLabel = strings.levelPromptKeep,
                            onCancel = viewModel::keepLevelPrompt,
                        )
                    }
                }
            }
            if (root) {
                InkTabBar(
                    labels = listOf(strings.tabShelf, strings.tabStore, strings.tabReview, strings.tabMe),
                    selected = state.tab.ordinal.coerceAtLeast(0),
                    onSelect = { index ->
                        MainTab.entries.getOrNull(index)?.let(viewModel::selectTab)
                    },
                )
            }
        }
    }
}

@Composable
private fun Loading(strings: Strings) {
    InkText(
        strings.loading,
        style = LocalInkTypography.current.ui,
        color = LocalInkColors.current.inkMuted,
        modifier = Modifier.padding(16.dp),
    )
}

/** 覆盖层：白底整页，不做半透明遮罩（墨水屏上底色会糊）。 */
@Composable
private fun Overlay(content: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize().background(LocalInkColors.current.paper)) { content() }
}

@Composable
private fun RecoveryBar(
    strings: Strings,
    recovery: RecoveryNotice,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
) {
    val title = when (recovery) {
        RecoveryNotice.FROM_BACKUP -> strings.recoveryFromBackupTitle
        RecoveryNotice.CORRUPT -> strings.recoveryCorruptTitle
        RecoveryNotice.CONTENT_FAILED -> strings.recoveryContentFailedTitle
        RecoveryNotice.CONTENT_PARTIAL -> strings.recoveryContentPartialTitle
    }
    val body = when (recovery) {
        RecoveryNotice.FROM_BACKUP -> strings.recoveryFromBackupBody
        RecoveryNotice.CORRUPT -> strings.recoveryCorruptBody
        RecoveryNotice.CONTENT_FAILED -> strings.recoveryContentFailedBody
        RecoveryNotice.CONTENT_PARTIAL -> strings.recoveryContentPartialBody
    }
    InkNotice("$title\n$body")
    Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
        InkTextButton(
            label = if (recovery == RecoveryNotice.CONTENT_FAILED) strings.recoveryRetry else strings.recoveryOk,
            onClick = if (recovery == RecoveryNotice.CONTENT_FAILED) onRetry else onDismiss,
        )
    }
}

@Composable
private fun OnboardingPage(strings: Strings, onStart: () -> Unit) {
    val typography = LocalInkTypography.current
    Column(Modifier.fillMaxSize().padding(24.dp)) {
        InkText(strings.onboardingTitle, style = typography.title)
        Spacer(Modifier.height(12.dp))
        InkText(strings.onboardingBody, style = typography.body)
        Spacer(Modifier.height(24.dp))
        InkButton(strings.onboardingSkip, onClick = onStart, primary = true)
    }
}

@Composable
private fun FinishedPage(
    strings: Strings,
    finished: FinishedBookUi,
    onReread: () -> Unit,
    onBackToShelf: () -> Unit,
    onOpenAchievements: () -> Unit,
) {
    val typography = LocalInkTypography.current
    val colors = LocalInkColors.current
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp)) {
        InkText(strings.finishedTitle, style = typography.display)
        Spacer(Modifier.height(6.dp))
        InkText(strings.finishedSubtitle, style = typography.ui, color = colors.inkMuted)
        Spacer(Modifier.height(18.dp))
        InkText(finished.title, style = typography.title)
        finished.finishedOn?.let {
            Spacer(Modifier.height(4.dp))
            InkText(it, style = typography.caption, color = colors.inkMuted)
        }
        Spacer(Modifier.height(18.dp))
        InkText("${finished.unitCount} ${strings.finishedUnitsLabel}", style = typography.body)
        InkText("${strings.finishedWordsLabel} · ${finished.wordCount}", style = typography.body)
        InkText("${strings.finishedCardsLabel} · ${finished.cardCount}", style = typography.body)
        Spacer(Modifier.height(24.dp))
        InkButton(strings.readAgain, onClick = onReread, primary = true)
        Spacer(Modifier.height(10.dp))
        InkButton(strings.backToShelf, onClick = onBackToShelf)
        Spacer(Modifier.height(10.dp))
        InkButton(strings.achievementSeeAll, onClick = onOpenAchievements)
    }
}

@Composable
private fun PlaceholderPage(strings: Strings, onBack: () -> Unit) {
    val typography = LocalInkTypography.current
    Column(Modifier.fillMaxSize()) {
        InkTopBar(title = strings.appName, backLabel = strings.back, onBack = onBack)
        Column(Modifier.padding(24.dp)) {
            InkText(strings.einkUnavailable, style = typography.body)
        }
    }
}
