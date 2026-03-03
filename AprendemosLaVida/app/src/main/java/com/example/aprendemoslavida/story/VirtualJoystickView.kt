package com.example.aprendemoslavida.story

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

// Drawn virtual joystick that emits a normalized movement vector while dragging.
class VirtualJoystickView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    interface Listener {
        fun onInputChanged(x: Float, y: Float)
    }

    var listener: Listener? = null

    private val basePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#55FFFFFF")
    }
    private val knobPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#CCFF7A59")
    }

    private var centerX = 0f
    private var centerY = 0f
    private var baseRadius = 0f
    private var knobRadius = 0f
    private var knobX = 0f
    private var knobY = 0f
    private var tracking = false

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        centerX = w / 2f
        centerY = h / 2f
        baseRadius = min(w, h) * 0.44f
        knobRadius = baseRadius * 0.38f
        resetStick()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawCircle(centerX, centerY, baseRadius, basePaint)
        canvas.drawCircle(knobX, knobY, knobRadius, knobPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE, MotionEvent.ACTION_POINTER_DOWN -> {
                tracking = true
                updateKnob(event.x, event.y)
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_POINTER_UP -> {
                if (tracking) {
                    resetStick()
                    listener?.onInputChanged(0f, 0f)
                    tracking = false
                }
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    fun resetStick() {
        knobX = centerX
        knobY = centerY
        invalidate()
    }

    private fun updateKnob(touchX: Float, touchY: Float) {
        val dx = touchX - centerX
        val dy = touchY - centerY
        val distance = sqrt((dx * dx) + (dy * dy))
        val maxDistance = baseRadius - knobRadius

        val finalDx: Float
        val finalDy: Float
        if (distance <= maxDistance || distance == 0f) {
            finalDx = dx
            finalDy = dy
        } else {
            val angle = atan2(dy, dx)
            finalDx = cos(angle) * maxDistance
            finalDy = sin(angle) * maxDistance
        }

        knobX = centerX + finalDx
        knobY = centerY + finalDy
        invalidate()

        val normalizedX = (finalDx / maxDistance).coerceIn(-1f, 1f)
        val normalizedY = (finalDy / maxDistance).coerceIn(-1f, 1f)
        listener?.onInputChanged(normalizedX, normalizedY)
    }
}
