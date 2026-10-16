package com.example.aprendemoslavida.data.repository

import com.example.aprendemoslavida.data.api.GameSessionApiService
import com.example.aprendemoslavida.data.model.CreateGameSession
import com.example.aprendemoslavida.data.model.GameAnswer
import com.example.aprendemoslavida.data.model.GameSession
import com.example.aprendemoslavida.data.model.SubmitGameAnswer
import com.example.aprendemoslavida.data.network.NetworkResult
import com.example.aprendemoslavida.data.network.ProblemJsonParser
import java.util.UUID

class GameSessionRepository(
    private val gameSessionApiService: GameSessionApiService
) {

    suspend fun createGameSession(
        childProfileId: String,
        subjectCode: String,
        topicCode: String? = null,
        questionCount: Int = 20,
        locale: String = "es-ES",
        idempotencyKey: String = UUID.randomUUID().toString()
    ): NetworkResult<GameSession> {
        return try {
            val response = gameSessionApiService.createGameSession(
                idempotencyKey = idempotencyKey,
                request = CreateGameSession(childProfileId, subjectCode, topicCode, questionCount, locale)
            )
            if (response.isSuccessful && response.body() != null) {
                NetworkResult.Success(response.body()!!)
            } else {
                NetworkResult.Error(ProblemJsonParser.parseProblem(response), response.code())
            }
        } catch (e: Exception) {
            NetworkResult.Exception(e)
        }
    }

    suspend fun getGameSession(sessionId: String): NetworkResult<GameSession> {
        return try {
            val response = gameSessionApiService.getGameSession(sessionId)
            if (response.isSuccessful && response.body() != null) {
                NetworkResult.Success(response.body()!!)
            } else {
                NetworkResult.Error(ProblemJsonParser.parseProblem(response), response.code())
            }
        } catch (e: Exception) {
            NetworkResult.Exception(e)
        }
    }

    suspend fun submitGameAnswer(
        sessionId: String,
        sessionQuestionId: String,
        selectedOptionId: String? = null,
        numericAnswer: Int? = null,
        idempotencyKey: String = UUID.randomUUID().toString()
    ): NetworkResult<GameAnswer> {
        return try {
            val response = gameSessionApiService.submitGameAnswer(
                sessionId = sessionId,
                idempotencyKey = idempotencyKey,
                request = SubmitGameAnswer(sessionQuestionId, selectedOptionId, numericAnswer)
            )
            if (response.isSuccessful && response.body() != null) {
                NetworkResult.Success(response.body()!!)
            } else {
                NetworkResult.Error(ProblemJsonParser.parseProblem(response), response.code())
            }
        } catch (e: Exception) {
            NetworkResult.Exception(e)
        }
    }

    suspend fun abandonGameSession(sessionId: String): NetworkResult<GameSession> {
        return try {
            val response = gameSessionApiService.abandonGameSession(sessionId)
            if (response.isSuccessful && response.body() != null) {
                NetworkResult.Success(response.body()!!)
            } else {
                NetworkResult.Error(ProblemJsonParser.parseProblem(response), response.code())
            }
        } catch (e: Exception) {
            NetworkResult.Exception(e)
        }
    }

    suspend fun finishCastleGameSession(sessionId: String): NetworkResult<GameSession> {
        return try {
            val response = gameSessionApiService.finishCastleGameSession(sessionId)
            if (response.isSuccessful && response.body() != null) {
                NetworkResult.Success(response.body()!!)
            } else {
                NetworkResult.Error(ProblemJsonParser.parseProblem(response), response.code())
            }
        } catch (e: Exception) {
            NetworkResult.Exception(e)
        }
    }
}
