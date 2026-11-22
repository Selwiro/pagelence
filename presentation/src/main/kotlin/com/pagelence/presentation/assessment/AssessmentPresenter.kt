package com.pagelence.presentation.assessment

import com.pagelence.core.assessment.AssessmentMode
import com.pagelence.core.assessment.AssessmentPlan
import com.pagelence.core.assessment.AssessmentResult
import com.pagelence.core.assessment.AssessmentSkill
import com.pagelence.uim3.Strings
import com.pagelence.uim3.model.AssessmentQuestionUi
import com.pagelence.uim3.model.AssessmentResultUi
import com.pagelence.uim3.model.AssessmentSkillUi

/**
 * Maps the placement-test plan and result to the UI models. Android-free so
 * both shells show the same paper and the same skill breakdown.
 */
class AssessmentPresenter(private val strings: Strings) {
    fun questions(plan: AssessmentPlan): List<AssessmentQuestionUi> = plan.questions.map { question ->
        val passage = plan.passages.firstOrNull { it.id == question.passageId }
        AssessmentQuestionUi(
            id = question.id,
            level = question.level.name,
            skillLabel = skillLabel(question.skill),
            prompt = question.prompt,
            options = question.options,
            selected = null,
            passageTitle = passage?.title,
            passageText = passage?.text,
        )
    }

    fun result(result: AssessmentResult): AssessmentResultUi {
        val band = if (result.bandLow == result.bandHigh) {
            result.bandLow.name
        } else {
            "${result.bandLow.name}–${result.bandHigh.name}"
        }
        return AssessmentResultUi(
            mode = result.mode,
            scorePercent = result.scorePercent,
            correct = result.correct,
            total = result.total,
            band = band,
            suggested = result.suggested.name,
            singleLevel = result.singleLevel,
            skills = result.skillPercent.entries.sortedBy { it.key }.map { (skill, percent) ->
                AssessmentSkillUi(skillLabel(skill), percent)
            },
        )
    }

    private fun skillLabel(skill: AssessmentSkill): String = when (skill) {
        AssessmentSkill.VOCAB -> strings.assessmentSkillVocab
        AssessmentSkill.GRAMMAR -> strings.assessmentSkillGrammar
        AssessmentSkill.READING -> strings.assessmentSkillReading
    }
}
