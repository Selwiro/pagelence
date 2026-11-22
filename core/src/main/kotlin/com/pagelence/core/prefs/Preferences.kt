package com.pagelence.core.prefs

import com.pagelence.core.content.PagelenceJson
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.floatOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive

@Serializable
enum class Language {
    @SerialName("zh") ZH,
    @SerialName("en") EN,
}

@Serializable
enum class ReadingMode {
    @SerialName("guided") GUIDED,
    @SerialName("original") ORIGINAL,
}

@Serializable
enum class ReaderLayout {
    /** One screen of text at a time; tap/swipe turns to the next screen. */
    @SerialName("paged") PAGED,

    /** One continuous vertical scroll through the unit. */
    @SerialName("scroll") SCROLL,

    /** Legacy value from the e-ink plan; treats the phone reader as paged. */
    @SerialName("eink") EINK,
}

@Serializable
enum class DefinitionMode {
    @SerialName("zh") ZH,
    @SerialName("mixed") MIXED,
    @SerialName("en") EN,
}

/**
 * How much of the 带读 text the reader sees. [BILINGUAL] replaced the old
 * Chinese-only mode in 2026-11-19: the guide now always shows both languages,
 * which is what readers actually used the Chinese mode for.
 */
@Serializable
enum class GuideMode {
    @SerialName("bilingual") BILINGUAL,
    @SerialName("en") EN,
    @SerialName("immersive") IMMERSIVE,
}

@Serializable
enum class ThemeMode {
    @SerialName("system") SYSTEM,
    @SerialName("light") LIGHT,
    @SerialName("dark") DARK,
}

@Serializable
enum class FontSize {
    @SerialName("small") SMALL,
    @SerialName("medium") MEDIUM,
    @SerialName("large") LARGE,
    @SerialName("xlarge") XLARGE,
}

/** Body face for the reading surface. UI chrome always follows the theme font. */
@Serializable
enum class ReadingFont {
    @SerialName("sans") SANS,
    @SerialName("serif") SERIF,
}

/** Line height as a multiple of the body size. */
@Serializable
enum class LineSpacing {
    @SerialName("compact") COMPACT,
    @SerialName("normal") NORMAL,
    @SerialName("relaxed") RELAXED,
}

/** Horizontal page margin of the reading column. */
@Serializable
enum class PageMargin {
    @SerialName("narrow") NARROW,
    @SerialName("normal") NORMAL,
    @SerialName("wide") WIDE,
}

/**
 * Interface and word-meaning preferences, frozen at version 1.
 *
 * Defaults and validation match the Web reader's `restorePreferences`: an
 * unknown or missing value falls back to the default instead of failing, so a
 * file written by a newer build still opens.
 */
@Serializable
data class Preferences(
    val language: Language = Language.ZH,
    val readingMode: ReadingMode = ReadingMode.GUIDED,
    val readerLayout: ReaderLayout = ReaderLayout.PAGED,
    /**
     * True once the reader picked a layout explicitly. Before this field
     * existed the app never exposed the setting, so legacy `scroll`/`eink`
     * values are treated as the old default and migrate to paged.
     */
    val readerLayoutChosen: Boolean = false,
    val definitionMode: DefinitionMode = DefinitionMode.ZH,
    val guideMode: GuideMode = GuideMode.BILINGUAL,
    val theme: ThemeMode = ThemeMode.SYSTEM,
    val fontSize: FontSize = FontSize.MEDIUM,
    /** Body face of the reading surface (T158). */
    val readingFont: ReadingFont = ReadingFont.SANS,
    /** Line height as a multiple of the body size (T158). */
    val lineSpacing: LineSpacing = LineSpacing.NORMAL,
    /** Horizontal margin of the reading column (T158). */
    val pageMargin: PageMargin = PageMargin.NORMAL,
    /**
     * Show every book translation under its sentence (T159). Off keeps the
     * per-sentence translate icon as the only way to reveal one.
     */
    val showAllTranslations: Boolean = false,
    /** Underline words that have a curated hint or contextual gloss. */
    val annotateHints: Boolean = true,
    val handledOffers: List<String> = emptyList(),
    /**
     * The first-run A1–C2 self-assessment was shown and either answered or
     * skipped. v1 states migrate to `true`, so existing readers are not asked
     * again; they can still set a level from "我的".
     */
    val levelOnboardingDone: Boolean = false,
    /** Text-to-speech rate, clamped to 0.7–1.3. */
    val speechRate: Float = 1.0f,
    /** `bookId::unitId` entries whose guide page was already started or dismissed. */
    val seenGuideUnits: List<String> = emptyList(),
    /**
     * Books the reader added to the shelf without starting them yet. Progress
     * stays in [com.pagelence.core.progress.BookProgress]; this list only
     * controls what the library shows before the first unit is read.
     */
    val shelfBookIds: List<String> = emptyList(),
    /**
     * Daily reading goal in minutes (T162), one of [DailyGoal.OPTIONS]. The
     * streak and the "今日" card read it; nothing is ever locked behind it.
     */
    val dailyGoalMinutes: Int = DailyGoal.DEFAULT_MINUTES,
) {
    companion object {
        fun restore(raw: JsonObject?): Preferences {
            if (raw == null) return Preferences()
            val chosenLayout = raw.boolean("readerLayoutChosen") ?: false
            val storedLayout = when (raw.text("readerLayout")) {
                "scroll" -> ReaderLayout.SCROLL
                "eink" -> ReaderLayout.EINK
                "paged" -> ReaderLayout.PAGED
                else -> ReaderLayout.PAGED
            }
            return Preferences(
                language = when (raw.text("language")) {
                    "en" -> Language.EN
                    "zh" -> Language.ZH
                    else -> Language.ZH
                },
                readingMode = when (raw.text("readingMode")) {
                    "original" -> ReadingMode.ORIGINAL
                    "guided" -> ReadingMode.GUIDED
                    else -> ReadingMode.GUIDED
                },
                readerLayout = if (chosenLayout) storedLayout else ReaderLayout.PAGED,
                readerLayoutChosen = chosenLayout,
                definitionMode = when (raw.text("definitionMode")) {
                    "mixed" -> DefinitionMode.MIXED
                    "en" -> DefinitionMode.EN
                    "zh" -> DefinitionMode.ZH
                    else -> DefinitionMode.ZH
                },
                guideMode = when (raw.text("guideMode")) {
                    "en" -> GuideMode.EN
                    "immersive" -> GuideMode.IMMERSIVE
                    // `zh` is the pre-2026-11-19 name of the bilingual mode.
                    "zh", "bilingual" -> GuideMode.BILINGUAL
                    else -> GuideMode.BILINGUAL
                },
                theme = when (raw.text("theme")) {
                    "light" -> ThemeMode.LIGHT
                    "dark" -> ThemeMode.DARK
                    "system" -> ThemeMode.SYSTEM
                    else -> ThemeMode.SYSTEM
                },
                fontSize = when (raw.text("fontSize")) {
                    "small" -> FontSize.SMALL
                    "large" -> FontSize.LARGE
                    "xlarge" -> FontSize.XLARGE
                    "medium" -> FontSize.MEDIUM
                    else -> FontSize.MEDIUM
                },
                readingFont = when (raw.text("readingFont")) {
                    "serif" -> ReadingFont.SERIF
                    "sans" -> ReadingFont.SANS
                    else -> ReadingFont.SANS
                },
                lineSpacing = when (raw.text("lineSpacing")) {
                    "compact" -> LineSpacing.COMPACT
                    "relaxed" -> LineSpacing.RELAXED
                    "normal" -> LineSpacing.NORMAL
                    else -> LineSpacing.NORMAL
                },
                pageMargin = when (raw.text("pageMargin")) {
                    "narrow" -> PageMargin.NARROW
                    "wide" -> PageMargin.WIDE
                    "normal" -> PageMargin.NORMAL
                    else -> PageMargin.NORMAL
                },
                showAllTranslations = raw.boolean("showAllTranslations") ?: false,
                annotateHints = raw.boolean("annotateHints") ?: true,
                handledOffers = raw.stringList("handledOffers"),
                levelOnboardingDone = raw.boolean("levelOnboardingDone") ?: false,
                speechRate = (raw.float("speechRate") ?: 1.0f).coerceIn(MIN_SPEECH_RATE, MAX_SPEECH_RATE),
                seenGuideUnits = raw.stringList("seenGuideUnits"),
                shelfBookIds = raw.stringList("shelfBookIds"),
                dailyGoalMinutes = DailyGoal.sanitize(raw.int("dailyGoalMinutes")),
            )
        }

        /** Restore from a preferences JSON string; malformed input means defaults. */
        fun restore(json: String): Preferences =
            runCatching {
                PagelenceJson.parseToJsonElement(json) as? JsonObject
            }.getOrNull().let(::restore)

        private fun JsonObject.text(key: String): String? =
            runCatching { this[key]?.jsonPrimitive?.content }.getOrNull()

        private fun JsonObject.boolean(key: String): Boolean? =
            runCatching { this[key]?.jsonPrimitive?.booleanOrNull }.getOrNull()

        private fun JsonObject.float(key: String): Float? =
            runCatching { this[key]?.jsonPrimitive?.floatOrNull }.getOrNull()

        private fun JsonObject.int(key: String): Int? =
            runCatching { this[key]?.jsonPrimitive?.intOrNull }.getOrNull()

        private fun JsonObject.stringList(key: String): List<String> {
            val value = this[key] ?: return emptyList()
            if (value !is kotlinx.serialization.json.JsonArray) return emptyList()
            return value.mapNotNull { element ->
                runCatching { element.jsonPrimitive.content }.getOrNull()
            }.filter { it.isNotBlank() }
        }

        const val MIN_SPEECH_RATE = 0.7f
        const val MAX_SPEECH_RATE = 1.3f
    }
}
