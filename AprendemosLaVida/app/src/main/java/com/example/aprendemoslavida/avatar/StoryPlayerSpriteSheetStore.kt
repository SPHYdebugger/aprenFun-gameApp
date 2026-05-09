package com.example.aprendemoslavida.avatar

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import com.example.aprendemoslavida.R
import com.example.aprendemoslavida.model.AvatarProfile
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Builds and caches a 4x4 directional sprite sheet for story mode.
 * Rows are ordered as: up, right, down, left. Columns are animation frames.
 */
object StoryPlayerSpriteSheetStore {
    data class StorySpriteBuildConfig(
        val heldItemId: String? = null
    )

    private const val CACHE_PREFS = "aprendemos_story_player_sheet"
    private const val KEY_SIGNATURE = "avatar_signature"
    private const val KEY_VERSION = "sheet_version"
    private const val CACHE_VERSION = 2
    private const val CACHE_DIR = "story_player_cache"
    private const val CACHE_FILE = "player_sheet_dynamic.png"
    private const val FRAME_COUNT = 4
    private const val ROW_COUNT = 4

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

        val avatar = AvatarBitmapBuilder.buildAvatarBitmap(profile, context)
        val reference = loadReferenceFrame(context) ?: avatar
        val tileSize = max(reference.width, reference.height).coerceAtLeast(64)
        val normalizedBase = normalizeAvatarToGameplayFrame(avatar, reference, tileSize)
        val sheet = buildDirectionalSheet(context, normalizedBase, tileSize)

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

    private fun loadReferenceFrame(context: Context): Bitmap? {
        val downFrame = BitmapFactory.decodeResource(context.resources, R.drawable.player_frames_down_1)
        if (downFrame != null) return downFrame
        return BitmapFactory.decodeResource(context.resources, R.drawable.story_player_base)
    }

    private fun normalizeAvatarToGameplayFrame(avatar: Bitmap, reference: Bitmap, tileSize: Int): Bitmap {
        val result = Bitmap.createBitmap(tileSize, tileSize, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        val avatarBounds = opaqueBounds(avatar)
            ?: Rect(0, 0, avatar.width, avatar.height)
        val referenceBounds = opaqueBounds(reference)
            ?: Rect(0, 0, reference.width, reference.height)

        val referenceScaleX = tileSize / reference.width.toFloat()
        val referenceScaleY = tileSize / reference.height.toFloat()
        val targetBounds = RectF(
            referenceBounds.left * referenceScaleX,
            referenceBounds.top * referenceScaleY,
            referenceBounds.right * referenceScaleX,
            referenceBounds.bottom * referenceScaleY
        )

        val srcWidth = avatarBounds.width().toFloat().coerceAtLeast(1f)
        val srcHeight = avatarBounds.height().toFloat().coerceAtLeast(1f)
        val targetWidth = targetBounds.width().coerceAtLeast(1f) * 0.98f
        val targetHeight = targetBounds.height().coerceAtLeast(1f) * 0.98f
        val fitScale = minOf(targetWidth / srcWidth, targetHeight / srcHeight)

        val drawWidth = srcWidth * fitScale
        val drawHeight = srcHeight * fitScale
        val drawLeft = targetBounds.centerX() - (drawWidth / 2f)
        val drawTop = targetBounds.bottom - drawHeight
        val destination = RectF(drawLeft, drawTop, drawLeft + drawWidth, drawTop + drawHeight)

        canvas.drawBitmap(avatar, avatarBounds, destination, paint)
        return result
    }

    private fun buildDirectionalSheet(context: Context, baseTile: Bitmap, tileSize: Int): Bitmap {
        val output = Bitmap.createBitmap(tileSize * FRAME_COUNT, tileSize * ROW_COUNT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val templateFrames = loadDirectionalTemplateFrames(context)

        if (templateFrames != null) {
            for (row in 0 until ROW_COUNT) {
                for (frame in 0 until FRAME_COUNT) {
                    val composed = composeFrameFromTemplate(
                        avatarTile = baseTile,
                        templateFrame = templateFrames[row][frame],
                        tileSize = tileSize,
                        row = row
                    )
                    drawTile(canvas, composed, tileSize, frame, row, 0f, 0f, paint)
                }
            }
            return output
        }

        val mirroredBase = mirror(baseTile)
        val frameOffsetsY = intArrayOf(0, -2, 0, 2)
        val sideOffsetsX = intArrayOf(0, 1, 0, -1)
        for (frame in 0 until FRAME_COUNT) {
            val yOffset = frameOffsetsY[frame]
            val sideOffsetX = sideOffsetsX[frame]
            drawTile(canvas, baseTile, tileSize, frame, 2, 0f, yOffset.toFloat(), paint) // down
            drawTile(canvas, baseTile, tileSize, frame, 0, 0f, yOffset.toFloat(), paint) // up
            drawTile(canvas, baseTile, tileSize, frame, 1, sideOffsetX.toFloat(), yOffset.toFloat(), paint) // right
            drawTile(canvas, mirroredBase, tileSize, frame, 3, (-sideOffsetX).toFloat(), yOffset.toFloat(), paint) // left
        }
        return output
    }

    private fun loadDirectionalTemplateFrames(context: Context): Array<Array<Bitmap>>? {
        // Keep this order aligned with gameplay rows: 0=up, 1=right, 2=down, 3=left.
        // Imported assets have right/left resource names swapped, matching StoryGameView correction.
        val rowPrefixes = arrayOf(
            "player_frames_up",
            "player_frames_left",
            "player_frames_down",
            "player_frames_right"
        )
        val rows = Array(ROW_COUNT) { row ->
            Array(FRAME_COUNT) { frame ->
                val name = "${rowPrefixes[row]}_${frame + 1}"
                val id = context.resources.getIdentifier(name, "drawable", context.packageName)
                if (id == 0) return null
                BitmapFactory.decodeResource(context.resources, id) ?: return null
            }
        }
        return rows
    }

    private fun composeFrameFromTemplate(
        avatarTile: Bitmap,
        templateFrame: Bitmap,
        tileSize: Int,
        row: Int
    ): Bitmap {
        val result = Bitmap.createBitmap(tileSize, tileSize, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val templateScaled = if (templateFrame.width == tileSize && templateFrame.height == tileSize) {
            templateFrame
        } else {
            Bitmap.createScaledBitmap(templateFrame, tileSize, tileSize, true)
        }

        val templateBounds = opaqueBounds(templateScaled)
            ?: Rect(0, 0, tileSize, tileSize)
        val avatarBounds = opaqueBounds(avatarTile)
            ?: Rect(0, 0, avatarTile.width, avatarTile.height)

        val srcWidth = avatarBounds.width().toFloat().coerceAtLeast(1f)
        val srcHeight = avatarBounds.height().toFloat().coerceAtLeast(1f)
        val dstWidth = templateBounds.width().toFloat().coerceAtLeast(1f)
        val dstHeight = templateBounds.height().toFloat().coerceAtLeast(1f)
        val fitScale = minOf(dstWidth / srcWidth, dstHeight / srcHeight) * 0.98f

        val drawWidth = srcWidth * fitScale
        val drawHeight = srcHeight * fitScale
        val drawLeft = templateBounds.centerX() - (drawWidth / 2f)
        val drawTop = templateBounds.bottom - drawHeight
        val avatarDst = RectF(drawLeft, drawTop, drawLeft + drawWidth, drawTop + drawHeight)

        canvas.drawBitmap(avatarTile, avatarBounds, avatarDst, paint)

        // Restrict avatar drawing to the animated directional silhouette.
        val maskPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
        }
        canvas.drawBitmap(templateScaled, null, Rect(0, 0, tileSize, tileSize), maskPaint)
        maskPaint.xfermode = null

        // Slightly darken back view to read as "espalda" while keeping avatar colors.
        if (row == 0) {
            val shadePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = 0x22000000
            }
            canvas.drawRect(0f, 0f, tileSize.toFloat(), tileSize.toFloat(), shadePaint)
            val remaskPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
            }
            canvas.drawBitmap(templateScaled, null, Rect(0, 0, tileSize, tileSize), remaskPaint)
            remaskPaint.xfermode = null
        }

        return result
    }

    private fun drawTile(
        canvas: Canvas,
        bitmap: Bitmap,
        tileSize: Int,
        col: Int,
        row: Int,
        offsetX: Float,
        offsetY: Float,
        paint: Paint
    ) {
        val left = (col * tileSize) + offsetX.roundToInt()
        val top = (row * tileSize) + offsetY.roundToInt()
        val dst = Rect(left, top, left + tileSize, top + tileSize)
        canvas.drawBitmap(bitmap, null, dst, paint)
    }

    private fun mirror(source: Bitmap): Bitmap {
        val matrix = Matrix().apply { preScale(-1f, 1f) }
        return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    }

    private fun opaqueBounds(bitmap: Bitmap): Rect? {
        var minX = bitmap.width
        var minY = bitmap.height
        var maxX = -1
        var maxY = -1

        for (y in 0 until bitmap.height) {
            for (x in 0 until bitmap.width) {
                val alpha = (bitmap.getPixel(x, y) ushr 24) and 0xFF
                if (alpha == 0) continue
                if (x < minX) minX = x
                if (y < minY) minY = y
                if (x > maxX) maxX = x
                if (y > maxY) maxY = y
            }
        }

        if (maxX < minX || maxY < minY) return null
        return Rect(minX, minY, maxX + 1, maxY + 1)
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
            append('|').append(config.heldItemId ?: "none")
        }
        val digest = MessageDigest.getInstance("SHA-256").digest(source.toByteArray(Charsets.UTF_8))
        return digest.joinToString(separator = "") { "%02x".format(it) }
    }
}
