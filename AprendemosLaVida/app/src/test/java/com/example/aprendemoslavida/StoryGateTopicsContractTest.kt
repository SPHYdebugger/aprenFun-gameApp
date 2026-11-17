package com.example.aprendemoslavida

import com.example.aprendemoslavida.data.model.CreateStorySession
import com.example.aprendemoslavida.story.StoryTopic
import com.google.gson.Gson
import com.google.gson.JsonParser
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * F-13: POST /story-sessions `gateTopics` only accepts the contract enum
 * (CreateStorySession.gateTopics in openapi.yaml), not game-session subject codes.
 */
class StoryGateTopicsContractTest {

    private val contractEnum = listOf(
        "NATURAL", "MATH_MULTIPLICATION", "MATH_ADD_SUB", "ENGLISH", "SOCIAL", "LANGUAGE", "CASTLES"
    )

    @Test
    fun everyStoryTopicMapsToAContractValue() {
        assertEquals(contractEnum, StoryTopic.values().map { it.apiCode })
    }

    @Test
    fun createStorySessionBodyUsesContractEnum() {
        val topics = listOf(
            StoryTopic.NATURAL, StoryTopic.MATH_MULTIPLICATION, StoryTopic.ENGLISH, StoryTopic.SOCIAL,
            StoryTopic.LANGUAGE, StoryTopic.CASTLES, StoryTopic.MATH_ADD_SUB, StoryTopic.ENGLISH,
            StoryTopic.SOCIAL, StoryTopic.CASTLES
        )
        val body = Gson().toJson(
            CreateStorySession("16b274fd-47e3-43da-aa2e-8c607f76afc5", 3, 420, topics.map { it.apiCode })
        )

        val sent = JsonParser.parseString(body).asJsonObject.getAsJsonArray("gateTopics").map { it.asString }
        assertEquals(10, sent.size)
        sent.forEach { assert(it in contractEnum) { "Not in contract enum: $it" } }
        assertEquals("MATH_ADD_SUB", sent[6])
    }
}
