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
    private val recentQuestionKeys = HashMap<StoryTopic, MutableList<String>>()

    fun nextQuestion(topic: StoryTopic): StoryQuestion {
        val pool = pools.getOrPut(topic) { buildPool(topic) }
        if (pool.isEmpty()) {
            pools[topic] = buildPool(topic)
        }
        val refreshedPool = pools[topic] ?: mutableListOf()
        if (refreshedPool.isEmpty()) {
            return StoryQuestion(
                text = "Pregunta no disponible",
                options = listOf("A", "B", "C", "D"),
                correctIndex = 0
            )
        }

        val recent = recentQuestionKeys.getOrPut(topic) { mutableListOf() }
        val selected = pickRandomWithRecentAvoidance(refreshedPool, recent)
        registerRecent(selected, refreshedPool.size, recent)

        // Re-shuffle answers on each ask to avoid repeated option order.
        return shuffledStoryQuestion(selected.text, selected.options, selected.correctIndex)
    }

    private fun buildPool(topic: StoryTopic): MutableList<StoryQuestion> {
        val questions = when (topic) {
            StoryTopic.NATURAL -> GameManager.loadAllQuestions(context).map { fromClassicQuestion(it) }
            StoryTopic.MATH_MULTIPLICATION -> MathGameManager.allQuestions().map { q ->
                shuffledStoryQuestion(
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
                shuffledStoryQuestion(
                    text = q.expression,
                    options = q.options.map { it.toString() },
                    correctIndex = q.correctIndex
                )
            }
            StoryTopic.ENGLISH -> EnglishGameManager.allQuestions(context).map { q ->
                fromClassicQuestion(
                    q.copy(text = EnglishGameManager.normalizeQuestionTextForPrompt(context, q.text))
                )
            }
            StoryTopic.SOCIAL -> SocialGameManager.allQuestions(context).map { fromClassicQuestion(it) }
        }.shuffled()

        return questions.toMutableList()
    }

    private fun pickRandomWithRecentAvoidance(
        pool: List<StoryQuestion>,
        recent: MutableList<String>
    ): StoryQuestion {
        if (recent.isEmpty()) return pool.random()
        val recentSet = recent.toHashSet()
        val candidates = pool.filter { !recentSet.contains(questionKey(it)) }
        return if (candidates.isNotEmpty()) {
            candidates.random()
        } else {
            pool.random()
        }
    }

    private fun registerRecent(question: StoryQuestion, poolSize: Int, recent: MutableList<String>) {
        val key = questionKey(question)
        recent.add(key)
        val maxRecent = (poolSize / 3).coerceIn(6, 30)
        while (recent.size > maxRecent) {
            recent.removeAt(0)
        }
    }

    private fun questionKey(question: StoryQuestion): String {
        return buildString {
            append(question.text)
            append('|')
            append(question.options.getOrNull(question.correctIndex) ?: "")
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
        return shuffledStoryQuestion(
            text = question.text,
            options = question.options.take(4),
            correctIndex = question.correctIndex.coerceIn(0, 3)
        )
    }

    private fun shuffledStoryQuestion(
        text: String,
        options: List<String>,
        correctIndex: Int
    ): StoryQuestion {
        val normalized = options.take(4)
        if (normalized.isEmpty()) {
            return StoryQuestion(text = text, options = listOf("A", "B", "C", "D"), correctIndex = 0)
        }
        val safeCorrect = correctIndex.coerceIn(0, normalized.lastIndex)
        val withFlags = normalized.mapIndexed { index, option -> option to (index == safeCorrect) }.shuffled()
        val newCorrect = withFlags.indexOfFirst { it.second }.coerceAtLeast(0)
        return StoryQuestion(
            text = text,
            options = withFlags.map { it.first },
            correctIndex = newCorrect
        )
    }
}
