package com.example.data.remote

import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query

interface GeminiImageApi {

    @POST(
        "v1beta/models/" +
            "gemini-2.5-flash-image:generateContent"
    )
    suspend fun generateImage(
        @Query("key") apiKey: String,
        @Body request: GeminiImageRequest
    ): GeminiImageResponse
}
