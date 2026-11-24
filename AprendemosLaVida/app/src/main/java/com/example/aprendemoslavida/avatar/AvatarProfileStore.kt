package com.example.aprendemoslavida.avatar

import android.content.Context
import android.content.SharedPreferences
import com.example.aprendemoslavida.data.network.ApiClient
import com.example.aprendemoslavida.model.AvatarProfile
import org.json.JSONObject

object AvatarProfileStore {
    private const val PREFS_NAME = "aprendemos_avatar_profile"
    private const val KEY_PROFILE_JSON = "profile_json"

    fun load(context: Context): AvatarProfile {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val key = keyForChild(activeChildId(context))
        val raw = prefs.getString(key, null) ?: migrateDeviceProfile(prefs, key) ?: return AvatarProfile()
        val defaults = AvatarProfile()
        return try {
            val json = JSONObject(raw)
            AvatarProfile(
                gender = json.optString("gender", defaults.gender),
                hairStyle = json.optInt("hairStyle", 0),
                hairColor = json.optInt("hairColor", 0),
                eyeShape = json.optInt("eyeShape", 0),
                eyeColor = json.optInt("eyeColor", 0),
                mouthShape = json.optInt("mouthShape", 0),
                mouthColor = json.optInt("mouthColor", 0),
                skinTone = json.optInt("skinTone", 0),
                outfit = json.optInt("outfit", defaults.outfit),
                shoes = json.optInt("shoes", defaults.shoes),
                name = json.optString("name", defaults.name)
            )
        } catch (_: Exception) {
            AvatarProfile()
        }
    }

    fun save(context: Context, profile: AvatarProfile) {
        val json = JSONObject().apply {
            put("gender", profile.gender)
            put("hairStyle", profile.hairStyle)
            put("hairColor", profile.hairColor)
            put("eyeShape", profile.eyeShape)
            put("eyeColor", profile.eyeColor)
            put("mouthShape", profile.mouthShape)
            put("mouthColor", profile.mouthColor)
            put("skinTone", profile.skinTone)
            put("outfit", profile.outfit)
            put("shoes", profile.shoes)
            put("name", AvatarLayerCatalog.cleanName(profile.name))
        }
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(keyForChild(activeChildId(context)), json.toString()).apply()
        try {
            StoryPlayerSpriteSheetStore.ensureGenerated(context, profile)
        } catch (_: Exception) {
            // If dynamic generation fails we keep gameplay working with the bundled fallback sprites.
        }
    }

    /** Clave del avatar de cada niño: el avatar va con el perfil infantil activo, no con el móvil. */
    fun keyForChild(childId: String?): String =
        if (childId.isNullOrBlank()) KEY_PROFILE_JSON else "${KEY_PROFILE_JSON}_$childId"

    private fun activeChildId(context: Context): String? =
        try {
            ApiClient.getInstance(context).tokenStorage.getActiveChildId()
        } catch (_: Exception) {
            null
        }

    /** El avatar que había antes (uno por móvil) pasa, una sola vez, al primer niño que entra. */
    private fun migrateDeviceProfile(prefs: SharedPreferences, key: String): String? {
        if (key == KEY_PROFILE_JSON) return null
        val legacy = prefs.getString(KEY_PROFILE_JSON, null) ?: return null
        prefs.edit().putString(key, legacy).remove(KEY_PROFILE_JSON).apply()
        return legacy
    }
}
