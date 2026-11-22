package com.pagelence.uieink.screens

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pagelence.core.progress.AnnotationKind
import com.pagelence.core.wordcards.CardMastery
import com.pagelence.core.wordcards.ReviewGrade
import com.pagelence.uim3.Strings
import com.pagelence.uim3.model.AnnotationFilter
import com.pagelence.uim3.model.AnnotationUi
import com.pagelence.uim3.model.GrammarPointUi
import com.pagelence.uim3.model.GrammarTabUiState
import com.pagelence.uim3.model.LookupHistoryUi
import com.pagelence.uim3.model.ReviewSessionUi
import com.pagelence.uim3.model.ReviewTab
import com.pagelence.uim3.model.WordCardUi
import com.pagelence.uim3.model.WordReviewUi
import com.pagelence.uieink.components.InkButton
import com.pagelence.uieink.components.InkDivider
import com.pagelence.uieink.components.InkListRow
import com.pagelence.uieink.components.InkTabBar
import com.pagelence.uieink.components.InkText
import com.pagelence.uieink.components.InkTextButton
import com.pagelence.uieink.components.InkTopBar
import com.pagelence.uieink.theme.LocalInkColors
import com.pagelence.uieink.theme.LocalInkTypography

/** 复习页需要的四块数据；参数单独传，避免 UI 层依赖壳的状态类型。 */
data class EinkReviewData(
    val tab: ReviewTab = ReviewTab.WORDS,
    val words: WordReviewUi = WordReviewUi(),
    val session: ReviewSessionUi? = null,
    val grammar: GrammarTabUiState = GrammarTabUiState(),
    val annotations: List<AnnotationUi> = emptyList(),
    val annotationFilter: AnnotationFilter = AnnotationFilter.ALL,
    val lookups: List<LookupHistoryUi> = emptyList(),
)

/**
 * 复习：单词（含间隔重复轮次）、语法、笔记、查词四个子页。
 *
 * 与普通版同一套数据与动作（`:presentation` 的 WordReviewUi / ReviewSessionUi），
 * 只是把浮层与提示条换成整页与常驻行。
 */
@Composable
fun EinkReviewScreen(
    strings: Strings,
    data: EinkReviewData,
    onSelectTab: (ReviewTab) -> Unit,
    onStartReview: () -> Unit,
    onReveal: () -> Unit,
    onGrade: (ReviewGrade) -> Unit,
    onCloseSession: () -> Unit,
    onDeleteCard: (String) -> Unit,
    onOpenGrammarPoint: (String) -> Unit,
    onOpenAnnotation: (AnnotationUi) -> Unit,
    onSelectFilter: (AnnotationFilter) -> Unit,
    onOpenLookup: (LookupHistoryUi) -> Unit,
    onClearLookups: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tabIndex = data.tab.ordinal.coerceAtLeast(0)
    Column(modifier.fillMaxSize()) {
        InkTopBar(title = strings.tabReview)
        if (data.session != null) {
            ReviewSessionView(
                strings = strings,
                session = data.session,
                onReveal = onReveal,
                onGrade = onGrade,
                onClose = onCloseSession,
            )
            return@Column
        }
        InkTabBar(
            labels = listOf(strings.reviewWords, strings.reviewGrammar, strings.reviewNotes, strings.reviewLookups),
            selected = tabIndex,
            onSelect = { index ->
                ReviewTab.entries.getOrNull(index)?.let(onSelectTab)
            },
        )
        when (data.tab) {
            ReviewTab.WORDS -> WordReviewView(
                strings = strings,
                words = data.words,
                onStart = onStartReview,
                onDelete = onDeleteCard,
            )
            ReviewTab.GRAMMAR -> GrammarListView(strings, data.grammar, onOpenGrammarPoint)
            ReviewTab.NOTES -> NotesView(strings, data, onSelectFilter, onOpenAnnotation)
            ReviewTab.LOOKUPS -> LookupsView(strings, data.lookups, onOpenLookup, onClearLookups)
        }
    }
}

@Composable
private fun WordReviewView(
    strings: Strings,
    words: WordReviewUi,
    onStart: () -> Unit,
    onDelete: (String) -> Unit,
) {
    var oftenWrongOnly by remember { mutableStateOf(false) }
    val colors = LocalInkColors.current
    val typography = LocalInkTypography.current
    val rows = if (oftenWrongOnly) words.rows.filter { it.lapses > 0 } else words.rows
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
            InkText(
                strings.reviewDueToday.format(words.dueCount, words.newCount),
                style = typography.uiStrong,
            )
            Spacer(Modifier.height(4.dp))
            InkText(
                "${strings.wordCards} · ${words.rows.size}",
                style = typography.caption,
                color = colors.inkMuted,
            )
        }
        if (words.hasWork) {
            InkButton(
                label = strings.reviewStartDue,
                onClick = onStart,
                primary = true,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }
        Spacer(Modifier.height(10.dp))
        InkDivider()
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
            InkTextButton(
                label = (if (!oftenWrongOnly) "■ " else "□ ") + strings.reviewFilterAll,
                onClick = { oftenWrongOnly = false },
            )
            InkTextButton(
                label = (if (oftenWrongOnly) "■ " else "□ ") + strings.reviewFilterOftenWrong,
                onClick = { oftenWrongOnly = true },
            )
        }
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            if (rows.isEmpty()) {
                Column(Modifier.padding(16.dp)) {
                    InkText(
                        if (oftenWrongOnly) strings.reviewEmptyOftenWrongTitle else strings.reviewEmptyDueTitle,
                        style = typography.title,
                    )
                    Spacer(Modifier.height(6.dp))
                    InkText(
                        if (oftenWrongOnly) strings.reviewEmptyOftenWrongBody else strings.reviewEmptyDueBody,
                        style = typography.ui,
                        color = colors.inkMuted,
                    )
                }
            } else {
                rows.forEach { card ->
                    WordCardRow(strings, card, onDelete)
                    InkDivider()
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun WordCardRow(strings: Strings, card: WordCardUi, onDelete: (String) -> Unit) {
    val colors = LocalInkColors.current
    val typography = LocalInkTypography.current
    val meta = buildList {
        add(masteryLabel(strings, card.mastery))
        card.dueAt?.let { add(strings.reviewDueOn.format(it)) }
        if (card.lapses > 0) add(strings.reviewLapses.format(card.lapses))
    }
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)) {
        InkText(
            listOfNotNull(card.term, card.ipa?.takeIf { it.isNotBlank() }?.let { "/$it/" }).joinToString("  "),
            style = typography.uiStrong,
        )
        (card.zh ?: card.en)?.takeIf { it.isNotBlank() }?.let {
            Spacer(Modifier.height(3.dp))
            InkText(it, style = typography.caption, color = colors.inkSecondary, maxLines = 2)
        }
        Spacer(Modifier.height(3.dp))
        Row(Modifier.fillMaxWidth()) {
            InkText(meta.joinToString(" · "), style = typography.caption, color = colors.inkMuted, modifier = Modifier.weight(1f))
            InkTextButton(strings.delete, onClick = { onDelete(card.id) })
        }
    }
}

@Composable
private fun ReviewSessionView(
    strings: Strings,
    session: ReviewSessionUi,
    onReveal: () -> Unit,
    onGrade: (ReviewGrade) -> Unit,
    onClose: () -> Unit,
) {
    val colors = LocalInkColors.current
    val typography = LocalInkTypography.current
    val card = session.current
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 12.dp)) {
        InkText(
            if (card != null) "${session.index + 1} / ${session.cards.size}" else strings.studyDone,
            style = typography.caption,
            color = colors.inkMuted,
        )
        Spacer(Modifier.height(10.dp))
        if (card == null) {
            InkText(strings.reviewSummary.format(session.cards.size, session.remembered, session.unsure, session.forgotten), style = typography.title)
            Spacer(Modifier.height(20.dp))
            InkButton(strings.reviewSummaryClose, onClick = onClose, primary = true)
            return@Column
        }
        InkText(card.term, style = typography.display)
        card.ipa?.takeIf { it.isNotBlank() }?.let {
            Spacer(Modifier.height(4.dp))
            InkText("/$it/", style = typography.bodySmall, color = colors.inkSecondary)
        }
        Spacer(Modifier.height(16.dp))
        if (session.revealed) {
            card.zh?.takeIf { it.isNotBlank() }?.let {
                InkText(it, style = typography.body)
                Spacer(Modifier.height(8.dp))
            }
            card.en?.takeIf { it.isNotBlank() }?.let {
                InkText(it, style = typography.bodySmall, color = colors.inkSecondary)
                Spacer(Modifier.height(8.dp))
            }
            card.exampleEn?.takeIf { it.isNotBlank() }?.let {
                InkText(it, style = typography.bodySmall, color = colors.inkSecondary)
                Spacer(Modifier.height(6.dp))
            }
            card.exampleZh?.takeIf { it.isNotBlank() }?.let {
                InkText(it, style = typography.bodySmall, color = colors.inkMuted)
                Spacer(Modifier.height(6.dp))
            }
            Spacer(Modifier.height(12.dp))
            InkDivider()
            Spacer(Modifier.height(12.dp))
            InkButton(strings.reviewGradeAgain, onClick = { onGrade(ReviewGrade.AGAIN) }, primary = true)
            Spacer(Modifier.height(8.dp))
            InkButton(strings.reviewGradeHard, onClick = { onGrade(ReviewGrade.HARD) })
            Spacer(Modifier.height(8.dp))
            InkButton(strings.reviewGradeGood, onClick = { onGrade(ReviewGrade.GOOD) })
        } else {
            InkButton(strings.studyShowAnswer, onClick = onReveal, primary = true)
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun GrammarListView(
    strings: Strings,
    grammar: GrammarTabUiState,
    onOpen: (String) -> Unit,
) {
    val colors = LocalInkColors.current
    val typography = LocalInkTypography.current
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        if (grammar.points.isEmpty()) {
            InkText(
                if (grammar.loading) strings.loading else strings.grammarEmpty,
                style = typography.ui,
                color = colors.inkMuted,
                modifier = Modifier.padding(16.dp),
            )
        } else {
            InkText(
                strings.grammarPointsCount.format(grammar.points.size),
                style = typography.caption,
                color = colors.inkMuted,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            )
            grammar.points.forEach { point ->
                InkListRow(
                    title = point.name,
                    subtitle = point.levelLabel,
                    trailing = point.levelStart,
                    onClick = { onOpen(point.id) },
                )
                InkDivider()
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
fun EinkGrammarPointScreen(
    strings: Strings,
    point: GrammarPointUi,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalInkColors.current
    val typography = LocalInkTypography.current
    Column(modifier.fillMaxSize()) {
        InkTopBar(title = strings.grammar, subtitle = point.levelLabel, backLabel = strings.back, onBack = onBack)
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 12.dp)) {
            InkText(point.name, style = typography.title)
            Spacer(Modifier.height(12.dp))
            InkText(strings.grammarExplain, style = typography.uiStrong)
            InkText(point.explain, style = typography.bodySmall, color = colors.inkSecondary)
            Spacer(Modifier.height(12.dp))
            InkText(strings.grammarForm, style = typography.uiStrong)
            InkText(point.form, style = typography.bodySmall, color = colors.inkSecondary)
            if (point.pitfalls.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                InkText(strings.grammarPitfalls, style = typography.uiStrong)
                point.pitfalls.forEach {
                    InkText("· $it", style = typography.bodySmall, color = colors.inkSecondary)
                }
            }
            if (point.examples.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                InkText(strings.grammarExamples, style = typography.uiStrong)
                point.examples.forEach { example ->
                    Spacer(Modifier.height(8.dp))
                    InkText(example.text, style = typography.bodySmall)
                    example.zh?.takeIf { it.isNotBlank() }?.let {
                        InkText(it, style = typography.bodySmall, color = colors.inkMuted)
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun NotesView(
    strings: Strings,
    data: EinkReviewData,
    onSelectFilter: (AnnotationFilter) -> Unit,
    onOpen: (AnnotationUi) -> Unit,
) {
    val colors = LocalInkColors.current
    val typography = LocalInkTypography.current
    var showFilter by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
            InkText(
                strings.annotationCount.format(data.annotations.size),
                style = typography.caption,
                color = colors.inkMuted,
                modifier = Modifier.weight(1f),
            )
            InkTextButton(
                label = filterLabel(strings, data.annotationFilter),
                onClick = { showFilter = !showFilter },
            )
        }
        if (showFilter) {
            listOf(
                AnnotationFilter.ALL to strings.annotationFilterAll,
                AnnotationFilter.BOOKMARK to strings.annotationBookmark,
                AnnotationFilter.HIGHLIGHT to strings.annotationHighlight,
                AnnotationFilter.NOTE to strings.annotationNote,
            ).forEach { (filter, label) ->
                InkListRow(
                    title = label,
                    trailing = if (data.annotationFilter == filter) "■" else "",
                    onClick = {
                        onSelectFilter(filter)
                        showFilter = false
                    },
                )
                InkDivider()
            }
        }
        InkDivider()
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            val rows = data.annotations.filter {
                data.annotationFilter == AnnotationFilter.ALL || it.kind.matches(data.annotationFilter)
            }
            if (rows.isEmpty()) {
                Column(Modifier.padding(16.dp)) {
                    InkText(strings.annotationEmptyTitle, style = typography.title)
                }
            } else {
                rows.forEach { annotation ->
                    InkListRow(
                        title = annotation.quote,
                        subtitle = listOf(
                            kindLabel(strings, annotation.kind),
                            annotation.bookTitle,
                            annotation.unitTitle,
                        ).joinToString(" · "),
                        trailing = annotation.dateLabel,
                        onClick = { onOpen(annotation) },
                    )
                    annotation.note?.takeIf { it.isNotBlank() }?.let {
                        InkText(it, style = typography.caption, color = colors.inkSecondary, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
                    }
                    InkDivider()
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun LookupsView(
    strings: Strings,
    lookups: List<LookupHistoryUi>,
    onOpen: (LookupHistoryUi) -> Unit,
    onClear: () -> Unit,
) {
    val colors = LocalInkColors.current
    val typography = LocalInkTypography.current
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
            InkText(
                strings.lookupCount.format(lookups.size),
                style = typography.caption,
                color = colors.inkMuted,
                modifier = Modifier.weight(1f),
            )
            if (lookups.isNotEmpty()) {
                InkTextButton(label = strings.lookupClear, onClick = onClear)
            }
        }
        InkDivider()
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            if (lookups.isEmpty()) {
                Column(Modifier.padding(16.dp)) {
                    InkText(strings.lookupEmptyTitle, style = typography.title)
                    Spacer(Modifier.height(6.dp))
                    InkText(strings.lookupEmptyHint, style = typography.ui, color = colors.inkMuted)
                }
            } else {
                lookups.forEach { entry ->
                    InkListRow(
                        title = entry.term,
                        subtitle = listOfNotNull(
                            entry.gloss,
                            "${entry.bookTitle} · ${entry.unitLabel}",
                        ).joinToString(" · "),
                        trailing = if (entry.available) entry.lookedUpAtLabel else strings.lookupBookRemoved,
                        onClick = { onOpen(entry) },
                    )
                    InkDivider()
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

private fun masteryLabel(strings: Strings, mastery: CardMastery): String = when (mastery) {
    CardMastery.NEW -> strings.masteryNew
    CardMastery.LEARNING -> strings.masteryLearning
    CardMastery.MASTERED -> strings.masteryMastered
}

private fun AnnotationKind.matches(filter: AnnotationFilter): Boolean = when (filter) {
    AnnotationFilter.ALL -> true
    AnnotationFilter.BOOKMARK -> this == AnnotationKind.BOOKMARK
    AnnotationFilter.HIGHLIGHT -> this == AnnotationKind.HIGHLIGHT
    AnnotationFilter.NOTE -> this == AnnotationKind.NOTE
}

private fun kindLabel(strings: Strings, kind: AnnotationKind): String = when (kind) {
    AnnotationKind.BOOKMARK -> strings.annotationBookmark
    AnnotationKind.HIGHLIGHT -> strings.annotationHighlight
    AnnotationKind.NOTE -> strings.annotationNote
}

private fun filterLabel(strings: Strings, filter: AnnotationFilter): String = when (filter) {
    AnnotationFilter.ALL -> strings.annotationFilterAll
    AnnotationFilter.BOOKMARK -> strings.annotationBookmark
    AnnotationFilter.HIGHLIGHT -> strings.annotationHighlight
    AnnotationFilter.NOTE -> strings.annotationNote
}
