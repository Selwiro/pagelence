package com.pagelence.uim3.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pagelence.uim3.Strings
import com.pagelence.uim3.components.MetaChip
import com.pagelence.uim3.components.PageTopBar
import com.pagelence.uim3.model.GrammarPointUi

/**
 * 语法详情：一条 CEFR-J 条目，所有字段直接铺开，没有折叠。
 * 顺序：名称/区间 → explain → form → pitfalls → examples → 上一条/下一条 → relatedIds。
 */
@Composable
fun GrammarPointScreen(
    strings: Strings,
    point: GrammarPointUi,
    onBack: () -> Unit,
    onOpenPoint: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = { PageTopBar(title = point.name, onBack = onBack) },
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 10.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                MetaChip(point.levelLabel)
                MetaChip(strings.grammarAll)
            }
            Spacer(Modifier.height(12.dp))
            Text(point.explain, style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(16.dp))
            Text(strings.grammarForm, style = MaterialTheme.typography.labelLarge)
            Text(point.form, style = MaterialTheme.typography.bodyLarge)
            if (point.pitfalls.isNotEmpty()) {
                Spacer(Modifier.height(14.dp))
                Text(strings.grammarPitfalls, style = MaterialTheme.typography.labelLarge)
                point.pitfalls.forEach { Text("• $it", style = MaterialTheme.typography.bodyMedium) }
            }
            if (point.examples.isNotEmpty()) {
                Spacer(Modifier.height(14.dp))
                Text(strings.grammarExamples, style = MaterialTheme.typography.labelLarge)
                point.examples.forEach { example ->
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Text(example.text, style = MaterialTheme.typography.bodyLarge)
                            example.zh?.let {
                                Text(
                                    it,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
            if (point.related.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                Text(strings.grammarRelated, style = MaterialTheme.typography.labelLarge)
                point.related.forEach { related ->
                    TextButton(onClick = { onOpenPoint(related.id) }) {
                        Text(related.name)
                    }
                }
            }
            Spacer(Modifier.height(18.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                point.previousId?.let { id ->
                    OutlinedButton(onClick = { onOpenPoint(id) }, modifier = Modifier.weight(1f)) {
                        Text(strings.grammarPrevious)
                    }
                }
                point.nextId?.let { id ->
                    OutlinedButton(onClick = { onOpenPoint(id) }, modifier = Modifier.weight(1f)) {
                        Text(strings.grammarNext)
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
