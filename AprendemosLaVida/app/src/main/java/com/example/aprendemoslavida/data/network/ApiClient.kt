package com.example.aprendemoslavida.data.network

import android.content.Context
import com.example.aprendemoslavida.BuildConfig
import com.example.aprendemoslavida.data.api.AccountApiService
import com.example.aprendemoslavida.data.api.AuthApiService
import com.example.aprendemoslavida.data.api.ProfileApiService
import com.example.aprendemoslavida.data.storage.TokenStorage
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class ApiClient private constructor(context: Context) {

    val tokenStorage: TokenStorage = TokenStorage(context.applicationContext)

    private val gson = GsonBuilder()
        .setLenient()
        .create()

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = if (BuildConfig.DEBUG) {
            HttpLoggingInterceptor.Level.BODY
        } else {
            HttpLoggingInterceptor.Level.NONE
        }
    }

    private val authInterceptor = AuthInterceptor(tokenStorage)
    private val tokenAuthenticator = TokenAuthenticator(tokenStorage, BuildConfig.BASE_URL, gson)

    val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .addInterceptor(authInterceptor)
        .authenticator(tokenAuthenticator)
        .addInterceptor(loggingInterceptor)
        .build()

    val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create(gson))
        .build()

    val authApiService: AuthApiService by lazy { retrofit.create(AuthApiService::class.java) }
    val accountApiService: AccountApiService by lazy { retrofit.create(AccountApiService::class.java) }
    val profileApiService: ProfileApiService by lazy { retrofit.create(ProfileApiService::class.java) }
    val contentApiService: com.example.aprendemoslavida.data.api.ContentApiService by lazy { retrofit.create(com.example.aprendemoslavida.data.api.ContentApiService::class.java) }
    val gameSessionApiService: com.example.aprendemoslavida.data.api.GameSessionApiService by lazy { retrofit.create(com.example.aprendemoslavida.data.api.GameSessionApiService::class.java) }
    val storySessionApiService: com.example.aprendemoslavida.data.api.StorySessionApiService by lazy { retrofit.create(com.example.aprendemoslavida.data.api.StorySessionApiService::class.java) }
    val progressApiService: com.example.aprendemoslavida.data.api.ProgressApiService by lazy { retrofit.create(com.example.aprendemoslavida.data.api.ProgressApiService::class.java) }
    val leaderboardApiService: com.example.aprendemoslavida.data.api.LeaderboardApiService by lazy { retrofit.create(com.example.aprendemoslavida.data.api.LeaderboardApiService::class.java) }
    val legacyImportApiService: com.example.aprendemoslavida.data.api.LegacyImportApiService by lazy { retrofit.create(com.example.aprendemoslavida.data.api.LegacyImportApiService::class.java) }

    companion object {
        @Volatile
        private var INSTANCE: ApiClient? = null

        fun getInstance(context: Context): ApiClient {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ApiClient(context).also { INSTANCE = it }
            }
        }
    }
}
