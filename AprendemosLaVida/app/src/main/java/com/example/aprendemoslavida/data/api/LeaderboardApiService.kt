package com.example.aprendemoslavida.data.api

import com.example.aprendemoslavida.data.model.PublicLeaderboard
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface LeaderboardApiService {

    @GET("leaderboards")
    suspend fun getPublicLeaderboard(
        @Query("mode") mode: String? = null,
        @Query("period") period: String = "weekly",
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20
    ): Response<PublicLeaderboard>
}
