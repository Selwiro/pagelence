package com.pagelence.core.content

import com.pagelence.core.ContentFixtures
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/** Golden cases for the frozen splitter shared with the Python tooling. */
class SentenceSplitterTest {
    @Test
    fun `splits on sentence punctuation followed by a capital`() {
        assertEquals(
            listOf("He was an old man.", "He fished alone.", "He was patient."),
            SentenceSplitter.split("He was an old man. He fished alone. He was patient."),
        )
    }

    @Test
    fun `keeps single-letter initials and lowercase continuations`() {
        assertEquals(
            listOf("J. R. Tolkien wrote it.", "It sold well."),
            SentenceSplitter.split("J. R. Tolkien wrote it. It sold well."),
        )
        assertEquals(
            listOf("Use e.g. this one.", "Do not use i.e. that."),
            SentenceSplitter.split("Use e.g. this one. Do not use i.e. that."),
        )
    }

    @Test
    fun `dotted abbreviations before a capital split - frozen limitation`() {
        // Documented in SentenceSplitter and docs/content-format.md. The Web
        // translations were authored against this behaviour, so it is frozen
        // until a later rule version re-translates the pilot units.
        assertEquals(
            listOf("Mr.", "Smith met Dr.", "Adams at St.", "Mary's. They talked."),
            SentenceSplitter.split("Mr. Smith met Dr. Adams at St. Mary's. They talked."),
        )
    }

    @Test
    fun `handles closing quotes after the full stop`() {
        assertEquals(
            listOf("He said, “I will go.”", "Then he left."),
            SentenceSplitter.split("He said, “I will go.” Then he left."),
        )
    }

    @Test
    fun `keeps the tail after the last punctuation mark`() {
        assertEquals(listOf("Wait!", "What happened?"), SentenceSplitter.split("Wait! What happened?"))
        assertEquals(listOf("No closing stop"), SentenceSplitter.split("No closing stop"))
    }

    @Test
    fun `lowercase continuation does not end a sentence`() {
        assertEquals(
            listOf("He arrived at 5 p.m. and left at six."),
            SentenceSplitter.split("He arrived at 5 p.m. and left at six."),
        )
    }

    @Test
    fun `re-splitting the whole book keeps every word`() {
        val repository = FileContentRepository(ContentFixtures.contentDir)
        val index = assertNotNull(repository.index(ContentFixtures.dolittle))
        val sentences = index.paragraphs.flatMap { SentenceSplitter.split(it.text) }
        assertEquals(
            index.paragraphs.joinToString(" ") { it.text.trim() },
            sentences.joinToString(" "),
        )
    }

    @Test
    fun `word count matches the manifest rule`() {
        assertEquals(4, SentenceSplitter.words("The old man's boat."))
        assertEquals(3, SentenceSplitter.words("don’t stop now"))
    }
}
