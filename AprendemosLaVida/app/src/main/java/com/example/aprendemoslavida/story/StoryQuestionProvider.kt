package com.example.aprendemoslavida.story

import android.content.Context
import com.example.aprendemoslavida.model.Question
import com.example.aprendemoslavida.utils.AddSubMathGameManager
import com.example.aprendemoslavida.utils.EnglishGameManager
import com.example.aprendemoslavida.utils.GameManager
import com.example.aprendemoslavida.utils.MathGameManager
import com.example.aprendemoslavida.utils.SocialGameManager

// Adapter that reuses existing quiz managers to provide one question per checkpoint.
class StoryQuestionProvider(private val context: Context) {
    private val pools = HashMap<StoryTopic, MutableList<StoryQuestion>>()
    private val poolIndices = HashMap<StoryTopic, Int>()

    fun nextQuestion(topic: StoryTopic): StoryQuestion {
        val pool = pools.getOrPut(topic) { buildPool(topic) }
        val index = poolIndices[topic] ?: 0
        if (pool.isEmpty() || index >= pool.size) {
            pools[topic] = buildPool(topic)
            poolIndices[topic] = 0
        }

        val refreshedPool = pools[topic] ?: buildPool(topic).also { pools[topic] = it }
        val safeIndex = poolIndices[topic] ?: 0
        val question = refreshedPool[safeIndex]
        poolIndices[topic] = safeIndex + 1
        return question
    }

    private fun buildPool(topic: StoryTopic): MutableList<StoryQuestion> {
        val questions = when (topic) {
            StoryTopic.NATURAL -> GameManager.loadAllQuestions(context).map { fromClassicQuestion(it) }
            StoryTopic.MATH_MULTIPLICATION -> MathGameManager.allQuestions().map { q ->
                StoryQuestion(
                    text = context.getString(
                        com.example.aprendemoslavida.R.string.math_question_format,
                        q.a,
                        q.b
                    ),
                    options = q.options.map { it.toString() },
                    correctIndex = q.correctIndex
                )
            }
            StoryTopic.MATH_ADD_SUB -> AddSubMathGameManager.allQuestions(context).map { q ->
                StoryQuestion(
                    text = q.expression,
                    options = q.options.map { it.toString() },
                    correctIndex = q.correctIndex
                )
            }
            StoryTopic.ENGLISH -> EnglishGameManager.allQuestions(context).map { fromClassicQuestion(it) }
            StoryTopic.SOCIAL -> SocialGameManager.allQuestions(context).map { fromClassicQuestion(it) }
        }.shuffled()

        return questions.toMutableList()
    }

    private fun fromClassicQuestion(question: Question?): StoryQuestion {
        if (question == null) {
            return StoryQuestion(
                text = "Pregunta no disponible",
                options = listOf("A", "B", "C", "D"),
                correctIndex = 0
            )
        }
        return StoryQuestion(
            text = question.text,
            options = question.options.take(4),
            correctIndex = question.correctIndex.coerceIn(0, 3)
        )
    }
}
