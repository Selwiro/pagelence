package com.pagelence.core.content

import java.nio.file.Path
import kotlinx.serialization.Serializable

/**
 * The built-in shelf shown in the store before any book source is live.
 * `store.json` (version 2) carries curation only: `order`, `status`, `monogram`
 * and an optional marketing summary. Identity fields (`title`, `author`,
 * `level`) come from the book package for `available` entries; only `planned`
 * entries carry their own, with `levelSource: "estimated"`.
 *
 * See `docs/book-sources.md` and the per-book notes under `docs/books/`.
 */
@Serializable
data class BuiltInStoreBook(
    val id: String,
    /** Positive, unique; available books come first, then planned. */
    val order: Int = 0,
    /** `available`, `planned` or `retired`; retired is hidden from the shelf. */
    val status: String = "available",
    /** Set for planned books only; available reads the package manifest. */
    val title: LocalizedText? = null,
    val author: String? = null,
    /** Set for planned books only; available reads the package manifest. */
    val level: String? = null,
    /** `estimated` for planned books. */
    val levelSource: String? = null,
    /** One cover character; defaults to the first character of the Chinese title. */
    val monogram: String? = null,
    /** Optional hook; when absent the app falls back to `book.json.description`. */
    val summary: LocalizedText? = null,
    /** Curation for planned books; available books read these from the package index. */
    val category: LocalizedText? = null,
    val tags: List<LocalizedText> = emptyList(),
)

@Serializable
data class BuiltInStoreCatalog(
    val schemaVersion: Int = INDEX_SCHEMA_VERSION,
    val updatedAt: String? = null,
    val books: List<BuiltInStoreBook> = emptyList(),
)

object StoreCatalogRules {
    const val STATUS_AVAILABLE = "available"
    const val STATUS_PLANNED = "planned"
    const val STATUS_RETIRED = "retired"

    /**
     * Shelf order: available books first (by `order`), planned after (by
     * `order`), retired never shown. A retired book keeps its store entry so old
     * progress can still be looked up.
     */
    fun visible(books: List<BuiltInStoreBook>): List<BuiltInStoreBook> = books
        .filter { it.status != STATUS_RETIRED }
        .sortedWith(
            compareBy(
                { if (it.status == STATUS_AVAILABLE) 0 else 1 },
                { if (it.order > 0) it.order else Int.MAX_VALUE },
            ),
        )
}

/** Reads `store.json` from a package source; a missing or malformed file yields an empty shelf. */
class FileBuiltInStoreCatalog(private val source: ContentSource) {
    /** Convenience for tests and file-based tools. */
    constructor(contentDir: Path) : this(FileContentSource(contentDir))

    fun catalog(): BuiltInStoreCatalog {
        if (!source.exists("store.json")) return BuiltInStoreCatalog()
        return runCatching {
            PagelenceJson.decodeFromString<BuiltInStoreCatalog>(source.readText("store.json"))
        }.getOrDefault(BuiltInStoreCatalog())
    }

    fun books(): List<BuiltInStoreBook> = StoreCatalogRules.visible(catalog().books)
}
