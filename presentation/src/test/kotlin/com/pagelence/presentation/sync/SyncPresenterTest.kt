package com.pagelence.presentation.sync

import com.pagelence.sync.SyncProvider
import com.pagelence.uim3.UiLanguage
import com.pagelence.uim3.stringsFor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SyncPresenterTest {
    private val presenter = SyncPresenter(stringsFor(UiLanguage.ZH).syncCopy)

    private fun state(
        configured: Boolean = true,
        success: String? = null,
        failure: String? = null,
        category: String? = null,
    ) = presenter.state(
        configured = configured,
        provider = SyncProvider.CUSTOM,
        url = "https://dav.example.com/pagelence/state.json",
        username = "reader",
        allowHttp = false,
        hasPassword = true,
        lastSuccessAt = success,
        lastFailureAt = failure,
        lastFailureCategory = category,
    )

    @Test
    fun `not configured shows the idle line and a template url`() {
        val ui = state(configured = false)

        assertEquals(false, ui.configured)
        assertTrue(!ui.status.isNullOrBlank())
        assertTrue(!ui.templateUrl.isNullOrBlank())
    }

    @Test
    fun `a successful sync shows an ok line with the time`() {
        val ui = state(success = "2026-11-22T02:00:00Z")

        assertEquals(com.pagelence.uim3.model.SyncTone.OK, ui.tone)
        assertTrue(ui.status?.contains("11-22") == true)
    }

    @Test
    fun `a later failure wins over an older success`() {
        val ui = state(
            success = "2026-11-22T02:00:00Z",
            failure = "2026-11-22T03:00:00Z",
            category = "network",
        )

        assertEquals(com.pagelence.uim3.model.SyncTone.ERROR, ui.tone)
        assertTrue(!ui.status.isNullOrBlank())
    }
}
