package com.example.aprendemoslavida

import com.example.aprendemoslavida.data.model.StoryGate
import com.example.aprendemoslavida.data.model.StoryMap
import com.example.aprendemoslavida.story.StoryServerBridge
import com.example.aprendemoslavida.story.StoryTopic
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** F-15: the local map follows the server map (gate position == local gate id). */
class StoryServerBridgeTest {

    private val topics = listOf(
        "NATURAL", "MATH_MULTIPLICATION", "ENGLISH", "SOCIAL", "LANGUAGE",
        "CASTLES", "MATH_ADD_SUB", "ENGLISH", "SOCIAL", "CASTLES"
    )

    private fun serverMap(resolved: Set<Int> = emptySet()) = StoryMap(
        id = "map-1", position = 0, visualTheme = "CITY_CLASSIC", layoutSeed = 9024255451911496106,
        status = "CURRENT", resolvedGates = resolved.size, minimumGatesToExit = 5,
        // Shuffled on purpose: the server does not guarantee order.
        gates = topics.indices.reversed().map { i ->
            StoryGate("gate-$i", i, topics[i], optional = i >= 8, status = if (i in resolved) "RESOLVED" else "AVAILABLE", points = 0)
        }
    )

    @Test
    fun topicsFollowServerGatePositions() {
        val result = StoryServerBridge.gateTopics(serverMap())
        assertEquals(10, result.size)
        assertEquals(StoryTopic.NATURAL, result[0])
        assertEquals(StoryTopic.MATH_ADD_SUB, result[6])
        assertEquals(StoryTopic.CASTLES, result[9])
    }

    @Test
    fun localGateMapsToServerGateByPosition() {
        assertEquals("gate-3", StoryServerBridge.serverGate(serverMap(), 3)?.id)
        assertNull(StoryServerBridge.serverGate(serverMap(), 42))
    }

    @Test
    fun exitNeedsMinimumResolvedGates() {
        assertFalse(StoryServerBridge.canExit(serverMap(setOf(0, 1, 2, 3))))
        assertTrue(StoryServerBridge.canExit(serverMap(setOf(0, 1, 2, 3, 9))))
        assertTrue(StoryServerBridge.isResolved(StoryServerBridge.serverGate(serverMap(setOf(9)), 9)))
    }
}
