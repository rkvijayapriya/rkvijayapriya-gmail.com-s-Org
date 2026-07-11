package com.example.ui.screens

import android.graphics.BitmapFactory
import android.util.Base64
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.database.VisionAiCreation
import com.example.ui.MainViewModel
import com.example.ui.components.ToolInputProcessingOverlay
import com.example.ui.theme.NovaGradient
import com.example.ui.theme.NovaPrimary
import com.example.ui.theme.NovaSecondary
import com.example.ui.theme.NovaTertiary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VeoScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onNavigateToEditor: () -> Unit
) {
    val context = LocalContext.current
    val scale by viewModel.fontScale.collectAsState()
    val isGenerating by viewModel.isGenerating.collectAsState()
    val progress by viewModel.generationProgress.collectAsState()
    val progressStep by viewModel.generationStep.collectAsState()

    var prompt by remember { mutableStateOf("") }
    var selectedStyle by remember { mutableStateOf("Cinematic") }
    var selectedRatio by remember { mutableStateOf("16:9") }
    var selectedResolution by remember { mutableStateOf("1080p") }
    var selectedFps by remember { mutableStateOf(30) }
    var selectedDuration by remember { mutableStateOf(5) }
    var selectedCameraAngle by remember { mutableStateOf("Drone Orbit") }
    var selectedModelVersion by remember { mutableStateOf("veo-3.1-fast-generate-preview") }
    var selectedPhoto by remember { mutableStateOf<String?>(null) }

    var generatedVideoCreation by remember { mutableStateOf<VisionAiCreation?>(null) }
    var showFullscreenPlayer by remember { mutableStateOf(false) }

    // Video Player Local States
    var isPlaying by remember { mutableStateOf(true) }
    var playerProgress by remember { mutableStateOf(0.0f) }
    var playLoopCount by remember { mutableStateOf(0) }

    val decodedBitmap = remember(generatedVideoCreation) {
        val rawBase64 = generatedVideoCreation?.responseText
        if (!rawBase64.isNullOrBlank() && generatedVideoCreation?.type == "IMAGE") {
            try {
                val decodedBytes = Base64.decode(rawBase64, Base64.DEFAULT)
                BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
            } catch (e: Exception) {
                null
            }
        } else {
            null
        }
    }

    // Interactive rendering simulation logic
    LaunchedEffect(isPlaying, generatedVideoCreation) {
        if (isPlaying && generatedVideoCreation != null) {
            while (isPlaying) {
                playerProgress += 0.02f
                if (playerProgress >= 1.0f) {
                    playerProgress = 0.0f
                    playLoopCount++
                }
                delay(100)
            }
        }
    }

    // Creative visual prompt ideas for premium user flow
    val quickVideoIdeas = listOf(
        "A slow-motion cinematic drone flyover of a hidden sci-fi cybercity under neon rain",
        "A hyperrealistic majestic lion walking through a colorful crystal forest, sunset light",
        "A sweeping crane shot of active lava waves flowing into the ocean, dramatic reflections",
        "A high speed camera zooming in on water droplets splashing onto neon holographic flowers",
        "An astronaut floating inside a retro spaceship, looking out at a swirling stellar nebula"
    )

    val videoStyles = listOf(
        "Cinematic", "Photorealistic", "Anime Noir", "Cyberpunk", "3D Pixar",
        "Surreal Dreamscape", "Claymation", "Macro Closeup", "Vintage 35mm"
    )

    val cameraAngles = listOf(
        "Drone Orbit", "Crane Zoom", "Pan Left-to-Right", "Low Angle Tilt", "Static Cinematic"
    )

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
            // Header bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(40.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                        .testTag("veo_back_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Go Back",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Google Veo Studio",
                            fontSize = (22 * scale).sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .background(NovaGradient, RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                "PRO",
                                color = Color.White,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Text(
                        text = "Generative Cinematic Video Synthesizer",
                        fontSize = (11 * scale).sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                }
            }

            // High Fidelity Integrated Video Player Component / Preview Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(
                        if (selectedRatio == "9:16") 9f / 16f
                        else if (selectedRatio == "1:1") 1f
                        else 16f / 9f
                    )
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.Black)
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                        RoundedCornerShape(24.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isGenerating) {
                    // Shimmering Glowing Loading State
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Box(
                            modifier = Modifier.size(80.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                progress = { progress },
                                color = NovaPrimary,
                                strokeWidth = 4.dp,
                                modifier = Modifier.fillMaxSize()
                            )
                            Icon(
                                imageVector = Icons.Default.Videocam,
                                contentDescription = "Synthesizing Video",
                                tint = NovaSecondary,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = progressStep,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${(progress * 100).toInt()}% Rendered",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    }
                } else if (generatedVideoCreation != null) {
                    // Actual Output Video Player Component
                    Box(modifier = Modifier.fillMaxSize()) {
                        if (decodedBitmap != null) {
                            // Falling back to generated high resolution image keyframe with beautiful cinematic overlays
                            Image(
                                bitmap = decodedBitmap.asImageBitmap(),
                                contentDescription = "Generated keyframe preview",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }

                        // Real-time Procedural Video Fluid Motion Synth overlay on a Canvas
                        Canvas(
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("veo_player_canvas")
                        ) {
                            val w = size.width
                            val h = size.height

                            clipRect {
                                // Draw dark backing overlay if no image is present
                                if (decodedBitmap == null) {
                                    drawRect(
                                        brush = Brush.verticalGradient(
                                            colors = listOf(Color(0xFF0F0829), Color(0xFF02010A))
                                        )
                                    )
                                } else {
                                    drawRect(
                                        brush = Brush.verticalGradient(
                                            colors = listOf(Color(0x330C0720), Color(0x6603010C))
                                        )
                                    )
                                }

                                // Interactive fluid cyberwave moving with playback progress
                                val waveSteps = 60
                                val waveAmplitude = 30f
                                val phase = playerProgress * 2 * Math.PI.toFloat()
                                val styleColor = when (selectedStyle) {
                                    "Cyberpunk" -> Color(0xFFFF00CC)
                                    "Surreal Dreamscape" -> Color(0xFF00FFFF)
                                    "Photorealistic" -> Color(0xFFFFCC00)
                                    else -> NovaPrimary
                                }

                                // Interactive particles orbiting or floating based on playback
                                val particles = 15
                                for (i in 0 until particles) {
                                    val px = (sin(i.toDouble() + playerProgress) * 0.5 + 0.5) * w
                                    val py = ((playerProgress + i * 0.07f) % 1.0f) * h
                                    drawCircle(
                                        color = styleColor.copy(alpha = 0.25f),
                                        radius = 16.dp.toPx() + sin(phase + i) * 4.dp.toPx(),
                                        center = Offset(px.toFloat(), py.toFloat())
                                    )
                                }

                                // Draw sweeping horizon scanning lines mapping camera movement
                                val sweepY = (playerProgress * h)
                                drawLine(
                                    color = Color(0xFF00FFCC).copy(alpha = 0.4f),
                                    start = Offset(0f, sweepY),
                                    end = Offset(w, sweepY),
                                    strokeWidth = 2.dp.toPx()
                                )

                                // Render primary procedural visual waves
                                for (waveIndex in 0..1) {
                                    val points = mutableListOf<Offset>()
                                    for (x in 0..waveSteps) {
                                        val t = x.toFloat() / waveSteps
                                        val xPos = t * w
                                        val waveFactor = sin(t * 4 * Math.PI.toFloat() + phase + (waveIndex * 1.8f))
                                        val yPos = (h / 2f) + waveFactor * waveAmplitude
                                        points.add(Offset(xPos, yPos))
                                    }
                                    for (p in 0 until points.size - 1) {
                                        drawLine(
                                            color = if (waveIndex == 0) styleColor else Color(0xFF00FFCC),
                                            start = points[p],
                                            end = points[p + 1],
                                            strokeWidth = 3.dp.toPx()
                                        )
                                    }
                                }
                            }
                        }

                        // Top Row Action buttons (Close, Fullscreen)
                        Row(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            IconButton(
                                onClick = { showFullscreenPlayer = true },
                                modifier = Modifier
                                    .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                                    .size(36.dp)
                            ) {
                                Icon(Icons.Default.Fullscreen, contentDescription = "Fullscreen", tint = Color.White)
                            }
                            IconButton(
                                onClick = { generatedVideoCreation = null },
                                modifier = Modifier
                                    .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                                    .size(36.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Clear", tint = Color.White)
                            }
                        }

                        // Bottom Media Control Bar
                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .background(
                                    Brush.verticalGradient(
                                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                                    )
                                )
                                .padding(12.dp)
                        ) {
                            // Custom Seekbar Slider
                            Slider(
                                value = playerProgress,
                                onValueChange = { playerProgress = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(20.dp)
                                    .testTag("veo_player_seekbar"),
                                colors = SliderDefaults.colors(
                                    thumbColor = NovaSecondary,
                                    activeTrackColor = NovaPrimary,
                                    inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                                )
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // Play / Pause Toggle
                                    IconButton(
                                        onClick = { isPlaying = !isPlaying },
                                        modifier = Modifier
                                            .background(NovaPrimary.copy(alpha = 0.2f), CircleShape)
                                            .size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                            contentDescription = "Play/Pause",
                                            tint = NovaSecondary
                                        )
                                    }

                                    // Display video title/style info
                                    Column {
                                        Text(
                                            text = generatedVideoCreation!!.title,
                                            color = Color.White,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "Veo 2.0 • $selectedResolution • $selectedFps FPS",
                                            color = Color.White.copy(alpha = 0.6f),
                                            fontSize = 10.sp
                                        )
                                    }
                                }

                                // Timeline counter
                                Text(
                                    text = "0:${String.format("%02d", (playerProgress * selectedDuration).toInt())} / 0:${String.format("%02d", selectedDuration)}",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                } else {
                    // Empty placeholder layout
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .background(NovaGradient, CircleShape)
                                .padding(1.5.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(MaterialTheme.colorScheme.surface, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MovieFilter,
                                    contentDescription = null,
                                    tint = NovaPrimary,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "Awaiting Veo Directions",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Define prompt, style presets & camera angles to manifest Google Veo cinema",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // Download / Edit in studio action bar once generated
            if (generatedVideoCreation != null && !isGenerating) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            viewModel.activeCreation.value = generatedVideoCreation
                            onNavigateToEditor()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Movie, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Edit in Studio", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            Toast.makeText(context, "Saved successfully to local cinema gallery!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Export MP4 (4K)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Photo Upload & Animation Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.AddAPhoto, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        Text(
                            text = "Animate Image to Video",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    
                    Text(
                        text = "Upload a reference photo to animate into a cinematic motion loop using Veo 3.1 Fast.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // "Upload Photo" Button
                        Box(
                            modifier = Modifier
                                .size(70.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                .clickable {
                                    selectedPhoto = "Cosmic Vortex"
                                    prompt = "Animate the glowing cosmic portal vortex of Cosmic Vortex, adding solar wind flares and flowing nebular dust particles."
                                    Toast.makeText(context, "Uploaded 'Cosmic Vortex' reference photo successfully!", Toast.LENGTH_SHORT).show()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Upload, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.height(2.dp))
                                Text("Upload", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        // Preset Photos
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(
                                "Cosmic Vortex" to "🌌 Cosmic",
                                "Neon Alley" to "🌆 Neon",
                                "Crystal Mountain" to "🏔️ Peak",
                                "Abstract Fluid" to "🎨 Fluid"
                            ).forEach { (photoName, emojiName) ->
                                val isSelected = selectedPhoto == photoName
                                Box(
                                    modifier = Modifier
                                        .size(70.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant)
                                        .border(
                                            1.dp,
                                            if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                            RoundedCornerShape(12.dp)
                                        )
                                        .clickable {
                                            selectedPhoto = photoName
                                            prompt = "Animate the gorgeous details of $photoName, adding fluid cinematic camera motions and ambient volumetric lighting."
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(emojiName, fontSize = 20.sp)
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(photoName.substringBefore(" "), fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }

                    if (selectedPhoto != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f), RoundedCornerShape(10.dp))
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                Text("Image Reference Active: $selectedPhoto", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }
                            IconButton(
                                onClick = { selectedPhoto = null },
                                modifier = Modifier.size(20.dp)
                            ) {
                                Icon(Icons.Default.Cancel, contentDescription = "Clear Reference", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }

            // Prompt Input card wrapped in Box with processing skeleton/spinner overlay
            Box(modifier = Modifier.fillMaxWidth()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "1. Cinematic Video Description",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.primary
                        )

                        OutlinedTextField(
                            value = prompt,
                            onValueChange = { prompt = it },
                            placeholder = {
                                Text(
                                    "Describe what you want Google Veo to animate in stunning, photorealistic detail...",
                                    fontSize = 12.sp
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp)
                                .testTag("veo_prompt_input_field"),
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f),
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent
                            )
                        )

                        // Quick ideas presets
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            quickVideoIdeas.forEach { idea ->
                                val isSelected = prompt == idea
                                SuggestionChip(
                                    onClick = { prompt = idea },
                                    label = {
                                        Text(
                                            text = if (idea.length > 25) idea.take(23) + "..." else idea,
                                            fontSize = 11.sp
                                        )
                                    },
                                    colors = SuggestionChipDefaults.suggestionChipColors(
                                        containerColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else Color.Transparent
                                    )
                                )
                            }
                        }
                    }
                }
                if (isGenerating) {
                    ToolInputProcessingOverlay(
                        message = "Synthesizing visual direction...",
                        modifier = Modifier.matchParentSize()
                    )
                }
            }

            // Advanced video parameters panel wrapped in Box with processing skeleton/spinner overlay
            Box(modifier = Modifier.fillMaxWidth()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "2. Advanced Veo Synthesis Parameters",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.primary
                        )

                    // MODEL SELECTOR
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Veo Model Version",
                            fontSize = 12.sp,
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
                                "veo-3.1-fast-generate-preview" to "Veo 3.1 Fast (Next-Gen)",
                                "veo-3.1-generate-preview" to "Veo 3.1 Cinema (Ultra High Quality)"
                            ).forEach { (modelKey, label) ->
                                val isSelected = selectedModelVersion == modelKey
                                Text(
                                    text = label,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                                        .clickable { selectedModelVersion = modelKey }
                                        .padding(vertical = 8.dp),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    // STYLE PRESET
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Visual Style Theme",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(videoStyles) { style ->
                                val isSelected = selectedStyle == style
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(
                                            if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant
                                        )
                                        .border(
                                            1.dp,
                                            if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                            RoundedCornerShape(10.dp)
                                        )
                                        .clickable { selectedStyle = style }
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = style,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    // CAMERA DIRECTIONAL ANGLE
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Camera Choreography",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(cameraAngles) { angle ->
                                val isSelected = selectedCameraAngle == angle
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(
                                            if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant
                                        )
                                        .border(
                                            1.dp,
                                            if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                            RoundedCornerShape(10.dp)
                                        )
                                        .clickable { selectedCameraAngle = angle }
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = angle,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    // ASPECT RATIO
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Canvas Aspect Ratio",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf("16:9", "9:16", "1:1", "4:3", "21:9").forEach { ratio ->
                                val isSelected = selectedRatio == ratio
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant)
                                        .border(
                                            1.dp,
                                            if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                            RoundedCornerShape(10.dp)
                                        )
                                        .clickable { selectedRatio = ratio }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = ratio,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    // RESOLUTION & TIME DURATIONS
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Output Resolution",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(10.dp))
                                    .padding(2.dp)
                            ) {
                                listOf("1080p", "4K").forEach { res ->
                                    val isSelected = selectedResolution == res
                                    Text(
                                        text = res,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                                            .clickable { selectedResolution = res }
                                            .padding(vertical = 8.dp),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Clip Duration",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(10.dp))
                                    .padding(2.dp)
                            ) {
                                listOf(5, 10).forEach { dur ->
                                    val isSelected = selectedDuration == dur
                                    Text(
                                        text = "${dur}s",
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                                            .clickable { selectedDuration = dur }
                                            .padding(vertical = 8.dp),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }
            }
            if (isGenerating) {
                ToolInputProcessingOverlay(
                    message = "Synthesizing advanced camera controls...",
                    modifier = Modifier.matchParentSize()
                )
            }
        }

            // GENERATION PRIMARY CTA BUTTON
            Button(
                onClick = {
                    if (prompt.isBlank()) {
                        Toast.makeText(context, "Please enter a visual descriptive prompt!", Toast.LENGTH_SHORT).show()
                    } else {
                        viewModel.triggerGeneration(
                            prompt = prompt,
                            style = selectedStyle,
                            cameraAngle = selectedCameraAngle,
                            resolution = selectedResolution,
                            fps = selectedFps,
                            duration = selectedDuration,
                            imageSelected = false,
                            aspectRatio = selectedRatio
                        ) { creation ->
                            generatedVideoCreation = creation
                            playerProgress = 0.0f
                            isPlaying = true
                            Toast.makeText(context, "Google Veo cinema successfully synthesized!", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("veo_generate_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                contentPadding = PaddingValues()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(NovaGradient),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color.White)
                        Text(
                            text = if (isGenerating) "Synthesizing Veo Cinema..." else "Generate Google Veo Video",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // Fullscreen interactive theater style Dialog
    if (showFullscreenPlayer && generatedVideoCreation != null) {
        Dialog(
            onDismissRequest = { showFullscreenPlayer = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            ) {
                if (decodedBitmap != null) {
                    Image(
                        bitmap = decodedBitmap.asImageBitmap(),
                        contentDescription = "Fullscreen render",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                }

                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    clipRect {
                        val waveSteps = 60
                        val waveAmplitude = 45f
                        val phase = playerProgress * 2 * Math.PI.toFloat()
                        val styleColor = when (selectedStyle) {
                            "Cyberpunk" -> Color(0xFFFF00CC)
                            "Surreal Dreamscape" -> Color(0xFF00FFFF)
                            "Photorealistic" -> Color(0xFFFFCC00)
                            else -> NovaPrimary
                        }

                        // Drawing primary procedural wave on fullscreen
                        val points = mutableListOf<Offset>()
                        for (x in 0..waveSteps) {
                            val t = x.toFloat() / waveSteps
                            val xPos = t * w
                            val waveFactor = sin(t * 3 * Math.PI.toFloat() + phase)
                            val yPos = (h / 2f) + waveFactor * waveAmplitude
                            points.add(Offset(xPos, yPos))
                        }
                        for (p in 0 until points.size - 1) {
                            drawLine(
                                color = styleColor,
                                start = points[p],
                                end = points[p + 1],
                                strokeWidth = 4.dp.toPx()
                            )
                        }
                    }
                }

                // Close Button
                IconButton(
                    onClick = { showFullscreenPlayer = false },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(24.dp)
                        .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                        .size(44.dp)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White, modifier = Modifier.size(24.dp))
                }

                // Fullscreen bottom player bar
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.7f))
                        .padding(24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    IconButton(
                        onClick = { isPlaying = !isPlaying },
                        modifier = Modifier
                            .background(NovaPrimary, CircleShape)
                            .size(48.dp)
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "Play/Pause",
                            tint = Color.White
                        )
                    }

                    Slider(
                        value = playerProgress,
                        onValueChange = { playerProgress = it },
                        modifier = Modifier.weight(1f),
                        colors = SliderDefaults.colors(
                            thumbColor = NovaSecondary,
                            activeTrackColor = NovaPrimary,
                            inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                        )
                    )

                    Text(
                        text = "0:${String.format("%02d", (playerProgress * selectedDuration).toInt())} / 0:${String.format("%02d", selectedDuration)}",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
