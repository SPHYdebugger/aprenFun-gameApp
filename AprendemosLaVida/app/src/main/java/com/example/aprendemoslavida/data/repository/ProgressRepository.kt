package com.example.aprendemoslavida.data.repository

import com.example.aprendemoslavida.data.api.LeaderboardApiService
import com.example.aprendemoslavida.data.api.ProgressApiService
import com.example.aprendemoslavida.data.model.PlayerProgress
import com.example.aprendemoslavida.data.model.PlayerRewards
import com.example.aprendemoslavida.data.model.PlayerScore
import com.example.aprendemoslavida.data.model.PublicLeaderboard
import com.example.aprendemoslavida.data.network.NetworkResult
import com.example.aprendemoslavida.data.network.ProblemJsonParser

class ProgressRepository(
    private val progressApiService: ProgressApiService,
    private val leaderboardApiService: LeaderboardApiService
) {

    suspend fun getPlayerProgress(childId: String): NetworkResult<PlayerProgress> {
        return try {
            val response = progressApiService.getPlayerProgress(childId)
            if (response.isSuccessful && response.body() != null) {
                NetworkResult.Success(response.body()!!)
            } else {
                NetworkResult.Error(ProblemJsonParser.parseProblem(response), response.code())
            }
        } catch (e: Exception) {
            NetworkResult.Exception(e)
        }
    }

    suspend fun getPlayerRewards(childId: String, locale: String = "es-ES"): NetworkResult<PlayerRewards> {
        return try {
            val response = progressApiService.getPlayerRewards(childId, locale)
            if (response.isSuccessful && response.body() != null) {
                NetworkResult.Success(response.body()!!)
            } else {
                NetworkResult.Error(ProblemJsonParser.parseProblem(response), response.code())
            }
        } catch (e: Exception) {
            NetworkResult.Exception(e)
        }
    }

    suspend fun getPlayerScores(childId: String): NetworkResult<List<PlayerScore>> {
        return try {
            val response = progressApiService.getPlayerScores(childId)
            if (response.isSuccessful && response.body() != null) {
                NetworkResult.Success(response.body()!!)
            } else {
                NetworkResult.Error(ProblemJsonParser.parseProblem(response), response.code())
            }
        } catch (e: Exception) {
            NetworkResult.Exception(e)
        }
    }

    suspend fun getPublicLeaderboard(
        mode: String? = null,
        period: String = "weekly",
        page: Int = 0,
        size: Int = 20
    ): NetworkResult<PublicLeaderboard> {
        return try {
            val response = leaderboardApiService.getPublicLeaderboard(mode, period, page, size)
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
