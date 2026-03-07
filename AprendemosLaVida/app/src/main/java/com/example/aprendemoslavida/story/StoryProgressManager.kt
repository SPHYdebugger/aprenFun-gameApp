package com.example.aprendemoslavida.story

import android.graphics.RectF
import android.os.SystemClock

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
    gateTopics: List<StoryTopic> = DEFAULT_GATE_TOPICS
) {
    private val gateQuestions = HashMap<Int, StoryQuestion>()
    private val gateStartTimeMs = HashMap<Int, Long>()
    private val topics = if (gateTopics.size == 10) gateTopics else DEFAULT_GATE_TOPICS

    val gates: List<StoryGate> = listOf(
        StoryGate(
            id = 0,
            topic = topics[0],
            rect = RectF(6f, 1f, 7f, 2f)
        ),
        StoryGate(
            id = 1,
            topic = topics[1],
            rect = RectF(13f, 1f, 14f, 2f)
        ),
        StoryGate(
            id = 2,
            topic = topics[2],
            rect = RectF(18f, 3f, 19f, 4f)
        ),
        StoryGate(
            id = 3,
            topic = topics[3],
            rect = RectF(8f, 4f, 9f, 5f)
        ),
        StoryGate(
            id = 4,
            topic = topics[4],
            rect = RectF(4f, 7f, 5f, 8f)
        ),
        StoryGate(
            id = 5,
            topic = topics[5],
            rect = RectF(9f, 6f, 10f, 7f),
            optional = true
        ),
        StoryGate(
            id = 6,
            topic = topics[6],
            rect = RectF(16f, 4f, 17f, 5f)
        ),
        StoryGate(
            id = 7,
            topic = topics[7],
            rect = RectF(10f, 8f, 11f, 9f)
        ),
        StoryGate(
            id = 8,
            topic = topics[8],
            rect = RectF(7f, 11f, 8f, 12f)
        ),
        StoryGate(
            id = 9,
            topic = topics[9],
            rect = RectF(15f, 13f, 16f, 14f),
            optional = true
        )
    )

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
}
