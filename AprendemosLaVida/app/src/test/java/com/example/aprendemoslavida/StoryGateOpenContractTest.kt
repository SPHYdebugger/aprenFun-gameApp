package com.example.aprendemoslavida

import com.example.aprendemoslavida.data.model.StoryGateOpenResponse
import com.example.aprendemoslavida.data.model.StorySession
import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * F-14: POST /story-sessions/{id}/gates/{gateId}/open returns the full StorySession snapshot.
 * Fixture: real staging response (17-nov 16:20), trimmed to three gates.
 */
class StoryGateOpenContractTest {

    private val openResponse = """
        {"id":"1c915c83-3a42-4a49-817c-727b0cb6419c","status":"ACTIVE","release":"android-legacy-2","locale":"es-ES",
         "startedAt":"2026-11-17T15:19:59.564765Z","remainingTimeMs":370823,"score":0,"mapCount":3,"completedMaps":0,
         "finishReason":null,
         "currentMap":{"id":"8668b4d7-f2c4-423d-9f89-7748ac4fa748","position":0,"visualTheme":"CITY_CLASSIC",
           "layoutSeed":9024255451911496106,"status":"CURRENT","resolvedGates":0,"minimumGatesToExit":5,
           "gates":[
             {"id":"a2186ee7-82c8-4496-b9ce-642e7c881b73","position":0,"topic":"NATURAL","optional":false,
              "status":"ACTIVE","points":0,
              "currentQuestion":{"id":"408ba2e9-addf-4494-9170-36eafa23e8a1",
                "contentQuestionId":"a92df891-b372-3963-8103-6a5d82230c81","position":0,"interaction":"MULTIPLE_CHOICE",
                "prompt":"¿Cuál de estos NO es un ser vivo? 🪨",
                "options":[{"id":"9a50750b-ee6e-36e6-b519-71ff5916291e","stableKey":"option-4","text":"Bacteria"},
                           {"id":"2ba667c4-ce70-3353-b0a2-6ecfa8ec0134","stableKey":"option-2","text":"Perro"},
                           {"id":"d0b37e57-23f0-34d9-bf5a-c5847d7e305d","stableKey":"option-1","text":"Piedra"},
                           {"id":"3eaff643-eb25-39ba-a7be-479e73d76abe","stableKey":"option-3","text":"Árbol"}],
                "castle":null,"presentedAt":"2026-11-17T15:19:59.900149Z","deadline":null}},
             {"id":"efbf0574-e2fc-444f-848d-abb2b5c3741b","position":1,"topic":"MATH_MULTIPLICATION","optional":false,
              "status":"AVAILABLE","points":0,"currentQuestion":null},
             {"id":"2a9e228f-1acb-4ebf-b722-121643c45a54","position":9,"topic":"CASTLES","optional":true,
              "status":"AVAILABLE","points":0,"currentQuestion":null}]},
         "santi":{"availableOnCurrentMap":false,"status":"AVAILABLE","rewardPoints":500,"currentChallenge":null}}
    """.trimIndent()

    private val session: StoryGateOpenResponse = Gson().fromJson(openResponse, StoryGateOpenResponse::class.java)

    /** F-16: real staging response (18-nov 19:33), only the opened gate. */
    private val gateOnlyResponse = """
        {"sessionId":"e3bd9a56-6b84-429b-a7fd-e634c87cbb55","remainingTimeMs":412389,
         "gate":{"id":"150fc303-83ca-4a19-91ca-2792f2f83772","position":4,"topic":"LANGUAGE","optional":false,
          "status":"ACTIVE","points":0,
          "currentQuestion":{"id":"b1ed6b46-5c57-4a8a-a718-5f5fa3005523",
            "contentQuestionId":"85a1f995-0000-3929-8a40-2ca47f607965","position":4,"interaction":"MULTIPLE_CHOICE",
            "prompt":"Plural de 'animal' es...",
            "options":[{"id":"fe971890-5bd0-38fe-a418-49dfa7cb5db8","stableKey":"option-2","text":"animals"},
                       {"id":"5a4b2407-2d2d-3dab-82fe-69d75557657e","stableKey":"option-4","text":"animalez"},
                       {"id":"285840d1-c3db-36dc-ba65-2e53f2e2be70","stableKey":"option-3","text":"animaleses"},
                       {"id":"809f7dca-468f-3553-ac7d-6d4608f22926","stableKey":"option-1","text":"animales"}],
            "castle":null,"presentedAt":"2026-11-18T19:33:14.330529Z","deadline":null}}}
    """.trimIndent()

    private val gateOnly: StoryGateOpenResponse = Gson().fromJson(gateOnlyResponse, StoryGateOpenResponse::class.java)

    @Test
    fun gateOnlyResponseIsNotAFullSession() {
        assertNull("Must not be read as a finished game", gateOnly.fullSession())
        assertEquals(4, gateOnly.questionForGate("150fc303-83ca-4a19-91ca-2792f2f83772")?.options?.size)
    }

    @Test
    fun gateOnlyResponseMergesIntoKnownSessionAndKeepsItActive() {
        val known = session.fullSession()!!.let { s ->
            s.copy(currentMap = s.currentMap!!.copy(gates = s.currentMap!!.gates.map {
                if (it.position == 0) it.copy(id = "150fc303-83ca-4a19-91ca-2792f2f83772", status = "AVAILABLE", currentQuestion = null) else it
            }))
        }
        val merged = gateOnly.mergeInto(known)
        assertEquals("ACTIVE", merged.status)
        assertEquals(412389L, merged.remainingTimeMs)
        val gate = merged.currentMap!!.gates.first { it.id == "150fc303-83ca-4a19-91ca-2792f2f83772" }
        assertEquals("ACTIVE", gate.status)
        assertNotNull(gate.currentQuestion)
        assertEquals(known.currentMap!!.gates.size, merged.currentMap!!.gates.size)
    }

    @Test
    fun openResponseParsesAsStorySession() {
        assertEquals("ACTIVE", session.fullSession()?.status)
        assertEquals(3, session.currentMap?.gates?.size)
        assertEquals(false, session.santi?.availableOnCurrentMap)
    }

    @Test
    fun questionIsTakenFromTheOpenedGate() {
        val q = session.questionForGate("a2186ee7-82c8-4496-b9ce-642e7c881b73")
        assertEquals("408ba2e9-addf-4494-9170-36eafa23e8a1", q?.id)
        assertEquals(4, q?.options?.size)
    }

    @Test
    fun gateWithoutExerciseOrUnknownGateGivesNoQuestion() {
        assertNull(session.questionForGate("efbf0574-e2fc-444f-848d-abb2b5c3741b"))
        assertNull(session.questionForGate("00000000-0000-0000-0000-000000000000"))
    }
}
