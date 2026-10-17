package com.example.aprendemoslavida

import android.content.Context
import android.content.SharedPreferences
import com.example.aprendemoslavida.data.api.AccountApiService
import com.example.aprendemoslavida.data.network.AuthInterceptor
import com.example.aprendemoslavida.data.network.TokenAuthenticator
import com.example.aprendemoslavida.data.storage.TokenStorage
import com.google.gson.Gson
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class ConcurrentRefreshTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var tokenStorage: TokenStorage
    private lateinit var accountApiService: AccountApiService

    private val inMemoryPrefsMap = mutableMapOf<String, Any?>()

    @Before
    fun setUp() {
        mockWebServer = MockWebServer()
        mockWebServer.start()

        val mockContext = mockk<Context>(relaxed = true)
        val mockSharedPreferences = mockk<SharedPreferences>(relaxed = true)
        val mockEditor = mockk<SharedPreferences.Editor>(relaxed = true)

        every { mockContext.getSharedPreferences(any(), any()) } returns mockSharedPreferences
        every { mockSharedPreferences.edit() } returns mockEditor

        every { mockSharedPreferences.getString(any(), any()) } answers {
            val key = firstArg<String>()
            val default = secondArg<String?>()
            (inMemoryPrefsMap[key] as? String) ?: default
        }

        every { mockEditor.putString(any(), any()) } answers {
            val key = firstArg<String>()
            val value = secondArg<String?>()
            inMemoryPrefsMap[key] = value
            mockEditor
        }

        every { mockEditor.remove(any()) } answers {
            val key = firstArg<String>()
            inMemoryPrefsMap.remove(key)
            mockEditor
        }

        every { mockEditor.apply() } returns Unit

        tokenStorage = TokenStorage(mockContext, mockSharedPreferences)
        // Set initial expired access token and valid refresh token
        inMemoryPrefsMap["key_access_token"] = "expired_access_token"
        inMemoryPrefsMap["key_refresh_token"] = "valid_refresh_token"
        inMemoryPrefsMap["key_installation_id"] = "11111111-2222-3333-4444-555555555555"

        val baseUrl = mockWebServer.url("/").toString()
        val authInterceptor = AuthInterceptor(tokenStorage)
        val authenticator = TokenAuthenticator(tokenStorage, baseUrl, Gson())

        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .authenticator(authenticator)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        accountApiService = retrofit.create(AccountApiService::class.java)
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun testConcurrentRefreshExecutesSingleRefreshCall() = runBlocking {
        // Enqueue 2 initial 401 responses for parallel requests
        mockWebServer.enqueue(MockResponse().setResponseCode(401))
        mockWebServer.enqueue(MockResponse().setResponseCode(401))

        // Enqueue 1 refresh success response
        val refreshResponseJson = """
            {
                "tokenType": "Bearer",
                "accessToken": "new_refreshed_access_token",
                "accessTokenExpiresAt": "2026-10-18T12:00:00Z",
                "refreshToken": "new_opaque_refresh_token",
                "installationId": "11111111-2222-3333-4444-555555555555"
            }
        """.trimIndent()
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(refreshResponseJson))

        // Enqueue 2 successful retry responses for the parallel requests
        val accountJson = """{"id":"111","email":"user@example.com"}"""
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(accountJson))
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(accountJson))

        // Dispatch 2 concurrent calls
        val call1 = async(Dispatchers.IO) { accountApiService.getCurrentAccount() }
        val call2 = async(Dispatchers.IO) { accountApiService.getCurrentAccount() }

        val res1 = call1.await()
        val res2 = call2.await()

        // Both calls should eventually succeed
        assertEquals(200, res1.code())
        assertEquals(200, res2.code())
        assertNotNull(res1.body())
        assertNotNull(res2.body())

        // Verify token storage updated
        assertEquals("new_refreshed_access_token", tokenStorage.getAccessToken())

        // Inspect requests received by MockWebServer:
        // Must contain: 2 original requests, 1 refresh request, and 2 retried requests.
        // Exactly ONE request to /auth/refresh!
        var refreshCount = 0
        var totalRequests = 0

        while (mockWebServer.requestCount > totalRequests) {
            val request = mockWebServer.takeRequest()
            totalRequests++
            if (request.path?.contains("/auth/refresh") == true) {
                refreshCount++
            }
        }

        assertEquals("Only ONE refresh request should be executed concurrently", 1, refreshCount)
    }
}
