package com.pagelence.core.progress

/**
 * Free-function grading for the reading quiz. The UI only renders the result;
 * keeping the score and the missed set here makes the "本轮回顾" retry rule
 * testable without Compose.
 */
data class QuizGrade(
    val correctCount: Int,
    val total: Int,
    val wrongIndices: List<Int>,
) {
    val wrongCount: Int get() = wrongIndices.size

    val allCorrect: Boolean get() = wrongIndices.isEmpty()
}

object QuizGrading {
    /**
     * @param answers question index → chosen option index. Unanswered questions
     *   count as wrong, which is the state right after submit.
     */
    fun grade(answers: Map<Int, Int>, correctOptions: List<Int>): QuizGrade {
        val wrong = correctOptions.indices.filter { answers[it] != correctOptions[it] }
        return QuizGrade(
            correctCount = correctOptions.size - wrong.size,
            total = correctOptions.size,
            wrongIndices = wrong,
        )
    }

    /** Keep correct answers and clear the missed ones for a short retry. */
    fun redoWrong(answers: Map<Int, Int>, correctOptions: List<Int>): Map<Int, Int> {
        val wrong = grade(answers, correctOptions).wrongIndices.toSet()
        return answers.filterKeys { it !in wrong }
    }
}
