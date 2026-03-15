package com.example.aprendemoslavida.utils

import android.content.Context
import com.example.aprendemoslavida.story.StoryTopic
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

object SettingsManager {
    private const val PREFS_NAME = "aprendemos_prefs"
    private const val KEY_QUESTION_TIME_MS = "question_time_ms"
    private const val KEY_STORY_GAME_TIME_MS = "story_game_time_ms"
    private const val KEY_STORY_MAP_COUNT = "story_map_count"
    private const val KEY_SOUND_ENABLED = "sound_enabled"
    private const val KEY_STORY_TROPHY_TOPIC_PREFIX = "story_trophy_topic_"
    private const val KEY_STORY_STREAK_DAYS = "story_streak_days"
    private const val KEY_STORY_STREAK_LAST_DAY = "story_streak_last_day" // last day when streak screen was shown
    private const val KEY_STORY_STREAK_LAST_COMPLETED_DAY = "story_streak_last_completed_day"
    private const val KEY_STORY_STREAK_LAST_BONUS_DAY = "story_streak_last_bonus_day"
    private const val STORY_TROPHY_COUNT = 10

    private const val DEFAULT_QUESTION_TIME_MS = 12000
    private const val DEFAULT_STORY_GAME_TIME_MS = 7 * 60 * 1000
    private const val DEFAULT_STORY_MAP_COUNT = 3
    private val AVAILABLE_STORY_MAP_COUNTS = listOf(3, 5)
    private const val MIN_STORY_GAME_TIME_MS = 10 * 1000
    private const val MAX_STORY_GAME_TIME_MS = 59 * 60 * 1000 + 59 * 1000
    private val AVAILABLE_TIMES_MS = listOf(8000, 12000, 16000, 20000)
    private val DEFAULT_STORY_TOPICS = listOf(
        StoryTopic.NATURAL,
        StoryTopic.MATH_MULTIPLICATION,
        StoryTopic.ENGLISH,
        StoryTopic.SOCIAL,
        StoryTopic.LANGUAGE,
        StoryTopic.CASTLES,
        StoryTopic.MATH_MULTIPLICATION,
        StoryTopic.ENGLISH,
        StoryTopic.SOCIAL,
        StoryTopic.CASTLES
    )
    private val STORY_RANDOM_TOPIC_POOL = listOf(
        StoryTopic.NATURAL,
        StoryTopic.MATH_ADD_SUB,
        StoryTopic.MATH_MULTIPLICATION,
        StoryTopic.SOCIAL,
        StoryTopic.ENGLISH,
        StoryTopic.LANGUAGE,
        StoryTopic.CASTLES
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

    fun getStoryGameTimeMs(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val value = prefs.getInt(KEY_STORY_GAME_TIME_MS, DEFAULT_STORY_GAME_TIME_MS)
        return value.coerceIn(MIN_STORY_GAME_TIME_MS, MAX_STORY_GAME_TIME_MS)
    }

    fun setStoryGameTimeMs(context: Context, valueMs: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putInt(KEY_STORY_GAME_TIME_MS, valueMs.coerceIn(MIN_STORY_GAME_TIME_MS, MAX_STORY_GAME_TIME_MS))
            .apply()
    }

    fun isSoundEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_SOUND_ENABLED, true)
    }

    fun setSoundEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_SOUND_ENABLED, enabled).apply()
    }

    fun getStoryMapCount(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val value = prefs.getInt(KEY_STORY_MAP_COUNT, DEFAULT_STORY_MAP_COUNT)
        return if (AVAILABLE_STORY_MAP_COUNTS.contains(value)) value else DEFAULT_STORY_MAP_COUNT
    }

    fun setStoryMapCount(context: Context, mapCount: Int) {
        val safe = if (AVAILABLE_STORY_MAP_COUNTS.contains(mapCount)) mapCount else DEFAULT_STORY_MAP_COUNT
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putInt(KEY_STORY_MAP_COUNT, safe).apply()
    }

    fun defaultStoryTimeMsForMapCount(mapCount: Int): Int {
        return if (mapCount == 3) 6 * 60 * 1000 else 10 * 60 * 1000
    }

    fun applyStorySessionPreset(context: Context, mapCount: Int) {
        val safe = if (AVAILABLE_STORY_MAP_COUNTS.contains(mapCount)) mapCount else DEFAULT_STORY_MAP_COUNT
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putInt(KEY_STORY_MAP_COUNT, safe)
            .putInt(KEY_STORY_GAME_TIME_MS, defaultStoryTimeMsForMapCount(safe))
            .apply()
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
        val randomTopics = (0 until STORY_TROPHY_COUNT).map { STORY_RANDOM_TOPIC_POOL.random() }
        setStoryGateTopics(context, randomTopics)
        return randomTopics
    }

    data class StoryStreakState(
        val isFirstTime: Boolean,
        val streakDays: Int,
        val firstPlayToday: Boolean,
        val bonusForToday: Int,
        val nextRewardDays: Int?,
        val daysToNextReward: Int
    )

    fun previewStoryStreak(context: Context): StoryStreakState {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val storedDays = prefs.getInt(KEY_STORY_STREAK_DAYS, 0).coerceAtLeast(0)
        val lastShownDay = prefs.getInt(KEY_STORY_STREAK_LAST_DAY, 0)
        val lastCompletedDay = prefs.getInt(KEY_STORY_STREAK_LAST_COMPLETED_DAY, 0)
        val isFirstTime = storedDays == 0 && lastCompletedDay == 0
        val today = todayKey()
        val yesterday = yesterdayKey()
        val firstPlayToday = lastShownDay != today
        val completedToday = lastCompletedDay == today
        val projectedAfterFinish = when {
            completedToday -> storedDays
            lastCompletedDay == yesterday -> (storedDays + 1).coerceAtLeast(1)
            else -> 1
        }
        val nextReward = nextRewardAfter(storedDays)
        return StoryStreakState(
            isFirstTime = isFirstTime,
            streakDays = storedDays,
            firstPlayToday = firstPlayToday,
            bonusForToday = if (!completedToday) rewardForStreak(projectedAfterFinish) else 0,
            nextRewardDays = nextReward,
            daysToNextReward = if (nextReward != null) (nextReward - storedDays).coerceAtLeast(0) else 0
        )
    }

    fun consumeStoryStreakLaunch(context: Context): StoryStreakState {
        val preview = previewStoryStreak(context)
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val today = todayKey()
        if (preview.firstPlayToday) {
            prefs.edit().putInt(KEY_STORY_STREAK_LAST_DAY, today).apply()
        }
        return preview
    }

    fun registerStoryCompletedSession(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val storedDays = prefs.getInt(KEY_STORY_STREAK_DAYS, 0).coerceAtLeast(0)
        val lastCompletedDay = prefs.getInt(KEY_STORY_STREAK_LAST_COMPLETED_DAY, 0)
        val today = todayKey()
        val yesterday = yesterdayKey()

        if (lastCompletedDay == today) {
            return 0
        }

        val updatedDays = when {
            lastCompletedDay == yesterday -> (storedDays + 1).coerceAtLeast(1)
            else -> 1
        }
        val reward = rewardForStreak(updatedDays)
        prefs.edit()
            .putInt(KEY_STORY_STREAK_DAYS, updatedDays)
            .putInt(KEY_STORY_STREAK_LAST_COMPLETED_DAY, today)
            .putInt(KEY_STORY_STREAK_LAST_BONUS_DAY, if (reward > 0) today else prefs.getInt(KEY_STORY_STREAK_LAST_BONUS_DAY, 0))
            .apply()
        return reward
    }

    private fun rewardForStreak(days: Int): Int {
        return when (days) {
            3 -> 300
            5 -> 500
            7 -> 700
            else -> 0
        }
    }

    private fun nextRewardAfter(days: Int): Int? {
        val rewards = listOf(3, 5, 7)
        return rewards.firstOrNull { it > days }
    }

    private fun todayKey(): Int {
        val formatter = SimpleDateFormat("yyyyMMdd", Locale.US)
        return formatter.format(Date()).toIntOrNull() ?: 0
    }

    private fun yesterdayKey(): Int {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -1)
        val formatter = SimpleDateFormat("yyyyMMdd", Locale.US)
        return formatter.format(calendar.time).toIntOrNull() ?: 0
    }
}
