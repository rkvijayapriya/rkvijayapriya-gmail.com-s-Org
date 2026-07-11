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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.foundation.Canvas
import androidx.compose.ui.window.Dialog
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.animation.core.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import com.example.ui.MainViewModel
import com.example.ui.theme.Translation
import com.example.ui.components.VideoPanelSkeleton
import com.example.ui.components.ToolInputProcessingOverlay
import com.example.ui.theme.NovaGradient
import com.example.ui.theme.NovaPrimary
import com.example.ui.theme.NovaSecondary
import com.example.ui.theme.NovaTertiary
import com.example.data.database.VisionAiCreation
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import androidx.compose.ui.input.key.*
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.foundation.focusable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatorScreen(
    viewModel: MainViewModel,
    onNavigateToPlayer: () -> Unit,
    onNavigateToEditor: () -> Unit,
    onNavigateToImagen: () -> Unit,
    onNavigateToVeo: () -> Unit,
    onNavigateToWriter: () -> Unit,
    onNavigateToVoiceOver: () -> Unit
) {
    val context = LocalContext.current
    val lang by viewModel.currentLanguage.collectAsState()
    val scale by viewModel.fontScale.collectAsState()
    val isGenerating by viewModel.isGenerating.collectAsState()
    val progress by viewModel.generationProgress.collectAsState()
    val progressStep by viewModel.generationStep.collectAsState()

    var creationType by remember { mutableStateOf("VIDEO") } // "VIDEO" or "IMAGE"
    var fabOffset by remember { mutableStateOf(Offset.Zero) }
    var prompt by remember { mutableStateOf("") }
    var sheetImagenPrompt by remember { mutableStateOf("") }
    var sheetTtsText by remember { mutableStateOf("") }
    var selectedTtsVoice by remember { mutableStateOf(voiceProfiles.first()) }

    val allCreations by viewModel.allCreations.collectAsState()
    val latestCreation = remember(allCreations, creationType) {
        allCreations.firstOrNull { it.type == creationType }
    }

    var selectedPastCreation by remember { mutableStateOf<VisionAiCreation?>(null) }
    
    val activeDisplayCreation = selectedPastCreation ?: latestCreation

    val displayBitmap = remember(activeDisplayCreation) {
        if (activeDisplayCreation != null && activeDisplayCreation.responseText.isNotBlank()) {
            try {
                val decodedBytes = Base64.decode(activeDisplayCreation.responseText, Base64.DEFAULT)
                BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
            } catch (e: Exception) {
                null
            }
        } else {
            null
        }
    }
    
    // Style Presets
    val videoStyles = listOf(
        "Cinematic", "Cartoon", "Anime", "Sketch", "Watercolor", "Retro", 
        "Clay Animation", "Sand Art", "Pixar Style", "Disney Style", "Realistic", 
        "3D Render", "Cyberpunk", "Fantasy", "Neon", "Comic Style", 
        "Oil Painting", "Pencil Drawing", "Low Poly", "Studio Quality"
    )
    var selectedStyle by remember { mutableStateOf("Cinematic") }

    // Quality / FPS / Duration Presets
    val resolutionOptions = listOf("720P", "1080P", "2K", "4K")
    var selectedResolution by remember { mutableStateOf("4K") }

    val fpsOptions = listOf(24, 30, 60)
    var selectedFps by remember { mutableStateOf(30) }

    val durationOptions = listOf(5, 10, 15, 30, 60)
    var selectedDuration by remember { mutableStateOf(10) }

    var selectedRatio by remember { mutableStateOf("16:9") }

    // Image Reference upload simulation status
    var attachImageSelected by remember { mutableStateOf(false) }

    // Veo API Custom Video configurations
    val veoQuickPrompts = listOf(
        "A majestic white stallion running across black volcanic sand, cinematic dramatic lighting",
        "Time-lapse of a glowing bioluminescent mushroom forest spreading magical sparks at night",
        "Close-up of a futuristic cyberpunk android opening eyes, highly reflective gaze, detailed lens",
        "A slow-motion drone flyover of a hidden temple inside a mountain peak",
        "An active cozy steampunk library with whirling brass gears, warm floating dust particles"
    )
    val imagenQuickPrompts = listOf(
        "A cyberpunk samurai holding a glowing neon katana in a rainy Tokyo alleyway",
        "Cute chibi fox astronaut sitting on a miniature moon, holding a shining star, 3D style",
        "Breathtaking oil painting of a peaceful waterfall cascade hidden inside a dense fantasy forest",
        "Sleek minimalist 3D isometric mockup of an ultra-modern smart home app dashboard",
        "Photorealistic close-up of a glass sphere containing an entire glowing colorful nebula galaxy inside"
    )
    val voiceoverQuickPrompts = listOf(
        "In a world where artificial intelligence defines human creativity, one platform reigns supreme.",
        "Welcome back to another video. Today we are exploring the deep mysteries of bioluminescent marine life.",
        "Experience pure tranquility. Close your eyes, breathe in the fresh mountain air, and let go of stress.",
        "Breaking news: Scientists have just discovered a secret underground oasis on Mars containing flowing water.",
        "Listen closely to the gentle rustling of autumn leaves as the cool forest breeze whisks them away."
    )
    var veoMotionIntensity by remember { mutableStateOf("Medium Flow") } // "Low (Slow)", "Medium Flow", "High Action"
    var veoCameraMotion by remember { mutableStateOf("Cinematic Orbit") } // "Static", "Pan Left", "Pan Right", "Zoom In", "Zoom Out", "Orbit", "Crane Up"
    var veoFidelityLevel by remember { mutableStateOf(8f) } // 1f to 10f

    // Pop-up Options bottom sheet state
    var showOptionsSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val coroutineScope = rememberCoroutineScope()
    var isListening by remember { mutableStateOf(false) }

    // Integrated Professional Editor state
    var activeEditorType by remember { mutableStateOf<String?>(null) } // "PROFESSIONAL" or null
    var selectedProfessionalTab by remember { mutableStateOf("IMAGE") } // "IMAGE", "VIDEO", "AUDIO", "TEXT"
    var showGalleryDialog by remember { mutableStateOf(false) }

    // Professional Image Editor State
    var proImageFilter by remember { mutableStateOf("Original") }
    var proImageBrightness by remember { mutableStateOf(100f) }
    var proImageContrast by remember { mutableStateOf(100f) }
    var proImageSaturation by remember { mutableStateOf(100f) }
    var proImageCropRatio by remember { mutableStateOf("Original") }
    var proImageTextSticker by remember { mutableStateOf("") }
    var proImageStickerColor by remember { mutableStateOf(Color.White) }

    // Professional Video Editor State
    var proVideoTrimStart by remember { mutableStateOf(0f) }
    var proVideoTrimEnd by remember { mutableStateOf(10f) }
    var proVideoSpeed by remember { mutableStateOf(1.0f) }
    var proVideoBgm by remember { mutableStateOf("None") }
    var proVideoBgmVolume by remember { mutableStateOf(50f) }
    var proVideoSubtitles by remember { mutableStateOf("") }
    var proVideoTransition by remember { mutableStateOf("None") }

    // Professional Audio / Voiceover Editor State
    var proAudioVolumeBoost by remember { mutableStateOf(1.0f) }
    var proAudioEffectPreset by remember { mutableStateOf("None") }
    var proAudioTrimStart by remember { mutableStateOf(0f) }
    var proAudioTrimEnd by remember { mutableStateOf(30f) }
    var proAudioPitchShift by remember { mutableStateOf(1.0f) }
    var proAudioNoiseReduction by remember { mutableStateOf(false) }

    // Professional Text / Writing Editor State
    var proTextFontSize by remember { mutableStateOf(16f) }
    var proTextFontFamily by remember { mutableStateOf("Sans-Serif") }
    var proTextStyleBold by remember { mutableStateOf(false) }
    var proTextStyleItalic by remember { mutableStateOf(false) }
    var proTextContentBody by remember { mutableStateOf("Welcome to the Professional Script Desk. Start drafting your cinematic screenplay or voiceover prompt script here. Fully optimized for text synthesis.") }

    val focusRequester = remember { FocusRequester() }

    val onTriggerGenerate = {
        if (creationType == "VOICEOVER") {
            val textToUse = if (sheetTtsText.isNotBlank()) sheetTtsText else prompt
            if (textToUse.isBlank()) {
                Toast.makeText(context, "Please enter voiceover text first!", Toast.LENGTH_SHORT).show()
            } else {
                showOptionsSheet = false
                viewModel.triggerVoiceOver(
                    text = textToUse,
                    voiceGender = selectedTtsVoice.gender,
                    accent = "${selectedTtsVoice.name} - ${selectedTtsVoice.styleDesc} (${selectedTtsVoice.language}, Friendly Accent)",
                    voiceId = selectedTtsVoice.elevenLabsVoiceId
                ) {
                    Toast.makeText(context, "Voiceover audio synthesized successfully!", Toast.LENGTH_SHORT).show()
                }
            }
        } else if (creationType == "VIDEO") {
            val textToUse = if (showOptionsSheet && sheetImagenPrompt.isNotBlank()) sheetImagenPrompt else prompt
            if (textToUse.isBlank()) {
                Toast.makeText(context, "Please enter a visual prompt description", Toast.LENGTH_SHORT).show()
            } else {
                showOptionsSheet = false
                val enhancedPrompt = buildString {
                    append(textToUse)
                    if (veoCameraMotion != "Static") {
                        append(", with camera conducting a $veoCameraMotion path")
                    }
                    append(", with $veoMotionIntensity motion dynamics")
                    append(", rendered at physical consistency scale of ${veoFidelityLevel.toInt()}/10")
                }

                viewModel.triggerGeneration(
                    prompt = enhancedPrompt,
                    style = selectedStyle,
                    cameraAngle = veoCameraMotion,
                    resolution = selectedResolution,
                    fps = selectedFps,
                    duration = selectedDuration,
                    imageSelected = attachImageSelected,
                    aspectRatio = selectedRatio
                ) {
                    onNavigateToEditor()
                }
            }
        } else { // "IMAGE"
            val textToUse = if (showOptionsSheet && sheetImagenPrompt.isNotBlank()) sheetImagenPrompt else prompt
            if (textToUse.isBlank()) {
                Toast.makeText(context, "Please enter a visual prompt description", Toast.LENGTH_SHORT).show()
            } else {
                showOptionsSheet = false
                viewModel.triggerImageGeneration(
                    prompt = textToUse,
                    style = selectedStyle,
                    aspectRatio = selectedRatio
                ) {
                    selectedPastCreation = null
                    Toast.makeText(context, "AI Image generated successfully!", Toast.LENGTH_SHORT).show()
                    onNavigateToEditor()
                }
            }
        }
    }

    val onTriggerExport = {
        val creationToExport = activeDisplayCreation
        if (creationToExport != null) {
            com.example.ui.components.MediaExportHelper.exportCreation(context, creationToExport) { success, path ->
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    if (success) {
                        Toast.makeText(context, "Exported successfully: $path", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(context, "Export failed: $path", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        } else {
            Toast.makeText(context, "No active media creation available to export.", Toast.LENGTH_SHORT).show()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .focusRequester(focusRequester)
            .focusable()
            .onKeyEvent { keyEvent ->
                if (keyEvent.type == KeyEventType.KeyDown) {
                    if ((keyEvent.isCtrlPressed && keyEvent.key == Key.G) || (keyEvent.isCtrlPressed && keyEvent.key == Key.Enter)) {
                        onTriggerGenerate()
                        true
                    } else if ((keyEvent.isCtrlPressed && keyEvent.key == Key.E) || (keyEvent.isCtrlPressed && keyEvent.key == Key.S)) {
                        onTriggerExport()
                        true
                    } else if (keyEvent.isCtrlPressed && keyEvent.isShiftPressed && keyEvent.key == Key.V) {
                        creationType = "VIDEO"
                        Toast.makeText(context, "Switched to Video Mode", Toast.LENGTH_SHORT).show()
                        true
                    } else if (keyEvent.isCtrlPressed && keyEvent.isShiftPressed && keyEvent.key == Key.I) {
                        creationType = "IMAGE"
                        Toast.makeText(context, "Switched to Image Mode", Toast.LENGTH_SHORT).show()
                        true
                    } else if (keyEvent.isCtrlPressed && keyEvent.isShiftPressed && keyEvent.key == Key.O) {
                        creationType = "VOICEOVER"
                        Toast.makeText(context, "Switched to Voiceover Mode", Toast.LENGTH_SHORT).show()
                        true
                    } else {
                        false
                    }
                } else {
                    false
                }
            }
    ) {
        LaunchedEffect(Unit) {
            focusRequester.requestFocus()
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Row (NovaAI Studio Branding with Premium HD styling)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Star Glowing Icon
                    Box(
                        modifier = Modifier
                            .size(36.dp)
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
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = NovaPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Column {
                        Text(
                            text = "STUDIO",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = NovaPrimary,
                            letterSpacing = 1.5.sp
                        )
                        Text(
                            text = "NovaAI Studio",
                            fontSize = (22 * scale).sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Keyboard Shortcuts Button
                    IconButton(
                        onClick = { viewModel.showShortcutManager.value = true },
                        modifier = Modifier
                            .size(38.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), CircleShape)
                            .testTag("shortcuts_help_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Keyboard,
                            contentDescription = "Show Keyboard Shortcuts",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Profile Badge
                    Box(
                        modifier = Modifier
                            .size(38.dp)
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
            }

            // MIDDLE FREE SPACE & LIVE HOLOGRAPHIC DIGITAL ARTBOARD
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                if (isGenerating) {
                    VideoPanelSkeleton()
                } else if (displayBitmap != null && activeDisplayCreation != null && creationType == "IMAGE") {
                    // Beautiful Output Display Card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight()
                            .testTag("ai_image_display_area"),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(24.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1.2f)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color.Black),
                                contentAlignment = Alignment.Center
                            ) {
                                androidx.compose.foundation.Image(
                                    bitmap = displayBitmap.asImageBitmap(),
                                    contentDescription = "Resulting generated image",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                                
                                // Reset back to free space button
                                IconButton(
                                    onClick = { selectedPastCreation = null },
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(8.dp)
                                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Close Output", tint = Color.White)
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = activeDisplayCreation.title,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = (14 * scale).sp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = activeDisplayCreation.generatedDescription,
                                        fontSize = (11 * scale).sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = activeDisplayCreation.style,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = (10 * scale).sp
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        viewModel.activeCreation.value = activeDisplayCreation
                                        onNavigateToEditor()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Edit Image", fontSize = (11 * scale).sp, fontWeight = FontWeight.Bold)
                                }

                                Button(
                                    onClick = {
                                        Toast.makeText(context, "Saved successfully to local gallery!", Toast.LENGTH_SHORT).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Download", fontSize = (11 * scale).sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                } else if (activeDisplayCreation != null && creationType == "VOICEOVER") {
                    // Beautiful Voiceover player card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight()
                            .testTag("ai_voiceover_display_area"),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(24.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(160.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color.Black),
                                contentAlignment = Alignment.Center
                            ) {
                                // Beautiful Pulsating/Dancing audio waveforms
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.VolumeUp,
                                        contentDescription = "Audio file",
                                        tint = NovaPrimary,
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "AI VOICEOVER SYNTHESIZED",
                                        color = NovaPrimary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 2.sp
                                    )
                                }
                                
                                // Reset back to free space button
                                IconButton(
                                    onClick = { selectedPastCreation = null },
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(8.dp)
                                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear preview", tint = Color.White)
                                }
                            }

                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = activeDisplayCreation.title,
                                    fontSize = (18 * scale).sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = activeDisplayCreation.generatedDescription,
                                    fontSize = (13 * scale).sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AssistChip(
                                    onClick = {},
                                    label = { Text(activeDisplayCreation.style) },
                                    leadingIcon = { Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(14.dp)) }
                                )
                                Text(
                                    text = "TTS Engine v3",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        viewModel.activeCreation.value = activeDisplayCreation
                                        onNavigateToPlayer()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Listen Voiceover", fontSize = (11 * scale).sp, fontWeight = FontWeight.Bold)
                                }

                                Button(
                                    onClick = {
                                        Toast.makeText(context, "Audio saved to device storage!", Toast.LENGTH_SHORT).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Download", fontSize = (11 * scale).sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                } else {
                    // Futuristic Premium Free Space layout with Integrated Editors
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Top,
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 8.dp, vertical = 16.dp)
                    ) {
                        // User Greeting and Stats Card
                        val currentUser = viewModel.loggedInUser.collectAsState().value
                        val creationCount = allCreations.size

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 20.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                            ),
                            shape = RoundedCornerShape(20.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Welcome Back,",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary,
                                        letterSpacing = 1.sp
                                    )
                                    Text(
                                        text = currentUser?.name ?: "Creator",
                                        fontSize = (20 * scale).sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Explore the next frontier of generative AI.",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = "$creationCount",
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = "Creations",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }

                        // AI STUDIO WORKSPACE (Modern 2x2 Grid)
                        Text(
                            text = "AI STUDIO WORKSPACE",
                            fontSize = (10 * scale).sp,
                            fontWeight = FontWeight.Bold,
                            color = NovaPrimary,
                            letterSpacing = 1.5.sp,
                            modifier = Modifier
                                .align(Alignment.Start)
                                .padding(start = 4.dp, bottom = 12.dp)
                        )

                        // 2x2 Grid Layout for AI tools
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Row 1: Video Studio & Image Studio
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // 1. Video Studio Card
                                Card(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(145.dp)
                                        .clickable { onNavigateToVeo() }
                                        .testTag("tool_card_video"),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                                    ),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                                    shape = RoundedCornerShape(18.dp)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(14.dp),
                                        verticalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(Color(0xFFFF1744).copy(alpha = 0.15f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Movie,
                                                    contentDescription = "Video Studio",
                                                    tint = Color(0xFFFF1744),
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(Color(0xFFFF1744).copy(alpha = 0.1f))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = "VEO 2.0",
                                                    fontSize = 8.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFFFF1744)
                                                )
                                            }
                                        }

                                        Column {
                                            Text(
                                                text = "Video Studio",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = "Generate premium cinematic videos from high-fidelity descriptions.",
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                                lineHeight = 12.sp,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }

                                // 2. Image Studio Card
                                Card(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(145.dp)
                                        .clickable { onNavigateToImagen() }
                                        .testTag("tool_card_image"),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                                    ),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                                    shape = RoundedCornerShape(18.dp)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(14.dp),
                                        verticalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(Color(0xFF00E5FF).copy(alpha = 0.15f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Brush,
                                                    contentDescription = "Image Studio",
                                                    tint = Color(0xFF00E5FF),
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(Color(0xFF00E5FF).copy(alpha = 0.1f))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = "IMAGEN 3",
                                                    fontSize = 8.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF00E5FF)
                                                )
                                            }
                                        }

                                        Column {
                                            Text(
                                                text = "Image Studio",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = "Synthesize photorealistic graphics and art from prompts.",
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                                lineHeight = 12.sp,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }

                            // Row 2: Audio Studio & Writing Studio
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // 3. Audio Studio Card
                                Card(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(145.dp)
                                        .clickable { onNavigateToVoiceOver() }
                                        .testTag("tool_card_audio"),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                                    ),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                                    shape = RoundedCornerShape(18.dp)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(14.dp),
                                        verticalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(Color(0xFFD500F9).copy(alpha = 0.15f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Mic,
                                                    contentDescription = "Audio Studio",
                                                    tint = Color(0xFFD500F9),
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(Color(0xFFD500F9).copy(alpha = 0.1f))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = "VOCAL TTS",
                                                    fontSize = 8.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFFD500F9)
                                                )
                                            }
                                        }

                                        Column {
                                            Text(
                                                text = "Audio Studio",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = "Synthesize high-quality vocal audio and voiceovers.",
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                                lineHeight = 12.sp,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }

                                // 4. Writing Studio Card
                                Card(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(145.dp)
                                        .clickable { onNavigateToWriter() }
                                        .testTag("tool_card_writing"),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                                    ),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                                    shape = RoundedCornerShape(18.dp)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(14.dp),
                                        verticalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(Color(0xFFFF9100).copy(alpha = 0.15f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.ChatBubble,
                                                    contentDescription = "Writing Studio",
                                                    tint = Color(0xFFFF9100),
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(Color(0xFFFF9100).copy(alpha = 0.1f))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = "NoVaGpT",
                                                    fontSize = 8.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFFFF9100)
                                                )
                                            }
                                        }

                                        Column {
                                            Text(
                                                text = "Writing Studio",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = "Draft scripts, screenplays, captions, or descriptions with AI.",
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                                lineHeight = 12.sp,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Quick Starter prompts as requested
                        Text(
                            text = "TAP A QUICK STARTER TO BEGIN",
                            fontSize = (10 * scale).sp,
                            fontWeight = FontWeight.Bold,
                            color = NovaPrimary,
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val activeQuickPrompts = when (creationType) {
                                "IMAGE" -> imagenQuickPrompts
                                "VOICEOVER" -> voiceoverQuickPrompts
                                else -> veoQuickPrompts
                            }
                            items(activeQuickPrompts) { quickPrompt ->
                                Card(
                                    modifier = Modifier
                                        .width(200.dp)
                                        .height(80.dp)
                                        .clickable { prompt = quickPrompt },
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                    ),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f)),
                                    shape = RoundedCornerShape(18.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(12.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = quickPrompt,
                                            fontSize = (11 * scale).sp,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis,
                                            textAlign = TextAlign.Center,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ATTACHED FILE CHIP indicator above input box
            AnimatedVisibility(visible = attachImageSelected) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                        .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Attached",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Reference Image Active: IMG_REF_AI_STYLE.PNG",
                            fontSize = (11 * scale).sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(
                        onClick = { attachImageSelected = false },
                        modifier = Modifier.size(18.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Remove Reference",
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            // 🎬 REAL-TIME CREATIVE AI MODE SWITCHER & PROGRESS RING SYSTEM (with dynamic status feedback)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf(
                    Triple("VIDEO", "Video Gen", Icons.Default.Movie),
                    Triple("IMAGE", "Image Gen", Icons.Default.Brush),
                    Triple("VOICEOVER", "Music Gen", Icons.Default.MusicNote)
                ).forEach { (type, label, icon) ->
                    val isSelected = creationType == type
                    val isCurrentGenerating = isGenerating && creationType == type

                    val infiniteTransition = rememberInfiniteTransition(label = "ring_rotate_$type")
                    val ringRotation by infiniteTransition.animateFloat(
                        initialValue = 0f,
                        targetValue = 360f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1200, easing = LinearEasing),
                            repeatMode = RepeatMode.Restart
                        ),
                        label = "angle"
                    )

                    val ringPulse by infiniteTransition.animateFloat(
                        initialValue = 0.95f,
                        targetValue = 1.05f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(800, easing = FastOutSlowInEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "pulse"
                    )

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .clickable { 
                                creationType = type
                                Toast.makeText(context, "Switched to $label Mode", Toast.LENGTH_SHORT).show()
                            }
                            .padding(8.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(56.dp)
                                .scale(if (isCurrentGenerating) ringPulse else 1f)
                        ) {
                            // Circular Progress Ring animation
                            if (isCurrentGenerating) {
                                CircularProgressIndicator(
                                    progress = { progress },
                                    color = NovaPrimary,
                                    strokeWidth = 3.dp,
                                    modifier = Modifier.fillMaxSize()
                                )
                                Canvas(modifier = Modifier.fillMaxSize().padding(3.dp)) {
                                    rotate(ringRotation) {
                                        drawArc(
                                            color = NovaSecondary.copy(alpha = 0.6f),
                                            startAngle = 0f,
                                            sweepAngle = 140f,
                                            useCenter = false,
                                            style = Stroke(
                                                width = 2.dp.toPx(),
                                                pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(
                                                    floatArrayOf(10f, 10f), 0f
                                                )
                                            )
                                        )
                                    }
                                }
                            } else {
                                // Default static outline/glow rings
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    drawCircle(
                                        color = if (isSelected) NovaPrimary.copy(alpha = 0.3f) else Color.Gray.copy(alpha = 0.15f),
                                        radius = size.minDimension / 2,
                                        style = Stroke(width = if (isSelected) 2.5.dp.toPx() else 1.dp.toPx())
                                    )
                                }
                            }

                            // Center Icon with visual gradient background
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(
                                        if (isSelected) NovaGradient else Brush.verticalGradient(listOf(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.surfaceVariant)),
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = label,
                                    tint = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Text(
                            text = label,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) NovaPrimary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
                        // BOTTOM CHAT INPUT BAR WITH "+" BUTTON, MIC BUTTON, AND SEND BUTTON wrapped in Box with processing skeleton/spinner overlay
            Box(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // "+" Gallery Upload Trigger
                    IconButton(
                        onClick = { showGalleryDialog = true },
                        modifier = Modifier
                            .size(48.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                            .testTag("plus_options_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Upload Media Reference",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // Chat Input text field with voice mic inside
                    OutlinedTextField(
                        value = prompt,
                        onValueChange = { prompt = it },
                        placeholder = {
                            Text(
                                text = "Describe your visual masterwork...",
                                fontSize = (13 * scale).sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("prompt_text_field")
                            .onPreviewKeyEvent { keyEvent ->
                                if (keyEvent.type == KeyEventType.KeyDown) {
                                    if ((keyEvent.isCtrlPressed && keyEvent.key == Key.G) || (keyEvent.isCtrlPressed && keyEvent.key == Key.Enter)) {
                                        onTriggerGenerate()
                                        true
                                    } else if ((keyEvent.isCtrlPressed && keyEvent.key == Key.E) || (keyEvent.isCtrlPressed && keyEvent.key == Key.S)) {
                                        onTriggerExport()
                                        true
                                    } else {
                                        false
                                    }
                                } else {
                                    false
                                }
                            },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f),
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        ),
                        singleLine = true,
                        leadingIcon = {
                            IconButton(
                                onClick = {
                                    coroutineScope.launch {
                                        isListening = true
                                        Toast.makeText(context, "Nova Voice Engine: Listening...", Toast.LENGTH_SHORT).show()
                                        delay(2000)
                                        isListening = false
                                        prompt = "A slow-motion drone flyover of a hidden temple nestled inside a high-altitude mountain peak, cinematic, detailed"
                                        Toast.makeText(context, "Speech transcribed!", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Mic,
                                    contentDescription = "Voice assistant input",
                                    tint = if (isListening) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        shape = RoundedCornerShape(24.dp)
                    )

                    // Send generation trigger Button
                    IconButton(
                        onClick = {
                            onTriggerGenerate()
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .background(NovaGradient, CircleShape)
                            .testTag("generate_button")
                    ) {
                        if (isGenerating) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                CircularProgressIndicator(
                                    progress = { progress },
                                    color = Color.White,
                                    strokeWidth = 2.5.dp,
                                    modifier = Modifier.fillMaxSize().padding(4.dp)
                                )
                                Icon(
                                    imageVector = when (creationType) {
                                        "VIDEO" -> Icons.Default.Movie
                                        "IMAGE" -> Icons.Default.Brush
                                        "VOICEOVER" -> Icons.Default.Mic
                                        else -> Icons.Default.Send
                                    },
                                    contentDescription = "Generating",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        } else {
                            Icon(
                                imageVector = Icons.Default.Send,
                                contentDescription = "Generate",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
                if (isGenerating) {
                    ToolInputProcessingOverlay(
                        message = "Synthesizing visual direction...",
                        modifier = Modifier.matchParentSize()
                    )
                }
            }  }

            Spacer(modifier = Modifier.height(56.dp)) // Padding for bottom navigation bar
        }

        // ATTRACTIVE FLOATING ACTION BUTTON (For All other options / generation settings)
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset { IntOffset(fabOffset.x.roundToInt(), fabOffset.y.roundToInt()) }
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        fabOffset += dragAmount
                    }
                }
                .padding(bottom = 120.dp, end = 16.dp)
                .testTag("fab_options_button")
                .clip(RoundedCornerShape(16.dp))
                .background(NovaGradient)
                .clickable { showOptionsSheet = true }
                .padding(1.5.dp)
        ) {
            Box(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(15.dp))
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "All Options",
                        tint = NovaPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "AI Engines & Presets",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }
        }

        // ALL OPTIONS IN BOTTOM POP-UP SHEET (as requested)
        if (showOptionsSheet) {
            ModalBottomSheet(
                onDismissRequest = { showOptionsSheet = false },
                sheetState = sheetState,
                containerColor = MaterialTheme.colorScheme.surface,
                dragHandle = { BottomSheetDefaults.DragHandle(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)) }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                        .navigationBarsPadding(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Generation Parameters",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        IconButton(onClick = { showOptionsSheet = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Close settings")
                        }
                    }

                    // OUTPUT ENGINE SWITCHER
                    Text(
                        text = "Creation Output Engine",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp))
                            .padding(4.dp)
                    ) {
                        listOf("VIDEO", "IMAGE", "VOICEOVER").forEach { type ->
                            val isSelected = creationType == type
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                                    .clickable { creationType = type }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (type == "VIDEO") "Cinematic Video" else if (type == "IMAGE") "Digital Image Art" else "AI Voiceover",
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }

                    // STYLE PRESETS
                    Text(
                        text = "Visual Art Style Preset",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(horizontal = 2.dp)
                    ) {
                        items(videoStyles) { style ->
                            val isSelected = selectedStyle == style
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
                                    .clickable { selectedStyle = style }
                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = style,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // ASPECT RATIOS
                    Text(
                        text = "Canvas Aspect Ratio",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("16:9", "9:16", "1:1").forEach { ratio ->
                            val isSelected = selectedRatio == ratio
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant)
                                    .border(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                                    .clickable { selectedRatio = ratio }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(
                                                width = when(ratio) {
                                                    "9:16" -> 8.dp
                                                    "16:9" -> 16.dp
                                                    else -> 12.dp
                                                },
                                                height = when(ratio) {
                                                    "9:16" -> 16.dp
                                                    "16:9" -> 9.dp
                                                    else -> 12.dp
                                                }
                                            )
                                            .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(2.dp))
                                    )
                                    Text(
                                        text = ratio,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    // VEO CINEMATIC CONTROLS (Only displayed if Video is selected)
                    if (creationType == "VIDEO") {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        
                        Text(
                            text = "Veo Cinematic Tuning",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.primary
                        )

                        // Motion Dynamics preset selection
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "Veo Motion Dynamics",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                                    .padding(4.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                listOf("Low (Slow)", "Medium Flow", "High Action").forEach { motion ->
                                    val isSel = veoMotionIntensity == motion
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSel) MaterialTheme.colorScheme.primary else Color.Transparent)
                                            .clickable { veoMotionIntensity = motion }
                                            .padding(vertical = 10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = motion,
                                            color = if (isSel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }
                        }

                        // Camera Motion presets
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "Camera Motion Paths",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            val motionPaths = listOf("Static", "Cinematic Orbit", "Pan Left", "Pan Right", "Zoom In", "Zoom Out", "Crane Up")
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                contentPadding = PaddingValues(vertical = 2.dp)
                            ) {
                                items(motionPaths) { path ->
                                    val isSel = veoCameraMotion == path
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(if (isSel) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant)
                                            .border(
                                                1.dp,
                                                if (isSel) MaterialTheme.colorScheme.primary else Color.Transparent,
                                                RoundedCornerShape(12.dp)
                                            )
                                            .clickable { veoCameraMotion = path }
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = path,
                                            color = if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }

                        // Physical Simulation Fidelity Slider
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Physical Consistency / Fidelity",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${veoFidelityLevel.toInt()}/10",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Slider(
                                value = veoFidelityLevel,
                                onValueChange = { veoFidelityLevel = it },
                                valueRange = 1f..10f,
                                steps = 8,
                                colors = SliderDefaults.colors(
                                    thumbColor = MaterialTheme.colorScheme.primary,
                                    activeTrackColor = MaterialTheme.colorScheme.primary,
                                    inactiveTrackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
                                )
                            )
                        }

                        // FPS & Duration Options Grid
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Frame rate
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "Frame Rate",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
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
                                            fontSize = 11.sp,
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
                                    text = "Duration",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
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
                                            fontSize = 11.sp,
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

                        // Custom premium shortcut card for the advanced Google Veo Studio workspace
                        Spacer(modifier = Modifier.height(12.dp))
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onNavigateToVeo() }
                                .testTag("advanced_veo_workspace_card"),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                            ),
                            border = BorderStroke(1.5.dp, NovaGradient),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .background(NovaGradient, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Videocam,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = "Advanced Veo 2.0 Studio",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = "Fidelity tuning, camera choreography & pro parameters",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                        )
                                    }
                                }
                                Icon(
                                    imageVector = Icons.Default.ArrowForward,
                                    contentDescription = "Open Veo Studio",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    if (creationType == "IMAGE") {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        
                        Text(
                            text = "Imagen 3 AI Image Generator",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.primary
                        )

                        // Custom premium shortcut card for the advanced Imagen 3 Studio workspace
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onNavigateToImagen() }
                                .testTag("advanced_imagen_workspace_card"),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                            ),
                            border = BorderStroke(1.5.dp, NovaGradient),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .background(NovaGradient, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.AutoAwesome,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = "Advanced Imagen 3 Studio",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = "Aspect ratios, HD resolutions & Pro presets",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                        )
                                    }
                                }
                                Icon(
                                    imageVector = Icons.Default.ArrowForward,
                                    contentDescription = "Open Studio",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = "Google Imagen 3 Engine",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }

                                OutlinedTextField(
                                    value = sheetImagenPrompt,
                                    onValueChange = { sheetImagenPrompt = it },
                                    placeholder = { 
                                        Text(
                                            "Enter detailed prompt for Imagen 3 (e.g., 'A cyberpunk samurai under heavy rain on a neon street' ...)",
                                            fontSize = 12.sp
                                        ) 
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(100.dp)
                                        .testTag("imagen_prompt_input"),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                                        unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                                    )
                                )

                                // QUICK PROMPTS
                                Text(
                                    text = "Try these Ideas:",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                val ideas = listOf(
                                    "Futuristic Cyber City",
                                    "Aesthetic Watercolor Lotus",
                                    "Cosmic Nebula Portal",
                                    "Cozy Rainy Coffee Shop"
                                )
                                Row(
                                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    ideas.forEach { idea ->
                                        SuggestionChip(
                                            onClick = { sheetImagenPrompt = idea },
                                            label = { Text(idea, fontSize = 11.sp) }
                                        )
                                    }
                                }

                                Button(
                                    onClick = {
                                        if (sheetImagenPrompt.isBlank()) {
                                            Toast.makeText(context, "Please enter a prompt first!", Toast.LENGTH_SHORT).show()
                                        } else {
                                            showOptionsSheet = false
                                            viewModel.triggerImageGeneration(
                                                prompt = sheetImagenPrompt,
                                                style = selectedStyle,
                                                aspectRatio = selectedRatio
                                            ) {
                                                selectedPastCreation = null
                                                Toast.makeText(context, "AI Image generated successfully!", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(44.dp)
                                        .testTag("imagen_generate_btn"),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(Icons.Default.Brush, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Text("Generate Artwork via Imagen", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }

                    if (creationType == "VOICEOVER") {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        
                        Text(
                            text = "Text-to-Speech Voiceover Synthesis",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = "AI Vocal Synthesizer Engine",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }

                                OutlinedTextField(
                                    value = sheetTtsText,
                                    onValueChange = { sheetTtsText = it },
                                    placeholder = { 
                                        Text(
                                            "Enter text narration to convert to an AI voice synthesized audio file...",
                                            fontSize = 12.sp
                                        ) 
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(100.dp)
                                        .testTag("voiceover_prompt_input"),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                                        unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                                    )
                                )

                                Text(
                                    text = "Select Vocal Artist profile:",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                
                                val voices = listOf(
                                    Triple("Serena", "FEMALE", "Warm Narrator"),
                                    Triple("Arthur", "MALE", "Bold Broadcaster"),
                                    Triple("Kamala", "FEMALE", "Tamil Narrator"),
                                    Triple("Kathir", "MALE", "Tamil Speaker"),
                                    Triple("Aanya", "FEMALE", "Hindi Storyteller")
                                )
                                
                                Row(
                                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    voices.forEach { (name, gender, desc) ->
                                        val isSel = selectedTtsVoice.name == name
                                        FilterChip(
                                            selected = isSel,
                                            onClick = {
                                                selectedTtsVoice = voiceProfiles.firstOrNull { it.name == name } ?: voiceProfiles.first()
                                            },
                                            label = { Text("$name ($desc)") }
                                        )
                                    }
                                }

                                Button(
                                    onClick = {
                                        if (sheetTtsText.isBlank()) {
                                            Toast.makeText(context, "Please enter voiceover text first!", Toast.LENGTH_SHORT).show()
                                        } else {
                                            showOptionsSheet = false
                                            viewModel.triggerVoiceOver(
                                                text = sheetTtsText,
                                                voiceGender = selectedTtsVoice.gender,
                                                accent = "${selectedTtsVoice.name} - ${selectedTtsVoice.styleDesc} (${selectedTtsVoice.language}, Friendly Accent)",
                                                voiceId = selectedTtsVoice.elevenLabsVoiceId
                                            ) {
                                                Toast.makeText(context, "Voiceover audio synthesized successfully!", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(44.dp)
                                        .testTag("voiceover_generate_btn"),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Text("Synthesize Dynamic Audio file", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }

                    // SOURCE IMAGE REFERENCE UPLOAD
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    Text(
                        text = "Source Image Reference",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                attachImageSelected = !attachImageSelected
                                Toast.makeText(
                                    context,
                                    if (attachImageSelected) "Image Reference Uploaded!" else "Image reference removed",
                                    Toast.LENGTH_SHORT
                                ).show()
                            },
                        colors = CardDefaults.cardColors(
                            containerColor = if (attachImageSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                        ),
                        border = BorderStroke(
                            1.dp,
                            if (attachImageSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f)
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = if (attachImageSelected) Icons.Default.CheckCircle else Icons.Default.Add,
                                contentDescription = null,
                                tint = if (attachImageSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (attachImageSelected) "IMG_REF_AI_STYLE.PNG Attached" else "Attach Image Reference File",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = if (attachImageSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Guide the style framework using a seed file",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }

                    // LOCAL PROJECTS SECTION
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Local Projects Archive",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        val projectCount = allCreations.count { it.type == "VIDEO" || it.type == "IMAGE" }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(100.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "$projectCount items",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    // Filters
                    var projectFilter by remember { mutableStateOf("ALL") } // "ALL", "VIDEO", "IMAGE"
                    val filteredProjects = remember(allCreations, projectFilter) {
                        val allFiltered = allCreations.filter { it.type == "VIDEO" || it.type == "IMAGE" }
                        when (projectFilter) {
                            "VIDEO" -> allFiltered.filter { it.type == "VIDEO" }
                            "IMAGE" -> allFiltered.filter { it.type == "IMAGE" }
                            else -> allFiltered
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("ALL", "VIDEO", "IMAGE").forEach { filter ->
                            val isSelected = projectFilter == filter
                            val label = when (filter) {
                                "ALL" -> "All Projects"
                                "VIDEO" -> "Videos"
                                else -> "Images"
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant)
                                    .border(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent, RoundedCornerShape(8.dp))
                                    .clickable { projectFilter = filter }
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    if (filteredProjects.isEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Folder,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                    modifier = Modifier.size(32.dp)
                                )
                                Text(
                                    text = "No saved projects found.",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    } else {
                        val localDateFormatter = remember { java.text.SimpleDateFormat("dd MMM, hh:mm a", java.util.Locale.getDefault()) }
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            filteredProjects.forEach { proj ->
                                val projBitmap = remember(proj) {
                                    if (proj.type == "IMAGE" && proj.responseText.isNotBlank()) {
                                        try {
                                            val decodedBytes = Base64.decode(proj.responseText, Base64.DEFAULT)
                                            BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                                        } catch (e: Exception) {
                                            null
                                        }
                                    } else {
                                        null
                                    }
                                }

                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selectedPastCreation = proj
                                            creationType = proj.type
                                            Toast.makeText(context, "Loaded project: ${proj.title}", Toast.LENGTH_SHORT).show()
                                            showOptionsSheet = false
                                        },
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        // Preview thumbnail or icon
                                        Box(
                                            modifier = Modifier
                                                .size(44.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(
                                                    if (proj.type == "VIDEO") MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                                    else MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f)
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (projBitmap != null) {
                                                androidx.compose.foundation.Image(
                                                    bitmap = projBitmap.asImageBitmap(),
                                                    contentDescription = "Thumbnail",
                                                    modifier = Modifier.fillMaxSize(),
                                                    contentScale = ContentScale.Crop
                                                )
                                            } else {
                                                Icon(
                                                    imageVector = if (proj.type == "VIDEO") Icons.Default.PlayArrow else Icons.Default.Image,
                                                    contentDescription = null,
                                                    tint = if (proj.type == "VIDEO") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }

                                        // Meta information
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = proj.title,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = proj.prompt,
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            val dateStr = remember(proj.timestamp) { localDateFormatter.format(java.util.Date(proj.timestamp)) }
                                            Text(
                                                text = "$dateStr • ${proj.style} • ${proj.resolution}",
                                                fontSize = 9.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                            )
                                        }

                                        // Delete Button
                                        IconButton(
                                            onClick = {
                                                viewModel.deleteCreation(proj.id)
                                                if (selectedPastCreation?.id == proj.id) {
                                                    selectedPastCreation = null
                                                }
                                                Toast.makeText(context, "Project deleted", Toast.LENGTH_SHORT).show()
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Delete Project",
                                                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // SAVE / CLOSE BUTTON
                    Button(
                        onClick = { showOptionsSheet = false },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .padding(top = 8.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text(
                            text = "Save & Apply Parameters",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color.White
                        )
                    }
                }
            }
        }

        // PROFESSIONAL MEDIA & CREATION EDITOR SUITE
        if (activeEditorType == "PROFESSIONAL") {
            Dialog(onDismissRequest = { activeEditorType = null }) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Scaffold(
                        topBar = {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surface)
                                    .statusBarsPadding()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    IconButton(
                                        onClick = { activeEditorType = null },
                                        modifier = Modifier.testTag("pro_editor_back_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ArrowBack,
                                            contentDescription = "Back to Studio",
                                            tint = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = "Professional Editor",
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 18.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "NovaAI Studio Engine v4.0",
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                        )
                                    }
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.primaryContainer)
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = "PRO MODE",
                                        fontSize = 9.sp,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = 1.sp
                                    )
                                }
                            }
                        }
                    ) { innerPadding ->
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                                .background(MaterialTheme.colorScheme.background)
                        ) {
                            // Professional Format Selector Tabs
                            TabRow(
                                selectedTabIndex = when (selectedProfessionalTab) {
                                    "IMAGE" -> 0
                                    "VIDEO" -> 1
                                    "AUDIO" -> 2
                                    "TEXT" -> 3
                                    else -> 0
                                },
                                containerColor = MaterialTheme.colorScheme.surface,
                                contentColor = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Tab(
                                    selected = selectedProfessionalTab == "IMAGE",
                                    onClick = { selectedProfessionalTab = "IMAGE" },
                                    text = { Text("Image", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                                    icon = { Icon(Icons.Default.Photo, contentDescription = null, modifier = Modifier.size(18.dp)) }
                                )
                                Tab(
                                    selected = selectedProfessionalTab == "VIDEO",
                                    onClick = { selectedProfessionalTab = "VIDEO" },
                                    text = { Text("Video", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                                    icon = { Icon(Icons.Default.Movie, contentDescription = null, modifier = Modifier.size(18.dp)) }
                                )
                                Tab(
                                    selected = selectedProfessionalTab == "AUDIO",
                                    onClick = { selectedProfessionalTab = "AUDIO" },
                                    text = { Text("Audio", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                                    icon = { Icon(Icons.Default.Mic, contentDescription = null, modifier = Modifier.size(18.dp)) }
                                )
                                Tab(
                                    selected = selectedProfessionalTab == "TEXT",
                                    onClick = { selectedProfessionalTab = "TEXT" },
                                    text = { Text("Script", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                                    icon = { Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(18.dp)) }
                                )
                            }

                            // Dynamic Editor Workspaces based on chosen format tab
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(16.dp)
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                when (selectedProfessionalTab) {
                                    "IMAGE" -> {
                                        // 🖼️ PROFESSIONAL IMAGE EDITOR WORKSPACE
                                        Text(
                                            text = "PRO VISUAL & COLOR GRADING DECK",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = NovaPrimary,
                                            letterSpacing = 1.sp,
                                            modifier = Modifier.align(Alignment.Start)
                                        )

                                        // Beautiful Real-time Image Preview Canvas
                                        val filterOverlayColor = when (proImageFilter) {
                                            "Cinematic" -> Color(0xFFE2B007).copy(alpha = 0.15f)
                                            "Noir" -> Color.Gray
                                            "Vintage" -> Color(0xFF8B5A2B).copy(alpha = 0.2f)
                                            "Cyberpunk" -> Color(0xFFFF00CC).copy(alpha = 0.15f)
                                            "Warm Sun" -> Color(0xFFFF7700).copy(alpha = 0.12f)
                                            "Cold Ice" -> Color(0xFF00FFFF).copy(alpha = 0.15f)
                                            else -> Color.Transparent
                                        }

                                        val previewHeight = when (proImageCropRatio) {
                                            "1:1" -> 220.dp
                                            "16:9" -> 124.dp
                                            "4:3" -> 165.dp
                                            "9:16" -> 240.dp
                                            else -> 180.dp
                                        }

                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(240.dp)
                                                .clip(RoundedCornerShape(16.dp))
                                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            // The cropped sub-box
                                            Box(
                                                modifier = Modifier
                                                    .width(if (proImageCropRatio == "9:16") 135.dp else 220.dp)
                                                    .height(previewHeight)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(
                                                        Brush.linearGradient(
                                                            colors = if (proImageFilter == "Noir") {
                                                                listOf(Color(0xFF333333), Color(0xFF777777))
                                                            } else {
                                                                listOf(Color(0xFF1E3C72), Color(0xFF2A5298))
                                                            }
                                                        )
                                                    )
                                                    .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                // Real Filter overlay
                                                if (filterOverlayColor != Color.Transparent) {
                                                    Box(
                                                        modifier = Modifier
                                                            .fillMaxSize()
                                                            .background(filterOverlayColor)
                                                    )
                                                }

                                                // Dynamic text sticker overlay
                                                Column(
                                                    horizontalAlignment = Alignment.CenterHorizontally,
                                                    modifier = Modifier.padding(12.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Photo,
                                                        contentDescription = null,
                                                        tint = Color.White.copy(alpha = 0.3f),
                                                        modifier = Modifier.size(36.dp)
                                                    )
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    if (proImageTextSticker.isNotBlank()) {
                                                        Text(
                                                            text = proImageTextSticker,
                                                            color = proImageStickerColor,
                                                            fontWeight = FontWeight.ExtraBold,
                                                            fontSize = 14.sp,
                                                            textAlign = TextAlign.Center,
                                                            modifier = Modifier
                                                                .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        // Controls Card
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                                            shape = RoundedCornerShape(16.dp)
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(16.dp),
                                                verticalArrangement = Arrangement.spacedBy(16.dp)
                                            ) {
                                                Text("Visual Tuning Deck", fontWeight = FontWeight.Bold, fontSize = 13.sp)

                                                // Filter Presets Selection
                                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                                    Text("Color Grading Presets", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                        items(listOf("Original", "Cinematic", "Noir", "Vintage", "Cyberpunk", "Warm Sun", "Cold Ice")) { filter ->
                                                            val isSel = proImageFilter == filter
                                                            Box(
                                                                modifier = Modifier
                                                                    .clip(RoundedCornerShape(8.dp))
                                                                    .background(if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                                                    .clickable { proImageFilter = filter }
                                                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                                                            ) {
                                                                Text(
                                                                    text = filter,
                                                                    color = if (isSel) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                                                    fontSize = 11.sp,
                                                                    fontWeight = FontWeight.Bold
                                                                )
                                                            }
                                                        }
                                                    }
                                                }

                                                // Brightness Slider
                                                Column {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween
                                                    ) {
                                                        Text("Brightness", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                        Text("${proImageBrightness.toInt()}%", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                                    }
                                                    Slider(
                                                        value = proImageBrightness,
                                                        onValueChange = { proImageBrightness = it },
                                                        valueRange = 0f..200f,
                                                        modifier = Modifier.height(28.dp)
                                                    )
                                                }

                                                // Contrast Slider
                                                Column {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween
                                                    ) {
                                                        Text("Contrast", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                        Text("${proImageContrast.toInt()}%", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                                    }
                                                    Slider(
                                                        value = proImageContrast,
                                                        onValueChange = { proImageContrast = it },
                                                        valueRange = 0f..200f,
                                                        modifier = Modifier.height(28.dp)
                                                    )
                                                }

                                                // Saturation Slider
                                                Column {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween
                                                    ) {
                                                        Text("Saturation", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                        Text("${proImageSaturation.toInt()}%", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                                    }
                                                    Slider(
                                                        value = proImageSaturation,
                                                        onValueChange = { proImageSaturation = it },
                                                        valueRange = 0f..200f,
                                                        modifier = Modifier.height(28.dp)
                                                    )
                                                }

                                                // Aspect Ratio Selection
                                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                                    Text("Aspect Ratio Crop", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                        listOf("Original", "1:1", "16:9", "4:3", "9:16").forEach { ratio ->
                                                            val isSel = proImageCropRatio == ratio
                                                            Box(
                                                                modifier = Modifier
                                                                    .weight(1f)
                                                                    .clip(RoundedCornerShape(8.dp))
                                                                    .background(if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                                                    .clickable { proImageCropRatio = ratio }
                                                                    .padding(vertical = 6.dp),
                                                                contentAlignment = Alignment.Center
                                                            ) {
                                                                Text(
                                                                    text = ratio,
                                                                    color = if (isSel) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                                                    fontSize = 10.sp,
                                                                    fontWeight = FontWeight.Bold
                                                                )
                                                            }
                                                        }
                                                    }
                                                }

                                                // Sticker overlay
                                                OutlinedTextField(
                                                    value = proImageTextSticker,
                                                    onValueChange = { proImageTextSticker = it },
                                                    label = { Text("Overlay Graphic Text Sticker", fontSize = 11.sp) },
                                                    placeholder = { Text("Type overlay message...") },
                                                    modifier = Modifier.fillMaxWidth(),
                                                    shape = RoundedCornerShape(12.dp)
                                                )

                                                // Sticker color
                                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                                    Text("Sticker Color Palette", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                        listOf(Color.White, Color.Yellow, Color.Cyan, Color.Magenta, Color.Green, Color.Black).forEach { color ->
                                                            val isSel = proImageStickerColor == color
                                                            Box(
                                                                modifier = Modifier
                                                                    .size(28.dp)
                                                                    .clip(CircleShape)
                                                                    .background(color)
                                                                    .border(
                                                                        if (isSel) 2.dp else 1.dp,
                                                                        if (isSel) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.4f),
                                                                        CircleShape
                                                                    )
                                                                    .clickable { proImageStickerColor = color }
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    "VIDEO" -> {
                                        // 🎬 PROFESSIONAL VIDEO EDITOR WORKSPACE
                                        Text(
                                            text = "PRO MULTI-TRACK VIDEO TIMELINE DECK",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = NovaSecondary,
                                            letterSpacing = 1.sp,
                                            modifier = Modifier.align(Alignment.Start)
                                        )

                                        // Multi-track timeline mockup monitor
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = CardDefaults.cardColors(containerColor = Color.Black),
                                            shape = RoundedCornerShape(16.dp)
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(16.dp),
                                                verticalArrangement = Arrangement.spacedBy(10.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text("TIMELINE CHANNELS", color = Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                                        Box(
                                                            modifier = Modifier
                                                                .clip(RoundedCornerShape(4.dp))
                                                                .background(Color.Red.copy(alpha = 0.2f))
                                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                                        ) {
                                                            Text("TRIM: ${"%.1f".format(proVideoTrimStart)}s - ${"%.1f".format(proVideoTrimEnd)}s", color = Color.Red, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                                        }
                                                        Text("Speed: ${proVideoSpeed}x", color = NovaSecondary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                                    }
                                                }

                                                // Subtitle Track Channel
                                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                                    Icon(Icons.Default.Title, contentDescription = null, tint = Color.Cyan, modifier = Modifier.size(16.dp))
                                                    Box(modifier = Modifier.weight(1f).height(16.dp).background(Color.Cyan.copy(alpha = 0.15f), RoundedCornerShape(4.dp)), contentAlignment = Alignment.CenterStart) {
                                                        Text(
                                                            text = if (proVideoSubtitles.isNotBlank()) proVideoSubtitles else "No subtitle track active",
                                                            color = Color.White.copy(alpha = 0.8f),
                                                            fontSize = 8.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            modifier = Modifier.padding(horizontal = 6.dp)
                                                        )
                                                    }
                                                }

                                                // Main Video track channel
                                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                                    Icon(Icons.Default.Movie, contentDescription = null, tint = NovaSecondary, modifier = Modifier.size(16.dp))
                                                    Box(modifier = Modifier.weight(1f).height(24.dp).background(NovaSecondary.copy(alpha = 0.2f), RoundedCornerShape(4.dp)), contentAlignment = Alignment.Center) {
                                                        Text("Active Video Track (Ratio Cinema / Filter: $proImageFilter)", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                                    }
                                                }

                                                // Transition Marker Channel
                                                Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
                                                    Box(
                                                        modifier = Modifier
                                                            .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                                            .padding(horizontal = 10.dp, vertical = 4.dp)
                                                    ) {
                                                        Text("Cinematic Transition: $proVideoTransition", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Black)
                                                    }
                                                }

                                                // Audio BGM track channel
                                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                                    Icon(Icons.Default.MusicNote, contentDescription = null, tint = Color.Green, modifier = Modifier.size(16.dp))
                                                    Box(modifier = Modifier.weight(1f).height(16.dp).background(if (proVideoBgm != "None") Color.Green.copy(alpha = 0.2f) else Color.DarkGray, RoundedCornerShape(4.dp)), contentAlignment = Alignment.CenterStart) {
                                                        Text(
                                                            text = if (proVideoBgm != "None") "Music: $proVideoBgm (Volume: ${proVideoBgmVolume.toInt()}%)" else "No background music added",
                                                            color = Color.White,
                                                            fontSize = 8.sp,
                                                            modifier = Modifier.padding(horizontal = 6.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        // Video Controls Deck
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                                            shape = RoundedCornerShape(16.dp)
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(16.dp),
                                                verticalArrangement = Arrangement.spacedBy(16.dp)
                                            ) {
                                                Text("Professional Timeline Controls", fontWeight = FontWeight.Bold, fontSize = 13.sp)

                                                // Trim Range Sliders
                                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween
                                                    ) {
                                                        Text("Cut / Trim Range", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                        Text("${"%.1f".format(proVideoTrimStart)}s - ${"%.1f".format(proVideoTrimEnd)}s", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = NovaSecondary)
                                                    }
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Text("Start:", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(40.dp))
                                                        Slider(
                                                            value = proVideoTrimStart,
                                                            onValueChange = { if (it < proVideoTrimEnd) proVideoTrimStart = it },
                                                            valueRange = 0f..15f,
                                                            modifier = Modifier.weight(1f)
                                                        )
                                                    }
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Text("End:", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(40.dp))
                                                        Slider(
                                                            value = proVideoTrimEnd,
                                                            onValueChange = { if (it > proVideoTrimStart) proVideoTrimEnd = it },
                                                            valueRange = 0f..15f,
                                                            modifier = Modifier.weight(1f)
                                                        )
                                                    }
                                                }

                                                // Speed selector buttons
                                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                                    Text("Timeline Playback Speed Curve", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                        listOf(0.5f, 1.0f, 1.5f, 2.0f, 4.0f).forEach { speed ->
                                                            val isSel = proVideoSpeed == speed
                                                            Box(
                                                                modifier = Modifier
                                                                    .weight(1f)
                                                                    .clip(RoundedCornerShape(8.dp))
                                                                    .background(if (isSel) NovaSecondary else MaterialTheme.colorScheme.surfaceVariant)
                                                                    .clickable { proVideoSpeed = speed }
                                                                    .padding(vertical = 6.dp),
                                                                contentAlignment = Alignment.Center
                                                            ) {
                                                                Text(
                                                                    text = "${speed}x",
                                                                    color = if (isSel) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                                                    fontSize = 11.sp,
                                                                    fontWeight = FontWeight.Bold
                                                                )
                                                            }
                                                        }
                                                    }
                                                }

                                                // BGM selector
                                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                                    Text("Background Soundtrack", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                        listOf("None", "Ambient Zen", "Cyberbeat", "Lo-Fi", "Corporate").forEach { bgm ->
                                                            val isSel = (proVideoBgm.split(" ")[0] == bgm)
                                                            Box(
                                                                modifier = Modifier
                                                                    .weight(1f)
                                                                    .clip(RoundedCornerShape(8.dp))
                                                                    .background(if (isSel) NovaSecondary else MaterialTheme.colorScheme.surfaceVariant)
                                                                    .clickable { proVideoBgm = bgm }
                                                                    .padding(vertical = 6.dp),
                                                                contentAlignment = Alignment.Center
                                                            ) {
                                                                Text(
                                                                    text = bgm,
                                                                    color = if (isSel) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                                                    fontSize = 10.sp,
                                                                    fontWeight = FontWeight.Bold
                                                                )
                                                            }
                                                        }
                                                    }
                                                }

                                                // BGM Volume
                                                if (proVideoBgm != "None") {
                                                    Column {
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            horizontalArrangement = Arrangement.SpaceBetween
                                                        ) {
                                                            Text("Music Track Volume Mix", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                            Text("${proVideoBgmVolume.toInt()}%", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = NovaSecondary)
                                                        }
                                                        Slider(
                                                            value = proVideoBgmVolume,
                                                            onValueChange = { proVideoBgmVolume = it },
                                                            valueRange = 0f..100f,
                                                            modifier = Modifier.height(28.dp)
                                                        )
                                                    }
                                                }

                                                // Subtitle overlay text
                                                OutlinedTextField(
                                                    value = proVideoSubtitles,
                                                    onValueChange = { proVideoSubtitles = it },
                                                    label = { Text("Generate Subtitle Layer Overlay", fontSize = 11.sp) },
                                                    placeholder = { Text("Type subtitle track line...") },
                                                    modifier = Modifier.fillMaxWidth(),
                                                    shape = RoundedCornerShape(12.dp)
                                                )

                                                // Cinematic Transitions
                                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                                    Text("Layer Transition effect", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                        listOf("None", "Fade Out", "Dissolve", "Radial Zoom", "Whip Pan").forEach { trans ->
                                                            val isSel = proVideoTransition == trans
                                                            Box(
                                                                modifier = Modifier
                                                                    .weight(1f)
                                                                    .clip(RoundedCornerShape(8.dp))
                                                                    .background(if (isSel) NovaSecondary else MaterialTheme.colorScheme.surfaceVariant)
                                                                    .clickable { proVideoTransition = trans }
                                                                    .padding(vertical = 6.dp),
                                                                contentAlignment = Alignment.Center
                                                            ) {
                                                                Text(
                                                                    text = trans.split(" ")[0],
                                                                    color = if (isSel) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                                                    fontSize = 9.sp,
                                                                    fontWeight = FontWeight.Bold
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    "AUDIO" -> {
                                        // 🎙️ PROFESSIONAL AUDIO WORKSPACE
                                        Text(
                                            text = "PRO AUDIO SPLICING & VOICE OVER SYNTHESIS DECK",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFFF9800),
                                            letterSpacing = 1.sp,
                                            modifier = Modifier.align(Alignment.Start)
                                        )

                                        // Audio bouncing waveforms
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = CardDefaults.cardColors(containerColor = Color(0xFF151515)),
                                            shape = RoundedCornerShape(16.dp)
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(16.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.spacedBy(12.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text("VOICEOVER WAVEFORM MONITOR", color = Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                                    Box(
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(4.dp))
                                                            .background(Color(0xFFFF9800).copy(alpha = 0.2f))
                                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                                    ) {
                                                        Text("ANC: ${if (proAudioNoiseReduction) "ACTIVE" else "OFF"}", color = Color(0xFFFF9800), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                                    }
                                                }

                                                // Bouncing wave bars simulation
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(60.dp),
                                                    horizontalArrangement = Arrangement.SpaceEvenly,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    val randomHeights = listOf(20, 45, 30, 55, 15, 35, 50, 40, 25, 48, 55, 12, 38, 45, 20)
                                                    randomHeights.forEachIndexed { idx, ht ->
                                                        val finalHt = if (proAudioEffectPreset != "None") {
                                                            ht * 1.15f
                                                        } else {
                                                            ht.toFloat()
                                                        }
                                                        Box(
                                                            modifier = Modifier
                                                                .width(4.dp)
                                                                .height((finalHt * proAudioVolumeBoost).coerceAtMost(60f).dp)
                                                                .clip(RoundedCornerShape(2.dp))
                                                                .background(
                                                                    if (idx in 3..11) Color(0xFFFF9800) else Color(0xFFFF9800).copy(alpha = 0.4f)
                                                                )
                                                        )
                                                    }
                                                }

                                                Text(
                                                    text = "Master Pitch Ratio: ${"%.2f".format(proAudioPitchShift)}x | Preset Effect: $proAudioEffectPreset",
                                                    color = Color.LightGray,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }

                                        // Audio Controls
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                                            shape = RoundedCornerShape(16.dp)
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(16.dp),
                                                verticalArrangement = Arrangement.spacedBy(16.dp)
                                            ) {
                                                Text("Voice & Soundwave Sculpting Deck", fontWeight = FontWeight.Bold, fontSize = 13.sp)

                                                // Volume Boost
                                                Column {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween
                                                    ) {
                                                        Text("Volume Gain Multiplier", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                        Text("${"%.1f".format(proAudioVolumeBoost)}x Gain", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFF9800))
                                                    }
                                                    Slider(
                                                        value = proAudioVolumeBoost,
                                                        onValueChange = { proAudioVolumeBoost = it },
                                                        valueRange = 0.5f..2.5f,
                                                        modifier = Modifier.height(28.dp),
                                                        colors = SliderDefaults.colors(activeTrackColor = Color(0xFFFF9800))
                                                    )
                                                }

                                                // Pitch shift
                                                Column {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween
                                                    ) {
                                                        Text("Voice Pitch Level Shift", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                        Text("${"%.2f".format(proAudioPitchShift)}x Pitch", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFF9800))
                                                    }
                                                    Slider(
                                                        value = proAudioPitchShift,
                                                        onValueChange = { proAudioPitchShift = it },
                                                        valueRange = 0.5f..1.5f,
                                                        modifier = Modifier.height(28.dp),
                                                        colors = SliderDefaults.colors(activeTrackColor = Color(0xFFFF9800))
                                                    )
                                                }

                                                // Trim start and end for Audio
                                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween
                                                    ) {
                                                        Text("Audio Trimming Range", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                        Text("${proAudioTrimStart.toInt()}s - ${proAudioTrimEnd.toInt()}s", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFF9800))
                                                    }
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Text("Start:", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(40.dp))
                                                        Slider(
                                                            value = proAudioTrimStart,
                                                            onValueChange = { if (it < proAudioTrimEnd) proAudioTrimStart = it },
                                                            valueRange = 0f..60f,
                                                            modifier = Modifier.weight(1f),
                                                            colors = SliderDefaults.colors(activeTrackColor = Color(0xFFFF9800))
                                                        )
                                                    }
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Text("End:", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(40.dp))
                                                        Slider(
                                                            value = proAudioTrimEnd,
                                                            onValueChange = { if (it > proAudioTrimStart) proAudioTrimEnd = it },
                                                            valueRange = 0f..60f,
                                                            modifier = Modifier.weight(1f),
                                                            colors = SliderDefaults.colors(activeTrackColor = Color(0xFFFF9800))
                                                        )
                                                    }
                                                }

                                                // Voice presets
                                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                                    Text("Voice Modulator Effect Presets", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                        listOf("None", "Studio Warmth", "Deep Resonance", "Concert Echo", "Space Alien").forEach { preset ->
                                                            val isSel = proAudioEffectPreset == preset
                                                            Box(
                                                                modifier = Modifier
                                                                    .weight(1f)
                                                                    .clip(RoundedCornerShape(8.dp))
                                                                    .background(if (isSel) Color(0xFFFF9800) else MaterialTheme.colorScheme.surfaceVariant)
                                                                    .clickable { proAudioEffectPreset = preset }
                                                                    .padding(vertical = 8.dp),
                                                                contentAlignment = Alignment.Center
                                                            ) {
                                                                Text(
                                                                    text = preset.split(" ")[0],
                                                                    color = if (isSel) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                                                    fontSize = 9.sp,
                                                                    fontWeight = FontWeight.Bold
                                                                )
                                                            }
                                                        }
                                                    }
                                                }

                                                // Active de-noise switch
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text("Active Smart De-Noise (ANC)", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                                        Text("Remove static noise, hum, and wind interference via AI", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                    }
                                                    Switch(
                                                        checked = proAudioNoiseReduction,
                                                        onCheckedChange = { proAudioNoiseReduction = it },
                                                        colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFFFF9800))
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    "TEXT" -> {
                                        // 📝 PROFESSIONAL TEXT / SCRIPTWRITER WORKSPACE
                                        Text(
                                            text = "PRO SCRIPTER & EDITORIAL COMPOSER DESK",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = NovaTertiary,
                                            letterSpacing = 1.sp,
                                            modifier = Modifier.align(Alignment.Start)
                                        )

                                        // Character counter and visual typing sheet
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)),
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f)),
                                            shape = RoundedCornerShape(16.dp)
                                        ) {
                                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Text("SCRIPT TYPE SHEET", color = NovaTertiary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                                    Text(
                                                        "Words: ${proTextContentBody.split("\\s+".toRegex()).filter { it.isNotBlank() }.size} | Chars: ${proTextContentBody.length}",
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.SemiBold
                                                    )
                                                }

                                                // Live dynamic text visual sheet
                                                OutlinedTextField(
                                                    value = proTextContentBody,
                                                    onValueChange = { proTextContentBody = it },
                                                    textStyle = androidx.compose.ui.text.TextStyle(
                                                        fontSize = proTextFontSize.sp,
                                                        fontFamily = when (proTextFontFamily) {
                                                            "Monospace" -> androidx.compose.ui.text.font.FontFamily.Monospace
                                                            "Serif" -> androidx.compose.ui.text.font.FontFamily.Serif
                                                            else -> androidx.compose.ui.text.font.FontFamily.SansSerif
                                                        },
                                                        fontWeight = if (proTextStyleBold) FontWeight.Bold else FontWeight.Normal,
                                                        fontStyle = if (proTextStyleItalic) androidx.compose.ui.text.font.FontStyle.Italic else androidx.compose.ui.text.font.FontStyle.Normal,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    ),
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .heightIn(min = 120.dp, max = 220.dp)
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(MaterialTheme.colorScheme.surface)
                                                        .border(1.dp, MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                                        .padding(12.dp)
                                                )
                                            }
                                        }

                                        // Script Controls
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                                            shape = RoundedCornerShape(16.dp)
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(16.dp),
                                                verticalArrangement = Arrangement.spacedBy(16.dp)
                                            ) {
                                                Text("Editorial & Typography Deck", fontWeight = FontWeight.Bold, fontSize = 13.sp)

                                                // Font size slider
                                                Column {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween
                                                    ) {
                                                        Text("Font Typography Size Scale", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                        Text("${proTextFontSize.toInt()}sp", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = NovaTertiary)
                                                    }
                                                    Slider(
                                                        value = proTextFontSize,
                                                        onValueChange = { proTextFontSize = it },
                                                        valueRange = 12f..30f,
                                                        modifier = Modifier.height(28.dp),
                                                        colors = SliderDefaults.colors(activeTrackColor = NovaTertiary)
                                                    )
                                                }

                                                // Font Family selector
                                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                                    Text("Font Serif/Monospace Families", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                        listOf("Sans-Serif", "Serif", "Monospace").forEach { family ->
                                                            val isSel = proTextFontFamily == family
                                                            Box(
                                                                modifier = Modifier
                                                                    .weight(1f)
                                                                    .clip(RoundedCornerShape(8.dp))
                                                                    .background(if (isSel) NovaTertiary else MaterialTheme.colorScheme.surfaceVariant)
                                                                    .clickable { proTextFontFamily = family }
                                                                    .padding(vertical = 8.dp),
                                                                contentAlignment = Alignment.Center
                                                            ) {
                                                                Text(
                                                                    text = family,
                                                                    color = if (isSel) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                                                    fontSize = 11.sp,
                                                                    fontWeight = FontWeight.Bold
                                                                )
                                                            }
                                                        }
                                                    }
                                                }

                                                // Styling weights: Bold & Italic toggles
                                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                                    Text("Weight & Emphasis", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                                        // Bold button
                                                        Box(
                                                            modifier = Modifier
                                                                .weight(1f)
                                                                .clip(RoundedCornerShape(8.dp))
                                                                .background(if (proTextStyleBold) NovaTertiary else MaterialTheme.colorScheme.surfaceVariant)
                                                                .clickable { proTextStyleBold = !proTextStyleBold }
                                                                .padding(vertical = 8.dp),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Text(
                                                                text = "Bold (B)",
                                                                color = if (proTextStyleBold) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                                                fontSize = 11.sp,
                                                                fontWeight = FontWeight.Bold
                                                            )
                                                        }

                                                        // Italic button
                                                        Box(
                                                            modifier = Modifier
                                                                .weight(1f)
                                                                .clip(RoundedCornerShape(8.dp))
                                                                .background(if (proTextStyleItalic) NovaTertiary else MaterialTheme.colorScheme.surfaceVariant)
                                                                .clickable { proTextStyleItalic = !proTextStyleItalic }
                                                                .padding(vertical = 8.dp),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Text(
                                                                text = "Italic (I)",
                                                                color = if (proTextStyleItalic) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                                                fontSize = 11.sp,
                                                                fontWeight = FontWeight.Bold
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                // Reset and Save Actions for the Professional Editor Suite
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 12.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            // Reset active tab states
                                            when (selectedProfessionalTab) {
                                                "IMAGE" -> {
                                                    proImageFilter = "Original"
                                                    proImageBrightness = 100f
                                                    proImageContrast = 100f
                                                    proImageSaturation = 100f
                                                    proImageCropRatio = "Original"
                                                    proImageTextSticker = ""
                                                    proImageStickerColor = Color.White
                                                }
                                                "VIDEO" -> {
                                                    proVideoTrimStart = 0f
                                                    proVideoTrimEnd = 10f
                                                    proVideoSpeed = 1.0f
                                                    proVideoBgm = "None"
                                                    proVideoBgmVolume = 50f
                                                    proVideoSubtitles = ""
                                                    proVideoTransition = "None"
                                                }
                                                "AUDIO" -> {
                                                    proAudioVolumeBoost = 1.0f
                                                    proAudioEffectPreset = "None"
                                                    proAudioTrimStart = 0f
                                                    proAudioTrimEnd = 30f
                                                    proAudioPitchShift = 1.0f
                                                    proAudioNoiseReduction = false
                                                }
                                                "TEXT" -> {
                                                    proTextFontSize = 16f
                                                    proTextFontFamily = "Sans-Serif"
                                                    proTextStyleBold = false
                                                    proTextStyleItalic = false
                                                    proTextContentBody = "Welcome to the Professional Script Desk. Start drafting your cinematic screenplay or voiceover prompt script here. Fully optimized for text synthesis."
                                                }
                                            }
                                            Toast.makeText(context, "${selectedProfessionalTab} edits reset to defaults.", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(50.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text(
                                            text = "Reset",
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    Button(
                                        onClick = {
                                            val proFormatName = when(selectedProfessionalTab) {
                                                "IMAGE" -> "Visual Image Master"
                                                "VIDEO" -> "Cinematic Video Cut"
                                                "AUDIO" -> "Voiceover Synthesis WAV"
                                                "TEXT" -> "Screenplay Copy script"
                                                else -> "Composite Draft"
                                            }
                                            Toast.makeText(context, "Initializing Pro Core compilation rendering for ${proFormatName}...", Toast.LENGTH_SHORT).show()
                                            coroutineScope.launch {
                                                delay(1600)
                                                Toast.makeText(context, "Successfully compiled, graded, and saved ${proFormatName} to Studio!", Toast.LENGTH_LONG).show()
                                                activeEditorType = null
                                            }
                                        },
                                        modifier = Modifier
                                            .weight(1.8f)
                                            .height(50.dp)
                                            .testTag("pro_editor_save_button"),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = when(selectedProfessionalTab) {
                                                "IMAGE" -> NovaPrimary
                                                "VIDEO" -> NovaSecondary
                                                "AUDIO" -> Color(0xFFFF9800)
                                                else -> NovaTertiary
                                            }
                                        ),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                            Text(
                                                text = "Save Pro Render",
                                                color = Color.White,
                                                fontWeight = FontWeight.ExtraBold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }        // SYSTEM MEDIA GALLERY PICKER OVERLAY
        if (showGalleryDialog) {
            Dialog(onDismissRequest = { showGalleryDialog = false }) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.95f)
                        .wrapContentHeight()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PhotoLibrary,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "System Media Gallery",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            IconButton(onClick = { showGalleryDialog = false }) {
                                Icon(Icons.Default.Close, contentDescription = "Close gallery")
                            }
                        }

                        Text(
                            text = "Select a high-fidelity video or image reference file to upload into NovaAI Studio:",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        // 6 Mock gallery files
                        val mockFiles = listOf(
                            Pair("CINEMATIC_REEL_01.MP4", "Video • 12.4 MB"),
                            Pair("GOLDEN_HOUR_PRESET.PNG", "Image • 2.1 MB"),
                            Pair("CYBERPUNK_CAR_CLIP.MP4", "Video • 24.5 MB"),
                            Pair("WATERCOLOR_PORTRAIT.JPG", "Image • 1.8 MB"),
                            Pair("RETRO_STYLE_GRAIN.PNG", "Image • 3.2 MB"),
                            Pair("VEO_TEST_MOTION.MP4", "Video • 15.1 MB")
                        )

                        Column(
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            mockFiles.forEach { file ->
                                val isVideo = file.first.endsWith(".MP4")
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            attachImageSelected = true
                                            showGalleryDialog = false
                                            Toast.makeText(context, "${file.first} successfully uploaded as active studio reference!", Toast.LENGTH_LONG).show()
                                        },
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .background(
                                                    if (isVideo) NovaSecondary.copy(alpha = 0.15f) else NovaPrimary.copy(alpha = 0.15f),
                                                    RoundedCornerShape(8.dp)
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = if (isVideo) Icons.Default.PlayCircle else Icons.Default.Image,
                                                contentDescription = null,
                                                tint = if (isVideo) NovaSecondary else NovaPrimary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = file.first,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = file.second,
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                            )
                                        }

                                        Icon(
                                            imageVector = Icons.Default.CloudUpload,
                                            contentDescription = "Upload",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // BEAUTIFUL BACKGROUND GENERATION OVERLAY
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

                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(100.dp)
                        ) {
                            CircularProgressIndicator(
                                progress = { progress },
                                color = MaterialTheme.colorScheme.primary,
                                strokeWidth = 6.dp,
                                modifier = Modifier.fillMaxSize()
                            )

                            val ringTransition = rememberInfiniteTransition(label = "ring_rotation")
                            val ringRotation by ringTransition.animateFloat(
                                initialValue = 0f,
                                targetValue = 360f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(1500, easing = LinearEasing),
                                    repeatMode = RepeatMode.Restart
                                ),
                                label = "angle"
                            )

                            Canvas(modifier = Modifier.fillMaxSize().padding(6.dp)) {
                                rotate(ringRotation) {
                                    drawArc(
                                        color = NovaSecondary.copy(alpha = 0.4f),
                                        startAngle = 0f,
                                        sweepAngle = 120f,
                                        useCenter = false,
                                        style = Stroke(width = 2.5.dp.toPx())
                                    )
                                }
                            }

                            val genIcon = when (creationType) {
                                "VIDEO" -> Icons.Default.Movie
                                "IMAGE" -> Icons.Default.Brush
                                "VOICEOVER" -> Icons.Default.Mic
                                else -> Icons.Default.AutoAwesome
                            }

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = genIcon,
                                    contentDescription = null,
                                    tint = NovaPrimary,
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${(progress * 100).toInt()}%",
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = (12 * scale).sp
                                )
                            }
                        }

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
