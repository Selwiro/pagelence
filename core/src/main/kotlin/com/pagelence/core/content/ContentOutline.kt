package com.pagelence.core.content

import kotlinx.serialization.Serializable

/** One unit in a book outline: identity, title and the paragraphs it owns. */
@Serializable
data class OutlineUnit(
    val id: String,
    val chapterId: String? = null,
    val number: Int = 0,
    val title: LocalizedText? = null,
    val paragraphIds: List<String> = emptyList(),
)

/**
 * The small per-book index read on startup (`outline.json`).
 *
 * The shelf and the store need the book's identity, how many units it has and
 * where the reader stopped — not the 1 MB `book.json`. The outline is generated
 * from `book.json` by the content scripts and validated against it, so building
 * the library never parses a book body.
 */
@Serializable
data class ContentOutline(
    val schemaVersion: Int = OUTLINE_SCHEMA_VERSION,
    val bookId: String,
    val title: LocalizedText? = null,
    val author: String? = null,
    val edition: String? = null,
    val description: LocalizedText? = null,
    val units: List<OutlineUnit> = emptyList(),
) {
    val unitIds: Set<String> by lazy { units.mapTo(mutableSetOf()) { it.id } }

    fun unit(unitId: String): OutlineUnit? = units.firstOrNull { it.id == unitId }

    fun unitOfParagraph(paragraphId: String): OutlineUnit? =
        units.firstOrNull { paragraphId in it.paragraphIds }

    /** Build an outline from a fully loaded book; used for packages without one. */
    companion object {
        fun from(index: ContentIndex): ContentOutline {
            val book = index.book
            return ContentOutline(
                bookId = book.id,
                title = book.title,
                author = book.author,
                edition = book.edition,
                description = book.description,
                units = index.units.map { unit ->
                    OutlineUnit(
                        id = unit.id,
                        chapterId = index.chapterByUnitId[unit.id]?.id,
                        number = unit.number,
                        title = unit.title,
                        paragraphIds = unit.paragraphs.map { it.id },
                    )
                },
            )
        }
    }
}

const val OUTLINE_SCHEMA_VERSION = 1
