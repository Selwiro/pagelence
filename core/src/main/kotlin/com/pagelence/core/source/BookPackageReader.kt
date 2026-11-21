package com.pagelence.core.source

import com.pagelence.core.content.Book
import com.pagelence.core.content.PagelenceJson
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipInputStream

/**
 * `.pagelence` package reader: a ZIP with exactly `manifest.json`, `book.json`
 * and optionally `LICENSE`.
 *
 * Packages come from the network and are untrusted. Entry names are checked
 * before anything is read, and total sizes are capped, so a zip bomb or a path
 * traversal is rejected instead of extracted.
 */
object BookPackageReader {
    val REQUIRED = setOf("manifest.json", "book.json")
    val OPTIONAL = setOf("LICENSE", "LICENSE.txt", "README.txt")

    fun read(bytes: ByteArray, expectedBookId: String? = null): SourceResult<BookPackage> {
        if (bytes.size > SourceLimits.MAX_PACKAGE_BYTES) {
            return SourceResult.Err(SourceError.TooLarge(bytes.size.toLong()))
        }
        val entries = LinkedHashMap<String, ByteArray>()
        var total = 0L
        try {
            ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
                var count = 0
                while (true) {
                    val entry = zip.nextEntry ?: break
                    count += 1
                    if (count > SourceLimits.MAX_ENTRIES) {
                        return SourceResult.Err(SourceError.BadPackage("too many entries"))
                    }
                    val name = entry.name
                    if (entry.isDirectory) continue
                    if (!isSafeName(name)) {
                        return SourceResult.Err(SourceError.BadPackage("unsafe entry $name"))
                    }
                    if (name !in REQUIRED && name !in OPTIONAL) {
                        return SourceResult.Err(SourceError.BadPackage("unexpected entry $name"))
                    }
                    if (entries.containsKey(name)) {
                        return SourceResult.Err(SourceError.BadPackage("duplicate entry $name"))
                    }
                    val buffer = ByteArrayOutputStream()
                    val chunk = ByteArray(16 * 1024)
                    var entrySize = 0L
                    while (true) {
                        val read = zip.read(chunk)
                        if (read <= 0) break
                        entrySize += read
                        total += read
                        if (entrySize > SourceLimits.MAX_ENTRY_BYTES) {
                            return SourceResult.Err(SourceError.BadPackage("entry $name is too large"))
                        }
                        if (total > SourceLimits.MAX_UNCOMPRESSED_BYTES) {
                            return SourceResult.Err(SourceError.BadPackage("package expands too much"))
                        }
                        buffer.write(chunk, 0, read)
                    }
                    entries[name] = buffer.toByteArray()
                }
            }
        } catch (error: Exception) {
            return SourceResult.Err(SourceError.BadPackage(error.message ?: "unreadable zip"))
        }

        val missing = REQUIRED - entries.keys
        if (missing.isNotEmpty()) {
            return SourceResult.Err(SourceError.BadPackage("missing ${missing.joinToString()}"))
        }
        val manifest = runCatching {
            PagelenceJson.decodeFromString<PackageManifest>(entries.getValue("manifest.json").toString(Charsets.UTF_8))
        }.getOrElse {
            return SourceResult.Err(SourceError.BadPackage("manifest.json: ${it.message}"))
        }
        if (manifest.schemaVersion != PACKAGE_SCHEMA_VERSION) {
            return SourceResult.Err(SourceError.BadPackage("unsupported package schemaVersion ${manifest.schemaVersion}"))
        }
        if (expectedBookId != null && manifest.bookId != expectedBookId) {
            return SourceResult.Err(SourceError.BadPackage("manifest bookId ${manifest.bookId} != $expectedBookId"))
        }
        val book: Book = runCatching {
            PagelenceJson.decodeFromString<Book>(entries.getValue("book.json").toString(Charsets.UTF_8))
        }.getOrElse {
            return SourceResult.Err(SourceError.BadPackage("book.json: ${it.message}"))
        }
        if (book.id != manifest.bookId) {
            return SourceResult.Err(SourceError.BadPackage("book id ${book.id} != manifest ${manifest.bookId}"))
        }
        val license = entries["LICENSE"]?.toString(Charsets.UTF_8)
            ?: entries["LICENSE.txt"]?.toString(Charsets.UTF_8)
        return SourceResult.Ok(BookPackage(manifest, book, license))
    }

    private fun isSafeName(name: String): Boolean {
        if (name.isBlank()) return false
        if (name.startsWith("/") || name.startsWith("\\")) return false
        if (name.contains("..")) return false
        if (name.contains('\\')) return false
        return true
    }
}
