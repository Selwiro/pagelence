package com.pagelence.presentation.grammar

import com.pagelence.core.content.GrammarExample
import com.pagelence.core.content.GrammarPoint
import com.pagelence.uim3.UiLanguage
import com.pagelence.uim3.model.GrammarExampleUi
import com.pagelence.uim3.model.GrammarPointUi
import com.pagelence.uim3.model.GrammarRelatedUi
import com.pagelence.uim3.model.GrammarTabUiState

/**
 * Grammar library list and detail mapping. Android-free: the phone and the
 * e-ink shell show the same CEFR-J entries.
 */
class GrammarPresenter(
    private val all: List<GrammarPoint>,
    private val language: UiLanguage,
) {
    fun point(point: GrammarPoint): GrammarPointUi {
        val index = all.indexOfFirst { it.id == point.id }
        fun nameOf(id: String): String? = all.firstOrNull { it.id == id }
            ?.name?.let { textFor(it) }
        return GrammarPointUi(
            id = point.id,
            name = point.name.let { textFor(it) }.orEmpty(),
            levelStart = point.levelStart,
            levelEnd = point.levelEnd,
            levelLabel = point.levelLabel,
            explain = point.explain.let { textFor(it) }.orEmpty(),
            form = point.form.let { textFor(it) }.orEmpty(),
            pitfalls = point.pitfalls.mapNotNull { textFor(it) },
            examples = point.examples.map {
                GrammarExampleUi(
                    text = it.text,
                    zh = if (language == UiLanguage.EN) null else it.zh,
                    fromBook = false,
                )
            },
            related = point.relatedIds.mapNotNull { id ->
                nameOf(id)?.let { GrammarRelatedUi(id, it) }
            },
            previousId = all.getOrNull(index - 1)?.id,
            nextId = all.getOrNull(index + 1)?.id,
        )
    }

    /** The 复习·语法 list: every library point in level order. */
    fun tab(): GrammarTabUiState = GrammarTabUiState(
        points = all.map(::point),
        loading = false,
    )

    private fun textFor(text: com.pagelence.core.content.LocalizedText): String? = when (language) {
        UiLanguage.ZH -> text.zh ?: text.en
        UiLanguage.EN -> text.en ?: text.zh
    }?.takeIf { it.isNotBlank() }
}
