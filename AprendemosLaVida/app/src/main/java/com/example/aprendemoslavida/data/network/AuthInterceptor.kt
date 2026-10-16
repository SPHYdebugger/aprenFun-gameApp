package com.example.aprendemoslavida.data.network

import com.example.aprendemoslavida.data.storage.TokenStorage
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Interceptor that injects:
 * - Accept: application/json
 * - X-Installation-Id: <uuid>
 * - Authorization: Bearer <accessToken> (unless request is unauthenticated auth endpoint)
 */
class AuthInterceptor(
    private val tokenStorage: TokenStorage
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val path = originalRequest.url.encodedPath

        val builder = originalRequest.newBuilder()
            .header("Accept", "application/json")
            .header("X-Installation-Id", tokenStorage.getOrGenerateInstallationId())

        // Exclude Bearer token for public auth endpoints
        val isPublicAuthEndpoint = path.endsWith("/auth/login") ||
                path.endsWith("/auth/register") ||
                path.endsWith("/auth/verify-email") ||
                path.endsWith("/auth/refresh") ||
                path.endsWith("/auth/password/forgot") ||
                path.endsWith("/auth/password/reset")

        if (!isPublicAuthEndpoint) {
            val accessToken = tokenStorage.getAccessToken()
            if (!accessToken.isNullOrBlank()) {
                builder.header("Authorization", "Bearer $accessToken")
            }
        }

        return chain.proceed(builder.build())
    }
}
