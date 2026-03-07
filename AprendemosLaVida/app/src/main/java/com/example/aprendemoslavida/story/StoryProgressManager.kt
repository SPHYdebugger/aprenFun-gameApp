package com.example.aprendemoslavida.story

import android.graphics.RectF
import android.os.SystemClock
import kotlin.random.Random

private val DEFAULT_GATE_TOPICS: List<StoryTopic> = listOf(
    StoryTopic.NATURAL,
    StoryTopic.MATH_MULTIPLICATION,
    StoryTopic.MATH_ADD_SUB,
    StoryTopic.ENGLISH,
    StoryTopic.SOCIAL,
    StoryTopic.NATURAL,
    StoryTopic.MATH_ADD_SUB,
    StoryTopic.ENGLISH,
    StoryTopic.SOCIAL,
    StoryTopic.NATURAL
)

// Tracks gate state, pending questions and per-gate timer.
class StoryProgressManager(
    private val questionProvider: StoryQuestionProvider,
    private val storyMap: StoryMap,
    gateTopics: List<StoryTopic> = DEFAULT_GATE_TOPICS
) {
    private val gateQuestions = HashMap<Int, StoryQuestion>()
    private val gateStartTimeMs = HashMap<Int, Long>()
    private val topics = if (gateTopics.size == 10) gateTopics else DEFAULT_GATE_TOPICS

    val gates: List<StoryGate> = buildRandomizedGates(topics)

    fun getGate(gateId: Int): StoryGate? = gates.firstOrNull { it.id == gateId }

    fun getOrCreateQuestion(gateId: Int): StoryQuestion {
        val gate = getGate(gateId) ?: error("Gate not found: $gateId")
        return gateQuestions.getOrPut(gateId) { questionProvider.nextQuestion(gate.topic) }
    }

    fun startTimerIfNeeded(gateId: Int) {
        if (!gateStartTimeMs.containsKey(gateId)) {
            gateStartTimeMs[gateId] = SystemClock.elapsedRealtime()
        }
    }

    fun elapsedForGate(gateId: Int): Long {
        val start = gateStartTimeMs[gateId] ?: SystemClock.elapsedRealtime()
        return SystemClock.elapsedRealtime() - start
    }

    fun unlockGate(gateId: Int) {
        getGate(gateId)?.unlocked = true
        gateQuestions.remove(gateId)
        gateStartTimeMs.remove(gateId)
    }

    fun requiredGateCount(): Int = gates.count { !it.optional }

    fun unlockedRequiredGates(): Int = gates.count { !it.optional && it.unlocked }

    fun requiredGatesUnlocked(): Boolean = gates.all { it.optional || it.unlocked }

    fun allGatesUnlocked(): Boolean = gates.all { it.unlocked }

    fun unlockedGateCount(): Int = gates.count { it.unlocked }

    private fun buildRandomizedGates(gateTopics: List<StoryTopic>): List<StoryGate> {
        val availableMain = storyMap.trophyMainCandidates
            .filter(::isValidTrophyTile)
            .shuffled()
            .toMutableList()
        val used = HashSet<Pair<Int, Int>>()

        val mainTiles = ArrayList<Pair<Int, Int>>(8)
        while (mainTiles.size < 8 && availableMain.isNotEmpty()) {
            val tile = availableMain.removeAt(0)
            if (used.add(tile)) {
                mainTiles.add(tile)
            }
        }
        while (mainTiles.size < 8) {
            val fallback = randomFallbackTile(used)
            used.add(fallback)
            mainTiles.add(fallback)
        }

        val secretTile = pickFromPoolOrFallback(storyMap.trophySecretCandidates.filter(::isValidTrophyTile), used)
        used.add(secretTile)
        val hiddenTile = pickFromPoolOrFallback(storyMap.trophyHiddenCandidates.filter(::isValidTrophyTile), used)
        used.add(hiddenTile)

        val finalTiles = mainTiles + listOf(secretTile, hiddenTile)
        return finalTiles.mapIndexed { index, (x, y) ->
            StoryGate(
                id = index,
                topic = gateTopics[index],
                rect = RectF(x.toFloat(), y.toFloat(), x + 1f, y + 1f),
                optional = index >= 8
            )
        }
    }

    private fun pickFromPoolOrFallback(
        pool: List<Pair<Int, Int>>,
        used: Set<Pair<Int, Int>>
    ): Pair<Int, Int> {
        val candidates = pool.filterNot { used.contains(it) }
        if (candidates.isNotEmpty()) return candidates.random()
        return randomFallbackTile(used)
    }

    private fun randomFallbackTile(used: Set<Pair<Int, Int>>): Pair<Int, Int> {
        val fallbackPool = (storyMap.trophyMainCandidates +
            storyMap.trophySecretCandidates +
            storyMap.trophyHiddenCandidates)
            .filter(::isValidTrophyTile)
        val available = fallbackPool.filterNot { used.contains(it) }
        if (available.isNotEmpty()) return available[Random.nextInt(available.size)]
        return 1 to 1
    }

    private fun isValidTrophyTile(tile: Pair<Int, Int>): Boolean {
        val (x, y) = tile
        if (!storyMap.isWalkable(x, y)) return false
        return storyMap.tileTypeAt(x, y) != StoryMap.TileType.TREE
    }
}
