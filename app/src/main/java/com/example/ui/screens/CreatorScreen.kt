package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MainViewModel
import com.example.ui.theme.Translation

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatorScreen(
    viewModel: MainViewModel,
    onNavigateToPlayer: () -> Unit
) {
    val context = LocalContext.current
    val lang by viewModel.currentLanguage.collectAsState()
    val scale by viewModel.fontScale.collectAsState()
    val isGenerating by viewModel.isGenerating.collectAsState()
    val progress by viewModel.generationProgress.collectAsState()
    val progressStep by viewModel.generationStep.collectAsState()

    var creationType by remember { mutableStateOf("VIDEO") } // "VIDEO" or "IMAGE"
    var prompt by remember { mutableStateOf("") }
    
    // Style Presets
    val videoStyles = listOf(
        "Cinematic", "Cartoon", "Anime", "Sketch", "Watercolor", "Retro", 
        "Clay Animation", "Sand Art", "Pixar Style", "Disney Style", "Realistic", 
        "3D Render", "Cyberpunk", "Fantasy", "Neon", "Comic Style", 
        "Oil Painting", "Pencil Drawing", "Low Poly", "Studio Quality"
    )
    var selectedStyle by remember { mutableStateOf("Cinematic") }

    // Camera Angles Presets
    val cameraAngles = listOf(
        "Close Up", "Medium Shot", "Wide Shot", "Drone View", "Bird Eye View",
        "Top View", "Side View", "Front View", "Back View", "Tracking Shot",
        "Dolly Zoom", "Pan Shot", "Tilt Shot", "Cinematic Camera"
    )
    var selectedAngle by remember { mutableStateOf("Cinematic Camera") }

    // Quality / FPS / Duration Presets
    val resolutionOptions = listOf("720P", "1080P", "2K", "4K")
    var selectedResolution by remember { mutableStateOf("4K") }

    val fpsOptions = listOf(24, 30, 60)
    var selectedFps by remember { mutableStateOf(30) }

    val durationOptions = listOf(5, 10, 15, 30, 60)
    var selectedDuration by remember { mutableStateOf(10) }

    // Image Reference upload simulation status
    var attachImageSelected by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Logo (Professional Polish theme)
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "STUDIO",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 1.5.sp
                    )
                    Text(
                        text = "VisionAI",
                        fontSize = (24 * scale).sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                // Header Profile Initials
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondaryContainer)
                        .border(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "JD",
                        fontSize = (13 * scale).sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Creator Switch Row (Video vs Image Generator)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp))
                    .padding(4.dp)
            ) {
                listOf("VIDEO", "IMAGE").forEach { type ->
                    val isSelected = creationType == type
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                            .clickable { creationType = type }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (type == "VIDEO") Translation.getString("video", lang) else Translation.getString("image", lang),
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            fontWeight = FontWeight.Bold,
                            fontSize = (14 * scale).sp
                        )
                    }
                }
            }

            // Input prompt Box with attachment
            OutlinedTextField(
                value = prompt,
                onValueChange = { prompt = it },
                placeholder = {
                    Text(
                        text = Translation.getString("prompt_placeholder", lang),
                        fontSize = (13 * scale).sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 120.dp)
                    .testTag("prompt_text_field"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f),
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(24.dp),
                trailingIcon = {
                    IconButton(
                        onClick = {
                            attachImageSelected = !attachImageSelected
                            Toast.makeText(
                                context,
                                if (attachImageSelected) "Image Reference Uploaded!" else "Image reference removed",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    ) {
                        Icon(
                            imageVector = if (attachImageSelected) Icons.Default.CheckCircle else Icons.Default.Add,
                            contentDescription = Translation.getString("upload_image", lang),
                            tint = if (attachImageSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            )

            // Attached reference thumbnail
            AnimatedVisibility(visible = attachImageSelected) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp))
                        .padding(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.secondaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("IMG_REF_AI_STYLE.PNG", color = MaterialTheme.colorScheme.onSurface, fontSize = (12 * scale).sp, fontWeight = FontWeight.Bold)
                        Text("Ready for cinematic neural synthesis", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f), fontSize = (11 * scale).sp)
                    }
                    IconButton(onClick = { attachImageSelected = false }) {
                        Icon(Icons.Default.Close, contentDescription = "Remove", tint = Color.Red.copy(alpha = 0.7f))
                    }
                }
            }

            // Aesthetic Art Style Selector
            Text(
                text = Translation.getString("styles", lang),
                fontWeight = FontWeight.Bold,
                fontSize = (15 * scale).sp,
                color = MaterialTheme.colorScheme.onBackground
            )
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 2.dp)
            ) {
                items(videoStyles) { style ->
                    val isSelected = selectedStyle == style
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                            )
                            .clickable { selectedStyle = style }
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = style,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = (12 * scale).sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            // Camera Angles (Only for video)
            if (creationType == "VIDEO") {
                Text(
                    text = Translation.getString("angles", lang),
                    fontWeight = FontWeight.Bold,
                    fontSize = (15 * scale).sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 2.dp)
                ) {
                    items(cameraAngles) { angle ->
                        val isSelected = selectedAngle == angle
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant
                                )
                                .border(
                                    1.dp,
                                    if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable { selectedAngle = angle }
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = angle,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = (12 * scale).sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // FPS & Duration Options Grid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Frame rate
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = Translation.getString("fps_rate", lang),
                            fontSize = (12 * scale).sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(10.dp)).padding(2.dp),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            fpsOptions.forEach { rate ->
                                val isSelected = selectedFps == rate
                                Text(
                                    text = "$rate FPS",
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                    fontSize = (11 * scale).sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                                        .clickable { selectedFps = rate }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    // Duration
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = Translation.getString("duration", lang),
                            fontSize = (12 * scale).sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(10.dp)).padding(2.dp),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            durationOptions.take(3).forEach { d ->
                                val isSelected = selectedDuration == d
                                Text(
                                    text = "${d}s",
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                    fontSize = (11 * scale).sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                                        .clickable { selectedDuration = d }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Quality Resolution Preset selector
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = Translation.getString("resolution", lang),
                    fontSize = (13 * scale).sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    resolutionOptions.forEach { res ->
                        val isSelected = selectedResolution == res
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant)
                                .border(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                                .clickable { selectedResolution = res }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = res,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = (12 * scale).sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action synthesis trigger
            Button(
                onClick = {
                    if (prompt.isBlank()) {
                        Toast.makeText(context, "Please enter a visual prompt description", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (creationType == "VIDEO") {
                        viewModel.triggerGeneration(
                            prompt = prompt,
                            style = selectedStyle,
                            cameraAngle = selectedAngle,
                            resolution = selectedResolution,
                            fps = selectedFps,
                            duration = selectedDuration,
                            imageSelected = attachImageSelected
                        ) {
                            onNavigateToPlayer()
                        }
                    } else {
                        viewModel.triggerImageGeneration(
                            prompt = prompt,
                            style = selectedStyle
                        ) {
                            onNavigateToPlayer()
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("generate_button")
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Star, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
                    Text(
                        text = Translation.getString("generate", lang),
                        fontSize = (15 * scale).sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(64.dp)) // Padding for bottom navbar
        }

        // Beautiful overlay for background processing of AI compilation
        if (isGenerating) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.7f))
                    .clickable(enabled = false) {}, // Absorbs click touches
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = Translation.getString("generating", lang),
                            fontWeight = FontWeight.Bold,
                            fontSize = (18 * scale).sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )

                        // Circular neon gradient style progress loader
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(80.dp)
                        ) {
                            CircularProgressIndicator(
                                progress = { progress },
                                color = MaterialTheme.colorScheme.primary,
                                strokeWidth = 5.dp,
                                modifier = Modifier.fillMaxSize()
                            )
                            Text(
                                text = "${(progress * 100).toInt()}%",
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold,
                                fontSize = (14 * scale).sp
                            )
                        }

                        // Informative steps tracking (Description / Rendering...)
                        Text(
                            text = progressStep,
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = (13 * scale).sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.animateContentSize()
                        )

                        Text(
                            text = Translation.getString("offline_notice", lang),
                            fontSize = (11 * scale).sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}
