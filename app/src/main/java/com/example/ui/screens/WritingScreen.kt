package com.example.ui.screens

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MainViewModel
import com.example.ui.theme.Translation
import com.example.ui.components.ChatBubbleSkeleton
import com.example.ui.components.ToolInputProcessingOverlay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WritingScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val lang by viewModel.currentLanguage.collectAsState()
    val scale by viewModel.fontScale.collectAsState()
    val isGenerating by viewModel.isGenerating.collectAsState()
    val generationProgress by viewModel.generationProgress.collectAsState()

    var prompt by remember { mutableStateOf("") }
    var responseOutput by remember { mutableStateOf("") }
    var selectedModel by remember { mutableStateOf("gemini-3.5-flash") }
    var searchGroundingEnabled by remember { mutableStateOf(false) }
    var thinkingLevel by remember { mutableStateOf("Standard") }

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
            // Header Info with Back Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("writing_back_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Go Back",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
                Column {
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
            }

            // Wrapped inputs with processing skeleton/spinner overlay
            Box(modifier = Modifier.fillMaxWidth()) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
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

                    // Multi-option Clipboard & Interactive Control Bar for user prompt text
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 1. Paste Button
                        AssistActionButton(
                            label = "Paste",
                            icon = Icons.Default.Add,
                            onClick = {
                                try {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    if (clipboard.hasPrimaryClip()) {
                                        val item = clipboard.primaryClip?.getItemAt(0)
                                        val textToPaste = item?.text?.toString() ?: ""
                                        if (textToPaste.isNotBlank()) {
                                            prompt = textToPaste
                                            Toast.makeText(context, "Pasted from clipboard!", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, "Clipboard is empty.", Toast.LENGTH_SHORT).show()
                                        }
                                    } else {
                                        Toast.makeText(context, "No clip inside clipboard.", Toast.LENGTH_SHORT).show()
                                    }
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Paste failed: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                                }
                            },
                            scale = scale
                        )

                        // 2. Select All (Acts as Select All and Copy for ease on modern touch screen layouts)
                        AssistActionButton(
                            label = "Select All",
                            icon = Icons.Default.Info,
                            onClick = {
                                if (prompt.isNotBlank()) {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = android.content.ClipData.newPlainText("All prompt content", prompt)
                                    clipboard.setPrimaryClip(clip)
                                    Toast.makeText(context, "Select All Action: Prompt entirely copied!", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Input is empty.", Toast.LENGTH_SHORT).show()
                                }
                            },
                            scale = scale
                        )

                        // 3. Copy Button
                        AssistActionButton(
                            label = "Copy",
                            icon = Icons.Default.Check,
                            onClick = {
                                if (prompt.isNotBlank()) {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = android.content.ClipData.newPlainText("Copied prompt", prompt)
                                    clipboard.setPrimaryClip(clip)
                                    Toast.makeText(context, "Copied prompt to clipboard!", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Input is empty.", Toast.LENGTH_SHORT).show()
                                }
                            },
                            scale = scale
                        )

                        // 4. Cut Button
                        AssistActionButton(
                            label = "Cut",
                            icon = Icons.Default.Edit,
                            onClick = {
                                if (prompt.isNotBlank()) {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = android.content.ClipData.newPlainText("Copied prompt", prompt)
                                    clipboard.setPrimaryClip(clip)
                                    prompt = ""
                                    Toast.makeText(context, "Prompt cut to clipboard!", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Input is empty.", Toast.LENGTH_SHORT).show()
                                }
                            },
                            scale = scale
                        )

                        // 5. Share Button
                        AssistActionButton(
                            label = "Share",
                            icon = Icons.Default.Share,
                            onClick = {
                                if (prompt.isNotBlank()) {
                                    val intent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_TEXT, prompt)
                                    }
                                    context.startActivity(Intent.createChooser(intent, "Share Prompt"))
                                } else {
                                    Toast.makeText(context, "Nothing to share.", Toast.LENGTH_SHORT).show()
                                }
                            },
                            scale = scale
                        )

                        // 6. Clear Button
                        AssistActionButton(
                            label = "Clear",
                            icon = Icons.Default.Delete,
                            onClick = {
                                prompt = ""
                                Toast.makeText(context, "Cleared input field.", Toast.LENGTH_SHORT).show()
                            },
                            scale = scale,
                            isDanger = true
                        )
                    }

                    // Advanced Intelligence Settings Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = "Advanced Intelligence Settings",
                                fontWeight = FontWeight.Bold,
                                fontSize = (13 * scale).sp,
                                color = MaterialTheme.colorScheme.primary
                            )

                            // 1. MODEL SELECTOR
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "Language Model Engine",
                                    fontSize = (11 * scale).sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(10.dp))
                                        .padding(2.dp),
                                    horizontalArrangement = Arrangement.SpaceAround
                                ) {
                                    listOf(
                                        "gemini-3.5-flash" to "General (Flash)",
                                        "gemini-3.1-pro-preview" to "Advanced (Pro)",
                                        "gemini-3.1-flash-lite" to "Lite (Fast)"
                                    ).forEach { (modelKey, label) ->
                                        val isSelected = selectedModel == modelKey
                                        Text(
                                            text = label,
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                            fontSize = (10 * scale).sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                                                .clickable { selectedModel = modelKey }
                                                .padding(vertical = 8.dp),
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }

                            // 2. SEARCH GROUNDING & THINKING BUDGET ROW
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Google Search Grounding Card
                                Card(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { searchGroundingEnabled = !searchGroundingEnabled },
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (searchGroundingEnabled) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface
                                    ),
                                    border = BorderStroke(
                                        1.dp,
                                        if (searchGroundingEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                                    ),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Search,
                                                contentDescription = null,
                                                tint = if (searchGroundingEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Text(
                                                text = "Search",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = (11 * scale).sp,
                                                color = if (searchGroundingEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                        Text(
                                            text = "Ground with live Google Search.",
                                            fontSize = (9 * scale).sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                        )
                                    }
                                }

                                // High Thinking Mode Card
                                val thinkingAllowed = selectedModel == "gemini-3.1-pro-preview"
                                val isHighThinking = thinkingLevel == "High" && thinkingAllowed
                                Card(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable(enabled = thinkingAllowed) {
                                            thinkingLevel = if (thinkingLevel == "High") "Standard" else "High"
                                        },
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isHighThinking) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface,
                                        disabledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                                    ),
                                    border = BorderStroke(
                                        1.dp,
                                        if (isHighThinking) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                                    ),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Info,
                                                contentDescription = null,
                                                tint = if (isHighThinking) MaterialTheme.colorScheme.primary else if (thinkingAllowed) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Text(
                                                text = "High Thinking",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = (11 * scale).sp,
                                                color = if (isHighThinking) MaterialTheme.colorScheme.primary else if (thinkingAllowed) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                                            )
                                        }
                                        Text(
                                            text = if (thinkingAllowed) "Reasoning steps." else "Requires Pro model.",
                                            fontSize = (9 * scale).sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                if (isGenerating) {
                    ToolInputProcessingOverlay(
                        message = "Gemini is synthesizing content...",
                        modifier = Modifier.matchParentSize()
                    )
                }
            }

            // Submit Button
            Button(
                onClick = {
                    if (prompt.isBlank()) {
                        Toast.makeText(context, "Please enter what to write.", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    viewModel.triggerWritingAssistantWithSettings(
                        prompt = prompt,
                        assistantType = selectedAspect,
                        model = selectedModel,
                        enableSearchGrounding = searchGroundingEnabled,
                        thinkingLevel = if (selectedModel == "gemini-3.1-pro-preview" && thinkingLevel == "High") "high" else null
                    ) { out ->
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
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(32.dp)
                    ) {
                        if (isGenerating) {
                            CircularProgressIndicator(
                                progress = { generationProgress },
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(16.dp)
                        )
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

                            // Quick Copy Short-cut
                            IconButton(onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = android.content.ClipData.newPlainText("Writing output", responseOutput)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, Translation.getString("copied", lang), Toast.LENGTH_SHORT).show()
                            }) {
                                Icon(Icons.Default.Check, contentDescription = "Copy Short-cut", tint = MaterialTheme.colorScheme.primary)
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                        if (isGenerating) {
                            ChatBubbleSkeleton()
                        } else {
                            SelectionContainer {
                                Text(
                                    text = responseOutput,
                                    fontSize = (13 * scale).sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    lineHeight = 20.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Prominent "Copy to Clipboard" Button
                            Button(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = android.content.ClipData.newPlainText("Generated Output", responseOutput)
                                    clipboard.setPrimaryClip(clip)
                                    Toast.makeText(context, "Copied to clipboard successfully!", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                ),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("copy_to_clipboard_button")
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Copy to Clipboard",
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "Copy to Clipboard",
                                        fontSize = (14 * scale).sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Bottom actions row for output text: copy, select all, share
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // 1. Copy All Text
                                OutputActionButton(
                                    label = "Copy All",
                                    icon = Icons.Default.Check,
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        val clip = android.content.ClipData.newPlainText("Generated Output", responseOutput)
                                        clipboard.setPrimaryClip(clip)
                                        Toast.makeText(context, "Full text copied to clipboard successfully!", Toast.LENGTH_SHORT).show()
                                    },
                                    scale = scale
                                )

                                // 2. Select All & Copy
                                OutputActionButton(
                                    label = "Select All",
                                    icon = Icons.Default.Info,
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        val clip = android.content.ClipData.newPlainText("Generated Output", responseOutput)
                                        clipboard.setPrimaryClip(clip)
                                        Toast.makeText(context, "Select All Action: Complete content loaded to clipboard!", Toast.LENGTH_SHORT).show()
                                    },
                                    scale = scale
                                )

                                // 3. Share Externally
                                OutputActionButton(
                                    label = "Share Output",
                                    icon = Icons.Default.Share,
                                    onClick = {
                                        val intent = Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(Intent.EXTRA_SUBJECT, "Gemini $selectedAspect Creative Draft")
                                            putExtra(Intent.EXTRA_TEXT, responseOutput)
                                        }
                                        context.startActivity(Intent.createChooser(intent, "Share Writing Output"))
                                    },
                                    scale = scale
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(64.dp))
        }
    }
}

@Composable
fun AssistActionButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    scale: Float,
    isDanger: Boolean = false
) {
    val contentColor = if (isDanger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    val containerColor = if (isDanger) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f) else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
    val borderColor = if (isDanger) MaterialTheme.colorScheme.error.copy(alpha = 0.3f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(containerColor)
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size((14 * scale).dp)
            )
            Text(
                text = label,
                fontSize = (11 * scale).sp,
                fontWeight = FontWeight.Bold,
                color = contentColor
            )
        }
    }
}

@Composable
fun OutputActionButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    scale: Float
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f))
            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size((12 * scale).dp)
            )
            Text(
                text = label,
                fontSize = (10 * scale).sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
