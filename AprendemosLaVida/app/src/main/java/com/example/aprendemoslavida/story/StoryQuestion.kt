package com.example.aprendemoslavida.story

// Lightweight reusable question model used by the story checkpoints.
data class StoryQuestion(
    val text: String,
    val options: List<String>,
    val correctIndex: Int
)
