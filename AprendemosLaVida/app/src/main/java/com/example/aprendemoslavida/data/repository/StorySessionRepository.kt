package com.example.aprendemoslavida.data.repository

import com.example.aprendemoslavida.data.api.StorySessionApiService
import com.example.aprendemoslavida.data.model.CreateStorySession
import com.example.aprendemoslavida.data.model.SantiActionRequest
import com.example.aprendemoslavida.data.model.SantiAnswerRequest
import com.example.aprendemoslavida.data.model.StorySession
import com.example.aprendemoslavida.data.model.SubmitStoryAnswer
import com.example.aprendemoslavida.data.network.NetworkResult
import com.example.aprendemoslavida.data.network.ProblemJsonParser
import java.util.UUID

class StorySessionRepository(
    private val storySessionApiService: StorySessionApiService
) {

    suspend fun createStorySession(
        childProfileId: String,
        mapCount: Int,
        durationSeconds: Int,
        gateTopics: List<String>,
        locale: String = "es-ES",
        idempotencyKey: String = UUID.randomUUID().toString()
    ): NetworkResult<StorySession> {
        return try {
            val response = storySessionApiService.createStorySession(
                idempotencyKey = idempotencyKey,
                request = CreateStorySession(childProfileId, mapCount, durationSeconds, gateTopics, locale)
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

    suspend fun getStorySession(sessionId: String): NetworkResult<StorySession> {
        return try {
            val response = storySessionApiService.getStorySession(sessionId)
            if (response.isSuccessful && response.body() != null) {
                NetworkResult.Success(response.body()!!)
            } else {
                NetworkResult.Error(ProblemJsonParser.parseProblem(response), response.code())
            }
        } catch (e: Exception) {
            NetworkResult.Exception(e)
        }
    }

    suspend fun openStoryGate(sessionId: String, gateId: String): NetworkResult<Unit> {
        return try {
            val response = storySessionApiService.openStoryGate(sessionId, gateId)
            if (response.isSuccessful) {
                NetworkResult.Success(Unit)
            } else {
                NetworkResult.Error(ProblemJsonParser.parseProblem(response), response.code())
            }
        } catch (e: Exception) {
            NetworkResult.Exception(e)
        }
    }

    suspend fun submitStoryAnswer(
        sessionId: String,
        gateId: String,
        sessionQuestionId: String,
        selectedOptionId: String? = null,
        numericAnswer: Int? = null,
        idempotencyKey: String = UUID.randomUUID().toString()
    ): NetworkResult<Unit> {
        return try {
            val response = storySessionApiService.submitStoryAnswer(
                sessionId = sessionId,
                idempotencyKey = idempotencyKey,
                request = SubmitStoryAnswer(gateId, sessionQuestionId, selectedOptionId, numericAnswer)
            )
            if (response.isSuccessful) {
                NetworkResult.Success(Unit)
            } else {
                NetworkResult.Error(ProblemJsonParser.parseProblem(response), response.code())
            }
        } catch (e: Exception) {
            NetworkResult.Exception(e)
        }
    }

    suspend fun exitStoryMap(sessionId: String, mapId: String): NetworkResult<StorySession> {
        return try {
            val response = storySessionApiService.exitStoryMap(sessionId, mapId)
            if (response.isSuccessful && response.body() != null) {
                NetworkResult.Success(response.body()!!)
            } else {
                NetworkResult.Error(ProblemJsonParser.parseProblem(response), response.code())
            }
        } catch (e: Exception) {
            NetworkResult.Exception(e)
        }
    }

    suspend fun updateSantiEncounter(sessionId: String, action: String): NetworkResult<Unit> {
        return try {
            val response = storySessionApiService.updateSantiEncounter(sessionId, SantiActionRequest(action))
            if (response.isSuccessful) {
                NetworkResult.Success(Unit)
            } else {
                NetworkResult.Error(ProblemJsonParser.parseProblem(response), response.code())
            }
        } catch (e: Exception) {
            NetworkResult.Exception(e)
        }
    }

    suspend fun submitSantiAnswer(
        sessionId: String,
        sessionQuestionId: String,
        numericAnswer: Int,
        idempotencyKey: String = UUID.randomUUID().toString()
    ): NetworkResult<Unit> {
        return try {
            val response = storySessionApiService.submitSantiAnswer(
                sessionId = sessionId,
                idempotencyKey = idempotencyKey,
                request = SantiAnswerRequest(sessionQuestionId, numericAnswer)
            )
            if (response.isSuccessful) {
                NetworkResult.Success(Unit)
            } else {
                NetworkResult.Error(ProblemJsonParser.parseProblem(response), response.code())
            }
        } catch (e: Exception) {
            NetworkResult.Exception(e)
        }
    }

    suspend fun finishStorySession(sessionId: String): NetworkResult<Unit> {
        return try {
            val response = storySessionApiService.finishStorySession(sessionId)
            if (response.isSuccessful) {
                NetworkResult.Success(Unit)
            } else {
                NetworkResult.Error(ProblemJsonParser.parseProblem(response), response.code())
            }
        } catch (e: Exception) {
            NetworkResult.Exception(e)
        }
    }

    suspend fun abandonStorySession(sessionId: String): NetworkResult<Unit> {
        return try {
            val response = storySessionApiService.abandonStorySession(sessionId)
            if (response.isSuccessful) {
                NetworkResult.Success(Unit)
            } else {
                NetworkResult.Error(ProblemJsonParser.parseProblem(response), response.code())
            }
        } catch (e: Exception) {
            NetworkResult.Exception(e)
        }
    }
}
