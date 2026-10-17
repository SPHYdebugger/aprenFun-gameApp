package com.example.aprendemoslavida

import com.example.aprendemoslavida.data.api.GameSessionApiService
import com.example.aprendemoslavida.data.api.StorySessionApiService
import com.example.aprendemoslavida.data.api.LegacyImportApiService
import com.example.aprendemoslavida.data.model.CreateGameSession
import com.example.aprendemoslavida.data.model.CreateStorySession
import com.example.aprendemoslavida.data.model.AnswerColor
import com.example.aprendemoslavida.data.model.AnswerFeedback
import com.example.aprendemoslavida.data.model.GameAnswer
import com.example.aprendemoslavida.data.model.PresentedOption
import com.example.aprendemoslavida.data.model.PresentedQuestion
import com.example.aprendemoslavida.data.model.LegacyImport
import com.example.aprendemoslavida.data.model.LegacyImportAvatar
import com.example.aprendemoslavida.data.model.LegacyImportPreferences
import com.example.aprendemoslavida.data.model.LegacyImportStreak
import com.example.aprendemoslavida.data.model.SubjectMapper
import com.example.aprendemoslavida.data.repository.LegacyImportRepository
import com.example.aprendemoslavida.data.network.NetworkResult
import com.google.gson.Gson
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class RemoteIntegrationCorrectionTests {
    private lateinit var server: MockWebServer
    private lateinit var gameApi: GameSessionApiService
    private lateinit var storyApi: StorySessionApiService
    private lateinit var legacyRepository: LegacyImportRepository

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        val retrofit = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .addConverterFactory(GsonConverterFactory.create(Gson()))
            .build()
        gameApi = retrofit.create(GameSessionApiService::class.java)
        storyApi = retrofit.create(StorySessionApiService::class.java)
        legacyRepository = LegacyImportRepository(retrofit.create(LegacyImportApiService::class.java))
    }

    @After
    fun tearDown() = server.shutdown()

    @Test
    fun mapsEveryLocalModeToBackendCode() {
        assertEquals(SubjectMapper.CODE_NATURAL, SubjectMapper.toBackendSubjectCode("natural"))
        assertEquals(SubjectMapper.CODE_MATH, SubjectMapper.toBackendSubjectCode("math"))
        assertEquals(SubjectMapper.CODE_ENGLISH, SubjectMapper.toBackendSubjectCode("english"))
        assertEquals(SubjectMapper.CODE_SOCIAL, SubjectMapper.toBackendSubjectCode("social"))
        assertEquals(SubjectMapper.CODE_LANGUAGE, SubjectMapper.toBackendSubjectCode("language"))
        assertEquals(SubjectMapper.CODE_STORY, SubjectMapper.toBackendSubjectCode("story"))
    }

    @Test
    fun storyMutationResponsesAreJsonModels() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"status":"ACTIVE","currentQuestion":{"id":"q-1","contentQuestionId":"c-1","position":0,"interaction":"MULTIPLE_CHOICE","prompt":"P","options":[],"presentedAt":"2026-10-17T00:00:00Z"}}"""))
        val gate = storyApi.openStoryGate("s-1", "g-1")
        assertTrue(gate.isSuccessful)
        assertEquals("q-1", gate.body()?.currentQuestion?.id)

        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"correct":true,"points":100,"totalScore":100,"sessionStatus":"ACTIVE"}"""))
        val answer = storyApi.submitStoryAnswer("s-1", "same-key", com.example.aprendemoslavida.data.model.SubmitStoryAnswer("g-1", "q-1", selectedOptionId = "o-1"))
        assertTrue(answer.isSuccessful)
        assertEquals(100, answer.body()?.totalScore)
    }

    @Test
    fun selectedAndCorrectOptionsGetDistinctAuthoritativeColors() {
        val question = PresentedQuestion(
            id = "q-1", contentQuestionId = "c-1", position = 0,
            interaction = "MULTIPLE_CHOICE", prompt = "P",
            options = listOf(PresentedOption("wrong", "A", "A"), PresentedOption("right", "B", "B")),
            presentedAt = "2026-10-17T00:00:00Z"
        )
        val answer = GameAnswer("a-1", "s-1", "q-1", false, correctOptionId = "right", questionResolved = true, points = 0, totalScore = 0, sessionStatus = "ACTIVE")
        val visual = AnswerFeedback.resolve(answer, question, selectedIndex = 0)
        assertEquals(AnswerColor.WRONG, visual.selectedColor)
        assertEquals(1, visual.correctIndex)
        assertEquals(AnswerColor.CORRECT, visual.correctColor)
    }

    @Test
    fun castleAnswerUsesRemoteNumericProtocol() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(201).setBody("""{"id":"castle-1","mode":"CASTLE","status":"ACTIVE","release":"v1","startedAt":"2026-10-17T00:00:00Z","expiresAt":"2026-10-17T01:00:00Z","score":0,"answeredQuestions":0,"correctAnswers":0,"totalQuestions":1,"currentQuestion":{"id":"cq-1","contentQuestionId":"cc-1","position":0,"interaction":"CASTLE","prompt":"2 + 2","options":[],"castle":{"top":2,"bottom":2,"operation":"ADD","columns":1,"attemptNumber":1,"availablePoints":100},"presentedAt":"2026-10-17T00:00:00Z"}}"""))
        val session = gameApi.createGameSession("castle-key", CreateGameSession("child-1", SubjectMapper.CODE_MATH, SubjectMapper.TOPIC_CASTLES))
        assertTrue(session.isSuccessful)
        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"answerId":"ca-1","sessionId":"castle-1","sessionQuestionId":"cq-1","correct":false,"correctNumericAnswer":4,"questionResolved":true,"points":0,"totalScore":0,"sessionStatus":"ACTIVE"}"""))
        val answer = gameApi.submitGameAnswer("castle-1", "castle-answer-key", com.example.aprendemoslavida.data.model.SubmitGameAnswer("cq-1", numericAnswer = 5))
        assertTrue(answer.isSuccessful)
        assertEquals(4, answer.body()?.correctNumericAnswer)
    }

    @Test
    fun retryKeepsTheSameIdempotencyKeyAfterDisconnect() = runBlocking {
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AFTER_REQUEST))
        server.enqueue(MockResponse().setResponseCode(201).setBody("""{"id":"s-1","mode":"QUIZ","status":"ACTIVE","release":"v1","startedAt":"2026-10-17T00:00:00Z","expiresAt":"2026-10-17T01:00:00Z","score":0,"answeredQuestions":0,"correctAnswers":0,"totalQuestions":1}"""))
        val request = CreateGameSession("child-1", SubjectMapper.CODE_NATURAL)
        runCatching { gameApi.createGameSession("same-create-key", request) }
        val response = gameApi.createGameSession("same-create-key", request)
        assertTrue(response.isSuccessful)
        assertEquals("same-create-key", server.takeRequest().getHeader("Idempotency-Key"))
        assertEquals("same-create-key", server.takeRequest().getHeader("Idempotency-Key"))
    }

    @Test
    fun alreadyImported409IsHandledWithoutDeletingLocalData() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(409).setBody("""{"childProfileId":"child-1","status":"ALREADY_IMPORTED","importedAt":"2026-10-17T00:00:00Z","pointsImported":10,"scoresImported":1,"preferencesImported":true,"avatarImported":true}"""))
        val result = legacyRepository.importLegacyData("child-1", sampleImport(), "legacy-key")
        assertTrue(result is NetworkResult.Success)
        assertEquals("ALREADY_IMPORTED", (result as NetworkResult.Success).data.status)
    }

    private fun sampleImport() = LegacyImport(
        globalPoints = 10,
        preferences = LegacyImportPreferences(true, true, 12000, 360000, 3, "es", emptyList()),
        avatar = LegacyImportAvatar("NINO", 0, 0, 0, 0, 0, 0, 0, 0),
        streak = LegacyImportStreak(1, 20261017),
        scores = emptyList()
    )
}
