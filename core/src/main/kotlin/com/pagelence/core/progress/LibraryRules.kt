package com.pagelence.core.progress

/**
 * One book as the library sees it, before the shelf filter is applied.
 *
 * [lastReadAt] and [finishedAt] are ISO-8601 strings and only used for ordering;
 * a book that was finished before the field existed has no [finishedAt] and is
 * still listed in the finished group.
 */
data class LibraryEntry(
    val bookId: String,
    val readUnits: Int,
    /** True when a resume position exists for this book. */
    val hasResume: Boolean,
    val finished: Boolean,
    val lastReadAt: String? = null,
    val finishedAt: String? = null,
)

/** The shelf has two groups: books being read, and books already finished. */
data class LibrarySections(
    val reading: List<LibraryEntry>,
    val finished: List<LibraryEntry>,
) {
    val isEmpty: Boolean get() = reading.isEmpty() && finished.isEmpty()
}

/**
 * What the shelf shows.
 *
 * - **在读**: started or added books that are not complete, newest read first;
 * - **已读完**: every complete book, newest finish first (a missing
 *   [LibraryEntry.finishedAt] falls back to the last read time).
 *
 * Finishing a book never removes it from the library: it moves to the second
 * group, where it keeps its progress and offers a re-read.
 */
object LibraryRules {
    fun sections(entries: List<LibraryEntry>, shelfIds: Set<String>): LibrarySections {
        val reading = entries
            .filter { entry ->
                !entry.finished &&
                    (entry.readUnits > 0 || entry.hasResume || entry.bookId in shelfIds)
            }
            .sortedWith(
                compareByDescending<LibraryEntry> { it.lastReadAt.orEmpty() }
                    .thenBy { it.bookId },
            )
        val finished = entries
            .filter { it.finished }
            .sortedWith(
                compareByDescending<LibraryEntry> { it.finishedAt ?: it.lastReadAt.orEmpty() }
                    .thenBy { it.bookId },
            )
        return LibrarySections(reading = reading, finished = finished)
    }
}
