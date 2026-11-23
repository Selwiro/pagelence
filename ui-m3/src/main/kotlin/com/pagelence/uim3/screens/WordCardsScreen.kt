package com.pagelence.uim3.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pagelence.core.wordcards.CardMastery
import com.pagelence.core.wordcards.ReviewGrade
import com.pagelence.uim3.Strings
import com.pagelence.uim3.components.EmptyState
import com.pagelence.uim3.components.MetaChip
import com.pagelence.uim3.model.ReviewSessionUi
import com.pagelence.uim3.model.WordCardUi
import com.pagelence.uim3.model.WordReviewUi

/**
 * 复习·单词页（T143）：顶部是今日到期与新卡，下面是全部已收词条。
 *
 * 复习按间隔重复排期：一张卡评过之后不会再出现在今天的队列里，除非评了
 * 「忘了」（当天再来）。列表始终可见，方便回看和删除；筛选只影响列表。
 */
@Composable
fun WordCardsContent(
    strings: Strings,
    review: WordReviewUi,
    session: ReviewSessionUi?,
    onStartReview: () -> Unit,
    onReveal: () -> Unit,
    onGrade: (ReviewGrade) -> Unit,
    onCloseReview: () -> Unit,
    onDelete: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (session != null) {
        ReviewRound(
            strings = strings,
            session = session,
            onReveal = onReveal,
            onGrade = onGrade,
            onClose = onCloseReview,
            modifier = modifier,
        )
        return
    }
    if (review.rows.isEmpty()) {
        EmptyState(
            icon = Icons.AutoMirrored.Filled.List,
            title = strings.noCards,
            body = strings.noCardsHint,
            modifier = modifier.fillMaxSize(),
        )
        return
    }
    var oftenWrongOnly by remember { mutableStateOf(false) }
    val rows = if (oftenWrongOnly) review.rows.filter { it.lapses > 0 } else review.rows

    Column(modifier.fillMaxSize()) {
        ReviewHeader(strings, review, onStartReview)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterChip(
                selected = !oftenWrongOnly,
                onClick = { oftenWrongOnly = false },
                label = { Text(strings.reviewFilterAll, style = MaterialTheme.typography.labelLarge) },
            )
            FilterChip(
                selected = oftenWrongOnly,
                onClick = { oftenWrongOnly = true },
                label = { Text(strings.reviewFilterOftenWrong, style = MaterialTheme.typography.labelLarge) },
            )
        }
        if (rows.isEmpty()) {
            EmptyState(
                icon = Icons.Filled.CheckCircle,
                title = strings.reviewEmptyOftenWrongTitle,
                body = strings.reviewEmptyOftenWrongBody,
                modifier = Modifier.fillMaxWidth(),
            )
            return@Column
        }
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(rows, key = { it.id }) { card -> WordCardRow(strings, card, onDelete) }
        }
    }
}

/** 「今日到期 N · 新卡 M」and the start button; an empty hint when nothing is due. */
@Composable
private fun ReviewHeader(
    strings: Strings,
    review: WordReviewUi,
    onStartReview: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 18.dp, end = 14.dp, top = 12.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                strings.reviewDueToday.format(review.dueCount, review.newCount),
                style = MaterialTheme.typography.titleMedium,
                color = if (review.hasWork) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
            Text(
                strings.cardsText(review.rows.size),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (review.hasWork) {
            Button(onClick = onStartReview) { Text(strings.reviewStartDue) }
        }
    }
    if (!review.hasWork) {
        EmptyState(
            icon = Icons.Filled.CheckCircle,
            title = strings.reviewEmptyDueTitle,
            body = strings.reviewEmptyDueBody,
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
        )
    }
}

/** One card: word → reveal → three grades. */
@Composable
private fun ReviewRound(
    strings: Strings,
    session: ReviewSessionUi,
    onReveal: () -> Unit,
    onGrade: (ReviewGrade) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                strings.studyWords,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onClose) { Text(strings.studyExit) }
        }
        if (session.finished) {
            Spacer(Modifier.height(24.dp))
            Text(
                strings.reviewSummary.format(
                    session.reviewed,
                    session.remembered,
                    session.unsure,
                    session.forgotten,
                ),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(6.dp))
            Text(
                strings.studyDone,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(20.dp))
            Button(onClick = onClose, modifier = Modifier.fillMaxWidth()) {
                Text(strings.reviewSummaryClose)
            }
            return@Column
        }
        val card = session.current ?: return@Column
        Text(
            strings.studyProgress.format(session.index + 1, session.cards.size),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(14.dp))
        ElevatedCard(
            shape = MaterialTheme.shapes.large,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    card.term,
                    style = MaterialTheme.typography.displaySmall,
                    textAlign = TextAlign.Center,
                )
                card.ipa?.takeIf { it.isNotBlank() }?.let {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "/$it/",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (card.lemma.isNotBlank() && !card.lemma.equals(card.term, ignoreCase = true)) {
                    Text(
                        "${strings.lemmaBase} ${card.lemma}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.height(18.dp))
                if (!session.revealed) {
                    Text(
                        strings.studyPrompt,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(18.dp))
                    Button(onClick = onReveal) { Text(strings.studyShowAnswer) }
                } else {
                    val meaning = listOfNotNull(
                        card.zh?.takeIf { it.isNotBlank() },
                        card.en?.takeIf { it.isNotBlank() },
                    ).joinToString(" · ")
                    if (meaning.isNotBlank()) {
                        Text(
                            meaning,
                            style = MaterialTheme.typography.titleMedium,
                            textAlign = TextAlign.Center,
                        )
                    }
                    card.exampleEn?.let {
                        Spacer(Modifier.height(14.dp))
                        Text(
                            it,
                            style = MaterialTheme.typography.bodyMedium,
                            fontStyle = FontStyle.Italic,
                            textAlign = TextAlign.Center,
                        )
                    }
                    card.exampleZh?.let {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            it,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    MetaChip(card.bookTitle)
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        if (session.revealed) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = { onGrade(ReviewGrade.AGAIN) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                ) {
                    Text(strings.reviewGradeAgain)
                }
                OutlinedButton(
                    onClick = { onGrade(ReviewGrade.HARD) },
                    modifier = Modifier.weight(1f),
                ) {
                    Text(strings.reviewGradeHard)
                }
                Button(
                    onClick = { onGrade(ReviewGrade.GOOD) },
                    modifier = Modifier.weight(1f),
                ) {
                    Text(strings.reviewGradeGood)
                }
            }
        } else {
            // Reserve the row so the card does not jump when the answer appears.
            Spacer(Modifier.height(48.dp))
        }
    }
}

@Composable
private fun WordCardRow(strings: Strings, card: WordCardUi, onDelete: (String) -> Unit) {
    ElevatedCard(shape = MaterialTheme.shapes.large, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(start = 18.dp, top = 14.dp, end = 10.dp, bottom = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(card.term, style = MaterialTheme.typography.titleLarge)
                        card.ipa?.let {
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "/$it/",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    if (card.lemma.isNotBlank() && !card.lemma.equals(card.term, ignoreCase = true)) {
                        Text(
                            "${strings.lemmaBase} ${card.lemma}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    val meaning = listOfNotNull(
                        card.zh?.takeIf { it.isNotBlank() },
                        card.en?.takeIf { it.isNotBlank() },
                    ).joinToString(" · ")
                    if (meaning.isNotBlank()) {
                        Text(meaning, style = MaterialTheme.typography.bodyLarge)
                    }
                    Spacer(Modifier.height(6.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        MetaChip(masteryLabel(strings, card.mastery))
                        MetaChip(card.bookTitle)
                    }
                    val schedule = listOfNotNull(
                        card.dueAt?.let { strings.reviewDueOn.format(it) },
                        card.lapses.takeIf { it > 0 }?.let { strings.reviewLapses.format(it) },
                    ).joinToString(" · ")
                    if (schedule.isNotBlank()) {
                        Text(
                            schedule,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                }
                IconButton(onClick = { onDelete(card.id) }) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = strings.delete,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            card.exampleEn?.let { example ->
                Spacer(Modifier.height(12.dp))
                Text(
                    example,
                    style = MaterialTheme.typography.bodyMedium,
                    fontStyle = FontStyle.Italic,
                )
                card.exampleZh?.let { zh ->
                    Text(
                        zh,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

private fun masteryLabel(strings: Strings, mastery: CardMastery): String = when (mastery) {
    CardMastery.NEW -> strings.masteryNew
    CardMastery.LEARNING -> strings.masteryLearning
    CardMastery.MASTERED -> strings.masteryMastered
}
