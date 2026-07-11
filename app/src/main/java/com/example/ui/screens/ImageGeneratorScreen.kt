package com.example.ui.screens

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.ui.components.ToolInputProcessingOverlay
import com.example.ui.viewmodel.ImageGeneratorViewModel
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@Composable
fun ImageGeneratorScreen(
    padding: PaddingValues,
    nav: NavHostController,
    viewModel: ImageGeneratorViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(state.generatedImageUri) {
        val generatedUri: Uri =
            state.generatedImageUri
                ?: return@LaunchedEffect

        val encodedUri =
            URLEncoder.encode(
                generatedUri.toString(),
                StandardCharsets.UTF_8.toString()
            )

        nav.navigate(
            "editor?uri=$encodedUri"
        )

        viewModel.consumeGeneratedImage()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(20.dp),
        verticalArrangement = Arrangement.Top
    ) {
        Text(
            text = "Gemini Image Generator",
            style = MaterialTheme.typography.headlineLarge
        )

        Spacer(
            modifier = Modifier.height(18.dp)
        )

        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = state.prompt,
                onValueChange =
                    viewModel::updatePrompt,
                label = {
                    Text("Describe your image")
                },
                placeholder = {
                    Text(
                        "A cinematic village at sunrise..."
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(170.dp),
                enabled = !state.isGenerating
            )
            if (state.isGenerating) {
                ToolInputProcessingOverlay(
                    message = "Synthesizing artwork parameters...",
                    modifier = Modifier.matchParentSize()
                )
            }
        }

        Spacer(
            modifier = Modifier.height(16.dp)
        )

        Button(
            onClick =
                viewModel::generateImage,
            enabled =
                !state.isGenerating &&
                    state.prompt.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) {
            if (state.isGenerating) {
                CircularProgressIndicator()
            } else {
                Text("Generate Image")
            }
        }

        state.errorMessage?.let { message ->
            Spacer(
                modifier = Modifier.height(14.dp)
            )

            Text(
                text = message,
                color =
                    MaterialTheme.colorScheme.error
            )
        }
    }
}
