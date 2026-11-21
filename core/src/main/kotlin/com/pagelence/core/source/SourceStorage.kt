package com.pagelence.core.source

import com.pagelence.core.content.Book
import com.pagelence.core.content.BookSummary
import com.pagelence.core.content.ContentIndex
import com.pagelence.core.content.ContentManifest
import com.pagelence.core.content.PagelenceJson
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlinx.serialization.Serializable

/**
 * User source settings in `filesDir/source-settings.json`.
 *
 * Deliberately outside `state.json`: source configuration is device-local and
 * must not be merged by WebDAV, while downloaded books live under their own
 * directory tree (`filesDir/downloaded/`).
 */
class SourceStore(private val file: Path) {
    fun settings(): SourceSettings = runCatching {
        if (!file.exists()) SourceSettings()
        else PagelenceJson.decodeFromString<SourceSettings>(file.readText(Charsets.UTF_8))
    }.getOrElse { SourceSettings() }

    fun save(settings: SourceSettings) {
        file.parent?.createDirectories()
        file.writeText(PagelenceJson.encodeToString(SourceSettings.serializer(), settings), Charsets.UTF_8)
    }
}

/**
 * Installed downloaded books under `root/`.
 *
 * Layout mirrors the shipped content package so [com.pagelence.core.content.FileContentRepository]
 * can read it as a second root:
 *
 * ```
 * downloaded/
 *   books-index.json       generated: books[] with size/sha256/stats
 *   books/<bookId>/book.json
 *   books/<bookId>/manifest.json   the package manifest
 *   books/<bookId>/LICENSE         optional
 *   books/<bookId>/installed.json  source, sha256, install time
 * ```
 */
class DownloadedBooks(private val root: Path) {
    /** Root of the second content package passed to `FileContentRepository`. */
    val dir: Path get() = root

    private val booksDir: Path get() = root.resolve("books")

    fun list(): List<InstalledBook> {
        if (!booksDir.isDirectory()) return emptyList()
        val found = mutableListOf<InstalledBook>()
        Files.newDirectoryStream(booksDir).use { stream ->
            for (entry in stream) {
                if (!entry.isDirectory()) continue
                val meta = entry.resolve("installed.json")
                if (!meta.exists()) continue
                runCatching {
                    PagelenceJson.decodeFromString<InstalledBook>(meta.readText(Charsets.UTF_8))
                }.getOrNull()?.let(found::add)
            }
        }
        return found.sortedBy { it.title.zh ?: it.title.en ?: it.bookId }
    }

    fun book(bookId: String): Book? {
        val file = booksDir.resolve(bookId).resolve("book.json")
        if (!file.exists()) return null
        return runCatching {
            PagelenceJson.decodeFromString<Book>(file.readText(Charsets.UTF_8))
        }.getOrNull()
    }

    fun install(packageFile: BookPackage, meta: InstalledBook): InstalledBook {
        val target = booksDir.resolve(meta.bookId)
        val temp = booksDir.resolve(".${meta.bookId}.tmp")
        temp.toFile().deleteRecursively()
        temp.createDirectories()
        temp.resolve("book.json").writeText(
            PagelenceJson.encodeToString(Book.serializer(), packageFile.book),
            Charsets.UTF_8,
        )
        temp.resolve("books-index.json").writeText(
            PagelenceJson.encodeToString(PackageManifest.serializer(), packageFile.manifest),
            Charsets.UTF_8,
        )
        packageFile.license?.let { temp.resolve("LICENSE").writeText(it, Charsets.UTF_8) }
        temp.resolve("installed.json").writeText(
            PagelenceJson.encodeToString(InstalledBook.serializer(), meta),
            Charsets.UTF_8,
        )
        target.toFile().deleteRecursively()
        Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING)
        rebuildManifest()
        return meta
    }

    fun remove(bookId: String): Boolean {
        val target = booksDir.resolve(bookId)
        if (!target.isDirectory()) return false
        target.toFile().deleteRecursively()
        rebuildManifest()
        return true
    }

    /** Rebuild `books-index.json` so `FileContentRepository` can index downloads. */
    fun rebuildManifest() {
        root.createDirectories()
        booksDir.createDirectories()
        val summaries = list().mapNotNull { installed ->
            book(installed.bookId)?.let { summarize(it, installed.bookId) }
        }
        val manifest = ContentManifest(books = summaries)
        root.resolve("books-index.json").writeText(
            PagelenceJson.encodeToString(ContentManifest.serializer(), manifest),
            Charsets.UTF_8,
        )
    }

    private fun summarize(book: Book, bookId: String): BookSummary {
        val stats = ContentIndex(book).stats()
        val path = booksDir.resolve(bookId).resolve("book.json")
        val bytes = Files.readAllBytes(path)
        return BookSummary(
            id = book.id,
            title = book.title,
            path = "books/$bookId/book.json",
            chapters = stats.chapters,
            units = stats.units,
            paragraphs = stats.paragraphs,
            sentences = stats.sentences,
            translatedSentences = stats.translatedSentences,
            words = stats.words,
            sizeBytes = bytes.size.toLong(),
            sha256 = Hashing.sha256(bytes),
        )
    }
}

@Serializable
data class CachedCatalog(
    val url: String,
    val etag: String? = null,
    val fetchedAt: String,
    val catalog: SourceCatalog,
)
