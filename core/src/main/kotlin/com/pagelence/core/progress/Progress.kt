package com.pagelence.core.progress

import com.pagelence.core.content.ContentIndex
import com.pagelence.core.lookup.LookupEntry
import com.pagelence.core.prefs.Preferences
import com.pagelence.core.wordcards.WordCard
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Where the reader was inside the last opened unit. */
@Serializable
enum class ReaderPhase {
    @SerialName("guide") GUIDE,
    @SerialName("text") TEXT,
    @SerialName("quiz") QUIZ,
    @SerialName("grammar") GRAMMAR,
}

/**
 * Per-unit resume point. Stored on [BookProgress] so backing out and reopening
 * the book returns to the same phase/page; quiz selections survive the trip.
 *
 * [textPage] is the 0-based page in paged layout. [textProgress] is the
 * normalised scroll offset used by the continuous layout and also lets a font
 * change land near the same place. Both are best-effort and clamped by the UI.
 */
@Serializable
data class ReaderPosition(
    val unitId: String,
    val phase: ReaderPhase = ReaderPhase.TEXT,
    val textPage: Int = 0,
    val textProgress: Float = 0f,
    val quizAnswers: Map<Int, Int> = emptyMap(),
    val quizSubmitted: Boolean = false,
    val grammarReached: Boolean = false,
)

/** Progress inside one book: last position plus the set of finished units. */
@Serializable
data class BookProgress(
    val bookId: String,
    val paragraphId: String? = null,
    val readUnitIds: List<String> = emptyList(),
    /** Last write that touched this book; drives "recent reading". */
    val lastReadAt: String? = null,
    /** Resume point for the last opened unit; null for older states. */
    val reader: ReaderPosition? = null,
    /**
     * When the reader finished the whole book, ISO-8601 UTC. Added on v2 as an
     * optional field, so documents written before T139 decode it as `null`.
     * Cleared if the book stops being complete; the shelf sorts finished books
     * by it (books finished before the field existed fall back to `lastReadAt`).
     */
    val finishedAt: String? = null,
)

/** CEFR band used by self-assessment, placement tests and the level defaults. */
@Serializable
enum class CefrLevel {
    @SerialName("A1") A1,
    @SerialName("A2") A2,
    @SerialName("B1") B1,
    @SerialName("B2") B2,
    @SerialName("C1") C1,
    @SerialName("C2") C2,
}

/** Where [ReadingState.selfLevel] came from. */
@Serializable
enum class LevelSource {
    @SerialName("self") SELF,
    @SerialName("quick") QUICK,
    @SerialName("full") FULL,
}

/**
 * One day of study, keyed by the local date (`YYYY-MM-DD`) in
 * [ReadingState.dailyActivity]. A key exists once the reader opened a book that
 * day, so the number of keys is the reading-day count; [completedUnits] counts
 * finished units (max on sync merge, see `DailyActivityRules`).
 */
@Serializable
data class DailyActivity(
    val completedUnits: Int = 0,
)

/**
 * Syncable reading state, frozen at version 2.
 *
 * The same document is stored locally, exported from the app and written to
 * WebDAV, so it carries [deviceId] and [updatedAt] for merge decisions.
 *
 * Version 2 adds the self-assessed level, its source and score, the per-day
 * activity log, and [BookProgress.lastReadAt]. Version 1 documents are migrated
 * by [ReadingStateCodec]; missing fields fall back to their defaults.
 */
@Serializable
data class ReadingState(
    val schemaVersion: Int = STATE_SCHEMA_VERSION,
    val deviceId: String,
    val updatedAt: String,
    val books: Map<String, BookProgress> = emptyMap(),
    val wordCards: List<WordCard> = emptyList(),
    val preferences: Preferences = Preferences(),
    val selfLevel: CefrLevel? = null,
    val levelSource: LevelSource = LevelSource.SELF,
    /** Placement-test score, 0–100. Null for a self-assessment. */
    val levelScore: Int? = null,
    val levelUpdatedAt: String? = null,
    val dailyActivity: Map<String, DailyActivity> = emptyMap(),
    /** Question ids of the last two placement tests, oldest paper first. */
    val recentAssessmentPapers: List<List<String>> = emptyList(),
    /**
     * Sentence-level bookmarks, highlights and notes. Added on v2 as an optional
     * field (T144), so documents written before it decode to an empty list.
     * Tombstones are kept for merge, exactly like word cards.
     */
    val annotations: List<Annotation> = emptyList(),
    /**
     * Recent word lookups (T161). Added on v2 as an optional field, so older
     * documents decode to an empty list; one row per word per book.
     */
    val lookupHistory: List<LookupEntry> = emptyList(),
) {
    fun progress(bookId: String): BookProgress =
        books[bookId] ?: BookProgress(bookId = bookId)
}

const val STATE_SCHEMA_VERSION = 2

/** The parts of [ReadingState] shown on "我的" and maintained by the profile repository. */
data class ReadingProfile(
    val selfLevel: CefrLevel? = null,
    val levelSource: LevelSource = LevelSource.SELF,
    val levelScore: Int? = null,
    val levelUpdatedAt: String? = null,
    val dailyActivity: Map<String, DailyActivity> = emptyMap(),
    /** Question ids of the last two placement tests, oldest paper first. */
    val recentAssessmentPapers: List<List<String>> = emptyList(),
) {
    val readingDays: Int get() = dailyActivity.size

    fun activity(date: String): DailyActivity =
        dailyActivity[date] ?: DailyActivity()

    companion object {
        fun of(state: ReadingState): ReadingProfile = ReadingProfile(
            selfLevel = state.selfLevel,
            levelSource = state.levelSource,
            levelScore = state.levelScore,
            levelUpdatedAt = state.levelUpdatedAt,
            dailyActivity = state.dailyActivity,
        )
    }
}

/**
 * Storage contract for the level and the daily activity log. `:data` implements
 * it over the same `state.json`; UI code only reads [ReadingProfile].
 */
interface ReadingProfileRepository {
    fun profile(): ReadingProfile

    fun setLevel(
        level: CefrLevel?,
        source: LevelSource,
        score: Int?,
        updatedAt: String,
    ): ReadingProfile

    /** Record that the reader opened a book on [date]; a no-op when already present. */
    fun recordReadingDay(date: String): ReadingProfile

    /** Record one finished unit on [date]. */
    fun recordCompletedUnit(date: String): ReadingProfile

    /** Question ids drawn by the last two tests, flattened, oldest first. */
    fun recentAssessmentIds(): List<String> = emptyList()

    /** Append one test's ids, keeping only the most recent two tests. */
    fun recordAssessmentIds(ids: List<String>) {}
}

/**
 * Rules for reading progress.
 *
 * `advance` deliberately mirrors the Web reader (`work/web/src/reading.mjs`,
 * `advance`): finish a unit, mark it read once, and move the position to the
 * first paragraph of the next unit. The Web/Android parity test replays the Web
 * data recorded by `tools/migrate/record_progress_parity.py` and checks that the
 * read-unit count stays identical.
 */
object ProgressSemantics {
    /**
     * Drop ids the book no longer has and collapse duplicates, keeping order.
     *
     * A stored [BookProgress.finishedAt] only survives when the sanitised unit
     * set still covers the book: a hand-edited document that removed a unit is
     * treated as "in progress" again instead of claiming a stale finish date.
     */
    fun restore(saved: BookProgress?, index: ContentIndex): BookProgress {
        val bookId = index.book.id
        if (saved == null) return BookProgress(bookId = bookId)
        val paragraphId = saved.paragraphId?.takeIf { it in index.paragraphById }
        val seen = LinkedHashSet<String>()
        for (unitId in saved.readUnitIds) {
            if (unitId in index.unitIds) seen += unitId
        }
        val readUnitIds = seen.toList()
        return BookProgress(
            bookId = bookId,
            paragraphId = paragraphId,
            readUnitIds = readUnitIds,
            lastReadAt = saved.lastReadAt,
            reader = saved.reader?.takeIf { it.unitId in index.unitIds },
            finishedAt = saved.finishedAt?.takeIf { isComplete(readUnitIds, index) },
        )
    }

    /**
     * Mark [unitId] read and move to the next unit, like the Web reader.
     *
     * [now] is the completion timestamp, or `null` for a passive save that must
     * not stamp a finish (a resume point written while browsing). Incomplete
     * books always lose [BookProgress.finishedAt].
     */
    fun advance(
        progress: BookProgress,
        index: ContentIndex,
        unitId: String,
        now: String?,
    ): BookProgress {
        val unit = index.unit(unitId) ?: return progress
        val next = index.nextUnit(unitId)
        val moved = progress.copy(
            readUnitIds = (progress.readUnitIds + unit.id).distinct(),
            paragraphId = next?.paragraphs?.firstOrNull()?.id ?: progress.paragraphId,
            reader = null,
        )
        return stamped(moved, index, now)
    }

    /** Mark a unit read without moving the position ("今天读到这里"). */
    fun markUnitRead(
        progress: BookProgress,
        index: ContentIndex,
        unitId: String,
        now: String?,
    ): BookProgress {
        if (unitId !in index.unitIds) return progress
        val marked = progress.copy(readUnitIds = (progress.readUnitIds + unitId).distinct())
        return stamped(marked, index, now)
    }

    /**
     * Keep the first finish date, set [now] on a fresh completion, and clear it
     * whenever the unit set no longer covers the book.
     */
    private fun stamped(progress: BookProgress, index: ContentIndex, now: String?): BookProgress {
        val complete = isComplete(progress, index)
        val finishedAt = when {
            !complete -> null
            progress.finishedAt != null -> progress.finishedAt
            else -> now
        }
        return if (finishedAt == progress.finishedAt) progress else progress.copy(finishedAt = finishedAt)
    }

    fun setPosition(progress: BookProgress, index: ContentIndex, paragraphId: String): BookProgress {
        if (paragraphId !in index.paragraphById) return progress
        return progress.copy(paragraphId = paragraphId)
    }

    fun readUnitCount(progress: BookProgress, index: ContentIndex): Int =
        progress.readUnitIds.count { it in index.unitIds }

    fun isComplete(progress: BookProgress, index: ContentIndex): Boolean =
        isComplete(progress.readUnitIds, index)

    private fun isComplete(readUnitIds: List<String>, index: ContentIndex): Boolean =
        index.units.isNotEmpty() && readUnitIds.count { it in index.unitIds } == index.units.size

    /** True when a save changed a book from "in progress" to "finished". */
    fun justFinished(before: BookProgress, after: BookProgress): Boolean =
        before.finishedAt == null && after.finishedAt != null
}

/** Storage contract for progress; Room implements this in the app modules. */
interface ProgressRepository {
    fun progress(bookId: String): BookProgress

    fun save(progress: BookProgress): BookProgress

    /**
     * Save only where the reader is (page turns). The file-backed store writes a
     * small journal instead of the whole document; in-memory stores can keep the
     * default, which folds the position into a normal save.
     */
    fun savePosition(bookId: String, paragraphId: String?, position: ReaderPosition) {
        val current = progress(bookId)
        save(current.copy(paragraphId = paragraphId ?: current.paragraphId, reader = position))
    }
}

class InMemoryProgressRepository(
    initial: Map<String, BookProgress> = emptyMap(),
) : ProgressRepository {
    private val books = LinkedHashMap(initial)

    override fun progress(bookId: String): BookProgress =
        books[bookId] ?: BookProgress(bookId = bookId)

    override fun save(progress: BookProgress): BookProgress {
        books[progress.bookId] = progress
        return progress
    }
}
