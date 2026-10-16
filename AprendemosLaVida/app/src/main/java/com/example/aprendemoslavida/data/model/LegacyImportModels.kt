package com.example.aprendemoslavida.data.model

import com.google.gson.annotations.SerializedName

data class LegacyImportPreferences(
    @SerializedName("soundEnabled") val soundEnabled: Boolean,
    @SerializedName("storyMusicEnabled") val storyMusicEnabled: Boolean,
    @SerializedName("questionTimeMs") val questionTimeMs: Int,
    @SerializedName("storyGameTimeMs") val storyGameTimeMs: Int,
    @SerializedName("storyMapCount") val storyMapCount: Int,
    @SerializedName("language") val language: String,
    @SerializedName("storyGateTopics") val storyGateTopics: List<String>
)

data class LegacyImportAvatar(
    @SerializedName("gender") val gender: String, // NINO, NINA
    @SerializedName("hairStyle") val hairStyle: Int,
    @SerializedName("hairColor") val hairColor: Int,
    @SerializedName("eyeShape") val eyeShape: Int,
    @SerializedName("eyeColor") val eyeColor: Int,
    @SerializedName("mouthShape") val mouthShape: Int,
    @SerializedName("mouthColor") val mouthColor: Int,
    @SerializedName("skinTone") val skinTone: Int,
    @SerializedName("outfit") val outfit: Int
)

data class LegacyImportStreak(
    @SerializedName("days") val days: Int,
    @SerializedName("lastCompletedDay") val lastCompletedDay: Int
)

data class LegacyImportScore(
    @SerializedName("mode") val mode: String,
    @SerializedName("score") val score: Int,
    @SerializedName("date") val date: String
)

data class LegacyImport(
    @SerializedName("sourceSchemaVersion") val sourceSchemaVersion: Int = 1,
    @SerializedName("sourceAppVersion") val sourceAppVersion: String? = null,
    @SerializedName("globalPoints") val globalPoints: Int,
    @SerializedName("preferences") val preferences: LegacyImportPreferences,
    @SerializedName("avatar") val avatar: LegacyImportAvatar,
    @SerializedName("streak") val streak: LegacyImportStreak,
    @SerializedName("scores") val scores: List<LegacyImportScore>
)

data class LegacyImportResult(
    @SerializedName("childProfileId") val childProfileId: String,
    @SerializedName("status") val status: String, // IMPORTED, ALREADY_IMPORTED
    @SerializedName("importedAt") val importedAt: String,
    @SerializedName("pointsImported") val pointsImported: Int,
    @SerializedName("scoresImported") val scoresImported: Int,
    @SerializedName("preferencesImported") val preferencesImported: Boolean,
    @SerializedName("avatarImported") val avatarImported: Boolean
)
