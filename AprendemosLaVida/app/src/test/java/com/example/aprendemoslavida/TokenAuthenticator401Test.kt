package com.example.aprendemoslavida

import android.content.Context
import android.content.SharedPreferences
import com.example.aprendemoslavida.data.api.AccountApiService
import com.example.aprendemoslavida.data.api.AuthApiService
import com.example.aprendemoslavida.data.model.GuardianTokenRequest
import com.example.aprendemoslavida.data.network.AuthInterceptor
import com.example.aprendemoslavida.data.network.ProblemJsonParser
import com.example.aprendemoslavida.data.network.TokenAuthenticator
import com.example.aprendemoslavida.data.storage.TokenStorage
import com.google.gson.Gson
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * F-11: only a 401 with `WWW-Authenticate: Bearer error="invalid_token"` triggers a refresh.
 * A problem+json 401 (e.g. wrong guardian password) must reach the caller with its body and
 * must not touch the stored session.
 */
class TokenAuthenticator401Test {

    private lateinit var server: MockWebServer
    private lateinit var authApi: AuthApiService
    private lateinit var accountApi: AccountApiService
    private val prefs = mutableMapOf<String, Any?>()

    private val invalidCredentials = """
        {"type":"https://api.aprenfun.es/errors/invalid-credentials","title":"Credenciales no válidas",
         "status":401,"detail":"Credenciales no válidas.","instance":"/api/v1/auth/guardian-action-token"}
    """.trimIndent()

    @Before
    fun setUp() {
        server = MockWebServer().apply { start() }

        val context = mockk<Context>(relaxed = true)
        val sharedPrefs = mockk<SharedPreferences>(relaxed = true)
        val editor = mockk<SharedPreferences.Editor>(relaxed = true)
        every { context.getSharedPreferences(any(), any()) } returns sharedPrefs
        every { sharedPrefs.edit() } returns editor
        every { sharedPrefs.getString(any(), any()) } answers {
            (prefs[firstArg()] as? String) ?: secondArg()
        }
        every { editor.putString(any(), any()) } answers { prefs[firstArg()] = secondArg<String?>(); editor }
        every { editor.remove(any()) } answers { prefs.remove(firstArg<String>()); editor }
        every { editor.apply() } returns Unit

        prefs["key_access_token"] = "access_token"
        prefs["key_refresh_token"] = "refresh_token"
        prefs["key_installation_id"] = "11111111-2222-3333-4444-555555555555"

        val storage = TokenStorage(context, sharedPrefs)
        val baseUrl = server.url("/").toString()
        val client = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(storage))
            .authenticator(TokenAuthenticator(storage, baseUrl, Gson()))
            .build()
        val retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        authApi = retrofit.create(AuthApiService::class.java)
        accountApi = retrofit.create(AccountApiService::class.java)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    private fun problem401() = MockResponse()
        .setResponseCode(401)
        .setHeader("Content-Type", "application/problem+json")
        .setBody(invalidCredentials)

    @Test
    fun wrongGuardianPasswordKeepsSessionAndExposesDetail() = runBlocking {
        server.enqueue(problem401())

        val response = authApi.createGuardianActionToken(
            GuardianTokenRequest("wrong", GuardianTokenRequest.PURPOSE_MANAGE_CHILDREN)
        )

        assertEquals(401, response.code())
        assertEquals("Credenciales no válidas.", ProblemJsonParser.parseProblem(response).detail)
        assertEquals("No refresh and no retry", 1, server.requestCount)
        assertEquals("access_token", prefs["key_access_token"])
        assertEquals("refresh_token", prefs["key_refresh_token"])
    }

    @Test
    fun problemJson401OnOtherEndpointIsNotTreatedAsExpiredToken() = runBlocking {
        server.enqueue(problem401())

        val response = accountApi.getCurrentAccount()

        assertEquals(401, response.code())
        assertEquals(1, server.requestCount)
        assertEquals("refresh_token", prefs["key_refresh_token"])
    }

    @Test
    fun invalidTokenChallengeRefreshesAndRetries() = runBlocking {
        server.enqueue(
            MockResponse().setResponseCode(401)
                .setHeader("WWW-Authenticate", "Bearer error=\"invalid_token\", error_description=\"Jwt expired\"")
        )
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{"tokenType":"Bearer","accessToken":"new_access","accessTokenExpiresAt":"2026-12-01T00:00:00Z",
                   "refreshToken":"new_refresh","installationId":"11111111-2222-3333-4444-555555555555"}"""
            )
        )
        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"id":"1","email":"tutor@example.com"}"""))

        val response = accountApi.getCurrentAccount()

        assertEquals(200, response.code())
        server.takeRequest()
        assertEquals("/auth/refresh", server.takeRequest().path)
        assertEquals("Bearer new_access", server.takeRequest().getHeader("Authorization"))
        assertEquals("new_refresh", prefs["key_refresh_token"])
    }

    @Test
    fun invalidTokenOnGuardianEndpointRefreshesAndRetriesWithSamePassword() = runBlocking {
        server.enqueue(
            MockResponse().setResponseCode(401)
                .setHeader("WWW-Authenticate", "Bearer error=\"invalid_token\", error_description=\"Jwt expired\"")
        )
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{"tokenType":"Bearer","accessToken":"new_access","accessTokenExpiresAt":"2026-12-01T00:00:00Z",
                   "refreshToken":"new_refresh","installationId":"11111111-2222-3333-4444-555555555555"}"""
            )
        )
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{"token":"guardian_token","expiresAt":"2026-11-17T15:00:00Z","purpose":"MANAGE_CHILDREN"}"""
            )
        )

        val response = authApi.createGuardianActionToken(
            GuardianTokenRequest("right-password", GuardianTokenRequest.PURPOSE_MANAGE_CHILDREN)
        )

        assertEquals(200, response.code())
        assertEquals("guardian_token", response.body()?.token)
        val first = server.takeRequest()
        assertEquals("/auth/refresh", server.takeRequest().path)
        val retry = server.takeRequest()
        assertEquals("/auth/guardian-action-token", retry.path)
        assertEquals("Bearer new_access", retry.getHeader("Authorization"))
        assertEquals(first.body.readUtf8(), retry.body.readUtf8())
        assertEquals("new_refresh", prefs["key_refresh_token"])
    }
}
