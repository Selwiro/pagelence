package com.pagelence.presentation.eink

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EinkInputTest {
    @Test
    fun rightDownAndPageDownTurnForward() {
        assertEquals(EinkAction.NEXT_PAGE, EinkKeyMap.action(EinkKeyMap.KEY_DPAD_RIGHT))
        assertEquals(EinkAction.NEXT_PAGE, EinkKeyMap.action(EinkKeyMap.KEY_DPAD_DOWN))
        assertEquals(EinkAction.NEXT_PAGE, EinkKeyMap.action(EinkKeyMap.KEY_PAGE_DOWN))
        assertEquals(EinkAction.NEXT_PAGE, EinkKeyMap.action(EinkKeyMap.KEY_VOLUME_DOWN))
    }

    @Test
    fun leftUpAndPageUpTurnBack() {
        assertEquals(EinkAction.PREVIOUS_PAGE, EinkKeyMap.action(EinkKeyMap.KEY_DPAD_LEFT))
        assertEquals(EinkAction.PREVIOUS_PAGE, EinkKeyMap.action(EinkKeyMap.KEY_DPAD_UP))
        assertEquals(EinkAction.PREVIOUS_PAGE, EinkKeyMap.action(EinkKeyMap.KEY_PAGE_UP))
        assertEquals(EinkAction.PREVIOUS_PAGE, EinkKeyMap.action(EinkKeyMap.KEY_VOLUME_UP))
    }

    @Test
    fun centerAndEnterConfirm() {
        assertEquals(EinkAction.CONFIRM, EinkKeyMap.action(EinkKeyMap.KEY_DPAD_CENTER))
        assertEquals(EinkAction.CONFIRM, EinkKeyMap.action(EinkKeyMap.KEY_ENTER))
    }

    @Test
    fun backStaysBack() {
        assertEquals(EinkAction.BACK, EinkKeyMap.action(EinkKeyMap.KEY_BACK))
    }

    @Test
    fun unknownKeysAreIgnored() {
        assertEquals(EinkAction.NONE, EinkKeyMap.action(0))
        assertEquals(EinkAction.NONE, EinkKeyMap.action(1000))
    }

    @Test
    fun onlyPageTurnsAreConsumedWhileReading() {
        assertTrue(EinkKeyMap.isPageTurn(EinkAction.NEXT_PAGE))
        assertTrue(EinkKeyMap.isPageTurn(EinkAction.PREVIOUS_PAGE))
        assertFalse(EinkKeyMap.isPageTurn(EinkAction.CONFIRM))
        assertFalse(EinkKeyMap.isPageTurn(EinkAction.BACK))
        assertFalse(EinkKeyMap.isPageTurn(EinkAction.NONE))
    }
}
