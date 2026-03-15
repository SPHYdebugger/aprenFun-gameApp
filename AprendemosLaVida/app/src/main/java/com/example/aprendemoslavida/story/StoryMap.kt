package com.example.aprendemoslavida.story

import android.graphics.RectF
import kotlin.math.floor
import kotlin.random.Random

// Tile map used by the story mode. Tiles not on the path are solid walls.
class StoryMap(
    val width: Int,
    val height: Int,
    val visualTheme: VisualTheme,
    private val walkableTiles: Set<Pair<Int, Int>>,
    private val tileTypes: Array<TileType>,
    private val hiddenZoneIndex: IntArray,
    private val hiddenZones: List<RectF>,
    private val hiddenZoneEntrances: Set<Pair<Int, Int>>,
    private val secretEntrances: List<Pair<Int, Int>>,
    val trophyMainCandidates: List<Pair<Int, Int>>,
    val trophySecretCandidates: List<Pair<Int, Int>>,
    val trophyHiddenCandidates: List<Pair<Int, Int>>,
    val startTileX: Float,
    val startTileY: Float,
    val exitRect: RectF
) {
    enum class VisualTheme {
        FOREST,
        CITY_CLASSIC,
        CITY_TILESET
    }

    val isCityTheme: Boolean
        get() = visualTheme != VisualTheme.FOREST

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

    fun isHiddenZoneEntrance(tileX: Int, tileY: Int): Boolean {
        return hiddenZoneEntrances.contains(tileX to tileY)
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

            val hiddenZoneSet = hiddenZoneTiles.toSet()
            val secretPathSet = secretPath.toSet()
            val hiddenAccessSet = hiddenAccessPath.toSet()
            val startTile = floor(1.5f).toInt() to floor(1.5f).toInt()
            val exitTile = floor(2f).toInt() to floor(11f).toInt()
            val mainCandidates = path.filter { tile ->
                tile != startTile &&
                    tile != exitTile &&
                    !secretPathSet.contains(tile) &&
                    !hiddenZoneSet.contains(tile) &&
                    !hiddenAccessSet.contains(tile)
            }

            return StoryMap(
                width = width,
                height = height,
                visualTheme = VisualTheme.FOREST,
                walkableTiles = path,
                tileTypes = tiles,
                hiddenZoneIndex = hiddenZoneIndex,
                hiddenZones = hiddenZones,
                hiddenZoneEntrances = hiddenZoneEntrances,
                secretEntrances = secretEntrances,
                trophyMainCandidates = mainCandidates,
                trophySecretCandidates = secretPath,
                trophyHiddenCandidates = hiddenZoneTiles,
                startTileX = 1.5f,
                startTileY = 1.5f,
                exitRect = RectF(2f, 11f, 3f, 12f)
            ).apply { randomizeSecretEntrance() }
        }

        // New map variants (not active yet). They keep the same mechanics:
        // - random secret entrance each match
        // - hidden passable-tree corridor to a hidden zone that reveals when entered
        fun createVariant1(): StoryMap {
            val mainPath = linkedSetOf<Pair<Int, Int>>().apply {
                addHorizontal(1, 1, 19)
                addVertical(19, 1, 5)
                addHorizontal(3, 5, 19)
                addVertical(3, 5, 9)
                addHorizontal(3, 9, 16)
                addVertical(16, 9, 12)
                addHorizontal(2, 12, 16)
            }
            val secretPath = listOf(7 to 7, 8 to 7, 9 to 7, 10 to 7)
            val secretEntrances = listOf(6 to 7, 7 to 6, 10 to 6)
            val hiddenZoneTiles = rectTiles(11, 14, 18, 15)
            val hiddenAccessPath = listOf(15 to 13, 15 to 14)
            val rocks = listOf(6 to 1, 11 to 5, 9 to 9)

            return buildVariantMap(
                basePath = mainPath,
                rocks = rocks,
                secretPath = secretPath,
                secretEntrances = secretEntrances,
                hiddenZoneTiles = hiddenZoneTiles,
                hiddenZoneRect = RectF(11f, 14f, 19f, 16f),
                hiddenAccessPath = hiddenAccessPath,
                startTileX = 1.5f,
                startTileY = 1.5f,
                exitRect = RectF(2f, 12f, 3f, 13f)
            )
        }

        fun createVariant2(): StoryMap {
            val mainPath = linkedSetOf<Pair<Int, Int>>().apply {
                addVertical(2, 1, 12)
                addHorizontal(2, 12, 18)
                addVertical(18, 3, 12)
                addHorizontal(5, 4, 18)
                addVertical(4, 5, 9)
                addHorizontal(4, 9, 15)
                addVertical(15, 9, 13)
            }
            val secretPath = listOf(9 to 4, 9 to 5, 9 to 6, 10 to 6)
            val secretEntrances = listOf(8 to 5, 10 to 5, 10 to 6)
            val hiddenZoneTiles = rectTiles(16, 13, 20, 15)
            val hiddenAccessPath = listOf(16 to 13, 17 to 13)
            val rocks = listOf(2 to 7, 12 to 12, 13 to 9)

            return buildVariantMap(
                basePath = mainPath,
                rocks = rocks,
                secretPath = secretPath,
                secretEntrances = secretEntrances,
                hiddenZoneTiles = hiddenZoneTiles,
                hiddenZoneRect = RectF(16f, 13f, 21f, 16f),
                hiddenAccessPath = hiddenAccessPath,
                startTileX = 2.5f,
                startTileY = 1.5f,
                exitRect = RectF(15f, 13f, 16f, 14f)
            )
        }

        // Non-linear map with multiple routes to the exit.
        fun createVariant3(): StoryMap {
            val mainPath = linkedSetOf<Pair<Int, Int>>().apply {
                // Maze-like layout with intersections and multiple ways to approach the end.
                addHorizontal(1, 1, 19)
                addHorizontal(1, 5, 17)
                addHorizontal(3, 9, 19)
                addHorizontal(2, 13, 20)

                addVertical(3, 1, 13)
                addVertical(7, 1, 9)
                addVertical(11, 5, 13)
                addVertical(15, 1, 13)
                addVertical(18, 9, 13)

                // Dead-end where the exit is placed.
                addVertical(20, 12, 13)
            }
            val secretPath = listOf(8 to 6, 9 to 6, 10 to 6, 11 to 6)
            val secretEntrances = listOf(7 to 6, 8 to 5, 11 to 5)
            val hiddenZoneTiles = rectTiles(4, 14, 8, 15)
            val hiddenAccessPath = listOf(8 to 13, 8 to 14)
            val rocks = listOf(5 to 1, 12 to 9, 16 to 5, 19 to 13)

            return buildVariantMap(
                basePath = mainPath,
                rocks = rocks,
                secretPath = secretPath,
                secretEntrances = secretEntrances,
                hiddenZoneTiles = hiddenZoneTiles,
                hiddenZoneRect = RectF(4f, 14f, 9f, 16f),
                hiddenAccessPath = hiddenAccessPath,
                startTileX = 1.5f,
                startTileY = 1.5f,
                exitRect = RectF(20f, 13f, 21f, 14f)
            )
        }

        fun createVariant4(): StoryMap {
            val mainPath = linkedSetOf<Pair<Int, Int>>().apply {
                // Top corridor to the right side.
                addHorizontal(1, 1, 19)
                addVertical(19, 1, 7)

                // Main middle corridor where exit is placed (left side).
                addHorizontal(4, 7, 19)

                // Right-side loop/bulge.
                addVertical(19, 7, 11)
                addHorizontal(17, 8, 19)
                addVertical(17, 8, 11)
                addHorizontal(17, 11, 19)

                // Lower loop and bottom return.
                addVertical(6, 7, 14)
                addHorizontal(6, 11, 17)
                addVertical(16, 11, 14)
                addHorizontal(6, 14, 16)
            }
            val secretPath = buildList {
                for (x in 8..15) add(x to 3)
                for (y in 3..5) add(11 to y)
                for (x in 10..12) add(x to 5)
            }
            val secretEntrances = listOf(10 to 6, 11 to 6, 12 to 6)
            val hiddenZoneTiles = rectTiles(1, 12, 5, 14)
            val hiddenAccessPath = listOf(6 to 13, 6 to 14)
            val rocks = listOf(8 to 1, 14 to 7, 11 to 11)

            return buildVariantMap(
                basePath = mainPath,
                rocks = rocks,
                secretPath = secretPath,
                secretEntrances = secretEntrances,
                hiddenZoneTiles = hiddenZoneTiles,
                hiddenZoneRect = RectF(1f, 12f, 6f, 15f),
                hiddenAccessPath = hiddenAccessPath,
                startTileX = 1.5f,
                startTileY = 1.5f,
                exitRect = RectF(4f, 7f, 5f, 8f)
            )
        }

        fun createAllVariants(): List<StoryMap> {
            return listOf(
                createVariant1(),
                createVariant2(),
                createVariant3(),
                createVariant4()
            )
        }

        fun createAllCityVariants(): List<StoryMap> {
            val baseMaps = listOf(createDefault()) + createAllVariants()
            return baseMaps.map(::copyAsTilesetCityMap)
        }

        fun createAllClassicCityVariants(): List<StoryMap> {
            val baseMaps = listOf(createDefault()) + createAllVariants()
            return baseMaps.map(::copyAsClassicCityMap)
        }

        private fun buildVariantMap(
            basePath: Set<Pair<Int, Int>>,
            rocks: List<Pair<Int, Int>>,
            secretPath: List<Pair<Int, Int>>,
            secretEntrances: List<Pair<Int, Int>>,
            hiddenZoneTiles: List<Pair<Int, Int>>,
            hiddenZoneRect: RectF,
            hiddenAccessPath: List<Pair<Int, Int>>,
            startTileX: Float,
            startTileY: Float,
            exitRect: RectF
        ): StoryMap {
            val width = 22
            val height = 16
            val path = LinkedHashSet<Pair<Int, Int>>()
            val tiles = Array(width * height) { TileType.GRASS }
            val hiddenZoneIndex = IntArray(width * height) { -1 }
            val hiddenZones = arrayListOf(hiddenZoneRect)
            val hiddenZoneEntrances = LinkedHashSet<Pair<Int, Int>>()
            val randomSecretEntrances = ArrayList<Pair<Int, Int>>()

            fun idx(x: Int, y: Int) = (y * width) + x
            fun setTile(x: Int, y: Int, type: TileType) {
                if (x < 0 || y < 0 || x >= width || y >= height) return
                tiles[idx(x, y)] = type
            }

            path.addAll(basePath)
            path.addAll(secretPath)
            path.addAll(hiddenZoneTiles)
            path.addAll(hiddenAccessPath)

            for (y in 0 until height) {
                for (x in 0 until width) {
                    setTile(x, y, TileType.GRASS)
                }
            }

            path.forEach { (x, y) -> setTile(x, y, TileType.DIRT) }

            path.forEach { (x, y) ->
                listOf(x - 1 to y, x + 1 to y, x to y - 1, x to y + 1).forEach { (tx, ty) ->
                    if (!path.contains(tx to ty)) {
                        setTile(tx, ty, TileType.TREE)
                    }
                }
            }

            rocks.forEach { (x, y) ->
                if (path.contains(x to y)) setTile(x, y, TileType.ROCK)
            }

            secretEntrances.forEach { (x, y) ->
                setTile(x, y, TileType.TREE)
                randomSecretEntrances.add(x to y)
            }

            hiddenZoneTiles.forEach { (x, y) ->
                hiddenZoneIndex[idx(x, y)] = 0
                setTile(x, y, TileType.DIRT)
            }

            hiddenAccessPath.forEach { (x, y) ->
                setTile(x, y, TileType.TREE)
                hiddenZoneEntrances.add(x to y)
                hiddenZoneIndex[idx(x, y)] = -1
            }

            val hiddenZoneSet = hiddenZoneTiles.toSet()
            val secretPathSet = secretPath.toSet()
            val hiddenAccessSet = hiddenAccessPath.toSet()
            val startTile = floor(startTileX).toInt() to floor(startTileY).toInt()
            val exitTile = floor(exitRect.left).toInt() to floor(exitRect.top).toInt()
            val mainCandidates = path.filter { tile ->
                tile != startTile &&
                    tile != exitTile &&
                    !secretPathSet.contains(tile) &&
                    !hiddenZoneSet.contains(tile) &&
                    !hiddenAccessSet.contains(tile)
            }

            return StoryMap(
                width = width,
                height = height,
                visualTheme = VisualTheme.FOREST,
                walkableTiles = path,
                tileTypes = tiles,
                hiddenZoneIndex = hiddenZoneIndex,
                hiddenZones = hiddenZones,
                hiddenZoneEntrances = hiddenZoneEntrances,
                secretEntrances = randomSecretEntrances,
                trophyMainCandidates = mainCandidates,
                trophySecretCandidates = secretPath,
                trophyHiddenCandidates = hiddenZoneTiles,
                startTileX = startTileX,
                startTileY = startTileY,
                exitRect = exitRect
            ).apply { randomizeSecretEntrance() }
        }

        private fun MutableSet<Pair<Int, Int>>.addHorizontal(xStart: Int, y: Int, xEnd: Int) {
            val from = minOf(xStart, xEnd)
            val to = maxOf(xStart, xEnd)
            for (x in from..to) add(x to y)
        }

        private fun MutableSet<Pair<Int, Int>>.addVertical(x: Int, yStart: Int, yEnd: Int) {
            val from = minOf(yStart, yEnd)
            val to = maxOf(yStart, yEnd)
            for (y in from..to) add(x to y)
        }

        private fun rectTiles(xFrom: Int, yFrom: Int, xTo: Int, yTo: Int): List<Pair<Int, Int>> {
            val list = ArrayList<Pair<Int, Int>>()
            for (y in yFrom..yTo) {
                for (x in xFrom..xTo) {
                    list.add(x to y)
                }
            }
            return list
        }

        private fun copyAsClassicCityMap(source: StoryMap): StoryMap {
            val cityTiles = Array(source.width * source.height) { index ->
                val x = index % source.width
                val y = index / source.width
                val tile = source.tileTypes[index]
                when {
                    source.walkableTiles.contains(x to y) -> TileType.DIRT
                    tile == TileType.TREE -> if (Random.nextBoolean()) TileType.TREE else TileType.ROCK
                    else -> tile
                }
            }
            return StoryMap(
                width = source.width,
                height = source.height,
                visualTheme = VisualTheme.CITY_CLASSIC,
                walkableTiles = source.walkableTiles.toSet(),
                tileTypes = cityTiles,
                hiddenZoneIndex = source.hiddenZoneIndex.copyOf(),
                hiddenZones = source.hiddenZones.map { RectF(it) },
                hiddenZoneEntrances = source.hiddenZoneEntrances.toSet(),
                secretEntrances = source.secretEntrances.toList(),
                trophyMainCandidates = source.trophyMainCandidates.toList(),
                trophySecretCandidates = source.trophySecretCandidates.toList(),
                trophyHiddenCandidates = source.trophyHiddenCandidates.toList(),
                startTileX = source.startTileX,
                startTileY = source.startTileY,
                exitRect = RectF(source.exitRect)
            ).apply { randomizeSecretEntrance() }
        }

        private fun copyAsTilesetCityMap(source: StoryMap): StoryMap {
            val cityTiles = Array(source.width * source.height) { index ->
                val x = index % source.width
                val y = index / source.width
                val tile = source.tileTypes[index]
                when {
                    // Keep roads clean and walkable.
                    source.walkableTiles.contains(x to y) -> TileType.DIRT
                    // Replace tree walls with 2 urban obstacle styles (buildings/houses).
                    tile == TileType.TREE -> if (Random.nextBoolean()) TileType.TREE else TileType.ROCK
                    else -> tile
                }
            }
            return StoryMap(
                width = source.width,
                height = source.height,
                visualTheme = VisualTheme.CITY_TILESET,
                walkableTiles = source.walkableTiles.toSet(),
                tileTypes = cityTiles,
                hiddenZoneIndex = source.hiddenZoneIndex.copyOf(),
                hiddenZones = source.hiddenZones.map { RectF(it) },
                hiddenZoneEntrances = source.hiddenZoneEntrances.toSet(),
                secretEntrances = source.secretEntrances.toList(),
                trophyMainCandidates = source.trophyMainCandidates.toList(),
                trophySecretCandidates = source.trophySecretCandidates.toList(),
                trophyHiddenCandidates = source.trophyHiddenCandidates.toList(),
                startTileX = source.startTileX,
                startTileY = source.startTileY,
                exitRect = RectF(source.exitRect)
            ).apply { randomizeSecretEntrance() }
        }
    }
}
