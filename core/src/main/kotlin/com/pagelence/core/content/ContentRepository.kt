package com.pagelence.core.content

import com.pagelence.core.lexicon.DictionaryFile
import com.pagelence.core.lexicon.DictionarySource
import com.pagelence.core.lexicon.BookOccurrences
import com.pagelence.core.lexicon.Lexicon
import com.pagelence.core.lexicon.WordFrequencyTable
import com.pagelence.core.lexicon.WordLevelTable
import com.pagelence.core.lexicon.WordLookup
import com.pagelence.core.lexicon.WordQuery
import com.pagelence.core.wordcards.WordCardRepository
import java.nio.file.Path

/**
 * Read access to books, the grammar library and the word tables.
 *
 * `content/` is read-only at runtime: every book is loaded once and kept in
 * memory (the built-in package is a few megabytes including dictionaries).
 */
interface ContentRepository {
    fun library(): List<BookSummary>

    fun book(bookId: String): Book?

    fun index(bookId: String): ContentIndex?

    /**
     * The lightweight per-book index used by the shelf and the store. Never
     * parses `book.json`; null when the package has no outline and no fallback.
     */
    fun outline(bookId: String): ContentOutline? = null

    fun grammarPoint(id: String): GrammarPoint?

    /** Every grammar point shipped with the app, for the "grammar book" list. */
    fun grammarPoints(): List<GrammarPoint>

    fun lookup(
        bookId: String,
        unitId: String,
        paragraphId: String,
        sentenceIndex: Int,
        spelling: String,
    ): WordLookup?

    /** Absolute path of a package `cover.jpg`, or null when the book has none. */
    fun coverPath(bookId: String): String? = null
}

/**
 * Loads a content package through a [ContentSource].
 *
 * The shipped package is read straight from the APK assets; [extra] is a
 * second, writable source for downloaded books (`filesDir/downloaded/`, same
 * layout). Lookup tables always come from the shipped source; a downloaded book
 * shares the word levels, dictionaries and grammar library, and simply has no
 * per-book frequency/occurrence tables.
 *
 * The `Path` constructor is kept for tests and tools; the app passes sources.
 */
class FileContentRepository(
    private val shipped: ContentSource,
    private val cards: WordCardRepository? = null,
    private val extra: ContentSource? = null,
    /** Optional downloaded full ECDICT table, queried after the shipped core. */
    private val fullDictionary: DictionarySource? = null,
    /**
     * The binary lexicon index, loaded on the first lookup. A provider rather
     * than a value so the app does not read ~11 MB at startup and a test can
     * keep using the JSON tables. Null keeps the JSON fallback below.
     */
    private val lexicon: () -> Lexicon? = { null },
) : ContentRepository {
    /** Convenience for tests and file-based tools. */
    constructor(
        contentDir: Path,
        cards: WordCardRepository? = null,
        extraContentDir: Path? = null,
        fullDictionary: DictionarySource? = null,
        lexicon: () -> Lexicon? = { null },
    ) : this(
        FileContentSource(contentDir),
        cards,
        extraContentDir?.let { FileContentSource(it) },
        fullDictionary,
        lexicon,
    )

    private var manifestCache: ContentManifest? = null
    private var extraManifestCache: ContentManifest? = null

    private fun manifest(): ContentManifest =
        manifestCache ?: loadManifest(shipped).also { manifestCache = it }

    private fun extraManifest(): ContentManifest? {
        extraManifestCache?.let { return it }
        val source = extra ?: return null
        if (!source.exists("books-index.json")) return null
        return loadManifest(source).also { extraManifestCache = it }
    }

    private fun summaries(): List<BookSummary> = manifest().books + extraManifest()?.books.orEmpty()

    private fun sourceOf(summary: BookSummary): ContentSource = when {
        extraManifest()?.books?.any { it.id == summary.id } == true -> extra ?: shipped
        else -> shipped
    }

    /** Re-read manifests after a book was downloaded or removed. */
    fun refresh() {
        manifestCache = null
        extraManifestCache = null
        bookCache.clear()
        indexCache.clear()
        queries.clear()
        outlineCache.clear()
    }

    private val bookCache = mutableMapOf<String, Book>()
    private val indexCache = mutableMapOf<String, ContentIndex>()
    private val outlineCache = mutableMapOf<String, ContentOutline?>()

    private val grammar: Map<String, GrammarPoint> by lazy {
        val path = "grammar/grammar-library.json"
        if (!shipped.exists(path)) emptyMap()
        else PagelenceJson.decodeFromString<GrammarLibrary>(shipped.readText(path)).points.associateBy { it.id }
    }

    private val levels: WordLevelTable by lazy {
        val path = "third-party/word-levels.json"
        if (!shipped.exists(path)) WordLevelTable() else WordQuery.parseLevels(shipped.readText(path))
    }

    private val occurrences = mutableMapOf<String, BookOccurrences>()
    private val queries = mutableMapOf<String, WordQuery>()

    /** One app-wide wordfreq table; book counts are scanned, not shipped. */
    private val frequencies: WordFrequencyTable by lazy {
        val path = "third-party/word-frequencies.json"
        if (!shipped.exists(path)) WordFrequencyTable()
        else WordQuery.parseWordFrequencies(shipped.readText(path))
    }

    private val dictionary: DictionaryFile? by lazy {
        val path = "third-party/dictionaries/ecdict.json"
        if (shipped.exists(path)) WordQuery.parseDictionary(shipped.readText(path)) else null
    }

    override fun library(): List<BookSummary> = summaries()

    override fun book(bookId: String): Book? {
        bookCache[bookId]?.let { return it }
        val summary = summaries().firstOrNull { it.id == bookId } ?: return null
        val loaded = loadBook(sourceOf(summary), summary.path)
        bookCache[bookId] = loaded
        return loaded
    }

    override fun index(bookId: String): ContentIndex? {
        indexCache[bookId]?.let { return it }
        val loaded = book(bookId) ?: return null
        val created = ContentIndex(loaded)
        indexCache[bookId] = created
        return created
    }

    override fun outline(bookId: String): ContentOutline? = outlineCache.getOrPut(bookId) {
        val summary = summaries().firstOrNull { it.id == bookId } ?: return@getOrPut null
        val source = sourceOf(summary)
        val path = summary.path.substringBeforeLast('/') + "/outline.json"
        if (!source.exists(path)) return@getOrPut null
        runCatching { PagelenceJson.decodeFromString<ContentOutline>(source.readText(path)) }.getOrNull()
    }

    override fun grammarPoint(id: String): GrammarPoint? = grammar[id]

    override fun grammarPoints(): List<GrammarPoint> = grammar.values.sortedWith(compareBy({ it.levelStart }, { it.id }))

    override fun lookup(
        bookId: String,
        unitId: String,
        paragraphId: String,
        sentenceIndex: Int,
        spelling: String,
    ): WordLookup? = query(bookId)?.lookup(unitId, paragraphId, sentenceIndex, spelling)

    override fun coverPath(bookId: String): String? {
        val summary = summaries().firstOrNull { it.id == bookId } ?: return null
        val source = sourceOf(summary)
        val cover = summary.path.substringBeforeLast('/') + "/cover.jpg"
        if (!source.exists(cover)) return null
        return source.filePath(cover)
    }

    private fun query(bookId: String): WordQuery? {
        val index = index(bookId) ?: return null
        return queries.getOrPut(bookId) {
            val indexed = lexicon()
            WordQuery(
                index = index,
                levels = indexed?.levels ?: levels,
                frequencies = indexed?.frequencies
                    ?: frequencies.takeIf { it.entries.isNotEmpty() },
                // The whole book is scanned once on first lookup and the table
                // stays in memory for the session; nothing is written to disk.
                occurrences = occurrences.getOrPut(bookId) {
                    BookOccurrences(WordQuery.countWords(index.book))
                },
                ecdict = indexed?.dictionary ?: dictionary,
                fullDictionary = fullDictionary,
                cards = cards,
            )
        }
    }

    companion object {
        fun loadManifest(source: ContentSource): ContentManifest =
            PagelenceJson.decodeFromString(source.readText("books-index.json"))

        /** File-based convenience, used by tests and tools. */
        fun loadManifest(contentDir: Path): ContentManifest = loadManifest(FileContentSource(contentDir))

        fun loadBook(source: ContentSource, path: String): Book =
            PagelenceJson.decodeFromString(source.readText(path))

        /** File-based convenience, used by tests and tools. */
        fun loadBook(path: Path): Book = loadBook(FileContentSource(path.parent), path.fileName.toString())
    }
}

/** In-memory repository for tests, previews and the importer. */
class InMemoryContentRepository(
    books: List<Book>,
    private val grammarPoints: List<GrammarPoint> = emptyList(),
    private val levels: WordLevelTable = WordLevelTable(),
    private val cards: WordCardRepository? = null,
) : ContentRepository {
    private val allBooks: List<Book> = books
    private val bookById = allBooks.associateBy { it.id }
    private val indexById = allBooks.associate { it.id to ContentIndex(it) }
    private val grammarById = grammarPoints.associateBy { it.id }

    override fun library(): List<BookSummary> = allBooks.map { book ->
        val stats = ContentIndex(book).stats()
        BookSummary(
            id = book.id,
            title = book.title,
            path = "memory/${book.id}/book.json",
            chapters = stats.chapters,
            units = stats.units,
            paragraphs = stats.paragraphs,
            sentences = stats.sentences,
            translatedSentences = stats.translatedSentences,
            words = stats.words,
        )
    }

    override fun book(bookId: String): Book? = bookById[bookId]

    override fun index(bookId: String): ContentIndex? = indexById[bookId]

    override fun outline(bookId: String): ContentOutline? = indexById[bookId]?.let(ContentOutline::from)

    override fun grammarPoint(id: String): GrammarPoint? = grammarById[id]

    override fun grammarPoints(): List<GrammarPoint> = grammarPoints.sortedWith(compareBy({ it.levelStart }, { it.id }))

    override fun lookup(
        bookId: String,
        unitId: String,
        paragraphId: String,
        sentenceIndex: Int,
        spelling: String,
    ): WordLookup? {
        val index = indexById[bookId] ?: return null
        return WordQuery(index = index, levels = levels, cards = cards)
            .lookup(unitId, paragraphId, sentenceIndex, spelling)
    }
}
