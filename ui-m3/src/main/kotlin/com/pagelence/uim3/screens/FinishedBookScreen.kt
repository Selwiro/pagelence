package com.pagelence.uim3.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pagelence.uim3.Strings
import com.pagelence.uim3.components.BookCover
import com.pagelence.uim3.components.PageTopBar
import com.pagelence.uim3.components.rememberCoverPainter
import com.pagelence.uim3.components.wordCountText
import com.pagelence.uim3.model.FinishedBookUi

/**
 * The ending after the last unit: the cover the reader just finished, the
 * book's numbers, and the three ways out. Deliberately quiet — no confetti,
 * no certificate, no streak pressure; the next book is the reward.
 */
@Composable
fun FinishedBookScreen(
    strings: Strings,
    state: FinishedBookUi,
    onReread: () -> Unit,
    onBackToShelf: () -> Unit,
    onBrowseStore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val words = wordCountText(state.wordCount, strings.uiLanguage)
    Scaffold(
        modifier = modifier,
        topBar = { PageTopBar(title = "", onBack = onBackToShelf) },
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(20.dp))
            BookCover(
                title = state.coverTitle,
                author = state.author,
                seed = state.bookId,
                monogram = state.monogram,
                cover = rememberCoverPainter(state.coverPath),
                modifier = Modifier.width(148.dp),
            )
            Spacer(Modifier.height(30.dp))
            Text(
                strings.finishedTitle,
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                strings.finishedSubtitle,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(22.dp))
            Text(
                state.title,
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
            state.author?.takeIf { it.isNotBlank() }?.let {
                Spacer(Modifier.height(4.dp))
                Text(
                    it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
            state.finishedOn?.let {
                Spacer(Modifier.height(10.dp))
                Text(
                    strings.finishedDate.format(it),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(Modifier.height(26.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                FinishedStat(
                    value = state.unitCount.toString(),
                    label = strings.finishedUnitsLabel,
                )
                FinishedStat(
                    value = words.ifBlank { "—" },
                    label = strings.finishedWordsLabel,
                )
                FinishedStat(
                    value = state.cardCount.toString(),
                    label = strings.finishedCardsLabel,
                )
            }

            state.noteCount.takeIf { it > 0 }?.let {
                Spacer(Modifier.height(12.dp))
                Text(
                    strings.finishedNotes.format(it),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }

            val reached = state.achievements.items.filter { it.id in state.newAchievementIds }
            val others = state.achievements.items.filter {
                it.unlocked && it.id !in state.newAchievementIds
            }
            val shown = (reached + others).take(4)
            if (shown.isNotEmpty()) {
                Spacer(Modifier.height(24.dp))
                Text(
                    strings.achievementsForBook,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                shown.forEach { item ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            item.title,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            if (item.id in state.newAchievementIds) {
                                strings.achievementNew
                            } else {
                                item.unlockedOn?.let { strings.achievementUnlockedOn.format(it) }
                                    ?: strings.achievementUnlocked
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = if (item.id in state.newAchievementIds) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        )
                    }
                }
                Text(
                    strings.achievementSeeAll,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(Modifier.height(34.dp))
            Button(
                onClick = onReread,
                modifier = Modifier.fillMaxWidth().height(50.dp),
            ) {
                Text(strings.readAgain)
            }
            Spacer(Modifier.height(10.dp))
            OutlinedButton(
                onClick = onBackToShelf,
                modifier = Modifier.fillMaxWidth().height(50.dp),
            ) {
                Text(strings.backToShelf)
            }
            TextButton(onClick = onBrowseStore) {
                Text(strings.browseStore)
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun FinishedStat(value: String, label: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(value, style = MaterialTheme.typography.titleLarge)
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
