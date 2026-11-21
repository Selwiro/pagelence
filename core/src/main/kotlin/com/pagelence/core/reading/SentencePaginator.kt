package com.pagelence.core.reading

/** One sentence measured in the current width, font size and reading style. */
data class MeasuredSentence(
    val paragraphId: String,
    /** 0-based index inside the paragraph. */
    val sentenceIndex: Int,
    val heightPx: Int,
    /** Paragraph gap that precedes the first sentence of a paragraph. */
    val leadingHeightPx: Int = 0,
)

/** A run of consecutive sentences from one paragraph on a page. */
data class TextPageChunk(
    val paragraphId: String,
    val sentenceStart: Int,
    val sentenceEndExclusive: Int,
)

/**
 * One screen of paged text. Paragraphs may span pages, but sentences are never
 * cut: the page boundaries always fall between sentences.
 */
data class SentencePage(
    val chunks: List<TextPageChunk>,
    val heightPx: Int,
    val pageHeightPx: Int,
    val scrollable: Boolean,
) {
    val anchorParagraphId: String get() = chunks.first().paragraphId
}

/**
 * Novel-style pagination for the paged reading layout.
 *
 * 1. Fill one screen with whole sentences; a paragraph may continue on the
 *    next page.
 * 2. A sentence is never split across pages.
 * 3. A sentence taller than the whole viewport gets its own scrollable page.
 */
object SentencePaginator {
    fun paginate(
        sentences: List<MeasuredSentence>,
        pageHeightPx: Int,
    ): List<SentencePage> {
        if (sentences.isEmpty()) return emptyList()
        require(pageHeightPx > 0) { "page height must be positive" }

        val pages = mutableListOf<SentencePage>()
        val chunks = mutableListOf<TextPageChunk>()
        var pageHeight = 0
        var pageScrollable = false

        fun flush() {
            if (chunks.isEmpty()) return
            pages += SentencePage(
                chunks = chunks.toList(),
                heightPx = pageHeight,
                pageHeightPx = pageHeightPx,
                scrollable = pageScrollable,
            )
            chunks.clear()
            pageHeight = 0
            pageScrollable = false
        }

        sentences.forEach { sentence ->
            val cost = sentence.leadingHeightPx + sentence.heightPx
            if (chunks.isNotEmpty() && pageHeight + cost > pageHeightPx) flush()
            val last = chunks.lastOrNull()
            if (last != null &&
                last.paragraphId == sentence.paragraphId &&
                last.sentenceEndExclusive == sentence.sentenceIndex
            ) {
                chunks[chunks.lastIndex] = last.copy(
                    sentenceEndExclusive = sentence.sentenceIndex + 1,
                )
            } else {
                chunks += TextPageChunk(
                    paragraphId = sentence.paragraphId,
                    sentenceStart = sentence.sentenceIndex,
                    sentenceEndExclusive = sentence.sentenceIndex + 1,
                )
            }
            pageHeight += cost
            if (cost > pageHeightPx) pageScrollable = true
        }
        flush()
        return pages
    }
}
