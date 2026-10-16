package com.example.aprendemoslavida.data.api

import com.example.aprendemoslavida.data.model.ActivationRequest
import com.example.aprendemoslavida.data.model.ChildDto
import com.example.aprendemoslavida.data.model.ChildInputDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path

interface ProfileApiService {

    @GET("children")
    suspend fun listChildren(): Response<List<ChildDto>>

    @POST("children")
    suspend fun createChild(
        @Header("X-Guardian-Action-Token") guardianToken: String,
        @Body childInput: ChildInputDto
    ): Response<ChildDto>

    @GET("children/{childId}")
    suspend fun getChild(
        @Path("childId") childId: String
    ): Response<ChildDto>

    @PATCH("children/{childId}")
    suspend fun updateChild(
        @Path("childId") childId: String,
        @Header("X-Guardian-Action-Token") guardianToken: String,
        @Body childInput: ChildInputDto
    ): Response<ChildDto>

    @DELETE("children/{childId}")
    suspend fun deleteChild(
        @Path("childId") childId: String,
        @Header("X-Guardian-Action-Token") guardianToken: String
    ): Response<Void>

    @POST("children/{childId}/activate")
    suspend fun activateChild(
        @Path("childId") childId: String,
        @Header("X-Guardian-Action-Token") guardianToken: String? = null,
        @Body activation: ActivationRequest
    ): Response<Void>
}
