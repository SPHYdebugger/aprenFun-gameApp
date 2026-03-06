package com.example.aprendemoslavida.utils

import android.content.Context
import com.example.aprendemoslavida.R
import com.example.aprendemoslavida.model.Question
import org.json.JSONArray

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
        fun loadAllQuestions(context: Context): List<Question> {
            return try {
                val json = context.resources.openRawResource(R.raw.questions)
                    .bufferedReader()
                    .use { it.readText() }
                val array = JSONArray(json)
                val list = ArrayList<Question>(array.length())
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val optionsArray = obj.getJSONArray("options")
                    val options = ArrayList<String>(optionsArray.length())
                    for (j in 0 until optionsArray.length()) {
                        options.add(optionsArray.getString(j))
                    }
                    list.add(
                        Question(
                            text = obj.getString("text"),
                            options = options,
                            correctIndex = obj.getInt("correctIndex")
                        )
                    )
                }
                list
            } catch (_: Exception) {
                emptyList()
            }
        }
    }
}
