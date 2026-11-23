package com.pagelence.presentation.store

import com.pagelence.core.content.FileBuiltInStoreCatalog
import com.pagelence.core.content.FileContentRepository
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
import kotlin.test.assertTrue

class StorePresenterTest {
    private val contentDir: Path = Path.of(
        System.getProperty("pagelence.contentDir") ?: "../content",
    ).toAbsolutePath().normalize()

    private fun presenter(): StorePresenter {
        val content = FileContentRepository(contentDir)
        val dir = Files.createTempDirectory("pagelence-store")
        return StorePresenter(
            content = content,
            sources = SourceManager(
                fetcher = OfflineHttpFetcher(),
                settingsStore = SourceStore(dir.resolve("source-settings.json")),
                downloads = DownloadedBooks(dir.resolve("downloaded")),
                cacheDir = dir.resolve("cache"),
                appVersionCode = 1,
            ),
            catalog = FileBuiltInStoreCatalog(contentDir),
            strings = stringsFor(UiLanguage.ZH),
            language = UiLanguage.ZH,
        )
    }

    @Test
    fun `the built-in shelf lists shipped and planned books`() {
        val books = presenter().builtIn()

        assertEquals(5, books.size)
        val available = books.filter { it.available }
        assertEquals(2, available.size)
        val dolittle = available.first { it.id == "doctor-dolittle-1920" }
        assertEquals(103, dolittle.unitCount)
        assertTrue(dolittle.title.isNotBlank())
        assertTrue(books.any { !it.available }, "planned books stay visible")
    }

    @Test
    fun `an empty catalog still returns a usable store state`() {
        val state = presenter().store(snapshots = emptyList())

        assertTrue(state.books.isEmpty())
        assertTrue(state.builtIn.isNotEmpty())
        assertEquals(false, state.refreshing)
    }

    @Test
    fun `a finished book is marked in the store shelf`() {
        val books = presenter().builtIn(finishedIds = setOf("doctor-dolittle-1920"))

        assertTrue(books.first { it.id == "doctor-dolittle-1920" }.finished)
        assertTrue(books.none { it.id == "alice-1866" && it.finished })
    }
}
