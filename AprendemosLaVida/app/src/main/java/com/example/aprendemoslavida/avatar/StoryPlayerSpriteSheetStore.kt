package com.example.aprendemoslavida.avatar

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.example.aprendemoslavida.model.AvatarProfile
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest

/**
 * Monta y cachea la hoja de sprites 4x4 del jugador en el modo historia a partir de las capas del avatar
 * (assets/avatar/story): cuerpo según el tono de piel, ropa recortando el cuerpo con su máscara, y pelo.
 * Filas: UP, RIGHT, DOWN, LEFT. Columnas: frames del ciclo de paso (0 = reposo).
 */
object StoryPlayerSpriteSheetStore {
    data class StorySpriteBuildConfig(
        val heldItemId: String? = null
    )

    private const val CACHE_PREFS = "aprendemos_story_player_sheet"
    private const val KEY_SIGNATURE = "avatar_signature"
    private const val KEY_VERSION = "sheet_version"

    // 3: hoja montada por capas con animación de paso real. 4: ropa básica y calzado como capa propia.
    private const val CACHE_VERSION = 4
    private const val CACHE_DIR = "story_player_cache"
    private const val CACHE_FILE = "player_sheet_dynamic.png"
    const val FRAME_COUNT = 4
    const val ROW_COUNT = 4

    @Volatile
    private var inMemorySheet: Bitmap? = null

    @Synchronized
    fun ensureGenerated(
        context: Context,
        profile: AvatarProfile,
        config: StorySpriteBuildConfig = StorySpriteBuildConfig()
    ) {
        val prefs = context.getSharedPreferences(CACHE_PREFS, Context.MODE_PRIVATE)
        val signature = signatureFor(profile, config)
        val versionMatches = prefs.getInt(KEY_VERSION, 0) == CACHE_VERSION
        val signatureMatches = prefs.getString(KEY_SIGNATURE, null) == signature
        val cacheFile = cacheFile(context)

        if (versionMatches && signatureMatches && cacheFile.exists()) {
            if (inMemorySheet == null) {
                inMemorySheet = BitmapFactory.decodeFile(cacheFile.absolutePath)
            }
            return
        }

        val sheet = AvatarLayerCompositor.compose(
            context,
            AvatarLayerCatalog.storyLayers(profile),
            rows = ROW_COUNT
        )

        cacheFile.parentFile?.mkdirs()
        FileOutputStream(cacheFile).use { output ->
            sheet.compress(Bitmap.CompressFormat.PNG, 100, output)
            output.flush()
        }

        prefs.edit()
            .putInt(KEY_VERSION, CACHE_VERSION)
            .putString(KEY_SIGNATURE, signature)
            .apply()

        inMemorySheet = sheet
    }

    @Synchronized
    fun load(context: Context): Bitmap? {
        inMemorySheet?.let { return it }

        val prefs = context.getSharedPreferences(CACHE_PREFS, Context.MODE_PRIVATE)
        if (prefs.getInt(KEY_VERSION, 0) != CACHE_VERSION) return null

        val file = cacheFile(context)
        if (!file.exists()) return null

        val decoded = BitmapFactory.decodeFile(file.absolutePath) ?: return null
        inMemorySheet = decoded
        return decoded
    }

    private fun cacheFile(context: Context): File {
        return File(File(context.filesDir, CACHE_DIR), CACHE_FILE)
    }

    private fun signatureFor(profile: AvatarProfile, config: StorySpriteBuildConfig): String {
        val source = buildString {
            append(CACHE_VERSION)
            append('|').append(profile.gender)
            append('|').append(profile.hairStyle)
            append('|').append(profile.hairColor)
            append('|').append(profile.eyeShape)
            append('|').append(profile.eyeColor)
            append('|').append(profile.mouthShape)
            append('|').append(profile.mouthColor)
            append('|').append(profile.skinTone)
            append('|').append(profile.outfit)
            append('|').append(profile.shoes)
            append('|').append(config.heldItemId ?: "none")
        }
        val digest = MessageDigest.getInstance("SHA-256").digest(source.toByteArray(Charsets.UTF_8))
        return digest.joinToString(separator = "") { "%02x".format(it) }
    }
}
