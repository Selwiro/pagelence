package com.pagelence.sync

import com.pagelence.core.prefs.Preferences
import com.pagelence.core.progress.BookProgress
import com.pagelence.core.progress.DailyActivityRules
import com.pagelence.core.progress.ReadingState
import com.pagelence.core.wordcards.WordCard
import java.time.Instant

/** Counts describing what changed relative to the local document. */
data class MergeSummary(
    val booksAdded: Int = 0,
    val booksUpdated: Int = 0,
    val cardsAdded: Int = 0,
    val cardsUpdated: Int = 0,
    val cardsDeleted: Int = 0,
    val activityDaysChanged: Int = 0,
    val preferencesFromRemote: Boolean = false,
    val levelFromRemote: Boolean = false,
)

data class MergeResult(
    val state: ReadingState,
    val summary: MergeSummary,
)

/**
 * Field-level merge of two `state.json` documents.
 *
 * This is deliberately *not* "stop when both sides changed": the normal case is
 * device A finishing one unit and device B collecting another card. Stopping
 * there is a failure, not caution. Every field in the frozen v2 schema has a
 * deterministic winner, so the merge never has to ask the reader anything:
 *
 * | field | rule |
 * |---|---|
 * | `readUnitIds` | set union, local order first |
 * | `paragraphId` / `reader` / `lastReadAt` | side with the later `lastReadAt` |
 * | `wordCards` | per id, later `updatedAt` wins; `deletedAt` travels with it |
 * | `dailyActivity` | per-day max, union of dates (`DailyActivityRules`) |
 * | level fields | later document `updatedAt` |
 * | `preferences` | base-aware last write wins, see [mergePreferences] |
 * | `recentAssessmentPapers` | union, keep the last two papers |
 * | `deviceId` / `updatedAt` | always local (the merged doc is a local write) |
 *
 * `updatedAt` is only refreshed when the merge actually changes content, so an
 * up-to-date sync does not make both devices look "changed" forever.
 */
object StateMerge {
    private const val MAX_RECENT_PAPERS = 2

    fun merge(
        local: ReadingState,
        remote: ReadingState,
        base: ReadingState?,
        now: String,
        today: String,
    ): MergeResult {
        val (books, bookSummary) = mergeBooks(local, remote)
        val cards = mergeCards(local.wordCards, remote.wordCards)
        val activity = DailyActivityRules.mergeDayMax(local.dailyActivity, remote.dailyActivity, today)
        val levelDoc = laterDocument(local, remote)
        val (preferences, preferencesFromRemote) = mergePreferences(local, remote, base)

        val merged = ReadingState(
            schemaVersion = local.schemaVersion,
            deviceId = local.deviceId,
            updatedAt = local.updatedAt,
            books = books,
            wordCards = cards.cards,
            preferences = preferences,
            selfLevel = levelDoc.selfLevel,
            levelSource = levelDoc.levelSource,
            levelScore = levelDoc.levelScore,
            levelUpdatedAt = levelDoc.levelUpdatedAt,
            dailyActivity = activity,
            recentAssessmentPapers = mergePapers(local.recentAssessmentPapers, remote.recentAssessmentPapers),
        )
        val summary = MergeSummary(
            booksAdded = bookSummary.first,
            booksUpdated = bookSummary.second,
            cardsAdded = cards.added,
            cardsUpdated = cards.updated,
            cardsDeleted = cards.deleted,
            activityDaysChanged = activity.count { (date, day) -> local.dailyActivity[date] != day },
            preferencesFromRemote = preferencesFromRemote,
            levelFromRemote = levelDoc !== local,
        )

        // Only move updatedAt when the document really changed; otherwise every
        // successful sync would look like a new local edit to the next device.
        val changed = !contentEquals(merged, local)
        val state = if (changed) merged.copy(updatedAt = now) else local
        return MergeResult(state = state, summary = summary)
    }

    /** Merge a remote/exported document into local state without a base. */
    fun mergeIntoLocal(
        local: ReadingState,
        remote: ReadingState,
        now: String,
        today: String,
    ): MergeResult = merge(local, remote, base = null, now = now, today = today)

    private data class CardMerge(
        val cards: List<WordCard>,
        val added: Int,
        val updated: Int,
        val deleted: Int,
    )

    /** @return merged books plus (added, updated) counts. */
    private fun mergeBooks(
        local: ReadingState,
        remote: ReadingState,
    ): Pair<Map<String, BookProgress>, Pair<Int, Int>> {
        var added = 0
        var updated = 0
        val ids = LinkedHashSet<String>(local.books.size + remote.books.size)
        ids += local.books.keys
        ids += remote.books.keys
        val merged = LinkedHashMap<String, BookProgress>(ids.size)
        for (id in ids) {
            val localBook = local.books[id]
            val remoteBook = remote.books[id]
            val book = when {
                localBook == null -> {
                    added += 1
                    remoteBook
                }

                remoteBook == null -> localBook
                else -> {
                    val result = mergeBook(localBook, remoteBook)
                    if (result != localBook) updated += 1
                    result
                }
            } ?: continue
            merged[id] = book
        }
        return merged to (added to updated)
    }

    private fun mergeBook(local: BookProgress, remote: BookProgress): BookProgress {
        val localAt = instant(local.lastReadAt)
        val remoteAt = instant(remote.lastReadAt)
        val remoteIsNewer = when {
            remoteAt == null -> false
            localAt == null -> true
            else -> remoteAt.isAfter(localAt)
        }
        val newer = if (remoteIsNewer) remote else local
        // Union keeps every finished unit; the newer side decides where the
        // reader resumes and which `lastReadAt` travels on.
        val readUnits = (local.readUnitIds + remote.readUnitIds).distinct()
        return newer.copy(readUnitIds = readUnits)
    }

    private fun mergeCards(local: List<WordCard>, remote: List<WordCard>): CardMerge {
        val localById = local.associateBy { it.id }
        val remoteById = remote.associateBy { it.id }
        val ids = LinkedHashSet<String>(local.size + remote.size)
        ids += local.map { it.id }
        ids += remote.map { it.id }

        var added = 0
        var updated = 0
        var deleted = 0
        val cards = ArrayList<WordCard>(ids.size)
        for (id in ids) {
            val l = localById[id]
            val r = remoteById[id]
            val keep = when {
                l == null -> {
                    added += 1
                    r
                }

                r == null -> l
                else -> {
                    val localAt = instant(l.updatedAt)
                    val remoteAt = instant(r.updatedAt)
                    if (remoteAt != null && (localAt == null || remoteAt.isAfter(localAt))) r else l
                }
            } ?: continue
            if (l != null && keep != l) {
                if (keep.isDeleted && !l.isDeleted) deleted += 1 else updated += 1
            }
            cards.add(keep)
        }
        return CardMerge(cards, added, updated, deleted)
    }

    /**
     * Preferences have no per-field timestamps. With a base:
     * - local unchanged since the last sync -> take remote;
     * - remote unchanged -> keep local;
     * - both changed -> the later document wins.
     * Without a base (upgrade, cleared storage) the later document wins, which
     * never blocks the first sync.
     */
    private fun mergePreferences(
        local: ReadingState,
        remote: ReadingState,
        base: ReadingState?,
    ): Pair<Preferences, Boolean> {
        if (local.preferences == remote.preferences) return local.preferences to false
        if (base != null) {
            if (local.preferences == base.preferences) return remote.preferences to true
            if (remote.preferences == base.preferences) return local.preferences to false
        }
        val remoteIsLater = isLater(remote.updatedAt, local.updatedAt)
        return if (remoteIsLater) remote.preferences to true else local.preferences to false
    }

    private fun mergePapers(local: List<List<String>>, remote: List<List<String>>): List<List<String>> {
        val merged = LinkedHashSet<List<String>>()
        (local + remote).forEach { paper ->
            if (paper.isNotEmpty()) merged.add(paper)
        }
        return merged.toList().takeLast(MAX_RECENT_PAPERS)
    }

    private fun laterDocument(local: ReadingState, remote: ReadingState): ReadingState =
        if (isLater(remote.updatedAt, local.updatedAt)) remote else local

    private fun isLater(candidate: String?, current: String?): Boolean {
        val a = instant(candidate) ?: return false
        val b = instant(current) ?: return true
        return a.isAfter(b)
    }

    private fun contentEquals(a: ReadingState, b: ReadingState): Boolean =
        a.copy(updatedAt = "", deviceId = "") == b.copy(updatedAt = "", deviceId = "")

    /** ISO-8601 to instant; unparseable or missing is treated as oldest. */
    internal fun instant(value: String?): Instant? {
        if (value.isNullOrBlank()) return null
        return runCatching { Instant.parse(value) }.getOrNull()
    }
}
