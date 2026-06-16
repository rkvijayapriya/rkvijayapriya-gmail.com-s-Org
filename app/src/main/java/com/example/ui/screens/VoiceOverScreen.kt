package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MainViewModel
import com.example.ui.theme.Translation
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceOverScreen(
    viewModel: MainViewModel,
    onNavigateToPlayer: () -> Unit
) {
    val context = LocalContext.current
    val lang by viewModel.currentLanguage.collectAsState()
    val scale by viewModel.fontScale.collectAsState()
    val isGenerating by viewModel.isGenerating.collectAsState()

    var textInput by remember { mutableStateOf("") }
    var voiceGender by remember { mutableStateOf("FEMALE") } // MALE vs FEMALE or emotional presets
    
    val languages = listOf("English", "Tamil", "Hindi", "Spanish", "French", "German", "Japanese")
    var selectedLanguage by remember { mutableStateOf("English") }

    val emotions = listOf("Friendly", "Energetic", "Relaxed", "Whisper", "Dramatic / Deep", "Monotone News")
    var selectedEmotion by remember { mutableStateOf("Dramatic / Deep") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Info
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = Translation.getString("voiceover_synthesis", lang),
                    fontSize = (22 * scale).sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Text-to-Speech Emotional Audio Synthesis Engine",
                    fontSize = (12 * scale).sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
            }

            // Input TextArea
            OutlinedTextField(
                value = textInput,
                onValueChange = { textInput = it },
                placeholder = {
                    Text(
                        Translation.getString("voice_placeholder", lang),
                        fontSize = (13 * scale).sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 120.dp)
                    .testTag("voice_text_input"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f),
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(24.dp)
            )

            // Gender Switch
            Text(
                text = Translation.getString("voice_gender", lang),
                fontWeight = FontWeight.Bold,
                fontSize = (14 * scale).sp,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.align(Alignment.Start)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                    .padding(4.dp)
            ) {
                // Male voiceoption
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (voiceGender == "MALE") MaterialTheme.colorScheme.primary else Color.Transparent)
                        .clickable { voiceGender = "MALE" }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = Translation.getString("male", lang),
                        color = if (voiceGender == "MALE") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold,
                        fontSize = (12 * scale).sp
                    )
                }

                // Female option
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (voiceGender == "FEMALE") MaterialTheme.colorScheme.primary else Color.Transparent)
                        .clickable { voiceGender = "FEMALE" }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = Translation.getString("female", lang),
                        color = if (voiceGender == "FEMALE") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold,
                        fontSize = (12 * scale).sp
                    )
                }
            }

            // Language Preset list
            Text(
                text = "Synthesis Accent Language",
                fontWeight = FontWeight.Bold,
                fontSize = (14 * scale).sp,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.align(Alignment.Start)
            )
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(languages) { language ->
                    val isSelected = selectedLanguage == language
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                            )
                            .border(
                                1.dp,
                                if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                RoundedCornerShape(20.dp)
                            )
                            .clickable { selectedLanguage = language }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = language,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = (12 * scale).sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Speech Mood Presets
            Text(
                text = Translation.getString("voice_accent", lang),
                fontWeight = FontWeight.Bold,
                fontSize = (14 * scale).sp,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.align(Alignment.Start)
            )
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(emotions) { emotion ->
                    val isSelected = selectedEmotion == emotion
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                            )
                            .clickable { selectedEmotion = emotion }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = emotion,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = (12 * scale).sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Trigger Button
            Button(
                onClick = {
                    if (textInput.isBlank()) {
                        Toast.makeText(context, "Please enter narration text.", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    viewModel.triggerVoiceOver(
                        text = textInput,
                        voiceGender = if (voiceGender == "MALE") "Male voice" else "Female voice",
                        accent = "$selectedLanguage ($selectedEmotion Accent)"
                    ) {
                        onNavigateToPlayer()
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("voice_generate_button")
            ) {
                Text(
                    "Synthesize & Preview",
                    fontSize = (14 * scale).sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }

            Spacer(modifier = Modifier.height(64.dp))
        }
    }
}
