package com.pagelence.core.prefs

import com.pagelence.core.progress.CefrLevel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LevelDefaultsTest {
    @Test
    fun `levels map to interface, meaning and guide defaults`() {
        assertEquals(Language.ZH, LevelDefaults.language(CefrLevel.A1))
        assertEquals(Language.ZH, LevelDefaults.language(CefrLevel.B2))
        assertEquals(Language.EN, LevelDefaults.language(CefrLevel.C1))
        assertEquals(Language.EN, LevelDefaults.language(CefrLevel.C2))

        assertEquals(DefinitionMode.ZH, LevelDefaults.definitionMode(CefrLevel.A1))
        assertEquals(DefinitionMode.ZH, LevelDefaults.definitionMode(CefrLevel.A2))
        assertEquals(DefinitionMode.MIXED, LevelDefaults.definitionMode(CefrLevel.B1))
        assertEquals(DefinitionMode.MIXED, LevelDefaults.definitionMode(CefrLevel.B2))
        assertEquals(DefinitionMode.EN, LevelDefaults.definitionMode(CefrLevel.C1))
        assertEquals(DefinitionMode.EN, LevelDefaults.definitionMode(CefrLevel.C2))

        assertEquals(GuideMode.BILINGUAL, LevelDefaults.guideMode(CefrLevel.A1))
        assertEquals(GuideMode.BILINGUAL, LevelDefaults.guideMode(CefrLevel.B1))
        assertEquals(GuideMode.EN, LevelDefaults.guideMode(CefrLevel.B2))
        assertEquals(GuideMode.EN, LevelDefaults.guideMode(CefrLevel.C1))
        assertEquals(GuideMode.IMMERSIVE, LevelDefaults.guideMode(CefrLevel.C2))
    }

    @Test
    fun `changes lists only the modes that differ`() {
        val defaults = Preferences()
        assertTrue(LevelDefaults.changes(defaults, CefrLevel.A1).isEmpty)
        assertEquals(
            LevelRecommendation(definitionMode = DefinitionMode.MIXED),
            LevelDefaults.changes(defaults, CefrLevel.B1),
        )
        assertEquals(
            LevelRecommendation(definitionMode = DefinitionMode.MIXED, guideMode = GuideMode.EN),
            LevelDefaults.changes(defaults, CefrLevel.B2),
        )
        assertEquals(
            LevelRecommendation(
                language = Language.EN,
                definitionMode = DefinitionMode.EN,
                guideMode = GuideMode.EN,
            ),
            LevelDefaults.changes(defaults, CefrLevel.C1),
        )
    }

    @Test
    fun `a reader already on the recommended modes is not offered anything`() {
        val preferences = Preferences(
            language = Language.EN,
            definitionMode = DefinitionMode.EN,
            guideMode = GuideMode.EN,
        )
        assertTrue(LevelDefaults.changes(preferences, CefrLevel.C1).isEmpty)
    }

    @Test
    fun `applying a partial recommendation keeps the modes it does not name`() {
        val preferences = Preferences(
            language = Language.EN,
            definitionMode = DefinitionMode.ZH,
            guideMode = GuideMode.IMMERSIVE,
            fontSize = FontSize.LARGE,
        )
        val applied = LevelDefaults.applyRecommendation(
            preferences,
            LevelRecommendation(definitionMode = DefinitionMode.MIXED),
        )
        assertEquals(Language.EN, applied.language)
        assertEquals(DefinitionMode.MIXED, applied.definitionMode)
        assertEquals(GuideMode.IMMERSIVE, applied.guideMode)
        assertEquals(FontSize.LARGE, applied.fontSize)
    }

    @Test
    fun `applying a level writes all three modes and nothing else`() {
        val applied = LevelDefaults.apply(Preferences(), CefrLevel.C2)
        assertEquals(Language.EN, applied.language)
        assertEquals(DefinitionMode.EN, applied.definitionMode)
        assertEquals(GuideMode.IMMERSIVE, applied.guideMode)
        assertEquals(FontSize.MEDIUM, applied.fontSize)
        assertEquals(ThemeMode.SYSTEM, applied.theme)
    }
}
