package com.pagelence.presentation.dictionary

import com.pagelence.core.dict.DictManifest
import com.pagelence.core.dict.DictStream
import com.pagelence.core.dict.DictTransport
import com.pagelence.core.dict.FullDictInstaller
import com.pagelence.uim3.UiLanguage
import com.pagelence.uim3.model.DictionaryPhase
import com.pagelence.uim3.stringsFor
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DictionaryPresenterTest {
    private val manifest = DictManifest(
        dictVersion = "39cfb96",
        schemaVersion = 1,
        bytes = 82L * 1024 * 1024,
        sha256 = "0".repeat(64),
        minAppVersionCode = 1,
    )

    private val offline = object : DictTransport {
        override fun open(url: String, offset: Long): DictStream = error("offline")
    }

    private fun installer() = FullDictInstaller(
        dir = Files.createTempDirectory("pagelence-dict"),
        transport = offline,
    )

    private fun presenter(hosting: Boolean = true) = DictionaryPresenter(
        strings = stringsFor(UiLanguage.ZH),
        appVersionCode = 1,
        hostingEnabled = hosting,
    )

    @Test
    fun `a manifest for a newer app is explained instead of offered`() {
        val state = presenter().state(
            manifest = manifest.copy(schemaVersion = 99),
            installer = installer(),
            downloading = false,
            progress = 0f,
        )

        assertEquals(DictionaryPhase.UNAVAILABLE, state.phase)
        assertTrue(!state.status.isNullOrBlank())
        assertEquals(false, state.canDownload)
    }

    @Test
    fun `a fresh install offers the download when hosting is on`() {
        val state = presenter(hosting = true).state(
            manifest = manifest,
            installer = installer(),
            downloading = false,
            progress = 0f,
        )

        assertEquals(DictionaryPhase.NOT_INSTALLED, state.phase)
        assertTrue(state.canDownload)
    }

    @Test
    fun `hosting off hides the download entry`() {
        val state = presenter(hosting = false).state(
            manifest = manifest,
            installer = installer(),
            downloading = false,
            progress = 0f,
        )

        assertEquals(DictionaryPhase.UNAVAILABLE, state.phase)
        assertEquals(false, state.canDownload)
    }

    @Test
    fun `no manifest means the row explains hosting is pending`() {
        val state = presenter().state(
            manifest = null,
            installer = installer(),
            downloading = false,
            progress = 0f,
        )

        assertEquals(DictionaryPhase.UNAVAILABLE, state.phase)
        assertTrue(!state.status.isNullOrBlank())
    }
}
