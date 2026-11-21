package com.pagelence.core.progress

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class QuizGradingTest {
    private val correct = listOf(0, 2, 1)

    @Test
    fun `grades answers and lists the missed questions`() {
        val grade = QuizGrading.grade(mapOf(0 to 0, 1 to 1, 2 to 2), correct)
        assertEquals(1, grade.correctCount)
        assertEquals(3, grade.total)
        assertEquals(2, grade.wrongCount)
        assertEquals(listOf(1, 2), grade.wrongIndices)
    }

    @Test
    fun `all correct is a clean result`() {
        val grade = QuizGrading.grade(mapOf(0 to 0, 1 to 2, 2 to 1), correct)
        assertEquals(3, grade.correctCount)
        assertTrue(grade.allCorrect)
    }

    @Test
    fun `unanswered questions count as missed`() {
        val grade = QuizGrading.grade(mapOf(0 to 0), correct)
        assertEquals(1, grade.correctCount)
        assertEquals(listOf(1, 2), grade.wrongIndices)
    }

    @Test
    fun `redo clears only the missed answers`() {
        val answers = mapOf(0 to 0, 1 to 1, 2 to 2)
        assertEquals(mapOf(0 to 0), QuizGrading.redoWrong(answers, correct))
    }
}
