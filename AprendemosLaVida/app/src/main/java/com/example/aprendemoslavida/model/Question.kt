package com.example.aprendemoslavida.model

data class Question(
    val text: String,
    val options: List<String>,
    val correctIndex: Int
)
