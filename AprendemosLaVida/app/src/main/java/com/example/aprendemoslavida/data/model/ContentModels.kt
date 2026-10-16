package com.example.aprendemoslavida.data.model

import com.google.gson.annotations.SerializedName

data class ContentManifest(
    @SerializedName("curriculum") val curriculum: String,
    @SerializedName("release") val release: String,
    @SerializedName("locale") val locale: String,
    @SerializedName("hash") val hash: String,
    @SerializedName("minimumClientVersion") val minimumClientVersion: Int,
    @SerializedName("publishedAt") val publishedAt: String,
    @SerializedName("downloadUrl") val downloadUrl: String
)

data class ContentRelease(
    @SerializedName("curriculum") val curriculum: String,
    @SerializedName("release") val release: String,
    @SerializedName("locale") val locale: String,
    @SerializedName("hash") val hash: String,
    @SerializedName("questions") val questions: List<ContentQuestion>
)

data class ContentQuestion(
    @SerializedName("id") val id: String,
    @SerializedName("stableKey") val stableKey: String,
    @SerializedName("subject") val subject: String,
    @SerializedName("topic") val topic: String,
    @SerializedName("kind") val kind: String, // MULTIPLE_CHOICE, ARITHMETIC, GENERATOR
    @SerializedName("gradeMin") val gradeMin: Int,
    @SerializedName("gradeMax") val gradeMax: Int,
    @SerializedName("prompt") val prompt: String,
    @SerializedName("generationRule") val generationRule: Map<String, Any>? = null,
    @SerializedName("options") val options: List<ContentOption>
)

data class ContentOption(
    @SerializedName("id") val id: String,
    @SerializedName("stableKey") val stableKey: String,
    @SerializedName("text") val text: String
)
