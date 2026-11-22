package com.pagelence.presentation.eink

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EinkRefreshTest {
    private val settings = EinkRefreshSettings(
        fullRefreshOnUnitChange = true,
        fullRefreshEveryPages = 3,
        fullRefreshEverySeconds = 60,
    )

    @Test
    fun firstPagesStayLocal() {
        var state = EinkRefreshState.opened(nowMillis = 0)
        val first = EinkRefreshRules.onPageTurn(state, settings, nowMillis = 1_000)
        assertFalse(first.fullRefresh)
        state = first.state
        val second = EinkRefreshRules.onPageTurn(state, settings, nowMillis = 2_000)
        assertFalse(second.fullRefresh)
        assertEquals(2, second.state.pagesSinceFullRefresh)
    }

    @Test
    fun everyNthPageRunsAFullRefresh() {
        var state = EinkRefreshState(pagesSinceFullRefresh = 2, lastFullRefreshAtMillis = 0)
        val third = EinkRefreshRules.onPageTurn(state, settings, nowMillis = 3_000)
        assertTrue(third.fullRefresh)
        assertEquals(0, third.state.pagesSinceFullRefresh)
        assertEquals(3_000, third.state.lastFullRefreshAtMillis)
        state = third.state
        assertFalse(EinkRefreshRules.onPageTurn(state, settings, nowMillis = 3_500).fullRefresh)
    }

    @Test
    fun elapsedTimeAlsoTriggers() {
        val state = EinkRefreshState(pagesSinceFullRefresh = 0, lastFullRefreshAtMillis = 1_000)
        assertFalse(EinkRefreshRules.onPageTurn(state, settings, nowMillis = 30_000).fullRefresh)
        assertTrue(EinkRefreshRules.onPageTurn(state, settings, nowMillis = 61_000).fullRefresh)
    }

    @Test
    fun unitChangeRefreshesAndResetsCounters() {
        val state = EinkRefreshState(pagesSinceFullRefresh = 5, lastFullRefreshAtMillis = 1_000)
        val decision = EinkRefreshRules.onUnitChange(state, settings, nowMillis = 9_000)
        assertTrue(decision.fullRefresh)
        assertEquals(0, decision.state.pagesSinceFullRefresh)
        assertEquals(9_000, decision.state.lastFullRefreshAtMillis)
    }

    @Test
    fun thresholdsAtZeroDisableThatRule() {
        val off = EinkRefreshSettings(
            fullRefreshOnUnitChange = false,
            fullRefreshEveryPages = 0,
            fullRefreshEverySeconds = 0,
        )
        var state = EinkRefreshState.opened(nowMillis = 0)
        repeat(50) { index ->
            val decision = EinkRefreshRules.onPageTurn(state, off, nowMillis = index * 10_000L)
            assertFalse(decision.fullRefresh)
            state = decision.state
        }
        assertFalse(EinkRefreshRules.onUnitChange(state, off, nowMillis = 999_999).fullRefresh)
        assertEquals(50, state.pagesSinceFullRefresh)
    }

    @Test
    fun openedStartsFromNowSoTheFirstTurnIsCheap() {
        val state = EinkRefreshState.opened(nowMillis = 100_000)
        assertEquals(0, state.pagesSinceFullRefresh)
        assertFalse(EinkRefreshRules.onPageTurn(state, settings, nowMillis = 101_000).fullRefresh)
    }

    @Test
    fun unitChangeCanBeLocalButStillCountsThePage() {
        val localOnly = settings.copy(fullRefreshOnUnitChange = false)
        val decision = EinkRefreshRules.onUnitChange(
            EinkRefreshState(pagesSinceFullRefresh = 1, lastFullRefreshAtMillis = 0),
            localOnly,
            nowMillis = 5_000,
        )
        assertFalse(decision.fullRefresh)
        assertEquals(2, decision.state.pagesSinceFullRefresh)
    }
}

class EinkRefreshPresetsTest {
    @Test
    fun presetsAreOrderedFromLightToStrong() {
        val presets = EinkRefreshPresets.all
        assertEquals(3, presets.size)
        assertTrue(presets[0].fullRefreshEveryPages > presets[1].fullRefreshEveryPages)
        assertTrue(presets[1].fullRefreshEveryPages > presets[2].fullRefreshEveryPages)
    }

    @Test
    fun indexFallsBackToStandard() {
        assertEquals(0, EinkRefreshPresets.indexOf(EinkRefreshPresets.Light))
        assertEquals(1, EinkRefreshPresets.indexOf(EinkRefreshSettings.Default))
        assertEquals(2, EinkRefreshPresets.indexOf(EinkRefreshPresets.Strong))
        assertEquals(1, EinkRefreshPresets.indexOf(EinkRefreshSettings(fullRefreshEveryPages = 999)))
    }
}
