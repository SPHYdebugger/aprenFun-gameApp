package com.example.aprendemoslavida.data.network

import com.example.aprendemoslavida.data.model.ProblemJson
import com.google.gson.Gson
import retrofit2.Response

object ProblemJsonParser {

    private val gson = Gson()

    fun <T> parseProblem(response: Response<T>): ProblemJson {
        val errorBody = response.errorBody()?.string()
        if (!errorBody.isNullOrBlank()) {
            try {
                return gson.fromJson(errorBody, ProblemJson::class.java)
            } catch (_: Exception) {
            }
        }
        return ProblemJson(
            type = "about:blank",
            title = "HTTP Error ${response.code()}",
            status = response.code(),
            detail = if (response.message().isNullOrBlank()) "Error en la comunicación con el servidor (${response.code()})" else response.message()
        )
    }
}
