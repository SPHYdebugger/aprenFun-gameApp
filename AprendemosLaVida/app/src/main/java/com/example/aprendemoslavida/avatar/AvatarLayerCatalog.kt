package com.example.aprendemoslavida.avatar

import com.example.aprendemoslavida.data.model.LegacyImportAvatar
import com.example.aprendemoslavida.model.AvatarProfile

/**
 * Rutas (en assets/) de las capas del avatar para el editor y para el modo historia, y reglas del perfil.
 *
 * - Editor: capas de 512x512 (frame 1 de frente).
 * - Modo historia: hojas 4x4 (filas UP, RIGHT, DOWN, LEFT; columnas = frames del paso).
 * - Orden de dibujo: cuerpo (recortado por la máscara de la ropa) -> calzado -> ropa -> pelo.
 *
 * El cuerpo es común a niño y niña (solo cambia el tono de piel); la ropa, el pelo y el calzado van por género,
 * salvo la ropa básica y el calzado básico, que son comunes.
 * Kotlin puro: sin dependencias de Android, para poder probarlo en tests de JVM.
 */
object AvatarLayerCatalog {
    const val EDITOR_ROOT = "avatar/"
    const val STORY_ROOT = "avatar/story/"
    const val THUMBS_ROOT = "avatar/thumbs/"

    const val GENDER_BOY = "NINO"
    const val GENDER_GIRL = "NINA"
    const val DEFAULT_GENDER = GENDER_GIRL

    /** Valor de outfit para la ropa básica común (polo blanco y pantalón largo azul). */
    const val OUTFIT_BASIC = -1

    const val HAIR_STYLE_COUNT = 7

    /** Colores: 0 negro, 1 castaño, 2 rubio, 3 rojizo, 4 amarillo, 5 gris. */
    const val HAIR_COLOR_COUNT = 6

    private val SKINS = arrayOf("01", "03", "05")
    private const val BASIC_OUTFIT = "basic_01"
    private val BOY_OUTFITS = arrayOf("boy_01", "boy_02", "boy_03", "boy_04", "boy_05", "boy_06", "boy_07")
    private val GIRL_OUTFITS = arrayOf("girl_01", "girl_02", "girl_03", "girl_04", "girl_05b", "girl_06", "girl_07")
    private val BOY_HAIRS = arrayOf("boy_01", "boy_02", "boy_03", "boy_04", "boy_05", "boy_06", "boy_07")
    private val GIRL_HAIRS = arrayOf("girl_01", "girl_02", "girl_03", "girl_04", "girl_05", "girl_06", "girl_07")
    private val BOY_SHOES = arrayOf("basic", "boy_01", "boy_02", "boy_03", "boy_04", "boy_05")
    private val GIRL_SHOES = arrayOf("basic", "girl_01", "girl_02", "girl_03", "girl_04", "girl_05")

    /** Puntos globales para desbloquear cada prenda (índices 0..6; la ropa básica es gratis). */
    val OUTFIT_UNLOCK_POINTS = intArrayOf(10000, 25000, 50000, 80000, 100000, 150000, 200000)

    /** Puntos globales para desbloquear cada calzado (índices 0..5; el 0 es el básico). */
    val SHOES_UNLOCK_POINTS = intArrayOf(0, 5000, 15000, 30000, 60000, 120000)

    val SKIN_COUNT: Int get() = SKINS.size
    val OUTFIT_COUNT: Int get() = BOY_OUTFITS.size
    val SHOES_COUNT: Int get() = BOY_SHOES.size

    /** Capas de un personaje. Todas tienen el mismo tamaño y alineación. */
    data class Layers(
        val body: String,
        val shoes: String,
        val outfit: String,
        val outfitMask: String,
        val hair: String
    )

    fun editorLayers(profile: AvatarProfile): Layers = layers(normalize(profile), EDITOR_ROOT, "base_body_skin_")

    fun storyLayers(profile: AvatarProfile): Layers = layers(normalize(profile), STORY_ROOT, "body_skin_")

    /** Miniatura (128 px) del calzado [index] del género del perfil, para los botones del editor. */
    fun shoesThumb(profile: AvatarProfile, index: Int): String {
        val p = normalize(profile)
        return "${THUMBS_ROOT}thumb_shoes_${(if (isGirl(p)) GIRL_SHOES else BOY_SHOES)[index]}.png"
    }

    /** Capa de 512 de la prenda [outfit] (OUTFIT_BASIC o 0..6) del género del perfil, para las miniaturas. */
    fun outfitEditorLayer(profile: AvatarProfile, outfit: Int): String =
        "${EDITOR_ROOT}outfit_${outfitId(normalize(profile.copy(outfit = outfit)))}.png"

    private fun layers(p: AvatarProfile, root: String, bodyPrefix: String): Layers {
        val outfit = outfitId(p)
        val shoes = (if (isGirl(p)) GIRL_SHOES else BOY_SHOES)[p.shoes]
        val hair = (if (isGirl(p)) GIRL_HAIRS else BOY_HAIRS)[p.hairStyle]
        return Layers(
            body = "$root$bodyPrefix${SKINS[p.skinTone]}.png",
            shoes = "${root}shoes_$shoes.png",
            outfit = "${root}outfit_$outfit.png",
            outfitMask = "${root}outfit_${outfit}_mask.png",
            hair = "${root}hair_${hair}_c${p.hairColor}.png"
        )
    }

    private fun outfitId(p: AvatarProfile): String = when {
        p.outfit == OUTFIT_BASIC -> BASIC_OUTFIT
        isGirl(p) -> GIRL_OUTFITS[p.outfit]
        else -> BOY_OUTFITS[p.outfit]
    }

    /**
     * Normaliza el perfil: un género desconocido pasa a niña, la ropa básica (-1 o cualquier negativo) se conserva
     * y el resto de índices fuera de rango dan la vuelta.
     */
    fun normalize(input: AvatarProfile): AvatarProfile {
        val profile = input.copy()
        if (profile.gender != GENDER_BOY && profile.gender != GENDER_GIRL) {
            profile.gender = DEFAULT_GENDER
        }
        profile.hairStyle = wrapIndex(profile.hairStyle, HAIR_STYLE_COUNT)
        profile.hairColor = wrapIndex(profile.hairColor, HAIR_COLOR_COUNT)
        profile.eyeShape = wrapIndex(profile.eyeShape, 3)
        profile.eyeColor = wrapIndex(profile.eyeColor, 4)
        profile.mouthShape = wrapIndex(profile.mouthShape, 3)
        profile.mouthColor = wrapIndex(profile.mouthColor, 3)
        profile.skinTone = wrapIndex(profile.skinTone, SKINS.size)
        profile.outfit = if (profile.outfit < 0) OUTFIT_BASIC else wrapIndex(profile.outfit, OUTFIT_COUNT)
        profile.shoes = wrapIndex(profile.shoes, SHOES_COUNT)
        profile.name = cleanName(profile.name)
        return profile
    }

    fun isGirl(profile: AvatarProfile): Boolean = profile.gender == GENDER_GIRL

    /** Longitud máxima del nombre del avatar. */
    const val NAME_MAX_LENGTH = 16

    /** Nombre limpio: sin saltos de línea ni espacios repetidos ni en los extremos, y como mucho [NAME_MAX_LENGTH] caracteres. */
    fun cleanName(raw: String): String =
        raw.replace(Regex("\\s+"), " ").trim().take(NAME_MAX_LENGTH).trim()

    fun isOutfitUnlocked(outfit: Int, globalPoints: Int): Boolean =
        outfit == OUTFIT_BASIC || globalPoints >= OUTFIT_UNLOCK_POINTS.getOrElse(outfit) { Int.MAX_VALUE }

    fun isShoesUnlocked(shoes: Int, globalPoints: Int): Boolean =
        globalPoints >= SHOES_UNLOCK_POINTS.getOrElse(shoes) { Int.MAX_VALUE }

    /**
     * Puntos acumulados para los desbloqueos del avatar: el saldo del servidor (suma partidas de preguntas,
     * modo historia y bonificaciones de racha) o, si es mayor, el contador local antiguo, que solo conserva puntos
     * de antes de jugar con el servidor y aún no importados (no se suman para no contarlos dos veces).
     */
    fun accumulatedPoints(serverBalance: Int, legacyLocalPoints: Int): Int =
        maxOf(serverBalance.coerceAtLeast(0), legacyLocalPoints.coerceAtLeast(0))

    /** El perfil con la ropa y el calzado no desbloqueados con [globalPoints] devueltos a los básicos (al salir del modo demo). */
    fun withinUnlocks(profile: AvatarProfile, globalPoints: Int): AvatarProfile {
        val p = normalize(profile)
        if (!isOutfitUnlocked(p.outfit, globalPoints)) p.outfit = OUTFIT_BASIC
        if (!isShoesUnlocked(p.shoes, globalPoints)) p.shoes = 0
        return p
    }

    /**
     * Avatar para la importación de datos antiguos al servidor. El contrato no conoce la ropa básica, la de gala,
     * el calzado ni los peinados y colores nuevos (son solo del móvil): se limitan a los valores originales.
     */
    fun toLegacyImport(profile: AvatarProfile): LegacyImportAvatar {
        val p = normalize(profile)
        return LegacyImportAvatar(
            gender = p.gender,
            hairStyle = p.hairStyle.coerceIn(0, LEGACY_MAX_HAIR_INDEX),
            hairColor = p.hairColor.coerceIn(0, LEGACY_MAX_HAIR_INDEX),
            eyeShape = p.eyeShape,
            eyeColor = p.eyeColor,
            mouthShape = p.mouthShape,
            mouthColor = p.mouthColor,
            skinTone = p.skinTone,
            outfit = p.outfit.coerceIn(0, LEGACY_MAX_OUTFIT)
        )
    }

    private const val LEGACY_MAX_OUTFIT = 5
    /** El servidor solo conoce los 3 peinados y 3 colores originales (índices 0..2). */
    private const val LEGACY_MAX_HAIR_INDEX = 2

    /** Todas las combinaciones válidas: 2 géneros × 7 peinados × 6 colores × 3 pieles × 8 prendas × 6 calzados. */
    fun allProfiles(): List<AvatarProfile> {
        val result = ArrayList<AvatarProfile>()
        for (gender in arrayOf(GENDER_BOY, GENDER_GIRL))
            for (style in 0 until HAIR_STYLE_COUNT)
                for (color in 0 until HAIR_COLOR_COUNT)
                    for (skin in SKINS.indices)
                        for (outfit in OUTFIT_BASIC until OUTFIT_COUNT)
                            for (shoes in 0 until SHOES_COUNT)
                                result.add(
                                    AvatarProfile(
                                        gender = gender,
                                        hairStyle = style,
                                        hairColor = color,
                                        skinTone = skin,
                                        outfit = outfit,
                                        shoes = shoes
                                    )
                                )
        return result
    }

    private fun wrapIndex(value: Int, length: Int): Int {
        val m = value % length
        return if (m < 0) m + length else m
    }
}
