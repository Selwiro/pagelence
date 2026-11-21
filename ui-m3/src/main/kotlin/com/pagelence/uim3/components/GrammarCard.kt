package com.pagelence.uim3.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pagelence.uim3.Strings
import com.pagelence.uim3.model.GrammarExampleUi
import com.pagelence.uim3.model.GrammarUi

/**
 * Unit grammar card: point title → the book's sentence → focus → explain → form
 * → pitfalls. No folding; examples live on the library detail page only.
 * Remaining `pointIds` are shown as "这一句还用到" links at the bottom.
 */
@Composable
fun GrammarCard(
    strings: Strings,
    grammar: GrammarUi,
    modifier: Modifier = Modifier,
    onOpenPoint: ((String) -> Unit)? = null,
) {
    val title = grammar.pointName ?: grammar.topic ?: strings.grammar
    ElevatedCard(shape = MaterialTheme.shapes.large, modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    title,
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.weight(1f),
                )
                grammar.levelLabel?.let { MetaChip(it) }
            }
            Spacer(Modifier.height(18.dp))
            GrammarExampleCard(
                example = GrammarExampleUi(text = grammar.excerpt, zh = null, fromBook = true),
                strings = strings,
            )
            grammar.focus?.takeIf { it.isNotBlank() }?.let {
                LabeledText(strings.grammarFocus, it)
            }
            grammar.explain?.takeIf { it.isNotBlank() }?.let {
                LabeledText(strings.grammarExplain, it)
            }
            grammar.form?.takeIf { it.isNotBlank() }?.let {
                LabeledText(strings.grammarForm, it)
            }
            if (grammar.pitfalls.isNotEmpty()) {
                Text(
                    strings.grammarPitfalls,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 12.dp),
                )
                grammar.pitfalls.forEach { pitfall ->
                    Text(
                        "· $pitfall",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(top = 3.dp),
                    )
                }
            }
            if (grammar.extraPoints.isNotEmpty()) {
                Text(
                    strings.grammarAlsoUses,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 14.dp),
                )
                grammar.extraPoints.forEach { point ->
                    if (onOpenPoint != null) {
                        TextButton(onClick = { onOpenPoint(point.id) }) { Text(point.name) }
                    } else {
                        Text("· ${point.name}", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}

@Composable
private fun GrammarExampleCard(example: GrammarExampleUi, strings: Strings) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.secondaryContainer,
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(
                strings.grammarBookExample,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                example.text,
                style = MaterialTheme.typography.bodyLarge,
                fontStyle = FontStyle.Italic,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Composable
private fun LabeledText(label: String, text: String) {
    Column(modifier = Modifier.padding(top = 12.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(2.dp))
        Text(text, style = MaterialTheme.typography.bodyLarge)
    }
}
