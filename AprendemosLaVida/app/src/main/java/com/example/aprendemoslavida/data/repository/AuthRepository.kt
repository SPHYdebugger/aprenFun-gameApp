package com.example.aprendemoslavida.data.repository

import com.example.aprendemoslavida.data.api.AuthApiService
import com.example.aprendemoslavida.data.model.AcceptedResponse
import com.example.aprendemoslavida.data.model.AuthTokensDto
import com.example.aprendemoslavida.data.model.GuardianTokenRequest
import com.example.aprendemoslavida.data.model.GuardianTokenResponse
import com.example.aprendemoslavida.data.model.LoginRequest
import com.example.aprendemoslavida.data.model.RefreshRequest
import com.example.aprendemoslavida.data.model.RegisterRequest
import com.example.aprendemoslavida.data.model.VerifyEmailRequest
import com.example.aprendemoslavida.data.network.NetworkResult
import com.example.aprendemoslavida.data.network.ProblemJsonParser
import com.example.aprendemoslavida.data.storage.TokenStorage

class AuthRepository(
    private val authApiService: AuthApiService,
    private val tokenStorage: TokenStorage
) {

    suspend fun register(
        email: String,
        password: String,
        locale: String = "es-ES",
        countryCode: String = "ES",
        legalDocumentVersion: String = "1.0",
        parentalConsent: Boolean = true
    ): NetworkResult<AcceptedResponse> {
        return try {
            val response = authApiService.register(
                RegisterRequest(
                    email = email,
                    password = password,
                    locale = locale,
                    countryCode = countryCode,
                    legalDocumentVersion = legalDocumentVersion,
                    parentalConsent = parentalConsent
                )
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

    suspend fun verifyEmail(token: String): NetworkResult<Unit> {
        return try {
            val response = authApiService.verifyEmail(VerifyEmailRequest(token))
            if (response.isSuccessful) {
                NetworkResult.Success(Unit)
            } else {
                NetworkResult.Error(ProblemJsonParser.parseProblem(response), response.code())
            }
        } catch (e: Exception) {
            NetworkResult.Exception(e)
        }
    }

    suspend fun login(email: String, password: String): NetworkResult<AuthTokensDto> {
        return try {
            val installationId = tokenStorage.getOrGenerateInstallationId()
            val response = authApiService.login(
                LoginRequest(
                    email = email,
                    password = password,
                    installationId = installationId
                )
            )
            if (response.isSuccessful && response.body() != null) {
                val tokens = response.body()!!
                tokenStorage.saveAuthTokens(tokens)
                NetworkResult.Success(tokens)
            } else {
                NetworkResult.Error(ProblemJsonParser.parseProblem(response), response.code())
            }
        } catch (e: Exception) {
            NetworkResult.Exception(e)
        }
    }

    suspend fun refresh(): NetworkResult<AuthTokensDto> {
        val refreshToken = tokenStorage.getRefreshToken()
            ?: return NetworkResult.Error(
                com.example.aprendemoslavida.data.model.ProblemJson(
                    type = "about:blank",
                    title = "No Refresh Token",
                    status = 401,
                    detail = "No hay token de refresco disponible"
                ), 401
            )
        return try {
            val response = authApiService.refresh(RefreshRequest(refreshToken))
            if (response.isSuccessful && response.body() != null) {
                val tokens = response.body()!!
                tokenStorage.saveAuthTokens(tokens)
                NetworkResult.Success(tokens)
            } else {
                tokenStorage.clearTokens()
                NetworkResult.Error(ProblemJsonParser.parseProblem(response), response.code())
            }
        } catch (e: Exception) {
            NetworkResult.Exception(e)
        }
    }

    suspend fun logout(): NetworkResult<Unit> {
        val refreshToken = tokenStorage.getRefreshToken()
        return try {
            if (!refreshToken.isNullOrBlank()) {
                authApiService.logout(RefreshRequest(refreshToken))
            }
            tokenStorage.clearTokens()
            NetworkResult.Success(Unit)
        } catch (e: Exception) {
            tokenStorage.clearTokens()
            NetworkResult.Exception(e)
        }
    }

    suspend fun logoutAll(): NetworkResult<Unit> {
        return try {
            val response = authApiService.logoutAll()
            tokenStorage.clearTokens()
            if (response.isSuccessful) {
                NetworkResult.Success(Unit)
            } else {
                NetworkResult.Error(ProblemJsonParser.parseProblem(response), response.code())
            }
        } catch (e: Exception) {
            tokenStorage.clearTokens()
            NetworkResult.Exception(e)
        }
    }

    suspend fun createGuardianActionToken(
        password: String,
        purpose: String
    ): NetworkResult<GuardianTokenResponse> {
        return try {
            val response = authApiService.createGuardianActionToken(
                GuardianTokenRequest(password = password, purpose = purpose)
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
}
