package com.example.aprendemoslavida.data.repository

import com.example.aprendemoslavida.data.api.ContentApiService
import com.example.aprendemoslavida.data.model.ContentManifest
import com.example.aprendemoslavida.data.model.ContentRelease
import com.example.aprendemoslavida.data.network.NetworkResult
import com.example.aprendemoslavida.data.network.ProblemJsonParser

class ContentRepository(
    private val contentApiService: ContentApiService
) {

    suspend fun getContentManifest(
        cachedETag: String? = null,
        locale: String = "es-ES"
    ): NetworkResult<ContentManifest?> {
        return try {
            val response = contentApiService.getContentManifest(eTag = cachedETag, locale = locale)
            if (response.code() == 304) {
                NetworkResult.Success(null) // Not modified
            } else if (response.isSuccessful && response.body() != null) {
                NetworkResult.Success(response.body()!!)
            } else {
                NetworkResult.Error(ProblemJsonParser.parseProblem(response), response.code())
            }
        } catch (e: Exception) {
            NetworkResult.Exception(e)
        }
    }

    suspend fun downloadContentRelease(
        version: String,
        cachedETag: String? = null,
        locale: String = "es-ES"
    ): NetworkResult<ContentRelease?> {
        return try {
            val response = contentApiService.downloadContentRelease(version = version, eTag = cachedETag, locale = locale)
            if (response.code() == 304) {
                NetworkResult.Success(null)
            } else if (response.isSuccessful && response.body() != null) {
                NetworkResult.Success(response.body()!!)
            } else {
                NetworkResult.Error(ProblemJsonParser.parseProblem(response), response.code())
            }
        } catch (e: Exception) {
            NetworkResult.Exception(e)
        }
    }
}
