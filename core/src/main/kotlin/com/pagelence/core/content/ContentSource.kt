package com.pagelence.core.content

import java.io.FileNotFoundException
import java.io.InputStream
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.readText

/**
 * Where a content package lives.
 *
 * The shipped package sits inside the APK (`assets/content`), which is not a
 * file system path; downloaded packages sit in `filesDir/downloaded`. Everything
 * in `:core` that reads a package goes through this interface, so the two cases
 * share one loader and nothing has to be copied onto disk first.
 *
 * Paths are package-relative, use `/` and never start with one
 * (`books/alice-1866/book.json`).
 */
interface ContentSource {
    fun exists(path: String): Boolean

    fun readText(path: String): String

    /**
     * Absolute file path for a package file, when the source is backed by the
     * file system. Assets return null: callers that need a real path (cover
     * images today) fall back to the default layout cover.
     */
    fun filePath(path: String): String? = null
}

/** A package unpacked in a real directory (tests, downloaded books). */
class FileContentSource(private val root: Path) : ContentSource {
    override fun exists(path: String): Boolean = root.resolve(path).exists()

    override fun readText(path: String): String = root.resolve(path).readText(Charsets.UTF_8)

    override fun filePath(path: String): String = root.resolve(path).toString()
}

/**
 * The minimal `AssetManager` surface the package loader needs.
 *
 * Declared here instead of referencing `android.content.res.AssetManager` so
 * the asset source can be unit-tested on a plain JVM with a fake reader.
 */
interface AssetReader {
    fun open(path: String): InputStream

    fun list(path: String): List<String>
}

/**
 * A package shipped inside the APK. `AssetManager` has no `exists`, so it is
 * answered by listing the parent directory; that keeps `exists` cheap for the
 * handful of optional files (dictionary, levels, cover) that use it.
 */
class AssetContentSource(private val reader: AssetReader) : ContentSource {
    override fun exists(path: String): Boolean {
        val parent = path.substringBeforeLast('/', missingDelimiterValue = "")
        val name = path.substringAfterLast('/')
        if (name.isEmpty()) return false
        return runCatching { reader.list(parent).contains(name) }.getOrDefault(false)
    }

    override fun readText(path: String): String =
        reader.open(path).use { stream -> stream.bufferedReader(Charsets.UTF_8).readText() }
}

/** In-memory package for tests: missing files throw like a real source. */
class InMemoryContentSource(files: Map<String, String>) : ContentSource {
    private val entries = files.toMap()

    override fun exists(path: String): Boolean = entries.containsKey(path)

    override fun readText(path: String): String =
        entries[path] ?: throw FileNotFoundException(path)
}
