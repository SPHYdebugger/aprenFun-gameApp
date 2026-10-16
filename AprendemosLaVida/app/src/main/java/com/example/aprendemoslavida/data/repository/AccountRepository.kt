package com.example.aprendemoslavida.data.repository

import com.example.aprendemoslavida.data.api.AccountApiService
import com.example.aprendemoslavida.data.model.AcceptedResponse
import com.example.aprendemoslavida.data.model.AccountDto
import com.example.aprendemoslavida.data.network.NetworkResult
import com.example.aprendemoslavida.data.network.ProblemJsonParser
import com.example.aprendemoslavida.data.storage.TokenStorage

class AccountRepository(
    private val accountApiService: AccountApiService,
    private val tokenStorage: TokenStorage
) {

    suspend fun getCurrentAccount(): NetworkResult<AccountDto> {
        return try {
            val response = accountApiService.getCurrentAccount()
            if (response.isSuccessful && response.body() != null) {
                val account = response.body()!!
                // Restore activeChildId if provided by backend
                tokenStorage.saveActiveChildId(account.activeChildId)
                NetworkResult.Success(account)
            } else {
                NetworkResult.Error(ProblemJsonParser.parseProblem(response), response.code())
            }
        } catch (e: Exception) {
            NetworkResult.Exception(e)
        }
    }

    suspend fun scheduleAccountDeletion(guardianToken: String): NetworkResult<AcceptedResponse> {
        return try {
            val response = accountApiService.scheduleAccountDeletion(guardianToken)
            if (response.isSuccessful && response.body() != null) {
                tokenStorage.clearTokens()
                NetworkResult.Success(response.body()!!)
            } else {
                NetworkResult.Error(ProblemJsonParser.parseProblem(response), response.code())
            }
        } catch (e: Exception) {
            NetworkResult.Exception(e)
        }
    }
}
