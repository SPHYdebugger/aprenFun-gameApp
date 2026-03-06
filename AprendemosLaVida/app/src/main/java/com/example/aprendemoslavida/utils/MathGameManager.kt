package com.example.aprendemoslavida.utils

import com.example.aprendemoslavida.model.MathQuestion
import kotlin.random.Random

class MathGameManager {
    private val questions: List<MathQuestion> = buildQuestions()

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
        fun allQuestions(): List<MathQuestion> = buildQuestions()

        private fun buildQuestions(): List<MathQuestion> {
            val allPairs = ArrayList<Pair<Int, Int>>(50)
            for (a in 1..5) {
                for (b in 1..10) {
                    allPairs.add(a to b)
                }
            }

            return allPairs
                .shuffled()
                .take(20)
                .map { (a, b) ->
                    val correct = a * b
                    val options = buildOptions(correct)
                    val correctIndex = options.indexOf(correct)
                    MathQuestion(a, b, correct, options, correctIndex)
                }
        }

        private fun buildOptions(correct: Int): List<Int> {
            val options = LinkedHashSet<Int>()
            options.add(correct)
            while (options.size < 4) {
                val candidate = Random.nextInt(1, 26)
                options.add(candidate)
            }
            return options.toList().shuffled()
        }
    }
}
