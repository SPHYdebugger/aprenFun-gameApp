package com.example.aprendemoslavida.story

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
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
        fun onSantiNpcReached()
    }

    enum class Direction {
        UP, DOWN, LEFT, RIGHT
    }

    private enum class Facing {
        UP, DOWN, LEFT, RIGHT
    }

    var listener: Listener? = null

    private var storyMap = StoryMap.createDefault()
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
    private val tilesetTileSizePx = 32
    private val tilesetColumns = 4
    private val forestTilesetBitmap: Bitmap? = buildForestTileset(tilesetTileSizePx)
    private val cityRoadTilesetBitmap: Bitmap? = buildCityTileset(tilesetTileSizePx)
    private val cityBuildingBitmaps: List<Bitmap> = buildCityBuildingBitmaps()
    private val cityTreeBuildingBitmaps: List<Bitmap> =
        if (cityBuildingBitmaps.size >= 10) cityBuildingBitmaps.take(5) else cityBuildingBitmaps
    private val cityRockBuildingBitmaps: List<Bitmap> =
        if (cityBuildingBitmaps.size >= 10) cityBuildingBitmaps.drop(5) else cityBuildingBitmaps
    private val forestTileSourceMap: Map<StoryMap.TileType, Rect> = buildTileSourceMap(tilesetTileSizePx)
    private val cityRoadTileSourceMap: Map<StoryMap.TileType, Rect> = buildTileSourceMap(tilesetTileSizePx)
    private val trophyBitmap: Bitmap = buildTrophyBitmap(24)
    private val santiBitmap: Bitmap? = loadBitmapByName("story_player2_front")

    private val fallbackPlayerBitmap: Bitmap? = BitmapFactory.decodeResource(
        resources,
        com.example.aprendemoslavida.R.drawable.splash_image
    )
    private val playerIdleBitmap: Bitmap? by lazy { loadBitmapByName("story_player_base") }
    private var playerUpBitmap: Bitmap? = null
    private var playerDownBitmap: Bitmap? = null
    private var playerLeftBitmap: Bitmap? = null
    private var playerRightBitmap: Bitmap? = null
    private var playerUpFrames: List<Bitmap> = emptyList()
    private var playerRightFrames: List<Bitmap> = emptyList()
    private var playerDownFrames: List<Bitmap> = emptyList()
    private var playerLeftFrames: List<Bitmap> = emptyList()
    private var playerTilesetBitmap: Bitmap? = null
    private var playerTilesetTileSize: Int = 0
    private val playerFrameCount = 4
    private val playerAnimFrameMs = 140L
    private var playerAnimTimeMs = 0L
    private var playerAnimFrame = 0
    private var playerMoving = false
    private var facing: Facing = Facing.DOWN

    private var playerCenterX = storyMap.startTileX
    private var playerCenterY = storyMap.startTileY
    private val playerHalfSize = 0.35f
    private val movementTilesPerSecond = 2.8f
    private val cameraVisibleTilesX = 4.5f
    private val cameraVisibleTilesY = 4.5f
    private val santiNpcHalfSize = 0.34f
    private var santiNpcRect: RectF? = null
    private var santiNpcReachedNotified: Boolean = false

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

    fun randomizeSecretEntrance() {
        storyMap.randomizeSecretEntrance()
        invalidate()
    }

    fun currentMap(): StoryMap = storyMap

    fun setSantiNpcTile(tile: Pair<Int, Int>?) {
        santiNpcRect = if (tile == null) {
            null
        } else {
            val (x, y) = tile
            RectF(x.toFloat(), y.toFloat(), x + 1f, y + 1f)
        }
        santiNpcReachedNotified = false
        invalidate()
    }

    fun setMap(map: StoryMap) {
        storyMap = map
        gates = emptyList()
        pressedDirections.clear()
        analogInputX = 0f
        analogInputY = 0f
        lastBlockedGateId = null
        exitNotified = false
        santiNpcRect = null
        santiNpcReachedNotified = false
        playerCenterX = storyMap.startTileX
        playerCenterY = storyMap.startTileY
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
        val activeHiddenZone = storyMap.hiddenZoneAtPoint(playerCenterX, playerCenterY)

        for (y in minTileY..maxTileY) {
            for (x in minTileX..maxTileX) {
                val left = ((x - cameraX) * tileSize) + (width / 2f)
                val top = ((y - cameraY) * tileSize) + (height / 2f)
                val rect = RectF(left, top, left + tileSize, top + tileSize)
                val tileZone = storyMap.hiddenZoneAtTile(x, y)
                val tileType = when {
                    tileZone >= 0 && tileZone != activeHiddenZone -> StoryMap.TileType.TREE
                    else -> storyMap.tileTypeAt(x, y)
                }
                val (tileset, src) = when (storyMap.visualTheme) {
                    StoryMap.VisualTheme.FOREST -> {
                        forestTilesetBitmap to forestTileSourceMap[tileType]
                    }
                    StoryMap.VisualTheme.CITY_CLASSIC -> {
                        cityRoadTilesetBitmap to cityRoadTileSourceMap[tileType]
                    }
                    StoryMap.VisualTheme.CITY_TILESET -> {
                        when (tileType) {
                            StoryMap.TileType.DIRT, StoryMap.TileType.GRASS -> {
                                cityRoadTilesetBitmap to cityRoadTileSourceMap[tileType]
                            }
                            StoryMap.TileType.TREE, StoryMap.TileType.ROCK -> {
                                val buildingPool = when (tileType) {
                                    StoryMap.TileType.TREE -> cityTreeBuildingBitmaps
                                    StoryMap.TileType.ROCK -> cityRockBuildingBitmaps
                                    else -> emptyList()
                                }
                                val seed = (x * 31) + (y * 17) + if (tileType == StoryMap.TileType.TREE) 0 else 11
                                val buildingBitmap = if (buildingPool.isNotEmpty()) {
                                    buildingPool[kotlin.math.abs(seed) % buildingPool.size]
                                } else {
                                    null
                                }
                                val buildingSet = buildingBitmap ?: cityRoadTilesetBitmap
                                val buildingSrc = if (buildingBitmap != null) {
                                    Rect(0, 0, buildingBitmap.width, buildingBitmap.height)
                                } else {
                                    cityRoadTileSourceMap[tileType]
                                }
                                buildingSet to buildingSrc
                            }
                        }
                    }
                }
                if (tileset != null && src != null) {
                    canvas.drawBitmap(tileset, src, rect, null)
                } else {
                    val paint = if (storyMap.isWalkable(x, y)) floorPaint else wallPaint
                    canvas.drawRect(rect, paint)
                }
            }
        }

        val exitRectPx = toScreenRect(storyMap.exitRect, tileSize, cameraX, cameraY)
        canvas.drawRect(exitRectPx, exitPaint)

        gates.forEach { gate ->
            val gateZone = storyMap.hiddenZoneAtPoint(gate.rect.centerX(), gate.rect.centerY())
            if (gateZone >= 0 && gateZone != activeHiddenZone) return@forEach
            val rect = toScreenRect(gate.rect, tileSize, cameraX, cameraY)
            if (!gate.unlocked) {
                drawTrophy(canvas, rect)
            }
        }

        val santiRect = santiNpcRect
        if (santiRect != null) {
            val santiZone = storyMap.hiddenZoneAtPoint(santiRect.centerX(), santiRect.centerY())
            if (santiZone < 0 || santiZone == activeHiddenZone) {
                val rect = toScreenRect(santiRect, tileSize, cameraX, cameraY)
                drawSanti(canvas, rect)
            }
        }

        val playerRectPx = RectF(
            ((playerCenterX - playerHalfSize - cameraX) * tileSize) + (width / 2f),
            ((playerCenterY - playerHalfSize - cameraY) * tileSize) + (height / 2f),
            ((playerCenterX + playerHalfSize - cameraX) * tileSize) + (width / 2f),
            ((playerCenterY + playerHalfSize - cameraY) * tileSize) + (height / 2f)
        )

        drawPlayer(canvas, playerRectPx)
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
                updateAnimation(deltaSec, moving = true)
                updateFacing(vector.first, vector.second)
                val distance = movementTilesPerSecond * deltaSec
                tryMove(vector.first * distance, vector.second * distance)
            } else {
                updateAnimation(deltaSec, moving = false)
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

        notifySantiNpcCollision()
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
            val candidateCenterX = playerCenterX + deltaX
            val candidateCenterY = playerCenterY
            val activeZone = effectiveHiddenZoneForMove(candidateCenterX, candidateCenterY)
            val candidate = RectF(
                playerCenterX + deltaX - playerHalfSize,
                playerCenterY - playerHalfSize,
                playerCenterX + deltaX + playerHalfSize,
                playerCenterY + playerHalfSize
            )
            if (!storyMap.collidesWithWall(candidate, activeZone) && !isBlockedByGate(candidate)) {
                playerCenterX += deltaX
                moved = true
            }
        }

        if (deltaY != 0f) {
            val candidateCenterX = playerCenterX
            val candidateCenterY = playerCenterY + deltaY
            val activeZone = effectiveHiddenZoneForMove(candidateCenterX, candidateCenterY)
            val candidate = RectF(
                playerCenterX - playerHalfSize,
                playerCenterY + deltaY - playerHalfSize,
                playerCenterX + playerHalfSize,
                playerCenterY + deltaY + playerHalfSize
            )
            if (!storyMap.collidesWithWall(candidate, activeZone) && !isBlockedByGate(candidate)) {
                playerCenterY += deltaY
                moved = true
            }
        }

        if (moved) {
            lastBlockedGateId = null
        }
    }

    private fun effectiveHiddenZoneForMove(candidateCenterX: Float, candidateCenterY: Float): Int {
        val currentZone = storyMap.hiddenZoneAtPoint(playerCenterX, playerCenterY)
        if (currentZone >= 0) return currentZone
        return storyMap.hiddenZoneAtPoint(candidateCenterX, candidateCenterY)
    }

    private fun isBlockedByGate(playerRect: RectF): Boolean {
        val blockingGate = gates.firstOrNull { gate ->
            if (gate.unlocked) return@firstOrNull false
            val hitRect = gateHitRect(gate)
            RectF(playerRect).intersect(hitRect)
        }
        if (blockingGate != null) {
            if (lastBlockedGateId != blockingGate.id) {
                lastBlockedGateId = blockingGate.id
                listener?.onGateBlocked(blockingGate.id)
            }
            return true
        }
        return false
    }

    private fun gateHitRect(gate: StoryGate): RectF {
        val inset = 0.2f
        val rect = RectF(gate.rect)
        rect.inset(inset, inset)
        if (rect.width() <= 0f || rect.height() <= 0f) {
            return RectF(gate.rect)
        }
        return rect
    }

    private fun notifySantiNpcCollision() {
        val santiRect = santiNpcRect ?: return
        if (santiNpcReachedNotified) return
        val playerRect = RectF(
            playerCenterX - playerHalfSize,
            playerCenterY - playerHalfSize,
            playerCenterX + playerHalfSize,
            playerCenterY + playerHalfSize
        )
        val hitRect = RectF(
            santiRect.centerX() - santiNpcHalfSize,
            santiRect.centerY() - santiNpcHalfSize,
            santiRect.centerX() + santiNpcHalfSize,
            santiRect.centerY() + santiNpcHalfSize
        )
        if (RectF(playerRect).intersect(hitRect)) {
            santiNpcReachedNotified = true
            listener?.onSantiNpcReached()
        }
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

    private fun currentPlayerFrameBitmap(): Bitmap? {
        val frameIndex = if (playerMoving) playerAnimFrame else 0
        val frames = when (facing) {
            Facing.UP -> playerUpFrames
            Facing.DOWN -> playerDownFrames
            Facing.LEFT -> playerLeftFrames
            Facing.RIGHT -> playerRightFrames
        }
        if (frames.isEmpty()) return null
        return frames[frameIndex.coerceIn(0, frames.lastIndex)]
    }

    private fun updateFacing(vectorX: Float, vectorY: Float) {
        facing = if (kotlin.math.abs(vectorX) > kotlin.math.abs(vectorY)) {
            if (vectorX >= 0f) Facing.RIGHT else Facing.LEFT
        } else {
            if (vectorY >= 0f) Facing.DOWN else Facing.UP
        }
    }

    private fun loadDirectionalSprites() {
        // Option -1: explicit frame-by-frame resources:
        // player_frames_<direction>_<1..4>
        val upFrames = loadFrameSequence("player_frames_up")
        // The imported sheet has left/right rows swapped relative to movement vectors.
        // Swap them here so on-screen motion matches joystick direction.
        val rightFrames = loadFrameSequence("player_frames_left")
        val downFrames = loadFrameSequence("player_frames_down")
        val leftFrames = loadFrameSequence("player_frames_right")
        if (upFrames.size == playerFrameCount &&
            rightFrames.size == playerFrameCount &&
            downFrames.size == playerFrameCount &&
            leftFrames.size == playerFrameCount
        ) {
            playerUpFrames = upFrames
            playerRightFrames = rightFrames
            playerDownFrames = downFrames
            playerLeftFrames = leftFrames
            playerUpBitmap = upFrames.firstOrNull()
            playerRightBitmap = rightFrames.firstOrNull()
            playerDownBitmap = downFrames.firstOrNull()
            playerLeftBitmap = leftFrames.firstOrNull()
            return
        }

        // Option 0: full tileset in a 4x4 grid (rows: up, right, down, left).
        val sheet = loadBitmapByName("story_player_sheet")
        if (sheet != null && sheet.width >= playerFrameCount && sheet.height >= 4) {
            playerTilesetBitmap = stylePlayerBitmap(sheet)
            playerTilesetTileSize = sheet.width / playerFrameCount
            return
        }

        // Option 0b: generate a tileset from a base sprite named story_player_base.
        val base = loadBitmapByName("story_player_base")
        if (base != null) {
            val generated = buildPlayerTileset(stylePlayerBitmap(base), playerFrameCount)
            playerTilesetBitmap = generated.first
            playerTilesetTileSize = generated.second
            return
        }

        // Option 0: single frontal sprite reused for all directions.
        val front = loadBitmapByName("story_player_front")
        if (front != null) {
            val styledFront = stylePlayerBitmap(front)
            playerUpBitmap = styledFront
            playerDownBitmap = styledFront
            playerLeftBitmap = styledFront
            playerRightBitmap = styledFront
            return
        }

        // Option A: separate drawables named story_player_up/down/left/right.
        playerUpBitmap = loadBitmapByName("story_player_up")?.let(::stylePlayerBitmap)
        playerDownBitmap = loadBitmapByName("story_player_down")?.let(::stylePlayerBitmap)
        playerLeftBitmap = loadBitmapByName("story_player_left")?.let(::stylePlayerBitmap)
        playerRightBitmap = loadBitmapByName("story_player_right")?.let(::stylePlayerBitmap)

        // Option B: single 2x2 sheet named story_player_sheet (up, right, left, down).
        if (playerUpBitmap == null || playerDownBitmap == null || playerLeftBitmap == null || playerRightBitmap == null) {
            val sheet = loadBitmapByName("story_player_sheet")
            if (sheet != null && sheet.width >= 2 && sheet.height >= 2) {
                val cellW = sheet.width / 2
                val cellH = sheet.height / 2
                playerUpBitmap = playerUpBitmap ?: stylePlayerBitmap(Bitmap.createBitmap(sheet, 0, 0, cellW, cellH))
                playerRightBitmap = playerRightBitmap ?: stylePlayerBitmap(Bitmap.createBitmap(sheet, cellW, 0, cellW, cellH))
                playerLeftBitmap = playerLeftBitmap ?: stylePlayerBitmap(Bitmap.createBitmap(sheet, 0, cellH, cellW, cellH))
                playerDownBitmap = playerDownBitmap ?: stylePlayerBitmap(Bitmap.createBitmap(sheet, cellW, cellH, cellW, cellH))
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

    private fun updateAnimation(deltaSec: Float, moving: Boolean) {
        if (!moving) {
            playerMoving = false
            playerAnimTimeMs = 0L
            playerAnimFrame = 0
            return
        }

        val deltaMs = (deltaSec * 1000f).toLong().coerceAtLeast(0L)
        playerMoving = true
        playerAnimTimeMs += deltaMs
        if (playerAnimTimeMs >= playerAnimFrameMs * playerFrameCount) {
            playerAnimTimeMs %= (playerAnimFrameMs * playerFrameCount)
        }
        playerAnimFrame = (playerAnimTimeMs / playerAnimFrameMs).toInt().coerceIn(0, playerFrameCount - 1)
    }

    private fun drawPlayer(canvas: Canvas, dst: RectF) {
        if (!playerMoving) {
            val idle = playerIdleBitmap
            if (idle != null) {
                canvas.drawBitmap(idle, null, dst, null)
                return
            }
        }

        val adjustedDst = adjustedPlayerDst(dst)
        val frameBitmap = currentPlayerFrameBitmap()
        if (frameBitmap != null) {
            canvas.drawBitmap(frameBitmap, null, adjustedDst, null)
            return
        }

        val tileset = playerTilesetBitmap
        if (tileset != null && playerTilesetTileSize > 0) {
            val src = currentPlayerFrameRect()
            if (src != null) {
                canvas.drawBitmap(tileset, src, adjustedDst, null)
                return
            }
        }

        val playerBitmap = currentPlayerBitmap()
        if (playerBitmap != null) {
            canvas.drawBitmap(playerBitmap, null, adjustedDst, null)
        } else {
            canvas.drawOval(adjustedDst, playerFallbackPaint)
        }
    }

    private fun adjustedPlayerDst(dst: RectF): RectF {
        var adjusted = dst
        if (facing == Facing.UP) {
            // Up-facing imported frames are slightly narrower; widen a bit to match other directions.
            val extraHalfWidth = adjusted.width() * 0.08f
            adjusted = RectF(adjusted.left - extraHalfWidth, adjusted.top, adjusted.right + extraHalfWidth, adjusted.bottom)
        }
        if (playerMoving) {
            adjusted = scaleRectFromCenter(adjusted, 0.90f)
        }
        return adjusted
    }

    private fun scaleRectFromCenter(rect: RectF, scale: Float): RectF {
        val safeScale = scale.coerceIn(0.1f, 2f)
        val cx = rect.centerX()
        val cy = rect.centerY()
        val halfW = (rect.width() * safeScale) / 2f
        val halfH = (rect.height() * safeScale) / 2f
        return RectF(cx - halfW, cy - halfH, cx + halfW, cy + halfH)
    }

    private fun currentPlayerFrameRect(): Rect? {
        val tileset = playerTilesetBitmap ?: return null
        val tile = playerTilesetTileSize
        if (tile <= 0) return null
        val row = when (facing) {
            Facing.UP -> 0
            Facing.RIGHT -> 1
            Facing.DOWN -> 2
            Facing.LEFT -> 3
        }
        val col = if (playerMoving) playerAnimFrame else 0
        val left = col * tile
        val top = row * tile
        if (left + tile > tileset.width || top + tile > tileset.height) return null
        return Rect(left, top, left + tile, top + tile)
    }

    private fun buildPlayerTileset(base: Bitmap, frames: Int): Pair<Bitmap, Int> {
        val tileSize = maxOf(base.width, base.height)
        val rows = 4
        val bitmap = Bitmap.createBitmap(tileSize * frames, tileSize * rows, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        fun drawFrame(
            targetCol: Int,
            targetRow: Int,
            offsetX: Float,
            offsetY: Float,
            mirror: Boolean
        ) {
            val left = targetCol * tileSize
            val top = targetRow * tileSize
            val dstLeft = left + (tileSize - base.width) / 2f + offsetX
            val dstTop = top + (tileSize - base.height) / 2f + offsetY
            val dst = RectF(dstLeft, dstTop, dstLeft + base.width, dstTop + base.height)

            if (mirror) {
                val matrix = Matrix()
                matrix.postScale(-1f, 1f, base.width / 2f, base.height / 2f)
                val mirrored = Bitmap.createBitmap(base, 0, 0, base.width, base.height, matrix, true)
                canvas.drawBitmap(mirrored, null, dst, paint)
            } else {
                canvas.drawBitmap(base, null, dst, paint)
            }
        }

        val offsetsY = listOf(0f, -1.5f, 0f, 1.5f)
        for (frame in 0 until frames) {
            val offsetY = offsetsY[frame % offsetsY.size]
            drawFrame(frame, 2, 0f, offsetY, mirror = false) // down
            drawFrame(frame, 0, 0f, offsetY, mirror = false) // up
            drawFrame(frame, 1, 0.5f, offsetY, mirror = false) // right
            drawFrame(frame, 3, -0.5f, offsetY, mirror = true) // left
        }

        return bitmap to tileSize
    }

    private fun buildTileSourceMap(tileSize: Int): Map<StoryMap.TileType, Rect> {
        val orderedTypes = listOf(
            StoryMap.TileType.DIRT,
            StoryMap.TileType.GRASS,
            StoryMap.TileType.TREE,
            StoryMap.TileType.ROCK
        )
        val map = HashMap<StoryMap.TileType, Rect>(orderedTypes.size)
        orderedTypes.forEachIndexed { index, type ->
            val col = index % tilesetColumns
            val row = index / tilesetColumns
            map[type] = Rect(
                col * tileSize,
                row * tileSize,
                (col + 1) * tileSize,
                (row + 1) * tileSize
            )
        }
        return map
    }

    private fun buildCityBuildingBitmaps(): List<Bitmap> {
        val bitmaps = ArrayList<Bitmap>(10)
        for (i in 1..10) {
            loadBitmapByName("casa$i")?.let { bitmaps.add(it) }
        }
        return bitmaps
    }

    private fun buildForestTileset(tileSize: Int): Bitmap? {
        if (tileSize <= 0) return null
        val rows = 1
        val bitmap = Bitmap.createBitmap(tileSize * tilesetColumns, tileSize * rows, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint().apply { isAntiAlias = false }

        fun tileRect(index: Int): Rect {
            val col = index % tilesetColumns
            val row = index / tilesetColumns
            return Rect(col * tileSize, row * tileSize, (col + 1) * tileSize, (row + 1) * tileSize)
        }

        // DIRT
        run {
            val rect = tileRect(0)
            paint.color = Color.parseColor("#C6A26E")
            canvas.drawRect(rect, paint)
            paint.color = Color.parseColor("#B28C5C")
            for (i in 0 until 20) {
                val x = rect.left + (i * 7 % tileSize)
                val y = rect.top + (i * 13 % tileSize)
                canvas.drawRect(x.toFloat(), y.toFloat(), (x + 2).toFloat(), (y + 2).toFloat(), paint)
            }
        }

        // GRASS
        run {
            val rect = tileRect(1)
            paint.color = Color.parseColor("#5FAE68")
            canvas.drawRect(rect, paint)
            paint.color = Color.parseColor("#4A9A55")
            for (i in 0 until 24) {
                val x = rect.left + (i * 5 % tileSize)
                val y = rect.top + (i * 11 % tileSize)
                canvas.drawRect(x.toFloat(), y.toFloat(), (x + 1).toFloat(), (y + 3).toFloat(), paint)
            }
        }

        // TREE
        run {
            val rect = tileRect(2)
            paint.color = Color.parseColor("#2E6A35")
            canvas.drawRect(rect, paint)
            paint.color = Color.parseColor("#244F2A")
            canvas.drawRect(
                rect.left + tileSize * 0.1f,
                rect.top + tileSize * 0.1f,
                rect.right - tileSize * 0.1f,
                rect.bottom - tileSize * 0.35f,
                paint
            )
            paint.color = Color.parseColor("#8B5A2B")
            canvas.drawRect(
                rect.left + tileSize * 0.42f,
                rect.top + tileSize * 0.55f,
                rect.right - tileSize * 0.42f,
                rect.bottom - tileSize * 0.1f,
                paint
            )
        }

        // ROCK
        run {
            val rect = tileRect(3)
            paint.color = Color.parseColor("#C6A26E")
            canvas.drawRect(rect, paint)
            paint.color = Color.parseColor("#8E9399")
            canvas.drawOval(
                rect.left + tileSize * 0.25f,
                rect.top + tileSize * 0.45f,
                rect.right - tileSize * 0.2f,
                rect.bottom - tileSize * 0.15f,
                paint
            )
            paint.color = Color.parseColor("#A7ADB3")
            canvas.drawOval(
                rect.left + tileSize * 0.35f,
                rect.top + tileSize * 0.55f,
                rect.right - tileSize * 0.35f,
                rect.bottom - tileSize * 0.25f,
                paint
            )
        }

        return bitmap
    }

    private fun buildCityTileset(tileSize: Int): Bitmap? {
        if (tileSize <= 0) return null
        val rows = 1
        val bitmap = Bitmap.createBitmap(tileSize * tilesetColumns, tileSize * rows, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint().apply { isAntiAlias = false }

        fun tileRect(index: Int): Rect {
            val col = index % tilesetColumns
            val row = index / tilesetColumns
            return Rect(col * tileSize, row * tileSize, (col + 1) * tileSize, (row + 1) * tileSize)
        }

        // DIRT -> road
        run {
            val rect = tileRect(0)
            paint.color = Color.parseColor("#70757A")
            canvas.drawRect(rect, paint)
            paint.color = Color.parseColor("#5D6369")
            for (i in 0 until 22) {
                val x = rect.left + (i * 5 % tileSize)
                val y = rect.top + (i * 9 % tileSize)
                canvas.drawRect(x.toFloat(), y.toFloat(), (x + 2).toFloat(), (y + 1).toFloat(), paint)
            }
        }

        // GRASS -> pavement
        run {
            val rect = tileRect(1)
            paint.color = Color.parseColor("#C9CED3")
            canvas.drawRect(rect, paint)
            paint.color = Color.parseColor("#B6BCC2")
            for (i in 0 until 16) {
                val x = rect.left + (i * 7 % tileSize)
                canvas.drawLine(x.toFloat(), rect.top.toFloat(), x.toFloat(), rect.bottom.toFloat(), paint)
            }
        }

        // TREE -> tall building
        run {
            val rect = tileRect(2)
            paint.color = Color.parseColor("#8FA0B3")
            canvas.drawRect(rect, paint)
            paint.color = Color.parseColor("#73869A")
            canvas.drawRect(
                rect.left + tileSize * 0.08f,
                rect.top + tileSize * 0.08f,
                rect.right - tileSize * 0.08f,
                rect.bottom - tileSize * 0.12f,
                paint
            )
            paint.color = Color.parseColor("#EAF2FF")
            for (r in 0 until 3) {
                for (c in 0 until 3) {
                    val wx = rect.left + tileSize * (0.18f + c * 0.22f)
                    val wy = rect.top + tileSize * (0.18f + r * 0.2f)
                    canvas.drawRect(wx, wy, wx + tileSize * 0.08f, wy + tileSize * 0.08f, paint)
                }
            }
        }

        // ROCK -> house
        run {
            val rect = tileRect(3)
            paint.color = Color.parseColor("#D9B48A")
            canvas.drawRect(rect, paint)
            paint.color = Color.parseColor("#A55F4A")
            canvas.drawRect(
                rect.left + tileSize * 0.12f,
                rect.top + tileSize * 0.12f,
                rect.right - tileSize * 0.12f,
                rect.bottom - tileSize * 0.12f,
                paint
            )
            paint.color = Color.parseColor("#7C3E30")
            canvas.drawRect(
                rect.left + tileSize * 0.18f,
                rect.top + tileSize * 0.16f,
                rect.right - tileSize * 0.18f,
                rect.top + tileSize * 0.28f,
                paint
            )
            paint.color = Color.parseColor("#F8E3C8")
            canvas.drawRect(
                rect.left + tileSize * 0.44f,
                rect.top + tileSize * 0.56f,
                rect.right - tileSize * 0.44f,
                rect.bottom - tileSize * 0.12f,
                paint
            )
        }

        return bitmap
    }

    private fun buildTrophyBitmap(sizePx: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val gold = Color.parseColor("#F2C94C")
        val darkGold = Color.parseColor("#C9A33A")
        val base = Color.parseColor("#8C6B2A")

        paint.color = gold
        canvas.drawRect(
            sizePx * 0.28f,
            sizePx * 0.2f,
            sizePx * 0.72f,
            sizePx * 0.55f,
            paint
        )
        canvas.drawOval(
            sizePx * 0.18f,
            sizePx * 0.25f,
            sizePx * 0.35f,
            sizePx * 0.45f,
            paint
        )
        canvas.drawOval(
            sizePx * 0.65f,
            sizePx * 0.25f,
            sizePx * 0.82f,
            sizePx * 0.45f,
            paint
        )

        paint.color = darkGold
        canvas.drawRect(
            sizePx * 0.45f,
            sizePx * 0.55f,
            sizePx * 0.55f,
            sizePx * 0.7f,
            paint
        )

        paint.color = base
        canvas.drawRect(
            sizePx * 0.3f,
            sizePx * 0.72f,
            sizePx * 0.7f,
            sizePx * 0.85f,
            paint
        )

        return bitmap
    }

    private fun drawTrophy(canvas: Canvas, rect: RectF) {
        val size = min(rect.width(), rect.height()) * 0.75f
        val left = rect.centerX() - size / 2f
        val top = rect.centerY() - size / 2f
        val dst = RectF(left, top, left + size, top + size)
        canvas.drawBitmap(trophyBitmap, null, dst, null)
    }

    private fun drawSanti(canvas: Canvas, rect: RectF) {
        val size = min(rect.width(), rect.height()) * 0.88f
        val halfHeight = size / 2f
        val halfWidth = halfHeight * 0.78f
        val dst = RectF(
            rect.centerX() - halfWidth,
            rect.centerY() - halfHeight,
            rect.centerX() + halfWidth,
            rect.centerY() + halfHeight
        )
        val bitmap = santiBitmap
        if (bitmap != null) {
            canvas.drawBitmap(bitmap, null, dst, null)
            return
        }
        canvas.drawOval(dst, playerFallbackPaint)
    }

    private fun loadBitmapByName(name: String): Bitmap? {
        val id = resources.getIdentifier(name, "drawable", context.packageName)
        if (id == 0) return null
        return BitmapFactory.decodeResource(resources, id)
    }

    private fun loadFrameSequence(prefix: String): List<Bitmap> {
        val list = ArrayList<Bitmap>(playerFrameCount)
        for (i in 1..playerFrameCount) {
            val bmp = loadBitmapByName("${prefix}_$i") ?: return emptyList()
            list.add(bmp)
        }
        return list
    }

    private fun stylePlayerBitmap(bitmap: Bitmap): Bitmap {
        return bitmap
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
