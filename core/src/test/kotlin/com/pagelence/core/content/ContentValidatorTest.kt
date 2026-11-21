package com.pagelence.core.content

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ContentValidatorTest {
    private fun paragraph(
        id: String = "c1-p1",
        text: String = "One. Two.",
        sentences: List<Sentence> = SentenceSplitter.split(text).map { Sentence(it) },
        hints: List<Hint> = emptyList(),
    ) = Paragraph(id = id, text = text, sentences = sentences, hints = hints)

    private fun unit(
        id: String = "c1-u1",
        paragraph: Paragraph = paragraph(),
        quiz: List<QuizItem> = emptyList(),
        grammar: Grammar? = null,
        title: LocalizedText? = LocalizedText(zh = "单元"),
    ) = StudyUnit(
        id = id,
        number = 1,
        title = title,
        paragraphs = listOf(paragraph),
        quiz = quiz,
        grammar = grammar,
    )

    private fun book(vararg units: StudyUnit) = Book(
        id = "test",
        title = LocalizedText(zh = "测试", en = "Test"),
        description = LocalizedText(zh = "简介", en = "Description"),
        chapters = listOf(Chapter(id = "c1", number = 1, title = LocalizedText(zh = "章"), units = units.toList())),
    )

    private fun quiz(vararg items: QuizItem) = items.toList()

    private fun item(sentence: Int? = null, feedbackZh: String = "解析") = QuizItem(
        question = "Q?",
        options = listOf("a", "b"),
        answer = 0,
        sentence = sentence,
        feedback = LocalizedText(zh = feedbackZh),
    )

    @Test
    fun `a minimal book passes`() {
        val report = ContentValidator.validate(book(unit()))
        assertTrue(report.isValid, report.summary())
    }

    @Test
    fun `description is required`() {
        val report = ContentValidator.validate(
            Book(
                id = "test",
                title = LocalizedText(zh = "测试"),
                chapters = listOf(Chapter(id = "c1", number = 1, units = listOf(unit()))),
            ),
        )
        assertTrue(report.errors.any { it.message.contains("description is required") })
    }

    @Test
    fun `skeleton content only warns`() {
        val skeleton = StudyUnit(
            id = "c1-u1",
            number = 1,
            paragraphs = listOf(Paragraph(id = "c1-p1", text = "Just text.")),
        )
        val report = ContentValidator.validate(book(skeleton))
        assertTrue(report.isValid, report.summary())
        assertTrue(report.warnings.any { it.message.contains("no sentences") })
        assertTrue(report.warnings.any { it.message.contains("title is missing") })
    }

    @Test
    fun `duplicate ids are rejected`() {
        val report = ContentValidator.validate(book(unit(), unit()))
        assertFalse(report.isValid)
        assertTrue(report.errors.any { it.message.contains("duplicate id c1-u1") })
    }

    @Test
    fun `stored sentences must equal the splitter output`() {
        val broken = paragraph(sentences = listOf(Sentence("One. Two. Three.")))
        val report = ContentValidator.validate(book(unit(paragraph = broken)))
        assertTrue(report.errors.any { it.message.contains("frozen splitter") })
    }

    @Test
    fun `quiz answers and sentence indexes are checked`() {
        val badAnswer = item(sentence = 9).copy(answer = 2)
        val report = ContentValidator.validate(book(unit(quiz = quiz(badAnswer))))
        assertTrue(report.errors.any { it.message.contains("answer is out of range") })
        assertTrue(report.errors.any { it.message.contains("sentence must be a 1-based") })
    }

    @Test
    fun `quiz sentence one is valid`() {
        val report = ContentValidator.validate(book(unit(quiz = quiz(item(sentence = 2)))))
        assertTrue(report.isValid, report.summary())
    }

    @Test
    fun `quiz feedback needs a Chinese line`() {
        val missing = QuizItem(
            question = "Q?",
            options = listOf("a", "b"),
            answer = 0,
            feedback = LocalizedText(en = "Because."),
        )
        val report = ContentValidator.validate(book(unit(quiz = quiz(missing))))
        assertTrue(report.errors.any { it.message.contains("feedback.zh is required") })
    }

    @Test
    fun `hints must point into the paragraph`() {
        val broken = paragraph(
            hints = listOf(Hint(id = "h1", kind = "word", target = "Three", zh = "三", en = "Three")),
        )
        val report = ContentValidator.validate(book(unit(paragraph = broken)))
        assertTrue(report.errors.any { it.message.contains("hint target is not in the paragraph") })
    }

    @Test
    fun `hint sentence must be inside the unit and contain the target`() {
        val broken = paragraph(
            hints = listOf(
                Hint(id = "h1", kind = "word", target = "One", sentence = 9, zh = "一", en = "One"),
                Hint(id = "h2", kind = "word", target = "One", sentence = 2, zh = "一", en = "One"),
            ),
        )
        val report = ContentValidator.validate(book(unit(paragraph = broken)))
        assertTrue(report.errors.any { it.message.contains("must be a 1-based unit index") })
        assertTrue(report.errors.any { it.message.contains("is not in the sentence it points at") })
    }

    @Test
    fun `duplicate hints for the same target and sentence are rejected`() {
        val broken = paragraph(
            hints = listOf(
                Hint(id = "h1", kind = "word", target = "One", zh = "一", en = "One"),
                Hint(id = "h2", kind = "word", target = "One", zh = "一", en = "One"),
            ),
        )
        val report = ContentValidator.validate(book(unit(paragraph = broken)))
        assertTrue(report.errors.any { it.message.contains("duplicate hint for the same target") })
    }

    @Test
    fun `grammar needs its excerpt in the unit and a focus`() {
        val grammar = Grammar(excerpt = "not in the text", topic = "test")
        val report = ContentValidator.validate(book(unit(grammar = grammar)))
        assertTrue(report.errors.any { it.message.contains("excerpt is not in the unit text") })
        assertTrue(report.errors.any { it.message.contains("focus is required") })
    }

    @Test
    fun `inline grammar analysis is accepted without warnings`() {
        val text = LocalizedText(zh = "讲解")
        val grammar = Grammar(
            excerpt = "One.",
            topic = "test",
            form = text,
            explain = text,
            focus = text,
        )
        val report = ContentValidator.validate(book(unit(grammar = grammar)))
        assertTrue(report.isValid, report.summary())
        assertTrue(report.warnings.none { it.message.contains("incomplete") })
    }

    @Test
    fun `library points can complete the analysis`() {
        val text = LocalizedText(zh = "讲解")
        val point = GrammarPoint(
            id = "cefrj-67",
            name = LocalizedText(zh = "过去完成时"),
            levelStart = "B1.2",
            explain = text,
            form = text,
            pitfalls = listOf(text),
        )
        val grammar = Grammar(excerpt = "One.", pointIds = listOf("cefrj-67"), focus = text)
        val report = ContentValidator.validate(book(unit(grammar = grammar)), mapOf("cefrj-67" to point))
        assertTrue(report.isValid, report.summary())
        assertTrue(report.warnings.none { it.message.contains("incomplete") })
    }

    @Test
    fun `unknown grammar point ids only warn`() {
        val text = LocalizedText(zh = "讲解")
        val grammar = Grammar(excerpt = "One.", pointIds = listOf("ghost"), focus = text)
        val report = ContentValidator.validate(
            book(unit(grammar = grammar)),
            mapOf(
                "cefrj-67" to GrammarPoint(
                    id = "cefrj-67",
                    name = LocalizedText(zh = "过去完成时"),
                    levelStart = "B1.2",
                    explain = text,
                    form = text,
                    pitfalls = listOf(text),
                ),
            ),
        )
        assertTrue(report.isValid, report.summary())
        assertTrue(report.warnings.any { it.message.contains("is not in the library") })
    }

    @Test
    fun `analysis parts must come from the sentence`() {
        val sentence = Sentence(
            "He said that he would come.",
            analysis = Analysis(
                level = "B2",
                core = "He said",
                parts = listOf(AnalysisPart(text = "not in the sentence", role = "subordinate", zh = "从句")),
            ),
        )
        val report = ContentValidator.validate(
            book(unit(paragraph = Paragraph(id = "c1-p1", text = "He said that he would come.", sentences = listOf(sentence)))),
        )
        assertTrue(report.errors.any { it.message.contains("part text is not in the sentence") })
    }

    @Test
    fun `a complete analysis is accepted`() {
        val sentence = Sentence(
            "He said that he would come.",
            analysis = Analysis(
                level = "B2",
                core = "He said",
                parts = listOf(AnalysisPart(text = "that he would come", role = "subordinate", zh = "宾语从句")),
                order = LocalizedText(zh = "先说他说，再补说的内容"),
                note = LocalizedText(zh = "先找 said，再看 that 从句"),
            ),
        )
        val report = ContentValidator.validate(
            book(unit(paragraph = Paragraph(id = "c1-p1", text = "He said that he would come.", sentences = listOf(sentence)))),
        )
        assertTrue(report.isValid, report.summary())
    }

    @Test
    fun `html in text is rejected`() {
        val broken = paragraph(
            sentences = listOf(
                Sentence("One.", zh = "<b>一</b>"),
                Sentence("Two."),
            ),
        )
        val report = ContentValidator.validate(book(unit(paragraph = broken)))
        assertFalse(report.isValid)
        assertEquals(1, report.errors.count { it.message.contains("contains HTML") })
    }
}
