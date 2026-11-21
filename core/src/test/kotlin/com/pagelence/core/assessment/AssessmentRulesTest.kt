package com.pagelence.core.assessment

import com.pagelence.core.ContentFixtures
import com.pagelence.core.progress.CefrLevel
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class AssessmentRulesTest {
    private val bank = FileAssessmentRepository(ContentFixtures.contentDir).bank()

    private fun correct(plan: AssessmentPlan) = plan.questions.associate { it.id to it.answer }

    @Test
    fun `shipped bank parses with the expected shape`() {
        assertEquals(210, bank.questions.size)
        assertEquals(15, bank.passages.size)
        assertTrue(bank.passages.isNotEmpty())
        val ids = bank.questions.map { it.id }
        assertEquals(ids.size, ids.toSet().size, "duplicate question ids")
        bank.questions.forEach { question ->
            assertEquals(4, question.options.size, question.id)
            assertTrue(question.answer in question.options.indices, question.id)
            assertNotNull(question.explanation.zh, question.id)
            assertNotNull(question.explanation.en, question.id)
        }
        assertTrue(bank.questions.any { it.skill == AssessmentSkill.READING })
    }

    @Test
    fun `the last two tests are excluded from the next draw`() {
        val recent = bank.questions.take(40).map { it.id }
        val plan = AssessmentRules.plan(
            bank,
            AssessmentMode.QUICK,
            CefrLevel.A2,
            Random(4),
            recent = recent,
        )
        assertTrue(plan.questions.none { it.id in recent }, "recent ids leaked into the draw")
        assertTrue(plan.questions.isNotEmpty())
    }

    @Test
    fun `prompts use one blank placeholder`() {
        bank.questions.forEach { question ->
            assertFalse(question.prompt.contains("___"), question.id)
            assertFalse(Regex("_{2,}").containsMatchIn(question.prompt), question.id)
        }
        assertTrue(bank.questions.any { it.prompt.contains("{{blank}}") })
    }

    @Test
    fun `quick test draws 12 to 15 non-reading questions near the level`() {
        listOf(CefrLevel.A1, CefrLevel.A2, CefrLevel.B1, CefrLevel.B2, CefrLevel.C2).forEach { level ->
            val plan = AssessmentRules.plan(bank, AssessmentMode.QUICK, level, Random(7))
            assertTrue(
                plan.questions.size in AssessmentRules.QUICK_MIN..AssessmentRules.QUICK_MAX,
                "$level: ${plan.questions.size}",
            )
            assertTrue(plan.questions.none { it.skill == AssessmentSkill.READING })
            assertTrue(plan.questions.all { it.id in bank.questions.map { q -> q.id } })
            val asked = plan.questions.map { it.level }.distinct()
            assertTrue(asked.size in 2..3, "$level asked $asked")
            assertTrue(
                asked.all { kotlin.math.abs(AssessmentRules.rank(it) - AssessmentRules.rank(level)) <= 2 },
                "$level drew $asked",
            )
        }
    }

    @Test
    fun `full test draws 30 to 40 questions and one passage`() {
        val plan = AssessmentRules.plan(bank, AssessmentMode.FULL, CefrLevel.B1, Random(11))
        assertTrue(
            plan.questions.size in AssessmentRules.FULL_MIN..AssessmentRules.FULL_MAX,
            "size: ${plan.questions.size}",
        )
        assertEquals(1, plan.passages.size)
        val passageQuestions = plan.questions.filter { it.skill == AssessmentSkill.READING }
        assertTrue(passageQuestions.isNotEmpty())
        assertTrue(passageQuestions.all { it.passageId == plan.passages.first().id })
    }

    @Test
    fun `all correct suggests the top level and a full test names one level`() {
        val plan = AssessmentRules.plan(bank, AssessmentMode.FULL, CefrLevel.B1, Random(3))
        val result = AssessmentRules.evaluate(plan, correct(plan))
        assertEquals(100, result.scorePercent)
        assertEquals(CefrLevel.C2, result.suggested)
        assertTrue(result.singleLevel)
        assertEquals(result.suggested, result.bandLow)
        assertEquals(result.suggested, result.bandHigh)
    }

    @Test
    fun `no correct answers suggests the lowest asked level`() {
        val plan = AssessmentRules.plan(bank, AssessmentMode.QUICK, CefrLevel.B1, Random(5))
        val result = AssessmentRules.evaluate(plan, emptyMap())
        assertEquals(0, result.correct)
        assertEquals(plan.questions.minOf { AssessmentRules.rank(it.level) }.let { CefrLevel.entries[it] }, result.suggested)
        assertFalse(result.singleLevel)
        assertTrue(AssessmentRules.rank(result.bandLow) <= AssessmentRules.rank(result.suggested))
        assertTrue(AssessmentRules.rank(result.suggested) <= AssessmentRules.rank(result.bandHigh))
        assertTrue(result.bandLow != result.bandHigh, "a quick result is always a band")
    }

    @Test
    fun `missing answers count as wrong`() {
        val plan = AssessmentRules.plan(bank, AssessmentMode.QUICK, CefrLevel.A2, Random(9))
        val answers = correct(plan).entries.take(plan.questions.size / 2).associate { it.key to it.value }
        val result = AssessmentRules.evaluate(plan, answers)
        assertEquals(plan.questions.size / 2, result.correct)
        assertTrue(result.scorePercent in 40..60)
    }

    @Test
    fun `thin C bank reports a band instead of a single level`() {
        val questions = (0 until 4).map { index ->
            AssessmentQuestion(
                id = "c1-$index",
                level = CefrLevel.C1,
                skill = AssessmentSkill.GRAMMAR,
                prompt = "Q$index",
                options = listOf("a", "b", "c", "d"),
                answer = 0,
                explanation = com.pagelence.core.content.LocalizedText(zh = "解析", en = "why"),
            )
        }
        val small = AssessmentBank(questions = questions)
        val plan = AssessmentRules.plan(small, AssessmentMode.FULL, CefrLevel.C1, Random(1))
        val result = AssessmentRules.evaluate(plan, correct(plan))
        assertEquals(CefrLevel.C1, result.suggested)
        assertFalse(result.singleLevel)
        assertEquals(CefrLevel.C2, result.bandHigh)
    }

    @Test
    fun `correct answers on the hardest level alone do not suggest it`() {
        fun question(id: String, level: CefrLevel, answer: Int) = AssessmentQuestion(
            id = id,
            level = level,
            skill = AssessmentSkill.GRAMMAR,
            prompt = id,
            options = listOf("a", "b", "c", "d"),
            answer = answer,
            explanation = com.pagelence.core.content.LocalizedText(zh = "解析", en = "why"),
        )
        val questions = listOf(
            question("a2-1", CefrLevel.A2, 1),
            question("a2-2", CefrLevel.A2, 2),
            question("a2-3", CefrLevel.A2, 3),
            question("b1-1", CefrLevel.B1, 1),
            question("b1-2", CefrLevel.B1, 2),
            question("b1-3", CefrLevel.B1, 3),
            question("c2-1", CefrLevel.C2, 0),
            question("c2-2", CefrLevel.C2, 0),
            question("c2-3", CefrLevel.C2, 0),
        )
        val plan = AssessmentRules.plan(
            AssessmentBank(questions = questions),
            AssessmentMode.FULL,
            CefrLevel.A2,
            Random(1),
        )
        // Only the C2 answers are right; the lowest asked level should stand.
        val answers = questions.filter { it.level == CefrLevel.C2 }.associate { it.id to 0 }
        val result = AssessmentRules.evaluate(plan, answers)
        assertEquals(CefrLevel.A2, result.suggested)
        assertEquals(CefrLevel.A2, result.bandLow)
        assertEquals(CefrLevel.A2, result.bandHigh)
    }

    @Test
    fun `skill percentages are reported per asked skill`() {
        val plan = AssessmentRules.plan(bank, AssessmentMode.FULL, CefrLevel.B2, Random(13))
        val result = AssessmentRules.evaluate(plan, correct(plan))
        assertEquals(setOf(AssessmentSkill.VOCAB, AssessmentSkill.GRAMMAR, AssessmentSkill.READING), result.skillPercent.keys)
        assertTrue(result.skillPercent.values.all { it == 100 })
    }
}
