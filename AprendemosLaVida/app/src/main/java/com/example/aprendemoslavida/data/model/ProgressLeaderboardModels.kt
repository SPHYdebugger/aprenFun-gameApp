package com.example.aprendemoslavida.data.model

import com.google.gson.annotations.SerializedName

data class TopicProgress(
    @SerializedName("subject") val subject: String,
    @SerializedName("topic") val topic: String,
    @SerializedName("attempts") val attempts: Int,
    @SerializedName("correctAnswers") val correctAnswers: Int,
    @SerializedName("totalPoints") val totalPoints: Long,
    @SerializedName("mastery") val mastery: Double,
    @SerializedName("lastActivityAt") val lastActivityAt: String? = null
)

data class PlayerProgress(
    @SerializedName("childProfileId") val childProfileId: String,
    @SerializedName("pointsBalance") val pointsBalance: Long,
    @SerializedName("currentStreak") val currentStreak: Int,
    @SerializedName("longestStreak") val longestStreak: Int,
    @SerializedName("topics") val topics: List<TopicProgress> = emptyList()
)

data class AchievementDto(
    @SerializedName("code") val code: String,
    @SerializedName("name") val name: String,
    @SerializedName("description") val description: String,
    @SerializedName("progress") val progress: Int,
    @SerializedName("threshold") val threshold: Int,
    @SerializedName("unlockedAt") val unlockedAt: String? = null
)

data class CosmeticDto(
    @SerializedName("code") val code: String,
    @SerializedName("type") val type: String,
    @SerializedName("assetKey") val assetKey: String,
    @SerializedName("pointsThreshold") val pointsThreshold: Int,
    @SerializedName("unlocked") val unlocked: Boolean,
    @SerializedName("unlockedAt") val unlockedAt: String? = null
)

data class PlayerRewards(
    @SerializedName("achievements") val achievements: List<AchievementDto> = emptyList(),
    @SerializedName("cosmetics") val cosmetics: List<CosmeticDto> = emptyList()
)

data class PlayerScore(
    @SerializedName("sessionId") val sessionId: String,
    @SerializedName("mode") val mode: String,
    @SerializedName("score") val score: Int,
    @SerializedName("correctAnswers") val correctAnswers: Int,
    @SerializedName("totalQuestions") val totalQuestions: Int,
    @SerializedName("playedAt") val playedAt: String,
    @SerializedName("source") val source: String // AUTHORITATIVE, LEGACY
)

data class LeaderboardEntry(
    @SerializedName("pseudonym") val pseudonym: String,
    @SerializedName("score") val score: Int,
    @SerializedName("date") val date: String
)

data class PublicLeaderboard(
    @SerializedName("entries") val entries: List<LeaderboardEntry> = emptyList(),
    @SerializedName("page") val page: Int,
    @SerializedName("size") val size: Int,
    @SerializedName("totalElements") val totalElements: Long,
    @SerializedName("totalPages") val totalPages: Int
)
