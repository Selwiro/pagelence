package com.pagelence.presentation.reader

import com.pagelence.core.content.ContentIndex
import com.pagelence.core.content.ContentRepository
import com.pagelence.core.content.StudyUnit
import com.pagelence.core.wordcards.WordCard
import com.pagelence.core.wordcards.WordCardRepository
import com.pagelence.uim3.model.PreviewWordUi

/**
 * The pre-reading vocabulary list: words in the unit above the reader's level
 * that are not yet collected. Android-free; the shell shows the list and adds
 * cards from it.
 */
class PreviewPresenter(
    private val content: ContentRepository,
    private val cards: WordCardRepository,
) {
    fun words(
        bookIndex: ContentIndex,
        unit: StudyUnit,
        bookLevel: String?,
    ): List<PreviewWordUi> {
        val threshold = (levelRank(bookLevel) ?: return emptyList()) + 1
        val words = mutableListOf<PreviewWordUi>()
        val seen = mutableSetOf<String>()
        var sentenceNumber = 0
        unit.paragraphs.forEach { paragraph ->
            bookIndex.sentences(paragraph).forEach { sentence ->
                sentenceNumber += 1
                PREVIEW_WORD.findAll(sentence.text).forEach { match ->
                    val term = match.value
                    if (term.length < 2) return@forEach
                    val lookup = content.lookup(
                        bookId = bookIndex.book.id,
                        unitId = unit.id,
                        paragraphId = paragraph.id,
                        sentenceIndex = sentenceNumber,
                        spelling = term,
                    ) ?: return@forEach
                    val rank = levelRank(lookup.level) ?: return@forEach
                    if (rank < threshold) return@forEach
                    val key = WordCard.lemmaKey(lookup.lemma)
                    if (!seen.add(key)) return@forEach
                    val zh = lookup.definitions
                        .firstOrNull { it.language == "zh" && it.definition.isNotBlank() }
                        ?.definition
                        ?: lookup.glossZh
                    val en = lookup.definitions
                        .firstOrNull { it.language == "en" && it.definition.isNotBlank() }
                        ?.definition
                    if (zh.isNullOrBlank() && en.isNullOrBlank()) return@forEach
                    words += PreviewWordUi(
                        term = term,
                        lemma = lookup.lemma,
                        ipa = lookup.ipa,
                        pos = lookup.pos,
                        level = lookup.level.orEmpty(),
                        zh = zh,
                        en = en,
                        exampleEn = sentence.text,
                        exampleZh = sentence.zh,
                        inWordCards = cards.find(bookIndex.book.id, lookup.lemma, term) != null,
                    )
                }
            }
        }
        return words
    }

    private fun levelRank(level: String?): Int? = when (level) {
        "A1" -> 0
        "A2" -> 1
        "B1" -> 2
        "B2" -> 3
        "C1" -> 4
        "C2" -> 5
        else -> null
    }

    private companion object {
        private val PREVIEW_WORD = Regex("[A-Za-z]+(?:['’][A-Za-z]+)?")
    }
}
