package com.example.aprendemoslavida.utils

import android.content.Context
import com.example.aprendemoslavida.model.Question

class SocialGameManager(private val context: Context, topic: Int) {
    private val questions: List<Question> = allQuestions(context, topic)
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

    fun pointsForElapsed(elapsedMs: Int): Int {
        return when {
            elapsedMs < 4000 -> 100
            elapsedMs < 8000 -> 70
            elapsedMs < 12000 -> 40
            else -> 0
        }
    }

    companion object {
        const val TOPIC_SOLAR_SYSTEM = 0
        const val TOPIC_LANDSCAPE = 1

        fun allQuestions(context: Context): List<Question> {
            return SocialQuestionBank.allQuestions(context)
        }

        fun allQuestions(context: Context, topic: Int): List<Question> {
            return SocialQuestionBank.byTopic(context, topic)
        }
    }
}
