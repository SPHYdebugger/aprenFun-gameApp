package com.example.aprendemoslavida.story

import android.graphics.RectF

// Gate in tile coordinates. Locked gates stop movement until the question is solved.
data class StoryGate(
    val id: Int,
    val topic: StoryTopic,
    val rect: RectF,
    var unlocked: Boolean = false,
    val optional: Boolean = false
)
