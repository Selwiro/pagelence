package com.pagelence.core.source

import com.pagelence.core.content.Book
import com.pagelence.core.content.LocalizedText
import java.security.MessageDigest
import kotlinx.serialization.Serializable

/**
 * Book-source models: the static catalog, the package layout and the user's
 * source settings. Networking lives behind [HttpFetcher], so everything here is
 * testable without a device.
 *
 * See `work/android/docs/book-sources.md` for the format and policy.
 */

@Serializable
data class CatalogBook(
    val id: String,
    val title: LocalizedText,
    val author: String? = null,
    val level: String? = null,
    val language: String = "en",
    val description: LocalizedText? = null,
    val sizeBytes: Long = 0,
    val sha256: String,
    val url: String,
    /**
     * `public-domain`, `permission`, `original` or an SPDX id. The package
     * manifest's `licenses` is authoritative after install; the catalog value
     * is only a pre-download filter and display hint.
     */
    val sourceType: String? = null,
    /** ISO 3166-1 alpha-2 regions where the text is public domain. */
    val publicDomain: List<String> = emptyList(),
    /** SPDX id, `custom` or `all-rights-reserved`. */
    val creatorType: String? = null,
    val creatorName: String? = null,
    val creatorUrl: String? = null,
    val minAppVersionCode: Int = 0,
)

@Serializable
data class SourceCatalog(
    val schemaVersion: Int = CATALOG_SCHEMA_VERSION,
    /** Catalog release/date, not the package semver. */
    val catalogVersion: String? = null,
    val updatedAt: String? = null,
    val books: List<CatalogBook> = emptyList(),
)

const val CATALOG_SCHEMA_VERSION = 2

/** Direct GitHub, a project mirror, or the automatic direct→mirror fallback. */
@Serializable
enum class SourceLine { AUTO, DIRECT, MIRROR }

/** A catalog endpoint the app may fetch. */
@Serializable
data class SourceEndpoint(
    val sourceId: String,
    val name: String,
    val url: String,
    val line: SourceLine = SourceLine.DIRECT,
)

@Serializable
data class CustomSource(
    val id: String,
    val name: String,
    val catalogUrl: String,
    val enabled: Boolean = true,
    val addedAt: String? = null,
)

@Serializable
data class SourceSettings(
    val line: SourceLine = SourceLine.AUTO,
    val customSources: List<CustomSource> = emptyList(),
    /** Advanced switch: allow plain HTTP for a LAN test server, with a warning. */
    val allowLocalHttp: Boolean = false,
    /** Endpoint that last worked, so AUTO starts there next time. */
    val lastGoodEndpoint: String? = null,
)

/** The project-maintained source; URLs are placeholders until hosting exists. */
object DefaultBookSource {
    const val ID = "default"
    const val NAME = "Pagelence 公版书源"
    const val DIRECT_URL = "https://raw.githubusercontent.com/pagelence/books/main/catalog.json"
    const val MIRROR_URL = "https://cdn.jsdelivr.net/gh/pagelence/books@main/catalog.json"

    /**
     * The hosted catalog does not exist yet, so the app neither lists nor calls
     * the default source. Flip this to true once the catalog and packages are
     * live; the store UI and the source rules already support it.
     */
    const val AVAILABLE = false

    fun endpoints(line: SourceLine): List<SourceEndpoint> = when (line) {
        SourceLine.DIRECT -> listOf(SourceEndpoint(ID, NAME, DIRECT_URL, SourceLine.DIRECT))
        SourceLine.MIRROR -> listOf(SourceEndpoint(ID, NAME, MIRROR_URL, SourceLine.MIRROR))
        SourceLine.AUTO -> listOf(
            SourceEndpoint(ID, NAME, DIRECT_URL, SourceLine.DIRECT),
            SourceEndpoint(ID, NAME, MIRROR_URL, SourceLine.MIRROR),
        )
    }
}

/** Limits applied to every download; a source cannot make the app fetch more. */
object SourceLimits {
    const val MAX_CATALOG_BYTES = 512L * 1024
    const val MAX_CATALOG_BOOKS = 200
    const val MAX_PACKAGE_BYTES = 8L * 1024 * 1024
    const val MAX_ENTRIES = 8
    const val MAX_ENTRY_BYTES = 6L * 1024 * 1024
    const val MAX_UNCOMPRESSED_BYTES = 12L * 1024 * 1024
    const val MAX_REDIRECTS = 3
    const val CONNECT_TIMEOUT_MS = 10_000
    const val READ_TIMEOUT_MS = 20_000
    const val MIN_APP_VERSION = 1
}

/** One source's catalog after a successful refresh. */
data class CatalogSnapshot(
    val sourceId: String,
    val sourceName: String,
    val endpoint: String,
    val line: SourceLine,
    val catalog: SourceCatalog,
    val fetchedAt: String,
    val fromCache: Boolean,
)

/** A book as offered by one or more sources. */
data class CatalogEntry(
    val book: CatalogBook,
    val sources: List<String>,
)

/** Failure taxonomy; the UI turns each into one readable sentence. */
sealed class SourceError(val detail: String) {
    class Network(detail: String) : SourceError(detail)
    class Http(val code: Int) : SourceError("HTTP $code")
    class Insecure(url: String) : SourceError("not HTTPS: $url")
    class TooLarge(bytes: Long) : SourceError("too large: $bytes bytes")
    class BadCatalog(reason: String) : SourceError(reason)
    class BadPackage(reason: String) : SourceError(reason)
    class Checksum(expected: String, actual: String) : SourceError("sha256 $actual, want $expected")
    class NotInstalled(bookId: String) : SourceError("not installed: $bookId")

    /** A package whose book id belongs to a book that ships inside the APK. */
    class AlreadyBuiltIn(bookId: String) : SourceError("already built in: $bookId")

    /** The chosen document could not be opened or read on this device. */
    class UnreadableFile(detail: String) : SourceError(detail)
}

/** Where a one-off imported package came from; not a reusable book source. */
object ImportedSource {
    const val LOCAL = "import:local"
    const val ONLINE = "import:online"
}

sealed class SourceResult<out T> {
    data class Ok<T>(val value: T) : SourceResult<T>()
    data class Err(val error: SourceError) : SourceResult<Nothing>()
}

inline fun <T, R> SourceResult<T>.map(transform: (T) -> R): SourceResult<R> = when (this) {
    is SourceResult.Ok -> SourceResult.Ok(transform(value))
    is SourceResult.Err -> this
}

/** Catalog validation: shape, limits, HTTPS and app-version compatibility. */
object CatalogRules {
    private val CEFR_LEVELS = setOf("A1", "A2", "B1", "B2", "C1", "C2")
    fun validate(
        catalog: SourceCatalog,
        appVersionCode: Int = Int.MAX_VALUE,
        allowLocalHttp: Boolean = false,
    ): List<String> {
        val errors = mutableListOf<String>()
        if (catalog.schemaVersion != CATALOG_SCHEMA_VERSION) {
            errors += "unsupported catalog schemaVersion ${catalog.schemaVersion}"
        }
        if (catalog.catalogVersion.isNullOrBlank()) {
            errors += "catalogVersion is required"
        }
        if (catalog.books.size > SourceLimits.MAX_CATALOG_BOOKS) {
            errors += "catalog has ${catalog.books.size} books, limit ${SourceLimits.MAX_CATALOG_BOOKS}"
        }
        val seen = mutableSetOf<String>()
        catalog.books.forEach { book ->
            val where = "book ${book.id.ifBlank { "<no id>" }}"
            if (book.id.isBlank()) errors += "$where: id is empty"
            if (!seen.add(book.id)) errors += "$where: duplicate id"
            if (book.title.isBlank) errors += "$where: title is empty"
            if (book.author.isNullOrBlank()) errors += "$where: author is required"
            if (book.level == null || book.level !in CEFR_LEVELS) {
                errors += "$where: level ${book.level} must be A1–C2"
            }
            if (book.description == null || book.description.isBlank) {
                errors += "$where: description is required"
            }
            if (book.url.isBlank()) errors += "$where: url is empty"
            if (!isAllowedUrl(book.url, allowLocalHttp)) errors += "$where: url must be HTTPS"
            if (!book.sha256.matches(Regex("^[0-9a-f]{64}$"))) errors += "$where: sha256 must be 64 hex chars"
            if (book.sizeBytes <= 0) errors += "$where: sizeBytes must be positive"
            if (book.sizeBytes > SourceLimits.MAX_PACKAGE_BYTES) errors += "$where: sizeBytes exceeds the limit"
            if (book.minAppVersionCode > appVersionCode) {
                errors += "$where: needs app version ${book.minAppVersionCode}"
            }
            if (book.language != "en") errors += "$where: only English books are supported"
            errors += licenseErrors(book, where)
        }
        return errors
    }

    private fun licenseErrors(book: CatalogBook, where: String): List<String> {
        val errors = mutableListOf<String>()
        val sourceType = book.sourceType.orEmpty()
        if (sourceType.isBlank()) {
            errors += "$where: sourceType is required"
        } else if (sourceType == "public-domain") {
            if (book.publicDomain.isEmpty()) {
                errors += "$where: public-domain needs a non-empty publicDomain list"
            }
            book.publicDomain.forEach { region ->
                if (!region.matches(Regex("^[A-Z]{2}$"))) {
                    errors += "$where: publicDomain $region must be ISO 3166-1 alpha-2"
                }
            }
        } else if (sourceType !in setOf("permission", "original") &&
            !sourceType.matches(Regex("^[A-Za-z0-9][A-Za-z0-9.+-]*$"))
        ) {
            errors += "$where: sourceType $sourceType is not a licence id"
        }
        val creatorType = book.creatorType.orEmpty()
        if (creatorType.isBlank()) {
            errors += "$where: creatorType is required"
        } else if (creatorType != "custom" && creatorType != "all-rights-reserved" &&
            !creatorType.matches(Regex("^[A-Za-z0-9][A-Za-z0-9.+-]*$"))
        ) {
            errors += "$where: creatorType $creatorType is not a licence id"
        }
        if (creatorType == "custom" || creatorType == "all-rights-reserved") {
            if (book.creatorName.isNullOrBlank()) errors += "$where: creatorName is required"
        }
        book.creatorUrl?.takeIf { it.isNotBlank() }?.let { url ->
            if (!isAllowedUrl(url, allowLocalHttp = false)) {
                errors += "$where: creatorUrl must be HTTPS"
            }
        }
        return errors
    }

    /**
     * Apply the catalog's own pre-download filter. A `public-domain` book must
     * intersect the target regions; `permission`, `original` and SPDX books are
     * shown as published. The package `licenses` remains authoritative after
     * download and re-checks the source region.
     */
    fun forRegions(catalog: SourceCatalog, regions: Set<String>): SourceCatalog {
        if (regions.isEmpty()) return catalog
        return catalog.copy(
            books = catalog.books.filter { book ->
                book.sourceType != "public-domain" || book.publicDomain.any { it in regions }
            },
        )
    }

    fun isAllowedUrl(url: String, allowLocalHttp: Boolean): Boolean {
        val lower = url.lowercase()
        if (lower.startsWith("https://")) return true
        if (!allowLocalHttp) return false
        val host = lower.removePrefix("http://").substringBefore('/').substringBefore(':')
        return host == "localhost" || host == "10.0.2.2" || host.startsWith("127.")
    }

    /** Merge catalogs by book id; the same book from two sources is one entry. */
    fun merge(snapshots: List<CatalogSnapshot>): List<CatalogEntry> {
        val merged = LinkedHashMap<String, CatalogEntry>()
        for (snapshot in snapshots) {
            for (book in snapshot.catalog.books) {
                val existing = merged[book.id]
                merged[book.id] = if (existing == null) {
                    CatalogEntry(book, listOf(snapshot.sourceId))
                } else {
                    existing.copy(sources = (existing.sources + snapshot.sourceId).distinct())
                }
            }
        }
        return merged.values.toList()
    }
}

/** SHA-256 helpers shared by downloads and package checks. */
object Hashing {
    fun sha256(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }

    fun matches(bytes: ByteArray, expected: String): Boolean =
        sha256(bytes).equals(expected.lowercase())
}

/** One book package as stored on disk, described by `installed.json`. */
@Serializable
data class InstalledBook(
    val bookId: String,
    val title: LocalizedText,
    val sourceId: String,
    val catalogVersion: String? = null,
    val sha256: String,
    val installedAt: String,
    val level: String? = null,
    val chapters: Int = 0,
    val units: Int = 0,
)

/** `manifest.json` inside a `.pagelence` package. */
@Serializable
data class PackageManifest(
    val schemaVersion: Int = PACKAGE_SCHEMA_VERSION,
    val bookId: String,
    val title: LocalizedText,
    val version: String? = null,
    val license: String? = null,
    val note: String? = null,
)

const val PACKAGE_SCHEMA_VERSION = 1

@Serializable
data class BookPackage(
    val manifest: PackageManifest,
    val book: Book,
    val license: String? = null,
)
