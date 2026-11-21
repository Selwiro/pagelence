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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pagelence.uim3.Strings
import com.pagelence.uim3.components.EmptyState
import com.pagelence.uim3.components.MetaChip
import com.pagelence.uim3.model.WordCardUi

/** 复习·单词页：已收词条列表，删除写墓碑。 */
@Composable
fun WordCardsContent(
    strings: Strings,
    cards: List<WordCardUi>,
    onDelete: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (cards.isEmpty()) {
        EmptyState(
            icon = Icons.AutoMirrored.Filled.List,
            title = strings.noCards,
            body = strings.noCardsHint,
            modifier = modifier.fillMaxSize(),
        )
        return
    }
    var studying by remember { mutableStateOf(false) }
    var index by remember { mutableIntStateOf(0) }
    var revealed by remember { mutableStateOf(false) }

    LaunchedEffect(cards.size) {
        index = index.coerceIn(0, cards.lastIndex.coerceAtLeast(0))
        if (cards.isEmpty()) studying = false
    }

    if (studying) {
        WordStudySession(
            strings = strings,
            cards = cards,
            index = index.coerceIn(0, cards.lastIndex),
            revealed = revealed,
            onReveal = { revealed = true },
            onPrevious = {
                if (index > 0) {
                    index -= 1
                    revealed = false
                }
            },
            onNext = {
                if (index >= cards.lastIndex) {
                    studying = false
                    revealed = false
                } else {
                    index += 1
                    revealed = false
                }
            },
            onExit = {
                studying = false
                revealed = false
            },
            modifier = modifier,
        )
        return
    }

    Column(modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 18.dp, end = 14.dp, top = 12.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                strings.studyCards.format(cards.size),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            Button(
                onClick = {
                    index = 0
                    revealed = false
                    studying = true
                },
            ) {
                Text(strings.studyStart)
            }
        }
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(cards, key = { it.id }) { card -> WordCardRow(strings, card, onDelete) }
        }
    }
}

@Composable
private fun WordStudySession(
    strings: Strings,
    cards: List<WordCardUi>,
    index: Int,
    revealed: Boolean,
    onReveal: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val card = cards[index]
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
            TextButton(onClick = onExit) { Text(strings.studyExit) }
        }
        Text(
            strings.studyProgress.format(index + 1, cards.size),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(14.dp))
        ElevatedCard(
            shape = MaterialTheme.shapes.large,
            modifier = Modifier.fillMaxWidth().weight(1f),
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
                if (!revealed) {
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
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(
                onClick = onPrevious,
                enabled = index > 0,
                modifier = Modifier.weight(1f),
            ) {
                Text(strings.studyPrevious)
            }
            Button(onClick = onNext, modifier = Modifier.weight(1f)) {
                Text(if (index >= cards.lastIndex) strings.studyDone else strings.studyNext)
            }
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        MetaChip(card.bookTitle)
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
