package com.pagelence.core.source

import com.pagelence.core.ContentFixtures
import com.pagelence.core.content.Book
import com.pagelence.core.content.LocalizedText
import com.pagelence.core.content.PagelenceJson
import java.io.ByteArrayOutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.io.path.readText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

private fun <T> SourceResult<T>.orFail(): T = when (this) {
    is SourceResult.Ok -> value
    is SourceResult.Err -> error("unexpected failure: ${error.detail}")
}

private fun SourceResult<*>.orError(): SourceError = when (this) {
    is SourceResult.Err -> error
    is SourceResult.Ok -> error("expected a failure")
}

class SourceRulesTest {
    private val now = Instant.parse("2026-11-19T10:00:00Z")

    @Test
    fun `catalog accepts a valid HTTPS book`() {
        val catalog = SourceCatalog(
            catalogVersion = "2026-11-20",
            books = listOf(book()),
        )
        assertTrue(CatalogRules.validate(catalog, appVersionCode = 1).isEmpty())
    }

    @Test
    fun `catalog rejects insecure urls, duplicates and oversized books`() {
        val bad = SourceCatalog(
            books = listOf(
                book(id = "a", url = "http://example.com/a.zip", sha = "not-a-hash", size = SourceLimits.MAX_PACKAGE_BYTES + 1),
                book(id = "a", url = "https://example.com/a.zip"),
            ),
        )
        val errors = CatalogRules.validate(bad, appVersionCode = 1)
        assertTrue(errors.any { it.contains("HTTPS") }, errors.toString())
        assertTrue(errors.any { it.contains("duplicate") }, errors.toString())
        assertTrue(errors.any { it.contains("sha256") }, errors.toString())
        assertTrue(errors.any { it.contains("exceeds") }, errors.toString())
    }

    @Test
    fun `default catalog drops books outside the target regions`() {
        val catalog = SourceCatalog(
            catalogVersion = "2026-11-20",
            books = listOf(
                book(id = "in-cn", url = "https://example.com/a.zip").copy(publicDomain = listOf("CN")),
                book(id = "us-only", url = "https://example.com/b.zip").copy(publicDomain = listOf("US")),
                book(id = "uk-only", url = "https://example.com/c.zip").copy(publicDomain = listOf("GB")),
                book(id = "permission", url = "https://example.com/d.zip").copy(
                    sourceType = "permission",
                    publicDomain = emptyList(),
                ),
            ),
        )
        val filtered = CatalogRules.forRegions(catalog, setOf("CN", "US"))
        assertEquals(setOf("in-cn", "us-only", "permission"), filtered.books.map { it.id }.toSet())
    }

    @Test
    fun `catalog v2 requires licence fields and a catalog version`() {
        val bad = SourceCatalog(
            books = listOf(
                book().copy(sourceType = "public-domain", publicDomain = emptyList()),
                book(id = "custom", url = "https://example.com/b.zip").copy(
                    creatorType = "custom",
                    creatorName = null,
                ),
            ),
        )
        val errors = CatalogRules.validate(bad, appVersionCode = 1)
        assertTrue(errors.any { it.contains("catalogVersion") }, errors.toString())
        assertTrue(errors.any { it.contains("publicDomain") }, errors.toString())
        assertTrue(errors.any { it.contains("creatorName") }, errors.toString())
    }

    @Test
    fun `min app version gates a book`() {
        val catalog = SourceCatalog(catalogVersion = "2026-11-20", books = listOf(book(minApp = 5)))
        val errors = CatalogRules.validate(catalog, appVersionCode = 3)
        assertTrue(errors.any { it.contains("app version") }, errors.toString())
    }

    @Test
    fun `local http is only allowed for loopback hosts when enabled`() {
        assertFalse(CatalogRules.isAllowedUrl("http://example.com/a", allowLocalHttp = true))
        assertTrue(CatalogRules.isAllowedUrl("http://10.0.2.2:8000/a", allowLocalHttp = true))
        assertFalse(CatalogRules.isAllowedUrl("http://10.0.2.2:8000/a", allowLocalHttp = false))
        assertTrue(CatalogRules.isAllowedUrl("https://example.com/a", allowLocalHttp = false))
    }

    @Test
    fun `the same book from two sources merges into one entry`() {
        val one = snapshot("default", book(id = "b1", sha = "a".repeat(64)))
        val two = snapshot("custom-x", book(id = "b1", sha = "a".repeat(64)))
        val merged = CatalogRules.merge(listOf(one, two))
        assertEquals(1, merged.size)
        assertEquals(listOf("default", "custom-x"), merged.first().sources)
    }

    @Test
    fun `package reader accepts a valid package`() {
        val result = BookPackageReader.read(zip(), expectedBookId = "alice")
        val packageFile = result.orFail()
        assertEquals("alice", packageFile.book.id)
        assertNotNull(packageFile.license)
        assertTrue(packageFile.license.orEmpty().contains("Public domain"))
    }

    @Test
    fun `package reader rejects traversal and unexpected entries`() {
        val traversal = BookPackageReader.read(zip(extraEntry = "../evil.json"))
        assertTrue(traversal is SourceResult.Err)
        val unexpected = BookPackageReader.read(zip(extraEntry = "assets/x.png"))
        assertTrue(unexpected is SourceResult.Err)
        assertTrue(unexpected.orError() is SourceError.BadPackage)
    }

    @Test
    fun `package reader rejects a mismatched book id`() {
        val result = BookPackageReader.read(zip(bookId = "alice"), expectedBookId = "dolittle")
        assertTrue(result is SourceResult.Err)
        assertTrue(result.orError() is SourceError.BadPackage)
    }

    @Test
    fun `package reader rejects missing book json`() {
        val bytes = zip(entries = mapOf("manifest.json" to manifestJson("alice")))
        val result = BookPackageReader.read(bytes)
        assertTrue(result is SourceResult.Err)
    }

    @Test
    fun `manager falls back from direct to mirror`() {
        val root = Files.createTempDirectory("sources")
        val good = Pages.catalogJson(book())
        val fetcher = FakeFetcher(
            responses = mutableMapOf(
                DefaultBookSource.DIRECT_URL to HttpResult(code = 503),
                DefaultBookSource.MIRROR_URL to HttpResult(code = 200, body = good.toByteArray(), etag = "\"v1\""),
            ),
        )
        val manager = manager(root, fetcher)
        val result = manager.refresh(DefaultBookSource.ID)
        val snapshot = result.orFail()
        assertEquals(DefaultBookSource.MIRROR_URL, snapshot.endpoint)
        assertEquals(SourceLine.MIRROR, snapshot.line)
        assertEquals(1, snapshot.catalog.books.size)
    }

    @Test
    fun `manager reuses the cached catalog on 304`() {
        val root = Files.createTempDirectory("sources")
        val body = Pages.catalogJson(book()).toByteArray()
        val fetcher = FakeFetcher(
            responses = mutableMapOf(
                DefaultBookSource.DIRECT_URL to HttpResult(200, body, etag = "\"v1\""),
            ),
        )
        val manager = manager(root, fetcher)
        val first = manager.refresh(DefaultBookSource.ID).orFail()
        assertFalse(first.fromCache)

        fetcher.responses[DefaultBookSource.DIRECT_URL] = HttpResult(304)
        val second = manager.refresh(DefaultBookSource.ID).orFail()
        assertTrue(second.fromCache)
        assertEquals(first.fetchedAt, second.fetchedAt)
        assertEquals("\"v1\"", fetcher.lastEtag)
    }

    @Test
    fun `download verifies the checksum and installs the book`() {
        val root = Files.createTempDirectory("sources")
        val packageBytes = zip()
        val catalogBook = book(sha = Hashing.sha256(packageBytes), size = packageBytes.size.toLong())
        val fetcher = FakeFetcher(
            responses = mutableMapOf(catalogBook.url to HttpResult(200, packageBytes)),
        )
        val manager = manager(root, fetcher)
        val installed = manager.download(catalogBook, DefaultBookSource.ID).orFail()
        assertEquals("alice", installed.bookId)
        assertEquals(1, installed.chapters)
        assertEquals(1, installed.units)
        assertNotNull(manager.installedBook("alice"))
        assertTrue(root.resolve("downloaded/books-index.json").toFile().exists())
        val manifest = root.resolve("downloaded/books-index.json").readText()
        assertTrue(manifest.contains("\"alice\""), manifest)

        assertTrue(manager.removeInstalled("alice"))
        assertNull(manager.installedBook("alice"))
    }

    @Test
    fun `download rejects a checksum mismatch`() {
        val root = Files.createTempDirectory("sources")
        val catalogBook = book(sha = "b".repeat(64))
        val fetcher = FakeFetcher(mutableMapOf(catalogBook.url to HttpResult(200, zip())))
        val manager = manager(root, fetcher)
        val result = manager.download(catalogBook, DefaultBookSource.ID)
        assertTrue(result is SourceResult.Err)
        assertTrue(result.orError() is SourceError.Checksum)
    }

    @Test
    fun `custom sources reject http until the advanced switch is on`() {
        val root = Files.createTempDirectory("sources")
        val manager = manager(root, FakeFetcher())
        assertTrue(manager.addCustomSource("LAN", "http://10.0.2.2:8000/catalog.json") is SourceResult.Err)
        manager.setAllowLocalHttp(true)
        val added = manager.addCustomSource("LAN", "http://10.0.2.2:8000/catalog.json")
        assertEquals("LAN", added.orFail().name)
        assertEquals(1, manager.settings().customSources.size)
        manager.setCustomSourceEnabled(added.orFail().id, false)
        assertEquals(0, manager.endpoints().count { it.sourceId == added.orFail().id })
    }

    @Test
    fun `import installs a package without a catalog entry`() {
        val root = Files.createTempDirectory("sources")
        val manager = manager(root, FakeFetcher())

        val installed = manager.importPackage(zip(), ImportedSource.LOCAL).orFail()
        assertEquals("alice", installed.bookId)
        assertEquals(ImportedSource.LOCAL, installed.sourceId)
        assertEquals(1, installed.units)
        assertNotNull(manager.installedBook("alice"))

        assertTrue(manager.importPackage(ByteArray(0), ImportedSource.LOCAL) is SourceResult.Err)
    }

    @Test
    fun `import refuses a book that ships with the app`() {
        val root = Files.createTempDirectory("sources")
        val manager = manager(root, FakeFetcher(), shipped = setOf("alice"))

        val result = manager.importPackage(zip(), ImportedSource.LOCAL)
        assertTrue(result.orError() is SourceError.AlreadyBuiltIn)
        assertNull(manager.installedBook("alice"))
    }

    @Test
    fun `import from url fetches one package and only allows https`() {
        val root = Files.createTempDirectory("sources")
        val url = "https://example.com/alice.pagelence"
        val manager = manager(root, FakeFetcher(mutableMapOf(url to HttpResult(200, zip()))))

        val installed = manager.importFromUrl(url).orFail()
        assertEquals(ImportedSource.ONLINE, installed.sourceId)
        assertEquals("alice", installed.bookId)

        assertTrue(manager.importFromUrl("http://example.com/alice.pagelence").orError() is SourceError.Insecure)
        assertTrue(manager.importFromUrl("https://example.com/missing.pagelence").orError() is SourceError.Http)
    }

    private fun manager(
        root: Path,
        fetcher: FakeFetcher,
        shipped: Set<String> = emptySet(),
    ): SourceManager = SourceManager(
        fetcher = fetcher,
        settingsStore = SourceStore(root.resolve("source-settings.json")),
        downloads = DownloadedBooks(root.resolve("downloaded")),
        cacheDir = root.resolve("cache"),
        appVersionCode = 1,
        now = { now },
        defaultSourceEnabled = true,
        shippedBookIds = { shipped },
    )

    private fun book(
        id: String = "alice",
        url: String = "https://example.com/alice.pagelence",
        sha: String = "a".repeat(64),
        size: Long = 1024,
        minApp: Int = 0,
    ) = CatalogBook(
        id = id,
        title = LocalizedText(zh = "爱丽丝", en = "Alice"),
        author = "Lewis Carroll",
        level = "B1",
        description = LocalizedText(zh = "测试书", en = "Test book"),
        sizeBytes = size,
        sha256 = sha,
        url = url,
        sourceType = "public-domain",
        publicDomain = listOf("CN", "US"),
        creatorType = "CC-BY-SA-4.0",
        creatorUrl = "https://creativecommons.org/licenses/by-sa/4.0/",
        minAppVersionCode = minApp,
    )

    private fun snapshot(sourceId: String, book: CatalogBook) = CatalogSnapshot(
        sourceId = sourceId,
        sourceName = sourceId,
        endpoint = "https://example.com/catalog.json",
        line = SourceLine.DIRECT,
        catalog = SourceCatalog(catalogVersion = "2026-11-20", books = listOf(book)),
        fetchedAt = now.toString(),
        fromCache = false,
    )

    /** A one-chapter, one-unit skeleton package; the shipped book is too big for a fixture. */
    private fun zip(
        bookId: String = "alice",
        extraEntry: String? = null,
        entries: Map<String, String>? = null,
    ): ByteArray {
        val files = entries?.toMutableMap() ?: mutableMapOf(
            "manifest.json" to manifestJson(bookId),
            "book.json" to bookJson(bookId),
            "LICENSE" to "Public domain in CN and US.",
        )
        extraEntry?.let { files[it] = "{}" }
        val output = ByteArrayOutputStream()
        ZipOutputStream(output).use { zip ->
            files.forEach { (name, content) ->
                zip.putNextEntry(ZipEntry(name))
                zip.write(content.toByteArray())
                zip.closeEntry()
            }
        }
        return output.toByteArray()
    }

    private fun manifestJson(bookId: String) = """
        {"schemaVersion":1,"bookId":"$bookId","title":{"zh":"爱丽丝","en":"Alice"},"version":"1"}
    """.trimIndent()

    private fun bookJson(bookId: String): String = PagelenceJson.encodeToString(
        Book.serializer(),
        Book(
            id = bookId,
            title = LocalizedText(zh = "爱丽丝", en = "Alice"),
            description = LocalizedText(zh = "测试包", en = "Test package"),
            chapters = listOf(
                com.pagelence.core.content.Chapter(
                    id = "chapter-01",
                    number = 1,
                    title = LocalizedText(zh = "第一章", en = "Chapter 1"),
                    units = listOf(
                        com.pagelence.core.content.StudyUnit(
                            id = "chapter-01-u01",
                            number = 1,
                            title = LocalizedText(zh = "掉进兔子洞", en = "Down the Rabbit-Hole"),
                            paragraphs = listOf(
                                com.pagelence.core.content.Paragraph(
                                    id = "chapter-01-p001",
                                    text = "Alice was beginning to get very tired.",
                                ),
                            ),
                        ),
                    ),
                ),
            ),
        ),
    )
}

private class FakeFetcher(
    var responses: MutableMap<String, HttpResult> = mutableMapOf(),
) : HttpFetcher {
    var lastEtag: String? = null

    override fun get(url: String, etag: String?, maxBytes: Long): HttpResult {
        lastEtag = etag
        return responses[url] ?: HttpResult(code = 404)
    }
}

private object Pages {
    fun catalogJson(vararg books: CatalogBook): String = PagelenceJson.encodeToString(
        SourceCatalog.serializer(),
        SourceCatalog(catalogVersion = "2026-11-20", updatedAt = "2026-11-20", books = books.toList()),
    )
}
