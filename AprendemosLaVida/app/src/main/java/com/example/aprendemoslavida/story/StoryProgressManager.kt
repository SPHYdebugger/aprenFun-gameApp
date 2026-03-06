package com.example.aprendemoslavida.story

import android.graphics.RectF
import android.os.SystemClock

// Tracks gate state, pending questions and per-gate timer.
class StoryProgressManager(private val questionProvider: StoryQuestionProvider) {
    private val gateQuestions = HashMap<Int, StoryQuestion>()
    private val gateStartTimeMs = HashMap<Int, Long>()

    val gates: List<StoryGate> = listOf(
        StoryGate(
            id = 0,
            topic = StoryTopic.NATURAL,
            rect = RectF(6f, 1f, 7f, 2f)
        ),
        StoryGate(
            id = 1,
            topic = StoryTopic.MATH_MULTIPLICATION,
            rect = RectF(13f, 1f, 14f, 2f)
        ),
        StoryGate(
            id = 2,
            topic = StoryTopic.MATH_ADD_SUB,
            rect = RectF(18f, 3f, 19f, 4f)
        ),
        StoryGate(
            id = 3,
            topic = StoryTopic.ENGLISH,
            rect = RectF(8f, 4f, 9f, 5f)
        ),
        StoryGate(
            id = 4,
            topic = StoryTopic.SOCIAL,
            rect = RectF(4f, 7f, 5f, 8f)
        ),
        StoryGate(
            id = 5,
            topic = StoryTopic.NATURAL,
            rect = RectF(9f, 6f, 10f, 7f),
            optional = true
        ),
        StoryGate(
            id = 6,
            topic = StoryTopic.MATH_ADD_SUB,
            rect = RectF(16f, 4f, 17f, 5f)
        ),
        StoryGate(
            id = 7,
            topic = StoryTopic.ENGLISH,
            rect = RectF(10f, 8f, 11f, 9f)
        ),
        StoryGate(
            id = 8,
            topic = StoryTopic.SOCIAL,
            rect = RectF(7f, 11f, 8f, 12f)
        ),
        StoryGate(
            id = 9,
            topic = StoryTopic.NATURAL,
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
