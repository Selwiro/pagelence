package com.pagelence.presentation.dictionary

import com.pagelence.core.dict.DictManifest
import com.pagelence.core.dict.DictRejection
import com.pagelence.core.dict.DictRules
import com.pagelence.core.dict.FullDictInstaller
import com.pagelence.presentation.store.formatBytes
import com.pagelence.uim3.Strings
import com.pagelence.uim3.model.DictionaryPhase
import com.pagelence.uim3.model.DictionaryUiState

/**
 * The optional full-ECDICT row in settings: version, size, install/update/
 * resume state. Android-free; the shell passes the current download flags.
 */
class DictionaryPresenter(
    private val strings: Strings,
    /** Passed to [DictRules] so a manifest for a newer app is explained, not offered. */
    private val appVersionCode: Int,
    /** Whether this build exposes the download entry (hosting may be offline). */
    private val hostingEnabled: Boolean,
) {
    fun state(
        manifest: DictManifest?,
        installer: FullDictInstaller,
        downloading: Boolean,
        progress: Float,
    ): DictionaryUiState {
        if (manifest == null) {
            return DictionaryUiState(
                phase = DictionaryPhase.UNAVAILABLE,
                status = strings.dictHostingPending,
            )
        }
        val rejection = DictRules.validate(manifest, appVersionCode)
        if (rejection != null) {
            return DictionaryUiState(
                phase = DictionaryPhase.UNAVAILABLE,
                version = manifest.dictVersion,
                sizeLabel = formatBytes(manifest.bytes),
                status = when (rejection) {
                    is DictRejection.Schema, is DictRejection.AppTooOld -> strings.dictNeedsNewerApp
                    else -> strings.dictHostingPending
                },
            )
        }
        val installed = installer.isInstalled(manifest)
        val installedVersion = installer.installedVersion()
        val partial = installer.downloadedBytes()
        val resumable = installer.canResume(manifest)
        val base = DictionaryUiState(
            version = manifest.dictVersion,
            sizeLabel = formatBytes(manifest.bytes),
            installedVersion = installedVersion,
            canDelete = installedVersion != null || partial > 0,
        )
        if (downloading) {
            val clamped = progress.coerceIn(0f, 1f)
            return base.copy(
                phase = DictionaryPhase.DOWNLOADING,
                progress = clamped,
                status = strings.dictDownloading.format((clamped * 100).toInt()),
                canPause = true,
            )
        }
        return when {
            installed -> base.copy(
                phase = DictionaryPhase.INSTALLED,
                status = strings.dictInstalled,
            )

            // The table may be installed even when this build will not offer a
            // download; show it, only disable the offer.
            !hostingEnabled -> base.copy(
                phase = DictionaryPhase.UNAVAILABLE,
                status = strings.dictHostingPending,
            )

            resumable -> base.copy(
                phase = DictionaryPhase.PAUSED,
                status = strings.dictPaused.format(formatBytes(partial)),
                canResume = true,
            )

            installedVersion != null -> base.copy(
                phase = DictionaryPhase.UPDATE_AVAILABLE,
                status = strings.dictUpdateAvailable.format(manifest.dictVersion, installedVersion),
                canDownload = true,
            )

            else -> base.copy(
                phase = DictionaryPhase.NOT_INSTALLED,
                status = strings.dictNotInstalled,
                canDownload = true,
            )
        }
    }
}
