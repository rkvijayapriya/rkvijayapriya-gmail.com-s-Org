package com.example.data.repository

import android.content.Context
import android.net.Uri
import android.util.Base64
import androidx.core.content.FileProvider
import com.example.BuildConfig
import com.example.data.remote.GeminiContent
import com.example.data.remote.GeminiGenerationConfig
import com.example.data.remote.GeminiImageRequest
import com.example.data.remote.GeminiRequestPart
import com.example.data.remote.GeminiApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class GeminiImageRepository(
    private val context: Context
) {

    suspend fun generateImage(
        prompt: String
    ): Result<Uri> = withContext(Dispatchers.IO) {

        runCatching {
            require(prompt.isNotBlank()) {
                "Image prompt காலியாக உள்ளது."
            }

            require(BuildConfig.GEMINI_API_KEY.isNotBlank()) {
                "Gemini API key அமைக்கப்படவில்லை."
            }

            val request = GeminiImageRequest(
                contents = listOf(
                    GeminiContent(
                        parts = listOf(
                            GeminiRequestPart(
                                text = prompt.trim()
                            )
                        )
                    )
                ),
                generationConfig =
                    GeminiGenerationConfig()
            )

            val response =
                GeminiApiClient.imageApi.generateImage(
                    apiKey =
                        BuildConfig.GEMINI_API_KEY,
                    request = request
                )

            val base64Image =
                response.candidates
                    ?.firstOrNull()
                    ?.content
                    ?.parts
                    ?.firstOrNull {
                        !it.inlineData?.data.isNullOrBlank()
                    }
                    ?.inlineData
                    ?.data
                    ?: error(
                        "Gemini image data கிடைக்கவில்லை."
                    )

            val imageBytes =
                Base64.decode(
                    base64Image,
                    Base64.DEFAULT
                )

            val imageDirectory =
                File(
                    context.filesDir,
                    "generated_images"
                ).apply {
                    mkdirs()
                }

            val outputFile =
                File(
                    imageDirectory,
                    "nova_${System.currentTimeMillis()}.png"
                )

            outputFile.writeBytes(imageBytes)

            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                outputFile
            )
        }
    }
}
