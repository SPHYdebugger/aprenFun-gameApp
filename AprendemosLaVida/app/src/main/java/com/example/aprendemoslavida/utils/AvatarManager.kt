package com.example.aprendemoslavida.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import com.example.aprendemoslavida.R
import org.json.JSONObject
import kotlin.math.roundToInt

object AvatarManager {
    data class AvatarStyle(
        val shirtColor: Int,
        val pantsColor: Int,
        val shoesColor: Int,
        val hairColor: Int,
        val skinColor: Int
    )

    enum class Part {
        SHIRT,
        PANTS,
        SHOES,
        HAIR,
        SKIN
    }

    data class AccessoryPlacement(
        val enabled: Boolean,
        val xNorm: Float,
        val yNorm: Float,
        val rotationDeg: Float
    )

    private const val PREFS_NAME = "aprendemos_prefs"
    private const val KEY_CLOTHES_COLOR = "avatar_clothes_color"
    private const val KEY_SHIRT_COLOR = "avatar_shirt_color"
    private const val KEY_PANTS_COLOR = "avatar_pants_color"
    private const val KEY_SHOES_COLOR = "avatar_shoes_color"
    private const val KEY_HAIR_COLOR = "avatar_hair_color"
    private const val KEY_SKIN_COLOR = "avatar_skin_color"
    private const val KEY_SELECTED_OUTFIT_ID = "avatar_selected_outfit_id"
    private const val KEY_SELECTED_ACCESSORY_ID = "avatar_selected_accessory_id"
    private const val KEY_ACCESSORY_PLACEMENTS = "avatar_accessory_placements"
    private const val ZONE_MATCH_TOLERANCE = 48
    private const val PART_NONE: Byte = 0
    private const val PART_HAIR: Byte = 1
    private const val PART_SKIN: Byte = 2
    private const val PART_SHIRT: Byte = 3
    private const val PART_PANTS: Byte = 4
    private const val PART_SHOES: Byte = 5

    private val ZONE_HAIR = Color.parseColor("#ED1C24")
    private val ZONE_SHIRT = Color.parseColor("#22B14C")
    private val ZONE_PANTS = Color.parseColor("#A349A4")
    private val ZONE_SHOES = Color.parseColor("#00A2E8")
    private val ZONE_SKIN = Color.parseColor("#7F7F7F")

    private data class ZoneMatrix(
        val width: Int,
        val height: Int,
        val parts: ByteArray
    ) {
        fun partCodeAt(x: Int, y: Int): Byte {
            if (x !in 0 until width || y !in 0 until height) return PART_NONE
            return parts[(y * width) + x]
        }
    }

    val shirtPalette: List<Int> = listOf(
        Color.parseColor("#E94F37"),
        Color.parseColor("#4D96FF"),
        Color.parseColor("#50C878"),
        Color.parseColor("#F59E0B"),
        Color.parseColor("#8B5CF6"),
        Color.parseColor("#FF5D8F")
    )

    val pantsPalette: List<Int> = listOf(
        Color.parseColor("#4C1D95"),
        Color.parseColor("#1E3A8A"),
        Color.parseColor("#166534"),
        Color.parseColor("#7C2D12"),
        Color.parseColor("#334155"),
        Color.parseColor("#BE185D")
    )

    val shoesPalette: List<Int> = listOf(
        Color.parseColor("#1F2937"),
        Color.parseColor("#7C3AED"),
        Color.parseColor("#EC4899"),
        Color.parseColor("#F59E0B"),
        Color.parseColor("#10B981"),
        Color.parseColor("#2563EB")
    )

    val hairPalette: List<Int> = listOf(
        Color.parseColor("#2D1B0E"),
        Color.parseColor("#4A2C1A"),
        Color.parseColor("#6D4C41"),
        Color.parseColor("#A97142"),
        Color.parseColor("#CFA06B"),
        Color.parseColor("#1C1C1C")
    )

    val skinPalette: List<Int> = listOf(
        Color.parseColor("#F8D5B8"),
        Color.parseColor("#F1C59E"),
        Color.parseColor("#E7B789"),
        Color.parseColor("#D59A6B"),
        Color.parseColor("#B97747"),
        Color.parseColor("#8A5A3B")
    )

    fun defaultStyle(): AvatarStyle {
        return AvatarStyle(
            shirtColor = shirtPalette.first(),
            pantsColor = pantsPalette.first(),
            shoesColor = shoesPalette.first(),
            hairColor = hairPalette[1],
            skinColor = skinPalette[1]
        )
    }

    fun getStyle(context: Context): AvatarStyle {
        val defaults = defaultStyle()
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val legacyClothes = prefs.getInt(KEY_CLOTHES_COLOR, defaults.shirtColor)
        return AvatarStyle(
            shirtColor = prefs.getInt(KEY_SHIRT_COLOR, legacyClothes),
            pantsColor = prefs.getInt(KEY_PANTS_COLOR, defaults.pantsColor),
            shoesColor = prefs.getInt(KEY_SHOES_COLOR, defaults.shoesColor),
            hairColor = prefs.getInt(KEY_HAIR_COLOR, defaults.hairColor),
            skinColor = prefs.getInt(KEY_SKIN_COLOR, defaults.skinColor)
        )
    }

    fun saveStyle(context: Context, style: AvatarStyle) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putInt(KEY_SHIRT_COLOR, style.shirtColor)
            .putInt(KEY_PANTS_COLOR, style.pantsColor)
            .putInt(KEY_SHOES_COLOR, style.shoesColor)
            .putInt(KEY_CLOTHES_COLOR, style.shirtColor)
            .putInt(KEY_HAIR_COLOR, style.hairColor)
            .putInt(KEY_SKIN_COLOR, style.skinColor)
            .apply()
    }

    fun getSelectedOutfitId(context: Context): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_SELECTED_OUTFIT_ID, null)
    }

    fun getSelectedAccessoryId(context: Context): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_SELECTED_ACCESSORY_ID, null)
    }

    fun saveSelectedOutfitId(context: Context, outfitId: String?) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_SELECTED_OUTFIT_ID, outfitId).apply()
    }

    fun saveSelectedAccessoryId(context: Context, accessoryId: String?) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_SELECTED_ACCESSORY_ID, accessoryId).apply()
    }

    fun getAccessoryPlacements(context: Context): Map<String, AccessoryPlacement> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_ACCESSORY_PLACEMENTS, null) ?: return emptyMap()
        return try {
            val root = JSONObject(raw)
            val map = LinkedHashMap<String, AccessoryPlacement>()
            val keys = root.keys()
            while (keys.hasNext()) {
                val id = keys.next()
                val obj = root.optJSONObject(id) ?: continue
                map[id] = AccessoryPlacement(
                    enabled = obj.optBoolean("enabled", false),
                    xNorm = obj.optDouble("x", 0.5).toFloat().coerceIn(0f, 1f),
                    yNorm = obj.optDouble("y", 0.5).toFloat().coerceIn(0f, 1f),
                    rotationDeg = obj.optDouble("rotation", 0.0).toFloat()
                )
            }
            map
        } catch (_: Exception) {
            emptyMap()
        }
    }

    fun saveAccessoryPlacements(context: Context, placements: Map<String, AccessoryPlacement>) {
        val root = JSONObject()
        placements.forEach { (id, placement) ->
            val obj = JSONObject()
            obj.put("enabled", placement.enabled)
            obj.put("x", placement.xNorm.toDouble())
            obj.put("y", placement.yNorm.toDouble())
            obj.put("rotation", placement.rotationDeg.toDouble())
            root.put(id, obj)
        }
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_ACCESSORY_PLACEMENTS, root.toString()).apply()
    }

    fun buildStoryAvatar(context: Context): Bitmap? {
        val base = BitmapFactory.decodeResource(context.resources, R.drawable.story_player_base) ?: return null
        val zones = BitmapFactory.decodeResource(context.resources, R.drawable.story_player_zones)
        return tintBitmap(base, getStyle(context), zones)
    }

    fun tintBitmap(source: Bitmap, style: AvatarStyle, zonesMask: Bitmap? = null): Bitmap {
        val mutable = source.copy(Bitmap.Config.ARGB_8888, true)
        val width = mutable.width
        val height = mutable.height
        val zoneMatrix = zonesMask?.let { buildFilledZoneMatrix(it, width, height) }

        for (y in 0 until height) {
            val yn = y.toFloat() / height.toFloat()
            for (x in 0 until width) {
                val color = mutable.getPixel(x, y)
                val alpha = Color.alpha(color)
                if (alpha == 0) continue

                val xn = x.toFloat() / width.toFloat()
                val zonePart = zoneMatrix?.partCodeAt(x, y)?.let(::partFromCode)
                val part = zonePart ?: detectPart(color, xn, yn, null) ?: continue
                val targetColor = when (part) {
                    Part.SHIRT -> style.shirtColor
                    Part.PANTS -> style.pantsColor
                    Part.SHOES -> style.shoesColor
                    Part.HAIR -> style.hairColor
                    Part.SKIN -> style.skinColor
                }

                mutable.setPixel(x, y, recolorKeepingShading(color, targetColor))
            }
        }

        return mutable
    }

    private fun buildFilledZoneMatrix(mask: Bitmap, sourceWidth: Int, sourceHeight: Int): ZoneMatrix {
        val total = sourceWidth * sourceHeight
        val boundary = ByteArray(total) { PART_NONE }

        for (y in 0 until sourceHeight) {
            for (x in 0 until sourceWidth) {
                val zoneColor = sampledZoneColor(mask, x, y, sourceWidth, sourceHeight)
                val part = partFromZoneColor(zoneColor)
                if (part != null) {
                    boundary[(y * sourceWidth) + x] = partCode(part)
                }
            }
        }

        val filled = ByteArray(total) { PART_NONE }
        val partCodes = listOf(PART_HAIR, PART_SKIN, PART_SHIRT, PART_PANTS, PART_SHOES)
        val priority = listOf(PART_HAIR, PART_SKIN, PART_SHIRT, PART_PANTS, PART_SHOES)

        for (code in partCodes) {
            val rowFill = BooleanArray(total)
            val colFill = BooleanArray(total)

            for (y in 0 until sourceHeight) {
                val xs = ArrayList<Int>()
                for (x in 0 until sourceWidth) {
                    if (boundary[(y * sourceWidth) + x] == code) xs.add(x)
                }
                fillLineSegmentsByPairs(xs, sourceWidth) { xFill ->
                    rowFill[(y * sourceWidth) + xFill] = true
                }
            }

            for (x in 0 until sourceWidth) {
                val ys = ArrayList<Int>()
                for (y in 0 until sourceHeight) {
                    if (boundary[(y * sourceWidth) + x] == code) ys.add(y)
                }
                fillLineSegmentsByPairs(ys, sourceHeight) { yFill ->
                    colFill[(yFill * sourceWidth) + x] = true
                }
            }

            for (i in 0 until total) {
                if (boundary[i] == code || rowFill[i] || colFill[i]) {
                    if (filled[i] == PART_NONE) {
                        filled[i] = code
                    }
                }
            }
        }

        // In rare overlaps, enforce stable priority.
        for (i in 0 until total) {
            if (boundary[i] != PART_NONE) {
                filled[i] = boundary[i]
                continue
            }
            val matches = ArrayList<Byte>(3)
            for (code in partCodes) {
                if (filled[i] == code) {
                    matches.add(code)
                }
            }
            if (matches.size > 1) {
                filled[i] = priority.firstOrNull { matches.contains(it) } ?: matches.first()
            }
        }

        return ZoneMatrix(sourceWidth, sourceHeight, filled)
    }

    private fun fillLineSegmentsByPairs(points: List<Int>, maxLength: Int, onFill: (Int) -> Unit) {
        if (points.size < 2) return
        val sorted = points.distinct().sorted()
        var i = 0
        while (i + 1 < sorted.size) {
            val a = sorted[i].coerceIn(0, maxLength - 1)
            val b = sorted[i + 1].coerceIn(0, maxLength - 1)
            val start = minOf(a, b)
            val end = maxOf(a, b)
            for (p in start..end) onFill(p)
            i += 2
        }
    }

    private fun sampledZoneColor(mask: Bitmap, x: Int, y: Int, sourceWidth: Int, sourceHeight: Int): Int {
        if (sourceWidth <= 1 || sourceHeight <= 1) {
            return mask.getPixel(0, 0)
        }
        val mx = ((x.toFloat() / (sourceWidth - 1).toFloat()) * (mask.width - 1).toFloat()).roundToInt().coerceIn(0, mask.width - 1)
        val my = ((y.toFloat() / (sourceHeight - 1).toFloat()) * (mask.height - 1).toFloat()).roundToInt().coerceIn(0, mask.height - 1)
        return mask.getPixel(mx, my)
    }

    private fun detectPart(color: Int, xn: Float, yn: Float, zoneColor: Int?): Part? {
        if (zoneColor != null) {
            return partFromZoneColor(zoneColor)
        }

        val r = Color.red(color)
        val g = Color.green(color)
        val b = Color.blue(color)

        val hsv = FloatArray(3)
        Color.RGBToHSV(r, g, b, hsv)
        val hue = hsv[0]
        val sat = hsv[1]
        val value = hsv[2]

        if (value < 0.08f) return null // preserve dark outlines

        val isFaceCoreZone = yn in 0.18f..0.56f && xn in 0.22f..0.78f
        val isEyeWhites = isFaceCoreZone && sat < 0.12f && value > 0.82f
        if (isEyeWhites) return null

        // Hair first: restrict to head perimeter so we don't tint face pixels as hair.
        val hairRegion =
            yn < 0.58f &&
                (
                    xn < 0.30f ||
                        xn > 0.70f ||
                        yn < 0.24f ||
                        (yn < 0.44f && (xn < 0.36f || xn > 0.64f))
                    )
        val hairLikeColor =
            ((hue in 8f..48f) && sat > 0.08f && value < 0.92f) ||
                (value < 0.35f && sat < 0.70f)
        if (hairRegion && hairLikeColor) return Part.HAIR

        // Skin constrained to face/arms/legs zones to avoid swallowing clothes and hair.
        val skinTone = hue in 12f..45f && sat in 0.18f..0.75f && value > 0.2f && r >= g
        val skinRegion =
            (yn in 0.18f..0.56f && xn in 0.22f..0.78f) ||
                (yn in 0.44f..0.80f && (xn < 0.34f || xn > 0.66f)) ||
                (yn in 0.82f..0.96f && xn in 0.32f..0.68f)
        if (skinTone && skinRegion) return Part.SKIN

        if (yn in 0.36f..0.70f && xn in 0.28f..0.72f) return Part.SHIRT
        if (yn in 0.69f..0.88f && xn in 0.27f..0.73f) return Part.PANTS
        if (yn >= 0.86f && xn in 0.24f..0.76f) return Part.SHOES

        return null
    }

    private fun partCode(part: Part): Byte {
        return when (part) {
            Part.HAIR -> PART_HAIR
            Part.SKIN -> PART_SKIN
            Part.SHIRT -> PART_SHIRT
            Part.PANTS -> PART_PANTS
            Part.SHOES -> PART_SHOES
        }
    }

    private fun partFromCode(code: Byte): Part? {
        return when (code) {
            PART_HAIR -> Part.HAIR
            PART_SKIN -> Part.SKIN
            PART_SHIRT -> Part.SHIRT
            PART_PANTS -> Part.PANTS
            PART_SHOES -> Part.SHOES
            else -> null
        }
    }

    private fun partFromZoneColor(zoneColor: Int): Part? {
        if (Color.alpha(zoneColor) < 10) return null

        val candidates = listOf(
            ZONE_HAIR to Part.HAIR,
            ZONE_SHIRT to Part.SHIRT,
            ZONE_PANTS to Part.PANTS,
            ZONE_SHOES to Part.SHOES,
            ZONE_SKIN to Part.SKIN
        )
        val closest = candidates.minByOrNull { colorDistanceSquared(it.first, zoneColor) } ?: return null
        val distance = colorDistanceSquared(closest.first, zoneColor)
        return if (distance <= (ZONE_MATCH_TOLERANCE * ZONE_MATCH_TOLERANCE)) closest.second else null
    }

    private fun colorDistanceSquared(a: Int, b: Int): Int {
        val dr = Color.red(a) - Color.red(b)
        val dg = Color.green(a) - Color.green(b)
        val db = Color.blue(a) - Color.blue(b)
        return (dr * dr) + (dg * dg) + (db * db)
    }

    private fun recolorKeepingShading(sourceColor: Int, targetColor: Int): Int {
        val alpha = Color.alpha(sourceColor)

        val sourceHsv = FloatArray(3)
        Color.RGBToHSV(Color.red(sourceColor), Color.green(sourceColor), Color.blue(sourceColor), sourceHsv)
        val targetHsv = FloatArray(3)
        Color.RGBToHSV(Color.red(targetColor), Color.green(targetColor), Color.blue(targetColor), targetHsv)

        val shadedValue = (sourceHsv[2] * 1.08f).coerceIn(0f, 1f)
        val shadedSat = (targetHsv[1] * (0.6f + (sourceHsv[1] * 0.4f))).coerceIn(0f, 1f)
        val finalHsv = floatArrayOf(targetHsv[0], shadedSat, shadedValue)
        val tinted = Color.HSVToColor(finalHsv)

        return Color.argb(alpha, Color.red(tinted), Color.green(tinted), Color.blue(tinted))
    }
}
