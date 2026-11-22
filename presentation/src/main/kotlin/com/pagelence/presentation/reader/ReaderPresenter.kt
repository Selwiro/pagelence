package com.pagelence.presentation.reader

import com.pagelence.core.content.Analysis
import com.pagelence.core.content.ContentIndex
import com.pagelence.core.content.ContentRepository
import com.pagelence.core.content.Grammar
import com.pagelence.core.content.GrammarPoint
import com.pagelence.core.content.HintExample
import com.pagelence.core.content.LocalizedText
import com.pagelence.core.content.Sentence
import com.pagelence.core.content.StudyUnit
import com.pagelence.core.prefs.Preferences
import com.pagelence.core.progress.AnnotationRepository
import com.pagelence.core.progress.AnnotationRules
import com.pagelence.core.progress.BookProgress
import com.pagelence.core.progress.InMemoryAnnotationRepository
import com.pagelence.core.progress.ReaderPhase
import com.pagelence.core.wordcards.WordCardRepository
import com.pagelence.presentation.annotations.AnnotationPresenter
import com.pagelence.uim3.Strings
import com.pagelence.uim3.UiLanguage
import com.pagelence.uim3.model.AnalysisPartUi
import com.pagelence.uim3.model.AnalysisUi
import com.pagelence.uim3.model.GrammarRelatedUi
import com.pagelence.uim3.model.GrammarUi
import com.pagelence.uim3.model.ParagraphUi
import com.pagelence.uim3.model.QuizQuestionUi
import com.pagelence.uim3.model.ReaderUiState
import com.pagelence.uim3.model.SentenceUi
import com.pagelence.uim3.model.TocChapterUi
import com.pagelence.uim3.model.TocUnitUi

/**
 * Builds one unit's [ReaderUiState] from the content index, the reader's
 * progress and their saved answers. Android-free: an e-ink shell renders the
 * same state with its own widgets.
 *
 * The shell owns prefs/language and the quiz globals; it passes them in so this
 * class has no mutable session of its own.
 */
class ReaderPresenter(
    private val content: ContentRepository,
    private val cards: WordCardRepository,
    private val strings: Strings,
    private val language: UiLanguage,
    private val preferences: Preferences,
    private val bookLevel: String?,
    private val annotations: AnnotationRepository = InMemoryAnnotationRepository(),
) {
/**
 * A hint worth saving after a quiz: it occurs in a sentence a question points
 * at and is not yet in the card list.
 */
data class QuizWordCandidate(
    val paragraphId: String,
    val hintId: String,
    val term: String,
    val lemma: String,
    val pos: String? = null,
    val zh: String? = null,
    val en: String? = null,
    val ipa: String? = null,
    val example: com.pagelence.core.content.HintExample? = null,
)
    fun unit(
        bookIndex: ContentIndex,
        unit: StudyUnit,
        progress: BookProgress,
        quizAnswers: Map<Int, Int>,
        quizSubmitted: Boolean,
        quizCollected: Int,
        startAtText: Boolean = false,
        startAtGuide: Boolean = false,
        startAtGrammar: Boolean = false,
    ): ReaderUiState {
        val prefs = preferences
        val book = bookIndex.book
        val chapter = bookIndex.chapterByUnitId[unit.id]
        val storedAnnotations = annotations.list(book.id)
        val annotationPresenter = AnnotationPresenter(content, language)
        val marks = annotationPresenter.marks(storedAnnotations, book.id, unit.id)
        var number = 0
        val sentenceByNumber = mutableMapOf<Int, Sentence>()
        val paragraphs = unit.paragraphs.map { paragraph ->
            // Sentence-level hints explain a whole line; underlining the line
            // would look like an error, so only words and phrases are marked.
            val wordHints = paragraph.hints.filter { it.kind != "sentence" }
            val sentences = buildList {
                bookIndex.sentences(paragraph).forEach { sentence ->
                    number += 1
                    sentenceByNumber[number] = sentence
                    // A hint with a sentence number underlines that sentence
                    // only; otherwise the same spelling elsewhere in the
                    // paragraph would look tappable but open nothing.
                    val annotated = wordHints
                        .filter { it.sentence == null || it.sentence == number }
                        .mapTo(mutableSetOf()) { it.target }
                    add(
                        SentenceUi(
                            number = number,
                            text = sentence.text,
                            zh = if (language == UiLanguage.EN) null else sentence.zh,
                            annotated = annotated,
                            analysis = sentence.analysis?.let { toAnalysisUi(it, language) },
                            mark = marks[AnnotationRules.sentenceKey(paragraph.id, number)],
                        ),
                    )
                }
            }
            ParagraphUi(
                id = paragraph.id,
                sentences = sentences,
            )
        }
        val quiz = unit.quiz.mapIndexed { position, item ->
            val sentence = item.sentence?.let { sentenceByNumber[it] }
            QuizQuestionUi(
                question = item.question,
                options = item.options,
                selected = quizAnswers[position],
                correct = item.answer,
                feedback = if (language == UiLanguage.EN) {
                    item.feedback.en?.takeIf { it.isNotBlank() } ?: strings.quizFeedbackGeneric
                } else {
                    item.feedback.zh
                },
                revealed = quizSubmitted,
                sentenceNumber = item.sentence,
                sentenceText = sentence?.text,
                sentenceZh = if (language == UiLanguage.EN) null else sentence?.zh,
            )
        }
        val collectible = quizCandidates(unit).count { candidate ->
            cards.find(book.id, candidate.lemma, candidate.term) == null
        }
        // The contents sheet numbers units across the whole book, matching the
        // reader's "position / total" label; chapter-local numbers restart at 1
        // and make a free jump feel inconsistent.
        val unitPositions = bookIndex.units
            .mapIndexed { position, item -> item.id to position + 1 }
            .toMap()
        val resume = progress.reader?.takeIf { it.unitId == unit.id }
        val showGuide = showGuideInitially(book, unit, progress, prefs)
        val hasGuide = unit.introduction?.let { !it.zh.isNullOrBlank() || !it.en.isNullOrBlank() } == true
        return ReaderUiState(
            bookId = book.id,
            unitId = unit.id,
            bookTitle = titleFor(book.title),
            chapterTitle = chapter?.title?.let { titleFor(it) } ?: "",
            unitTitle = unit.title?.let { titleFor(it) } ?: strings.unit.format(unit.number),
            chapterNumber = chapter?.number ?: 0,
            unitNumber = unit.number,
            unitPosition = bookIndex.units.indexOfFirst { it.id == unit.id } + 1,
            unitCount = bookIndex.units.size,
            introductionZh = if (language == UiLanguage.EN) null else unit.introduction?.zh,
            introductionEn = unit.introduction?.en,
            showGuideInitially = (showGuide || startAtGuide) && !startAtText,
            paragraphs = paragraphs,
            startParagraphId = progress.paragraphId?.takeIf { pid ->
                unit.paragraphs.any { it.id == pid }
            },
            quiz = quiz,
            quizSubmitted = quizSubmitted,
            quizScore = quiz.indices.count { quizAnswers[it] == quiz[it].correct },
            quizCollectibleCount = collectible,
            quizCollectedCount = quizCollected,
            grammar = unit.grammar?.let { grammarUi(it) },
            unitRead = unit.id in progress.readUnitIds,
            hasNextUnit = bookIndex.nextUnit(unit.id) != null,
            hasPreviousUnit = bookIndex.previousUnit(unit.id) != null,
            resumePhase = when {
                startAtText -> ReaderPhase.TEXT
                startAtGrammar -> ReaderPhase.GRAMMAR
                startAtGuide && hasGuide -> ReaderPhase.GUIDE
                resume != null -> resume.phase
                showGuide && hasGuide -> ReaderPhase.GUIDE
                else -> ReaderPhase.TEXT
            },
            resumeTextPage = resume?.textPage ?: 0,
            resumeTextProgress = resume?.textProgress ?: 0f,
            bookLevel = bookLevel,
            preview = null,
            preference = prefs,
            language = language,
            wordPanel = null,
            wordStrip = null,
            selectedWord = null,
            cardsCount = cards.list(book.id).size,
            speechNotice = null,
            annotations = annotationPresenter.book(storedAnnotations, book.id),
            toc = book.chapters.map { chapter ->
                TocChapterUi(
                    id = chapter.id,
                    number = chapter.number,
                    title = chapter.title?.let { titleFor(it) }.orEmpty(),
                    units = chapter.units.map { tocUnit ->
                        val position = unitPositions[tocUnit.id] ?: tocUnit.number
                        TocUnitUi(
                            id = tocUnit.id,
                            number = position,
                            title = tocUnit.title?.let { titleFor(it) }
                                ?: strings.unit.format(position),
                            read = tocUnit.id in progress.readUnitIds,
                            current = tocUnit.id == unit.id,
                        )
                    },
                )
            },
        )
    }

    /** Book CEFR band from the built-in shelf or a source catalog; self-level is the fallback. */

    private fun toAnalysisUi(analysis: Analysis, language: UiLanguage): AnalysisUi {
        fun pick(text: com.pagelence.core.content.LocalizedText?): String? =
            if (language == UiLanguage.EN) text?.en?.takeIf { it.isNotBlank() }
            else text?.zh?.takeIf { it.isNotBlank() } ?: text?.en?.takeIf { it.isNotBlank() }
        return AnalysisUi(
            level = analysis.level,
            core = analysis.core,
            parts = analysis.parts.map { part ->
                AnalysisPartUi(
                    text = part.text,
                    role = part.role,
                    description = if (language == UiLanguage.EN) {
                        part.en?.takeIf { it.isNotBlank() }
                    } else {
                        part.zh
                    },
                )
            },
            order = pick(analysis.order),
            note = pick(analysis.note),
        )
    }

    /** Grammar card for the detail page; same mapping the reader shows. */
    fun grammar(grammar: Grammar): GrammarUi = grammarUi(grammar)

    private fun grammarUi(grammar: Grammar): GrammarUi {
        val primaryPointId = grammar.pointIds.firstOrNull()
        val point: GrammarPoint? = primaryPointId?.let(content::grammarPoint)
        val extra = grammar.pointIds.drop(1).mapNotNull { id ->
            content.grammarPoint(id)?.let { related ->
                GrammarRelatedUi(id, related.name.let { textFor(it) }.orEmpty())
            }
        }
        return GrammarUi(
            excerpt = grammar.excerpt,
            topic = grammar.topic,
            pointId = primaryPointId,
            pointName = point?.name?.let { textFor(it) },
            levelLabel = point?.levelLabel,
            explain = grammar.explain?.let { textFor(it) }
                ?: point?.explain?.let { textFor(it) },
            form = grammar.form?.let { textFor(it) }
                ?: point?.form?.let { textFor(it) },
            pitfalls = point?.pitfalls?.mapNotNull { textFor(it) }.orEmpty(),
            focus = grammar.focus?.let { textFor(it) },
            extraPoints = extra,
        )
    }

    fun quizCandidates(unit: StudyUnit): List<QuizWordCandidate> {
        val targetSentences = unit.quiz.mapNotNull { it.sentence }.toSet()
        if (targetSentences.isEmpty()) return emptyList()
        val found = LinkedHashMap<String, QuizWordCandidate>()
        var number = 0
        for (paragraph in unit.paragraphs) {
            for (sentence in paragraph.sentences) {
                number += 1
                if (number !in targetSentences) continue
                paragraph.hints.filter { it.kind != "sentence" }.forEach { hint ->
                    if (!sentence.text.contains(hint.target, ignoreCase = true)) return@forEach
                    val key = "${paragraph.id}::${hint.id}"
                    found.putIfAbsent(
                        key,
                        QuizWordCandidate(
                            paragraphId = paragraph.id,
                            hintId = hint.id,
                            term = hint.target,
                            lemma = hint.lemma ?: hint.target.lowercase(),
                            pos = hint.pos,
                            zh = hint.zh,
                            en = hint.en,
                            ipa = hint.ipa,
                            example = hint.example,
                        ),
                    )
                }
            }
        }
        return found.values.toList()
    }

    private fun showGuideInitially(
        book: com.pagelence.core.content.Book,
        unit: StudyUnit,
        progress: BookProgress,
        prefs: Preferences,
    ): Boolean {
        val first = unit.paragraphs.firstOrNull()?.id ?: return false
        val atStart = progress.paragraphId == null || progress.paragraphId == first
        val seen = "${book.id}::${unit.id}" in prefs.seenGuideUnits
        return !seen && unit.id !in progress.readUnitIds && atStart && unit.introduction != null
    }

    private fun titleFor(title: com.pagelence.core.content.LocalizedText): String = when (language) {
        UiLanguage.ZH -> title.zh ?: title.en ?: ""
        UiLanguage.EN -> title.en ?: title.zh ?: ""
    }

    private fun textFor(text: com.pagelence.core.content.LocalizedText?): String? = when (language) {
        UiLanguage.ZH -> text?.zh ?: text?.en
        UiLanguage.EN -> text?.en ?: text?.zh
    }?.takeIf { it.isNotBlank() }
}
