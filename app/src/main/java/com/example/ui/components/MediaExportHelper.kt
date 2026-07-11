package com.example.ui.components

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Base64
import com.example.data.database.VisionAiCreation
import java.io.File
import java.io.FileOutputStream

object MediaExportHelper {
    fun exportCreation(
        context: Context,
        item: VisionAiCreation,
        onResult: (Boolean, String?) -> Unit
    ) {
        try {
            val resolver = context.contentResolver
            val displayName = "NovaAI_${item.type}_${System.currentTimeMillis()}"
            val mimeType = when (item.type) {
                "IMAGE" -> "image/jpeg"
                "VIDEO" -> "video/mp4"
                "VOICEOVER" -> "audio/wav"
                "WRITING" -> "text/plain"
                else -> "application/octet-stream"
            }
            val extension = when (item.type) {
                "IMAGE" -> ".jpg"
                "VIDEO" -> ".mp4"
                "VOICEOVER" -> ".wav"
                "WRITING" -> ".txt"
                else -> ".bin"
            }
            val fileName = "$displayName$extension"

            // Get bytes to write
            var fileBytes: ByteArray? = null
            var sourceFile: File? = null

            when (item.type) {
                "IMAGE" -> {
                    if (item.responseText.isNotBlank()) {
                        fileBytes = try {
                            Base64.decode(item.responseText, Base64.DEFAULT)
                        } catch (e: Exception) {
                            null
                        }
                    }
                }
                "VIDEO" -> {
                    if (item.responseText.isNotBlank()) {
                        fileBytes = try {
                            Base64.decode(item.responseText, Base64.DEFAULT)
                        } catch (e: Exception) {
                            null
                        }
                    } else if (item.visualUrl.startsWith("data:video/mp4;base64,")) {
                        val base64 = item.visualUrl.substringAfter("base64,")
                        fileBytes = try {
                            Base64.decode(base64, Base64.DEFAULT)
                        } catch (e: Exception) {
                            null
                        }
                    } else if (item.visualUrl.startsWith("data:image/jpeg;base64,")) {
                        val base64 = item.visualUrl.substringAfter("base64,")
                        fileBytes = try {
                            Base64.decode(base64, Base64.DEFAULT)
                        } catch (e: Exception) {
                            null
                        }
                    }
                }
                "VOICEOVER" -> {
                    if (item.responseText.isNotBlank()) {
                        val file = File(item.responseText)
                        if (file.exists()) {
                            sourceFile = file
                        }
                    }
                }
                "WRITING" -> {
                    if (item.responseText.isNotBlank()) {
                        fileBytes = item.responseText.toByteArray(Charsets.UTF_8)
                    }
                }
            }

            // Perform saving in a background thread to prevent UI stutter
            val thread = Thread {
                try {
                    var bytesToWrite = fileBytes
                    if (bytesToWrite == null && sourceFile != null) {
                        bytesToWrite = try {
                            sourceFile.readBytes()
                        } catch (e: Exception) {
                            null
                        }
                    }

                    // Try local file url as a final fallback
                    if (bytesToWrite == null && item.visualUrl.startsWith("file://")) {
                        val localPath = item.visualUrl.removePrefix("file://")
                        val localFile = File(localPath)
                        if (localFile.exists()) {
                            bytesToWrite = try {
                                localFile.readBytes()
                            } catch (e: Exception) {
                                null
                            }
                        }
                    }

                    // Try downloading if it's a web URL
                    if (bytesToWrite == null && item.type == "VIDEO" && item.visualUrl.startsWith("http")) {
                        bytesToWrite = try {
                            java.net.URL(item.visualUrl).readBytes()
                        } catch (e: Exception) {
                            null
                        }
                    }

                    if (bytesToWrite == null) {
                        onResult(false, "No valid media content found to export.")
                        return@Thread
                    }

                    var success = false
                    var savedPath: String? = null

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        val contentValues = ContentValues().apply {
                            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                        }

                        val collectionUri = MediaStore.Downloads.EXTERNAL_CONTENT_URI
                        val itemUri = resolver.insert(collectionUri, contentValues)

                        if (itemUri != null) {
                            resolver.openOutputStream(itemUri)?.use { outputStream ->
                                outputStream.write(bytesToWrite)
                                success = true
                                savedPath = "Downloads/$fileName"
                            }
                        }
                    } else {
                        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                        if (!downloadsDir.exists()) {
                            downloadsDir.mkdirs()
                        }
                        val destFile = File(downloadsDir, fileName)
                        FileOutputStream(destFile).use { outputStream ->
                            outputStream.write(bytesToWrite)
                            success = true
                            savedPath = destFile.absolutePath
                        }
                    }

                    if (success) {
                        onResult(true, savedPath)
                    } else {
                        onResult(false, "Could not write file to device storage.")
                    }
                } catch (e: Exception) {
                    onResult(false, e.localizedMessage ?: "Storage write failure.")
                }
            }
            thread.start()
        } catch (e: Exception) {
            onResult(false, e.localizedMessage ?: "Initialization failure.")
        }
    }
}
