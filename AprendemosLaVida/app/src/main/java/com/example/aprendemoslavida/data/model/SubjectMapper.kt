package com.example.aprendemoslavida.data.model

object SubjectMapper {

    const val CODE_NATURAL = "natural-sciences"
    const val CODE_MATH = "mathematics"
    const val CODE_ENGLISH = "english"
    const val CODE_SOCIAL = "social-sciences"
    const val CODE_LANGUAGE = "spanish-language"
    const val CODE_STORY = "story"

    const val TOPIC_MULTIPLICATION = "multiplication"
    const val TOPIC_ADD_SUB = "addition-subtraction"
    const val TOPIC_CASTLES = "castles"

    fun toBackendSubjectCode(localMode: String): String {
        return when (localMode.lowercase().trim()) {
            "natural", "naturales", "natural-sciences" -> CODE_NATURAL
            "math", "matematicas", "matemáticas", "mathematics" -> CODE_MATH
            "english", "ingles", "inglés" -> CODE_ENGLISH
            "social", "sociales", "social-sciences" -> CODE_SOCIAL
            "language", "lengua", "spanish-language" -> CODE_LANGUAGE
            "story", "historia" -> CODE_STORY
            else -> localMode
        }
    }

    fun toLocalMode(backendCode: String): String {
        return when (backendCode.lowercase().trim()) {
            CODE_NATURAL -> "natural"
            CODE_MATH -> "math"
            CODE_ENGLISH -> "english"
            CODE_SOCIAL -> "social"
            CODE_LANGUAGE -> "language"
            CODE_STORY -> "story"
            else -> backendCode
        }
    }
}
