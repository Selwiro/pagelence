package com.pagelence.uieink.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import com.pagelence.uim3.Strings
import com.pagelence.uim3.model.SentenceMarkUi
import com.pagelence.uieink.components.InkButton
import com.pagelence.uieink.components.InkDivider
import com.pagelence.uieink.components.InkText
import com.pagelence.uieink.components.InkTextField
import com.pagelence.uieink.components.InkTopBar
import com.pagelence.uieink.theme.LocalInkColors
import com.pagelence.uieink.theme.LocalInkTypography

/**
 * 句级标注页：书签、划线、笔记。
 *
 * 墨屏没有长按，所以阅读页先切到「标记句子」模式，点句子进入这里；每个动作都是
 * 一次点击，写完笔记直接回落。与普通版共用同一套存储与合并规则。
 */
@Composable
fun EinkSentenceActionsPage(
    strings: Strings,
    quote: String,
    mark: SentenceMarkUi?,
    onToggleBookmark: () -> Unit,
    onToggleHighlight: () -> Unit,
    onSaveNote: (String) -> Unit,
    onDeleteNote: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalInkColors.current
    val typography = LocalInkTypography.current
    var note by remember(quote, mark?.note) { mutableStateOf(mark?.note.orEmpty()) }
    val bookmarked = mark?.bookmarked == true
    val highlighted = mark?.highlighted == true
    val hasNote = mark?.hasNote == true
    Column(modifier.fillMaxSize()) {
        InkTopBar(title = strings.markSentences, backLabel = strings.close, onBack = onClose)
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            InkText(
                quote,
                style = typography.body,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            )
            InkDivider()
            Column(Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                InkButton(
                    label = if (bookmarked) strings.annotationRemoveBookmark else strings.annotationAddBookmark,
                    onClick = onToggleBookmark,
                    primary = !bookmarked,
                )
                Spacer(Modifier.height(8.dp))
                InkButton(
                    label = if (highlighted) strings.annotationRemoveHighlight else strings.annotationAddHighlight,
                    onClick = onToggleHighlight,
                    primary = !highlighted,
                )
            }
            InkDivider()
            InkTextField(
                label = strings.annotationNote,
                value = note,
                onValueChange = { note = it },
                placeholder = strings.annotationNoteHint,
                singleLine = false,
            )
            Column(Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                InkButton(
                    label = strings.annotationSave,
                    onClick = {
                        if (note.isBlank()) onDeleteNote() else onSaveNote(note)
                        onClose()
                    },
                    primary = true,
                    enabled = note.isNotBlank() || hasNote,
                )
                if (hasNote) {
                    Spacer(Modifier.height(8.dp))
                    InkButton(strings.annotationDeleteNote, onClick = { onDeleteNote(); onClose() })
                }
                Spacer(Modifier.height(8.dp))
                InkText(
                    strings.annotationNoteHint,
                    style = typography.caption,
                    color = colors.inkMuted,
                )
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}
