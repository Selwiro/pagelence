package com.pagelence.uim3.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pagelence.core.prefs.FontSize
import com.pagelence.core.prefs.LineSpacing
import com.pagelence.core.prefs.PageMargin
import com.pagelence.core.prefs.ReadingFont
import com.pagelence.uim3.Strings
import com.pagelence.uim3.components.PageTopBar
import com.pagelence.uim3.components.SegmentedRow
import com.pagelence.uim3.model.SettingsUiState
import com.pagelence.uim3.theme.readingTextStyle

/**
 * 排版 (T168): the one place that edits the reading surface. A live sample sits
 * on top so size, face, spacing and margin are judged together, then four
 * equal-width segments change them in place. The settings home only links here.
 */
@Composable
fun TypographyScreen(
    strings: Strings,
    state: SettingsUiState,
    onBack: () -> Unit,
    onFontSize: (FontSize) -> Unit,
    onReadingFont: (ReadingFont) -> Unit,
    onLineSpacing: (LineSpacing) -> Unit,
    onPageMargin: (PageMargin) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = { PageTopBar(title = strings.typography, onBack = onBack) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            Spacer(Modifier.height(4.dp))
            PreviewCard(strings, state)
            Spacer(Modifier.height(16.dp))
            SegmentedRow(
                label = strings.fontSize,
                options = listOf(
                    FontSize.SMALL to strings.fontSmall,
                    FontSize.MEDIUM to strings.fontMedium,
                    FontSize.LARGE to strings.fontLarge,
                    FontSize.XLARGE to strings.fontXLarge,
                ),
                selected = state.fontSize,
                onSelect = onFontSize,
            )
            SegmentedRow(
                label = strings.readingFont,
                options = listOf(
                    ReadingFont.SANS to strings.fontSans,
                    ReadingFont.SERIF to strings.fontSerif,
                ),
                selected = state.readingFont,
                onSelect = onReadingFont,
            )
            SegmentedRow(
                label = strings.lineSpacing,
                options = listOf(
                    LineSpacing.COMPACT to strings.lineSpacingCompact,
                    LineSpacing.NORMAL to strings.lineSpacingNormal,
                    LineSpacing.RELAXED to strings.lineSpacingRelaxed,
                ),
                selected = state.lineSpacing,
                onSelect = onLineSpacing,
            )
            SegmentedRow(
                label = strings.pageMargin,
                options = listOf(
                    PageMargin.NARROW to strings.pageMarginNarrow,
                    PageMargin.NORMAL to strings.pageMarginNormal,
                    PageMargin.WIDE to strings.pageMarginWide,
                ),
                selected = state.pageMargin,
                onSelect = onPageMargin,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                strings.typographyHint,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(28.dp))
        }
    }
}

@Composable
private fun PreviewCard(strings: Strings, state: SettingsUiState) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 16.dp)) {
            Text(
                strings.typographyPreview,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 6.dp, bottom = 10.dp),
            )
            val margin = state.pageMargin.horizontalPadding()
            Column(Modifier.padding(horizontal = (margin - 12.dp).coerceAtLeast(0.dp))) {
                Text(
                    strings.typographySample,
                    style = readingTextStyle(state.fontSize, state.readingFont, state.lineSpacing),
                )
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant,
                    modifier = Modifier.padding(vertical = 12.dp),
                )
                Text(
                    strings.typographySampleZh,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
