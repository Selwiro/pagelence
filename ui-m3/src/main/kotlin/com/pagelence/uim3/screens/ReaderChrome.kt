package com.pagelence.uim3.screens

// ReaderChrome：从 ReaderScreen.kt 拆出；只做展示，不含业务规则。

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pagelence.uim3.Strings
import com.pagelence.uim3.components.PagelenceIcons
import com.pagelence.uim3.model.ReaderUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ReaderTopBar(
    strings: Strings,
    state: ReaderUiState,
    onBack: () -> Unit,
    onOpenCards: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenToc: () -> Unit,
    onOpenAnnotations: () -> Unit,
    annotationCount: Int,
) {
    TopAppBar(
        title = {
            Column {
                Text(state.bookTitle, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    "${state.chapterTitle} · ${state.unitPosition}/${state.unitCount}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = strings.back)
            }
        },
        actions = {
            IconButton(onClick = onOpenToc) {
                Icon(
                    Icons.AutoMirrored.Filled.List,
                    contentDescription = strings.toc,
                )
            }
            MoreMenu(
                cardsCount = state.cardsCount,
                annotationCount = annotationCount,
                strings = strings,
                onOpenCards = onOpenCards,
                onOpenSettings = onOpenSettings,
                onOpenAnnotations = onOpenAnnotations,
            )
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
    )
}

@Composable
internal fun MoreMenu(
    cardsCount: Int,
    annotationCount: Int,
    strings: Strings,
    onOpenCards: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenAnnotations: () -> Unit,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(Icons.Filled.MoreVert, contentDescription = strings.more)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text("${strings.wordCards} ($cardsCount)") },
                leadingIcon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = null) },
                onClick = {
                    expanded = false
                    onOpenCards()
                },
            )
            DropdownMenuItem(
                text = {
                    Text(
                        if (annotationCount > 0) {
                            "${strings.annotations} ($annotationCount)"
                        } else {
                            strings.annotations
                        },
                    )
                },
                leadingIcon = { Icon(PagelenceIcons.Bookmark, contentDescription = null) },
                onClick = {
                    expanded = false
                    onOpenAnnotations()
                },
            )
            DropdownMenuItem(
                text = { Text(strings.settings) },
                leadingIcon = { Icon(Icons.Filled.Settings, contentDescription = null) },
                onClick = {
                    expanded = false
                    onOpenSettings()
                },
            )
        }
    }
}

@Composable
internal fun ReaderProgressBar(
    segments: List<ProgressSegment>,
    onSegmentClick: (ProgressKind) -> Unit,
) {
    if (segments.isEmpty()) return
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 6.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            segments.forEach { segment ->
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(9.dp))
                        .background(
                            if (segment.active) {
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                            } else {
                                androidx.compose.ui.graphics.Color.Transparent
                            },
                        )
                        .clickable(
                            role = Role.Button,
                            onClickLabel = segment.label,
                            onClick = { onSegmentClick(segment.kind) },
                        )
                        .padding(horizontal = 6.dp, vertical = 7.dp),
                ) {
                    Text(
                        segment.label,
                        style = MaterialTheme.typography.labelMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                        color = if (segment.active) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                    Spacer(Modifier.height(5.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(MaterialTheme.colorScheme.outlineVariant),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(segment.value.coerceIn(0f, 1f))
                                .fillMaxHeight()
                                .background(MaterialTheme.colorScheme.primary),
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun PageIndicator(
    strings: Strings,
    page: Int,
    total: Int,
    showNext: Boolean,
    nextLabel: String,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            shape = RoundedCornerShape(50),
            color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.94f),
            shadowElevation = 1.dp,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    strings.textProgressLabel.format(page, total),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (showNext) {
                    TextButton(
                        onClick = onNext,
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                    ) {
                        Text(nextLabel, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}
