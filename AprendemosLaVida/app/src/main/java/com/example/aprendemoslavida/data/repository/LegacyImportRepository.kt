package com.example.aprendemoslavida.data.repository

import com.example.aprendemoslavida.data.api.LegacyImportApiService
import com.example.aprendemoslavida.data.model.LegacyImport
import com.example.aprendemoslavida.data.model.LegacyImportResult
import com.example.aprendemoslavida.data.network.NetworkResult
import com.example.aprendemoslavida.data.network.ProblemJsonParser
import com.google.gson.Gson

class LegacyImportRepository(
    private val legacyImportApiService: LegacyImportApiService
) {

    suspend fun importLegacyData(
        childId: String,
        legacyImport: LegacyImport,
        idempotencyKey: String
    ): NetworkResult<LegacyImportResult> {
        return try {
            val response = legacyImportApiService.importLegacyData(
                childId = childId,
                idempotencyKey = idempotencyKey,
                request = legacyImport
            )
            if (response.isSuccessful && response.body() != null) {
                NetworkResult.Success(response.body()!!)
            } else if (response.code() == 409) {
                val body = response.errorBody()?.string()
                val result = body?.let { runCatching { Gson().fromJson(it, LegacyImportResult::class.java) }.getOrNull() }
                if (result?.status == "ALREADY_IMPORTED") NetworkResult.Success(result)
                else NetworkResult.Error(ProblemJsonParser.parseProblem(response), response.code())
            } else {
                NetworkResult.Error(ProblemJsonParser.parseProblem(response), response.code())
            }
        } catch (e: Exception) {
            NetworkResult.Exception(e)
        }
    }
}
