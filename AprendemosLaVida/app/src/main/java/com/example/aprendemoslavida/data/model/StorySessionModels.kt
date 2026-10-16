package com.example.aprendemoslavida.data.model

import com.google.gson.annotations.SerializedName

data class CreateStorySession(
    @SerializedName("childProfileId") val childProfileId: String,
    @SerializedName("mapCount") val mapCount: Int, // 3 or 5
    @SerializedName("durationSeconds") val durationSeconds: Int,
    @SerializedName("gateTopics") val gateTopics: List<String>,
    @SerializedName("locale") val locale: String = "es-ES"
)

data class StoryGate(
    @SerializedName("id") val id: String,
    @SerializedName("position") val position: Int,
    @SerializedName("topic") val topic: String,
    @SerializedName("optional") val optional: Boolean,
    @SerializedName("status") val status: String, // AVAILABLE, ACTIVE, RESOLVED
    @SerializedName("points") val points: Int,
    @SerializedName("currentQuestion") val currentQuestion: PresentedQuestion? = null
)

data class StoryMap(
    @SerializedName("id") val id: String,
    @SerializedName("position") val position: Int,
    @SerializedName("visualTheme") val visualTheme: String, // FOREST, CITY_TILESET, CITY_CLASSIC
    @SerializedName("layoutSeed") val layoutSeed: Long,
    @SerializedName("status") val status: String, // CURRENT, COMPLETED
    @SerializedName("resolvedGates") val resolvedGates: Int,
    @SerializedName("minimumGatesToExit") val minimumGatesToExit: Int = 5,
    @SerializedName("gates") val gates: List<StoryGate> = emptyList()
)

data class StorySanti(
    @SerializedName("availableOnCurrentMap") val availableOnCurrentMap: Boolean,
    @SerializedName("status") val status: String, // AVAILABLE, OFFERED, ACTIVE, DECLINED, SUCCEEDED, FAILED
    @SerializedName("rewardPoints") val rewardPoints: Int = 500,
    @SerializedName("currentChallenge") val currentChallenge: PresentedQuestion? = null
)

data class StorySession(
    @SerializedName("id") val id: String,
    @SerializedName("status") val status: String, // ACTIVE, COMPLETED, ABANDONED
    @SerializedName("release") val release: String,
    @SerializedName("locale") val locale: String,
    @SerializedName("startedAt") val startedAt: String,
    @SerializedName("remainingTimeMs") val remainingTimeMs: Long,
    @SerializedName("score") val score: Int,
    @SerializedName("mapCount") val mapCount: Int,
    @SerializedName("completedMaps") val completedMaps: Int,
    @SerializedName("finishReason") val finishReason: String? = null,
    @SerializedName("currentMap") val currentMap: StoryMap? = null,
    @SerializedName("santi") val santi: StorySanti? = null
)

data class SubmitStoryAnswer(
    @SerializedName("gateId") val gateId: String,
    @SerializedName("sessionQuestionId") val sessionQuestionId: String,
    @SerializedName("selectedOptionId") val selectedOptionId: String? = null,
    @SerializedName("numericAnswer") val numericAnswer: Int? = null
)

data class SantiActionRequest(
    @SerializedName("action") val action: String // OFFER, ACCEPT, DECLINE
)

data class SantiAnswerRequest(
    @SerializedName("sessionQuestionId") val sessionQuestionId: String,
    @SerializedName("numericAnswer") val numericAnswer: Int
)
