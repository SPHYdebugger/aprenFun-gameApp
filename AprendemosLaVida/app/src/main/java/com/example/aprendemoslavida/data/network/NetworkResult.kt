package com.example.aprendemoslavida.data.network

import com.example.aprendemoslavida.data.model.ProblemJson

sealed class NetworkResult<out T> {
    data class Success<out T>(val data: T) : NetworkResult<T>()
    data class Error(val problem: ProblemJson, val statusCode: Int) : NetworkResult<Nothing>()
    data class Exception(val throwable: Throwable) : NetworkResult<Nothing>()
}
