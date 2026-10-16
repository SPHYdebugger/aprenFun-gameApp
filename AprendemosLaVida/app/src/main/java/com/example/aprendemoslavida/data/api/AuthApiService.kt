package com.example.aprendemoslavida.data.api

import com.example.aprendemoslavida.data.model.AcceptedResponse
import com.example.aprendemoslavida.data.model.AuthTokensDto
import com.example.aprendemoslavida.data.model.GuardianTokenRequest
import com.example.aprendemoslavida.data.model.GuardianTokenResponse
import com.example.aprendemoslavida.data.model.LoginRequest
import com.example.aprendemoslavida.data.model.RefreshRequest
import com.example.aprendemoslavida.data.model.RegisterRequest
import com.example.aprendemoslavida.data.model.VerifyEmailRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApiService {

    @POST("auth/register")
    suspend fun register(
        @Body request: RegisterRequest
    ): Response<AcceptedResponse>

    @POST("auth/verify-email")
    suspend fun verifyEmail(
        @Body request: VerifyEmailRequest
    ): Response<Void>

    @POST("auth/login")
    suspend fun login(
        @Body request: LoginRequest
    ): Response<AuthTokensDto>

    @POST("auth/refresh")
    suspend fun refresh(
        @Body request: RefreshRequest
    ): Response<AuthTokensDto>

    @POST("auth/logout")
    suspend fun logout(
        @Body request: RefreshRequest
    ): Response<Void>

    @POST("auth/logout-all")
    suspend fun logoutAll(): Response<Void>

    @POST("auth/guardian-action-token")
    suspend fun createGuardianActionToken(
        @Body request: GuardianTokenRequest
    ): Response<GuardianTokenResponse>
}
