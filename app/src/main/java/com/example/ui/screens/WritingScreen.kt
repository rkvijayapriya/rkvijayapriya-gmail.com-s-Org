package com.example.ui.screens

import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MainViewModel
import com.example.ui.theme.Translation

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WritingScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val lang by viewModel.currentLanguage.collectAsState()
    val scale by viewModel.fontScale.collectAsState()
    val isGenerating by viewModel.isGenerating.collectAsState()

    var prompt by remember { mutableStateOf("") }
    var responseOutput by remember { mutableStateOf("") }

    // Writing Presets
    val writingTemplates = listOf(
        "Script Generator" to "Write a cinematic science fiction video script dealing with deep space portals.",
        "Description Generator" to "Outline a stunning scenic landscape video of Swiss Alps under aurora glow.",
        "Story Generator" to "A short story of a clever cybernetic kitten discovering a forgotten computer lab.",
        "Hashtag Generator" to "Generate highly explosive trending hashtags for an AI visual effects video.",
        "Title Generator" to "Catchy clickworthy video titles for a tech tutorial about building with local databases.",
        "YouTube Description" to "Optimized YouTube descriptive text and keyword list for an immersive tech presentation.",
        "Instagram Caption" to "A sleek engaging minimalist Instagram caption with matching emojis about creative freedom.",
        "Marketing Content" to "High converting viral commercial copy promoting an advanced video rendering Android tool."
    )

    var selectedAspect by remember { mutableStateOf("Script Generator") }

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
                    text = Translation.getString("writing_desk", lang),
                    fontSize = (22 * scale).sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "ChatGPT & Gemini Powered Interactive Desk",
                    fontSize = (12 * scale).sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
            }

            // Quick select template row
            Text(
                text = "Creative Presets",
                fontWeight = FontWeight.SemiBold,
                fontSize = (14 * scale).sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                modifier = Modifier.align(Alignment.Start)
            )

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(writingTemplates) { template ->
                    val isSelected = selectedAspect == template.first
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                            )
                            .clickable {
                                selectedAspect = template.first
                                prompt = template.second
                            }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = template.first,
                            fontSize = (12 * scale).sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Text input container
            OutlinedTextField(
                value = prompt,
                onValueChange = { prompt = it },
                placeholder = {
                    Text(
                        text = Translation.getString("writing_placeholder", lang),
                        fontSize = (13 * scale).sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 120.dp)
                    .testTag("writing_prompt_field"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f),
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(24.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { /* Done */ })
            )

            // Submit Button
            Button(
                onClick = {
                    if (prompt.isBlank()) {
                        Toast.makeText(context, "Please enter what to write.", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    viewModel.triggerWritingAssistant(prompt, selectedAspect) { out ->
                        responseOutput = out
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
                    .testTag("writing_submit_button")
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isGenerating) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.Send, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(18.dp))
                    }
                    Text(
                        text = if (isGenerating) "Synthesizing..." else "Send to Gemini",
                        fontSize = (14 * scale).sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }

            // Output Display Card
            if (responseOutput.isNotBlank() || isGenerating) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.Star, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                Text(
                                    text = "Generated $selectedAspect Output",
                                    fontSize = (13 * scale).sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            // Copy option
                            IconButton(onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = android.content.ClipData.newPlainText("Writing output", responseOutput)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, Translation.getString("copied", lang), Toast.LENGTH_SHORT).show()
                            }) {
                                Icon(Icons.Default.Share, contentDescription = "Copy Content", tint = MaterialTheme.colorScheme.primary)
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                        if (isGenerating) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                LinearProgressIndicator(color = MaterialTheme.colorScheme.primary, modifier = Modifier.fillMaxWidth())
                                Text("Creative Assistant is thinking...", fontSize = (12 * scale).sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                            }
                        } else {
                            Text(
                                text = responseOutput,
                                fontSize = (13 * scale).sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                lineHeight = 20.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(64.dp))
        }
    }
}
