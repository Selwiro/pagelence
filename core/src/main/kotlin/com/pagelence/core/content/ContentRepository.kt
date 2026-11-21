package com.pagelence.core.content

import com.pagelence.core.lexicon.DictionaryFile
import com.pagelence.core.lexicon.DictionarySource
import com.pagelence.core.lexicon.BookOccurrences
import com.pagelence.core.lexicon.WordFrequencyTable
import com.pagelence.core.lexicon.WordLevelTable
import com.pagelence.core.lexicon.WordLookup
import com.pagelence.core.lexicon.WordQuery
import com.pagelence.core.wordcards.WordCardRepository
import java.nio.file.Path
import kotlin.io.path.exists

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
 * Loads the content package from `work/android/content`.
 *
 * [extraContentDir] is a second, writable root for downloaded books
 * (`filesDir/downloaded/`, same layout). Lookup tables always come from the
 * shipped root; a downloaded book shares the word levels, dictionaries and
 * grammar library, and simply has no per-book frequency/occurrence tables.
 */
class FileContentRepository(
    private val contentDir: Path,
    private val cards: WordCardRepository? = null,
    private val extraContentDir: Path? = null,
    /** Optional downloaded full ECDICT table, queried after the shipped core. */
    private val fullDictionary: DictionarySource? = null,
) : ContentRepository {
    private var manifestCache: ContentManifest? = null
    private var extraManifestCache: ContentManifest? = null

    private fun manifest(): ContentManifest =
        manifestCache ?: loadManifest(contentDir).also { manifestCache = it }

    private fun extraManifest(): ContentManifest? {
        extraManifestCache?.let { return it }
        val root = extraContentDir ?: return null
        if (!root.resolve("books-index.json").exists()) return null
        return loadManifest(root).also { extraManifestCache = it }
    }

    private fun summaries(): List<BookSummary> = manifest().books + extraManifest()?.books.orEmpty()

    private fun rootOf(summary: BookSummary): Path =
        if (extraManifest()?.books?.any { it.id == summary.id } == true) extraContentDir!! else contentDir

    /** Re-read manifests after a book was downloaded or removed. */
    fun refresh() {
        manifestCache = null
        extraManifestCache = null
        bookCache.clear()
        indexCache.clear()
        queries.clear()
    }

    private val bookCache = mutableMapOf<String, Book>()
    private val indexCache = mutableMapOf<String, ContentIndex>()

    private val grammar: Map<String, GrammarPoint> by lazy {
        val path = contentDir.resolve("grammar/grammar-library.json")
        if (!path.exists()) emptyMap()
        else PagelenceJson.decodeFromString<GrammarLibrary>(path.toFile().readText(Charsets.UTF_8)).points.associateBy { it.id }
    }

    private val levels: WordLevelTable by lazy {
        val path = contentDir.resolve("third-party/word-levels.json")
        if (!path.exists()) WordLevelTable() else WordQuery.parseLevels(path.toFile().readText(Charsets.UTF_8))
    }

    private val occurrences = mutableMapOf<String, BookOccurrences>()
    private val queries = mutableMapOf<String, WordQuery>()

    /** One app-wide wordfreq table; book counts are scanned, not shipped. */
    private val frequencies: WordFrequencyTable by lazy {
        val path = contentDir.resolve("third-party/word-frequencies.json")
        if (!path.exists()) WordFrequencyTable()
        else WordQuery.parseWordFrequencies(path.toFile().readText(Charsets.UTF_8))
    }

    private val dictionary: DictionaryFile? by lazy {
        val path = contentDir.resolve("third-party/dictionaries/ecdict.json")
        if (path.exists()) WordQuery.parseDictionary(path.toFile().readText(Charsets.UTF_8)) else null
    }

    override fun library(): List<BookSummary> = summaries()

    override fun book(bookId: String): Book? {
        bookCache[bookId]?.let { return it }
        val summary = summaries().firstOrNull { it.id == bookId } ?: return null
        val loaded = loadBook(rootOf(summary).resolve(summary.path))
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
        val cover = rootOf(summary).resolve(summary.path).parent.resolve("cover.jpg")
        return if (cover.exists()) cover.toString() else null
    }

    private fun query(bookId: String): WordQuery? {
        val index = index(bookId) ?: return null
        return queries.getOrPut(bookId) {
            WordQuery(
                index = index,
                levels = levels,
                frequencies = frequencies.takeIf { it.entries.isNotEmpty() },
                // The whole book is scanned once on first lookup and the table
                // stays in memory for the session; nothing is written to disk.
                occurrences = occurrences.getOrPut(bookId) {
                    BookOccurrences(WordQuery.countWords(index.book))
                },
                ecdict = dictionary,
                fullDictionary = fullDictionary,
                cards = cards,
            )
        }
    }

    companion object {
        fun loadManifest(contentDir: Path): ContentManifest {
            val path = contentDir.resolve("books-index.json")
            return PagelenceJson.decodeFromString(path.toFile().readText(Charsets.UTF_8))
        }

        fun loadBook(path: Path): Book = PagelenceJson.decodeFromString(path.toFile().readText(Charsets.UTF_8))
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
