package com.pagelence.core.assessment

import com.pagelence.core.content.LocalizedText
import com.pagelence.core.progress.CefrLevel
import com.pagelence.core.progress.LevelSource
import kotlin.random.Random
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Placement-test question bank and the two test modes from the feature backlog
 * 8.2: a 12–15 question quick test (band result) and a 30–40 question full test
 * (single level, plus skills), sharing one bank.
 *
 * The bank ships as `content/assessment/bank.json`; the schema is documented in
 * `work/android/docs/content-format.md`. All rules here are pure functions so
 * the app shell only renders and forwards events.
 */

/** What a question measures. Reading questions hang off an [AssessmentPassage]. */
@Serializable
enum class AssessmentSkill {
    @SerialName("vocab") VOCAB,
    @SerialName("grammar") GRAMMAR,
    @SerialName("reading") READING,
}

@Serializable
enum class AssessmentMode { QUICK, FULL }

/** Where an accepted result is stored in `state.json`. */
fun AssessmentMode.levelSource(): LevelSource = when (this) {
    AssessmentMode.QUICK -> LevelSource.QUICK
    AssessmentMode.FULL -> LevelSource.FULL
}

@Serializable
data class AssessmentQuestion(
    val id: String,
    val level: CefrLevel,
    val skill: AssessmentSkill,
    /** Set for reading questions; the passage is shown above the prompt. */
    val passageId: String? = null,
    val prompt: String,
    val options: List<String>,
    /** Index into [options]. */
    val answer: Int,
    val explanation: LocalizedText,
)

@Serializable
data class AssessmentPassage(
    val id: String,
    val level: CefrLevel,
    val title: String,
    val text: String,
)

@Serializable
data class AssessmentBank(
    val schemaVersion: Int = ASSESSMENT_SCHEMA_VERSION,
    val updatedAt: String? = null,
    val source: String? = null,
    val license: String? = null,
    val passages: List<AssessmentPassage> = emptyList(),
    val questions: List<AssessmentQuestion> = emptyList(),
) {
    fun passage(id: String?): AssessmentPassage? = id?.let { key -> passages.firstOrNull { it.id == key } }

    fun byLevel(level: CefrLevel): List<AssessmentQuestion> = questions.filter { it.level == level }
}

const val ASSESSMENT_SCHEMA_VERSION = 1

/** One quick/full plan: the drawn questions plus the passages they need. */
data class AssessmentPlan(
    val mode: AssessmentMode,
    val level: CefrLevel,
    val questions: List<AssessmentQuestion>,
    val passages: List<AssessmentPassage>,
)

/** Per-skill result; reading is absent when the mode/bank had no reading items. */
data class AssessmentResult(
    val mode: AssessmentMode,
    val correct: Int,
    val total: Int,
    /** 0–100, rounded. */
    val scorePercent: Int,
    /** Lowest and highest level of the reference band; equal for a single level. */
    val bandLow: CefrLevel,
    val bandHigh: CefrLevel,
    /** The level the test suggests; always inside the band. */
    val suggested: CefrLevel,
    /** False when the band is wider than one level (quick test, thin C bank). */
    val singleLevel: Boolean,
    val skillPercent: Map<AssessmentSkill, Int>,
)

/**
 * Question drawing and grading.
 *
 * Level mapping is deliberately coarse: a level counts as passed at 60% of its
 * asked questions, the suggestion is the highest passed level (lowest asked
 * level when nothing passes), and a band is widened instead of pretending
 * precision. No IRT/CAT: the bank has no calibration sample.
 */
object AssessmentRules {
    const val QUICK_MIN = 12
    const val QUICK_MAX = 15
    const val FULL_MIN = 30
    const val FULL_MAX = 40
    const val PASS_RATIO = 0.6
    const val MIN_PER_LEVEL = 3
    /** A full test needs this many questions at C1/C2 before naming a single level. */
    const val SINGLE_LEVEL_MIN = 5

    private val LEVELS = CefrLevel.entries

    fun rank(level: CefrLevel): Int = LEVELS.indexOf(level)

    /**
     * Levels asked by a quick test: the current one and its neighbours, nearest
     * first. A level with fewer than [MIN_PER_LEVEL] questions is skipped so a
     * thin bank still yields a readable band.
     */
    fun quickLevels(level: CefrLevel, bank: AssessmentBank): List<CefrLevel> {
        val here = rank(level)
        val order = buildList {
            add(here)
            for (step in 1..LEVELS.size) {
                if (here - step in LEVELS.indices) add(here - step)
                if (here + step in LEVELS.indices) add(here + step)
            }
        }.map { LEVELS[it] }
            .filter { kotlin.math.abs(rank(it) - here) <= 2 }
        val usable = order.filter { bank.byLevel(it).size >= MIN_PER_LEVEL }.take(3)
        return usable.ifEmpty { listOf(level) }
    }

    /**
     * Draw a test. Quick: 4 per level (3 levels → 12, fewer levels → 5 per level
     * up to [QUICK_MAX]). Full: 6 per level plus one passage, 30–40 total.
     */
    fun plan(
        bank: AssessmentBank,
        mode: AssessmentMode,
        level: CefrLevel,
        random: Random = Random.Default,
        /** Question ids shown in the last two tests; excluded before drawing. */
        recent: List<String> = emptyList(),
    ): AssessmentPlan = when (mode) {
        AssessmentMode.QUICK -> quickPlan(bank, level, random, recent.toSet())
        AssessmentMode.FULL -> fullPlan(bank, level, random, recent.toSet())
    }

    private fun quickPlan(
        bank: AssessmentBank,
        level: CefrLevel,
        random: Random,
        recent: Set<String>,
    ): AssessmentPlan {
        val levels = quickLevels(level, bank)
        val perLevel = if (levels.size >= 3) 4 else 5
        val drawn = levels.flatMap { current ->
            bank.byLevel(current)
                .filter { it.skill != AssessmentSkill.READING && it.id !in recent }
                .shuffled(random)
                .take(perLevel)
        }
        val questions = drawn
            .take(QUICK_MAX)
            .ifEmpty { drawn }
            .let { topUp(bank, it, QUICK_MIN, level, random, recent) }
        return AssessmentPlan(
            mode = AssessmentMode.QUICK,
            level = level,
            questions = questions,
            passages = emptyList(),
        )
    }

    private fun fullPlan(
        bank: AssessmentBank,
        level: CefrLevel,
        random: Random,
        recent: Set<String>,
    ): AssessmentPlan {
        val perLevel = mutableListOf<AssessmentQuestion>()
        // Prefer the reader's own level and its neighbours when a level is thin.
        val order = LEVELS.sortedBy { kotlin.math.abs(rank(it) - rank(level)) }
        for (candidate in order) {
            if (perLevel.size >= FULL_MAX - 5) break
            val take = minOf(6, FULL_MAX - 5 - perLevel.size)
            perLevel += bank.byLevel(candidate)
                .filter { it.skill != AssessmentSkill.READING && it.id !in recent }
                .shuffled(random)
                .take(take)
        }
        val reading = drawReading(bank, level, random, recent)
        val filled = topUp(bank, perLevel, FULL_MIN - reading.questions.size, level, random, recent)
        val questions = (filled + reading.questions)
            .distinctBy { it.id }
            .take(FULL_MAX)
        return AssessmentPlan(
            mode = AssessmentMode.FULL,
            level = level,
            questions = questions,
            passages = reading.passages,
        )
    }

    /** Add unused non-reading questions (nearest levels first) until [minimum]. */
    private fun topUp(
        bank: AssessmentBank,
        questions: List<AssessmentQuestion>,
        minimum: Int,
        level: CefrLevel,
        random: Random,
        recent: Set<String>,
    ): List<AssessmentQuestion> {
        if (questions.size >= minimum) return questions
        val used = questions.map { it.id }.toSet()
        val candidates = bank.questions.filter {
            it.id !in used && it.id !in recent && it.skill != AssessmentSkill.READING
        }
        val near = candidates.filter { kotlin.math.abs(rank(it.level) - rank(level)) <= 2 }
        val extra = (near.ifEmpty { candidates })
            .groupBy { it.level }
            .toList()
            .sortedBy { (candidate, _) -> kotlin.math.abs(rank(candidate) - rank(level)) }
            .flatMap { (_, group) -> group.shuffled(random) }
            .take(minimum - questions.size)
        return questions + extra
    }

    private data class ReadingDraw(
        val questions: List<AssessmentQuestion>,
        val passages: List<AssessmentPassage>,
    )

    /** One passage with all its questions; empty when the bank has no passage. */
    private fun drawReading(
        bank: AssessmentBank,
        level: CefrLevel,
        random: Random,
        recent: Set<String>,
    ): ReadingDraw {
        if (bank.passages.isEmpty()) return ReadingDraw(emptyList(), emptyList())
        val chosen = bank.passages
            .filter { passage -> bank.questions.any { it.passageId == passage.id && it.id !in recent } }
            .shuffled(random)
            .minByOrNull { kotlin.math.abs(rank(it.level) - rank(level)) }
            ?: return ReadingDraw(emptyList(), emptyList())
        val questions = bank.questions.filter { it.passageId == chosen.id }
        return ReadingDraw(questions, listOf(chosen))
    }

    /**
     * Grade [answers] (question id → option index) against [plan].
     *
     * A missing answer counts as wrong. The band logic never widens below the
     * lowest asked level and never suggests a level the bank did not test.
     */
    fun evaluate(plan: AssessmentPlan, answers: Map<String, Int>): AssessmentResult {
        val questions = plan.questions
        val total = questions.size
        val correct = questions.count { answers[it.id] == it.answer }
        val score = if (total == 0) 0 else Math.round(correct * 100f / total)

        val levelsAsked = questions.map { it.level }.distinct().sortedBy(::rank)
        val lowest = levelsAsked.firstOrNull() ?: plan.level
        // Walk up from the lowest asked level and stop at the first failure: a
        // lucky run on the hardest level alone must not suggest C2.
        var suggested = lowest
        for (asked in levelsAsked) {
            val levelQuestions = questions.filter { it.level == asked }
            if (levelQuestions.size < MIN_PER_LEVEL) break
            val hits = levelQuestions.count { answers[it.id] == it.answer }
            if (hits < Math.ceil(levelQuestions.size * PASS_RATIO).toInt()) break
            suggested = asked
        }

        val thinTop = suggested >= CefrLevel.C1 &&
            questions.count { it.level == suggested } < SINGLE_LEVEL_MIN
        val single = plan.mode == AssessmentMode.FULL && !thinTop
        var bandLow = suggested
        var bandHigh = suggested
        if (!single) {
            val below = LEVELS.getOrNull(rank(suggested) - 1)
            bandLow = if (below != null && rank(below) >= rank(lowest)) below else lowest
            bandHigh = if (thinTop) CefrLevel.C2 else suggested
            // A quick test never returns a single level: when the suggestion is
            // already the lowest asked level, widen upwards by one instead.
            if (bandHigh == bandLow) {
                bandHigh = LEVELS.getOrNull(rank(bandLow) + 1) ?: bandLow
            }
        }

        val skills = questions.map { it.skill }.distinct()
        val skillPercent = skills.associateWith { skill ->
            val ofSkill = questions.filter { it.skill == skill }
            if (ofSkill.isEmpty()) 0
            else Math.round(ofSkill.count { answers[it.id] == it.answer } * 100f / ofSkill.size)
        }
        return AssessmentResult(
            mode = plan.mode,
            correct = correct,
            total = total,
            scorePercent = score,
            bandLow = bandLow,
            bandHigh = bandHigh,
            suggested = suggested,
            singleLevel = single,
            skillPercent = skillPercent,
        )
    }
}
