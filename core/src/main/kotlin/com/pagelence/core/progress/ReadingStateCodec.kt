package com.pagelence.core.progress

import com.pagelence.core.content.PagelenceJson
import com.pagelence.core.lookup.LookupHistoryRules
import com.pagelence.core.prefs.DailyGoal
import com.pagelence.core.prefs.ReaderLayout
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject

/**
 * Reads and writes the one `state.json` document.
 *
 * Version handling is deliberately forgiving: a v1 document is upgraded by
 * adding v2 defaults (and marking the level prompt as already handled for
 * existing users), unknown fields are **kept** on write, and a higher version
 * is decoded on a best-effort basis without rewriting the file on load. A
 * version mismatch never clears books, cards or preferences.
 *
 * Keeping unknown fields is what makes a downgrade non-destructive: a reader
 * who installs a newer build (say, v3 with a new top-level list), then rolls back
 * to this build, can still turn pages without the next write silently deleting
 * the fields this build cannot see. Unknown fields inside arrays (a new field on
 * a word card or an annotation, for example) are still dropped; document that
 * when v3 lands.
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
        /** The document as read, used to carry unknown fields through a write. */
        val raw: JsonObject,
    )

    /**
     * Encode [state]. When [raw] is the document it was decoded from, top-level
     * and nested-object fields this build does not know are copied over, so an
     * older build cannot erase a newer build's additions.
     */
    fun encode(state: ReadingState, raw: JsonObject? = null): String {
        val known = PagelenceJson.encodeToJsonElement(ReadingState.serializer(), state)
        if (raw == null) return known.toString()
        return mergeUnknown(known.jsonObject, raw).toString()
    }

    /**
     * Known values win; keys only present in [raw] (top level, or inside a
     * shared object such as one book entry) are kept. Arrays are replaced
     * wholesale because their element order is not stable enough to merge.
     */
    private fun mergeUnknown(known: JsonObject, raw: JsonObject): JsonObject {
        val merged = LinkedHashMap<String, JsonElement>(known.size + 4)
        known.forEach { (key, value) ->
            val old = raw[key]
            merged[key] = if (value is JsonObject && old is JsonObject) {
                mergeUnknown(value, old)
            } else {
                value
            }
        }
        raw.forEach { (key, value) ->
            if (key !in merged) merged[key] = value
        }
        return JsonObject(merged)
    }

    /** Convenience for tests: the document's schemaVersion without full decode. */
    fun versionOf(json: String): Int {
        val root = runCatching { PagelenceJson.parseToJsonElement(json) as? JsonObject }.getOrNull()
            ?: return 0
        return (root["schemaVersion"] as? JsonPrimitive)?.intOrNull ?: 0
    }

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
            annotations = AnnotationRules.sanitize(decoded.annotations),
            lookupHistory = LookupHistoryRules.sanitize(decoded.lookupHistory),
            preferences = decoded.preferences.copy(
                dailyGoalMinutes = DailyGoal.sanitize(decoded.preferences.dailyGoalMinutes),
            ),
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
            raw = root,
        )
    }

    /** Rename-friendly alias for callers that treat null as an empty state. */
    fun decodeOrNull(json: String): ReadingState? = decode(json)?.state
}
