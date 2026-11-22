package com.pagelence.uim3

import com.pagelence.core.progress.AchievementId

/**
 * Achievement copy. The catalogue and its thresholds live in `:core`; this file
 * only bridges ids to the two strings every screen shows, the same way
 * [levelName] bridges `CefrLevel`.
 */
fun achievementTitle(strings: Strings, id: AchievementId): String = when (id) {
    AchievementId.FIRST_BOOK -> strings.achFirstBook
    AchievementId.THREE_BOOKS -> strings.achThreeBooks
    AchievementId.HUNDRED_UNITS -> strings.achHundredUnits
    AchievementId.STREAK_3 -> strings.achStreak3
    AchievementId.STREAK_7 -> strings.achStreak7
    AchievementId.STREAK_30 -> strings.achStreak30
    AchievementId.CARDS_50 -> strings.achCards50
    AchievementId.NOTES_20 -> strings.achNotes20
    AchievementId.READING_HOURS_10 -> strings.achReadingHours10
    AchievementId.LEVEL_B1 -> strings.achLevelB1
}

fun achievementDescription(strings: Strings, id: AchievementId): String = when (id) {
    AchievementId.FIRST_BOOK -> strings.achFirstBookHint
    AchievementId.THREE_BOOKS -> strings.achThreeBooksHint
    AchievementId.HUNDRED_UNITS -> strings.achHundredUnitsHint
    AchievementId.STREAK_3 -> strings.achStreak3Hint
    AchievementId.STREAK_7 -> strings.achStreak7Hint
    AchievementId.STREAK_30 -> strings.achStreak30Hint
    AchievementId.CARDS_50 -> strings.achCards50Hint
    AchievementId.NOTES_20 -> strings.achNotes20Hint
    AchievementId.READING_HOURS_10 -> strings.achReadingHours10Hint
    AchievementId.LEVEL_B1 -> strings.achLevelB1Hint
}
