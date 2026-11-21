package com.pagelence.sync

import com.pagelence.core.progress.CefrLevel
import com.pagelence.core.progress.MergeSummary
import com.pagelence.core.progress.ReadingState
import com.pagelence.core.progress.ReadingStateCodec
import com.pagelence.core.progress.StateMerge

/**
 * Local export/import: the same `state.json` that WebDAV uploads, so a reader
 * without sync configured still has a migration path (new phone, backup).
 *
 * Import is a two-step flow: [inspect] validates the file and produces the
 * numbers shown before anything is written, then [import] merges (default) or
 * replaces the whole document after an explicit confirmation. An export never
 * carries a sync baseline, so the merge runs without one.
 */
object StateTransfer {
    /** Import cap: a progress document is a few KB; 5 MB leaves ample headroom. */
    const val MAX_IMPORT_BYTES: Long = 5L * 1024 * 1024

    enum class ImportMode { MERGE, OVERWRITE }

    data class ImportPreview(
        val books: Int,
        val readUnits: Int,
        val cards: Int,
        val level: CefrLevel?,
        val updatedAt: String,
    )

    /** Result of parsing a picked file; nothing is written until [import]. */
    sealed interface Inspection {
        data class Ready(val document: ReadingState, val preview: ImportPreview) : Inspection

        /** Not JSON, not a progress document, or fields unreadable. */
        data object Invalid : Inspection

        /** Written by a newer build; importing would drop fields this build cannot see. */
        data object FutureVersion : Inspection
    }

    data class ImportResult(
        val state: ReadingState,
        val summary: MergeSummary?,
        val mode: ImportMode,
    )

    fun export(local: LocalState): String = local.snapshot().json

    fun inspect(json: String): Inspection {
        val decoded = ReadingStateCodec.decode(json) ?: return Inspection.Invalid
        if (decoded.fromFuture) return Inspection.FutureVersion
        val document = decoded.state
        return Inspection.Ready(
            document = document,
            preview = ImportPreview(
                books = document.books.size,
                readUnits = document.books.values.sumOf { it.readUnitIds.size },
                cards = document.wordCards.count { !it.isDeleted },
                level = document.selfLevel,
                updatedAt = document.updatedAt,
            ),
        )
    }

    /**
     * Apply an inspected document. Merge keeps everything the reader did after
     * the export; overwrite keeps only the file (and the local `deviceId`, so
     * the merged document still identifies this installation).
     */
    fun import(
        local: LocalState,
        document: ReadingState,
        mode: ImportMode,
        now: String,
        today: String,
    ): ImportResult = when (mode) {
        ImportMode.MERGE -> {
            val result = StateMerge.mergeIntoLocal(local.current(), document, now, today)
            local.replace(result.state)
            ImportResult(state = result.state, summary = result.summary, mode = mode)
        }

        ImportMode.OVERWRITE -> {
            val state = document.copy(deviceId = local.current().deviceId)
            local.replace(state)
            ImportResult(state = state, summary = null, mode = mode)
        }
    }
}
