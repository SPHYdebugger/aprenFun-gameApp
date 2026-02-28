package com.example.aprendemoslavida.model

data class MathQuestion(
    val a: Int,
    val b: Int,
    val correctAnswer: Int,
    val options: List<Int>,
    val correctIndex: Int
)
