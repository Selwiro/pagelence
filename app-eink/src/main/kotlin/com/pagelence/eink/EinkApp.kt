package com.pagelence.eink

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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pagelence.PagelenceViewModel
import com.pagelence.RecoveryNotice
import com.pagelence.Screen
import com.pagelence.uim3.Strings
import com.pagelence.uim3.model.FinishedBookUi
import com.pagelence.uim3.stringsFor
import com.pagelence.uieink.components.InkButton
import com.pagelence.uieink.components.InkNotice
import com.pagelence.uieink.components.InkText
import com.pagelence.uieink.components.InkTextButton
import com.pagelence.uieink.components.InkTopBar
import com.pagelence.uieink.screens.EinkReaderScreen
import com.pagelence.uieink.screens.EinkShelfScreen
import com.pagelence.uieink.theme.InkTheme
import com.pagelence.uieink.theme.LocalInkColors
import com.pagelence.uieink.theme.LocalInkTypography

/**
 * 墨水屏壳的根组件：只做屏幕切换与事件转发，与普通版的 `PagelenceApp` 同构。
 *
 * 本轮覆盖书架、阅读（导读/正文/测验/语法）、读完页与首次启动；书城、复习、
 * 我的、设置仍是占位页（[Strings.einkUnavailable]），后续增量补齐。
 */
@Composable
fun EinkApp(
    viewModel: PagelenceViewModel,
    onFullRefresh: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.ui.collectAsState()
    val strings = stringsFor(state.settings.language)
    InkTheme {
        Column(modifier.fillMaxSize()) {
            state.recovery?.let { recovery ->
                RecoveryBar(
                    strings = strings,
                    recovery = recovery,
                    onRetry = viewModel::retryContent,
                    onDismiss = viewModel::dismissRecovery,
                )
            }
            Box(Modifier.weight(1f).fillMaxWidth()) {
                when (state.screen) {
                    Screen.LIBRARY -> {
                        // 书架只列在读书；冷启动时用随包书单补一段「可读的书」，
                        // 否则新装用户没有入口（墨屏版本轮还没有书城页）。
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
                            builtIn = state.store.builtIn,
                        )
                    }
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
                            )
                        }
                    }
                    Screen.ONBOARDING -> OnboardingPage(strings, viewModel::skipOnboarding)
                    else -> PlaceholderPage(strings, viewModel::back)
                }
                if (state.readerExitConfirm) {
                    Overlay {
                        ConfirmPage(
                            strings = strings,
                            title = strings.quizExitTitle,
                            body = strings.quizExitBody,
                            confirmLabel = strings.quizExitConfirm,
                            onConfirm = viewModel::closeReader,
                            stayLabel = strings.quizStay,
                            onStay = viewModel::dismissReaderExit,
                        )
                    }
                }
                state.levelPrompt?.let { prompt ->
                    Overlay {
                        ConfirmPage(
                            strings = strings,
                            title = strings.levelPromptTitle,
                            body = strings.levelPromptBody,
                            confirmLabel = strings.levelPromptAccept,
                            onConfirm = viewModel::acceptLevelPrompt,
                            stayLabel = strings.levelPromptKeep,
                            onStay = viewModel::keepLevelPrompt,
                        )
                    }
                }
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
private fun ConfirmPage(
    strings: Strings,
    title: String,
    body: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    stayLabel: String,
    onStay: () -> Unit,
) {
    val typography = LocalInkTypography.current
    Column(Modifier.fillMaxSize().padding(24.dp)) {
        InkText(title, style = typography.title)
        Spacer(Modifier.height(12.dp))
        InkText(body, style = typography.body)
        Spacer(Modifier.height(24.dp))
        InkButton(confirmLabel, onClick = onConfirm, primary = true)
        Spacer(Modifier.height(10.dp))
        InkButton(stayLabel, onClick = onStay)
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

/** 供 Activity 传入的「全刷」接入点类型说明见 [EinkRefresher]。 */
interface EinkRefresher {
    fun fullRefresh()
}

/** 没有厂商刷新 SDK 时的默认实现：什么都不做，仅保证调用点存在。 */
object NoopEinkRefresher : EinkRefresher {
    override fun fullRefresh() = Unit
}
