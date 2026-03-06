package com.example.aprendemoslavida.utils

import android.content.Context
import kotlin.math.roundToInt

object SettingsManager {
    private const val PREFS_NAME = "aprendemos_prefs"
    private const val KEY_QUESTION_TIME_MS = "question_time_ms"

    private const val DEFAULT_QUESTION_TIME_MS = 12000
    private val AVAILABLE_TIMES_MS = listOf(8000, 12000, 16000, 20000)

    fun availableQuestionTimesMs(): List<Int> = AVAILABLE_TIMES_MS

    fun getQuestionTimeMs(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val value = prefs.getInt(KEY_QUESTION_TIME_MS, DEFAULT_QUESTION_TIME_MS)
        return if (AVAILABLE_TIMES_MS.contains(value)) value else DEFAULT_QUESTION_TIME_MS
    }

    fun setQuestionTimeMs(context: Context, value: Int) {
        val safeValue = if (AVAILABLE_TIMES_MS.contains(value)) value else DEFAULT_QUESTION_TIME_MS
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putInt(KEY_QUESTION_TIME_MS, safeValue).apply()
    }

    fun maxPointsForQuestionTime(questionTimeMs: Int): Int {
        val base = (12000f / questionTimeMs.toFloat()) * 100f
        return base.roundToInt().coerceIn(50, 100)
    }
}
