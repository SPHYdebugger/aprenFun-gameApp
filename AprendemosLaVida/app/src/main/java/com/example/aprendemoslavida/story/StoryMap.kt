package com.example.aprendemoslavida.story

import android.graphics.RectF
import kotlin.math.floor

// Tile map used by the story mode. Tiles not on the path are solid walls.
class StoryMap(
    val width: Int,
    val height: Int,
    private val walkableTiles: Set<Pair<Int, Int>>,
    val startTileX: Float,
    val startTileY: Float,
    val exitRect: RectF
) {
    fun isWalkable(tileX: Int, tileY: Int): Boolean {
        if (tileX < 0 || tileY < 0 || tileX >= width || tileY >= height) return false
        return walkableTiles.contains(tileX to tileY)
    }

    fun collidesWithWall(rect: RectF): Boolean {
        val minX = floor(rect.left).toInt()
        val maxX = floor(rect.right - 0.001f).toInt()
        val minY = floor(rect.top).toInt()
        val maxY = floor(rect.bottom - 0.001f).toInt()

        for (y in minY..maxY) {
            for (x in minX..maxX) {
                if (!isWalkable(x, y)) return true
            }
        }
        return false
    }

    companion object {
        fun createDefault(): StoryMap {
            val width = 20
            val height = 14
            val path = LinkedHashSet<Pair<Int, Int>>()

            // Main path segments (single-route style).
            for (x in 1..18) path.add(x to 1)
            for (y in 1..4) path.add(18 to y)
            for (x in 4..18) path.add(x to 4)
            for (y in 4..8) path.add(4 to y)
            for (x in 4..15) path.add(x to 8)
            for (y in 8..11) path.add(15 to y)
            for (x in 2..15) path.add(x to 11)

            return StoryMap(
                width = width,
                height = height,
                walkableTiles = path,
                startTileX = 1.5f,
                startTileY = 1.5f,
                exitRect = RectF(2f, 11f, 3f, 12f)
            )
        }
    }
}
