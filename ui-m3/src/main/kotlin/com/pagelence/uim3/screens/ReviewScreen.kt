package com.pagelence.uim3.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.pagelence.uim3.Strings
import com.pagelence.uim3.components.PageTopBar
import com.pagelence.uim3.model.AnnotationFilter
import com.pagelence.uim3.model.AnnotationUi
import com.pagelence.uim3.model.GrammarTabUiState
import com.pagelence.uim3.model.ReviewTab
import com.pagelence.uim3.model.WordCardUi

/**
 * 复习栏：两个子页。单词是收集的生词卡，语法是本书语法点与语法库。
 * 两页原来各是一个独立页面（生词卡 / 语法），现在收进同一个 tab。
 */
@Composable
fun ReviewScreen(
    strings: Strings,
    tab: ReviewTab,
    cards: List<WordCardUi>,
    grammar: GrammarTabUiState,
    annotations: List<AnnotationUi>,
    annotationFilter: AnnotationFilter,
    onSelectTab: (ReviewTab) -> Unit,
    onSelectAnnotationFilter: (AnnotationFilter) -> Unit,
    onOpenAnnotation: (AnnotationUi) -> Unit,
    onOpenPoint: (String) -> Unit,
    onDeleteCard: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = { PageTopBar(title = strings.tabReview) },
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            TabRow(
                selectedTabIndex = tab.ordinal,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary,
            ) {
                ReviewTab.entries.forEach { entry ->
                    val selected = entry == tab
                    Tab(
                        selected = selected,
                        onClick = { onSelectTab(entry) },
                        text = {
                            Text(
                                when (entry) {
                                    ReviewTab.WORDS -> strings.reviewWords
                                    ReviewTab.GRAMMAR -> strings.reviewGrammar
                                    ReviewTab.NOTES -> strings.reviewNotes
                                },
                                style = MaterialTheme.typography.labelLarge,
                            )
                        },
                    )
                }
            }
            when (tab) {
                ReviewTab.WORDS -> WordCardsContent(
                    strings = strings,
                    cards = cards,
                    onDelete = onDeleteCard,
                )

                ReviewTab.GRAMMAR -> GrammarTabContent(
                    strings = strings,
                    state = grammar,
                    onOpenPoint = onOpenPoint,
                )

                ReviewTab.NOTES -> AnnotationsContent(
                    strings = strings,
                    annotations = annotations,
                    filter = annotationFilter,
                    onSelectFilter = onSelectAnnotationFilter,
                    onOpen = onOpenAnnotation,
                )
            }
        }
    }
}
