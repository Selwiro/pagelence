package com.pagelence.uim3.screens

// ReaderAnnotations：句子标注的动作条、笔记编辑框与「本书书签与笔记」列表。
// 只做展示与事件上报；身份、墓碑与合并规则在 :core。

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pagelence.core.progress.AnnotationKind
import com.pagelence.uim3.Strings
import com.pagelence.uim3.components.MetaChip
import com.pagelence.uim3.components.PagelenceIcons
import com.pagelence.uim3.model.AnnotationUi
import com.pagelence.uim3.model.SentenceMarkUi

/** The sentence a long-press opened the action sheet for. */
internal data class SentenceActionTarget(
    val paragraphId: String,
    val sentenceNumber: Int,
    val text: String,
)

internal fun annotationKindLabel(strings: Strings, kind: AnnotationKind): String = when (kind) {
    AnnotationKind.BOOKMARK -> strings.annotationBookmark
    AnnotationKind.HIGHLIGHT -> strings.annotationHighlight
    AnnotationKind.NOTE -> strings.annotationNote
}

internal fun annotationKindIcon(kind: AnnotationKind): ImageVector = when (kind) {
    AnnotationKind.BOOKMARK -> PagelenceIcons.Bookmark
    AnnotationKind.HIGHLIGHT -> PagelenceIcons.Highlight
    AnnotationKind.NOTE -> PagelenceIcons.Note
}

/**
 * Long-press action bar. Each row is a toggle: its label says what the tap will
 * do, and the check state says what is already on the sentence.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SentenceActionSheet(
    strings: Strings,
    target: SentenceActionTarget,
    mark: SentenceMarkUi?,
    onToggleBookmark: () -> Unit,
    onToggleHighlight: () -> Unit,
    onEditNote: () -> Unit,
    onDeleteNote: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() },
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 22.dp, vertical = 4.dp),
        ) {
            Text(strings.annotationTitle, style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(10.dp))
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceContainer,
            ) {
                Text(
                    target.text,
                    style = MaterialTheme.typography.bodyMedium,
                    fontStyle = FontStyle.Italic,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(12.dp),
                )
            }
            Spacer(Modifier.height(12.dp))
            AnnotationActionRow(
                icon = PagelenceIcons.Bookmark,
                title = if (mark?.bookmarked == true) {
                    strings.annotationRemoveBookmark
                } else {
                    strings.annotationAddBookmark
                },
                active = mark?.bookmarked == true,
                onClick = onToggleBookmark,
            )
            AnnotationActionRow(
                icon = PagelenceIcons.Highlight,
                title = if (mark?.highlighted == true) {
                    strings.annotationRemoveHighlight
                } else {
                    strings.annotationAddHighlight
                },
                active = mark?.highlighted == true,
                onClick = onToggleHighlight,
            )
            AnnotationActionRow(
                icon = PagelenceIcons.Note,
                title = if (mark?.hasNote == true) {
                    strings.annotationEditNote
                } else {
                    strings.annotationAddNote
                },
                active = mark?.hasNote == true,
                onClick = onEditNote,
            )
            if (mark?.hasNote == true) {
                TextButton(onClick = onDeleteNote, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        strings.annotationDeleteNote,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun AnnotationActionRow(
    icon: ImageVector,
    title: String,
    active: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        color = if (active) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
        } else {
            MaterialTheme.colorScheme.surfaceContainerLow
        },
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (active) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(12.dp))
            Text(title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            if (active) MetaChip(stringsActive)
        }
    }
}

// The check chip is only a small "on" marker; the row label already names the
// action, so it does not need a localised word.
private const val stringsActive = "✓"

/** Multi-line note editor; saving is disabled while the draft is blank. */
@Composable
internal fun NoteEditorDialog(
    strings: Strings,
    initial: String,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var text by remember(initial) { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.annotationNote) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.fillMaxWidth(),
                minLines = 4,
                maxLines = 8,
                placeholder = { Text(strings.annotationNoteHint) },
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(text.trim()) },
                enabled = text.isNotBlank(),
            ) { Text(strings.annotationSave) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(strings.annotationCancel) }
        },
    )
}

/** In-reader 「书签与笔记」 sheet: current book, grouped by chapter and unit. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AnnotationSheet(
    strings: Strings,
    bookTitle: String,
    annotations: List<AnnotationUi>,
    onOpen: (AnnotationUi) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() },
    ) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding()) {
            Column(Modifier.padding(horizontal = 22.dp)) {
                Text(strings.annotations, style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(4.dp))
                Text(
                    if (annotations.isEmpty()) {
                        strings.annotationEmptyTitle
                    } else {
                        "$bookTitle · ${strings.annotationCount.format(annotations.size)}"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(12.dp))
            if (annotations.isEmpty()) {
                Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    Text(
                        strings.annotationEmptyBody,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(start = 14.dp, end = 14.dp, bottom = 24.dp),
                ) {
                    val groups = annotations.groupBy { it.unitId }
                    groups.forEach { (unitId, rows) ->
                        item(key = "unit:$unitId") {
                            val first = rows.first()
                            Text(
                                listOfNotNull(
                                    first.chapterTitle.takeIf { it.isNotBlank() },
                                    first.unitTitle.takeIf { it.isNotBlank() },
                                ).joinToString(" · "),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(start = 8.dp, top = 14.dp, bottom = 6.dp),
                            )
                        }
                        items(rows, key = { it.id }) { annotation ->
                            AnnotationRow(strings, annotation, onClick = { onOpen(annotation) })
                        }
                    }
                }
            }
        }
    }
}

/**
 * One list row shared by the in-reader sheet and the review tab. The kind icon
 * says bookmark/highlight/note; a note shows its summary under the quote.
 */
@Composable
internal fun AnnotationRow(
    strings: Strings,
    annotation: AnnotationUi,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(
                imageVector = annotationKindIcon(annotation.kind),
                contentDescription = annotationKindLabel(strings, annotation.kind),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp).padding(top = 2.dp),
            )
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    annotation.quote.ifBlank { strings.annotationAnchorMoved },
                    style = MaterialTheme.typography.bodyMedium,
                    fontStyle = FontStyle.Italic,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                annotation.note?.takeIf { it.isNotBlank() }?.let { note ->
                    Spacer(Modifier.height(4.dp))
                    Text(
                        note,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        annotationKindLabel(strings, annotation.kind),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    annotation.dateLabel?.let { date ->
                        Text(
                            " · $date",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (!annotation.anchorValid) {
                        Spacer(Modifier.width(6.dp))
                        MetaChip(strings.annotationAnchorMoved)
                    }
                }
            }
        }
    }
}

/** Thin divider used between chapters in the review list. */
@Composable
internal fun AnnotationDivider() {
    HorizontalDivider(
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
        modifier = Modifier.padding(vertical = 6.dp),
    )
}

/** Shared label row for a book title in the cross-book review list. */
@Composable
internal fun AnnotationBookHeader(title: String, count: Int) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 6.dp, top = 10.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Text(
            count.toString(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .background(
                    MaterialTheme.colorScheme.surfaceContainer,
                    RoundedCornerShape(50),
                )
                .padding(horizontal = 8.dp, vertical = 2.dp),
        )
    }
}
