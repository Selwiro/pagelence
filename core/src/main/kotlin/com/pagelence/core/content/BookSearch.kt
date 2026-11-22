package com.pagelence.core.content

/** Which side of a sentence a query matched. */
enum class SearchField {
    /** The English original. */
    TEXT,

    /** The book's existing Chinese translation. */
    TRANSLATION,
}

/** One match, already trimmed to a readable snippet for the search list. */
data class SearchHit(
    val unitId: String,
    val unitNumber: Int,
    val unitTitle: LocalizedText?,
    val chapterId: String?,
    val chapterTitle: LocalizedText?,
    val paragraphId: String,
    /** 1-based sentence number inside the unit, same index as the reader's marks. */
    val sentenceNumber: Int,
    val sentence: String,
    val translation: String?,
    val field: SearchField,
    /** Text shown in the result row: the original or its translation, with ellipses. */
    val snippet: String,
    /** Offset of the match inside [snippet]. */
    val matchStart: Int,
    val matchLength: Int,
)

data class SearchResults(
    val query: String,
    val hits: List<SearchHit>,
    /** More matches existed than [BookSearch.DEFAULT_LIMIT]; the list was cut. */
    val truncated: Boolean = false,
) {
    val isEmpty: Boolean get() = hits.isEmpty()
}

/**
 * Case-insensitive in-book search (T160). Scans the reader's own sentence
 * stream, so a match always has a real unit/paragraph/sentence to jump back to,
 * and searches the existing translations instead of machine-translating
 * anything.
 *
 * Rules:
 * - the query is trimmed; an empty query returns no hits;
 * - English text is searched first, then the book translation, so one sentence
 *   contributes at most one hit;
 * - results keep document order (chapter → unit → paragraph → sentence);
 * - scanning stops at [limit] hits and marks the result truncated.
 */
object BookSearch {
    const val DEFAULT_LIMIT = 150

    fun search(
        index: ContentIndex,
        query: String,
        limit: Int = DEFAULT_LIMIT,
    ): SearchResults {
        val needle = query.trim()
        if (needle.isEmpty() || limit <= 0) return SearchResults(query = needle, hits = emptyList())

        val hits = ArrayList<SearchHit>(minOf(limit, 64))
        var truncated = false
        var sentenceNumber = 0

        outer@ for (chapter in index.book.chapters) {
            for (unit in chapter.units) {
                sentenceNumber = 0
                for (paragraph in unit.paragraphs) {
                    for (sentence in index.sentences(paragraph)) {
                        sentenceNumber += 1
                        val textAt = sentence.text.indexOf(needle, ignoreCase = true)
                        val translation = sentence.zh?.takeIf { it.isNotBlank() }
                        val translationAt = if (textAt >= 0 || translation == null) {
                            -1
                        } else {
                            translation.indexOf(needle, ignoreCase = true)
                        }
                        if (textAt < 0 && translationAt < 0) continue
                        if (hits.size >= limit) {
                            truncated = true
                            break@outer
                        }

                        val field = if (textAt >= 0) SearchField.TEXT else SearchField.TRANSLATION
                        val source = if (field == SearchField.TEXT) sentence.text else translation.orEmpty()
                        val at = if (field == SearchField.TEXT) textAt else translationAt
                        val snippet = snippetOf(source, at, needle.length)
                        hits += SearchHit(
                            unitId = unit.id,
                            unitNumber = unit.number,
                            unitTitle = unit.title,
                            chapterId = chapter.id,
                            chapterTitle = chapter.title,
                            paragraphId = paragraph.id,
                            sentenceNumber = sentenceNumber,
                            sentence = sentence.text,
                            translation = translation,
                            field = field,
                            snippet = snippet.text,
                            matchStart = snippet.matchStart,
                            matchLength = snippet.matchLength,
                        )
                    }
                }
            }
        }
        return SearchResults(query = needle, hits = hits, truncated = truncated)
    }
}

/** A clipped excerpt plus where the query starts inside it. */
data class SearchSnippet(
    val text: String,
    val matchStart: Int,
    val matchLength: Int,
)

/**
 * Clip [text] around a match, keeping [radius] characters of context on each
 * side. Ellipses mark a cut; [SearchSnippet.matchStart] always points at the
 * query inside the returned string.
 */
fun snippetOf(text: String, matchStart: Int, matchLength: Int, radius: Int = 36): SearchSnippet {
    if (text.isBlank()) return SearchSnippet("", 0, 0)
    val start = matchStart.coerceIn(0, text.length)
    val length = matchLength.coerceIn(0, text.length - start)
    val from = (start - radius).coerceAtLeast(0)
    val to = (start + length + radius).coerceAtMost(text.length)
    val raw = text.substring(from, to)
    val leadingSpace = raw.indexOfFirst { !it.isWhitespace() }.let { if (it < 0) raw.length else it }
    val core = raw.trim()
    val prefix = if (from > 0) "…" else ""
    val suffix = if (to < text.length) "…" else ""
    val adjusted = (prefix.length + start - from - leadingSpace).coerceIn(0, core.length)
    return SearchSnippet(
        text = prefix + core + suffix,
        matchStart = adjusted,
        matchLength = length.coerceAtMost(core.length - adjusted).coerceAtLeast(0),
    )
}
