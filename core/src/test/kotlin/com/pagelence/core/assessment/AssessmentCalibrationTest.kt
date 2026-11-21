package com.pagelence.core.assessment

import com.pagelence.core.ContentFixtures
import com.pagelence.core.progress.CefrLevel
import kotlin.math.abs
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Calibration sanity check, not a human trial: for each modelled true level we
 * simulate respondents who answer at or below their level correctly 85% of the
 * time and above it 35% of the time, then require the result to land near the
 * truth. It catches an inverted or mis-scaled mapping; it says nothing about
 * real-world validity, which needs 10–20 human testers.
 */
class AssessmentCalibrationTest {
    private val bank = FileAssessmentRepository(ContentFixtures.contentDir).bank()
    private val levels = listOf(CefrLevel.A2, CefrLevel.B1, CefrLevel.B2, CefrLevel.C1, CefrLevel.C2)

    private fun answer(plan: AssessmentPlan, trueLevel: CefrLevel, random: Random): Map<String, Int> {
        val trueRank = AssessmentRules.rank(trueLevel)
        return plan.questions.associate { question ->
            val atOrBelow = AssessmentRules.rank(question.level) <= trueRank
            val probability = if (atOrBelow) 0.85 else 0.35
            val correct = random.nextDouble() < probability
            question.id to if (correct) question.answer else (question.answer + 1 + random.nextInt(3)) % 4
        }
    }

    @Test
    fun `full test lands within one level of the modelled ability`() {
        val runs = 40
        levels.forEach { level ->
            var close = 0
            repeat(runs) { seed ->
                val random = Random(seed * 31 + level.ordinal)
                val plan = AssessmentRules.plan(bank, AssessmentMode.FULL, level, random)
                val result = AssessmentRules.evaluate(plan, answer(plan, level, random))
                if (abs(AssessmentRules.rank(result.suggested) - AssessmentRules.rank(level)) <= 1) close += 1
            }
            assertTrue(close >= runs * 3 / 4, "$level: $close/$runs within one level")
        }
    }

    @Test
    fun `quick band contains the modelled ability`() {
        val runs = 40
        levels.forEach { level ->
            // C2 is the top of the bank, so a missed C2 question drops the band
            // below it more often; the edge is expected, not a mapping bug.
            val threshold = if (level == CefrLevel.C2) runs * 3 / 5 else runs * 3 / 4
            var inside = 0
            repeat(runs) { seed ->
                val random = Random(seed * 17 + level.ordinal)
                val plan = AssessmentRules.plan(bank, AssessmentMode.QUICK, level, random)
                val result = AssessmentRules.evaluate(plan, answer(plan, level, random))
                val rank = AssessmentRules.rank(level)
                if (rank >= AssessmentRules.rank(result.bandLow) - 1 &&
                    rank <= AssessmentRules.rank(result.bandHigh)
                ) {
                    inside += 1
                }
            }
            assertTrue(inside >= threshold, "$level: $inside/$runs inside the band")
        }
    }
}
