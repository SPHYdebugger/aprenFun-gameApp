package com.example.aprendemoslavida.avatar

import android.content.Context
import com.example.aprendemoslavida.model.AvatarProfile
import org.json.JSONObject

object AvatarProfileStore {
    private const val PREFS_NAME = "aprendemos_avatar_profile"
    private const val KEY_PROFILE_JSON = "profile_json"

    fun load(context: Context): AvatarProfile {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_PROFILE_JSON, null) ?: return AvatarProfile()
        return try {
            val json = JSONObject(raw)
            AvatarProfile(
                gender = json.optString("gender", "NINO"),
                hairStyle = json.optInt("hairStyle", 0),
                hairColor = json.optInt("hairColor", 0),
                eyeShape = json.optInt("eyeShape", 0),
                eyeColor = json.optInt("eyeColor", 0),
                mouthShape = json.optInt("mouthShape", 0),
                mouthColor = json.optInt("mouthColor", 0),
                skinTone = json.optInt("skinTone", 0),
                outfit = json.optInt("outfit", 0)
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
        }
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_PROFILE_JSON, json.toString()).apply()
    }
}
