package com.example.data.remote

import com.google.gson.annotations.SerializedName

data class GeminiImageRequest(
    val contents: List<GeminiContent>,
    @SerializedName("generationConfig")
    val generationConfig: GeminiGenerationConfig
)

data class GeminiContent(
    val role: String = "user",
    val parts: List<GeminiRequestPart>
)

data class GeminiRequestPart(
    val text: String
)

data class GeminiGenerationConfig(
    @SerializedName("responseModalities")
    val responseModalities: List<String> = listOf(
        "TEXT",
        "IMAGE"
    )
)

data class GeminiImageResponse(
    val candidates: List<GeminiCandidate>?
)

data class GeminiCandidate(
    val content: GeminiResponseContent?
)

data class GeminiResponseContent(
    val parts: List<GeminiResponsePart>?
)

data class GeminiResponsePart(
    val text: String? = null,

    @SerializedName("inlineData")
    val inlineData: GeminiInlineData? = null
)

data class GeminiInlineData(
    @SerializedName("mimeType")
    val mimeType: String?,

    val data: String?
)
