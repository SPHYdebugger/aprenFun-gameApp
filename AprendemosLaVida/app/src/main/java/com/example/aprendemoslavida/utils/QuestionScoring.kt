package com.example.aprendemoslavida.utils

object QuestionScoring {
    fun pointsForElapsed(elapsedMs: Int, questionTimeMs: Int): Int {
        if (questionTimeMs <= 0) return 0
        val base = SettingsManager.maxPointsForQuestionTime(questionTimeMs)
        val third = questionTimeMs / 3
        val twoThirds = (questionTimeMs * 2) / 3
        return when {
            elapsedMs <= third -> base
            elapsedMs <= twoThirds -> (base * 0.7f).toInt()
            elapsedMs < questionTimeMs -> (base * 0.4f).toInt()
            else -> 0
        }
    }
}
