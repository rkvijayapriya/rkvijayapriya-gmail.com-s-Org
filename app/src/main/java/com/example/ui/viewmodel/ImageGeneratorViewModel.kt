package com.example.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repository.GeminiImageRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ImageGeneratorUiState(
    val prompt: String = "",
    val isGenerating: Boolean = false,
    val generatedImageUri: Uri? = null,
    val errorMessage: String? = null
)

class ImageGeneratorViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val repository =
        GeminiImageRepository(application)

    private val _uiState =
        MutableStateFlow(
            ImageGeneratorUiState()
        )

    val uiState: StateFlow<ImageGeneratorUiState> =
        _uiState.asStateFlow()

    fun updatePrompt(value: String) {
        _uiState.value =
            _uiState.value.copy(
                prompt = value,
                errorMessage = null
            )
    }

    fun generateImage() {
        val prompt = _uiState.value.prompt.trim()

        if (prompt.isBlank()) {
            _uiState.value =
                _uiState.value.copy(
                    errorMessage =
                        "Image prompt எழுதுங்கள்."
                )
            return
        }

        viewModelScope.launch {
            _uiState.value =
                _uiState.value.copy(
                    isGenerating = true,
                    generatedImageUri = null,
                    errorMessage = null
                )

            repository
                .generateImage(prompt)
                .onSuccess { uri ->
                    _uiState.value =
                        _uiState.value.copy(
                            isGenerating = false,
                            generatedImageUri = uri
                        )
                }
                .onFailure { error ->
                    _uiState.value =
                        _uiState.value.copy(
                            isGenerating = false,
                            errorMessage =
                                error.message
                                    ?: "Image generation failed."
                        )
                }
        }
    }

    fun consumeGeneratedImage() {
        _uiState.value =
            _uiState.value.copy(
                generatedImageUri = null
            )
    }
}
