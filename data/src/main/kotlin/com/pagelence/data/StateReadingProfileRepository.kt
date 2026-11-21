package com.pagelence.data

import com.pagelence.core.progress.CefrLevel
import com.pagelence.core.progress.DailyActivityRules
import com.pagelence.core.progress.LevelSource
import com.pagelence.core.progress.ReadingProfile
import com.pagelence.core.progress.ReadingProfileRepository

/** Level and daily activity, stored in the same `state.json` as progress. */
class StateReadingProfileRepository(private val store: ReadingStateStore) : ReadingProfileRepository {
    override fun profile(): ReadingProfile = ReadingProfile.of(store.state())

    override fun setLevel(
        level: CefrLevel?,
        source: LevelSource,
        score: Int?,
        updatedAt: String,
    ): ReadingProfile {
        val next = store.update { state ->
            state.copy(
                selfLevel = level,
                levelSource = if (level == null) LevelSource.SELF else source,
                levelScore = if (level == null) null else score?.coerceIn(0, 100),
                levelUpdatedAt = if (level == null) null else updatedAt,
            )
        }
        return ReadingProfile.of(next)
    }

    override fun recordReadingDay(date: String): ReadingProfile {
        val current = store.state()
        if (date in current.dailyActivity) return ReadingProfile.of(current)
        val next = store.update { state ->
            state.copy(dailyActivity = DailyActivityRules.recordReadingDay(state.dailyActivity, date))
        }
        return ReadingProfile.of(next)
    }

    override fun recordCompletedUnit(date: String): ReadingProfile {
        val next = store.update { state ->
            state.copy(dailyActivity = DailyActivityRules.recordCompletedUnit(state.dailyActivity, date))
        }
        return ReadingProfile.of(next)
    }

    override fun recentAssessmentIds(): List<String> =
        store.state().recentAssessmentPapers.flatten()

    override fun recordAssessmentIds(ids: List<String>) {
        if (ids.isEmpty()) return
        store.update { state ->
            // The window is two tests; a quick and a full paper both count as one.
            state.copy(
                recentAssessmentPapers = (state.recentAssessmentPapers + listOf(ids)).takeLast(2),
            )
        }
    }
}
