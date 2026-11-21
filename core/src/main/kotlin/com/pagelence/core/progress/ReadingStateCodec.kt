package com.pagelence.core.progress

import com.pagelence.core.content.PagelenceJson
import com.pagelence.core.prefs.ReaderLayout
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.intOrNull

/**
 * Reads and writes the one `state.json` document.
 *
 * Version handling is deliberately forgiving: a v1 document is upgraded by
 * adding v2 defaults (and marking the level prompt as already handled for
 * existing users), unknown fields are ignored, and a higher version is decoded
 * on a best-effort basis without rewriting the file. A version mismatch never
 * clears books, cards or preferences.
 */
object ReadingStateCodec {
    data class Decoded(
        val state: ReadingState,
        /** Version found in the document; `0` means the field was missing. */
        val sourceVersion: Int,
        /** The document is older than [STATE_SCHEMA_VERSION] and was upgraded. */
        val upgraded: Boolean,
        /** The document came from a newer build; do not write it back automatically. */
        val fromFuture: Boolean,
    )

    fun encode(state: ReadingState): String =
        PagelenceJson.encodeToString(ReadingState.serializer(), state)

    /** Decode, or null when the text is not a readable state document at all. */
    fun decode(json: String): Decoded? {
        val root = runCatching {
            PagelenceJson.parseToJsonElement(json) as? JsonObject
        }.getOrNull() ?: return null
        val version = (root["schemaVersion"] as? JsonPrimitive)?.intOrNull ?: 0
        val decoded = runCatching {
            PagelenceJson.decodeFromJsonElement<ReadingState>(root)
        }.getOrNull() ?: return null

        val isOld = version < STATE_SCHEMA_VERSION
        val isFuture = version > STATE_SCHEMA_VERSION
        var state = decoded.copy(
            schemaVersion = if (isFuture) version else STATE_SCHEMA_VERSION,
            levelScore = decoded.levelScore?.coerceIn(0, 100),
            dailyActivity = DailyActivityRules.sanitize(decoded.dailyActivity),
        )
        if (!state.preferences.readerLayoutChosen) {
            // Older states never exposed the layout choice; their `scroll` value
            // is the old default, not a user choice. Start those readers in paged.
            state = state.copy(
                preferences = state.preferences.copy(readerLayout = ReaderLayout.PAGED),
            )
        }
        if (isOld) {
            // A v1 reader had no level prompt; do not show it again after the upgrade.
            state = state.copy(
                preferences = state.preferences.copy(levelOnboardingDone = true),
            )
        }
        return Decoded(
            state = state,
            sourceVersion = version,
            upgraded = isOld,
            fromFuture = isFuture,
        )
    }

    /** Rename-friendly alias for callers that treat null as an empty state. */
    fun decodeOrNull(json: String): ReadingState? = decode(json)?.state
}
