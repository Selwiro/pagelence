package com.pagelence.core.progress

/**
 * Shelf membership for books the reader added before starting them.
 *
 * Kept separate from progress: adding or removing a book never touches the
 * reading position or the finished-unit set, and a book started without being
 * added still appears in the library through its progress.
 */
object ShelfRules {
    fun contains(ids: List<String>, bookId: String): Boolean = bookId in ids

    /** Add the book when absent, remove it when present; order is preserved. */
    fun toggle(ids: List<String>, bookId: String): List<String> =
        if (bookId in ids) ids - bookId else ids + bookId
}
