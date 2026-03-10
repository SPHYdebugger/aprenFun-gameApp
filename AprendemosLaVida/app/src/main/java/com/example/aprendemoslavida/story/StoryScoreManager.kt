package com.example.aprendemoslavida.story

import kotlin.math.max

// Scoring rules for story mode.
class StoryScoreManager {
    var score: Int = 0
        private set

    fun reset() {
        score = 0
    }

    fun onWrongAttempt(): Int {
        score -= 25
        return -25
    }

    fun onCorrectAnswer(elapsedMs: Long): Int {
        val seconds = (elapsedMs / 1000L).toInt()
        val bonus = max(0, 50 - (seconds * 5))
        val gained = 100 + bonus
        score += gained
        return gained
    }

    fun addPoints(points: Int) {
        score += points
    }
}
