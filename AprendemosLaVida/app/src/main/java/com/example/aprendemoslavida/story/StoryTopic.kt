package com.example.aprendemoslavida.story

// Topics covered by story checkpoints. Mirrors the existing quiz subjects.
enum class StoryTopic {
    NATURAL,
    MATH_MULTIPLICATION,
    MATH_ADD_SUB,
    ENGLISH,
    SOCIAL,
    LANGUAGE,
    CASTLES;

    /**
     * Value expected by POST /story-sessions `gateTopics` (contract enum). It is the enum name,
     * not the subject/topic code used by game-sessions (natural-sciences, multiplication...).
     */
    val apiCode: String get() = name
}
