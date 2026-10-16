package com.example.aprendemoslavida.data.repository

import com.example.aprendemoslavida.data.api.LegacyImportApiService
import com.example.aprendemoslavida.data.model.LegacyImport
import com.example.aprendemoslavida.data.model.LegacyImportResult
import com.example.aprendemoslavida.data.network.NetworkResult
import com.example.aprendemoslavida.data.network.ProblemJsonParser
import java.util.UUID

class LegacyImportRepository(
    private val legacyImportApiService: LegacyImportApiService
) {

    suspend fun importLegacyData(
        childId: String,
        legacyImport: LegacyImport,
        idempotencyKey: String = UUID.randomUUID().toString()
    ): NetworkResult<LegacyImportResult> {
        return try {
            val response = legacyImportApiService.importLegacyData(
                childId = childId,
                idempotencyKey = idempotencyKey,
                request = legacyImport
            )
            if (response.isSuccessful && response.body() != null) {
                NetworkResult.Success(response.body()!!)
            } else {
                NetworkResult.Error(ProblemJsonParser.parseProblem(response), response.code())
            }
        } catch (e: Exception) {
            NetworkResult.Exception(e)
        }
    }
}
