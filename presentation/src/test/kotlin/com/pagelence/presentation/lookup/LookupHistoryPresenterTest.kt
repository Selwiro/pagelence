package com.pagelence.presentation.lookup

import com.pagelence.core.content.ContentRepository
import com.pagelence.core.content.FileContentRepository
import com.pagelence.core.lookup.LookupEntry
import com.pagelence.core.lookup.LookupHistoryRepository
import com.pagelence.uim3.UiLanguage
import java.nio.file.Path
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LookupHistoryPresenterTest {
    private val contentDir: Path = Path.of(
        System.getProperty("pagelence.contentDir") ?: "../content",
    ).toAbsolutePath().normalize()
    private val content: ContentRepository = FileContentRepository(contentDir)

    private class FakeLookups(private var entries: List<LookupEntry>) : LookupHistoryRepository {
        override fun all(): List<LookupEntry> = entries

        override fun record(entry: LookupEntry): LookupEntry {
            entries = listOf(entry) + entries.filterNot { it.id == entry.id }
            return entry
        }

        override fun clear() {
            entries = emptyList()
        }
    }

    @Test
    fun `installed book resolves its title and unit`() {
        val outline = content.outline("doctor-dolittle-1920")
        val unit = outline?.units?.first()
        val entry = LookupEntry(
            id = "doctor-dolittle-1920::skiff",
            bookId = "doctor-dolittle-1920",
            unitId = unit?.id ?: "missing",
            paragraphId = "p1",
            sentenceNumber = 3,
            term = "skiff",
            lemma = "skiff",
            gloss = "小艇",
            lookedUpAt = "2026-11-22T08:30:00Z",
        )

        val row = LookupHistoryPresenter(
            content = content,
            lookups = FakeLookups(listOf(entry)),
            language = UiLanguage.ZH,
            zone = ZoneOffset.UTC,
        ).list().single()

        assertTrue(row.available)
        assertTrue(row.bookTitle.isNotBlank())
        assertTrue(row.unitLabel.isNotBlank())
        assertEquals("2026-11-22 08:30", row.lookedUpAtLabel)
    }

    @Test
    fun `a removed book stays visible but unavailable`() {
        val entry = LookupEntry(
            id = "gone::skiff",
            bookId = "gone",
            unitId = "u1",
            paragraphId = "p1",
            sentenceNumber = 1,
            term = "skiff",
            lemma = "skiff",
            lookedUpAt = "2026-11-22T08:30:00Z",
        )

        val row = LookupHistoryPresenter(
            content = content,
            lookups = FakeLookups(listOf(entry)),
            language = UiLanguage.ZH,
        ).list().single()

        assertFalse(row.available)
        assertEquals("gone", row.bookTitle)
    }
}
