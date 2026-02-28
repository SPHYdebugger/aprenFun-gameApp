package com.example.aprendemoslavida.model

data class ArithmeticQuestion(
    val expression: String,
    val correctAnswer: Int,
    val options: List<Int>,
    val correctIndex: Int
)
