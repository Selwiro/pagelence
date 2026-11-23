package com.pagelence.presentation.store

import com.pagelence.core.content.ContentRepository
import com.pagelence.core.content.FileBuiltInStoreCatalog
import com.pagelence.core.content.LocalizedText
import com.pagelence.core.content.StoreCatalogRules
import com.pagelence.core.source.CatalogRules
import com.pagelence.core.source.CatalogSnapshot
import com.pagelence.core.source.ImportedSource
import com.pagelence.core.source.SourceError
import com.pagelence.core.source.SourceManager
import com.pagelence.uim3.Strings
import com.pagelence.uim3.UiLanguage
import com.pagelence.uim3.model.BuiltInBookUi
import com.pagelence.uim3.model.InstalledBookUi
import com.pagelence.uim3.model.StoreBookUi
import com.pagelence.uim3.model.StoreSourceUi
import com.pagelence.uim3.model.StoreUiState

/**
 * The 书城 page state: built-in shelf, custom sources, downloaded books and
 * import/update status. Android-free, so an e-ink shell can render the same
 * store without copying the merge rules.
 */
class StorePresenter(
    private val content: ContentRepository,
    private val sources: SourceManager,
    private val catalog: FileBuiltInStoreCatalog,
    private val strings: Strings,
    private val language: UiLanguage,
) {
    fun store(
        snapshots: List<CatalogSnapshot>,
        refreshing: Boolean = false,
        error: String? = null,
        notice: String? = null,
        downloadingId: String? = null,
        importing: Boolean = false,
        finishedIds: Set<String> = emptySet(),
    ): StoreUiState {
        val settings = sources.settings()
        val installed = sources.installed()
        val installedHashes = installed.associate { it.bookId to it.sha256 }
        val installedIds = installedHashes.keys
        val entries = CatalogRules.merge(snapshots)
        val books = entries.map { entry ->
            val book = entry.book
            StoreBookUi(
                id = book.id,
                title = titleFor(book.title),
                coverTitle = book.title.en ?: book.title.zh ?: book.id,
                author = book.author,
                level = book.level,
                description = book.description?.let { textFor(it) },
                sizeLabel = formatBytes(book.sizeBytes),
                sourceNames = entry.sources.map(sources::sourceName),
                sourceIds = entry.sources,
                installed = book.id in installedIds,
                updateAvailable = installedHashes[book.id]?.let { it != book.sha256 } == true,
                downloading = downloadingId == book.id,
                finished = book.id in finishedIds,
            )
        }
        val customSources = settings.customSources.map {
            StoreSourceUi(it.id, it.name, it.catalogUrl, builtIn = false, enabled = it.enabled)
        }
        val snapshot = snapshots.firstOrNull()
        val status = snapshot?.let {
            if (it.fromCache) strings.storeStatusCached.format(it.fetchedAt.take(10))
            else strings.storeStatusUpdated.format(it.fetchedAt.take(10))
        }
        return StoreUiState(
            loading = false,
            refreshing = refreshing,
            allowLocalHttp = settings.allowLocalHttp,
            books = books,
            builtIn = builtIn(finishedIds),
            sources = customSources,
            installed = installed.map {
                InstalledBookUi(
                    id = it.bookId,
                    title = titleFor(it.title),
                    units = it.units,
                    sourceName = when (it.sourceId) {
                        ImportedSource.LOCAL -> strings.storeImportedLocal
                        ImportedSource.ONLINE -> strings.storeImportedOnline
                        else -> sources.sourceName(it.sourceId)
                    },
                )
            },
            downloadingId = downloadingId,
            importing = importing,
            status = status,
            error = error,
            notice = notice,
        )
    }

    /**
     * The planned shelf from `store.json`. A title is readable only when the
     * manifest actually ships it; the rest stay visible as "in production"
     * instead of pretending to be downloadable.
     */
    fun builtIn(finishedIds: Set<String> = emptySet()): List<BuiltInBookUi> {
        val shipped = content.library().associateBy { it.id }
        return catalog.books().map { entry ->
            val summary = shipped[entry.id]
            // Identity comes from the package outline, never from parsing the
            // book body: the store is part of startup.
            val outline = summary?.let { runCatching { content.outline(it.id) }.getOrNull() }
            val title = entry.title ?: outline?.title ?: summary?.title
                ?: LocalizedText(zh = entry.id, en = entry.id)
            val available = entry.status == StoreCatalogRules.STATUS_AVAILABLE && summary != null
            BuiltInBookUi(
                id = entry.id,
                title = titleFor(title),
                coverTitle = title.en ?: title.zh ?: entry.id,
                monogram = entry.monogram
                    ?: title.zh.orEmpty().ifBlank { title.en.orEmpty() }.ifBlank { entry.id }.take(1),
                author = entry.author ?: outline?.author,
                level = entry.level ?: summary?.level,
                description = (entry.summary ?: outline?.description)?.let { textFor(it) },
                unitCount = summary?.units ?: 0,
                available = available,
                order = entry.order,
                status = entry.status,
                coverPath = content.coverPath(entry.id),
                finished = entry.id in finishedIds,
            )
        }
    }

    private fun titleFor(title: LocalizedText): String = when (language) {
        UiLanguage.ZH -> title.zh ?: title.en ?: ""
        UiLanguage.EN -> title.en ?: title.zh ?: ""
    }

    private fun textFor(text: LocalizedText): String? = when (language) {
        UiLanguage.ZH -> text.zh ?: text.en
        UiLanguage.EN -> text.en ?: text.zh
    }?.takeIf { it.isNotBlank() }
}

/** Prefer a specific content error over a transport one when listing failures. */
fun storeErrorPriority(error: SourceError): Int = when (error) {
    is SourceError.AlreadyBuiltIn -> 0
    is SourceError.BadCatalog -> 0
    is SourceError.BadPackage -> 1
    is SourceError.UnreadableFile -> 2
    is SourceError.Checksum -> 2
    is SourceError.TooLarge -> 3
    is SourceError.Insecure -> 4
    is SourceError.Http -> 5
    is SourceError.Network -> 6
    is SourceError.NotInstalled -> 7
}

/** One localized line for an import/download failure. */
fun storeErrorText(strings: Strings, error: SourceError): String = when (error) {
    is SourceError.Network -> strings.storeErrorNetwork
    is SourceError.Http -> strings.storeErrorHttp.format(error.code)
    is SourceError.Insecure -> strings.storeErrorHttps
    is SourceError.TooLarge -> strings.storeErrorTooLarge
    is SourceError.BadCatalog -> strings.storeErrorBadCatalog.format(error.detail)
    is SourceError.BadPackage -> strings.storeErrorBadPackage.format(error.detail)
    is SourceError.Checksum -> strings.storeErrorChecksum
    is SourceError.NotInstalled -> strings.storeErrorBadPackage.format(error.detail)
    is SourceError.AlreadyBuiltIn -> strings.storeErrorBuiltIn
    is SourceError.UnreadableFile -> strings.storeErrorFile
}

/** `1.2 MB` / `340 KB` / `12 B`; sizes are shown in source cards and the dictionary row. */
fun formatBytes(bytes: Long): String = when {
    bytes >= 1024 * 1024 -> String.format("%.1f MB", bytes / 1024.0 / 1024.0)
    bytes >= 1024 -> String.format("%d KB", bytes / 1024)
    else -> "$bytes B"
}
