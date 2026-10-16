package com.example.aprendemoslavida.data.api

import com.example.aprendemoslavida.data.model.CreateGameSession
import com.example.aprendemoslavida.data.model.GameAnswer
import com.example.aprendemoslavida.data.model.GameSession
import com.example.aprendemoslavida.data.model.SubmitGameAnswer
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

interface GameSessionApiService {

    @POST("game-sessions")
    suspend fun createGameSession(
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body request: CreateGameSession
    ): Response<GameSession>

    @GET("game-sessions/{sessionId}")
    suspend fun getGameSession(
        @Path("sessionId") sessionId: String
    ): Response<GameSession>

    @POST("game-sessions/{sessionId}/answers")
    suspend fun submitGameAnswer(
        @Path("sessionId") sessionId: String,
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body request: SubmitGameAnswer
    ): Response<GameAnswer>

    @POST("game-sessions/{sessionId}/abandon")
    suspend fun abandonGameSession(
        @Path("sessionId") sessionId: String
    ): Response<GameSession>

    @POST("game-sessions/{sessionId}/finish")
    suspend fun finishCastleGameSession(
        @Path("sessionId") sessionId: String
    ): Response<GameSession>
}
