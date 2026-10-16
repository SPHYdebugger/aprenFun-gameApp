package com.example.aprendemoslavida

import com.example.aprendemoslavida.data.api.AccountApiService
import com.example.aprendemoslavida.data.api.AuthApiService
import com.example.aprendemoslavida.data.api.ProfileApiService
import com.example.aprendemoslavida.data.model.ActivationRequest
import com.example.aprendemoslavida.data.model.ChildInputDto
import com.example.aprendemoslavida.data.model.GuardianTokenRequest
import com.example.aprendemoslavida.data.model.LoginRequest
import com.example.aprendemoslavida.data.model.RegisterRequest
import com.example.aprendemoslavida.data.model.VerifyEmailRequest
import com.example.aprendemoslavida.data.network.ProblemJsonParser
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

class ContractTests {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var authApiService: AuthApiService
    private lateinit var accountApiService: AccountApiService
    private lateinit var profileApiService: ProfileApiService
    private val gson = Gson()

    @Before
    fun setUp() {
        mockWebServer = MockWebServer()
        mockWebServer.start()

        val retrofit = Retrofit.Builder()
            .baseUrl(mockWebServer.url("/"))
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()

        authApiService = retrofit.create(AuthApiService::class.java)
        accountApiService = retrofit.create(AccountApiService::class.java)
        profileApiService = retrofit.create(ProfileApiService::class.java)
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun testLoginContract() = runBlocking {
        val mockJson = """
            {
                "tokenType": "Bearer",
                "accessToken": "test_access_token_jwt",
                "accessTokenExpiresAt": "2026-10-17T12:00:00Z",
                "refreshToken": "test_refresh_token_opaque",
                "installationId": "11111111-2222-3333-4444-555555555555"
            }
        """.trimIndent()

        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(mockJson))

        val response = authApiService.login(
            LoginRequest(
                email = "tutor@example.com",
                password = "Password123456!",
                installationId = "11111111-2222-3333-4444-555555555555"
            )
        )

        assertTrue(response.isSuccessful)
        val body = response.body()
        assertNotNull(body)
        assertEquals("Bearer", body?.tokenType)
        assertEquals("test_access_token_jwt", body?.accessToken)
        assertEquals("test_refresh_token_opaque", body?.refreshToken)

        val recordedRequest = mockWebServer.takeRequest()
        assertEquals("/auth/login", recordedRequest.path)
        assertEquals("POST", recordedRequest.method)
    }

    @Test
    fun testRegisterContract() = runBlocking {
        val mockJson = """{"message": "Registro aceptado. Verifique su correo."}"""
        mockWebServer.enqueue(MockResponse().setResponseCode(202).setBody(mockJson))

        val response = authApiService.register(
            RegisterRequest(
                email = "tutor@example.com",
                password = "Password123456!",
                locale = "es-ES",
                countryCode = "ES",
                legalDocumentVersion = "1.0",
                parentalConsent = true
            )
        )

        assertTrue(response.isSuccessful)
        assertEquals(202, response.code())
        assertEquals("Registro aceptado. Verifique su correo.", response.body()?.message)
    }

    @Test
    fun testProblemJsonContractOn400Error() = runBlocking {
        val problemJsonStr = """
            {
                "type": "https://api.aprenfun.es/errors/invalid-credentials",
                "title": "Credenciales no válidas",
                "status": 401,
                "detail": "El correo o la contraseña son incorrectos",
                "instance": "/api/v1/auth/login"
            }
        """.trimIndent()

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(401)
                .setHeader("Content-Type", "application/problem+json")
                .setBody(problemJsonStr)
        )

        val response = authApiService.login(
            LoginRequest("wrong@email.com", "wrongpass123", "11111111-2222-3333-4444-555555555555")
        )

        assertEquals(401, response.code())
        val problem = ProblemJsonParser.parseProblem(response)
        assertEquals(401, problem.status)
        assertEquals("Credenciales no válidas", problem.title)
        assertEquals("El correo o la contraseña son incorrectos", problem.detail)
    }

    @Test
    fun testGetCurrentAccountContract() = runBlocking {
        val accountJson = """
            {
                "id": "11111111-1111-1111-1111-111111111111",
                "email": "tutor@example.com",
                "locale": "es-ES",
                "countryCode": "ES",
                "roles": ["TUTOR"],
                "activeChildId": "22222222-2222-2222-2222-222222222222",
                "createdAt": "2026-01-01T00:00:00Z"
            }
        """.trimIndent()

        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(accountJson))

        val response = accountApiService.getCurrentAccount()
        assertTrue(response.isSuccessful)
        val account = response.body()
        assertNotNull(account)
        assertEquals("tutor@example.com", account?.email)
        assertEquals("22222222-2222-2222-2222-222222222222", account?.activeChildId)
    }

    @Test
    fun testListAndActivateChildContract() = runBlocking {
        val childrenListJson = """
            [
                {
                    "id": "22222222-2222-2222-2222-222222222222",
                    "pseudonym": "Lucas",
                    "age": 8,
                    "version": 0
                }
            ]
        """.trimIndent()

        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(childrenListJson))
        mockWebServer.enqueue(MockResponse().setResponseCode(200))

        val listResponse = profileApiService.listChildren()
        assertTrue(listResponse.isSuccessful)
        assertEquals(1, listResponse.body()?.size)
        assertEquals("Lucas", listResponse.body()?.get(0)?.pseudonym)

        val activateResponse = profileApiService.activateChild(
            childId = "22222222-2222-2222-2222-222222222222",
            activation = ActivationRequest(platform = "ANDROID", appVersion = "1.0")
        )
        assertTrue(activateResponse.isSuccessful)
    }
}
