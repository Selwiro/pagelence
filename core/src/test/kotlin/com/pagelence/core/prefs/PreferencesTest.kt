package com.pagelence.core.prefs

import com.pagelence.core.content.PagelenceJson
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PreferencesTest {
    @Test
    fun `defaults follow the web reader`() {
        val preferences = Preferences()
        assertEquals(Language.ZH, preferences.language)
        assertEquals(ReadingMode.GUIDED, preferences.readingMode)
        assertEquals(ReaderLayout.PAGED, preferences.readerLayout)
        assertEquals(DefinitionMode.ZH, preferences.definitionMode)
        assertEquals(GuideMode.BILINGUAL, preferences.guideMode)
        assertEquals(ThemeMode.SYSTEM, preferences.theme)
        assertEquals(FontSize.MEDIUM, preferences.fontSize)
        assertTrue(preferences.annotateHints)
        assertEquals(emptyList(), preferences.handledOffers)
        assertEquals(1.0f, preferences.speechRate)
    }

    @Test
    fun `the old Chinese-only guide mode restores as bilingual`() {
        val raw = buildJsonObject { put("guideMode", JsonPrimitive("zh")) }
        assertEquals(GuideMode.BILINGUAL, Preferences.restore(raw).guideMode)
    }

    @Test
    fun `unknown values fall back instead of failing`() {
        val raw = buildJsonObject {
            put("language", JsonPrimitive("fr"))
            put("readingMode", JsonPrimitive("scroll"))
            put("readerLayout", JsonPrimitive("paged"))
            put("definitionMode", JsonPrimitive("bilingual"))
            put("guideMode", JsonPrimitive("auto"))
            put("theme", JsonPrimitive("sepia"))
            put("fontSize", JsonPrimitive("huge"))
        }
        assertEquals(Preferences(), Preferences.restore(raw))
    }

    @Test
    fun `valid values are restored`() {
        val raw = buildJsonObject {
            put("language", JsonPrimitive("en"))
            put("readingMode", JsonPrimitive("original"))
            put("readerLayout", JsonPrimitive("eink"))
            put("readerLayoutChosen", JsonPrimitive(true))
            put("definitionMode", JsonPrimitive("mixed"))
            put("guideMode", JsonPrimitive("immersive"))
            put("theme", JsonPrimitive("dark"))
            put("fontSize", JsonPrimitive("large"))
            put("annotateHints", JsonPrimitive(false))
            put("speechRate", JsonPrimitive(0.85))
            put(
                "handledOffers",
                buildJsonArray {
                    add(JsonPrimitive("pilot-ui-en"))
                    add(JsonPrimitive(""))
                    add(JsonPrimitive(7))
                },
            )
        }
        assertEquals(
            Preferences(
                language = Language.EN,
                readingMode = ReadingMode.ORIGINAL,
                readerLayout = ReaderLayout.EINK,
                readerLayoutChosen = true,
                definitionMode = DefinitionMode.MIXED,
                guideMode = GuideMode.IMMERSIVE,
                theme = ThemeMode.DARK,
                fontSize = FontSize.LARGE,
                annotateHints = false,
                handledOffers = listOf("pilot-ui-en", "7"),
                speechRate = 0.85f,
            ),
            Preferences.restore(raw),
        )
    }

    @Test
    fun `speech rate is clamped to the usable range`() {
        assertEquals(1.3f, Preferences.restore(buildJsonObject { put("speechRate", JsonPrimitive(2.0)) }).speechRate)
        assertEquals(0.7f, Preferences.restore(buildJsonObject { put("speechRate", JsonPrimitive(0.1)) }).speechRate)
    }

    @Test
    fun `malformed json means defaults`() {
        assertEquals(Preferences(), Preferences.restore("not json"))
        assertEquals(Preferences(), Preferences.restore("[]"))
        assertEquals(Preferences(), Preferences.restore(JsonObject(emptyMap())))
        assertEquals(Preferences(), Preferences.restore(null))
    }

    @Test
    fun `serialised enum values match the web storage keys`() {
        val preferences = Preferences(
            language = Language.EN,
            readingMode = ReadingMode.ORIGINAL,
            readerLayout = ReaderLayout.EINK,
            readerLayoutChosen = true,
            definitionMode = DefinitionMode.MIXED,
            guideMode = GuideMode.IMMERSIVE,
            theme = ThemeMode.DARK,
            fontSize = FontSize.LARGE,
        )
        val json = PagelenceJson.encodeToString(Preferences.serializer(), preferences)
        listOf(
            "\"en\"", "\"original\"", "\"eink\"", "\"mixed\"", "\"immersive\"",
            "\"dark\"", "\"large\"",
        )
            .forEach { fragment -> assertTrue(json.contains(fragment), "missing $fragment in $json") }
        assertEquals(preferences, Preferences.restore(json))
    }
}
