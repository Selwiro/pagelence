package com.pagelence.core.lexicon

/**
 * One read-only term → entry table.
 *
 * The shipped core dictionary is an in-memory map ([DictionaryFile]); the
 * optional full ECDICT table is a SQLite index behind the same interface, so
 * [WordQuery] queries the core table first and falls through to the full table
 * without a second lookup path. [term] is a normalized key (lower case,
 * possessive stripped).
 */
interface DictionarySource {
    fun entry(term: String): DictionaryEntry?
}
