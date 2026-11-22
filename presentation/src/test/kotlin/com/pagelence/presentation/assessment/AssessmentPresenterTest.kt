package com.pagelence.presentation.assessment

import com.pagelence.core.assessment.AssessmentMode
import com.pagelence.core.assessment.AssessmentResult
import com.pagelence.core.assessment.AssessmentSkill
import com.pagelence.core.progress.CefrLevel
import com.pagelence.uim3.UiLanguage
import com.pagelence.uim3.stringsFor
import kotlin.test.Test
import kotlin.test.assertEquals

class AssessmentPresenterTest {
    private val presenter = AssessmentPresenter(stringsFor(UiLanguage.ZH))

    private fun result(
        low: CefrLevel = CefrLevel.A2,
        high: CefrLevel = CefrLevel.B1,
        single: Boolean = false,
    ) = AssessmentResult(
        mode = AssessmentMode.QUICK,
        correct = 9,
        total = 12,
        scorePercent = 75,
        bandLow = low,
        bandHigh = high,
        suggested = CefrLevel.A2,
        singleLevel = single,
        skillPercent = mapOf(
            AssessmentSkill.GRAMMAR to 70,
            AssessmentSkill.VOCAB to 80,
            AssessmentSkill.READING to 75,
        ),
    )

    @Test
    fun `a two-level band renders as a range`() {
        val ui = presenter.result(result())

        assertEquals("A2–B1", ui.band)
        assertEquals("A2", ui.suggested)
        assertEquals(75, ui.scorePercent)
    }

    @Test
    fun `a single level has no range dash`() {
        val ui = presenter.result(result(low = CefrLevel.B1, high = CefrLevel.B1, single = true))

        assertEquals("B1", ui.band)
    }

    @Test
    fun `skills keep a stable order and localized names`() {
        val ui = presenter.result(result())

        assertEquals(listOf("词汇", "语法", "阅读"), ui.skills.map { it.label })
        assertEquals(listOf(80, 70, 75), ui.skills.map { it.percent })
    }
}
