package com.example.aprendemoslavida.data.network

import android.util.Log
import com.example.aprendemoslavida.data.model.AuthTokensDto
import com.example.aprendemoslavida.data.model.RefreshRequest
import com.example.aprendemoslavida.data.storage.TokenStorage
import com.google.gson.Gson
import okhttp3.Authenticator
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.Route

/**
 * OkHttp Authenticator for handling HTTP 401 Unauthorized responses.
 * Executes synchronized token renewal via POST /auth/refresh exactly once.
 * Never modifies or retries mutations with a different Idempotency-Key.
 */
class TokenAuthenticator(
    private val tokenStorage: TokenStorage,
    private val baseUrl: String,
    private val gson: Gson = Gson()
) : Authenticator {

    private val lock = Any()

    override fun authenticate(route: Route?, response: Response): Request? {
        val path = response.request.url.encodedPath

        // Avoid infinite loop if 401 occurs on refresh or login endpoints
        if (path.endsWith("/auth/refresh") ||
            path.endsWith("/auth/login") ||
            path.endsWith("/auth/register")
        ) {
            return null
        }

        // Limit retries to 1
        if (responseCount(response) >= 2) {
            Log.w(TAG, "401 retry limit reached for path: $path")
            tokenStorage.clearTokens()
            return null
        }

        synchronized(lock) {
            val currentAccessToken = tokenStorage.getAccessToken()
            val requestAccessToken = response.request.header("Authorization")
                ?.removePrefix("Bearer ")
                ?.trim()

            // If another concurrent request refreshed the token while we were waiting
            if (!currentAccessToken.isNullOrBlank() && currentAccessToken != requestAccessToken) {
                return response.request.newBuilder()
                    .header("Authorization", "Bearer $currentAccessToken")
                    .build()
            }

            // Perform synchronous refresh call
            val refreshToken = tokenStorage.getRefreshToken()
            if (refreshToken.isNullOrBlank()) {
                Log.w(TAG, "No refresh token available in storage")
                tokenStorage.clearTokens()
                return null
            }

            val newTokens = executeRefreshTokenCall(refreshToken)
            return if (newTokens != null) {
                tokenStorage.saveAuthTokens(newTokens)
                Log.i(TAG, "Token successfully refreshed")
                response.request.newBuilder()
                    .header("Authorization", "Bearer ${newTokens.accessToken}")
                    .build()
            } else {
                Log.e(TAG, "Token refresh call failed, clearing local session")
                tokenStorage.clearTokens()
                null
            }
        }
    }

    private fun executeRefreshTokenCall(refreshToken: String): AuthTokensDto? {
        val refreshUrl = baseUrl.trimEnd('/') + "/auth/refresh"
        val requestBodyJson = gson.toJson(RefreshRequest(refreshToken))
        val body = requestBodyJson.toRequestBody("application/json; charset=utf-8".toMediaType())

        val request = Request.Builder()
            .url(refreshUrl)
            .post(body)
            .header("Accept", "application/json")
            .header("X-Installation-Id", tokenStorage.getOrGenerateInstallationId())
            .build()

        val rawClient = OkHttpClient.Builder().build()
        return try {
            rawClient.newCall(request).execute().use { refreshResponse ->
                if (refreshResponse.isSuccessful) {
                    val responseBodyString = refreshResponse.body?.string()
                    if (!responseBodyString.isNullOrBlank()) {
                        gson.fromJson(responseBodyString, AuthTokensDto::class.java)
                    } else null
                } else {
                    Log.e(TAG, "Refresh HTTP failed with status: ${refreshResponse.code}")
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during refresh token call", e)
            null
        }
    }

    private fun responseCount(response: Response): Int {
        var result = 1
        var priorResponse = response.priorResponse
        while (priorResponse != null) {
            result++
            priorResponse = priorResponse.priorResponse
        }
        return result
    }

    companion object {
        private const val TAG = "TokenAuthenticator"
    }
}
