package com.pagelence.core.reading

/** Screen width bucket from the Inkdeck study; used for the default max page height. */
enum class ScreenBucket(
    val minWidthDp: Int,
    val maxPageScale: Float,
) {
    COMPACT(0, 1.5f),
    MEDIUM(600, 1.5f),
    LARGE(840, 2.0f),
    XLARGE(1200, 2.0f),
    ;

    companion object {
        fun of(widthDp: Int): ScreenBucket = entries.last { widthDp >= it.minWidthDp }
    }
}

/** One paragraph measured with the current width, font size and reading style. */
data class MeasuredParagraph(
    val id: String,
    val heightPx: Int,
)

/**
 * A text page: whole paragraphs only, in reading order. [scrollable] means the
 * page is taller than the 1.0× viewport and the reader must scroll inside it.
 */
data class TextPage(
    val paragraphIds: List<String>,
    val heightPx: Int,
    val pageHeightPx: Int,
    val maxPageHeightPx: Int,
    val scrollable: Boolean,
) {
    val anchorParagraphId: String get() = paragraphIds.first()
}

/**
 * Whole-paragraph pagination for T081:
 *
 * 1. a paragraph is never split;
 * 2. keep adding whole paragraphs while the page stays within the max height;
 * 3. a page taller than the viewport is scrollable (at most one extra screen
 *    for multi-paragraph pages);
 * 4. a single paragraph taller than the max gets its own page and may exceed it.
 *
 * The UI measures paragraph heights with `TextMeasurer`; this object only owns
 * the grouping rule, so the boundary properties are testable without Compose.
 */
object ParagraphPaginator {
    fun maxPageHeight(pageHeightPx: Int, bucket: ScreenBucket): Int =
        (pageHeightPx * bucket.maxPageScale).toInt()

    fun paginate(
        paragraphs: List<MeasuredParagraph>,
        pageHeightPx: Int,
        maxPageHeightPx: Int,
    ): List<TextPage> {
        if (paragraphs.isEmpty()) return emptyList()
        require(pageHeightPx > 0) { "page height must be positive" }
        require(maxPageHeightPx >= pageHeightPx) { "max page height must cover the viewport" }

        val pages = mutableListOf<TextPage>()
        var index = 0
        while (index < paragraphs.size) {
            val ids = mutableListOf<String>()
            var height = 0
            while (index < paragraphs.size) {
                val paragraph = paragraphs[index]
                val fits = ids.isEmpty() || height + paragraph.heightPx <= maxPageHeightPx
                if (!fits) break
                ids += paragraph.id
                height += paragraph.heightPx
                index += 1
            }
            if (ids.isEmpty()) break
            pages += TextPage(
                paragraphIds = ids,
                heightPx = height,
                pageHeightPx = pageHeightPx,
                maxPageHeightPx = maxPageHeightPx,
                scrollable = height > pageHeightPx,
            )
        }
        return pages
    }
}
