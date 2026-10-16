package com.example.aprendemoslavida.data.api

import com.example.aprendemoslavida.data.model.ContentManifest
import com.example.aprendemoslavida.data.model.ContentRelease
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import retrofit2.http.Query

interface ContentApiService {

    @GET("content/manifest")
    suspend fun getContentManifest(
        @Header("If-None-Match") eTag: String? = null,
        @Query("locale") locale: String = "es-ES"
    ): Response<ContentManifest>

    @GET("content/releases/{version}")
    suspend fun downloadContentRelease(
        @Path("version") version: String,
        @Header("If-None-Match") eTag: String? = null,
        @Query("locale") locale: String = "es-ES"
    ): Response<ContentRelease>
}
