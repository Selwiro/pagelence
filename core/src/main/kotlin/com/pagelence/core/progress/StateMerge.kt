package com.pagelence.core.progress

import com.pagelence.core.lookup.LookupHistoryRules
import com.pagelence.core.prefs.Preferences
import com.pagelence.core.wordcards.Srs
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
    val annotationsAdded: Int = 0,
    val annotationsUpdated: Int = 0,
    val annotationsDeleted: Int = 0,
    val lookupsChanged: Int = 0,
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
 * | `finishedAt` | earliest non-null finish date; cleared on load when the union is incomplete |
 * | `paragraphId` / `reader` / `lastReadAt` | side with the later `lastReadAt` |
 * | `wordCards` | tombstone wins over any live copy; else later `updatedAt` for content, schedule from the later `lastReviewedAt` (T143) |
 * | `annotations` | same rule as `wordCards`: tombstone wins, else later `updatedAt` |
 * | `lookupHistory` | union by word id; the later `lookedUpAt` wins, newest first, capped |
 * | `dailyActivity` | per-day max, union of dates (`DailyActivityRules`) |
 * | level fields | later `levelUpdatedAt`; missing timestamps fall back to the document `updatedAt` |
 * | `preferences` | base-aware last write wins; a blank local document takes the file's copy, see [mergePreferences] |
 * | `recentAssessmentPapers` | union, keep the last two papers |
 * | `deviceId` / `updatedAt` | always local (the merged doc is a local write) |
 *
 * A tombstone is a deletion the reader already saw; a later edit on another
 * device must not silently resurrect the card. `updatedAt` is only refreshed
 * when the merge actually changes content, so an up-to-date sync does not make
 * both devices look "changed" forever.
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
        val annotationMerge = mergeAnnotations(local.annotations, remote.annotations)
        val activity = DailyActivityRules.mergeDayMax(local.dailyActivity, remote.dailyActivity, today)
        val lookups = LookupHistoryRules.merge(local.lookupHistory, remote.lookupHistory)
        val levelDoc = laterLevel(local, remote)
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
            annotations = annotationMerge.list,
            lookupHistory = lookups,
        )
        val summary = MergeSummary(
            booksAdded = bookSummary.first,
            booksUpdated = bookSummary.second,
            cardsAdded = cards.added,
            cardsUpdated = cards.updated,
            cardsDeleted = cards.deleted,
            activityDaysChanged = activity.count { (date, day) -> local.dailyActivity[date] != day },
            annotationsAdded = annotationMerge.added,
            annotationsUpdated = annotationMerge.updated,
            annotationsDeleted = annotationMerge.deleted,
            lookupsChanged = lookups.count { entry -> local.lookupHistory.none { it == entry } },
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
        // reader resumes and which `lastReadAt` travels on. The finish date is
        // the earliest non-null of both sides: a device that finished the book
        // earlier must not lose that date to a later sync. A union that no
        // longer covers the book is cleared when the app loads it
        // (`ProgressSemantics.restore`), because the merge itself has no book.
        val readUnits = (local.readUnitIds + remote.readUnitIds).distinct()
        return newer.copy(
            readUnitIds = readUnits,
            finishedAt = earliest(local.finishedAt, remote.finishedAt),
        )
    }

    /** Earliest parseable timestamp; an unparseable side is ignored. */
    private fun earliest(first: String?, second: String?): String? {
        val a = instant(first)
        val b = instant(second)
        return when {
            a == null -> second
            b == null -> first
            else -> if (a.isAfter(b)) second else first
        }
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
                // Tombstones are deletions the reader already acted on; a later
                // edit on the other device must not resurrect the card.
                l.isDeleted -> if (r.isDeleted) laterCard(l, r) else l
                r.isDeleted -> r
                else -> laterCard(l, r)
            } ?: continue
            if (l != null && keep != l) {
                if (keep.isDeleted && !l.isDeleted) deleted += 1 else updated += 1
            }
            cards.add(keep)
        }
        return CardMerge(cards, added, updated, deleted)
    }

    /**
     * Content comes from the card with the later `updatedAt`; the review
     * schedule is then overlaid from the side with the later `lastReviewedAt`
     * (see [com.pagelence.core.wordcards.Srs.mergeSchedule]). Grading a card and
     * collecting the same word on another device are independent edits, so the
     * newer content must not roll back a newer due date.
     */
    private fun laterCard(local: WordCard, remote: WordCard): WordCard {
        val localAt = instant(local.updatedAt)
        val remoteAt = instant(remote.updatedAt)
        val remoteIsNewer = remoteAt != null && (localAt == null || remoteAt.isAfter(localAt))
        val base = if (remoteIsNewer) remote else local
        val other = if (remoteIsNewer) local else remote
        return Srs.mergeSchedule(base, other)
    }

    private data class AnnotationMerge(
        val list: List<Annotation>,
        val added: Int,
        val updated: Int,
        val deleted: Int,
    )

    /** Same shape as [mergeCards], so both tombstone stores behave identically. */
    private fun mergeAnnotations(
        local: List<Annotation>,
        remote: List<Annotation>,
    ): AnnotationMerge {
        val localById = local.associateBy { it.id }
        val merge = AnnotationRules.merge(local, remote)
        var added = 0
        var updated = 0
        var deleted = 0
        merge.forEach { annotation ->
            val mine = localById[annotation.id]
            when {
                mine == null -> added += 1
                mine == annotation -> Unit
                annotation.isDeleted && !mine.isDeleted -> deleted += 1
                else -> updated += 1
            }
        }
        return AnnotationMerge(merge, added, updated, deleted)
    }

    /**
     * Preferences have no per-field timestamps. With a base:
     * - local unchanged since the last sync -> take remote;
     * - remote unchanged -> keep local;
     * - both changed -> the later document wins.
     * Without a base the later document wins, except on a blank local document
     * (fresh install / cleared data): there the imported file is the only real
     * source, so its preferences come back even though the empty doc was
     * written later.
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
        if (base == null && isBlank(local)) return remote.preferences to true
        val remoteIsLater = isLater(remote.updatedAt, local.updatedAt)
        return if (remoteIsLater) remote.preferences to true else local.preferences to false
    }

    private fun isBlank(state: ReadingState): Boolean =
        state.books.isEmpty() && state.wordCards.isEmpty() && state.dailyActivity.isEmpty()

    private fun mergePapers(local: List<List<String>>, remote: List<List<String>>): List<List<String>> {
        val merged = LinkedHashSet<List<String>>()
        (local + remote).forEach { paper ->
            if (paper.isNotEmpty()) merged.add(paper)
        }
        return merged.toList().takeLast(MAX_RECENT_PAPERS)
    }

    /** Level four fields follow their own timestamp, not the whole document. */
    private fun laterLevel(local: ReadingState, remote: ReadingState): ReadingState {
        // A device that never set a level has nothing to defend; take the only
        // level that exists even when the timestamps are missing (older files).
        if (local.selfLevel == null && remote.selfLevel != null) return remote
        if (remote.selfLevel == null && local.selfLevel != null) return local
        val localAt = instant(local.levelUpdatedAt)
        val remoteAt = instant(remote.levelUpdatedAt)
        return when {
            remoteAt != null && (localAt == null || remoteAt.isAfter(localAt)) -> remote
            localAt != null -> local
            else -> laterDocument(local, remote)
        }
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
    fun instant(value: String?): Instant? {
        if (value.isNullOrBlank()) return null
        return runCatching { Instant.parse(value) }.getOrNull()
    }
}
