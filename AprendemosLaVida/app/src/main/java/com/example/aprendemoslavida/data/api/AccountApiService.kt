package com.example.aprendemoslavida.data.api

import com.example.aprendemoslavida.data.model.AcceptedResponse
import com.example.aprendemoslavida.data.model.AccountDto
import retrofit2.Response
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header

interface AccountApiService {

    @GET("me")
    suspend fun getCurrentAccount(): Response<AccountDto>

    @DELETE("me")
    suspend fun scheduleAccountDeletion(
        @Header("X-Guardian-Action-Token") guardianToken: String
    ): Response<AcceptedResponse>
}
