package com.pagelence.presentation.grammar

import com.pagelence.core.content.FileContentRepository
import com.pagelence.uim3.UiLanguage
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GrammarPresenterTest {
    private val contentDir: Path = Path.of(
        System.getProperty("pagelence.contentDir") ?: "../content",
    ).toAbsolutePath().normalize()

    private val points = FileContentRepository(contentDir).grammarPoints()
    private val presenter = GrammarPresenter(points, UiLanguage.ZH)

    @Test
    fun `the tab lists every library point with names`() {
        val tab = presenter.tab()

        assertEquals(256, tab.points.size)
        assertEquals(false, tab.loading)
        assertTrue(tab.points.all { it.name.isNotBlank() })
        assertTrue(tab.points.all { it.levelLabel.isNotBlank() })
    }

    @Test
    fun `a middle point has previous and next links`() {
        val ui = presenter.point(points[1])

        assertEquals(points[0].id, ui.previousId)
        assertEquals(points[2].id, ui.nextId)
    }

    @Test
    fun `the first point has no previous link`() {
        val ui = presenter.point(points.first())

        assertNull(ui.previousId)
        assertNotNull(ui.nextId)
    }
}
