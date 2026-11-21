package com.pagelence.sync

import com.pagelence.core.progress.ReadingState
import com.pagelence.core.progress.ReadingStateCodec

/**
 * Local export/import: the same `state.json` that WebDAV uploads, so a reader
 * without sync configured still has a migration path (new phone, backup).
 *
 * Import merges instead of replacing: progress and cards collected after the
 * export must survive. The merge is the same code WebDAV uses, minus the
 * baseline (an exported file carries no sync history).
 */
object StateTransfer {
    data class ImportResult(
        val state: ReadingState,
        val summary: MergeSummary,
    )

    fun export(local: LocalState): String = local.snapshot().json

    fun import(
        local: LocalState,
        json: String,
        now: String,
        today: String,
    ): ImportResult {
        val decoded = ReadingStateCodec.decode(json)?.state
            ?: throw SyncException(
                "导入的文件不是有效的进度文件",
                SyncErrorCategory.OTHER,
                "IMPORT_DECODE",
            )
        val result = StateMerge.mergeIntoLocal(local.current(), decoded, now, today)
        local.replace(result.state)
        return ImportResult(state = result.state, summary = result.summary)
    }
}
