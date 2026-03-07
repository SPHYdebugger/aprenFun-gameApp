package com.example.aprendemoslavida.story

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import kotlin.math.max
import kotlin.random.Random

class ConfettiView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private data class Piece(
        var x: Float,
        var y: Float,
        var speedY: Float,
        var speedX: Float,
        var size: Float,
        var color: Int
    )

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val pieces = ArrayList<Piece>()
    private var running = false
    private var lastFrameMs = System.currentTimeMillis()

    private val colors = intArrayOf(
        Color.parseColor("#FF5252"),
        Color.parseColor("#FFC107"),
        Color.parseColor("#4CAF50"),
        Color.parseColor("#42A5F5"),
        Color.parseColor("#AB47BC")
    )

    private val ticker = object : Runnable {
        override fun run() {
            if (!running) return
            val now = System.currentTimeMillis()
            val dt = ((now - lastFrameMs).coerceAtMost(40L) / 1000f).coerceAtLeast(0.016f)
            lastFrameMs = now
            update(dt)
            invalidate()
            postOnAnimation(this)
        }
    }

    fun start() {
        if (running) return
        running = true
        pieces.clear()
        repeat(80) { pieces.add(newPiece(spawnAtTop = false)) }
        lastFrameMs = System.currentTimeMillis()
        postOnAnimation(ticker)
    }

    fun stop() {
        running = false
        removeCallbacks(ticker)
        pieces.clear()
        invalidate()
    }

    override fun onDetachedFromWindow() {
        stop()
        super.onDetachedFromWindow()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        pieces.forEach { p ->
            paint.color = p.color
            canvas.drawRect(p.x, p.y, p.x + p.size, p.y + p.size, paint)
        }
    }

    private fun update(dt: Float) {
        if (width <= 0 || height <= 0) return

        repeat(4) { pieces.add(newPiece(spawnAtTop = true)) }

        val iterator = pieces.iterator()
        while (iterator.hasNext()) {
            val p = iterator.next()
            p.x += p.speedX * dt
            p.y += p.speedY * dt
            if (p.y > height + 20f || p.x < -40f || p.x > width + 40f) {
                iterator.remove()
            }
        }

        val maxPieces = max(100, width / 4)
        while (pieces.size > maxPieces) {
            pieces.removeAt(0)
        }
    }

    private fun newPiece(spawnAtTop: Boolean): Piece {
        val w = max(width, 1)
        val h = max(height, 1)
        val x = Random.nextFloat() * w
        val y = if (spawnAtTop) -20f else Random.nextFloat() * h
        return Piece(
            x = x,
            y = y,
            speedY = 130f + Random.nextFloat() * 210f,
            speedX = -45f + Random.nextFloat() * 90f,
            size = 6f + Random.nextFloat() * 8f,
            color = colors[Random.nextInt(colors.size)]
        )
    }
}

