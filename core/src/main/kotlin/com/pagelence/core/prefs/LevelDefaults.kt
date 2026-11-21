package com.pagelence.core.prefs

import com.pagelence.core.progress.CefrLevel

/**
 * The three reading modes a CEFR level recommends, decided 2026-11-21.
 *
 * A level only changes defaults; it never locks content. Since the interface
 * language and the reading aids are independent settings, the app shows the
 * differences to the reader first and writes only what they accept, so a
 * manual choice is never overwritten silently.
 */
object LevelDefaults {
    /**
     * Navigation stays in Chinese until the reader can read English without
     * extra effort; from C1 the English interface is the cheaper choice.
     */
    fun language(level: CefrLevel): Language = when (level) {
        CefrLevel.A1, CefrLevel.A2, CefrLevel.B1, CefrLevel.B2 -> Language.ZH
        CefrLevel.C1, CefrLevel.C2 -> Language.EN
    }

    fun definitionMode(level: CefrLevel): DefinitionMode = when (level) {
        CefrLevel.A1, CefrLevel.A2 -> DefinitionMode.ZH
        CefrLevel.B1, CefrLevel.B2 -> DefinitionMode.MIXED
        CefrLevel.C1, CefrLevel.C2 -> DefinitionMode.EN
    }

    /**
     * The guide carries context, not translation, so C1 keeps an English guide
     * instead of dropping it: literary originals still need the setup.
     */
    fun guideMode(level: CefrLevel): GuideMode = when (level) {
        CefrLevel.A1, CefrLevel.A2, CefrLevel.B1 -> GuideMode.BILINGUAL
        CefrLevel.B2, CefrLevel.C1 -> GuideMode.EN
        CefrLevel.C2 -> GuideMode.IMMERSIVE
    }

    /** All three recommended modes, including the ones already in use. */
    fun recommendation(level: CefrLevel): LevelRecommendation = LevelRecommendation(
        language = language(level),
        definitionMode = definitionMode(level),
        guideMode = guideMode(level),
    )

    /** Only the modes that differ from [preferences]; empty when nothing changes. */
    fun changes(preferences: Preferences, level: CefrLevel): LevelRecommendation {
        val recommended = recommendation(level)
        return LevelRecommendation(
            language = recommended.language?.takeIf { it != preferences.language },
            definitionMode = recommended.definitionMode?.takeIf { it != preferences.definitionMode },
            guideMode = recommended.guideMode?.takeIf { it != preferences.guideMode },
        )
    }

    /** Write the modes a [LevelRecommendation] names; null keeps the current value. */
    fun applyRecommendation(
        preferences: Preferences,
        recommendation: LevelRecommendation,
    ): Preferences = preferences.copy(
        language = recommendation.language ?: preferences.language,
        definitionMode = recommendation.definitionMode ?: preferences.definitionMode,
        guideMode = recommendation.guideMode ?: preferences.guideMode,
    )

    /** Write all three modes this level recommends. */
    fun apply(preferences: Preferences, level: CefrLevel): Preferences =
        applyRecommendation(preferences, recommendation(level))
}

/**
 * Recommended modes for a level. A null field means "leave the current value
 * as it is", which is how a partial recommendation (only the differences) is
 * expressed.
 */
data class LevelRecommendation(
    val language: Language? = null,
    val definitionMode: DefinitionMode? = null,
    val guideMode: GuideMode? = null,
) {
    val isEmpty: Boolean
        get() = language == null && definitionMode == null && guideMode == null
}
