package com.example.aprendemoslavida.utils

import android.content.Context
import com.example.aprendemoslavida.model.Question

class GameManager(context: Context) {
    private val allQuestions: List<Question> = loadQuestions(context)

    private val questions: List<Question> = allQuestions
        .shuffled()
        .take(minOf(20, allQuestions.size))

    var currentIndex: Int = 0
        private set
    var score: Int = 0
        private set
    var totalTimeMs: Int = 0
        private set
    var correctAnswers: Int = 0
        private set

    fun totalQuestions(): Int = questions.size

    fun currentQuestion(): Question? = questions.getOrNull(currentIndex)

    fun addScore(points: Int) {
        score += points
    }

    fun addTime(ms: Int) {
        totalTimeMs += ms
    }

    fun addCorrect() {
        correctAnswers += 1
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

    private fun loadQuestions(context: Context): List<Question> = loadAllQuestions(context)

    companion object {
        fun loadAllQuestions(context: Context): List<Question> = NaturalQuestionBank.allQuestions(context)
    }
}
