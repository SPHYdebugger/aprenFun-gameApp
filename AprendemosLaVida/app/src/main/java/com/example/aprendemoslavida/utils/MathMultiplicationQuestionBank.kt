package com.example.aprendemoslavida.utils

import com.example.aprendemoslavida.model.MathQuestion
import kotlin.random.Random

object MathMultiplicationQuestionBank {
    fun allQuestions(): List<MathQuestion> {
        val allPairs = ArrayList<Pair<Int, Int>>(50)
        for (a in 2..9) {
            for (b in 2..10) {
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
