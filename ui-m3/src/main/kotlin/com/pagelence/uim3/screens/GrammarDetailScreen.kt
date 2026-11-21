package com.pagelence.uim3.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pagelence.uim3.Strings
import com.pagelence.uim3.components.GrammarCard
import com.pagelence.uim3.components.PageTopBar
import com.pagelence.uim3.model.GrammarDetailUi

/** 语法详情页：复用 T079 的点→例→析，并提供回到本单元。 */
@Composable
fun GrammarDetailScreen(
    strings: Strings,
    state: GrammarDetailUi,
    onBack: () -> Unit,
    onOpenUnit: () -> Unit,
    onOpenPoint: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            PageTopBar(
                title = state.grammar.pointName ?: state.unitTitle,
                subtitle = listOf(state.bookTitle, state.chapterTitle, state.unitTitle)
                    .filter { it.isNotBlank() }
                    .joinToString(" · "),
                onBack = onBack,
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 10.dp),
        ) {
            GrammarCard(strings = strings, grammar = state.grammar, onOpenPoint = onOpenPoint)
            Spacer(Modifier.height(14.dp))
            Button(onClick = onOpenUnit, modifier = Modifier.fillMaxWidth()) {
                Text(strings.openUnit)
            }
            Spacer(Modifier.height(20.dp))
            Text(
                state.unitTitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
