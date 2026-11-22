package com.pagelence.presentation.book

import com.pagelence.core.content.FileBuiltInStoreCatalog
import com.pagelence.core.content.FileContentRepository
import com.pagelence.core.prefs.Preferences
import com.pagelence.core.progress.InMemoryProgressRepository
import com.pagelence.core.source.DownloadedBooks
import com.pagelence.core.source.OfflineHttpFetcher
import com.pagelence.core.source.SourceManager
import com.pagelence.core.source.SourceStore
import com.pagelence.uim3.UiLanguage
import com.pagelence.uim3.stringsFor
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BookDetailPresenterTest {
    private val contentDir: Path = Path.of(
        System.getProperty("pagelence.contentDir") ?: "../content",
    ).toAbsolutePath().normalize()

    private fun presenter(prefs: Preferences = Preferences()): BookDetailPresenter {
        val content = FileContentRepository(contentDir)
        val dir = Files.createTempDirectory("pagelence-detail")
        return BookDetailPresenter(
            content = content,
            sources = SourceManager(
                fetcher = OfflineHttpFetcher(),
                settingsStore = SourceStore(dir.resolve("source-settings.json")),
                downloads = DownloadedBooks(dir.resolve("downloaded")),
                cacheDir = dir.resolve("cache"),
                appVersionCode = 1,
            ),
            catalog = FileBuiltInStoreCatalog(contentDir),
            progressRepo = InMemoryProgressRepository(),
            preferences = prefs,
            snapshots = emptyList(),
            downloadingId = null,
            strings = stringsFor(UiLanguage.ZH),
            language = UiLanguage.ZH,
        )
    }

    @Test
    fun `a shipped book has chapters units and licence lines`() {
        val detail = assertNotNull(presenter().detail("doctor-dolittle-1920"))

        assertEquals("怪医杜立德", detail.title)
        assertEquals(21, detail.chapters.size)
        assertEquals(103, detail.unitCount)
        assertTrue(detail.available)
        assertTrue(detail.builtIn)
        assertNotNull(detail.sourceLicense)
        assertEquals(false, detail.onShelf)
    }

    @Test
    fun `a shelf book is marked as on the shelf`() {
        val prefs = Preferences(shelfBookIds = listOf("alice-1866"))

        val detail = assertNotNull(presenter(prefs).detail("alice-1866"))

        assertTrue(detail.onShelf)
    }

    @Test
    fun `an unknown book has no detail page`() {
        assertNull(presenter().detail("does-not-exist"))
    }
}
