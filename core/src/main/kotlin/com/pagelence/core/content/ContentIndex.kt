package com.pagelence.core.content

/**
 * Read-only, in-memory index over one book. Reading, paging and progress all
 * go through the unit order and paragraph ids defined here.
 */
class ContentIndex(val book: Book) {
    val units: List<StudyUnit> = book.chapters.flatMap { it.units }

    val unitById: Map<String, StudyUnit> = units.associateBy { it.id }
    val unitIds: Set<String> = unitById.keys

    val chapterByUnitId: Map<String, Chapter> = book.chapters.flatMap { chapter ->
        chapter.units.map { unit -> unit.id to chapter }
    }.toMap()

    val paragraphs: List<Paragraph> = units.flatMap { it.paragraphs }
    val paragraphById: Map<String, Paragraph> = paragraphs.associateBy { it.id }
    val unitByParagraphId: Map<String, StudyUnit> = units.flatMap { unit ->
        unit.paragraphs.map { paragraph -> paragraph.id to unit }
    }.toMap()

    val firstParagraphId: String? = paragraphs.firstOrNull()?.id

    fun unit(unitId: String): StudyUnit? = unitById[unitId]

    fun unitOf(paragraphId: String): StudyUnit? = unitByParagraphId[paragraphId]

    fun nextUnit(unitId: String): StudyUnit? {
        val position = units.indexOfFirst { it.id == unitId }
        return if (position >= 0 && position + 1 < units.size) units[position + 1] else null
    }

    fun previousUnit(unitId: String): StudyUnit? {
        val position = units.indexOfFirst { it.id == unitId }
        return if (position > 0) units[position - 1] else null
    }

    fun sentenceCount(unitId: String): Int = unit(unitId)?.paragraphs?.sumOf { it.sentences.size } ?: 0

    /**
     * Sentences of [paragraph]. Skeleton books store only the paragraph text;
     * fall back to the frozen splitter so those units are still readable and
     * tappable while their study edition is pending.
     */
    fun sentences(paragraph: Paragraph): List<Sentence> =
        paragraph.sentences.ifEmpty {
            SentenceSplitter.split(paragraph.text).map { Sentence(it) }
        }

    /** The 1-based sentence inside the unit, or null when out of range. */
    fun sentence(unitId: String, index: Int): Sentence? {
        val unit = unit(unitId) ?: return null
        var position = 0
        for (paragraph in unit.paragraphs) {
            for (sentence in sentences(paragraph)) {
                position += 1
                if (position == index) return sentence
            }
        }
        return null
    }

    fun stats(): BookStats {
        val paragraphs = paragraphs
        return BookStats(
            chapters = book.chapters.size,
            units = units.size,
            paragraphs = paragraphs.size,
            sentences = paragraphs.sumOf { it.sentences.size },
            translatedSentences = paragraphs.sumOf { paragraph ->
                paragraph.sentences.count { !it.zh.isNullOrBlank() }
            },
            words = paragraphs.sumOf { SentenceSplitter.words(it.text) },
        )
    }
}

data class BookStats(
    val chapters: Int,
    val units: Int,
    val paragraphs: Int,
    val sentences: Int,
    val translatedSentences: Int,
    val words: Int,
)
