package com.example.aprendemoslavida.data.model

import com.google.gson.annotations.SerializedName

data class CreateGameSession(
    @SerializedName("childProfileId") val childProfileId: String,
    @SerializedName("subjectCode") val subjectCode: String,
    @SerializedName("topicCode") val topicCode: String? = null,
    @SerializedName("questionCount") val questionCount: Int = 20,
    @SerializedName("locale") val locale: String = "es-ES"
)

data class CastleProblem(
    @SerializedName("top") val top: Int,
    @SerializedName("bottom") val bottom: Int,
    @SerializedName("operation") val operation: String, // ADD, SUBTRACT
    @SerializedName("columns") val columns: Int,
    @SerializedName("attemptNumber") val attemptNumber: Int,
    @SerializedName("availablePoints") val availablePoints: Int
)

data class PresentedOption(
    @SerializedName("id") val id: String,
    @SerializedName("stableKey") val stableKey: String,
    @SerializedName("text") val text: String
)

data class PresentedQuestion(
    @SerializedName("id") val id: String,
    @SerializedName("contentQuestionId") val contentQuestionId: String,
    @SerializedName("position") val position: Int,
    @SerializedName("interaction") val interaction: String, // MULTIPLE_CHOICE, CASTLE
    @SerializedName("prompt") val prompt: String,
    @SerializedName("options") val options: List<PresentedOption> = emptyList(),
    @SerializedName("castle") val castle: CastleProblem? = null,
    @SerializedName("presentedAt") val presentedAt: String,
    @SerializedName("deadline") val deadline: String? = null
)

data class GameSession(
    @SerializedName("id") val id: String,
    @SerializedName("mode") val mode: String, // QUIZ, CASTLE
    @SerializedName("status") val status: String, // ACTIVE, COMPLETED, ABANDONED, EXPIRED
    @SerializedName("release") val release: String,
    @SerializedName("startedAt") val startedAt: String,
    @SerializedName("expiresAt") val expiresAt: String,
    @SerializedName("score") val score: Int,
    @SerializedName("answeredQuestions") val answeredQuestions: Int,
    @SerializedName("correctAnswers") val correctAnswers: Int,
    @SerializedName("totalQuestions") val totalQuestions: Int,
    @SerializedName("currentQuestion") val currentQuestion: PresentedQuestion? = null
)

data class SubmitGameAnswer(
    @SerializedName("sessionQuestionId") val sessionQuestionId: String,
    @SerializedName("selectedOptionId") val selectedOptionId: String? = null,
    @SerializedName("numericAnswer") val numericAnswer: Int? = null
)

data class GameAnswer(
    @SerializedName("answerId") val answerId: String,
    @SerializedName("sessionId") val sessionId: String,
    @SerializedName("sessionQuestionId") val sessionQuestionId: String,
    @SerializedName("correct") val correct: Boolean,
    @SerializedName("correctOptionId") val correctOptionId: String? = null,
    @SerializedName("correctNumericAnswer") val correctNumericAnswer: Int? = null,
    @SerializedName("questionResolved") val questionResolved: Boolean,
    @SerializedName("points") val points: Int,
    @SerializedName("totalScore") val totalScore: Int,
    @SerializedName("sessionStatus") val sessionStatus: String, // ACTIVE, COMPLETED
    @SerializedName("nextQuestion") val nextQuestion: PresentedQuestion? = null
)
