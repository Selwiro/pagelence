package com.pagelence.presentation.search

import com.pagelence.core.content.BookSearch
import com.pagelence.core.content.ContentIndex
import com.pagelence.core.content.LocalizedText
import com.pagelence.core.content.SearchField
import com.pagelence.uim3.UiLanguage
import com.pagelence.uim3.model.SearchHitUi
import com.pagelence.uim3.model.SearchUiState

/**
 * Maps [BookSearch] results to the search screen (T160). Search rules live in
 * `:core`; this only resolves localized chapter/unit labels and the row model.
 */
class SearchPresenter(private val language: UiLanguage) {
    fun initial(index: ContentIndex, query: String = ""): SearchUiState = SearchUiState(
        bookId = index.book.id,
        bookTitle = title(index.book.title),
        query = query,
        hasSearched = false,
    )

    fun search(index: ContentIndex, query: String): SearchUiState {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return initial(index)
        val results = BookSearch.search(index, trimmed)
        val hits = results.hits.map { hit ->
            val chapterNumber = index.book.chapters
                .firstOrNull { it.id == hit.chapterId }
                ?.number
            val unit = index.unit(hit.unitId)
            SearchHitUi(
                bookId = index.book.id,
                unitId = hit.unitId,
                paragraphId = hit.paragraphId,
                sentenceNumber = hit.sentenceNumber,
                chapterLabel = hit.chapterTitle?.let(::title)
                    ?: chapterNumber?.let { "#$it" }
                    ?: "",
                unitLabel = hit.unitTitle?.let(::title)
                    ?: unit?.title?.let(::title)
                    ?: unit?.number?.let { "#$it" }
                    ?: hit.unitId,
                snippet = hit.snippet,
                matchStart = hit.matchStart,
                matchLength = hit.matchLength,
                sentence = hit.sentence,
                translation = hit.translation,
                matchedTranslation = hit.field == SearchField.TRANSLATION,
            )
        }
        return SearchUiState(
            bookId = index.book.id,
            bookTitle = title(index.book.title),
            query = trimmed,
            hits = hits,
            truncated = results.truncated,
            hasSearched = true,
        )
    }

    private fun title(text: LocalizedText): String = when (language) {
        UiLanguage.ZH -> text.zh ?: text.en ?: ""
        UiLanguage.EN -> text.en ?: text.zh ?: ""
    }.ifBlank { "" }
}
