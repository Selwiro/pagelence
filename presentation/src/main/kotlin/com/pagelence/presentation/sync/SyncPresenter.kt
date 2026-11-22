package com.pagelence.presentation.sync

import com.pagelence.sync.SyncProvider
import com.pagelence.sync.SyncProviders
import com.pagelence.uim3.SyncCopy
import com.pagelence.uim3.model.ImportPreviewUi
import com.pagelence.uim3.model.SyncTone
import com.pagelence.uim3.model.SyncUiState
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * The 同步与备份 screen state: configured provider, last success/failure line,
 * password presence and any one-shot notice. Android-free; credentials and the
 * sync engine stay in the shell.
 */
class SyncPresenter(private val copy: SyncCopy) {
    fun state(
        configured: Boolean,
        provider: SyncProvider,
        url: String,
        username: String,
        allowHttp: Boolean,
        hasPassword: Boolean,
        lastSuccessAt: String?,
        lastFailureAt: String?,
        lastFailureCategory: String?,
        busy: Boolean = false,
        notice: String? = null,
        noticeIsError: Boolean = false,
        importPreview: ImportPreviewUi? = null,
    ): SyncUiState {
        val tone: SyncTone
        val statusText: String
        if (!configured) {
            tone = SyncTone.IDLE
            statusText = copy.statusIdle
        } else {
            val failedAt = parseInstant(lastFailureAt)
            val okAt = parseInstant(lastSuccessAt)
            if (failedAt != null && (okAt == null || failedAt.isAfter(okAt))) {
                tone = SyncTone.ERROR
                statusText = copy.statusFailure.format(
                    formatTime(lastFailureAt),
                    copy.errorFor(lastFailureCategory),
                )
            } else if (okAt != null) {
                tone = SyncTone.OK
                statusText = copy.statusOk.format(formatTime(lastSuccessAt))
            } else {
                tone = SyncTone.IDLE
                statusText = copy.statusConfigured
            }
        }
        return SyncUiState(
            configured = configured,
            provider = provider,
            url = url,
            username = username,
            allowHttp = allowHttp,
            hasPassword = hasPassword,
            templateUrl = SyncProviders.templateUrl(provider),
            busy = busy,
            tone = tone,
            status = statusText,
            notice = notice,
            noticeIsError = noticeIsError,
            importPreview = importPreview,
        )
    }

    private fun parseInstant(value: String?): Instant? =
        value?.let { runCatching { Instant.parse(it) }.getOrNull() }

    private fun formatTime(value: String?): String {
        val instant = parseInstant(value) ?: return "—"
        return DateTimeFormatter.ofPattern("MM-dd HH:mm")
            .withZone(ZoneId.systemDefault())
            .format(instant)
    }
}
