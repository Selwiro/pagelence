package com.pagelence.core.progress

/** One book as the library sees it, before the shelf filter is applied. */
data class LibraryEntry(
    val bookId: String,
    val readUnits: Int,
    /** True when a resume position exists for this book. */
    val hasResume: Boolean,
    val finished: Boolean,
)

/**
 * What the shelf shows: books being read, plus books the reader added without
 * starting them. Finished books leave the shelf, whether or not they were
 * added; the store and the profile still keep them.
 */
object LibraryRules {
    fun visible(entries: List<LibraryEntry>, shelfIds: Set<String>): List<LibraryEntry> =
        entries.filter { entry ->
            !entry.finished &&
                (entry.readUnits > 0 || entry.hasResume || entry.bookId in shelfIds)
        }
}
