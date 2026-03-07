package com.example.aprendemoslavida.utils

import com.example.aprendemoslavida.model.MathQuestion

class MathGameManager {
    private val questions: List<MathQuestion> = allQuestions()

    var currentIndex: Int = 0
        private set
    var score: Int = 0
        private set
    var totalTimeMs: Int = 0
        private set

    fun totalQuestions(): Int = questions.size

    fun currentQuestion(): MathQuestion? = questions.getOrNull(currentIndex)

    fun addScore(points: Int) {
        score += points
    }

    fun addTime(ms: Int) {
        totalTimeMs += ms
    }

    fun moveNext(): Boolean {
        currentIndex += 1
        return currentIndex < questions.size
    }

    fun pointsForElapsed(elapsedMs: Int): Int {
        return when {
            elapsedMs < 4000 -> 100
            elapsedMs < 8000 -> 70
            elapsedMs < 12000 -> 40
            else -> 0
        }
    }

    companion object {
        fun allQuestions(): List<MathQuestion> = MathMultiplicationQuestionBank.allQuestions()
    }
}
