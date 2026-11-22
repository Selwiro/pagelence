package com.pagelence.presentation.book

import com.pagelence.core.content.ContentRepository
import com.pagelence.core.content.FileBuiltInStoreCatalog
import com.pagelence.core.content.LocalizedText
import com.pagelence.core.prefs.Preferences
import com.pagelence.core.content.BookLicense
import com.pagelence.core.progress.ProgressRepository
import com.pagelence.core.progress.ProgressSemantics
import com.pagelence.core.source.CatalogRules
import com.pagelence.core.source.CatalogSnapshot
import com.pagelence.core.source.ImportedSource
import com.pagelence.core.source.SourceManager
import com.pagelence.presentation.store.formatBytes
import com.pagelence.uim3.Strings
import com.pagelence.uim3.UiLanguage
import com.pagelence.uim3.model.BookChapterUi
import com.pagelence.uim3.model.BookDetailUi
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * The book-detail page: package facts, chapters, licence lines, shelf and
 * download state. Android-free so both shells show the same page.
 */
class BookDetailPresenter(
    private val content: ContentRepository,
    private val sources: SourceManager,
    private val catalog: FileBuiltInStoreCatalog,
    private val progressRepo: ProgressRepository,
    private val preferences: Preferences,
    private val snapshots: List<CatalogSnapshot>,
    private val downloadingId: String?,
    private val strings: Strings,
    private val language: UiLanguage,
) {
    private fun titleFor(title: LocalizedText): String = when (language) {
        UiLanguage.ZH -> title.zh ?: title.en ?: ""
        UiLanguage.EN -> title.en ?: title.zh ?: ""
    }

    private fun textFor(text: LocalizedText?): String? = when (language) {
        UiLanguage.ZH -> text?.zh ?: text?.en
        UiLanguage.EN -> text?.en ?: text?.zh
    }?.takeIf { it.isNotBlank() }

    private fun formatDate(value: String?): String? {
        val instant = value?.let { runCatching { Instant.parse(it) }.getOrNull() } ?: return null
        return DateTimeFormatter.ofPattern("yyyy-MM-dd")
            .withZone(ZoneId.systemDefault())
            .format(instant)
    }

    fun detail(bookId: String): BookDetailUi? {
        val summary = content.library().firstOrNull { it.id == bookId }
        val entry = catalog.books().firstOrNull { it.id == bookId }
        val catalog = CatalogRules.merge(snapshots).firstOrNull { it.book.id == bookId }
        val installed = sources.installed().firstOrNull { it.bookId == bookId }
        if (summary == null && entry == null && catalog == null) return null
        val book = summary?.let { content.book(bookId) }
        val title = entry?.title ?: book?.title ?: catalog?.book?.title
            ?: LocalizedText(zh = bookId, en = bookId)
        val index = content.index(bookId)
        val prefs = preferences
        val chapters = book?.chapters.orEmpty().map { chapter ->
            BookChapterUi(
                id = chapter.id,
                number = chapter.number,
                title = textFor(chapter.title ?: LocalizedText()) ?: "",
                unitCount = chapter.units.size,
            )
        }
        val monogram = entry?.monogram
            ?: title.zh.orEmpty().ifBlank { title.en.orEmpty() }.ifBlank { bookId }.take(1)
        val catalogBook = catalog?.book
        val strings = strings
        val savedProgress = progressRepo.progress(bookId)
        val progress = index?.let { ProgressSemantics.restore(savedProgress, it) } ?: savedProgress
        return BookDetailUi(
            bookId = bookId,
            title = titleFor(title),
            coverTitle = title.en ?: title.zh ?: bookId,
            monogram = monogram,
            author = entry?.author ?: book?.author ?: catalogBook?.author,
            level = summary?.level ?: entry?.level ?: catalogBook?.level ?: installed?.level,
            wordCount = summary?.words ?: 0,
            category = (summary?.category ?: entry?.category)?.let { textFor(it) },
            tags = (summary?.tags ?: entry?.tags).orEmpty().mapNotNull { textFor(it) },
            description = (book?.description ?: entry?.summary ?: catalogBook?.description)
                ?.let { textFor(it) },
            creator = summary?.creator ?: catalogBook?.creatorName,
            chapters = chapters,
            unitCount = chapters.sumOf { it.unitCount },
            available = book != null,
            builtIn = entry != null,
            downloadable = catalog != null,
            downloading = downloadingId == bookId,
            updateAvailable = installed != null && catalogBook != null &&
                installed.sha256 != catalogBook.sha256,
            sizeLabel = catalogBook?.sizeBytes?.takeIf { it > 0 }?.let { formatBytes(it) },
            sourceNames = catalog?.sources?.map { sourceLabel(it) }
                ?: installed?.let { listOf(sourceLabel(it.sourceId)) }
                ?: emptyList(),
            sourceIds = catalog?.sources.orEmpty(),
            publisher = book?.source?.publisher,
            edition = book?.edition,
            sourceLicense = licenseLabel(summary?.licenses?.source),
            sourceLicenseNote = summary?.licenses?.source?.note
                ?.takeIf { language == UiLanguage.ZH },
            creatorLicense = summary?.licenses?.creator?.let { it.name ?: it.type },
            onShelf = bookId in prefs.shelfBookIds,
            readUnits = index?.let { ProgressSemantics.readUnitCount(progress, it) } ?: 0,
            finished = index != null && index.units.isNotEmpty() &&
                ProgressSemantics.isComplete(progress, index),
            finishedOn = formatDate(progress.finishedAt),
            coverPath = content.coverPath(bookId),
        )
    }

    /** Reader-facing name of one licence side, e.g.「公有领域（CN、US）」. */

    private fun licenseLabel(license: BookLicense?): String? {
        if (license == null) return null
        val label = when (license.type) {
            "public-domain" -> if (language == UiLanguage.ZH) "公有领域" else "Public domain"
            "permission" -> if (language == UiLanguage.ZH) "授权使用" else "By permission"
            "original" -> if (language == UiLanguage.ZH) "原创" else "Original"
            else -> license.name ?: license.type
        } ?: return null
        if (license.publicDomain.isEmpty()) return label
        val regions = license.publicDomain.joinToString(
            if (language == UiLanguage.ZH) "、" else ", ",
        )
        return if (language == UiLanguage.ZH) "$label（$regions）" else "$label ($regions)"
    }

    /** Imported one-off packages are not reusable sources; name them for the reader. */

    private fun sourceLabel(sourceId: String): String = when (sourceId) {
        ImportedSource.LOCAL -> strings.storeImportedLocal
        ImportedSource.ONLINE -> strings.storeImportedOnline
        else -> sources.sourceName(sourceId)
    }

    /** Rebuild the open detail page after a download, removal or store refresh. */
}
