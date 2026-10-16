package com.example.aprendemoslavida.data.api

import com.example.aprendemoslavida.data.model.CreateStorySession
import com.example.aprendemoslavida.data.model.SantiActionRequest
import com.example.aprendemoslavida.data.model.SantiAnswerRequest
import com.example.aprendemoslavida.data.model.StorySession
import com.example.aprendemoslavida.data.model.SubmitStoryAnswer
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

interface StorySessionApiService {

    @POST("story-sessions")
    suspend fun createStorySession(
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body request: CreateStorySession
    ): Response<StorySession>

    @GET("story-sessions/{sessionId}")
    suspend fun getStorySession(
        @Path("sessionId") sessionId: String
    ): Response<StorySession>

    @POST("story-sessions/{sessionId}/gates/{gateId}/open")
    suspend fun openStoryGate(
        @Path("sessionId") sessionId: String,
        @Path("gateId") gateId: String
    ): Response<Void>

    @POST("story-sessions/{sessionId}/answers")
    suspend fun submitStoryAnswer(
        @Path("sessionId") sessionId: String,
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body request: SubmitStoryAnswer
    ): Response<Void>

    @POST("story-sessions/{sessionId}/maps/{mapId}/exit")
    suspend fun exitStoryMap(
        @Path("sessionId") sessionId: String,
        @Path("mapId") mapId: String
    ): Response<StorySession>

    @POST("story-sessions/{sessionId}/santi")
    suspend fun updateSantiEncounter(
        @Path("sessionId") sessionId: String,
        @Body request: SantiActionRequest
    ): Response<Void>

    @POST("story-sessions/{sessionId}/santi/answers")
    suspend fun submitSantiAnswer(
        @Path("sessionId") sessionId: String,
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body request: SantiAnswerRequest
    ): Response<Void>

    @POST("story-sessions/{sessionId}/finish")
    suspend fun finishStorySession(
        @Path("sessionId") sessionId: String
    ): Response<Void>

    @POST("story-sessions/{sessionId}/abandon")
    suspend fun abandonStorySession(
        @Path("sessionId") sessionId: String
    ): Response<Void>
}
