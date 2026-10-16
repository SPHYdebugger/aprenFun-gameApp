package com.example.aprendemoslavida.data.repository

import com.example.aprendemoslavida.BuildConfig
import com.example.aprendemoslavida.data.api.ProfileApiService
import com.example.aprendemoslavida.data.model.ActivationRequest
import com.example.aprendemoslavida.data.model.ChildDto
import com.example.aprendemoslavida.data.model.ChildInputDto
import com.example.aprendemoslavida.data.network.NetworkResult
import com.example.aprendemoslavida.data.network.ProblemJsonParser
import com.example.aprendemoslavida.data.storage.TokenStorage

class ProfileRepository(
    private val profileApiService: ProfileApiService,
    private val tokenStorage: TokenStorage
) {

    suspend fun listChildren(): NetworkResult<List<ChildDto>> {
        return try {
            val response = profileApiService.listChildren()
            if (response.isSuccessful && response.body() != null) {
                NetworkResult.Success(response.body()!!)
            } else {
                NetworkResult.Error(ProblemJsonParser.parseProblem(response), response.code())
            }
        } catch (e: Exception) {
            NetworkResult.Exception(e)
        }
    }

    suspend fun createChild(
        guardianToken: String,
        pseudonym: String,
        age: Int
    ): NetworkResult<ChildDto> {
        return try {
            val response = profileApiService.createChild(
                guardianToken = guardianToken,
                childInput = ChildInputDto(pseudonym = pseudonym, age = age, version = 0)
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

    suspend fun getChild(childId: String): NetworkResult<ChildDto> {
        return try {
            val response = profileApiService.getChild(childId)
            if (response.isSuccessful && response.body() != null) {
                NetworkResult.Success(response.body()!!)
            } else {
                NetworkResult.Error(ProblemJsonParser.parseProblem(response), response.code())
            }
        } catch (e: Exception) {
            NetworkResult.Exception(e)
        }
    }

    suspend fun updateChild(
        childId: String,
        guardianToken: String,
        pseudonym: String,
        age: Int,
        version: Int
    ): NetworkResult<ChildDto> {
        return try {
            val response = profileApiService.updateChild(
                childId = childId,
                guardianToken = guardianToken,
                childInput = ChildInputDto(pseudonym = pseudonym, age = age, version = version)
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

    suspend fun deleteChild(childId: String, guardianToken: String): NetworkResult<Unit> {
        return try {
            val response = profileApiService.deleteChild(childId, guardianToken)
            if (response.isSuccessful) {
                if (tokenStorage.getActiveChildId() == childId) {
                    tokenStorage.saveActiveChildId(null)
                }
                NetworkResult.Success(Unit)
            } else {
                NetworkResult.Error(ProblemJsonParser.parseProblem(response), response.code())
            }
        } catch (e: Exception) {
            NetworkResult.Exception(e)
        }
    }

    suspend fun activateChild(
        childId: String,
        guardianToken: String? = null
    ): NetworkResult<Unit> {
        return try {
            val response = profileApiService.activateChild(
                childId = childId,
                guardianToken = guardianToken,
                activation = ActivationRequest(
                    platform = "ANDROID",
                    appVersion = BuildConfig.VERSION_NAME
                )
            )
            if (response.isSuccessful) {
                tokenStorage.saveActiveChildId(childId)
                NetworkResult.Success(Unit)
            } else {
                NetworkResult.Error(ProblemJsonParser.parseProblem(response), response.code())
            }
        } catch (e: Exception) {
            NetworkResult.Exception(e)
        }
    }
}
