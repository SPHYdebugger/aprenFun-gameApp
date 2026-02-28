package com.example.aprendemoslavida.utils

import android.content.Context
import com.example.aprendemoslavida.R
import com.example.aprendemoslavida.model.Question

class SocialGameManager(private val context: Context, topic: Int) {
    private val questions: List<Question> = buildQuestions(topic)
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

    private fun buildQuestions(topic: Int): List<Question> {
        return when (topic) {
            TOPIC_LANDSCAPE -> topicLandscapeQuestions()
            else -> topicSolarSystemQuestions()
        }
    }

    private fun q(text: Int, a: Int, b: Int, c: Int, d: Int, correct: Int = 0): Question {
        return Question(
            text = context.getString(text),
            options = listOf(
                context.getString(a),
                context.getString(b),
                context.getString(c),
                context.getString(d)
            ),
            correctIndex = correct
        )
    }

    private fun topicSolarSystemQuestions(): List<Question> {
        return listOf(
            q(R.string.social_t0_q1_text, R.string.social_t0_q1_a1, R.string.social_t0_q1_a2, R.string.social_t0_q1_a3, R.string.social_t0_q1_a4),
            q(R.string.social_t0_q2_text, R.string.social_t0_q2_a1, R.string.social_t0_q2_a2, R.string.social_t0_q2_a3, R.string.social_t0_q2_a4),
            q(R.string.social_t0_q3_text, R.string.social_t0_q3_a1, R.string.social_t0_q3_a2, R.string.social_t0_q3_a3, R.string.social_t0_q3_a4),
            q(R.string.social_t0_q4_text, R.string.social_t0_q4_a1, R.string.social_t0_q4_a2, R.string.social_t0_q4_a3, R.string.social_t0_q4_a4),
            q(R.string.social_t0_q5_text, R.string.social_t0_q5_a1, R.string.social_t0_q5_a2, R.string.social_t0_q5_a3, R.string.social_t0_q5_a4),
            q(R.string.social_t0_q6_text, R.string.social_t0_q6_a1, R.string.social_t0_q6_a2, R.string.social_t0_q6_a3, R.string.social_t0_q6_a4),
            q(R.string.social_t0_q7_text, R.string.social_t0_q7_a1, R.string.social_t0_q7_a2, R.string.social_t0_q7_a3, R.string.social_t0_q7_a4),
            q(R.string.social_t0_q8_text, R.string.social_t0_q8_a1, R.string.social_t0_q8_a2, R.string.social_t0_q8_a3, R.string.social_t0_q8_a4),
            q(R.string.social_t0_q9_text, R.string.social_t0_q9_a1, R.string.social_t0_q9_a2, R.string.social_t0_q9_a3, R.string.social_t0_q9_a4),
            q(R.string.social_t0_q10_text, R.string.social_t0_q10_a1, R.string.social_t0_q10_a2, R.string.social_t0_q10_a3, R.string.social_t0_q10_a4),
            q(R.string.social_t0_q11_text, R.string.social_t0_q11_a1, R.string.social_t0_q11_a2, R.string.social_t0_q11_a3, R.string.social_t0_q11_a4),
            q(R.string.social_t0_q12_text, R.string.social_t0_q12_a1, R.string.social_t0_q12_a2, R.string.social_t0_q12_a3, R.string.social_t0_q12_a4),
            q(R.string.social_t0_q13_text, R.string.social_t0_q13_a1, R.string.social_t0_q13_a2, R.string.social_t0_q13_a3, R.string.social_t0_q13_a4),
            q(R.string.social_t0_q14_text, R.string.social_t0_q14_a1, R.string.social_t0_q14_a2, R.string.social_t0_q14_a3, R.string.social_t0_q14_a4),
            q(R.string.social_t0_q15_text, R.string.social_t0_q15_a1, R.string.social_t0_q15_a2, R.string.social_t0_q15_a3, R.string.social_t0_q15_a4),
            q(R.string.social_t0_q16_text, R.string.social_t0_q16_a1, R.string.social_t0_q16_a2, R.string.social_t0_q16_a3, R.string.social_t0_q16_a4),
            q(R.string.social_t0_q17_text, R.string.social_t0_q17_a1, R.string.social_t0_q17_a2, R.string.social_t0_q17_a3, R.string.social_t0_q17_a4),
            q(R.string.social_t0_q18_text, R.string.social_t0_q18_a1, R.string.social_t0_q18_a2, R.string.social_t0_q18_a3, R.string.social_t0_q18_a4),
            q(R.string.social_t0_q19_text, R.string.social_t0_q19_a1, R.string.social_t0_q19_a2, R.string.social_t0_q19_a3, R.string.social_t0_q19_a4),
            q(R.string.social_t0_q20_text, R.string.social_t0_q20_a1, R.string.social_t0_q20_a2, R.string.social_t0_q20_a3, R.string.social_t0_q20_a4)
        )
    }

    private fun topicLandscapeQuestions(): List<Question> {
        return listOf(
            q(R.string.social_t1_q1_text, R.string.social_t1_q1_a1, R.string.social_t1_q1_a2, R.string.social_t1_q1_a3, R.string.social_t1_q1_a4),
            q(R.string.social_t1_q2_text, R.string.social_t1_q2_a1, R.string.social_t1_q2_a2, R.string.social_t1_q2_a3, R.string.social_t1_q2_a4),
            q(R.string.social_t1_q3_text, R.string.social_t1_q3_a1, R.string.social_t1_q3_a2, R.string.social_t1_q3_a3, R.string.social_t1_q3_a4),
            q(R.string.social_t1_q4_text, R.string.social_t1_q4_a1, R.string.social_t1_q4_a2, R.string.social_t1_q4_a3, R.string.social_t1_q4_a4),
            q(R.string.social_t1_q5_text, R.string.social_t1_q5_a1, R.string.social_t1_q5_a2, R.string.social_t1_q5_a3, R.string.social_t1_q5_a4),
            q(R.string.social_t1_q6_text, R.string.social_t1_q6_a1, R.string.social_t1_q6_a2, R.string.social_t1_q6_a3, R.string.social_t1_q6_a4),
            q(R.string.social_t1_q7_text, R.string.social_t1_q7_a1, R.string.social_t1_q7_a2, R.string.social_t1_q7_a3, R.string.social_t1_q7_a4),
            q(R.string.social_t1_q8_text, R.string.social_t1_q8_a1, R.string.social_t1_q8_a2, R.string.social_t1_q8_a3, R.string.social_t1_q8_a4),
            q(R.string.social_t1_q9_text, R.string.social_t1_q9_a1, R.string.social_t1_q9_a2, R.string.social_t1_q9_a3, R.string.social_t1_q9_a4),
            q(R.string.social_t1_q10_text, R.string.social_t1_q10_a1, R.string.social_t1_q10_a2, R.string.social_t1_q10_a3, R.string.social_t1_q10_a4),
            q(R.string.social_t1_q11_text, R.string.social_t1_q11_a1, R.string.social_t1_q11_a2, R.string.social_t1_q11_a3, R.string.social_t1_q11_a4),
            q(R.string.social_t1_q12_text, R.string.social_t1_q12_a1, R.string.social_t1_q12_a2, R.string.social_t1_q12_a3, R.string.social_t1_q12_a4),
            q(R.string.social_t1_q13_text, R.string.social_t1_q13_a1, R.string.social_t1_q13_a2, R.string.social_t1_q13_a3, R.string.social_t1_q13_a4),
            q(R.string.social_t1_q14_text, R.string.social_t1_q14_a1, R.string.social_t1_q14_a2, R.string.social_t1_q14_a3, R.string.social_t1_q14_a4),
            q(R.string.social_t1_q15_text, R.string.social_t1_q15_a1, R.string.social_t1_q15_a2, R.string.social_t1_q15_a3, R.string.social_t1_q15_a4),
            q(R.string.social_t1_q16_text, R.string.social_t1_q16_a1, R.string.social_t1_q16_a2, R.string.social_t1_q16_a3, R.string.social_t1_q16_a4),
            q(R.string.social_t1_q17_text, R.string.social_t1_q17_a1, R.string.social_t1_q17_a2, R.string.social_t1_q17_a3, R.string.social_t1_q17_a4),
            q(R.string.social_t1_q18_text, R.string.social_t1_q18_a1, R.string.social_t1_q18_a2, R.string.social_t1_q18_a3, R.string.social_t1_q18_a4),
            q(R.string.social_t1_q19_text, R.string.social_t1_q19_a1, R.string.social_t1_q19_a2, R.string.social_t1_q19_a3, R.string.social_t1_q19_a4),
            q(R.string.social_t1_q20_text, R.string.social_t1_q20_a1, R.string.social_t1_q20_a2, R.string.social_t1_q20_a3, R.string.social_t1_q20_a4)
        )
    }

    companion object {
        const val TOPIC_SOLAR_SYSTEM = 0
        const val TOPIC_LANDSCAPE = 1
    }
}
