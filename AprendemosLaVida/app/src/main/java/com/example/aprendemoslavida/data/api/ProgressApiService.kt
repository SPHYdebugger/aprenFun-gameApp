package com.example.aprendemoslavida.data.api

import com.example.aprendemoslavida.data.model.PlayerProgress
import com.example.aprendemoslavida.data.model.PlayerRewards
import com.example.aprendemoslavida.data.model.PlayerScore
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface ProgressApiService {

    @GET("children/{childId}/progress")
    suspend fun getPlayerProgress(
        @Path("childId") childId: String
    ): Response<PlayerProgress>

    @GET("children/{childId}/rewards")
    suspend fun getPlayerRewards(
        @Path("childId") childId: String,
        @Query("locale") locale: String = "es-ES"
    ): Response<PlayerRewards>

    @GET("children/{childId}/scores")
    suspend fun getPlayerScores(
        @Path("childId") childId: String
    ): Response<List<PlayerScore>>
}
