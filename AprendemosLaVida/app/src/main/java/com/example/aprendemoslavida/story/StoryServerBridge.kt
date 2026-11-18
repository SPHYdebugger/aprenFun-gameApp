package com.example.aprendemoslavida.story

import com.example.aprendemoslavida.data.model.StorySession
import kotlin.random.Random
import com.example.aprendemoslavida.data.model.StoryGate as ServerGate
import com.example.aprendemoslavida.data.model.StoryMap as ServerMap

/**
 * F-15: links the server StorySession (authoritative: questions, points, time, progression)
 * to the local map renderer (StoryMap / StoryGameView). Local gate `id` == server gate `position`.
 */
object StoryServerBridge {

    /** Picks the local map variant for the server map: same visual theme, variant chosen by layoutSeed. */
    fun localMapIndex(maps: List<StoryMap>, serverMap: ServerMap): Int {
        val theme = StoryMap.VisualTheme.values().firstOrNull { it.name == serverMap.visualTheme }
        val candidates = maps.indices.filter { theme == null || maps[it].visualTheme == theme }
            .ifEmpty { maps.indices.toList() }
        return candidates[Random(serverMap.layoutSeed).nextInt(candidates.size)]
    }

    /** Topics for the 10 local gates, ordered by server gate position. */
    fun gateTopics(serverMap: ServerMap): List<StoryTopic> =
        serverMap.gates.sortedBy { it.position }.map { gate ->
            StoryTopic.values().firstOrNull { it.apiCode == gate.topic } ?: StoryTopic.NATURAL
        }

    fun serverGate(serverMap: ServerMap?, localGateId: Int): ServerGate? =
        serverMap?.gates?.firstOrNull { it.position == localGateId }

    fun isResolved(gate: ServerGate?): Boolean = gate?.status == "RESOLVED"

    fun canExit(serverMap: ServerMap?): Boolean =
        serverMap != null && serverMap.resolvedGates >= serverMap.minimumGatesToExit

    fun santiOnMap(session: StorySession): Boolean =
        session.santi?.let { it.availableOnCurrentMap && it.status == "AVAILABLE" } == true
}
