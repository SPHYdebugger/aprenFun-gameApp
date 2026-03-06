package com.example.aprendemoslavida.story

import android.graphics.RectF
import kotlin.math.floor
import kotlin.random.Random

// Tile map used by the story mode. Tiles not on the path are solid walls.
class StoryMap(
    val width: Int,
    val height: Int,
    private val walkableTiles: Set<Pair<Int, Int>>,
    private val tileTypes: Array<TileType>,
    private val hiddenZoneIndex: IntArray,
    private val hiddenZones: List<RectF>,
    private val hiddenZoneEntrances: Set<Pair<Int, Int>>,
    private val secretEntrances: List<Pair<Int, Int>>,
    val startTileX: Float,
    val startTileY: Float,
    val exitRect: RectF
) {
    private var chosenSecretEntrance: Pair<Int, Int>? = secretEntrances.firstOrNull()

    enum class TileType {
        DIRT,
        GRASS,
        TREE,
        ROCK
    }

    fun isWalkable(tileX: Int, tileY: Int): Boolean {
        if (tileX < 0 || tileY < 0 || tileX >= width || tileY >= height) return false
        if (secretEntrances.isNotEmpty()) {
            val entrance = secretEntrances.firstOrNull { it.first == tileX && it.second == tileY }
            if (entrance != null) {
                return entrance == chosenSecretEntrance
            }
        }
        return walkableTiles.contains(tileX to tileY)
    }

    fun tileTypeAt(tileX: Int, tileY: Int): TileType {
        if (tileX < 0 || tileY < 0 || tileX >= width || tileY >= height) return TileType.TREE
        return tileTypes[(tileY * width) + tileX]
    }

    fun collidesWithWall(rect: RectF, activeHiddenZone: Int): Boolean {
        val minX = floor(rect.left).toInt()
        val maxX = floor(rect.right - 0.001f).toInt()
        val minY = floor(rect.top).toInt()
        val maxY = floor(rect.bottom - 0.001f).toInt()

        for (y in minY..maxY) {
            for (x in minX..maxX) {
                if (!isWalkableForZone(x, y, activeHiddenZone)) return true
            }
        }
        return false
    }

    private fun isWalkableForZone(tileX: Int, tileY: Int, activeHiddenZone: Int): Boolean {
        if (!isWalkable(tileX, tileY)) return false
        if (hiddenZoneEntrances.contains(tileX to tileY)) return true
        val zone = hiddenZoneAtTile(tileX, tileY)
        // Hidden zones should be reachable from outside; rendering still keeps them hidden
        // until the player actually enters.
        if (zone >= 0 && activeHiddenZone >= 0 && zone != activeHiddenZone) return false
        return true
    }

    fun hiddenZoneAtTile(tileX: Int, tileY: Int): Int {
        if (tileX < 0 || tileY < 0 || tileX >= width || tileY >= height) return -1
        return hiddenZoneIndex[(tileY * width) + tileX]
    }

    fun hiddenZoneAtPoint(worldX: Float, worldY: Float): Int {
        val tileZone = hiddenZoneAtTile(floor(worldX).toInt(), floor(worldY).toInt())
        if (tileZone >= 0) return tileZone
        hiddenZones.forEachIndexed { index, rect ->
            if (rect.contains(worldX, worldY)) return index
        }
        return -1
    }

    fun randomizeSecretEntrance() {
        if (secretEntrances.isEmpty()) return
        if (secretEntrances.size == 1) {
            chosenSecretEntrance = secretEntrances.first()
            return
        }
        var next = secretEntrances[Random.nextInt(secretEntrances.size)]
        if (next == chosenSecretEntrance) {
            next = secretEntrances[(secretEntrances.indexOf(next) + 1) % secretEntrances.size]
        }
        chosenSecretEntrance = next
    }

    companion object {
        fun createDefault(): StoryMap {
            val width = 22
            val height = 16
            val path = LinkedHashSet<Pair<Int, Int>>()
            val tiles = Array(width * height) { TileType.GRASS }
            val hiddenZoneIndex = IntArray(width * height) { -1 }
            val hiddenZones = ArrayList<RectF>()
            val hiddenZoneEntrances = LinkedHashSet<Pair<Int, Int>>()
            val secretEntrances = ArrayList<Pair<Int, Int>>()

            // Main path segments (single-route style).
            for (x in 1..18) path.add(x to 1)
            for (y in 1..4) path.add(18 to y)
            for (x in 4..18) path.add(x to 4)
            for (y in 4..8) path.add(4 to y)
            for (x in 4..15) path.add(x to 8)
            for (y in 8..11) path.add(15 to y)
            for (x in 2..15) path.add(x to 11)

            fun index(x: Int, y: Int) = (y * width) + x
            fun setTile(x: Int, y: Int, type: TileType) {
                if (x < 0 || y < 0 || x >= width || y >= height) return
                tiles[index(x, y)] = type
            }

            fun markHiddenZone(zoneIndex: Int, tilesInZone: List<Pair<Int, Int>>) {
                tilesInZone.forEach { (x, y) ->
                    if (x < 0 || y < 0 || x >= width || y >= height) return@forEach
                    hiddenZoneIndex[index(x, y)] = zoneIndex
                }
            }

            // Base forest ground.
            for (y in 0 until height) {
                for (x in 0 until width) {
                    setTile(x, y, TileType.GRASS)
                }
            }

            // Dirt path.
            path.forEach { (x, y) -> setTile(x, y, TileType.DIRT) }

            // Trees lining the path.
            path.forEach { (x, y) ->
                listOf(
                    x - 1 to y,
                    x + 1 to y,
                    x to y - 1,
                    x to y + 1
                ).forEach { (tx, ty) ->
                    if (!path.contains(tx to ty)) {
                        setTile(tx, ty, TileType.TREE)
                    }
                }
            }

            // Scatter a few rocks on the path (still walkable).
            listOf(6 to 1, 9 to 4, 12 to 8).forEach { (x, y) ->
                if (path.contains(x to y)) setTile(x, y, TileType.ROCK)
            }

            // Secret passage between zones.
            val secretPath = listOf(6 to 6, 7 to 6, 8 to 6, 9 to 6, 10 to 6)
            secretPath.forEach { (x, y) ->
                path.add(x to y)
                setTile(x, y, TileType.DIRT)
            }

            // Hidden entrance: looks like a tree but is walkable (randomized).
            val secretEntranceCandidates = listOf(5 to 6, 6 to 5, 10 to 5)
            secretEntranceCandidates.forEach { (x, y) ->
                setTile(x, y, TileType.TREE)
                secretEntrances.add(x to y)
            }

            // Hidden zone in the lower-right block. It stays disguised as trees until entered.
            val hiddenZoneTiles = ArrayList<Pair<Int, Int>>()
            for (y in 13..14) {
                for (x in 10..17) {
                    hiddenZoneTiles.add(x to y)
                    path.add(x to y)
                    setTile(x, y, TileType.DIRT)
                }
            }
            val hiddenZoneRect = RectF(10f, 13f, 18f, 15f)
            hiddenZones.add(hiddenZoneRect)
            markHiddenZone(0, hiddenZoneTiles)

            // Hidden access from the lower final dirt segment, camouflaged as normal trees.
            val hiddenAccessPath = listOf(16 to 11, 16 to 12)
            hiddenAccessPath.forEach { (x, y) ->
                path.add(x to y)
                setTile(x, y, TileType.TREE)
                hiddenZoneEntrances.add(x to y)
                hiddenZoneIndex[index(x, y)] = -1
            }

            return StoryMap(
                width = width,
                height = height,
                walkableTiles = path,
                tileTypes = tiles,
                hiddenZoneIndex = hiddenZoneIndex,
                hiddenZones = hiddenZones,
                hiddenZoneEntrances = hiddenZoneEntrances,
                secretEntrances = secretEntrances,
                startTileX = 1.5f,
                startTileY = 1.5f,
                exitRect = RectF(2f, 11f, 3f, 12f)
            ).apply { randomizeSecretEntrance() }
        }
    }
}
