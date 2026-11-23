package com.pagelence.uim3.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Create
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pagelence.uim3.Strings
import com.pagelence.uim3.components.EmptyState
import com.pagelence.uim3.components.MetaChip
import com.pagelence.uim3.model.GrammarPointUi
import com.pagelence.uim3.model.GrammarTabUiState

/**
 * 复习·语法：CEFR-J 条目列表，按 `levelStart` 分段。列表行只放名称与级别区间，
 * 点任意一行进入详情；不写规则、不放例句、不展开。
 */
@Composable
fun GrammarTabContent(
    strings: Strings,
    state: GrammarTabUiState,
    onOpenPoint: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (state.points.isEmpty()) {
        EmptyState(
            icon = Icons.Filled.Create,
            title = strings.grammarEmpty,
            body = strings.libraryHint,
            modifier = modifier.fillMaxSize(),
        )
        return
    }
    val groups = state.points.groupBy { it.levelStart }.toSortedMap()
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item(key = "title") {
            Column {
                Text(strings.grammarAll, style = MaterialTheme.typography.titleLarge)
                Text(
                    strings.grammarPointsText(state.points.size),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        groups.forEach { (level, points) ->
            item(key = "level:$level") {
                Text(
                    level,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            items(points, key = { it.id }) { point ->
                GrammarPointRow(strings, point, onOpenPoint)
            }
        }
    }
}

@Composable
private fun GrammarPointRow(strings: Strings, point: GrammarPointUi, onOpen: (String) -> Unit) {
    ElevatedCard(
        onClick = { onOpen(point.id) },
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                point.name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.size(8.dp))
            MetaChip(point.levelLabel)
            Spacer(Modifier.size(6.dp))
            Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}
