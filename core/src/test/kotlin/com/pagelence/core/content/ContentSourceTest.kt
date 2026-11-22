package com.pagelence.core.content

import java.io.FileNotFoundException
import java.io.InputStream
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Covers the two package sources the app relies on: APK assets and a directory. */
class ContentSourceTest {
    private class FakeAssets(private val files: Map<String, String>) : AssetReader {
        override fun open(path: String): InputStream =
            files[path]?.byteInputStream(Charsets.UTF_8) ?: throw FileNotFoundException(path)

        override fun list(path: String): List<String> {
            val prefix = if (path.isEmpty()) "" else "$path/"
            return files.keys
                .filter { it.startsWith(prefix) && !it.removePrefix(prefix).contains('/') }
                .map { it.removePrefix(prefix) }
        }
    }

    @Test
    fun `asset source answers exists from the parent directory`() {
        val source = AssetContentSource(
            FakeAssets(
                mapOf(
                    "books-index.json" to "{}",
                    "books/alice-1866/book.json" to "{\"id\":\"alice-1866\"}",
                ),
            ),
        )

        assertTrue(source.exists("books-index.json"))
        assertTrue(source.exists("books/alice-1866/book.json"))
        assertFalse(source.exists("books/alice-1866/cover.jpg"))
        assertFalse(source.exists("missing/book.json"))
    }

    @Test
    fun `asset source reads text and reports missing files`() {
        val source = AssetContentSource(FakeAssets(mapOf("store.json" to "{\"books\":[]}")))

        assertEquals("{\"books\":[]}", source.readText("store.json"))
        assertFailsWith<FileNotFoundException> { source.readText("nope.json") }
    }

    @Test
    fun `asset source has no file path`() {
        val source = AssetContentSource(FakeAssets(emptyMap()))

        assertEquals(null, source.filePath("books/index.json"))
    }

    @Test
    fun `file source exposes a real path`() {
        val root = Files.createTempDirectory("pagelence-source")
        Files.writeString(root.resolve("store.json"), "{}")

        val source = FileContentSource(root)

        assertTrue(source.exists("store.json"))
        assertEquals("{}", source.readText("store.json"))
        assertTrue(source.filePath("store.json").endsWith("store.json"))
    }

    @Test
    fun `in-memory source behaves like a package`() {
        val source = InMemoryContentSource(mapOf("a/b.json" to "{}"))

        assertTrue(source.exists("a/b.json"))
        assertEquals("{}", source.readText("a/b.json"))
        assertFailsWith<FileNotFoundException> { source.readText("a/c.json") }
    }

    @Test
    fun `catalog reads store json through the source`() {
        val source = InMemoryContentSource(
            mapOf("store.json" to """{"books":[{"id":"alice-1866","order":1}]}"""),
        )

        val catalog = FileBuiltInStoreCatalog(source)

        assertEquals(listOf("alice-1866"), catalog.books().map { it.id })
    }

    @Test
    fun `catalog falls back to an empty shelf for a missing file`() {
        val catalog = FileBuiltInStoreCatalog(InMemoryContentSource(emptyMap()))

        assertTrue(catalog.books().isEmpty())
    }
}
