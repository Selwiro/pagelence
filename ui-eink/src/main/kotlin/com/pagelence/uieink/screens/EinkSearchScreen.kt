package com.pagelence.uieink.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pagelence.uim3.Strings
import com.pagelence.uim3.model.SearchHitUi
import com.pagelence.uim3.model.SearchUiState
import com.pagelence.uieink.components.InkDivider
import com.pagelence.uieink.components.InkListRow
import com.pagelence.uieink.components.InkText
import com.pagelence.uieink.components.InkTextField
import com.pagelence.uieink.components.InkTextButton
import com.pagelence.uieink.components.InkTopBar
import com.pagelence.uieink.theme.LocalInkColors
import com.pagelence.uieink.theme.LocalInkTypography

/**
 * 书内搜索：检索原句与已有句译，点结果跳回该句所在页。
 *
 * 墨屏版没有实时联想，输入后由 ViewModel 按现有规则检索（上限与普通版一致）；
 * 结果行给出章 · 单元与命中片段，点一行直接定位。
 */
@Composable
fun EinkSearchScreen(
    strings: Strings,
    search: SearchUiState,
    onQuery: (String) -> Unit,
    onOpenHit: (SearchHitUi) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalInkColors.current
    val typography = LocalInkTypography.current
    Column(modifier.fillMaxSize()) {
        InkTopBar(
            title = strings.inBookSearch,
            subtitle = search.bookTitle,
            backLabel = strings.back,
            onBack = onBack,
        )
        InkTextField(
            label = strings.inBookSearch,
            value = search.query,
            onValueChange = onQuery,
            placeholder = strings.searchHint,
        )
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            InkText(
                when {
                    !search.hasSearched -> strings.searchPrompt
                    search.hits.isEmpty() -> strings.searchNoResults.format(search.query)
                    search.truncated -> strings.searchTruncated.format(search.hits.size)
                    else -> strings.searchResultsCount.format(search.hits.size)
                },
                style = typography.caption,
                color = colors.inkMuted,
                modifier = Modifier.weight(1f),
            )
            if (search.query.isNotEmpty()) {
                InkTextButton(strings.searchClear, onClick = { onQuery("") })
            }
        }
        InkDivider()
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            search.hits.forEach { hit ->
                InkListRow(
                    title = hit.snippet,
                    subtitle = "${hit.chapterLabel} · ${hit.unitLabel}" +
                        if (hit.matchedTranslation) " · ${strings.translation}" else "",
                    onClick = { onOpenHit(hit) },
                )
                InkDivider()
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}
