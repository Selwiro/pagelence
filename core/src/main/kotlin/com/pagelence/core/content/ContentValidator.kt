package com.pagelence.core.content

/**
 * Schema check for one book, used by content tests and (later) by the book
 * importer. It mirrors `tools/validate/validate_content.py`: anything that
 * would break reading is an error; missing study content is a warning because
 * skeleton books and the content gap are expected states.
 */
data class ValidationIssue(val path: String, val message: String)

data class ValidationReport(
    val errors: List<ValidationIssue>,
    val warnings: List<ValidationIssue>,
) {
    val isValid: Boolean get() = errors.isEmpty()

    fun summary(): String = buildString {
        append("${errors.size} error(s), ${warnings.size} warning(s)")
        errors.take(10).forEach { append("\n  $it") }
    }
}

object ContentValidator {
    private val hintKinds = setOf("word", "phrase", "sentence")
    private val analysisRoles = setOf(
        "subordinate",
        "relative",
        "nonfinite",
        "prepositional",
        "appositive",
        "parenthetical",
    )
    private val cefrLevels = setOf("A1", "A2", "B1", "B2", "C1", "C2")

    fun validate(book: Book, library: Map<String, GrammarPoint> = emptyMap()): ValidationReport {
        val errors = mutableListOf<ValidationIssue>()
        val warnings = mutableListOf<ValidationIssue>()

        if (book.schemaVersion != BOOK_SCHEMA_VERSION) {
            errors += ValidationIssue("/", "unsupported schemaVersion ${book.schemaVersion}")
        }
        if (book.id.isBlank()) errors += ValidationIssue("/", "book id is empty")
        if (book.title.isBlank) errors += ValidationIssue("/", "title is empty")
        if (!book.description.hasText()) errors += ValidationIssue("/", "description is required")
        if (book.chapters.isEmpty()) errors += ValidationIssue("/", "book has no chapters")

        val seen = mutableSetOf<String>()
        fun claim(id: String, kind: String, path: String) {
            if (id.isBlank()) errors += ValidationIssue(path, "$kind id is empty")
            if (!seen.add(id)) errors += ValidationIssue(path, "duplicate id $id ($kind)")
        }

        book.chapters.forEachIndexed { chapterIndex, chapter ->
            val chapterPath = "/${chapter.id}"
            claim(chapter.id, "chapter", chapterPath)
            if (chapter.number != chapterIndex + 1) {
                errors += ValidationIssue(chapterPath, "chapter.number should be ${chapterIndex + 1}")
            }
            if (chapter.units.isEmpty()) errors += ValidationIssue(chapterPath, "chapter has no units")

            chapter.units.forEachIndexed { unitIndex, unit ->
                val unitPath = "$chapterPath/${unit.id}"
                claim(unit.id, "unit", unitPath)
                if (unit.number != unitIndex + 1) {
                    errors += ValidationIssue(
                        unitPath,
                        "unit.number should be the chapter-local ${unitIndex + 1}",
                    )
                }
                if (unit.title == null) {
                    warnings += ValidationIssue(unitPath, "unit title is missing (skeleton content)")
                }
                if (unit.paragraphs.isEmpty()) errors += ValidationIssue(unitPath, "unit has no paragraphs")

                val unitText = unit.paragraphs.joinToString(" ") { it.text }
                val unitSentences = unit.paragraphs.flatMap { it.sentences }
                var sentenceCount = 0
                val hintTargets = mutableSetOf<String>()

                for (paragraph in unit.paragraphs) {
                    val paragraphPath = "$unitPath/${paragraph.id}"
                    claim(paragraph.id, "paragraph", paragraphPath)
                    if (paragraph.text.isBlank()) {
                        errors += ValidationIssue(paragraphPath, "paragraph text is empty")
                    } else if (containsHtml(paragraph.text)) {
                        errors += ValidationIssue(paragraphPath, "paragraph text contains HTML")
                    }

                    if (paragraph.sentences.isEmpty()) {
                        warnings += ValidationIssue(
                            paragraphPath,
                            "paragraph has no sentences (study edition pending)",
                        )
                    } else {
                        val expected = SentenceSplitter.split(paragraph.text)
                        if (paragraph.sentences.map { it.text } != expected) {
                            errors += ValidationIssue(
                                paragraphPath,
                                "sentences do not match the frozen splitter " +
                                    "(${paragraph.sentences.size} stored, ${expected.size} split)",
                            )
                        }
                    }
                    sentenceCount += paragraph.sentences.size

                    paragraph.sentences.forEachIndexed { sentenceIndex, sentence ->
                        val sentencePath = "$paragraphPath/s${sentenceIndex + 1}"
                        if (sentence.text.isBlank()) {
                            errors += ValidationIssue(sentencePath, "sentence text is empty")
                        }
                        sentence.zh?.let {
                            if (it.isBlank()) errors += ValidationIssue(sentencePath, "sentence zh is empty")
                            if (containsHtml(it)) errors += ValidationIssue(sentencePath, "sentence zh contains HTML")
                        }
                        sentence.analysis?.let { analysis ->
                            validateAnalysis(analysis, sentence.text, sentencePath, errors)
                        }
                    }

                    paragraph.hints.forEach { hint ->
                        val hintPath = "$paragraphPath/hint:${hint.id}"
                        claim(hint.id, "hint", hintPath)
                        if (hint.kind !in hintKinds) {
                            errors += ValidationIssue(hintPath, "unknown hint kind ${hint.kind}")
                        }
                        if (hint.target.isBlank() || !paragraph.text.contains(hint.target, ignoreCase = true)) {
                            errors += ValidationIssue(hintPath, "hint target is not in the paragraph")
                        }
                        if (hint.zh.isNullOrBlank() || hint.en.isNullOrBlank()) {
                            errors += ValidationIssue(hintPath, "hint needs zh and en")
                        }
                        hint.sentence?.let { index ->
                            if (index < 1 || index > unitSentences.size) {
                                errors += ValidationIssue(
                                    hintPath,
                                    "hint.sentence must be a 1-based unit index",
                                )
                            } else if (!unitSentences[index - 1].text.contains(hint.target, ignoreCase = true)) {
                                errors += ValidationIssue(
                                    hintPath,
                                    "hint target is not in the sentence it points at",
                                )
                            }
                        }
                        val key = "${hint.target.lowercase()}@${hint.sentence ?: 0}"
                        if (!hintTargets.add(key)) {
                            errors += ValidationIssue(
                                hintPath,
                                "duplicate hint for the same target and sentence",
                            )
                        }
                    }
                }

                unit.quiz.forEachIndexed { quizIndex, item ->
                    val quizPath = "$unitPath/quiz${quizIndex + 1}"
                    if (item.question.isBlank()) errors += ValidationIssue(quizPath, "question is empty")
                    if (item.options.size < 2) errors += ValidationIssue(quizPath, "needs at least two options")
                    if (item.answer !in item.options.indices) {
                        errors += ValidationIssue(quizPath, "answer is out of range")
                    }
                    if (item.feedback.zh.isNullOrBlank()) {
                        errors += ValidationIssue(quizPath, "feedback.zh is required")
                    }
                    item.sentence?.let { index ->
                        if (index < 1 || index > sentenceCount) {
                            errors += ValidationIssue(quizPath, "sentence must be a 1-based unit index")
                        }
                    }
                }

                unit.grammar?.let { grammar ->
                    validateGrammar(grammar, unitText, library, unitPath, errors, warnings)
                }
            }
        }

        return ValidationReport(errors = errors, warnings = warnings)
    }

    private fun validateAnalysis(
        analysis: Analysis,
        sentenceText: String,
        path: String,
        errors: MutableList<ValidationIssue>,
    ) {
        if (analysis.level !in cefrLevels) {
            errors += ValidationIssue("$path/analysis", "unknown analysis.level ${analysis.level}")
        }
        if (analysis.core.isBlank()) {
            errors += ValidationIssue("$path/analysis", "analysis.core is required")
        }
        if (analysis.parts.size !in 1..4) {
            errors += ValidationIssue("$path/analysis", "analysis.parts must have 1–4 items")
        }
        analysis.parts.forEachIndexed { index, part ->
            val partPath = "$path/analysis/p${index + 1}"
            if (part.text.isBlank() || !sentenceText.contains(part.text)) {
                errors += ValidationIssue(partPath, "part text is not in the sentence")
            }
            if (part.role !in analysisRoles) {
                errors += ValidationIssue(partPath, "unknown part role ${part.role}")
            }
            if (part.zh.isBlank()) errors += ValidationIssue(partPath, "part zh is required")
        }
    }

    private fun validateGrammar(
        grammar: Grammar,
        unitText: String,
        library: Map<String, GrammarPoint>,
        unitPath: String,
        errors: MutableList<ValidationIssue>,
        warnings: MutableList<ValidationIssue>,
    ) {
        val grammarPath = "$unitPath/grammar"
        if (grammar.excerpt.isBlank() || !unitText.contains(grammar.excerpt)) {
            errors += ValidationIssue(grammarPath, "excerpt is not in the unit text")
        }
        if (!grammar.focus.hasText()) {
            errors += ValidationIssue(grammarPath, "focus is required")
        }
        val duplicates = grammar.pointIds.groupingBy { it }.eachCount().filterValues { it > 1 }.keys
        if (duplicates.isNotEmpty()) {
            errors += ValidationIssue(grammarPath, "duplicate pointIds ${duplicates.joinToString()}")
        }
        val unknown = grammar.pointIds.filter { it !in library }
        unknown.forEach { id ->
            warnings += ValidationIssue(grammarPath, "pointId $id is not in the library (ignored)")
        }
        val primary = grammar.pointIds.firstOrNull()
        val primaryPoint = primary?.let(library::get)
        if (primaryPoint == null && !grammar.hasInlineAnalysis) {
            warnings += ValidationIssue(grammarPath, "grammar analysis is incomplete (content gap)")
        }
        if (primaryPoint != null && !coveredByLibrary(primaryPoint)) {
            warnings += ValidationIssue(grammarPath, "library point $primary has incomplete fields")
        }
    }

    private fun coveredByLibrary(point: GrammarPoint): Boolean =
        point.form.hasText() && point.explain.hasText() && point.pitfalls.isNotEmpty()

    private fun containsHtml(text: String): Boolean = HTML_TAG.containsMatchIn(text)

    private val HTML_TAG = Regex("<\\s*/?\\s*[a-zA-Z][^>]*>")
}
