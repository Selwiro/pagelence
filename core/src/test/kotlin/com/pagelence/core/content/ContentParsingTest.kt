package com.pagelence.core.content

import com.pagelence.core.ContentFixtures
import java.security.MessageDigest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** The shipped package must be readable and structurally complete. */
class ContentParsingTest {
    private val repository = FileContentRepository(ContentFixtures.contentDir)

    @Test
    fun `manifest describes the shipped books with matching size and sha`() {
        val manifest = FileContentRepository.loadManifest(ContentFixtures.contentDir)
        assertEquals(
            listOf(ContentFixtures.dolittle, ContentFixtures.alice).sorted(),
            manifest.books.map { it.id }.sorted(),
        )
        manifest.books.forEach { summary ->
            val bytes = ContentFixtures.contentDir.resolve(summary.path).toFile().readBytes()
            assertEquals(bytes.size.toLong(), summary.sizeBytes, summary.id)
            assertEquals(sha256(bytes), summary.sha256, summary.id)
            assertEquals(INDEX_SCHEMA_VERSION, summary.schemaVersion, summary.id)
        }
    }

    @Test
    fun `alice is complete and every unit is annotated`() {
        val book = assertNotNull(repository.book(ContentFixtures.alice))
        assertEquals(12, book.chapters.size)
        val index = ContentIndex(book)
        assertEquals(104, index.units.size)
        assertEquals(779, index.paragraphs.size)
        val stats = index.stats()
        assertEquals(1359, stats.sentences)
        assertEquals(1359, stats.translatedSentences)
        assertEquals(104, index.units.count { it.quiz.isNotEmpty() })
        assertEquals(104, index.units.count { it.grammar != null })
        assertTrue(index.units.all { it.title.hasText() && it.introduction.hasText() })
        assertTrue(index.paragraphs.all { p -> p.sentences.isNotEmpty() })
        assertTrue(book.chapters.first().units.all { it.quiz.size == 2 })
    }

    @Test
    fun `unit navigation walks the whole book and stops at both ends`() {
        val index = assertNotNull(repository.index(ContentFixtures.alice))
        assertNull(index.previousUnit(index.units.first().id))
        assertNull(index.nextUnit(index.units.last().id))
        index.units.forEachIndexed { position, unit ->
            assertEquals(index.units.getOrNull(position - 1)?.id, index.previousUnit(unit.id)?.id)
            assertEquals(index.units.getOrNull(position + 1)?.id, index.nextUnit(unit.id)?.id)
        }
    }

    @Test
    fun `alice sentences follow the frozen splitter`() {
        val index = assertNotNull(repository.index(ContentFixtures.alice))
        for (paragraph in index.paragraphs) {
            assertEquals(
                paragraph.sentences.map { it.text },
                SentenceSplitter.split(paragraph.text),
                "${paragraph.id} sentence split drifted",
            )
            assertTrue(paragraph.sentences.all { !it.zh.isNullOrBlank() })
        }
    }

    @Test
    fun `shipped book has the expected structure`() {
        val book = assertNotNull(repository.book(ContentFixtures.dolittle))
        assertEquals(CONTENT_SCHEMA_VERSION, book.schemaVersion)
        assertEquals("怪医杜立德", book.title.zh)
        assertEquals("Hugh Lofting", book.author)
        assertEquals(21, book.chapters.size)

        val index = ContentIndex(book)
        assertEquals(103, index.units.size)
        assertEquals(729, index.paragraphs.size)
        val stats = index.stats()
        assertEquals(1724, stats.sentences)
        assertEquals(1724, stats.translatedSentences)
        assertEquals(103, index.units.count { it.quiz.isNotEmpty() })
        assertEquals(206, index.units.sumOf { it.quiz.size })

        val firstChapter = book.chapters.first()
        assertTrue(firstChapter.units.all { it.introduction.hasText() })
    }

    @Test
    fun `every annotated paragraph re-splits into the stored sentences`() {
        val index = assertNotNull(repository.index(ContentFixtures.dolittle))
        var annotated = 0
        for (paragraph in index.paragraphs) {
            if (paragraph.sentences.isEmpty()) continue
            annotated += 1
            assertEquals(
                paragraph.sentences.map { it.text },
                SentenceSplitter.split(paragraph.text),
                "${paragraph.id} sentence split drifted",
            )
            assertTrue(paragraph.sentences.all { !it.zh.isNullOrBlank() })
        }
        assertEquals(729, annotated)
    }

    @Test
    fun `long-sentence analysis is parsed`() {
        val book = assertNotNull(repository.book(ContentFixtures.dolittle))
        val analysed = book.chapters
            .flatMap { it.units }
            .flatMap { it.paragraphs }
            .flatMap { it.sentences }
            .mapNotNull { it.analysis }
        assertTrue(analysed.isNotEmpty())
        val analysis = analysed.first()
        assertTrue(analysis.level in setOf("A1", "A2", "B1", "B2", "C1", "C2"))
        assertTrue(analysis.core.isNotBlank())
        assertTrue(analysis.parts.isNotEmpty())
        assertTrue(analysis.parts.all { it.text.isNotBlank() && it.zh.isNotBlank() })
    }

    @Test
    fun `ids are unique inside the book`() {
        val book = assertNotNull(repository.book(ContentFixtures.dolittle))
        val ids = mutableListOf<String>()
        book.chapters.forEach { chapter ->
            ids += chapter.id
            chapter.units.forEach { unit ->
                ids += unit.id
                unit.paragraphs.forEach { paragraph ->
                    ids += paragraph.id
                    ids += paragraph.hints.map { it.id }
                }
            }
        }
        assertEquals(ids.size, ids.distinct().size)
    }

    @Test
    fun `content passes the schema validator`() {
        val book = assertNotNull(repository.book(ContentFixtures.dolittle))
        val library = book.chapters
            .flatMap { it.units }
            .flatMap { it.grammar?.pointIds.orEmpty() }
            .mapNotNull { repository.grammarPoint(it) }
            .associateBy { it.id }
        val report = ContentValidator.validate(book, library)
        assertTrue(report.isValid, report.summary())
    }

    @Test
    fun `store v2 keeps available identity in the package`() {
        val store = PagelenceJson.decodeFromString<BuiltInStoreCatalog>(ContentFixtures.read("store.json"))
        assertEquals(INDEX_SCHEMA_VERSION, store.schemaVersion)
        val shipped = FileContentRepository.loadManifest(ContentFixtures.contentDir).books.map { it.id }.toSet()
        val available = store.books.filter { it.status == StoreCatalogRules.STATUS_AVAILABLE }
        assertEquals(shipped, available.map { it.id }.toSet())
        available.forEach { book ->
            assertNull(book.title, book.id)
            assertNull(book.author, book.id)
            assertNull(book.level, book.id)
            assertNotNull(book.order.takeIf { it > 0 }, book.id)
        }
        val planned = store.books.filter { it.status == StoreCatalogRules.STATUS_PLANNED }
        assertTrue(planned.isNotEmpty())
        planned.forEach { book ->
            assertNotNull(book.title, book.id)
            assertNotNull(book.author, book.id)
            assertNotNull(book.level, book.id)
            assertEquals("estimated", book.levelSource, book.id)
        }
    }

    @Test
    fun `grammar library ships with the app`() {
        val point = assertNotNull(repository.grammarPoint("cefrj-67"))
        assertEquals("过去完成时", point.name.zh)
        assertEquals("B1.2", point.levelStart)
        assertTrue(point.form.hasText())
        assertTrue(point.explain.hasText())
        assertTrue(point.pitfalls.isNotEmpty())
    }

    private fun sha256(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes)
            .joinToString("") { "%02x".format(it) }
}
