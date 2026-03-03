package com.example.aprendemoslavida.story

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.sqrt

// Lightweight top-down view with update/draw loop, collisions and gate blocking.
class StoryGameView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    interface Listener {
        fun onGateBlocked(gateId: Int)
        fun onExitReached()
    }

    enum class Direction {
        UP, DOWN, LEFT, RIGHT
    }

    private enum class Facing {
        UP, DOWN, LEFT, RIGHT
    }

    var listener: Listener? = null

    private val storyMap = StoryMap.createDefault()
    private var gates: List<StoryGate> = emptyList()

    private var lastFrameMs: Long = System.currentTimeMillis()
    private var loopStarted: Boolean = false
    private var interactionLocked: Boolean = false
    private var lastBlockedGateId: Int? = null
    private var exitNotified: Boolean = false
    private var analogInputX: Float = 0f
    private var analogInputY: Float = 0f

    private val pressedDirections = HashSet<Direction>()

    private val floorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#FFF0CF") }
    private val wallPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#516250") }
    private val gateLockedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#C23B3B") }
    private val gateOpenPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#6BBE5A") }
    private val exitPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#F2C94C") }
    private val playerFallbackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#3866D8") }

    private val fallbackPlayerBitmap: Bitmap? = BitmapFactory.decodeResource(
        resources,
        com.example.aprendemoslavida.R.drawable.splash_image
    )
    private var playerUpBitmap: Bitmap? = null
    private var playerDownBitmap: Bitmap? = null
    private var playerLeftBitmap: Bitmap? = null
    private var playerRightBitmap: Bitmap? = null
    private var facing: Facing = Facing.DOWN

    private var playerCenterX = storyMap.startTileX
    private var playerCenterY = storyMap.startTileY
    private val playerHalfSize = 0.35f
    private val movementTilesPerSecond = 2.8f
    private val cameraVisibleTilesX = 8f
    private val cameraVisibleTilesY = 8f

    init {
        loadDirectionalSprites()
    }

    fun setGates(gates: List<StoryGate>) {
        this.gates = gates
        invalidate()
    }

    fun setDirectionPressed(direction: Direction, pressed: Boolean) {
        if (pressed) {
            pressedDirections.add(direction)
        } else {
            pressedDirections.remove(direction)
        }
    }

    fun setInputVector(x: Float, y: Float) {
        analogInputX = x.coerceIn(-1f, 1f)
        analogInputY = y.coerceIn(-1f, 1f)
    }

    fun setQuestionBlocking(active: Boolean) {
        interactionLocked = active
        if (active) {
            pressedDirections.clear()
            setInputVector(0f, 0f)
        }
    }

    fun resetPlayerPosition() {
        playerCenterX = storyMap.startTileX
        playerCenterY = storyMap.startTileY
        exitNotified = false
        invalidate()
    }

    fun intersectsExit(): Boolean {
        return RectF(
            playerCenterX - playerHalfSize,
            playerCenterY - playerHalfSize,
            playerCenterX + playerHalfSize,
            playerCenterY + playerHalfSize
        ).intersect(storyMap.exitRect)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (!loopStarted) {
            loopStarted = true
            lastFrameMs = System.currentTimeMillis()
            post(frameRunnable)
        }
    }

    override fun onDetachedFromWindow() {
        loopStarted = false
        removeCallbacks(frameRunnable)
        super.onDetachedFromWindow()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val tileSize = min(width / cameraVisibleTilesX, height / cameraVisibleTilesY)
        val halfViewTilesX = width / (2f * tileSize)
        val halfViewTilesY = height / (2f * tileSize)
        val cameraX = clampCameraCoord(playerCenterX, halfViewTilesX, storyMap.width.toFloat())
        val cameraY = clampCameraCoord(playerCenterY, halfViewTilesY, storyMap.height.toFloat())

        val minTileX = floor(cameraX - halfViewTilesX).toInt() - 1
        val maxTileX = ceil(cameraX + halfViewTilesX).toInt() + 1
        val minTileY = floor(cameraY - halfViewTilesY).toInt() - 1
        val maxTileY = ceil(cameraY + halfViewTilesY).toInt() + 1

        for (y in minTileY..maxTileY) {
            for (x in minTileX..maxTileX) {
                val left = ((x - cameraX) * tileSize) + (width / 2f)
                val top = ((y - cameraY) * tileSize) + (height / 2f)
                val rect = RectF(left, top, left + tileSize, top + tileSize)
                val paint = if (storyMap.isWalkable(x, y)) floorPaint else wallPaint
                canvas.drawRect(rect, paint)
            }
        }

        val exitRectPx = toScreenRect(storyMap.exitRect, tileSize, cameraX, cameraY)
        canvas.drawRect(exitRectPx, exitPaint)

        gates.forEach { gate ->
            val rect = toScreenRect(gate.rect, tileSize, cameraX, cameraY)
            canvas.drawRect(rect, if (gate.unlocked) gateOpenPaint else gateLockedPaint)
        }

        val playerRectPx = RectF(
            ((playerCenterX - playerHalfSize - cameraX) * tileSize) + (width / 2f),
            ((playerCenterY - playerHalfSize - cameraY) * tileSize) + (height / 2f),
            ((playerCenterX + playerHalfSize - cameraX) * tileSize) + (width / 2f),
            ((playerCenterY + playerHalfSize - cameraY) * tileSize) + (height / 2f)
        )

        val playerBitmap = currentPlayerBitmap()
        if (playerBitmap != null) {
            canvas.drawBitmap(playerBitmap, null, playerRectPx, null)
        } else {
            canvas.drawOval(playerRectPx, playerFallbackPaint)
        }
    }

    private val frameRunnable = object : Runnable {
        override fun run() {
            if (!loopStarted) return

            val now = System.currentTimeMillis()
            val deltaSec = ((now - lastFrameMs).coerceAtMost(34L) / 1000f)
            lastFrameMs = now

            update(deltaSec)
            invalidate()
            postOnAnimation(this)
        }
    }

    private fun update(deltaSec: Float) {
        if (!interactionLocked) {
            val vector = movementVector()
            if (vector.first != 0f || vector.second != 0f) {
                updateFacing(vector.first, vector.second)
                val distance = movementTilesPerSecond * deltaSec
                tryMove(vector.first * distance, vector.second * distance)
            } else {
                lastBlockedGateId = null
            }
        }

        if (intersectsExit()) {
            if (!exitNotified) {
                exitNotified = true
                listener?.onExitReached()
            }
        } else {
            exitNotified = false
        }
    }

    private fun movementVector(): Pair<Float, Float> {
        if (analogInputX != 0f || analogInputY != 0f) {
            val magnitude = sqrt((analogInputX * analogInputX) + (analogInputY * analogInputY))
            if (magnitude > 0f) {
                return (analogInputX / magnitude) to (analogInputY / magnitude)
            }
        }

        var x = 0f
        var y = 0f
        if (pressedDirections.contains(Direction.LEFT)) x -= 1f
        if (pressedDirections.contains(Direction.RIGHT)) x += 1f
        if (pressedDirections.contains(Direction.UP)) y -= 1f
        if (pressedDirections.contains(Direction.DOWN)) y += 1f

        if (x == 0f && y == 0f) return 0f to 0f

        val magnitude = sqrt((x * x) + (y * y))
        return (x / magnitude) to (y / magnitude)
    }

    private fun tryMove(deltaX: Float, deltaY: Float) {
        var moved = false

        if (deltaX != 0f) {
            val candidate = RectF(
                playerCenterX + deltaX - playerHalfSize,
                playerCenterY - playerHalfSize,
                playerCenterX + deltaX + playerHalfSize,
                playerCenterY + playerHalfSize
            )
            if (!storyMap.collidesWithWall(candidate) && !isBlockedByGate(candidate)) {
                playerCenterX += deltaX
                moved = true
            }
        }

        if (deltaY != 0f) {
            val candidate = RectF(
                playerCenterX - playerHalfSize,
                playerCenterY + deltaY - playerHalfSize,
                playerCenterX + playerHalfSize,
                playerCenterY + deltaY + playerHalfSize
            )
            if (!storyMap.collidesWithWall(candidate) && !isBlockedByGate(candidate)) {
                playerCenterY += deltaY
                moved = true
            }
        }

        if (moved) {
            lastBlockedGateId = null
        }
    }

    private fun isBlockedByGate(playerRect: RectF): Boolean {
        val blockingGate = gates.firstOrNull { !it.unlocked && RectF(playerRect).intersect(it.rect) }
        if (blockingGate != null) {
            if (lastBlockedGateId != blockingGate.id) {
                lastBlockedGateId = blockingGate.id
                listener?.onGateBlocked(blockingGate.id)
            }
            return true
        }
        return false
    }

    private fun toScreenRect(worldRect: RectF, tileSize: Float, cameraX: Float, cameraY: Float): RectF {
        return RectF(
            ((worldRect.left - cameraX) * tileSize) + (width / 2f),
            ((worldRect.top - cameraY) * tileSize) + (height / 2f),
            ((worldRect.right - cameraX) * tileSize) + (width / 2f),
            ((worldRect.bottom - cameraY) * tileSize) + (height / 2f)
        )
    }

    private fun currentPlayerBitmap(): Bitmap? {
        return when (facing) {
            Facing.UP -> playerUpBitmap
            Facing.DOWN -> playerDownBitmap
            Facing.LEFT -> playerLeftBitmap
            Facing.RIGHT -> playerRightBitmap
        } ?: fallbackPlayerBitmap
    }

    private fun updateFacing(vectorX: Float, vectorY: Float) {
        facing = if (kotlin.math.abs(vectorX) > kotlin.math.abs(vectorY)) {
            if (vectorX >= 0f) Facing.RIGHT else Facing.LEFT
        } else {
            if (vectorY >= 0f) Facing.DOWN else Facing.UP
        }
    }

    private fun loadDirectionalSprites() {
        // Option 0: single frontal sprite reused for all directions.
        val front = loadBitmapByName("story_player_front")
        if (front != null) {
            playerUpBitmap = front
            playerDownBitmap = front
            playerLeftBitmap = front
            playerRightBitmap = front
            return
        }

        // Option A: separate drawables named story_player_up/down/left/right.
        playerUpBitmap = loadBitmapByName("story_player_up")
        playerDownBitmap = loadBitmapByName("story_player_down")
        playerLeftBitmap = loadBitmapByName("story_player_left")
        playerRightBitmap = loadBitmapByName("story_player_right")

        // Option B: single 2x2 sheet named story_player_sheet (up, right, left, down).
        if (playerUpBitmap == null || playerDownBitmap == null || playerLeftBitmap == null || playerRightBitmap == null) {
            val sheet = loadBitmapByName("story_player_sheet")
            if (sheet != null && sheet.width >= 2 && sheet.height >= 2) {
                val cellW = sheet.width / 2
                val cellH = sheet.height / 2
                playerUpBitmap = playerUpBitmap ?: Bitmap.createBitmap(sheet, 0, 0, cellW, cellH)
                playerRightBitmap = playerRightBitmap ?: Bitmap.createBitmap(sheet, cellW, 0, cellW, cellH)
                playerLeftBitmap = playerLeftBitmap ?: Bitmap.createBitmap(sheet, 0, cellH, cellW, cellH)
                playerDownBitmap = playerDownBitmap ?: Bitmap.createBitmap(sheet, cellW, cellH, cellW, cellH)
            }
        }

        // Fallback: mirror right sprite if left is missing.
        if (playerLeftBitmap == null && playerRightBitmap != null) {
            val mirrorMatrix = Matrix().apply { preScale(-1f, 1f) }
            playerLeftBitmap = Bitmap.createBitmap(
                playerRightBitmap!!,
                0,
                0,
                playerRightBitmap!!.width,
                playerRightBitmap!!.height,
                mirrorMatrix,
                true
            )
        }
    }

    private fun loadBitmapByName(name: String): Bitmap? {
        val id = resources.getIdentifier(name, "drawable", context.packageName)
        if (id == 0) return null
        return BitmapFactory.decodeResource(resources, id)
    }

    private fun clampCameraCoord(value: Float, halfView: Float, mapSize: Float): Float {
        val minBound = halfView
        val maxBound = mapSize - halfView
        if (minBound > maxBound) {
            return mapSize / 2f
        }
        return value.coerceIn(minBound, maxBound)
    }
}
