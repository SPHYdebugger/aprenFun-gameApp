package com.example.aprendemoslavida.avatar

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Rect
import com.example.aprendemoslavida.model.AvatarProfile

object AvatarBitmapBuilder {
    private const val AVATAR_SIZE = 512
    private const val AVATAR_ROOT = "avatar/"

    private const val HAIR_COLOR_COUNT = 3

    private val bitmapCache = HashMap<String, Bitmap>()

    private val BODY_PATHS = arrayOf(
        "${AVATAR_ROOT}base_body_skin_01.png",
        "${AVATAR_ROOT}base_body_skin_02.png",
        "${AVATAR_ROOT}base_body_skin_03.png",
        "${AVATAR_ROOT}base_body_skin_04.png",
        "${AVATAR_ROOT}base_body_skin_05.png"
    )

    private val HAIR_BOY_PATHS = arrayOf(
        arrayOf("${AVATAR_ROOT}hair_boy_01_c0.png", "${AVATAR_ROOT}hair_boy_01_c1.png", "${AVATAR_ROOT}hair_boy_01_c2.png"),
        arrayOf("${AVATAR_ROOT}hair_boy_02_c0.png", "${AVATAR_ROOT}hair_boy_02_c1.png", "${AVATAR_ROOT}hair_boy_02_c2.png"),
        arrayOf("${AVATAR_ROOT}hair_boy_03_c0.png", "${AVATAR_ROOT}hair_boy_03_c1.png", "${AVATAR_ROOT}hair_boy_03_c2.png")
    )

    private val HAIR_GIRL_PATHS = arrayOf(
        arrayOf("${AVATAR_ROOT}hair_girl_01_c0.png", "${AVATAR_ROOT}hair_girl_01_c1.png", "${AVATAR_ROOT}hair_girl_01_c2.png"),
        arrayOf("${AVATAR_ROOT}hair_girl_02_c0.png", "${AVATAR_ROOT}hair_girl_02_c1.png", "${AVATAR_ROOT}hair_girl_02_c2.png"),
        arrayOf("${AVATAR_ROOT}hair_girl_03_c0.png", "${AVATAR_ROOT}hair_girl_03_c1.png", "${AVATAR_ROOT}hair_girl_03_c2.png")
    )

    private val OUTFIT_BOY_PATHS = arrayOf(
        "${AVATAR_ROOT}outfit_boy_01.png",
        "${AVATAR_ROOT}outfit_boy_02.png",
        "${AVATAR_ROOT}outfit_boy_03.png",
        "${AVATAR_ROOT}outfit_boy_04.png",
        "${AVATAR_ROOT}outfit_boy_05.png",
        "${AVATAR_ROOT}outfit_boy_06.png"
    )

    private val OUTFIT_GIRL_PATHS = arrayOf(
        "${AVATAR_ROOT}outfit_girl_01.png",
        "${AVATAR_ROOT}outfit_girl_02.png",
        "${AVATAR_ROOT}outfit_girl_03.png",
        "${AVATAR_ROOT}outfit_girl_04.png",
        "${AVATAR_ROOT}outfit_girl_05b.png",
        "${AVATAR_ROOT}outfit_girl_06.png"
    )

    fun buildAvatarBitmap(profileInput: AvatarProfile, context: Context): Bitmap {
        val profile = normalize(profileInput)
        val output = Bitmap.createBitmap(AVATAR_SIZE, AVATAR_SIZE, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        canvas.drawColor(Color.TRANSPARENT)

        val bodyPath = BODY_PATHS[profile.skinTone]
        val outfitPath = if (isGirl(profile)) OUTFIT_GIRL_PATHS[profile.outfit] else OUTFIT_BOY_PATHS[profile.outfit]
        val hairPath = if (isGirl(profile)) {
            HAIR_GIRL_PATHS[profile.hairStyle][profile.hairColor]
        } else {
            HAIR_BOY_PATHS[profile.hairStyle][profile.hairColor]
        }

        drawLayer(canvas, loadBitmap(context, bodyPath))
        drawLayer(canvas, loadBitmap(context, outfitPath))
        drawLayer(canvas, loadBitmap(context, hairPath))

        return output
    }

    private fun drawLayer(canvas: Canvas, layer: Bitmap) {
        val dst = Rect(0, 0, AVATAR_SIZE, AVATAR_SIZE)
        canvas.drawBitmap(layer, null, dst, null)
    }

    private fun loadBitmap(context: Context, assetPath: String): Bitmap {
        return bitmapCache.getOrPut(assetPath) {
            context.assets.open(assetPath).use { stream ->
                BitmapFactory.decodeStream(stream)
                    ?: throw IllegalStateException("No se pudo decodificar $assetPath")
            }
        }
    }

    private fun normalize(input: AvatarProfile): AvatarProfile {
        val profile = input.copy()
        if (profile.gender != "NINO" && profile.gender != "NINA") {
            profile.gender = "NINO"
        }
        profile.hairStyle = wrapIndex(profile.hairStyle, 3)
        profile.hairColor = wrapIndex(profile.hairColor, HAIR_COLOR_COUNT)
        profile.eyeShape = wrapIndex(profile.eyeShape, 3)
        profile.eyeColor = wrapIndex(profile.eyeColor, 4)
        profile.mouthShape = wrapIndex(profile.mouthShape, 3)
        profile.mouthColor = wrapIndex(profile.mouthColor, 3)
        profile.skinTone = wrapIndex(profile.skinTone, BODY_PATHS.size)
        profile.outfit = wrapIndex(profile.outfit, OUTFIT_BOY_PATHS.size)
        return profile
    }

    private fun isGirl(profile: AvatarProfile): Boolean = profile.gender == "NINA"

    private fun wrapIndex(value: Int, length: Int): Int {
        val m = value % length
        return if (m < 0) m + length else m
    }
}
