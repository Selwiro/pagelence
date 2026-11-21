package com.pagelence.core.content

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Frozen content schema, version 3.
 *
 * One book is one `book.json`. The classes read the shipped Doctor Dolittle
 * package (public domain worldwide). Field names and semantics are documented in
 * `work/android/docs/content-format.md`; changing them means bumping
 * [Book.schemaVersion] and migrating every built-in book.
 *
 * v3 removes the fields that duplicated the dictionary or the package manifest:
 * `units[].recap`, `units[].review`, `paragraphs[].guide`, `sentences[].glosses`,
 * `grammar.pointId`/`function`/`prompt`, `hints.italic`, `book.license` and
 * `source.publicDomain`. It adds `grammar.pointIds`, `hints.sentence` and
 * `sentences[].analysis`, and makes `quiz.feedback` a localized pair.
 */

/** A zh/en pair. Either side may be absent; at least one is present in practice. */
@Serializable
data class LocalizedText(
    val zh: String? = null,
    val en: String? = null,
) {
    fun forLanguage(language: String): String? =
        if (language == "en") en?.takeIf { it.isNotBlank() } ?: zh
        else zh?.takeIf { it.isNotBlank() } ?: en

    val isBlank: Boolean get() = zh.isNullOrBlank() && en.isNullOrBlank()
}

@Serializable
data class BookSource(
    val publisher: String? = null,
    val url: String? = null,
    val note: String? = null,
)

@Serializable
data class Book(
    val schemaVersion: Int = BOOK_SCHEMA_VERSION,
    val id: String,
    val title: LocalizedText,
    val author: String? = null,
    val edition: String? = null,
    val description: LocalizedText? = null,
    val source: BookSource? = null,
    val chapters: List<Chapter> = emptyList(),
)

@Serializable
data class Chapter(
    val id: String,
    val number: Int,
    val title: LocalizedText? = null,
    /** Editorial divisions that are not shown as numbered chapters. */
    val unnumbered: Boolean = false,
    val units: List<StudyUnit> = emptyList(),
)

@Serializable
data class StudyUnit(
    val id: String,
    val number: Int,
    val title: LocalizedText? = null,
    val introduction: LocalizedText? = null,
    val paragraphs: List<Paragraph> = emptyList(),
    val quiz: List<QuizItem> = emptyList(),
    val grammar: Grammar? = null,
)

@Serializable
data class Paragraph(
    val id: String,
    val text: String,
    /**
     * Sentences of [text] under the frozen [SentenceSplitter] rule. A skeleton
     * book may omit study annotations entirely, in which case this is empty.
     */
    val sentences: List<Sentence> = emptyList(),
    val hints: List<Hint> = emptyList(),
)

@Serializable
data class Sentence(
    val text: String,
    /** Chinese translation; absent means a content gap, not an error. */
    val zh: String? = null,
    /** Long-sentence analysis; optional, only for sentences above the book level. */
    val analysis: Analysis? = null,
)

/**
 * 长难句解析: main clause, its non-main parts, and how to read the Chinese order.
 * See the book package spec §5.8.
 */
@Serializable
data class Analysis(
    /** `A1`–`C2`; an editorial label, not a display switch. */
    val level: String,
    /** The main clause with subordinate clauses and modifiers removed. */
    val core: String,
    val parts: List<AnalysisPart> = emptyList(),
    val order: LocalizedText? = null,
    val note: LocalizedText? = null,
)

@Serializable
data class AnalysisPart(
    /** Verbatim text from the sentence. */
    val text: String,
    /** `subordinate`, `relative`, `nonfinite`, `prepositional`, `appositive` or `parenthetical`. */
    val role: String,
    val zh: String,
    val en: String? = null,
)

@Serializable
data class Hint(
    val id: String,
    /** `word`, `phrase` or `sentence`. */
    val kind: String = "word",
    val target: String,
    /**
     * 1-based sentence number inside the unit (same index as [QuizItem.sentence]).
     * Absent means the hint applies to the whole paragraph.
     */
    val sentence: Int? = null,
    val zh: String? = null,
    val en: String? = null,
    val lemma: String? = null,
    val pos: String? = null,
    val ipa: String? = null,
    val context: LocalizedText? = null,
    /** The word-card sentence containing this hint, with its translation. */
    val example: HintExample? = null,
)

@Serializable
data class HintExample(
    val en: String,
    val zh: String? = null,
)

@Serializable
data class QuizItem(
    val question: String,
    val options: List<String>,
    val answer: Int,
    /** 1-based sentence number inside the unit; absent when not linked. */
    val sentence: Int? = null,
    /** `{zh, en?}`; the English UI uses a generic line when `en` is absent. */
    val feedback: LocalizedText,
)

/**
 * Unit grammar: an anchor into the shared library plus the book's own reading
 * of the sentence. [focus] carries everything only this book can say — why the
 * sentence is written this way, and how it contrasts with nearby sentences.
 *
 * [pointIds] is ordered: the first id is the main point taught in this unit, the
 * rest are cross-links shown as "this sentence also uses". When the first id is
 * absent from the library, the book must supply [explain] + [form] + [focus].
 */
@Serializable
data class Grammar(
    val excerpt: String,
    val topic: String? = null,
    /** `new` or `review`; an editorial label, not shown in the reader. */
    val type: String? = null,
    val pointIds: List<String> = emptyList(),
    val form: LocalizedText? = null,
    val explain: LocalizedText? = null,
    val focus: LocalizedText? = null,
) {
    /** explain, form and focus all written inside the book. */
    val hasInlineAnalysis: Boolean
        get() = form.hasText() && explain.hasText() && focus.hasText()
}

/** A CEFR-J grammar point shipped with the app, one teacher-list item per entry. */
@Serializable
data class GrammarPoint(
    /** `cefrj-<teacher-list id>`; the suffix is the original CEFR-J item id. */
    val id: String,
    val name: LocalizedText,
    /** Introduction band start, e.g. `A2.1`. */
    val levelStart: String,
    /** Band end; absent means the same as [levelStart]. */
    val levelEnd: String? = null,
    /** `FREQ*DISP` level; selection aid only, never used for the UI badge. */
    val freqLevel: String? = null,
    /** Bidirectional cross-links, not a reason to merge entries. */
    val relatedIds: List<String> = emptyList(),
    /** When to use it, what it means, what the reader should notice; ≤120 汉字. */
    val explain: LocalizedText,
    /** Structure only: how it changes, no examples. */
    val form: LocalizedText,
    val pitfalls: List<LocalizedText> = emptyList(),
    /** 1–2 examples; shown on the detail page only, never in the unit lecture. */
    val examples: List<GrammarExample> = emptyList(),
) {
    /** `A2.1–B1.2`, or a single band when [levelEnd] is absent. */
    val levelLabel: String get() = if (levelEnd.isNullOrBlank() || levelEnd == levelStart) levelStart else "$levelStart–$levelEnd"
}

@Serializable
data class GrammarExample(
    val text: String,
    val zh: String? = null,
)

@Serializable
data class GrammarLibrary(
    val schemaVersion: Int = GRAMMAR_SCHEMA_VERSION,
    val updatedAt: String? = null,
    val source: String? = null,
    val license: String? = null,
    val points: List<GrammarPoint> = emptyList(),
)

/** One entry of `content/books-index.json` (the built-in book index). */
@Serializable
data class BookSummary(
    val id: String,
    val title: LocalizedText? = null,
    val path: String,
    val schemaVersion: Int = INDEX_SCHEMA_VERSION,
    val sizeBytes: Long = 0,
    val sha256: String? = null,
    val chapters: Int = 0,
    val units: Int = 0,
    val paragraphs: Int = 0,
    val sentences: Int = 0,
    val translatedSentences: Int = 0,
    val words: Int = 0,
    val level: String? = null,
    val levelNote: String? = null,
    val contentStatus: String? = null,
    /** Package creator shown on the book detail page. */
    val creator: String? = null,
    val category: LocalizedText? = null,
    val tags: List<LocalizedText> = emptyList(),
    /** Copyright/licence facts from the package manifest, shown on the info page. */
    val licenses: BookLicenses? = null,
)

/** One side of the package licences: the source text and the produced edition. */
@Serializable
data class BookLicense(
    /** `public-domain`, `permission`, `original` or an SPDX id. */
    val type: String? = null,
    val name: String? = null,
    val url: String? = null,
    val scope: String? = null,
    val note: String? = null,
    val publicDomain: List<String> = emptyList(),
)

@Serializable
data class BookLicenses(
    val source: BookLicense? = null,
    val creator: BookLicense? = null,
)

@Serializable
data class ContentManifest(
    val schemaVersion: Int = INDEX_SCHEMA_VERSION,
    val books: List<BookSummary> = emptyList(),
)

/** A zh/en pair that may carry extra keys in third-party source files. */
@Serializable
data class ThirdPartySource(
    val name: String? = null,
    val author: String? = null,
    val version: String? = null,
    val url: String? = null,
    val license: String? = null,
    val licenseUrl: String? = null,
    val language: String? = null,
    val changes: String? = null,
)

/** `book.json` format: v3 removed glosses, recap/review/guide and the old grammar fields. */
const val BOOK_SCHEMA_VERSION = 3

/** Built-in index / package manifest format; independent from `book.json`. */
const val INDEX_SCHEMA_VERSION = 2

/** Grammar library format; the CEFR-J migration in T113 keeps version 2. */
const val GRAMMAR_SCHEMA_VERSION = 2

/** Backwards-compatible alias for callers that mean the book format. */
const val CONTENT_SCHEMA_VERSION = BOOK_SCHEMA_VERSION

/** The one JSON configuration used for content, progress and sync state. */
val PagelenceJson: Json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
    prettyPrint = false
}

/** True when the pair exists and is not blank. */
fun LocalizedText?.hasText(): Boolean = this != null && !this.isBlank
