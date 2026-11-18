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

data class StoryAnswerResponse(
    @SerializedName("correct") val correct: Boolean = false,
    @SerializedName("points") val points: Int = 0,
    @SerializedName("totalScore") val totalScore: Int = 0,
    @SerializedName("gateStatus") val gateStatus: String? = null,
    @SerializedName("correctOptionId") val correctOptionId: String? = null,
    @SerializedName("correctNumericAnswer") val correctNumericAnswer: Int? = null,
    @SerializedName("nextQuestion") val nextQuestion: PresentedQuestion? = null,
    @SerializedName("sessionStatus") val sessionStatus: String? = null
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

data class SantiState(
    @SerializedName("availableOnCurrentMap") val availableOnCurrentMap: Boolean,
    @SerializedName("status") val status: String, // AVAILABLE, OFFERED, ACTIVE, DECLINED, SUCCEEDED, FAILED
    @SerializedName("rewardPoints") val rewardPoints: Int = 500,
    @SerializedName("currentChallenge") val currentChallenge: PresentedQuestion? = null
)

typealias StorySanti = SantiState

data class SantiAnswerResponse(
    @SerializedName("correct") val correct: Boolean = false,
    @SerializedName("points") val points: Int = 0,
    @SerializedName("totalScore") val totalScore: Int = 0,
    @SerializedName("status") val status: String? = null,
    @SerializedName("nextChallenge") val nextChallenge: PresentedQuestion? = null
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

/**
 * POST /story-sessions/{id}/gates/{gateId}/open. The 200 schema is not in openapi.yaml (B-12),
 * and staging has returned two shapes:
 *  - 17-nov: the full StorySession snapshot (exercise inside currentMap.gates[i]) (F-14);
 *  - 18-nov: `{sessionId, remainingTimeMs, gate}` with the opened gate only (F-16).
 * Every field is nullable so either shape parses; use the helpers below.
 */
data class StoryGateOpenResponse(
    // 18-nov shape
    @SerializedName("sessionId") val sessionId: String? = null,
    @SerializedName("remainingTimeMs") val remainingTimeMs: Long? = null,
    @SerializedName("gate") val gate: StoryGate? = null,
    // 17-nov shape (StorySession snapshot)
    @SerializedName("id") val id: String? = null,
    @SerializedName("status") val status: String? = null,
    @SerializedName("release") val release: String? = null,
    @SerializedName("locale") val locale: String? = null,
    @SerializedName("startedAt") val startedAt: String? = null,
    @SerializedName("score") val score: Int? = null,
    @SerializedName("mapCount") val mapCount: Int? = null,
    @SerializedName("completedMaps") val completedMaps: Int? = null,
    @SerializedName("finishReason") val finishReason: String? = null,
    @SerializedName("currentMap") val currentMap: StoryMap? = null,
    @SerializedName("santi") val santi: StorySanti? = null
) {
    /** The full snapshot, only when the server sent one (17-nov shape). */
    fun fullSession(): StorySession? {
        if (id == null || status == null || currentMap == null) return null
        return StorySession(
            id = id, status = status, release = release.orEmpty(), locale = locale.orEmpty(),
            startedAt = startedAt.orEmpty(), remainingTimeMs = remainingTimeMs ?: 0L, score = score ?: 0,
            mapCount = mapCount ?: 0, completedMaps = completedMaps ?: 0, finishReason = finishReason,
            currentMap = currentMap, santi = santi
        )
    }

    /** The opened gate as the server reports it, whatever the shape. */
    fun openedGate(gateId: String): StoryGate? =
        gate?.takeIf { it.id == gateId } ?: currentMap?.gates?.firstOrNull { it.id == gateId }

    fun questionForGate(gateId: String): PresentedQuestion? = openedGate(gateId)?.currentQuestion

    /** Applies this response to the last known session without inventing a game status. */
    fun mergeInto(previous: StorySession): StorySession {
        fullSession()?.let { return it }
        val opened = gate ?: return previous
        val map = previous.currentMap ?: return previous
        return previous.copy(
            remainingTimeMs = remainingTimeMs ?: previous.remainingTimeMs,
            currentMap = map.copy(gates = map.gates.map { if (it.id == opened.id) opened else it })
        )
    }
}

fun StorySession.questionForGate(gateId: String): PresentedQuestion? =
    currentMap?.gates?.firstOrNull { it.id == gateId }?.currentQuestion
