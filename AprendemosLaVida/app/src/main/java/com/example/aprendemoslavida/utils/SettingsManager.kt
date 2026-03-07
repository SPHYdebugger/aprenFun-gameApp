package com.example.aprendemoslavida.utils

import android.content.Context
import com.example.aprendemoslavida.story.StoryTopic
import kotlin.math.roundToInt

object SettingsManager {
    private const val PREFS_NAME = "aprendemos_prefs"
    private const val KEY_QUESTION_TIME_MS = "question_time_ms"
    private const val KEY_STORY_TROPHY_TOPIC_PREFIX = "story_trophy_topic_"
    private const val STORY_TROPHY_COUNT = 10

    private const val DEFAULT_QUESTION_TIME_MS = 12000
    private val AVAILABLE_TIMES_MS = listOf(8000, 12000, 16000, 20000)
    private val DEFAULT_STORY_TOPICS = listOf(
        StoryTopic.NATURAL,
        StoryTopic.MATH_MULTIPLICATION,
        StoryTopic.MATH_ADD_SUB,
        StoryTopic.ENGLISH,
        StoryTopic.SOCIAL,
        StoryTopic.NATURAL,
        StoryTopic.MATH_ADD_SUB,
        StoryTopic.ENGLISH,
        StoryTopic.SOCIAL,
        StoryTopic.NATURAL
    )

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

    fun storyTrophyCount(): Int = STORY_TROPHY_COUNT

    fun getStoryGateTopics(context: Context): List<StoryTopic> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val allTopics = StoryTopic.values()
        return (0 until STORY_TROPHY_COUNT).map { index ->
            val storedOrdinal = prefs.getInt(
                "$KEY_STORY_TROPHY_TOPIC_PREFIX$index",
                DEFAULT_STORY_TOPICS[index].ordinal
            )
            allTopics.getOrNull(storedOrdinal) ?: DEFAULT_STORY_TOPICS[index]
        }
    }

    fun setStoryGateTopics(context: Context, topics: List<StoryTopic>) {
        if (topics.size != STORY_TROPHY_COUNT) return
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().apply {
            topics.forEachIndexed { index, topic ->
                putInt("$KEY_STORY_TROPHY_TOPIC_PREFIX$index", topic.ordinal)
            }
        }.apply()
    }

    fun restoreRandomStoryGateTopics(context: Context): List<StoryTopic> {
        val allTopics = StoryTopic.values().toList()
        val randomTopics = (0 until STORY_TROPHY_COUNT).map { allTopics.random() }
        setStoryGateTopics(context, randomTopics)
        return randomTopics
    }
}
