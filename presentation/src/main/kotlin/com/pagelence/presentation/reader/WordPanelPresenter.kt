package com.pagelence.presentation.reader

import com.pagelence.core.content.ContentIndex
import com.pagelence.core.content.ContentRepository
import com.pagelence.core.content.Hint
import com.pagelence.core.content.HintTapResolver
import com.pagelence.core.content.Paragraph
import com.pagelence.core.lexicon.WordLookup
import com.pagelence.core.reading.SelectedTerm
import com.pagelence.uim3.Strings
import com.pagelence.uim3.model.SelectedWordUi
import com.pagelence.uim3.model.SenseUi
import com.pagelence.uim3.model.WordPanelUi
import com.pagelence.uim3.model.WordStripUi

/** The panel and the highlighted term produced by one tap. */
data class WordPanelResult(
    val panel: WordPanelUi,
    val selected: SelectedTerm,
)

/**
 * Turns a tap in the text into the word entry the reader shows.
 *
 * Android-free: both shells report the same paragraph/sentence/character range
 * and render the returned panel. The book's hint wins over the tapped word so
 * phrases stay reachable, and dictionary/level/occurrence data comes from
 * [ContentRepository].
 */
class WordPanelPresenter(
    private val content: ContentRepository,
    private val strings: Strings,
) {
    fun open(
        index: ContentIndex,
        bookId: String,
        unitId: String,
        paragraphId: String,
        sentenceNumber: Int,
        spelling: String,
        start: Int,
        end: Int,
    ): WordPanelResult? {
        val paragraph = index.paragraphById[paragraphId] ?: return null
        val sentence = index.sentence(unitId, sentenceNumber) ?: return null
        // A dashed hint may cover several words. The tap handler reports only
        // one word, so promote it to the whole target before lookup; otherwise
        // the phrase's own explanation is unreachable.
        val tap = HintTapResolver.resolve(
            sentenceText = sentence.text,
            sentenceNumber = sentenceNumber,
            hints = paragraph.hints,
            wordStart = start,
            wordEnd = end,
        )
        val term = tap?.target ?: spelling
        val lookup = content.lookup(bookId, unitId, paragraphId, sentenceNumber, term) ?: return null
        val hint = tap?.hint
            ?: paragraph.hints.firstOrNull { it.target.equals(term, ignoreCase = true) }
        val panel = panel(
            paragraph = paragraph,
            hint = hint,
            lookup = lookup,
            sentenceNumber = sentenceNumber,
            sentenceZh = sentence.zh,
        )
        return WordPanelResult(
            panel = panel,
            selected = SelectedTerm(
                paragraphId = paragraphId,
                sentenceNumber = sentenceNumber,
                spelling = term,
                term = panel.term,
                ipa = panel.ipa,
                gloss = panel.gloss,
                start = tap?.start ?: start,
                end = tap?.end ?: end,
            ),
        )
    }

    private fun panel(
        paragraph: Paragraph,
        hint: Hint?,
        lookup: WordLookup,
        sentenceNumber: Int,
        sentenceZh: String?,
    ): WordPanelUi = WordPanelUi(
        term = lookup.term,
        lemma = lookup.lemma,
        pos = lookup.pos,
        ipa = lookup.ipa,
        level = lookup.level,
        levelApprox = lookup.levelApprox,
        levelSource = lookup.levelSource,
        gloss = lookup.glossZh,
        emptyLabel = when (lookup.label) {
            "人名" -> strings.properName
            "地名" -> strings.properPlace
            "专名" -> strings.properNoun
            "未收录释义" -> if (lookup.glossZh.isNullOrBlank()) {
                strings.noDefinition
            } else {
                // The book explains this phrase even though ECDICT has no row;
                // "未收录释义" next to a filled 「本句释义」 only reads as an error.
                null
            }
            else -> null
        },
        note = lookup.note,
        exampleEn = hint?.example?.en,
        exampleZh = hint?.example?.zh,
        sentenceZh = sentenceZh,
        zipf = lookup.zipf,
        frequencyBand = lookup.frequencyBand?.name?.lowercase(),
        occurrences = lookup.bookOccurrences,
        senses = lookup.definitions.map { sense ->
            SenseUi(
                pos = sense.pos,
                definition = sense.definition,
                language = sense.language,
                fromSurface = sense.fromSurface,
            )
        },
        collected = lookup.inWordCards,
        paragraphId = paragraph.id,
        sentenceNumber = sentenceNumber,
        hintId = hint?.id,
    )
}

/** Top-strip summary for the selected term (shared by both shells). */
fun SelectedTerm.toStripUi(): WordStripUi = WordStripUi(
    paragraphId = paragraphId,
    sentenceNumber = sentenceNumber,
    spelling = spelling,
    term = term,
    ipa = ipa,
    gloss = gloss,
)

/** Highlight range for the selected term. */
fun SelectedTerm.toSelectedUi(): SelectedWordUi = SelectedWordUi(
    paragraphId = paragraphId,
    sentenceNumber = sentenceNumber,
    spelling = spelling,
    start = start,
    end = end,
)
