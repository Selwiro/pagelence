package com.pagelence.presentation.pagination

import com.pagelence.uim3.model.AnalysisPartUi
import com.pagelence.uim3.model.AnalysisUi
import com.pagelence.uim3.model.ParagraphUi
import com.pagelence.uim3.model.SentenceMarkUi
import com.pagelence.uim3.model.SentenceUi
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class PaginationFingerprintTest {
    @Test
    fun sameContentHasTheSameFingerprint() {
        assertEquals(PaginationFingerprint.of(unit()), PaginationFingerprint.of(unit()))
    }

    @Test
    fun textChangeOfEqualLengthChangesFingerprint() {
        val changed = unit(text = "abce")
        assertNotEquals(PaginationFingerprint.of(unit()), PaginationFingerprint.of(changed))
    }

    @Test
    fun addingASentenceTranslationChangesFingerprint() {
        val changed = unit(zh = "从前有一个人。")
        assertNotEquals(PaginationFingerprint.of(unit()), PaginationFingerprint.of(changed))
    }

    @Test
    fun addingAnalysisChangesFingerprint() {
        val analysis = AnalysisUi(
            level = "B1",
            core = "主句",
            parts = listOf(AnalysisPartUi("abcd", "main", "main clause")),
            order = null,
            note = null,
        )
        assertNotEquals(
            PaginationFingerprint.of(unit()),
            PaginationFingerprint.of(unit(analysis = analysis)),
        )
    }

    @Test
    fun marksDoNotChangeFingerprint() {
        // T144 draws bookmark/highlight/note markers after the text, so adding
        // one must not invalidate the cached pagination.
        val marked = unit().map { paragraph ->
            paragraph.copy(
                sentences = paragraph.sentences.map { sentence ->
                    sentence.copy(
                        mark = SentenceMarkUi(
                            bookmarked = true,
                            highlighted = true,
                            note = "note",
                        ),
                    )
                },
            )
        }
        assertEquals(PaginationFingerprint.of(unit()), PaginationFingerprint.of(marked))
    }

    @Test
    fun regroupingSentencesChangesFingerprint() {
        val oneParagraph = listOf(
            ParagraphUi(
                id = "p1",
                sentences = listOf(sentence(1, "abcd"), sentence(2, "efgh")),
            ),
        )
        val twoParagraphs = listOf(
            ParagraphUi(id = "p1", sentences = listOf(sentence(1, "abcd"))),
            ParagraphUi(id = "p2", sentences = listOf(sentence(2, "efgh"))),
        )
        assertNotEquals(
            PaginationFingerprint.of(oneParagraph),
            PaginationFingerprint.of(twoParagraphs),
        )
    }

    @Test
    fun renumberingSentencesChangesFingerprint() {
        assertNotEquals(
            PaginationFingerprint.of(unit(number = 1)),
            PaginationFingerprint.of(unit(number = 2)),
        )
    }

    private fun unit(
        number: Int = 1,
        text: String = "abcd",
        zh: String? = null,
        analysis: AnalysisUi? = null,
    ): List<ParagraphUi> = listOf(
        ParagraphUi(
            id = "p1",
            sentences = listOf(sentence(number, text, zh, analysis)),
        ),
    )

    private fun sentence(
        number: Int,
        text: String,
        zh: String? = null,
        analysis: AnalysisUi? = null,
    ) = SentenceUi(
        number = number,
        text = text,
        zh = zh,
        annotated = emptySet(),
        analysis = analysis,
    )
}
