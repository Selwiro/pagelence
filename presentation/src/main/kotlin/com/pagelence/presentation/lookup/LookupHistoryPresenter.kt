package com.pagelence.presentation.lookup

import com.pagelence.core.content.ContentOutline
import com.pagelence.core.content.ContentRepository
import com.pagelence.core.content.LocalizedText
import com.pagelence.core.lookup.LookupHistoryRepository
import com.pagelence.uim3.UiLanguage
import com.pagelence.uim3.model.LookupHistoryUi
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Maps stored lookups to the 复习 → 查词 list (T161).
 *
 * The book title and unit title come from the lightweight outline, not the book
 * body, so opening the review tab never parses a whole book. A lookup whose
 * book is no longer installed stays visible but marked unavailable, which is
 * clearer than silently dropping it or opening a broken reader.
 */
class LookupHistoryPresenter(
    private val content: ContentRepository,
    private val lookups: LookupHistoryRepository,
    private val language: UiLanguage,
    private val zone: ZoneId = ZoneId.systemDefault(),
) {
    private val dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(zone)

    fun list(): List<LookupHistoryUi> {
        val entries = lookups.all()
        if (entries.isEmpty()) return emptyList()
        val summaries = content.library().associateBy { it.id }
        val outlines = HashMap<String, ContentOutline?>()
        return entries.map { entry ->
            val summary = summaries[entry.bookId]
            val outline = outlines.getOrPut(entry.bookId) { content.outline(entry.bookId) }
            val unit = outline?.unit(entry.unitId)
            LookupHistoryUi(
                id = entry.id,
                bookId = entry.bookId,
                unitId = entry.unitId,
                paragraphId = entry.paragraphId,
                sentenceNumber = entry.sentenceNumber,
                term = entry.term,
                lemma = entry.lemma,
                gloss = entry.gloss,
                ipa = entry.ipa,
                start = entry.start,
                end = entry.end,
                bookTitle = summary?.title?.let(::title) ?: entry.bookId,
                unitLabel = unit?.title?.let(::title)
                    ?: unit?.number?.let { "#$it" }
                    ?: entry.unitId,
                lookedUpAtLabel = format(entry.lookedUpAt),
                available = summary != null && outline != null,
            )
        }
    }

    private fun title(text: LocalizedText): String = when (language) {
        UiLanguage.ZH -> text.zh ?: text.en ?: ""
        UiLanguage.EN -> text.en ?: text.zh ?: ""
    }

    private fun format(value: String): String {
        val instant = runCatching { Instant.parse(value) }.getOrNull() ?: return value
        return dateFormat.format(instant)
    }
}
