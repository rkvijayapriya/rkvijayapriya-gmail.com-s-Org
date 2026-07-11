package com.example.data.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Streaming
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.concurrent.TimeUnit
import com.example.BuildConfig

@JsonClass(generateAdapter = true)
data class ElevenLabsVoiceSettings(
    @Json(name = "stability") val stability: Double = 0.5,
    @Json(name = "similarity_boost") val similarityBoost: Double = 0.75
)

@JsonClass(generateAdapter = true)
data class ElevenLabsRequest(
    @Json(name = "text") val text: String,
    @Json(name = "model_id") val modelId: String = "eleven_monolingual_v1",
    @Json(name = "voice_settings") val voiceSettings: ElevenLabsVoiceSettings = ElevenLabsVoiceSettings()
)

interface ElevenLabsApiService {
    @POST("v1/text-to-speech/{voiceId}")
    @Streaming
    suspend fun generateSpeech(
        @Path("voiceId") voiceId: String,
        @Header("xi-api-key") apiKey: String,
        @Body request: ElevenLabsRequest
    ): ResponseBody
}

object ElevenLabsRetrofitClient {
    private const val BASE_URL = "https://api.elevenlabs.io/"

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.HEADERS
        })
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    val service: ElevenLabsApiService by lazy {
        retrofit.create(ElevenLabsApiService::class.java)
    }
}

class ElevenLabsRepository {
    private val apiService = ElevenLabsRetrofitClient.service

    // Default voice: Rachel (polished, high-quality professional female voice)
    val defaultVoiceId = "21m00Tcm4TlvDq8ikWAM"

    /**
     * Calls ElevenLabs API to generate TTS and saves the response stream to a local cache file.
     * Returns the absolute file path if successful, or null if it fails or key is missing.
     */
    suspend fun generateSpeechToFile(
        text: String,
        cacheDir: File,
        voiceId: String = defaultVoiceId
    ): String? {
        val apiKey = BuildConfig.ELEVENLABS_API_KEY
        if (apiKey.isEmpty() || apiKey == "YOUR_ELEVENLABS_API_KEY") {
            return null
        }

        val request = ElevenLabsRequest(text = text)

        return try {
            val responseBody = apiService.generateSpeech(voiceId, apiKey, request)
            val file = File(cacheDir, "elevenlabs_${System.currentTimeMillis()}.mp3")
            
            writeResponseBodyToDisk(responseBody, file)
            file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun writeResponseBodyToDisk(body: ResponseBody, file: File) {
        var inputStream: InputStream? = null
        var outputStream: FileOutputStream? = null
        try {
            inputStream = body.byteStream()
            outputStream = FileOutputStream(file)
            val fileReader = ByteArray(4096)
            while (true) {
                val read = inputStream.read(fileReader)
                if (read == -1) {
                    break
                }
                outputStream.write(fileReader, 0, read)
            }
            outputStream.flush()
        } finally {
            inputStream?.close()
            outputStream?.close()
        }
    }
}
