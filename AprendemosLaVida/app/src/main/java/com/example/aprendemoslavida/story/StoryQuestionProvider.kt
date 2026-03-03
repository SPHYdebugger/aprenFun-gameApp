package com.example.aprendemoslavida.story

import android.content.Context
import com.example.aprendemoslavida.model.Question
import com.example.aprendemoslavida.utils.AddSubMathGameManager
import com.example.aprendemoslavida.utils.EnglishGameManager
import com.example.aprendemoslavida.utils.GameManager
import com.example.aprendemoslavida.utils.MathGameManager
import com.example.aprendemoslavida.utils.SocialGameManager
import kotlin.random.Random

// Adapter that reuses existing quiz managers to provide one question per checkpoint.
class StoryQuestionProvider(private val context: Context) {
    fun nextQuestion(topic: StoryTopic): StoryQuestion {
        return when (topic) {
            StoryTopic.NATURAL -> fromClassicQuestion(GameManager(context).currentQuestion())
            StoryTopic.MATH_MULTIPLICATION -> {
                val q = MathGameManager().currentQuestion()
                StoryQuestion(
                    text = context.getString(com.example.aprendemoslavida.R.string.math_question_format, q?.a ?: 2, q?.b ?: 2),
                    options = (q?.options ?: listOf(4, 5, 6, 7)).map { it.toString() },
                    correctIndex = q?.correctIndex ?: 0
                )
            }
            StoryTopic.MATH_ADD_SUB -> {
                val q = AddSubMathGameManager(context).currentQuestion()
                StoryQuestion(
                    text = q?.expression ?: context.getString(
                        com.example.aprendemoslavida.R.string.math_add_sub_question_format,
                        3,
                        "+",
                        3
                    ),
                    options = (q?.options ?: listOf(6, 5, 7, 8)).map { it.toString() },
                    correctIndex = q?.correctIndex ?: 0
                )
            }
            StoryTopic.ENGLISH -> fromClassicQuestion(EnglishGameManager(context).currentQuestion())
            StoryTopic.SOCIAL -> {
                val topicId = if (Random.nextBoolean()) {
                    SocialGameManager.TOPIC_SOLAR_SYSTEM
                } else {
                    SocialGameManager.TOPIC_LANDSCAPE
                }
                fromClassicQuestion(SocialGameManager(context, topicId).currentQuestion())
            }
        }
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
