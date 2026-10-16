package com.example.aprendemoslavida.data.api

import com.example.aprendemoslavida.data.model.LegacyImport
import com.example.aprendemoslavida.data.model.LegacyImportResult
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

interface LegacyImportApiService {

    @POST("children/{childId}/legacy-import")
    suspend fun importLegacyData(
        @Path("childId") childId: String,
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body request: LegacyImport
    ): Response<LegacyImportResult>
}
