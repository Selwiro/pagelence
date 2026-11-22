package com.pagelence.presentation.reader

import com.pagelence.core.content.FileContentRepository
import com.pagelence.core.wordcards.InMemoryWordCardRepository
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class PreviewPresenterTest {
    private val contentDir: Path = Path.of(
        System.getProperty("pagelence.contentDir") ?: "../content",
    ).toAbsolutePath().normalize()

    private val content = FileContentRepository(contentDir)
    private val index = assertNotNull(content.index("doctor-dolittle-1920"))

    private fun presenter() = PreviewPresenter(content, InMemoryWordCardRepository())

    @Test
    fun `a lower-level reader gets words to preview`() {
        val words = presenter().words(index, index.units.first(), "A1")

        assertTrue(words.isNotEmpty())
        assertTrue(words.all { it.term.isNotBlank() && it.level.isNotBlank() })
        assertEquals(words.size, words.map { it.lemma }.distinct().size, "one row per lemma")
    }

    @Test
    fun `an unknown level yields no preview`() {
        assertTrue(presenter().words(index, index.units.first(), null).isEmpty())
    }
}
