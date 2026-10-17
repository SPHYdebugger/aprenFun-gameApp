package com.example.aprendemoslavida.data.storage

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.example.aprendemoslavida.data.model.AuthTokensDto
import java.util.UUID

/**
 * Secure storage using Android Keystore / EncryptedSharedPreferences
 * Stores: installationId (UUID), accessToken, refreshToken, activeChildId
 * Prohibits storing adult passwords or persistent guardian tokens.
 */
class TokenStorage(context: Context, preferencesOverride: SharedPreferences? = null) {

    private val prefs: SharedPreferences = preferencesOverride ?: createEncryptedSharedPreferences(context)

    init {
        // Ensure X-Installation-Id exists on initialization
        getOrGenerateInstallationId()
    }

    private fun createEncryptedSharedPreferences(context: Context): SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        return EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    /**
     * Gets or generates a persistent X-Installation-Id as a unique UUID v4.
     * Guaranteed NOT to use AAID, IMEI, MAC or any hardware identifiers.
     */
    fun getOrGenerateInstallationId(): String {
        var id = prefs.getString(KEY_INSTALLATION_ID, null)
        if (id.isNull_or_empty_custom()) {
            id = UUID.randomUUID().toString()
            prefs.edit().putString(KEY_INSTALLATION_ID, id).apply()
        }
        return id!!
    }

    fun saveAuthTokens(tokens: AuthTokensDto) {
        prefs.edit()
            .putString(KEY_ACCESS_TOKEN, tokens.accessToken)
            .putString(KEY_ACCESS_TOKEN_EXPIRES_AT, tokens.accessTokenExpiresAt)
            .putString(KEY_REFRESH_TOKEN, tokens.refreshToken)
            .putString(KEY_INSTALLATION_ID, tokens.installationId)
            .apply()
    }

    fun getAccessToken(): String? = prefs.getString(KEY_ACCESS_TOKEN, null)

    fun getRefreshToken(): String? = prefs.getString(KEY_REFRESH_TOKEN, null)

    fun getAccessTokenExpiresAt(): String? = prefs.getString(KEY_ACCESS_TOKEN_EXPIRES_AT, null)

    fun saveActiveChildId(childId: String?) {
        if (childId == null) {
            prefs.edit().remove(KEY_ACTIVE_CHILD_ID).apply()
        } else {
            prefs.edit().putString(KEY_ACTIVE_CHILD_ID, childId).apply()
        }
    }

    fun getActiveChildId(): String? = prefs.getString(KEY_ACTIVE_CHILD_ID, null)

    fun clearTokens() {
        val installationId = getOrGenerateInstallationId()
        prefs.edit()
            .remove(KEY_ACCESS_TOKEN)
            .remove(KEY_ACCESS_TOKEN_EXPIRES_AT)
            .remove(KEY_REFRESH_TOKEN)
            .remove(KEY_ACTIVE_CHILD_ID)
            .putString(KEY_INSTALLATION_ID, installationId)
            .apply()
    }

    fun isLoggedIn(): Boolean {
        return !getAccessToken().isNullOrBlank() && !getRefreshToken().isNullOrBlank()
    }

    private fun String?.isNull_or_empty_custom(): Boolean {
        return this == null || this.trim().isEmpty()
    }

    companion object {
        private const val PREFS_NAME = "secure_token_prefs"

        private const val KEY_INSTALLATION_ID = "key_installation_id"
        private const val KEY_ACCESS_TOKEN = "key_access_token"
        private const val KEY_ACCESS_TOKEN_EXPIRES_AT = "key_access_token_expires_at"
        private const val KEY_REFRESH_TOKEN = "key_refresh_token"
        private const val KEY_ACTIVE_CHILD_ID = "key_active_child_id"
    }
}
