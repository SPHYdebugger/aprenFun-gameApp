package com.example.aprendemoslavida.utils

import android.content.Context
import com.example.aprendemoslavida.R
import com.example.aprendemoslavida.model.ArithmeticQuestion
import kotlin.random.Random

object MathAddSubQuestionBank {
    fun allQuestions(context: Context): List<ArithmeticQuestion> {
        val templates = listOf(
            Triple(7, '+', 5),
            Triple(9, '-', 4),
            Triple(6, '+', 8),
            Triple(15, '-', 7),
            Triple(23, '+', 6),
            Triple(34, '-', 8),
            Triple(5, '+', 9),
            Triple(18, '-', 9),
            Triple(42, '+', 7),
            Triple(27, '-', 5),
            Triple(8, '+', 3),
            Triple(30, '-', 4),
            Triple(16, '+', 5),
            Triple(50, '-', 6),
            Triple(4, '+', 7),
            Triple(21, '-', 9),
            Triple(33, '+', 4),
            Triple(19, '-', 8),
            Triple(62, '-', 7),
            Triple(14, '+', 6)
        )

        return templates.map { (a, op, b) ->
            val correct = if (op == '+') a + b else a - b
            val options = buildOptions(correct)
            val correctIndex = options.indexOf(correct)
            val expression = context.getString(
                R.string.math_add_sub_question_format,
                a,
                op.toString(),
                b
            )
            ArithmeticQuestion(expression, correct, options, correctIndex)
        }
    }

    private fun buildOptions(correct: Int): List<Int> {
        val options = LinkedHashSet<Int>()
        options.add(correct)
        while (options.size < 4) {
            val delta = Random.nextInt(1, 11)
            val candidate = if (Random.nextBoolean()) correct + delta else correct - delta
            if (candidate >= 0) {
                options.add(candidate)
            }
        }
        return options.toList().shuffled()
    }
}
