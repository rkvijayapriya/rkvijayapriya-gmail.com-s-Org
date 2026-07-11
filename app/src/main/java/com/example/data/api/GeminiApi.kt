package com.example.data.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.TimeUnit
import com.example.BuildConfig

@JsonClass(generateAdapter = true)
data class InlineData(
    @Json(name = "mimeType") val mimeType: String,
    @Json(name = "data") val data: String
)

@JsonClass(generateAdapter = true)
data class Part(
    @Json(name = "text") val text: String? = null,
    @Json(name = "inlineData") val inlineData: InlineData? = null
)

@JsonClass(generateAdapter = true)
data class Content(
    @Json(name = "parts") val parts: List<Part>
)

@JsonClass(generateAdapter = true)
data class ImageConfig(
    @Json(name = "aspectRatio") val aspectRatio: String? = "1:1",
    @Json(name = "imageSize") val imageSize: String? = "1K"
)

@JsonClass(generateAdapter = true)
data class ThinkingConfig(
    @Json(name = "thinkingLevel") val thinkingLevel: String
)

@JsonClass(generateAdapter = true)
data class Tool(
    @Json(name = "googleSearch") val googleSearch: Map<String, String>? = null
)

@JsonClass(generateAdapter = true)
data class GenerationConfig(
    @Json(name = "temperature") val temperature: Double? = null,
    @Json(name = "topP") val topP: Double? = null,
    @Json(name = "topK") val topK: Int? = null,
    @Json(name = "imageConfig") val imageConfig: ImageConfig? = null,
    @Json(name = "responseModalities") val responseModalities: List<String>? = null,
    @Json(name = "thinkingConfig") val thinkingConfig: ThinkingConfig? = null
)

@JsonClass(generateAdapter = true)
data class GenerateContentRequest(
    @Json(name = "contents") val contents: List<Content>,
    @Json(name = "generationConfig") val generationConfig: GenerationConfig? = null,
    @Json(name = "systemInstruction") val systemInstruction: Content? = null,
    @Json(name = "tools") val tools: List<Tool>? = null
)

@JsonClass(generateAdapter = true)
data class Candidate(
    @Json(name = "content") val content: Content
)

@JsonClass(generateAdapter = true)
data class GenerateContentResponse(
    @Json(name = "candidates") val candidates: List<Candidate>? = null
)

@JsonClass(generateAdapter = true)
data class VideoConfig(
    @Json(name = "aspectRatio") val aspectRatio: String? = "16:9",
    @Json(name = "durationSeconds") val durationSeconds: Int? = 5
)

@JsonClass(generateAdapter = true)
data class GenerateVideosRequest(
    @Json(name = "prompt") val prompt: String,
    @Json(name = "videoConfig") val videoConfig: VideoConfig? = null
)

@JsonClass(generateAdapter = true)
data class VideoMedia(
    @Json(name = "uri") val uri: String? = null,
    @Json(name = "bytes") val bytes: String? = null
)

@JsonClass(generateAdapter = true)
data class GeneratedVideo(
    @Json(name = "video") val video: VideoMedia? = null
)

@JsonClass(generateAdapter = true)
data class GenerateVideosResponse(
    @Json(name = "generatedVideos") val generatedVideos: List<GeneratedVideo>? = null
)

interface GeminiApiService {
    @POST("v1beta/models/{model}:generateContent")
    suspend fun generateContent(
        @Path("model") model: String,
        @Query("key") apiKey: String,
        @Body request: GenerateContentRequest
    ): GenerateContentResponse

    @POST("v1beta/models/{model}:generateVideos")
    suspend fun generateVideos(
        @Path("model") model: String,
        @Query("key") apiKey: String,
        @Body request: GenerateVideosRequest
    ): GenerateVideosResponse
}

object RetrofitClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    val service: GeminiApiService by lazy {
        retrofit.create(GeminiApiService::class.java)
    }
}

class GeminiRepository {
    private val apiService = RetrofitClient.service
    
    // Default model used: gemini-3.5-flash
    private val modelName = "gemini-3.5-flash"

    suspend fun generateCreativeContent(prompt: String, systemInstruction: String? = null): String {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return "Error: API Key is not configured in Secrets Panel. Please set GEMINI_API_KEY."
        }

        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = prompt)))),
            generationConfig = GenerationConfig(temperature = 0.7),
            systemInstruction = systemInstruction?.let { Content(parts = listOf(Part(text = it))) }
        )

        return try {
            val response = apiService.generateContent(modelName, apiKey, request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text 
                ?: "No valid response generated."
        } catch (e: Exception) {
            "Exception during generation: ${e.localizedMessage ?: e.message}"
        }
    }

    suspend fun generateImageContent(
        prompt: String,
        style: String = "",
        aspectRatio: String = "1:1",
        imageSize: String = "1K",
        model: String = "gemini-2.5-flash-image"
    ): String? {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return null
        }

        val formattedPrompt = if (style.isNotBlank()) "$prompt, in $style style" else prompt
        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = formattedPrompt)))),
            generationConfig = GenerationConfig(
                imageConfig = ImageConfig(aspectRatio = aspectRatio, imageSize = imageSize),
                responseModalities = listOf("IMAGE")
            )
        )

        return try {
            val response = apiService.generateContent(model, apiKey, request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull { it.inlineData != null }?.inlineData?.data
        } catch (e: Exception) {
            null
        }
    }

    suspend fun generateVideoContent(
        prompt: String,
        style: String = "",
        aspectRatio: String = "16:9",
        durationSeconds: Int = 5
    ): String? {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return null
        }

        val formattedPrompt = if (style.isNotBlank()) "$prompt, in $style style" else prompt
        val request = GenerateVideosRequest(
            prompt = formattedPrompt,
            videoConfig = VideoConfig(
                aspectRatio = if (aspectRatio == "4K" || aspectRatio == "2K" || aspectRatio == "1080P" || aspectRatio == "720P") "16:9" else aspectRatio,
                durationSeconds = durationSeconds
            )
        )

        return try {
            val response = apiService.generateVideos("veo-3.1-fast-generate-preview", apiKey, request)
            response.generatedVideos?.firstOrNull()?.video?.bytes 
                ?: response.generatedVideos?.firstOrNull()?.video?.uri
        } catch (e: Exception) {
            null
        }
    }

    suspend fun generateContentWithSettings(
        prompt: String,
        model: String = "gemini-3.5-flash",
        systemInstruction: String? = null,
        enableSearchGrounding: Boolean = false,
        thinkingLevel: String? = null,
        inputImageBase64: String? = null,
        inputImageMimeType: String? = "image/jpeg"
    ): String {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return "Error: API Key is not configured in Secrets Panel. Please set GEMINI_API_KEY."
        }

        val parts = mutableListOf<Part>()
        if (inputImageBase64 != null) {
            parts.add(Part(inlineData = InlineData(mimeType = inputImageMimeType ?: "image/jpeg", data = inputImageBase64)))
        }
        parts.add(Part(text = prompt))

        val generationConfig = GenerationConfig(
            temperature = if (thinkingLevel != null && thinkingLevel.lowercase() == "high") null else 0.7,
            thinkingConfig = if (thinkingLevel != null && thinkingLevel.lowercase() == "high") ThinkingConfig(thinkingLevel = "HIGH") else null
        )

        val tools = if (enableSearchGrounding) {
            listOf(Tool(googleSearch = emptyMap()))
        } else {
            null
        }

        val request = GenerateContentRequest(
            contents = listOf(Content(parts = parts)),
            generationConfig = generationConfig,
            systemInstruction = systemInstruction?.let { Content(parts = listOf(Part(text = it))) },
            tools = tools
        )

        return try {
            val response = apiService.generateContent(model, apiKey, request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text 
                ?: "No valid response generated."
        } catch (e: Exception) {
            "Exception during generation: ${e.localizedMessage ?: e.message}"
        }
    }
}
