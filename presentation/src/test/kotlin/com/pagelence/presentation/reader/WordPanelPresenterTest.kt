package com.pagelence.presentation.reader

import com.pagelence.core.content.FileContentRepository
import com.pagelence.uim3.UiLanguage
import com.pagelence.uim3.stringsFor
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class WordPanelPresenterTest {
    private val contentDir: Path = Path.of(
        System.getProperty("pagelence.contentDir") ?: "../content",
    ).toAbsolutePath().normalize()

    private val bookId = "doctor-dolittle-1920"
    private val content = FileContentRepository(contentDir)
    private val index = assertNotNull(content.index(bookId))
    private val presenter = WordPanelPresenter(
        content = content,
        strings = stringsFor(UiLanguage.ZH),
        language = UiLanguage.ZH,
    )

    @Test
    fun `tapping a word returns its entry and selection range`() {
        val unit = index.units.first()
        val paragraph = unit.paragraphs.first()
        val sentence = index.sentences(paragraph).first()
        val start = sentence.text.indexOf("years")
        assertNotNull(start, "fixture sentence contains the tapped word")

        val result = assertNotNull(
            presenter.open(
                index = index,
                bookId = bookId,
                unitId = unit.id,
                paragraphId = paragraph.id,
                sentenceNumber = 1,
                spelling = "years",
                start = start,
                end = start + "years".length - 1,
            ),
        )

        assertEquals(paragraph.id, result.panel.paragraphId)
        assertEquals(1, result.panel.sentenceNumber)
        assertEquals("years", result.selected.spelling)
        assertNotNull(result.panel.term)
    }

    @Test
    fun `a tap inside a multi-word hint opens the whole phrase`() {
        val unit = index.units.first()
        val paragraph = unit.paragraphs.first { p ->
            p.hints.any { it.kind != "sentence" && it.target.contains(" ") }
        }
        val hint = paragraph.hints.first { it.kind != "sentence" && it.target.contains(" ") }
        // Hint.sentence and the reader's sentence numbers are unit-wide, not
        // paragraph-local: count across the unit until the target paragraph.
        var number = 0
        var tappedNumber = -1
        var tappedStart = -1
        for (pg in unit.paragraphs) {
            for (sentence in index.sentences(pg)) {
                number += 1
                if (pg.id == paragraph.id && tappedNumber < 0) {
                    val at = sentence.text.indexOf(hint.target, ignoreCase = true)
                    if (at >= 0) {
                        tappedNumber = number
                        tappedStart = at
                    }
                }
            }
        }
        val sentenceNumber = hint.sentence ?: tappedNumber
        assertNotNull(tappedStart, "the hint target occurs in its paragraph")

        val result = assertNotNull(
            presenter.open(
                index = index,
                bookId = bookId,
                unitId = unit.id,
                paragraphId = paragraph.id,
                sentenceNumber = sentenceNumber,
                spelling = hint.target.split(" ").first(),
                start = tappedStart,
                end = tappedStart + 1,
            ),
        )

        assertEquals(hint.target, result.panel.term)
        assertEquals(hint.id, result.panel.hintId)
    }
}
