package com.example.aprendemoslavida.utils

import android.content.Context
import com.example.aprendemoslavida.model.Question

class LanguageGameManager(context: Context) {
    private val questions: List<Question> = allQuestions(context)
        .shuffled()
        .take(20)

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

    companion object {
        fun allQuestions(_context: Context): List<Question> {
            return LanguageQuestionBank.allQuestions()
        }
    }
}
