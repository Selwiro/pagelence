package com.pagelence.presentation.shell

import com.pagelence.core.content.ContentRepository
import com.pagelence.core.prefs.Preferences
import com.pagelence.core.progress.ProgressRepository
import com.pagelence.core.progress.ProgressSemantics
import com.pagelence.core.wordcards.WordCard
import com.pagelence.core.wordcards.WordCardRepository
import com.pagelence.uim3.UiLanguage
import com.pagelence.uim3.model.FinishedBookUi
import com.pagelence.uim3.model.SettingsUiState
import com.pagelence.uim3.model.WordCardUi
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Preferences → settings screen state. Trivial, but shared with a second shell. */
class SettingsPresenter(private val preferences: Preferences) {
    fun state(): SettingsUiState = SettingsUiState(
        language = language,
        theme = preferences.theme,
        fontSize = preferences.fontSize,
        readerLayout = preferences.readerLayout,
        annotateHints = preferences.annotateHints,
        definitionMode = preferences.definitionMode,
        guideMode = preferences.guideMode,
        speechRate = preferences.speechRate,
    )

    private val language: UiLanguage =
        if (preferences.language == com.pagelence.core.prefs.Language.EN) UiLanguage.EN else UiLanguage.ZH
}

/** The ending page: what the book was worth once it is finished. */
class FinishedPresenter(
    private val content: ContentRepository,
    private val progress: ProgressRepository,
    private val cards: WordCardRepository,
    private val language: UiLanguage,
) {
    fun finished(bookId: String): FinishedBookUi? {
        val index = content.index(bookId) ?: return null
        val saved = progress.progress(bookId)
        val restored = ProgressSemantics.restore(saved, index)
        if (restored != saved) progress.save(restored)
        val book = index.book
        return FinishedBookUi(
            bookId = book.id,
            title = titleFor(book.title),
            coverTitle = book.title.en ?: book.title.zh ?: book.id,
            monogram = (book.title.zh ?: book.title.en ?: book.id).take(1),
            author = book.author,
            unitCount = index.units.size,
            wordCount = index.stats().words,
            cardCount = cards.list(book.id).size,
            finishedOn = formatDate(restored.finishedAt),
            coverPath = content.coverPath(book.id),
        )
    }

    private fun titleFor(title: com.pagelence.core.content.LocalizedText): String = when (language) {
        UiLanguage.ZH -> title.zh ?: title.en ?: ""
        UiLanguage.EN -> title.en ?: title.zh ?: ""
    }

    private fun formatDate(value: String?): String? {
        val instant = value?.let { runCatching { Instant.parse(it) }.getOrNull() } ?: return null
        return DateTimeFormatter.ofPattern("yyyy-MM-dd")
            .withZone(ZoneId.systemDefault())
            .format(instant)
    }
}

/** Collected word cards, newest last, with the book title resolved for each row. */
class WordCardPresenter(
    private val content: ContentRepository,
    private val cards: WordCardRepository,
    private val language: UiLanguage,
) {
    fun cards(): List<WordCardUi> {
        val titles = content.library().associate { summary ->
            summary.id to (summary.title?.let { titleFor(it) } ?: summary.id)
        }
        // Cards of a book that is no longer shipped stay in state.json but are
        // not listed; that keeps an upgrade from showing cards it cannot open.
        return cards.all().mapNotNull { card ->
            titles[card.bookId]?.let { title -> cardUi(card, title) }
        }
    }

    private fun cardUi(card: WordCard, bookTitle: String): WordCardUi = WordCardUi(
        id = card.id,
        term = card.term,
        lemma = card.lemma,
        ipa = card.ipa,
        zh = card.zh,
        en = card.en,
        exampleEn = card.example?.en,
        exampleZh = card.example?.zh,
        bookId = card.bookId,
        bookTitle = bookTitle,
    )

    private fun titleFor(title: com.pagelence.core.content.LocalizedText): String = when (language) {
        UiLanguage.ZH -> title.zh ?: title.en ?: ""
        UiLanguage.EN -> title.en ?: title.zh ?: ""
    }
}
