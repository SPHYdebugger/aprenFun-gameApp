package com.example.aprendemoslavida.avatar

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.example.aprendemoslavida.R
import com.example.aprendemoslavida.utils.AvatarManager
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

class AvatarEditorView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    interface Listener {
        fun onAccessoryPlacementChanged(id: String, placement: AvatarManager.AccessoryPlacement)
    }

    var listener: Listener? = null

    private val baseAvatar: Bitmap? = BitmapFactory.decodeResource(resources, R.drawable.story_player_base)
    private val zonesAvatar: Bitmap? = BitmapFactory.decodeResource(resources, R.drawable.story_player_zones)
    private var style: AvatarManager.AvatarStyle = AvatarManager.defaultStyle()
    private var selectedOutfitId: String? = null
    private var placements: Map<String, AvatarManager.AccessoryPlacement> = emptyMap()

    private val avatarRect = RectF()
    private val workingPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val outlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 4f
        color = Color.WHITE
    }
    private val handlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#FFECC9")
    }
    private var selectedAccessoryId: String? = null
    private var gestureMode: GestureMode = GestureMode.NONE
    private var dragPointerId: Int = MotionEvent.INVALID_POINTER_ID
    private var rotationOffsetDeg: Float = 0f

    private enum class GestureMode {
        NONE,
        DRAG,
        ROTATE
    }

    fun setAvatarData(
        style: AvatarManager.AvatarStyle,
        selectedOutfitId: String?,
        placements: Map<String, AvatarManager.AccessoryPlacement>
    ) {
        this.style = style
        this.selectedOutfitId = selectedOutfitId
        this.placements = placements
        invalidate()
    }

    fun clearSelection() {
        selectedAccessoryId = null
        gestureMode = GestureMode.NONE
        dragPointerId = MotionEvent.INVALID_POINTER_ID
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val base = baseAvatar ?: return
        val tinted = AvatarManager.tintBitmap(base, style, zonesAvatar)
        computeAvatarRect(tinted.width.toFloat(), tinted.height.toFloat())
        canvas.drawBitmap(tinted, null, avatarRect, null)

        selectedOutfitId?.let { drawOutfit(canvas, avatarRect, it, style.shirtColor) }
        placements.forEach { (id, placement) ->
            if (!placement.enabled) return@forEach
            drawAccessory(canvas, avatarRect, id, placement)
        }
        drawSelectedOverlay(canvas)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                val x = event.x
                val y = event.y
                val hitId = findHitAccessory(x, y)
                selectedAccessoryId = hitId
                if (hitId == null) {
                    clearSelection()
                    return false
                }
                dragPointerId = event.getPointerId(0)
                gestureMode = if (isOnRotateHandle(hitId, x, y)) GestureMode.ROTATE else GestureMode.DRAG
                if (gestureMode == GestureMode.ROTATE) {
                    val center = accessoryCenterOnScreen(hitId) ?: return true
                    val current = placements[hitId] ?: return true
                    val touchDeg = screenAngleDeg(center.first, center.second, x, y)
                    rotationOffsetDeg = current.rotationDeg - touchDeg
                }
                parent.requestDisallowInterceptTouchEvent(true)
                invalidate()
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                val id = selectedAccessoryId ?: return false
                val pointerIndex = event.findPointerIndex(dragPointerId).takeIf { it >= 0 } ?: 0
                val x = event.getX(pointerIndex)
                val y = event.getY(pointerIndex)
                val current = placements[id] ?: return false

                val updated = when (gestureMode) {
                    GestureMode.DRAG -> {
                        val normX = ((x - avatarRect.left) / avatarRect.width()).coerceIn(0f, 1f)
                        val normY = ((y - avatarRect.top) / avatarRect.height()).coerceIn(0f, 1f)
                        current.copy(xNorm = normX, yNorm = normY)
                    }
                    GestureMode.ROTATE -> {
                        val center = accessoryCenterOnScreen(id) ?: return true
                        val touchDeg = screenAngleDeg(center.first, center.second, x, y)
                        current.copy(rotationDeg = touchDeg + rotationOffsetDeg)
                    }
                    GestureMode.NONE -> current
                }

                val mutable = placements.toMutableMap()
                mutable[id] = updated
                placements = mutable
                listener?.onAccessoryPlacementChanged(id, updated)
                invalidate()
                return true
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                gestureMode = GestureMode.NONE
                dragPointerId = MotionEvent.INVALID_POINTER_ID
                parent.requestDisallowInterceptTouchEvent(false)
                invalidate()
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun computeAvatarRect(bitmapW: Float, bitmapH: Float) {
        val margin = width * 0.08f
        val targetW = width - (margin * 2f)
        val targetH = height - (margin * 2f)
        if (targetW <= 0f || targetH <= 0f) {
            avatarRect.set(0f, 0f, width.toFloat(), height.toFloat())
            return
        }
        val scale = min(targetW / bitmapW, targetH / bitmapH)
        val drawW = bitmapW * scale
        val drawH = bitmapH * scale
        val left = (width - drawW) / 2f
        val top = (height - drawH) / 2f
        avatarRect.set(left, top, left + drawW, top + drawH)
    }

    private fun findHitAccessory(x: Float, y: Float): String? {
        val candidates = placements.entries.filter { it.value.enabled }
        for (entry in candidates.asReversed()) {
            val center = accessoryCenterOnScreen(entry.key) ?: continue
            val radius = accessoryBaseSizePx() * 0.55f
            val dist = distance(center.first, center.second, x, y)
            if (dist <= radius) return entry.key
        }
        return null
    }

    private fun isOnRotateHandle(id: String, x: Float, y: Float): Boolean {
        val center = accessoryCenterOnScreen(id) ?: return false
        val placement = placements[id] ?: return false
        val size = accessoryBaseSizePx()
        val rad = Math.toRadians(placement.rotationDeg.toDouble())
        val hx = center.first + (cos(rad) * 0.0 - sin(rad) * -size * 0.8).toFloat()
        val hy = center.second + (sin(rad) * 0.0 + cos(rad) * -size * 0.8).toFloat()
        return distance(hx, hy, x, y) < (size * 0.2f)
    }

    private fun accessoryCenterOnScreen(id: String): Pair<Float, Float>? {
        val placement = placements[id] ?: return null
        val cx = avatarRect.left + (placement.xNorm * avatarRect.width())
        val cy = avatarRect.top + (placement.yNorm * avatarRect.height())
        return cx to cy
    }

    private fun drawSelectedOverlay(canvas: Canvas) {
        val id = selectedAccessoryId ?: return
        val placement = placements[id] ?: return
        if (!placement.enabled) return
        val (cx, cy) = accessoryCenterOnScreen(id) ?: return
        val size = accessoryBaseSizePx()
        canvas.drawCircle(cx, cy, size * 0.62f, outlinePaint)

        val rad = Math.toRadians(placement.rotationDeg.toDouble())
        val hx = cx + (cos(rad) * 0.0 - sin(rad) * -size * 0.8).toFloat()
        val hy = cy + (sin(rad) * 0.0 + cos(rad) * -size * 0.8).toFloat()
        canvas.drawCircle(hx, hy, size * 0.16f, handlePaint)
    }

    private fun drawOutfit(canvas: Canvas, rect: RectF, outfitId: String, clothesColor: Int) {
        val w = rect.width()
        val h = rect.height()
        val darker = darken(clothesColor, 0.8f)
        workingPaint.style = Paint.Style.FILL
        when (outfitId) {
            "hoodie" -> {
                workingPaint.color = clothesColor
                canvas.drawRoundRect(
                    RectF(rect.left + w * 0.30f, rect.top + h * 0.43f, rect.left + w * 0.70f, rect.top + h * 0.82f),
                    w * 0.03f,
                    w * 0.03f,
                    workingPaint
                )
                workingPaint.color = darker
                canvas.drawRoundRect(
                    RectF(rect.left + w * 0.24f, rect.top + h * 0.48f, rect.left + w * 0.32f, rect.top + h * 0.78f),
                    w * 0.02f,
                    w * 0.02f,
                    workingPaint
                )
                canvas.drawRoundRect(
                    RectF(rect.left + w * 0.68f, rect.top + h * 0.48f, rect.left + w * 0.76f, rect.top + h * 0.78f),
                    w * 0.02f,
                    w * 0.02f,
                    workingPaint
                )
            }
            "cape" -> {
                workingPaint.color = darker
                val path = Path().apply {
                    moveTo(rect.left + w * 0.33f, rect.top + h * 0.46f)
                    lineTo(rect.left + w * 0.25f, rect.top + h * 0.88f)
                    lineTo(rect.left + w * 0.75f, rect.top + h * 0.88f)
                    lineTo(rect.left + w * 0.67f, rect.top + h * 0.46f)
                    close()
                }
                canvas.drawPath(path, workingPaint)
            }
            "boots" -> {
                workingPaint.color = darker
                canvas.drawRoundRect(
                    RectF(rect.left + w * 0.36f, rect.top + h * 0.82f, rect.left + w * 0.47f, rect.top + h * 0.95f),
                    w * 0.015f,
                    w * 0.015f,
                    workingPaint
                )
                canvas.drawRoundRect(
                    RectF(rect.left + w * 0.53f, rect.top + h * 0.82f, rect.left + w * 0.64f, rect.top + h * 0.95f),
                    w * 0.015f,
                    w * 0.015f,
                    workingPaint
                )
            }
            "skirt" -> {
                workingPaint.color = clothesColor
                val path = Path().apply {
                    moveTo(rect.left + w * 0.36f, rect.top + h * 0.60f)
                    lineTo(rect.left + w * 0.64f, rect.top + h * 0.60f)
                    lineTo(rect.left + w * 0.72f, rect.top + h * 0.82f)
                    lineTo(rect.left + w * 0.28f, rect.top + h * 0.82f)
                    close()
                }
                canvas.drawPath(path, workingPaint)
            }
            "armor" -> {
                workingPaint.color = Color.parseColor("#B8BEC8")
                canvas.drawRoundRect(
                    RectF(rect.left + w * 0.32f, rect.top + h * 0.44f, rect.left + w * 0.68f, rect.top + h * 0.80f),
                    w * 0.02f,
                    w * 0.02f,
                    workingPaint
                )
                workingPaint.color = Color.parseColor("#9098A5")
                workingPaint.strokeWidth = w * 0.02f
                canvas.drawLine(rect.left + w * 0.5f, rect.top + h * 0.44f, rect.left + w * 0.5f, rect.top + h * 0.80f, workingPaint)
            }
            "star_shirt" -> {
                workingPaint.color = clothesColor
                canvas.drawRoundRect(
                    RectF(rect.left + w * 0.31f, rect.top + h * 0.47f, rect.left + w * 0.69f, rect.top + h * 0.79f),
                    w * 0.02f,
                    w * 0.02f,
                    workingPaint
                )
                drawStar(canvas, rect.left + w * 0.50f, rect.top + h * 0.62f, w * 0.06f, Color.parseColor("#FFF2A6"))
            }
        }
    }

    private fun drawAccessory(canvas: Canvas, rect: RectF, id: String, placement: AvatarManager.AccessoryPlacement) {
        val cx = rect.left + (placement.xNorm * rect.width())
        val cy = rect.top + (placement.yNorm * rect.height())
        val s = accessoryBaseSizePx()
        canvas.save()
        canvas.translate(cx, cy)
        canvas.rotate(placement.rotationDeg)
        workingPaint.style = Paint.Style.FILL
        when (id) {
            "glasses" -> {
                workingPaint.style = Paint.Style.STROKE
                workingPaint.strokeWidth = s * 0.10f
                workingPaint.color = Color.parseColor("#3C3C3C")
                canvas.drawOval(RectF(-s * 0.55f, -s * 0.22f, -s * 0.15f, s * 0.18f), workingPaint)
                canvas.drawOval(RectF(s * 0.15f, -s * 0.22f, s * 0.55f, s * 0.18f), workingPaint)
                canvas.drawLine(-s * 0.15f, -s * 0.02f, s * 0.15f, -s * 0.02f, workingPaint)
            }
            "crown" -> {
                workingPaint.style = Paint.Style.FILL
                workingPaint.color = Color.parseColor("#F2C94C")
                val path = Path().apply {
                    moveTo(-s * 0.65f, -s * 0.08f)
                    lineTo(-s * 0.35f, -s * 0.55f)
                    lineTo(0f, -s * 0.12f)
                    lineTo(s * 0.35f, -s * 0.55f)
                    lineTo(s * 0.65f, -s * 0.08f)
                    lineTo(s * 0.65f, s * 0.15f)
                    lineTo(-s * 0.65f, s * 0.15f)
                    close()
                }
                canvas.drawPath(path, workingPaint)
            }
            "bow" -> {
                workingPaint.color = Color.parseColor("#FF5D8F")
                canvas.drawOval(RectF(-s * 0.58f, -s * 0.35f, -s * 0.10f, s * 0.02f), workingPaint)
                canvas.drawOval(RectF(s * 0.10f, -s * 0.35f, s * 0.58f, s * 0.02f), workingPaint)
                canvas.drawCircle(0f, -s * 0.16f, s * 0.11f, workingPaint)
            }
            "necklace" -> {
                workingPaint.style = Paint.Style.STROKE
                workingPaint.strokeWidth = s * 0.09f
                workingPaint.color = Color.parseColor("#E8B44D")
                canvas.drawArc(RectF(-s * 0.38f, -s * 0.22f, s * 0.38f, s * 0.40f), 25f, 130f, false, workingPaint)
            }
            "wand" -> {
                workingPaint.style = Paint.Style.STROKE
                workingPaint.strokeWidth = s * 0.07f
                workingPaint.color = Color.parseColor("#7A57D1")
                canvas.drawLine(-s * 0.20f, s * 0.20f, s * 0.45f, -s * 0.45f, workingPaint)
                drawStar(canvas, s * 0.55f, -s * 0.55f, s * 0.16f, Color.parseColor("#F2C94C"))
            }
            "balloon" -> {
                workingPaint.style = Paint.Style.FILL
                workingPaint.color = Color.parseColor("#FF6F61")
                canvas.drawOval(RectF(-s * 0.32f, -s * 0.68f, s * 0.32f, -s * 0.02f), workingPaint)
                workingPaint.style = Paint.Style.STROKE
                workingPaint.strokeWidth = s * 0.04f
                workingPaint.color = Color.parseColor("#7A4B35")
                canvas.drawLine(0f, -s * 0.02f, -s * 0.18f, s * 0.55f, workingPaint)
            }
        }
        canvas.restore()
    }

    private fun drawStar(canvas: Canvas, cx: Float, cy: Float, r: Float, color: Int) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color }
        val path = Path()
        for (i in 0..9) {
            val angle = Math.toRadians((i * 36.0) - 90.0)
            val radius = if (i % 2 == 0) r else r * 0.45f
            val x = cx + (cos(angle) * radius).toFloat()
            val y = cy + (sin(angle) * radius).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        canvas.drawPath(path, paint)
    }

    private fun accessoryBaseSizePx(): Float {
        return avatarRect.width() * 0.16f
    }

    private fun darken(color: Int, factor: Float): Int {
        val safe = factor.coerceIn(0f, 1f)
        return Color.rgb(
            (Color.red(color) * safe).toInt(),
            (Color.green(color) * safe).toInt(),
            (Color.blue(color) * safe).toInt()
        )
    }

    private fun distance(x1: Float, y1: Float, x2: Float, y2: Float): Float {
        val dx = x2 - x1
        val dy = y2 - y1
        return sqrt((dx * dx) + (dy * dy))
    }

    private fun screenAngleDeg(cx: Float, cy: Float, x: Float, y: Float): Float {
        return Math.toDegrees(atan2((y - cy).toDouble(), (x - cx).toDouble())).toFloat()
    }
}
