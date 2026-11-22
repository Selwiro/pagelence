package com.pagelence.uim3.screens

// AnnotationsScreen：复习栏的「笔记」子页，跨书汇总书签、划线与笔记。

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pagelence.core.progress.AnnotationKind
import com.pagelence.uim3.Strings
import com.pagelence.uim3.components.EmptyState
import com.pagelence.uim3.model.AnnotationFilter
import com.pagelence.uim3.model.AnnotationUi

/** Review tab → 笔记: all books, filterable by kind, rows jump to the sentence. */
@Composable
fun AnnotationsContent(
    strings: Strings,
    annotations: List<AnnotationUi>,
    filter: AnnotationFilter,
    onSelectFilter: (AnnotationFilter) -> Unit,
    onOpen: (AnnotationUi) -> Unit,
    modifier: Modifier = Modifier,
) {
    val filtered = annotations.filter { annotation ->
        when (filter) {
            AnnotationFilter.ALL -> true
            AnnotationFilter.BOOKMARK -> annotation.kind == AnnotationKind.BOOKMARK
            AnnotationFilter.HIGHLIGHT -> annotation.kind == AnnotationKind.HIGHLIGHT
            AnnotationFilter.NOTE -> annotation.kind == AnnotationKind.NOTE
        }
    }
    Column(modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            AnnotationFilter.entries.forEach { entry ->
                FilterChip(
                    selected = entry == filter,
                    onClick = { onSelectFilter(entry) },
                    label = {
                        Text(
                            when (entry) {
                                AnnotationFilter.ALL -> strings.annotationFilterAll
                                AnnotationFilter.BOOKMARK -> strings.annotationBookmark
                                AnnotationFilter.HIGHLIGHT -> strings.annotationHighlight
                                AnnotationFilter.NOTE -> strings.annotationNote
                            },
                            style = MaterialTheme.typography.labelLarge,
                        )
                    },
                )
            }
        }
        if (filtered.isEmpty()) {
            EmptyState(
                icon = Icons.AutoMirrored.Filled.List,
                title = if (annotations.isEmpty()) {
                    strings.annotationEmptyTitle
                } else {
                    strings.annotationEmptyFilter
                },
                body = strings.annotationEmptyBody,
                modifier = Modifier.fillMaxSize(),
            )
            return@Column
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 14.dp, end = 14.dp, bottom = 24.dp),
        ) {
            val books = filtered.groupBy { it.bookId }
            books.forEach { (bookId, rows) ->
                val first = rows.first()
                item(key = "book:$bookId") {
                    AnnotationBookHeader(first.bookTitle, rows.size)
                }
                val units = rows.groupBy { it.unitId }
                units.forEach { (unitId, unitRows) ->
                    item(key = "book:$bookId:unit:$unitId") {
                        Text(
                            listOfNotNull(
                                unitRows.first().chapterTitle.takeIf { it.isNotBlank() },
                                unitRows.first().unitTitle.takeIf { it.isNotBlank() },
                            ).joinToString(" · "),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 6.dp, top = 8.dp, bottom = 4.dp),
                        )
                    }
                    items(unitRows, key = { it.id }) { annotation ->
                        AnnotationRow(strings, annotation, onClick = { onOpen(annotation) })
                    }
                }
                item(key = "book:$bookId:divider") {
                    Spacer(Modifier.height(4.dp))
                    AnnotationDivider()
                }
            }
        }
    }
}
