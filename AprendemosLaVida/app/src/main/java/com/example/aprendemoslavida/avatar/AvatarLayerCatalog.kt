package com.example.aprendemoslavida.avatar

import com.example.aprendemoslavida.model.AvatarProfile

/**
 * Rutas (en assets/) de las capas del avatar para el editor y para el modo historia.
 *
 * - Editor: capas de 512x512 (frame 1 de frente).
 * - Modo historia: hojas 4x4 (filas UP, RIGHT, DOWN, LEFT; columnas = frames del paso).
 *
 * El cuerpo es común a niño y niña (solo cambia el tono de piel); la ropa y el pelo van por género.
 * Kotlin puro: sin dependencias de Android, para poder probarlo en tests de JVM.
 */
object AvatarLayerCatalog {
    const val EDITOR_ROOT = "avatar/"
    const val STORY_ROOT = "avatar/story/"

    const val GENDER_BOY = "NINO"
    const val GENDER_GIRL = "NINA"
    const val HAIR_STYLE_COUNT = 3
    const val HAIR_COLOR_COUNT = 3
    const val OUTFIT_COUNT = 6

    private val SKINS = arrayOf("01", "03", "05")
    private val BOY_OUTFITS = arrayOf("boy_01", "boy_02", "boy_03", "boy_04", "boy_05", "boy_06")
    private val GIRL_OUTFITS = arrayOf("girl_01", "girl_02", "girl_03", "girl_04", "girl_05b", "girl_06")
    private val BOY_HAIRS = arrayOf("boy_01", "boy_02", "boy_03")
    private val GIRL_HAIRS = arrayOf("girl_01", "girl_02", "girl_03")

    val SKIN_COUNT: Int get() = SKINS.size

    /** Capas de un personaje. Todas tienen el mismo tamaño y alineación, y se apilan en este orden. */
    data class Layers(
        val body: String,
        val outfit: String,
        val outfitMask: String,
        val hair: String
    )

    fun editorLayers(profile: AvatarProfile): Layers = layers(normalize(profile), EDITOR_ROOT, "base_body_skin_")

    fun storyLayers(profile: AvatarProfile): Layers = layers(normalize(profile), STORY_ROOT, "body_skin_")

    private fun layers(p: AvatarProfile, root: String, bodyPrefix: String): Layers {
        val outfit = (if (isGirl(p)) GIRL_OUTFITS else BOY_OUTFITS)[p.outfit]
        val hair = (if (isGirl(p)) GIRL_HAIRS else BOY_HAIRS)[p.hairStyle]
        return Layers(
            body = "$root$bodyPrefix${SKINS[p.skinTone]}.png",
            outfit = "${root}outfit_$outfit.png",
            outfitMask = "${root}outfit_${outfit}_mask.png",
            hair = "${root}hair_${hair}_c${p.hairColor}.png"
        )
    }

    /** Misma normalización que usaba AvatarBitmapBuilder: índices fuera de rango dan la vuelta. */
    fun normalize(input: AvatarProfile): AvatarProfile {
        val profile = input.copy()
        if (profile.gender != GENDER_BOY && profile.gender != GENDER_GIRL) {
            profile.gender = GENDER_BOY
        }
        profile.hairStyle = wrapIndex(profile.hairStyle, HAIR_STYLE_COUNT)
        profile.hairColor = wrapIndex(profile.hairColor, HAIR_COLOR_COUNT)
        profile.eyeShape = wrapIndex(profile.eyeShape, 3)
        profile.eyeColor = wrapIndex(profile.eyeColor, 4)
        profile.mouthShape = wrapIndex(profile.mouthShape, 3)
        profile.mouthColor = wrapIndex(profile.mouthColor, 3)
        profile.skinTone = wrapIndex(profile.skinTone, SKINS.size)
        profile.outfit = wrapIndex(profile.outfit, OUTFIT_COUNT)
        return profile
    }

    fun isGirl(profile: AvatarProfile): Boolean = profile.gender == GENDER_GIRL

    /** Todas las combinaciones válidas (2 géneros × 3 peinados × 3 colores × 3 pieles × 6 prendas). */
    fun allProfiles(): List<AvatarProfile> {
        val result = ArrayList<AvatarProfile>()
        for (gender in arrayOf(GENDER_BOY, GENDER_GIRL))
            for (style in 0 until HAIR_STYLE_COUNT)
                for (color in 0 until HAIR_COLOR_COUNT)
                    for (skin in SKINS.indices)
                        for (outfit in 0 until OUTFIT_COUNT)
                            result.add(
                                AvatarProfile(
                                    gender = gender,
                                    hairStyle = style,
                                    hairColor = color,
                                    skinTone = skin,
                                    outfit = outfit
                                )
                            )
        return result
    }

    private fun wrapIndex(value: Int, length: Int): Int {
        val m = value % length
        return if (m < 0) m + length else m
    }
}
