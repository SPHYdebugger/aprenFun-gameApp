package com.example.aprendemoslavida

import com.example.aprendemoslavida.data.api.ContentApiService
import com.example.aprendemoslavida.data.api.GameSessionApiService
import com.example.aprendemoslavida.data.api.LeaderboardApiService
import com.example.aprendemoslavida.data.api.LegacyImportApiService
import com.example.aprendemoslavida.data.api.ProgressApiService
import com.example.aprendemoslavida.data.api.StorySessionApiService
import com.example.aprendemoslavida.data.model.CreateGameSession
import com.example.aprendemoslavida.data.model.CreateStorySession
import com.example.aprendemoslavida.data.model.LegacyImport
import com.example.aprendemoslavida.data.model.LegacyImportAvatar
import com.example.aprendemoslavida.data.model.LegacyImportPreferences
import com.example.aprendemoslavida.data.model.LegacyImportStreak
import com.example.aprendemoslavida.data.model.SantiActionRequest
import com.example.aprendemoslavida.data.model.SubmitGameAnswer
import com.example.aprendemoslavida.data.model.SubmitStoryAnswer
import com.example.aprendemoslavida.data.model.SubjectMapper
import com.google.gson.Gson
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class GameContractTests {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var gameSessionApiService: GameSessionApiService
    private lateinit var storySessionApiService: StorySessionApiService
    private lateinit var contentApiService: ContentApiService
    private lateinit var progressApiService: ProgressApiService
    private lateinit var leaderboardApiService: LeaderboardApiService
    private lateinit var legacyImportApiService: LegacyImportApiService
    private val gson = Gson()

    @Before
    fun setUp() {
        mockWebServer = MockWebServer()
        mockWebServer.start()

        val retrofit = Retrofit.Builder()
            .baseUrl(mockWebServer.url("/"))
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()

        gameSessionApiService = retrofit.create(GameSessionApiService::class.java)
        storySessionApiService = retrofit.create(StorySessionApiService::class.java)
        contentApiService = retrofit.create(ContentApiService::class.java)
        progressApiService = retrofit.create(ProgressApiService::class.java)
        leaderboardApiService = retrofit.create(LeaderboardApiService::class.java)
        legacyImportApiService = retrofit.create(LegacyImportApiService::class.java)
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun testCreateGameSessionAndSubmitAnswerContract() = runBlocking {
        val sessionJson = """
            {
                "id": "11111111-1111-1111-1111-111111111111",
                "mode": "QUIZ",
                "status": "ACTIVE",
                "release": "v1.0",
                "startedAt": "2026-10-17T00:00:00Z",
                "expiresAt": "2026-10-17T01:00:00Z",
                "score": 0,
                "answeredQuestions": 0,
                "correctAnswers": 0,
                "totalQuestions": 20,
                "currentQuestion": {
                    "id": "q-session-1",
                    "contentQuestionId": "content-q-1",
                    "position": 0,
                    "interaction": "MULTIPLE_CHOICE",
                    "prompt": "¿De qué color es la hierba?",
                    "options": [
                        {"id": "opt-1", "stableKey": "A", "text": "Verde"},
                        {"id": "opt-2", "stableKey": "B", "text": "Azul"}
                    ],
                    "presentedAt": "2026-10-17T00:00:00Z"
                }
            }
        """.trimIndent()

        mockWebServer.enqueue(MockResponse().setResponseCode(201).setBody(sessionJson))

        val createResponse = gameSessionApiService.createGameSession(
            idempotencyKey = "idempotency-key-1",
            request = CreateGameSession(
                childProfileId = "22222222-2222-2222-2222-222222222222",
                subjectCode = SubjectMapper.CODE_NATURAL
            )
        )

        assertTrue(createResponse.isSuccessful)
        assertEquals(201, createResponse.code())
        assertEquals("11111111-1111-1111-1111-111111111111", createResponse.body()?.id)

        val req1 = mockWebServer.takeRequest()
        assertEquals("/game-sessions", req1.path)
        assertEquals("idempotency-key-1", req1.getHeader("Idempotency-Key"))

        val answerJson = """
            {
                "answerId": "ans-1",
                "sessionId": "11111111-1111-1111-1111-111111111111",
                "sessionQuestionId": "q-session-1",
                "correct": true,
                "questionResolved": true,
                "points": 100,
                "totalScore": 100,
                "sessionStatus": "ACTIVE"
            }
        """.trimIndent()

        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(answerJson))

        val answerResponse = gameSessionApiService.submitGameAnswer(
            sessionId = "11111111-1111-1111-1111-111111111111",
            idempotencyKey = "idempotency-key-2",
            request = SubmitGameAnswer(
                sessionQuestionId = "q-session-1",
                selectedOptionId = "opt-1"
            )
        )

        assertTrue(answerResponse.isSuccessful)
        assertTrue(answerResponse.body()?.correct == true)
        assertEquals(100, answerResponse.body()?.totalScore)
    }

    @Test
    fun testStorySessionContract() = runBlocking {
        val storySessionJson = """
            {
                "id": "story-s1",
                "status": "ACTIVE",
                "release": "v1.0",
                "locale": "es-ES",
                "startedAt": "2026-10-17T00:00:00Z",
                "remainingTimeMs": 600000,
                "score": 0,
                "mapCount": 3,
                "completedMaps": 0,
                "currentMap": {
                    "id": "map-1",
                    "position": 0,
                    "visualTheme": "FOREST",
                    "layoutSeed": 12345,
                    "status": "CURRENT",
                    "resolvedGates": 0,
                    "minimumGatesToExit": 5,
                    "gates": []
                },
                "santi": {
                    "availableOnCurrentMap": true,
                    "status": "AVAILABLE",
                    "rewardPoints": 500
                }
            }
        """.trimIndent()

        mockWebServer.enqueue(MockResponse().setResponseCode(201).setBody(storySessionJson))

        val createResponse = storySessionApiService.createStorySession(
            idempotencyKey = "story-idem-1",
            request = CreateStorySession(
                childProfileId = "22222222-2222-2222-2222-222222222222",
                mapCount = 3,
                durationSeconds = 600,
                gateTopics = List(10) { SubjectMapper.CODE_NATURAL }
            )
        )

        assertTrue(createResponse.isSuccessful)
        assertEquals("story-s1", createResponse.body()?.id)

        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody("""{"status":"ACTIVE","currentQuestion":{"id":"q-open","contentQuestionId":"c-open","position":0,"interaction":"MULTIPLE_CHOICE","prompt":"P","options":[],"presentedAt":"2026-10-17T00:00:00Z"}}"""))
        val openGateRes = storySessionApiService.openStoryGate("story-s1", "gate-1")
        assertTrue(openGateRes.isSuccessful)

        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody("""{"availableOnCurrentMap":true,"status":"ACTIVE","rewardPoints":500}"""))
        val santiRes = storySessionApiService.updateSantiEncounter("story-s1", SantiActionRequest("ACCEPT"))
        assertTrue(santiRes.isSuccessful)
    }

    @Test
    fun testContentManifestAndReleaseContract() = runBlocking {
        val manifestJson = """
            {
                "curriculum": "ES-PRIMARY",
                "release": "v1.0",
                "locale": "es-ES",
                "hash": "a1b2c3d4e5f60000000000000000000000000000000000000000000000000000",
                "minimumClientVersion": 1,
                "publishedAt": "2026-10-17T00:00:00Z",
                "downloadUrl": "https://api.aprenfun.es/releases/v1.0.json"
            }
        """.trimIndent()

        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(manifestJson))

        val manifestRes = contentApiService.getContentManifest()
        assertTrue(manifestRes.isSuccessful)
        assertEquals("v1.0", manifestRes.body()?.release)
        assertEquals(1, manifestRes.body()?.minimumClientVersion)
    }

    @Test
    fun testProgressAndLeaderboardsContract() = runBlocking {
        val progressJson = """
            {
                "childProfileId": "22222222-2222-2222-2222-222222222222",
                "pointsBalance": 1500,
                "currentStreak": 5,
                "longestStreak": 7,
                "topics": []
            }
        """.trimIndent()

        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(progressJson))

        val progressRes = progressApiService.getPlayerProgress("22222222-2222-2222-2222-222222222222")
        assertTrue(progressRes.isSuccessful)
        assertEquals(1500L, progressRes.body()?.pointsBalance)
        assertEquals(5, progressRes.body()?.currentStreak)

        val leaderboardJson = """
            {
                "entries": [
                    {"pseudonym": "Lucas", "score": 2500, "date": "2026-10-17T00:00:00Z"}
                ],
                "page": 0,
                "size": 20,
                "totalElements": 1,
                "totalPages": 1
            }
        """.trimIndent()

        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(leaderboardJson))

        val leaderboardRes = leaderboardApiService.getPublicLeaderboard()
        assertTrue(leaderboardRes.isSuccessful)
        assertEquals(1, leaderboardRes.body()?.entries?.size)
        assertEquals("Lucas", leaderboardRes.body()?.entries?.get(0)?.pseudonym)
    }

    @Test
    fun testLegacyImportContract() = runBlocking {
        val importResultJson = """
            {
                "childProfileId": "22222222-2222-2222-2222-222222222222",
                "status": "IMPORTED",
                "importedAt": "2026-10-17T00:00:00Z",
                "pointsImported": 1200,
                "scoresImported": 10,
                "preferencesImported": true,
                "avatarImported": true
            }
        """.trimIndent()

        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(importResultJson))

        val importReq = LegacyImport(
            sourceSchemaVersion = 1,
            globalPoints = 1200,
            preferences = LegacyImportPreferences(
                soundEnabled = true,
                storyMusicEnabled = true,
                questionTimeMs = 10000,
                storyGameTimeMs = 600000,
                storyMapCount = 3,
                language = "es",
                storyGateTopics = List(10) { SubjectMapper.CODE_NATURAL }
            ),
            avatar = LegacyImportAvatar(
                gender = "NINO",
                hairStyle = 0,
                hairColor = 0,
                eyeShape = 0,
                eyeColor = 0,
                mouthShape = 0,
                mouthColor = 0,
                skinTone = 0,
                outfit = 0
            ),
            streak = LegacyImportStreak(5, 20261017),
            scores = emptyList()
        )

        val importRes = legacyImportApiService.importLegacyData(
            childId = "22222222-2222-2222-2222-222222222222",
            idempotencyKey = "import-idem-1",
            request = importReq
        )

        assertTrue(importRes.isSuccessful)
        assertEquals("IMPORTED", importRes.body()?.status)
        assertEquals(1200, importRes.body()?.pointsImported)
    }
}
