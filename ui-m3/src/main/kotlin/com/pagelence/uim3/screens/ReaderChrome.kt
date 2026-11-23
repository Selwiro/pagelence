package com.pagelence.uim3.screens

// ReaderChrome：从 ReaderScreen.kt 拆出；只做展示，不含业务规则。

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pagelence.uim3.Strings
import com.pagelence.uim3.components.TranslateIcon
import com.pagelence.uim3.model.ReaderUiState

@Composable
internal fun ReaderTopBar(
    strings: Strings,
    state: ReaderUiState,
    onBack: () -> Unit,
    onOpenSearch: () -> Unit,
    onToggleTranslations: () -> Unit,
) {
    // The bar is a plain row: the old single-line TopAppBar clipped English
    // book names and wrapped the four tabs at a 2.0 font scale. The title may
    // use two lines; the bar grows to fit. Bar text is capped at its 1× size so
    // a 2.0 system scale enlarges the page, not the chrome.
    val fontScale = LocalDensity.current.fontScale
    val barScale = if (fontScale > 1f) 1f / fontScale else 1f
    val titleStyle = MaterialTheme.typography.titleLarge.let {
        if (barScale == 1f) it else it.copy(fontSize = (it.fontSize.value * barScale).sp)
    }
    val subtitleStyle = MaterialTheme.typography.bodyMedium.let {
        if (barScale == 1f) it else it.copy(fontSize = (it.fontSize.value * barScale).sp)
    }
    Surface(color = MaterialTheme.colorScheme.surface) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars)
                .heightIn(min = 64.dp)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = strings.back)
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp, vertical = 6.dp),
            ) {
                Text(
                    state.bookTitle,
                    style = titleStyle,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    "${state.chapterTitle} · ${state.unitPosition}/${state.unitCount}",
                    style = subtitleStyle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            // One tap turns every book translation on or off (T159).
            IconButton(onClick = onToggleTranslations) {
                Icon(
                    TranslateIcon,
                    contentDescription = if (state.preference.showAllTranslations) {
                        strings.hideAllTranslations
                    } else {
                        strings.showAllTranslations
                    },
                    tint = if (state.preference.showAllTranslations) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
            // Contents, bookmarks and settings moved to the bottom dock (T167);
            // the top-right slot is now search.
            IconButton(onClick = onOpenSearch) {
                Icon(Icons.Filled.Search, contentDescription = strings.inBookSearch)
            }
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
                        .padding(horizontal = 4.dp, vertical = 7.dp),
                ) {
                    Text(
                        segment.label,
                        style = MaterialTheme.typography.labelMedium,
                        autoSize = TextAutoSize.StepBased(
                            minFontSize = 7.sp,
                            maxFontSize = MaterialTheme.typography.labelMedium.fontSize,
                        ),
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
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
