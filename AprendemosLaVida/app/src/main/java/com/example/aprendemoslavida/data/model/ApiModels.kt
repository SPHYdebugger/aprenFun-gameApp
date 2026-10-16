package com.example.aprendemoslavida.data.model

import com.google.gson.annotations.SerializedName

/**
 * RFC 7807 Problem Details representation returned on HTTP errors (4xx / 5xx)
 */
data class ProblemJson(
    @SerializedName("type") val type: String? = null,
    @SerializedName("title") val title: String? = null,
    @SerializedName("status") val status: Int? = null,
    @SerializedName("detail") val detail: String? = null,
    @SerializedName("instance") val instance: String? = null
)

/**
 * Authentication DTOs
 */
data class RegisterRequest(
    @SerializedName("email") val email: String,
    @SerializedName("password") val password: String,
    @SerializedName("locale") val locale: String,
    @SerializedName("countryCode") val countryCode: String,
    @SerializedName("legalDocumentVersion") val legalDocumentVersion: String,
    @SerializedName("parentalConsent") val parentalConsent: Boolean = true
)

data class LoginRequest(
    @SerializedName("email") val email: String,
    @SerializedName("password") val password: String,
    @SerializedName("installationId") val installationId: String,
    @SerializedName("deviceLabel") val deviceLabel: String? = null
)

data class VerifyEmailRequest(
    @SerializedName("token") val token: String
)

data class RefreshRequest(
    @SerializedName("refreshToken") val refreshToken: String
)

data class AuthTokensDto(
    @SerializedName("tokenType") val tokenType: String,
    @SerializedName("accessToken") val accessToken: String,
    @SerializedName("accessTokenExpiresAt") val accessTokenExpiresAt: String,
    @SerializedName("refreshToken") val refreshToken: String,
    @SerializedName("installationId") val installationId: String
)

data class AcceptedResponse(
    @SerializedName("message") val message: String,
    @SerializedName("oneTimeToken") val oneTimeToken: String? = null
)

/**
 * Guardian Action Token DTOs
 */
data class GuardianTokenRequest(
    @SerializedName("password") val password: String,
    @SerializedName("purpose") val purpose: String
) {
    companion object {
        const val PURPOSE_MANAGE_CHILDREN = "MANAGE_CHILDREN"
        const val PURPOSE_DELETE_ACCOUNT = "DELETE_ACCOUNT"
    }
}

data class GuardianTokenResponse(
    @SerializedName("token") val token: String,
    @SerializedName("expiresAt") val expiresAt: String,
    @SerializedName("purpose") val purpose: String
)

/**
 * Account DTO
 */
data class AccountDto(
    @SerializedName("id") val id: String,
    @SerializedName("email") val email: String,
    @SerializedName("locale") val locale: String? = null,
    @SerializedName("countryCode") val countryCode: String? = null,
    @SerializedName("roles") val roles: List<String>? = null,
    @SerializedName("activeChildId") val activeChildId: String? = null,
    @SerializedName("createdAt") val createdAt: String? = null
)

/**
 * Child Profile DTOs
 */
data class ChildInputDto(
    @SerializedName("pseudonym") val pseudonym: String,
    @SerializedName("age") val age: Int,
    @SerializedName("version") val version: Int = 0
)

data class ChildDto(
    @SerializedName("id") val id: String,
    @SerializedName("pseudonym") val pseudonym: String,
    @SerializedName("age") val age: Int,
    @SerializedName("version") val version: Int = 0,
    @SerializedName("ageUpdatedAt") val ageUpdatedAt: String? = null,
    @SerializedName("createdAt") val createdAt: String? = null,
    @SerializedName("updatedAt") val updatedAt: String? = null
)

data class ActivationRequest(
    @SerializedName("platform") val platform: String = "ANDROID",
    @SerializedName("appVersion") val appVersion: String? = null
)
