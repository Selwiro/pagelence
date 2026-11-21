package com.pagelence.core.source

import com.pagelence.core.content.Book
import com.pagelence.core.content.ContentIndex
import com.pagelence.core.content.ContentValidator
import com.pagelence.core.content.PagelenceJson
import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText

/**
 * Catalog fetching, line fallback, downloads and installs.
 *
 * Rules:
 * - every endpoint is HTTPS unless the advanced local-HTTP switch is on;
 * - AUTO tries the last good endpoint first, then the other line;
 * - a 304 reuses the cached catalog, an ETag never replaces the checksum;
 * - a package is only installed after its SHA-256 matches the catalog entry and
 *   [BookPackageReader] + [ContentValidator] accept it.
 */
class SourceManager(
    private val fetcher: HttpFetcher,
    private val settingsStore: SourceStore,
    private val downloads: DownloadedBooks,
    private val cacheDir: Path,
    private val appVersionCode: Int,
    private val now: () -> Instant = Instant::now,
    /** Mirrors [DefaultBookSource.AVAILABLE]; tests turn it on to exercise lines. */
    private val defaultSourceEnabled: Boolean = DefaultBookSource.AVAILABLE,
    /**
     * Book ids that ship inside the APK. Importing one of them is refused so a
     * downloaded copy never shadows the built-in book (and its progress).
     */
    private val shippedBookIds: () -> Set<String> = { emptySet() },
) {
    fun settings(): SourceSettings = settingsStore.settings()

    fun updateSettings(transform: (SourceSettings) -> SourceSettings): SourceSettings {
        val next = transform(settingsStore.settings())
        settingsStore.save(next)
        return next
    }

    fun addCustomSource(name: String, url: String): SourceResult<CustomSource> {
        val settings = settingsStore.settings()
        if (!CatalogRules.isAllowedUrl(url, settings.allowLocalHttp)) {
            return SourceResult.Err(SourceError.Insecure(url))
        }
        val trimmed = name.trim().ifBlank { url.substringAfter("://").substringBefore('/') }
        val id = "custom-" + Hashing.sha256("$url@${now()}".toByteArray()).take(12)
        val source = CustomSource(id = id, name = trimmed, catalogUrl = url, addedAt = now().toString())
        settingsStore.save(settings.copy(customSources = settings.customSources + source))
        return SourceResult.Ok(source)
    }

    fun setCustomSourceEnabled(id: String, enabled: Boolean) {
        updateSettings { settings ->
            settings.copy(
                customSources = settings.customSources.map { if (it.id == id) it.copy(enabled = enabled) else it },
            )
        }
    }

    fun removeCustomSource(id: String) {
        updateSettings { settings -> settings.copy(customSources = settings.customSources.filterNot { it.id == id }) }
    }

    fun setLine(line: SourceLine) {
        updateSettings { settings -> settings.copy(line = line) }
    }

    fun setAllowLocalHttp(allowed: Boolean) {
        updateSettings { settings -> settings.copy(allowLocalHttp = allowed) }
    }

    /** Enabled endpoints in preference order: default line(s) first, then custom. */
    fun endpoints(): List<SourceEndpoint> {
        val settings = settingsStore.settings()
        val defaults = if (defaultSourceEnabled) DefaultBookSource.endpoints(settings.line) else emptyList()
        val orderedDefaults = settings.lastGoodEndpoint?.let { last ->
            defaults.sortedBy { if (it.url == last) 0 else 1 }
        } ?: defaults
        val custom = settings.customSources.filter { it.enabled }.map { source ->
            SourceEndpoint(source.id, source.name, source.catalogUrl, SourceLine.DIRECT)
        }
        return orderedDefaults + custom
    }

    fun sourceName(sourceId: String): String {
        if (sourceId == DefaultBookSource.ID) return DefaultBookSource.NAME
        return settingsStore.settings().customSources.firstOrNull { it.id == sourceId }?.name ?: sourceId
    }

    fun refresh(sourceId: String): SourceResult<CatalogSnapshot> {
        val settings = settingsStore.settings()
        val candidates = endpoints().filter { it.sourceId == sourceId }
        if (candidates.isEmpty()) return SourceResult.Err(SourceError.BadCatalog("source $sourceId is disabled"))
        var lastError: SourceError? = null
        for (endpoint in candidates) {
            if (!CatalogRules.isAllowedUrl(endpoint.url, settings.allowLocalHttp)) {
                lastError = SourceError.Insecure(endpoint.url)
                continue
            }
            when (val result = fetchCatalog(endpoint, settings)) {
                is SourceResult.Ok -> return result
                is SourceResult.Err -> lastError = result.error
            }
        }
        return SourceResult.Err(lastError ?: SourceError.Network("no endpoint"))
    }

    /** Refresh every enabled source; one failing source never hides the others. */
    fun refreshAll(): List<Pair<SourceEndpoint, SourceResult<CatalogSnapshot>>> {
        // One endpoint per source id: refresh() already tries every line of that
        // source in AUTO mode, and the default source may be switched off.
        val sources = endpoints().distinctBy { it.sourceId }
        return sources.map { endpoint -> endpoint to refresh(endpoint.sourceId) }
    }

    private fun fetchCatalog(endpoint: SourceEndpoint, settings: SourceSettings): SourceResult<CatalogSnapshot> {
        val cache = cacheFile(endpoint.url)
        val cached = readCache(cache)
        val etag = cached?.etag
        val response = runCatching {
            fetcher.get(endpoint.url, etag, SourceLimits.MAX_CATALOG_BYTES)
        }.getOrElse { error ->
            return SourceResult.Err(SourceError.Network(error.message ?: error::class.simpleName ?: "network error"))
        }
        if (response.code == 304 && cached != null) {
            return SourceResult.Ok(
                CatalogSnapshot(
                    sourceId = endpoint.sourceId,
                    sourceName = endpoint.name,
                    endpoint = endpoint.url,
                    line = endpoint.line,
                    catalog = cached.catalog,
                    fetchedAt = cached.fetchedAt,
                    fromCache = true,
                ),
            )
        }
        if (response.code == 0 || response.code >= 400) {
            return SourceResult.Err(
                if (response.code == 0) SourceError.Network("no response") else SourceError.Http(response.code),
            )
        }
        if (response.body.isEmpty()) return SourceResult.Err(SourceError.BadCatalog("empty catalog"))
        if (response.body.size > SourceLimits.MAX_CATALOG_BYTES) {
            return SourceResult.Err(SourceError.TooLarge(response.body.size.toLong()))
        }
        val catalog = runCatching {
            PagelenceJson.decodeFromString<SourceCatalog>(response.body.toString(Charsets.UTF_8))
        }.getOrElse { return SourceResult.Err(SourceError.BadCatalog("catalog json: ${it.message}")) }

        val errors = CatalogRules.validate(catalog, appVersionCode, settings.allowLocalHttp)
        if (errors.isNotEmpty()) return SourceResult.Err(SourceError.BadCatalog(errors.first()))
        // The catalog's own public-domain list is the pre-download filter; it
        // applies to the default and custom sources alike. The package
        // `licenses` re-checks the source after download.
        val visible = CatalogRules.forRegions(catalog, TARGET_REGIONS)

        val fetchedAt = now().toString()
        writeCache(cache, CachedCatalog(endpoint.url, response.etag ?: etag, fetchedAt, visible))
        settingsStore.save(settings.copy(lastGoodEndpoint = endpoint.url))
        return SourceResult.Ok(
            CatalogSnapshot(
                sourceId = endpoint.sourceId,
                sourceName = endpoint.name,
                endpoint = endpoint.url,
                line = endpoint.line,
                catalog = catalog,
                fetchedAt = fetchedAt,
                fromCache = false,
            ),
        )
    }

    fun download(book: CatalogBook, sourceId: String): SourceResult<InstalledBook> {
        val settings = settingsStore.settings()
        if (!CatalogRules.isAllowedUrl(book.url, settings.allowLocalHttp)) {
            return SourceResult.Err(SourceError.Insecure(book.url))
        }
        if (book.sizeBytes > SourceLimits.MAX_PACKAGE_BYTES) {
            return SourceResult.Err(SourceError.TooLarge(book.sizeBytes))
        }
        val response = runCatching {
            fetcher.get(book.url, null, SourceLimits.MAX_PACKAGE_BYTES)
        }.getOrElse { error ->
            return SourceResult.Err(SourceError.Network(error.message ?: error::class.simpleName ?: "network error"))
        }
        if (response.code == 0 || response.code >= 400) {
            return SourceResult.Err(
                if (response.code == 0) SourceError.Network("no response") else SourceError.Http(response.code),
            )
        }
        if (response.body.size > SourceLimits.MAX_PACKAGE_BYTES) {
            return SourceResult.Err(SourceError.TooLarge(response.body.size.toLong()))
        }
        if (!Hashing.matches(response.body, book.sha256)) {
            return SourceResult.Err(SourceError.Checksum(book.sha256, Hashing.sha256(response.body)))
        }
        val packageFile = when (val read = BookPackageReader.read(response.body, book.id)) {
            is SourceResult.Ok -> read.value
            is SourceResult.Err -> return read
        }
        return install(packageFile, response.body, sourceId, book.level)
    }

    /**
     * Import a package the user picked from local storage.
     *
     * Unlike [download] there is no catalog entry, so the package is its own
     * source of truth: the zip, manifest, book and content are validated before
     * anything is written. [sourceId] records where it came from
     * ([ImportedSource]).
     */
    fun importPackage(bytes: ByteArray, sourceId: String): SourceResult<InstalledBook> {
        if (bytes.isEmpty()) return SourceResult.Err(SourceError.BadPackage("empty file"))
        if (bytes.size > SourceLimits.MAX_PACKAGE_BYTES) {
            return SourceResult.Err(SourceError.TooLarge(bytes.size.toLong()))
        }
        val packageFile = when (val read = BookPackageReader.read(bytes)) {
            is SourceResult.Ok -> read.value
            is SourceResult.Err -> return read
        }
        return install(packageFile, bytes, sourceId, null)
    }

    /**
     * Import a `.pagelence` package from a direct HTTPS link. Only this one file
     * is fetched; the URL is not stored as a book source.
     */
    fun importFromUrl(url: String, sourceId: String = ImportedSource.ONLINE): SourceResult<InstalledBook> {
        val settings = settingsStore.settings()
        if (!CatalogRules.isAllowedUrl(url, settings.allowLocalHttp)) {
            return SourceResult.Err(SourceError.Insecure(url))
        }
        val response = runCatching {
            fetcher.get(url, null, SourceLimits.MAX_PACKAGE_BYTES)
        }.getOrElse { error ->
            return SourceResult.Err(SourceError.Network(error.message ?: error::class.simpleName ?: "network error"))
        }
        if (response.code == 0 || response.code >= 400) {
            return SourceResult.Err(
                if (response.code == 0) SourceError.Network("no response") else SourceError.Http(response.code),
            )
        }
        if (response.body.size > SourceLimits.MAX_PACKAGE_BYTES) {
            return SourceResult.Err(SourceError.TooLarge(response.body.size.toLong()))
        }
        return importPackage(response.body, sourceId)
    }

    /** Validate a package and write it under `downloaded/`. */
    private fun install(
        packageFile: BookPackage,
        bytes: ByteArray,
        sourceId: String,
        level: String?,
    ): SourceResult<InstalledBook> {
        if (packageFile.book.id in shippedBookIds()) {
            return SourceResult.Err(SourceError.AlreadyBuiltIn(packageFile.book.id))
        }
        val report = ContentValidator.validate(packageFile.book)
        if (!report.isValid) return SourceResult.Err(SourceError.BadPackage(report.summary()))

        val stats = ContentIndex(packageFile.book).stats()
        val installed = InstalledBook(
            bookId = packageFile.book.id,
            title = packageFile.book.title,
            sourceId = sourceId,
            catalogVersion = packageFile.manifest.version,
            sha256 = Hashing.sha256(bytes),
            installedAt = now().toString(),
            level = level,
            chapters = stats.chapters,
            units = stats.units,
        )
        return SourceResult.Ok(downloads.install(packageFile, installed))
    }

    fun installed(): List<InstalledBook> = downloads.list()

    fun removeInstalled(bookId: String): Boolean = downloads.remove(bookId)

    fun installedBook(bookId: String): Book? = downloads.book(bookId)

    private companion object {
        /** Target markets for the default source; a book must be PD in one of them. */
        val TARGET_REGIONS = setOf("CN", "US")
    }

    private fun cacheFile(url: String): Path =
        cacheDir.resolve("catalog-${Hashing.sha256(url.toByteArray()).take(16)}.json")

    private fun readCache(file: Path): CachedCatalog? = runCatching {
        if (!file.exists()) null
        else PagelenceJson.decodeFromString<CachedCatalog>(file.readText(Charsets.UTF_8))
    }.getOrNull()

    private fun writeCache(file: Path, value: CachedCatalog) {
        file.parent?.createDirectories()
        file.writeText(PagelenceJson.encodeToString(CachedCatalog.serializer(), value), Charsets.UTF_8)
    }
}
