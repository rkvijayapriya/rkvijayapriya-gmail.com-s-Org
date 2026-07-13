package com.example.ui.screens

import android.graphics.BitmapFactory
import android.media.MediaPlayer
import android.util.Base64
import android.content.Intent
import android.speech.RecognizerIntent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import com.example.data.database.VisionAiCreation
import com.example.ui.MainViewModel
import com.example.ui.components.ToolInputProcessingOverlay
import com.example.ui.components.VideoPanelSkeleton
import com.example.ui.theme.NovaGradient
import com.example.ui.theme.NovaPrimary
import com.example.ui.theme.NovaSecondary
import com.example.ui.theme.NovaTertiary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.sin

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
    val coroutineScope = rememberCoroutineScope()

    val triggerExport = remember(context) {
        { creation: VisionAiCreation ->
            com.example.ui.components.MediaExportHelper.exportCreation(context, creation) { success, path ->
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    if (success) {
                        Toast.makeText(context, "Exported successfully: $path", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(context, "Export failed: $path", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    // Active Tab State: 0 for Video, 1 for Image, 2 for Audio, 3 for Template, 4 for Song
    var activeTab by remember { mutableStateOf(0) }

    // State for image generation
    var imagenPrompt by remember { mutableStateOf("") }
    var selectedStyle by remember { mutableStateOf("Cinematic") }
    var lastGeneratedCreation by remember { mutableStateOf<VisionAiCreation?>(null) }
    
    // State for video generation (Veo)
    var veoPrompt by remember { mutableStateOf("") }
    var selectedVeoStyle by remember { mutableStateOf("Cinematic") }
    var selectedVeoCameraAngle by remember { mutableStateOf("Drone Orbit") }
    var selectedVeoResolution by remember { mutableStateOf("1080p") }
    var selectedVeoFps by remember { mutableStateOf(30) }
    var selectedVeoDuration by remember { mutableStateOf(5) }
    var selectedVeoRatio by remember { mutableStateOf("16:9") }
    var lastGeneratedVideoCreation by remember { mutableStateOf<VisionAiCreation?>(null) }

    // Video player local states for preview inside CreatorScreen
    var isVeoPlaying by remember { mutableStateOf(true) }
    var veoPlayerProgress by remember { mutableStateOf(0.0f) }
    var veoPlayLoopCount by remember { mutableStateOf(0) }

    val videoStyles = listOf(
        "Cinematic", "Photorealistic", "Anime Noir", "Cyberpunk", "3D Pixar",
        "Surreal Dreamscape", "Claymation", "Macro Closeup", "Vintage 35mm"
    )

    val cameraAngles = listOf(
        "Drone Orbit", "Crane Zoom", "Pan Left-to-Right", "Low Angle Tilt", "Static Cinematic"
    )

    // Interactive rendering simulation logic for Veo preview
    LaunchedEffect(isVeoPlaying, lastGeneratedVideoCreation) {
        if (isVeoPlaying && lastGeneratedVideoCreation != null) {
            while (isVeoPlaying) {
                veoPlayerProgress += 0.02f
                if (veoPlayerProgress >= 1.0f) {
                    veoPlayerProgress = 0.0f
                    veoPlayLoopCount++
                }
                delay(100)
            }
        }
    }
    
    // State for ElevenLabs AI Voiceover Generator
    var voiceoverText by remember { mutableStateOf("") }
    val creativeVoiceoverProfiles = listOf(
        Pair("Serena (Warm Narrator)", "21m00Tcm4TlvDq8ikWAM"),
        Pair("Arthur (Bold Broadcaster)", "pNInz6obpg7656pky15p"),
        Pair("Sophia (Upbeat Conversational)", "EXAVITQu4vr4xnSDxMaL"),
        Pair("James (Professional Executive)", "ErXwobaYiN019PkySvjV"),
        Pair("Kamala (Inspirational Narrator)", "MF3mGyEYCl7YOf7k8ebS"),
        Pair("Aanya (Warm Storyteller)", "zrHiExvYt7vI1AlS7xgB")
    )
    var selectedVoiceoverProfile by remember { mutableStateOf(creativeVoiceoverProfiles[0]) }
    var voiceoverStability by remember { mutableStateOf(0.5f) }
    var voiceoverSimilarity by remember { mutableStateOf(0.75f) }
    var lastGeneratedVoiceoverCreation by remember { mutableStateOf<VisionAiCreation?>(null) }
    
    // State for Gemini Template Studio
    var templateTopic by remember { mutableStateOf("") }
    val templateTypes = listOf(
        "Video Script Template",
        "Social Media Ad Copy",
        "Creative Story Outline",
        "Educational Lesson Plan",
        "Technical Documentation"
    )
    var selectedTemplateType by remember { mutableStateOf(templateTypes[0]) }
    var selectedTemplateTone by remember { mutableStateOf("Professional") }
    var generatedTemplateContent by remember { mutableStateOf("") }
    var isGeneratingTemplate by remember { mutableStateOf(false) }

    // State for Suno & Udio AI Song Studio
    var songPrompt by remember { mutableStateOf("") }
    val songGenres = listOf(
        "Synthwave Retro",
        "Lofi Chill Beats",
        "Cinematic Pop",
        "Acoustic Indie Rock",
        "Cyberpunk Techno"
    )
    var selectedSongGenre by remember { mutableStateOf(songGenres[0]) }
    var selectedSongMood by remember { mutableStateOf("Upbeat & Energetic") }
    var generatedSongLyrics by remember { mutableStateOf("") }
    var generatedSongTitle by remember { mutableStateOf("") }
    var isGeneratingSong by remember { mutableStateOf(false) }
    var lastGeneratedSongCreation by remember { mutableStateOf<VisionAiCreation?>(null) }
    var isSongPlaying by remember { mutableStateOf(false) }
    var songProgress by remember { mutableStateOf(0f) }

    var activeSpeechTarget by remember { mutableStateOf<String?>(null) }
    var activeFileTarget by remember { mutableStateOf<String?>(null) }

    var selectedMediaUriImagen by remember { mutableStateOf<Uri?>(null) }
    var selectedMediaNameImagen by remember { mutableStateOf<String?>(null) }

    var selectedMediaUriVeo by remember { mutableStateOf<Uri?>(null) }
    var selectedMediaNameVeo by remember { mutableStateOf<String?>(null) }

    var selectedMediaUriVoiceover by remember { mutableStateOf<Uri?>(null) }
    var selectedMediaNameVoiceover by remember { mutableStateOf<String?>(null) }

    var selectedMediaUriTemplate by remember { mutableStateOf<Uri?>(null) }
    var selectedMediaNameTemplate by remember { mutableStateOf<String?>(null) }

    var selectedMediaUriSong by remember { mutableStateOf<Uri?>(null) }
    var selectedMediaNameSong by remember { mutableStateOf<String?>(null) }

    var selectedMediaUriAnalysis by remember { mutableStateOf<Uri?>(null) }
    var selectedMediaNameAnalysis by remember { mutableStateOf<String?>(null) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val name = uri.lastPathSegment ?: "Selected File"
            when (activeFileTarget) {
                "imagen" -> {
                    selectedMediaUriImagen = uri
                    selectedMediaNameImagen = name
                }
                "veo" -> {
                    selectedMediaUriVeo = uri
                    selectedMediaNameVeo = name
                }
                "voiceover" -> {
                    selectedMediaUriVoiceover = uri
                    selectedMediaNameVoiceover = name
                }
                "template" -> {
                    selectedMediaUriTemplate = uri
                    selectedMediaNameTemplate = name
                }
                "song" -> {
                    selectedMediaUriSong = uri
                    selectedMediaNameSong = name
                }
                "analysis" -> {
                    selectedMediaUriAnalysis = uri
                    selectedMediaNameAnalysis = name
                }
            }
            Toast.makeText(context, "File selected: $name", Toast.LENGTH_SHORT).show()
        }
    }

    val speechRecognizerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val data = result.data
            val results = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            if (!results.isNullOrEmpty()) {
                val spokenText = results[0]
                when (activeSpeechTarget) {
                    "imagen" -> imagenPrompt = if (imagenPrompt.isBlank()) spokenText else "$imagenPrompt $spokenText"
                    "veo" -> veoPrompt = if (veoPrompt.isBlank()) spokenText else "$veoPrompt $spokenText"
                    "voiceover" -> voiceoverText = if (voiceoverText.isBlank()) spokenText else "$voiceoverText $spokenText"
                    "template" -> templateTopic = if (templateTopic.isBlank()) spokenText else "$templateTopic $spokenText"
                    "song" -> songPrompt = if (songPrompt.isBlank()) spokenText else "$songPrompt $spokenText"
                }
                Toast.makeText(context, "Voice input appended!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Polling progress of playing song
    LaunchedEffect(isSongPlaying) {
        if (isSongPlaying) {
            while (isSongPlaying) {
                songProgress += 0.015f
                if (songProgress >= 1.0f) {
                    isSongPlaying = false
                    songProgress = 1.0f
                }
                delay(100)
            }
        }
    }
    
    // Playback state for preview of generated voiceover in CreatorScreen
    var isVoiceoverPlaying by remember { mutableStateOf(false) }
    var voiceoverProgress by remember { mutableStateOf(0f) }
    val localVoiceoverPlayer = remember { MediaPlayer() }
    
    // Clean MediaPlayer closure when navigating out
    DisposableEffect(Unit) {
        onDispose {
            try {
                localVoiceoverPlayer.stop()
                localVoiceoverPlayer.release()
            } catch (e: Exception) {
                // ignore
            }
        }
    }
    
    // Polling progress of playing audio
    LaunchedEffect(isVoiceoverPlaying) {
        if (isVoiceoverPlaying) {
            while (isVoiceoverPlaying) {
                try {
                    val current = localVoiceoverPlayer.currentPosition
                    val duration = localVoiceoverPlayer.duration.coerceAtLeast(100)
                    voiceoverProgress = (current.toFloat() / duration).coerceIn(0f, 1f)
                    if (!localVoiceoverPlayer.isPlaying) {
                        isVoiceoverPlaying = false
                        voiceoverProgress = 1.0f
                    }
                } catch (e: Exception) {
                    isVoiceoverPlaying = false
                }
                delay(100)
            }
        }
    }

    // Zoom/Fullscreen Dialog State
    var fullscreenCreation by remember { mutableStateOf<VisionAiCreation?>(null) }

    val isGenerating by viewModel.isGenerating.collectAsState()
    val generationProgress by viewModel.generationProgress.collectAsState()
    val generationStep by viewModel.generationStep.collectAsState()
    val workspaceImages by viewModel.workspaceImages.collectAsState()

    val imageStyles = listOf("Cinematic", "Anime", "Cyberpunk", "Watercolor", "3D Render")

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                        MaterialTheme.colorScheme.background
                    ),
                    radius = 2000f
                )
            )
            .systemBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Section: High-Fidelity Studio Branding with ample padding
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(NovaGradient, CircleShape)
                        .padding(2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "Nova Spark Icon",
                            tint = NovaPrimary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                Text(
                    text = "Nova Creative Desk",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onBackground,
                    letterSpacing = (-0.5).sp
                )

                Text(
                    text = "Co-create with Google Veo, Imagen 3, ElevenLabs, and Gemini in a single unified workspace.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            // Modern, Sleek Responsive Tab Navigation inside Workspace
            BoxWithConstraints(
                modifier = Modifier.fillMaxWidth()
            ) {
                val isSmallScreen = maxWidth < 480.dp
                val tabsList = listOf(
                    Triple(0, "Video", Icons.Default.Videocam),
                    Triple(1, "Image", Icons.Default.Image),
                    Triple(2, "Audio", Icons.Default.Mic),
                    Triple(3, "Template", Icons.Default.Description),
                    Triple(4, "Song", Icons.Default.Audiotrack),
                    Triple(5, "Analysis", Icons.Default.Explore)
                )

                if (isSmallScreen) {
                    var menuExpanded by remember { mutableStateOf(false) }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("creative_desk_tabs_container")
                    ) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { menuExpanded = true }
                                .testTag("creative_desk_tabs_dropdown_trigger"),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val currentTabTuple = tabsList.find { it.first == activeTab } ?: tabsList[0]
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = currentTabTuple.third,
                                            contentDescription = "${currentTabTuple.second} Tab Icon",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = "Active Workspace",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            text = currentTabTuple.second,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                    }
                                }
                                Icon(
                                    imageVector = Icons.Default.ExpandMore,
                                    contentDescription = "Expand Workspace Menu",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false },
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surface)
                        ) {
                            tabsList.forEach { (index, title, icon) ->
                                val isSelected = activeTab == index
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Icon(
                                                imageVector = icon,
                                                contentDescription = null,
                                                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Text(
                                                text = title,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                fontSize = 14.sp
                                            )
                                        }
                                    },
                                    onClick = {
                                        activeTab = index
                                        menuExpanded = false
                                    },
                                    modifier = Modifier.testTag("tab_$title")
                                )
                            }
                        }
                    }
                } else {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("creative_desk_tabs_container"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            tabsList.forEach { (index, title, icon) ->
                                val isSelected = activeTab == index
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
                                        )
                                        .clickable { activeTab = index }
                                        .padding(horizontal = 14.dp, vertical = 8.dp)
                                        .testTag("tab_$title"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = "$title Tab",
                                            tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = title,
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // SECTION 1: Imagen AI Generator Component (Tab 1)
            AnimatedVisibility(
                visible = (activeTab == 1),
                enter = fadeIn(animationSpec = tween(300)) + expandVertically(),
                exit = fadeOut(animationSpec = tween(150)) + shrinkVertically(),
                modifier = Modifier.fillMaxWidth()
            ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("imagen_generator_component"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Brush,
                            contentDescription = "Imagen Brush",
                            tint = NovaPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Google Imagen 3 Studio",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Prompt Input Field for Imagen
                    OutlinedTextField(
                        value = imagenPrompt,
                        onValueChange = { imagenPrompt = it },
                        placeholder = {
                            Text(
                                text = "Describe what you want Imagen 3 to paint...",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                        },
                        trailingIcon = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(end = 4.dp)
                            ) {
                                IconButton(
                                    onClick = {
                                        activeFileTarget = "imagen"
                                        filePickerLauncher.launch("*/*")
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Upload files image video",
                                        tint = NovaPrimary
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        activeSpeechTarget = "imagen"
                                        try {
                                            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                                putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak now...")
                                            }
                                            speechRecognizerLauncher.launch(intent)
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "Speech recognition not supported on this device", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Mic,
                                        contentDescription = "Speech recognition",
                                        tint = NovaPrimary
                                    )
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("imagen_prompt_field"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NovaPrimary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                            focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.3f)
                        ),
                        shape = RoundedCornerShape(16.dp)
                    )

                    selectedMediaUriImagen?.let { uri ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.AttachFile, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                            Text(
                                text = "Attached: $selectedMediaNameImagen",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            IconButton(
                                onClick = {
                                    selectedMediaUriImagen = null
                                    selectedMediaNameImagen = null
                                },
                                modifier = Modifier.size(16.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Remove media", modifier = Modifier.size(12.dp))
                            }
                        }
                    }

                    // Quick Style Selector
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Artistic Style",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            imageStyles.forEach { style ->
                                val isSelected = selectedStyle == style
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isSelected) NovaPrimary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface)
                                        .border(
                                            1.dp,
                                            if (isSelected) NovaPrimary else MaterialTheme.colorScheme.outline.copy(alpha = 0.1f),
                                            RoundedCornerShape(10.dp)
                                        )
                                        .clickable { selectedStyle = style }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = style,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) NovaPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    // Generate Button
                    Button(
                        onClick = {
                            if (imagenPrompt.isBlank()) {
                                Toast.makeText(context, "Please write a text prompt first!", Toast.LENGTH_SHORT).show()
                            } else {
                                viewModel.triggerImageGeneration(
                                    prompt = imagenPrompt,
                                    style = selectedStyle
                                ) { creation ->
                                    lastGeneratedCreation = creation
                                    Toast.makeText(context, "Artwork successfully synthesized!", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("imagen_generate_button"),
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
                                    text = if (isGenerating) "Synthesizing Artwork..." else "Generate Imagen Art",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Last Generated Preview
                    val isGeneratingImage = isGenerating && (generationStep.contains("Image") || generationStep.contains("image") || generationStep.contains("artwork") || generationStep.contains("Artwork") || generationStep.contains("canvas") || generationStep.contains("Canvas") || generationStep.contains("Imagen") || generationStep.contains("imagen") || generationStep.contains("ஆய்வு") || generationStep.contains("உருவாக்குகிறார்") || generationStep.contains("படம்"))
                    AnimatedVisibility(
                        visible = lastGeneratedCreation != null || isGeneratingImage,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        if (isGeneratingImage) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = "Synthesizing Image Assets...",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(200.dp)
                                        .clip(RoundedCornerShape(16.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    VideoPanelSkeleton(modifier = Modifier.fillMaxSize())
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center,
                                        modifier = Modifier
                                            .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                                            .padding(16.dp)
                                    ) {
                                        CircularProgressIndicator(
                                            color = NovaPrimary,
                                            strokeWidth = 3.dp,
                                            modifier = Modifier.size(36.dp)
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = generationStep.ifBlank { "Generating..." },
                                            fontSize = 11.sp,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            textAlign = TextAlign.Center
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "${(generationProgress * 100).toInt()}% Completed",
                                            fontSize = 10.sp,
                                            color = Color.White.copy(alpha = 0.8f)
                                        )
                                    }
                                }
                            }
                        } else {
                            lastGeneratedCreation?.let { creation ->
                            val bitmap = remember(creation) {
                                val base64Str = creation.responseText
                                if (!base64Str.isNullOrBlank()) {
                                    try {
                                        val decoded = Base64.decode(base64Str, Base64.DEFAULT)
                                        BitmapFactory.decodeByteArray(decoded, 0, decoded.size)
                                    } catch (e: Exception) {
                                        null
                                    }
                                } else null
                            }

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = "Generated Preview",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(200.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(16.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (bitmap != null) {
                                        Image(
                                            bitmap = bitmap.asImageBitmap(),
                                            contentDescription = "Generated image preview",
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            viewModel.addImageToWorkspace(creation)
                                            Toast.makeText(context, "Added to Creative Workspace!", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(44.dp)
                                            .testTag("add_to_workspace_button"),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(16.dp))
                                            Text("Add to Desk", color = MaterialTheme.colorScheme.onPrimaryContainer, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        }
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            triggerExport(creation)
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(44.dp)
                                            .testTag("export_image_button"),
                                        shape = RoundedCornerShape(12.dp),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(Icons.Default.Download, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                            Text("Export", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            }
            }

            // SECTION 1.5: Google Veo AI Video Generator Component (Tab 0)
            AnimatedVisibility(
                visible = (activeTab == 0),
                enter = fadeIn(animationSpec = tween(300)) + expandVertically(),
                exit = fadeOut(animationSpec = tween(150)) + shrinkVertically(),
                modifier = Modifier.fillMaxWidth()
            ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("veo_generator_component"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Movie,
                            contentDescription = "Veo Video",
                            tint = NovaSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Google Veo AI Studio",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }

                    // Prompt Input Field for Veo
                    OutlinedTextField(
                        value = veoPrompt,
                        onValueChange = { veoPrompt = it },
                        placeholder = {
                            Text(
                                text = "Describe what you want Veo to animate in cinematic detail...",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                        },
                        trailingIcon = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(end = 4.dp)
                            ) {
                                IconButton(
                                    onClick = {
                                        activeFileTarget = "veo"
                                        filePickerLauncher.launch("*/*")
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Upload files image video",
                                        tint = NovaSecondary
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        activeSpeechTarget = "veo"
                                        try {
                                            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                                putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak now...")
                                            }
                                            speechRecognizerLauncher.launch(intent)
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "Speech recognition not supported on this device", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Mic,
                                        contentDescription = "Speech recognition",
                                        tint = NovaSecondary
                                    )
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("veo_prompt_field"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NovaSecondary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                            focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.3f)
                        ),
                        shape = RoundedCornerShape(16.dp)
                    )

                    selectedMediaUriVeo?.let { uri ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.AttachFile, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                            Text(
                                text = "Attached: $selectedMediaNameVeo",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            IconButton(
                                onClick = {
                                    selectedMediaUriVeo = null
                                    selectedMediaNameVeo = null
                                },
                                modifier = Modifier.size(16.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Remove media", modifier = Modifier.size(12.dp))
                            }
                        }
                    }

                    // Quick Style Selector for Veo
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Visual Style Theme",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            items(videoStyles) { style ->
                                val isSelected = selectedVeoStyle == style
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isSelected) NovaSecondary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface)
                                        .border(
                                            1.dp,
                                            if (isSelected) NovaSecondary else MaterialTheme.colorScheme.outline.copy(alpha = 0.1f),
                                            RoundedCornerShape(10.dp)
                                        )
                                        .clickable { selectedVeoStyle = style }
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = style,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) NovaSecondary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    // Camera Angle Selector for Veo
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Camera Choreography",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            items(cameraAngles) { angle ->
                                val isSelected = selectedVeoCameraAngle == angle
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isSelected) NovaSecondary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface)
                                        .border(
                                            1.dp,
                                            if (isSelected) NovaSecondary else MaterialTheme.colorScheme.outline.copy(alpha = 0.1f),
                                            RoundedCornerShape(10.dp)
                                        )
                                        .clickable { selectedVeoCameraAngle = angle }
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = angle,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) NovaSecondary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    // Aspect Ratio, Fps, Duration and Resolution Configuration
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Aspect Ratio Box Selector
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text("Aspect Ratio", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                listOf("16:9", "9:16", "1:1").forEach { ratio ->
                                    val isSelected = selectedVeoRatio == ratio
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSelected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface)
                                            .border(1.dp, if (isSelected) NovaSecondary else MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                            .clickable { selectedVeoRatio = ratio }
                                            .padding(vertical = 6.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(ratio, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }

                        // Duration Box Selector
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text("Duration", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                listOf(5, 10).forEach { sec ->
                                    val isSelected = selectedVeoDuration == sec
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSelected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface)
                                            .border(1.dp, if (isSelected) NovaSecondary else MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                            .clickable { selectedVeoDuration = sec }
                                            .padding(vertical = 6.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("${sec}s", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }

                    // Generate Button for Veo
                    Button(
                        onClick = {
                            if (veoPrompt.isBlank()) {
                                Toast.makeText(context, "Please write a cinematic video prompt first!", Toast.LENGTH_SHORT).show()
                            } else {
                                viewModel.triggerGeneration(
                                    prompt = veoPrompt,
                                    style = selectedVeoStyle,
                                    cameraAngle = selectedVeoCameraAngle,
                                    resolution = selectedVeoResolution,
                                    fps = selectedVeoFps,
                                    duration = selectedVeoDuration,
                                    imageSelected = false,
                                    aspectRatio = selectedVeoRatio
                                ) { creation ->
                                    lastGeneratedVideoCreation = creation
                                    veoPlayerProgress = 0.0f
                                    isVeoPlaying = true
                                    Toast.makeText(context, "Veo video successfully synthesized!", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
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
                                Icon(Icons.Default.Videocam, contentDescription = null, tint = Color.White)
                                Text(
                                    text = if (isGenerating) "Synthesizing Video..." else "Generate Veo Video",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Last Generated Video Preview
                    val isGeneratingVideo = isGenerating && (generationStep.contains("Video") || generationStep.contains("video") || generationStep.contains("Veo") || generationStep.contains("veo") || generationStep.contains("வீடியோ") || generationStep.contains("Script") || generationStep.contains("script"))
                    AnimatedVisibility(
                        visible = lastGeneratedVideoCreation != null || isGeneratingVideo,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        if (isGeneratingVideo) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = "Synthesizing Video Assets...",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(200.dp)
                                        .clip(RoundedCornerShape(16.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    VideoPanelSkeleton(modifier = Modifier.fillMaxSize())
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center,
                                        modifier = Modifier
                                            .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                                            .padding(16.dp)
                                    ) {
                                        CircularProgressIndicator(
                                            color = NovaSecondary,
                                            strokeWidth = 3.dp,
                                            modifier = Modifier.size(36.dp)
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = generationStep.ifBlank { "Rendering Video..." },
                                            fontSize = 11.sp,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            textAlign = TextAlign.Center
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "${(generationProgress * 100).toInt()}% Completed",
                                            fontSize = 10.sp,
                                            color = Color.White.copy(alpha = 0.8f)
                                        )
                                    }
                                }
                            }
                        } else {
                            lastGeneratedVideoCreation?.let { creation ->
                            val decodedBitmap = remember(creation) {
                                val rawBase64 = creation.responseText
                                if (!rawBase64.isNullOrBlank()) {
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

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Generated Cinematic Preview",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    // Play/Pause indicator
                                    IconButton(
                                        onClick = { isVeoPlaying = !isVeoPlaying },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isVeoPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                            contentDescription = "Toggle playback",
                                            tint = NovaSecondary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(200.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(Color.Black)
                                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(16.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (decodedBitmap != null) {
                                        Image(
                                            bitmap = decodedBitmap.asImageBitmap(),
                                            contentDescription = "Generated video keyframe",
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    }

                                    // Procedural Canvas motion waves overlay
                                    Canvas(
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        val w = size.width
                                        val h = size.height
                                        val phase = veoPlayerProgress * 2 * Math.PI.toFloat()
                                        val styleColor = when (selectedVeoStyle) {
                                            "Cyberpunk" -> Color(0xFFFF00CC)
                                            "Surreal Dreamscape" -> Color(0xFF00FFFF)
                                            "Photorealistic" -> Color(0xFFFFCC00)
                                            else -> NovaSecondary
                                        }

                                        // Draw scanning laser line
                                        val scanLineY = veoPlayerProgress * h
                                        drawLine(
                                            color = Color(0xFF00FFCC).copy(alpha = 0.4f),
                                            start = Offset(0f, scanLineY),
                                            end = Offset(w, scanLineY),
                                            strokeWidth = 2.dp.toPx()
                                        )

                                        // Floating cinematic dust particles
                                        for (i in 0 until 12) {
                                            val px = (sin(i.toDouble() + veoPlayerProgress) * 0.5 + 0.5) * w
                                            val py = ((veoPlayerProgress + i * 0.08f) % 1.0f) * h
                                            drawCircle(
                                                color = styleColor.copy(alpha = 0.2f),
                                                radius = 12.dp.toPx() + sin(phase + i) * 3.dp.toPx(),
                                                center = Offset(px.toFloat(), py.toFloat())
                                            )
                                        }

                                        // Beautiful sine waves showing AI animation rendering
                                        val waveSteps = 40
                                        val waveAmplitude = 20f
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
                                                strokeWidth = 2.dp.toPx()
                                            )
                                        }
                                    }

                                    // Overlay progress bar at the bottom
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .align(Alignment.BottomCenter)
                                            .height(4.dp)
                                            .background(Color.White.copy(alpha = 0.2f))
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxHeight()
                                                .fillMaxWidth(veoPlayerProgress)
                                                .background(NovaSecondary)
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            viewModel.addImageToWorkspace(creation)
                                            Toast.makeText(context, "Video added to Creative Workspace!", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(44.dp)
                                            .testTag("add_video_to_workspace_button"),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(16.dp))
                                            Text("Add to Desk", color = MaterialTheme.colorScheme.onSecondaryContainer, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        }
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            triggerExport(creation)
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(44.dp)
                                            .testTag("export_video_button"),
                                        shape = RoundedCornerShape(12.dp),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(Icons.Default.Download, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(16.dp))
                                            Text("Export", color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            }
        }

            // SECTION 1.7: ElevenLabs AI Voiceover Generator Component (Tab 2)
            AnimatedVisibility(
                visible = (activeTab == 2),
                enter = fadeIn(animationSpec = tween(300)) + expandVertically(),
                exit = fadeOut(animationSpec = tween(150)) + shrinkVertically(),
                modifier = Modifier.fillMaxWidth()
            ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("voiceover_generator_component"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = "Voiceover Studio",
                                tint = NovaTertiary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "ElevenLabs Voiceover Studio",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }

                        // Launch full voice screen button!
                        TextButton(
                            onClick = { onNavigateToVoiceOver() },
                            modifier = Modifier.testTag("nav_to_full_voice_studio")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(14.dp), tint = NovaTertiary)
                                Text("Full Voice Space", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = NovaTertiary)
                            }
                        }
                    }

                    // Prompt Input Field for Voiceover
                    OutlinedTextField(
                        value = voiceoverText,
                        onValueChange = { voiceoverText = it },
                        placeholder = {
                            Text(
                                text = "Enter text or script for ElevenLabs to voice...",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                        },
                        trailingIcon = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(end = 4.dp)
                            ) {
                                IconButton(
                                    onClick = {
                                        activeFileTarget = "voiceover"
                                        filePickerLauncher.launch("*/*")
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Upload files image video",
                                        tint = NovaTertiary
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        activeSpeechTarget = "voiceover"
                                        try {
                                            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                                putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak now...")
                                            }
                                            speechRecognizerLauncher.launch(intent)
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "Speech recognition not supported on this device", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Mic,
                                        contentDescription = "Speech recognition",
                                        tint = NovaTertiary
                                    )
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("voiceover_prompt_field"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NovaTertiary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                            focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.3f)
                        ),
                        shape = RoundedCornerShape(16.dp)
                    )

                    selectedMediaUriVoiceover?.let { uri ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.AttachFile, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                            Text(
                                text = "Attached: $selectedMediaNameVoiceover",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            IconButton(
                                onClick = {
                                    selectedMediaUriVoiceover = null
                                    selectedMediaNameVoiceover = null
                                },
                                modifier = Modifier.size(16.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Remove media", modifier = Modifier.size(12.dp))
                            }
                        }
                    }

                    // Quick Voice Selector
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Select Voice Profile",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            items(creativeVoiceoverProfiles) { profile ->
                                val isSelected = selectedVoiceoverProfile == profile
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isSelected) NovaTertiary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface)
                                        .border(
                                            1.dp,
                                            if (isSelected) NovaTertiary else MaterialTheme.colorScheme.outline.copy(alpha = 0.1f),
                                            RoundedCornerShape(10.dp)
                                        )
                                        .clickable { selectedVoiceoverProfile = profile }
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = profile.first,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) NovaTertiary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    // Voice settings parameter sliders (Stability & Similarity Boost)
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // 1. Stability Slider
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Stability (Vocal Tone consistency)",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = String.format("%.2f", voiceoverStability),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NovaTertiary
                                )
                            }
                            Slider(
                                value = voiceoverStability,
                                onValueChange = { voiceoverStability = it },
                                valueRange = 0.0f..1.0f,
                                modifier = Modifier.testTag("voiceover_stability_slider"),
                                colors = SliderDefaults.colors(
                                    thumbColor = NovaTertiary,
                                    activeTrackColor = NovaTertiary,
                                    inactiveTrackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f)
                                )
                            )
                        }

                        // 2. Similarity Boost Slider
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Clarity / Similarity Boost",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = String.format("%.2f", voiceoverSimilarity),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NovaTertiary
                                )
                            }
                            Slider(
                                value = voiceoverSimilarity,
                                onValueChange = { voiceoverSimilarity = it },
                                valueRange = 0.0f..1.0f,
                                modifier = Modifier.testTag("voiceover_similarity_slider"),
                                colors = SliderDefaults.colors(
                                    thumbColor = NovaTertiary,
                                    activeTrackColor = NovaTertiary,
                                    inactiveTrackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f)
                                )
                            )
                        }
                    }

                    // Generate Button for Voiceover
                    Button(
                        onClick = {
                            if (voiceoverText.isBlank()) {
                                Toast.makeText(context, "Please write a script first!", Toast.LENGTH_SHORT).show()
                            } else {
                                isVoiceoverPlaying = false
                                try {
                                    localVoiceoverPlayer.reset()
                                } catch (e: Exception) {}
                                
                                viewModel.triggerVoiceOver(
                                    text = voiceoverText,
                                    voiceGender = if (selectedVoiceoverProfile.first.contains("Kamala") || selectedVoiceoverProfile.first.contains("Serena") || selectedVoiceoverProfile.first.contains("Sophia")) "FEMALE" else "MALE",
                                    accent = selectedVoiceoverProfile.first,
                                    voiceId = selectedVoiceoverProfile.second,
                                    stability = voiceoverStability.toDouble(),
                                    similarityBoost = voiceoverSimilarity.toDouble()
                                ) { creation ->
                                    lastGeneratedVoiceoverCreation = creation
                                    voiceoverProgress = 0.0f
                                    Toast.makeText(context, "Voiceover successfully synthesized!", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("voiceover_generate_button"),
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
                                Icon(Icons.Default.VolumeUp, contentDescription = null, tint = Color.White)
                                Text(
                                    text = if (isGenerating) "Synthesizing Audio..." else "Generate ElevenLabs Voiceover",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Last Generated Voiceover Preview
                    val isGeneratingAudio = isGenerating && (generationStep.contains("Voice-Over") || generationStep.contains("ElevenLabs") || generationStep.contains("குரல்") || generationStep.contains("ஒலி") || generationStep.contains("audio") || generationStep.contains("Audio") || generationStep.contains("voiceover") || generationStep.contains("Voiceover"))
                    AnimatedVisibility(
                        visible = lastGeneratedVoiceoverCreation != null || isGeneratingAudio,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        if (isGeneratingAudio) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = "Synthesizing Audio Assets...",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Box(
                                    modifier = Modifier.fillMaxWidth(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    com.example.ui.components.AudioPlayerSkeleton()
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center,
                                        modifier = Modifier
                                            .background(Color.Black.copy(alpha = 0.65f), RoundedCornerShape(16.dp))
                                            .padding(16.dp)
                                    ) {
                                        CircularProgressIndicator(
                                            color = NovaTertiary,
                                            strokeWidth = 3.dp,
                                            modifier = Modifier.size(36.dp)
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = generationStep.ifBlank { "Synthesizing Premium ElevenLabs Voice..." },
                                            fontSize = 11.sp,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            textAlign = TextAlign.Center
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "${(generationProgress * 100).toInt()}% Completed",
                                            fontSize = 10.sp,
                                            color = Color.White.copy(alpha = 0.8f)
                                        )
                                    }
                                }
                            }
                        } else {
                            lastGeneratedVoiceoverCreation?.let { creation ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Generated Voiceover Preview",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    // Play/Pause button
                                    IconButton(
                                        onClick = {
                                            val rawPath = creation.responseText
                                            if (rawPath.isNotEmpty()) {
                                                try {
                                                    if (isVoiceoverPlaying) {
                                                        localVoiceoverPlayer.pause()
                                                        isVoiceoverPlaying = false
                                                    } else {
                                                        localVoiceoverPlayer.reset()
                                                        localVoiceoverPlayer.setDataSource(rawPath)
                                                        localVoiceoverPlayer.prepare()
                                                        localVoiceoverPlayer.start()
                                                        isVoiceoverPlaying = true
                                                        localVoiceoverPlayer.setOnCompletionListener {
                                                            isVoiceoverPlaying = false
                                                            voiceoverProgress = 1.0f
                                                        }
                                                    }
                                                } catch (e: Exception) {
                                                    Toast.makeText(context, "Error playing audio file", Toast.LENGTH_SHORT).show()
                                                }
                                            } else {
                                                Toast.makeText(context, "Audio file path is empty", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isVoiceoverPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                            contentDescription = "Toggle playback",
                                            tint = NovaTertiary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(80.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(Color.Black)
                                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(16.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    // Animated procedural audio spectrum waves
                                    Canvas(
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        val w = size.width
                                        val h = size.height
                                        val phase = voiceoverProgress * 2 * Math.PI.toFloat()
                                        
                                        // Draw beautiful sine wave matching play progress
                                        val waveSteps = 60
                                        val waveAmplitude = if (isVoiceoverPlaying) 25f else 5f
                                        val points = mutableListOf<Offset>()
                                        for (x in 0..waveSteps) {
                                            val t = x.toFloat() / waveSteps
                                            val xPos = t * w
                                            val waveFactor = sin(t * 6 * Math.PI.toFloat() + phase)
                                            val yPos = (h / 2f) + waveFactor * waveAmplitude
                                            points.add(Offset(xPos, yPos))
                                        }
                                        for (p in 0 until points.size - 1) {
                                            drawLine(
                                                color = NovaTertiary.copy(alpha = if (isVoiceoverPlaying) 0.8f else 0.4f),
                                                start = points[p],
                                                end = points[p + 1],
                                                strokeWidth = 3.dp.toPx()
                                            )
                                        }

                                        // Draw reflection wave
                                        val pointsRefl = mutableListOf<Offset>()
                                        for (x in 0..waveSteps) {
                                            val t = x.toFloat() / waveSteps
                                            val xPos = t * w
                                            val waveFactor = -sin(t * 4 * Math.PI.toFloat() + phase * 1.5f)
                                            val yPos = (h / 2f) + waveFactor * (waveAmplitude * 0.7f)
                                            pointsRefl.add(Offset(xPos, yPos))
                                        }
                                        for (p in 0 until pointsRefl.size - 1) {
                                            drawLine(
                                                color = NovaPrimary.copy(alpha = if (isVoiceoverPlaying) 0.5f else 0.2f),
                                                start = pointsRefl[p],
                                                end = pointsRefl[p + 1],
                                                strokeWidth = 1.5.dp.toPx()
                                            )
                                        }
                                    }

                                    // Overlay progress bar at the bottom
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .align(Alignment.BottomCenter)
                                            .height(4.dp)
                                            .background(Color.White.copy(alpha = 0.2f))
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxHeight()
                                                .fillMaxWidth(voiceoverProgress)
                                                .background(NovaTertiary)
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            viewModel.addImageToWorkspace(creation)
                                            Toast.makeText(context, "Voiceover added to Creative Workspace!", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(44.dp)
                                            .testTag("add_voiceover_to_workspace_button"),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(16.dp))
                                            Text("Add to Desk", color = MaterialTheme.colorScheme.onSecondaryContainer, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        }
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            triggerExport(creation)
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(44.dp)
                                            .testTag("export_voiceover_button"),
                                        shape = RoundedCornerShape(12.dp),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(Icons.Default.Download, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(16.dp))
                                            Text("Export", color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            }
        }

            // SECTION 1.8: Google Gemini Prompt & Script Template Studio (Tab 3)
            AnimatedVisibility(
                visible = (activeTab == 3),
                enter = fadeIn(animationSpec = tween(300)) + expandVertically(),
                exit = fadeOut(animationSpec = tween(150)) + shrinkVertically(),
                modifier = Modifier.fillMaxWidth()
            ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("template_generator_component"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = "Template Studio",
                            tint = NovaPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Google Gemini Template Studio",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Prompt Input Field for Template Topic
                    OutlinedTextField(
                        value = templateTopic,
                        onValueChange = { templateTopic = it },
                        placeholder = {
                            Text(
                                text = "Enter the topic or prompt for your creative template...",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                        },
                        trailingIcon = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(end = 4.dp)
                            ) {
                                IconButton(
                                    onClick = {
                                        activeFileTarget = "template"
                                        filePickerLauncher.launch("*/*")
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Upload files image video",
                                        tint = NovaPrimary
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        activeSpeechTarget = "template"
                                        try {
                                            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                                putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak now...")
                                            }
                                            speechRecognizerLauncher.launch(intent)
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "Speech recognition not supported on this device", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Mic,
                                        contentDescription = "Speech recognition",
                                        tint = NovaPrimary
                                    )
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("template_prompt_field"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NovaPrimary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                            focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.3f)
                        ),
                        shape = RoundedCornerShape(16.dp)
                    )

                    selectedMediaUriTemplate?.let { uri ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.AttachFile, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                            Text(
                                text = "Attached: $selectedMediaNameTemplate",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            IconButton(
                                onClick = {
                                    selectedMediaUriTemplate = null
                                    selectedMediaNameTemplate = null
                                },
                                modifier = Modifier.size(16.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Remove media", modifier = Modifier.size(12.dp))
                            }
                        }
                    }

                    // Template Type Selector
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Select Template Format",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            items(templateTypes) { templateType ->
                                val isSelected = selectedTemplateType == templateType
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isSelected) NovaPrimary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface)
                                        .border(
                                            1.dp,
                                            if (isSelected) NovaPrimary else MaterialTheme.colorScheme.outline.copy(alpha = 0.1f),
                                            RoundedCornerShape(10.dp)
                                        )
                                        .clickable { selectedTemplateType = templateType }
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = templateType,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) NovaPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    // Style Selector
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Tone & Style",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            val tones = listOf("Professional", "Engaging", "Informative", "Creative", "Poetic")
                            LazyRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                items(tones) { tone ->
                                    val isSelected = selectedTemplateTone == tone
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSelected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface)
                                            .border(1.dp, if (isSelected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outline.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                            .clickable { selectedTemplateTone = tone }
                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Text(tone, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                    }
                                }
                            }
                        }
                    }

                    // Generate Template Button
                    Button(
                        onClick = {
                            if (templateTopic.isBlank()) {
                                Toast.makeText(context, "Please enter a topic first!", Toast.LENGTH_SHORT).show()
                            } else {
                                isGeneratingTemplate = true
                                val templatePrompt = "Generate a highly structured, professional $selectedTemplateType template about '$templateTopic' with a $selectedTemplateTone tone. Include beautiful Markdown styling, distinct sections, clear headers, detailed advice, helpful guidelines, and hashtags where relevant."
                                viewModel.triggerWritingAssistantWithSettings(
                                    prompt = templatePrompt,
                                    assistantType = "Template Creator"
                                ) { content ->
                                    generatedTemplateContent = content
                                    isGeneratingTemplate = false
                                    Toast.makeText(context, "Template successfully generated!", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("generate_template_button"),
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
                                    text = if (isGeneratingTemplate) "Synthesizing Template..." else "Generate Professional Template",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Generated Template Preview
                    AnimatedVisibility(
                        visible = generatedTemplateContent.isNotEmpty(),
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Generated Template Preview",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            // Display template text
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 200.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(MaterialTheme.colorScheme.surface)
                                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
                                    .verticalScroll(rememberScrollState())
                                    .padding(16.dp)
                            ) {
                                Text(
                                    text = generatedTemplateContent,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    lineHeight = 18.sp
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Copy Button
                                OutlinedButton(
                                    onClick = {
                                        val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                        val clip = android.content.ClipData.newPlainText("Gemini Template", generatedTemplateContent)
                                        clipboard.setPrimaryClip(clip)
                                        Toast.makeText(context, "Template copied to clipboard!", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.weight(1f).height(40.dp),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Copy Template", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }

                                // Add to Desk
                                Button(
                                    onClick = {
                                        val creation = VisionAiCreation(
                                            title = "$selectedTemplateType: $templateTopic",
                                            prompt = templateTopic,
                                            style = selectedTemplateTone,
                                            cameraAngle = "None",
                                            resolution = "Text Document",
                                            fps = 0,
                                            duration = 0,
                                            type = "WRITING",
                                            visualUrl = "",
                                            responseText = generatedTemplateContent,
                                            generatedDescription = "A professional, structured $selectedTemplateType template generated by Google Gemini."
                                        )
                                        viewModel.addImageToWorkspace(creation)
                                        Toast.makeText(context, "Template added to Desk!", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.weight(1f).height(40.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Add to Desk", color = MaterialTheme.colorScheme.onSecondaryContainer, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }

                                // Export Button
                                OutlinedButton(
                                    onClick = {
                                        val creation = VisionAiCreation(
                                            title = "$selectedTemplateType: $templateTopic",
                                            prompt = templateTopic,
                                            style = selectedTemplateTone,
                                            cameraAngle = "None",
                                            resolution = "Text Document",
                                            fps = 0,
                                            duration = 0,
                                            type = "WRITING",
                                            visualUrl = "",
                                            responseText = generatedTemplateContent,
                                            generatedDescription = "A professional, structured $selectedTemplateType template generated by Google Gemini."
                                        )
                                        triggerExport(creation)
                                    },
                                    modifier = Modifier.weight(1f).height(40.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                                ) {
                                    Icon(Icons.Default.Download, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Export", color = MaterialTheme.colorScheme.primary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
            }

            // SECTION 1.9: Suno & Udio AI Song Studio (Tab 4)
            AnimatedVisibility(
                visible = (activeTab == 4),
                enter = fadeIn(animationSpec = tween(300)) + expandVertically(),
                exit = fadeOut(animationSpec = tween(150)) + shrinkVertically(),
                modifier = Modifier.fillMaxWidth()
            ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("song_generator_component"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = "Song Studio",
                            tint = NovaSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Suno & Udio AI Song Studio",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }

                    // Prompt Input Field for Song
                    OutlinedTextField(
                        value = songPrompt,
                        onValueChange = { songPrompt = it },
                        placeholder = {
                            Text(
                                text = "Describe song lyrics, vibe, theme, or concept...",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                        },
                        trailingIcon = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(end = 4.dp)
                            ) {
                                IconButton(
                                    onClick = {
                                        activeFileTarget = "song"
                                        filePickerLauncher.launch("*/*")
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Upload files image video",
                                        tint = NovaSecondary
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        activeSpeechTarget = "song"
                                        try {
                                            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                                putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak now...")
                                            }
                                            speechRecognizerLauncher.launch(intent)
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "Speech recognition not supported on this device", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Mic,
                                        contentDescription = "Speech recognition",
                                        tint = NovaSecondary
                                    )
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("song_prompt_field"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NovaSecondary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                            focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.3f)
                        ),
                        shape = RoundedCornerShape(16.dp)
                    )

                    selectedMediaUriSong?.let { uri ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.AttachFile, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                            Text(
                                text = "Attached: $selectedMediaNameSong",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            IconButton(
                                onClick = {
                                    selectedMediaUriSong = null
                                    selectedMediaNameSong = null
                                },
                                modifier = Modifier.size(16.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Remove media", modifier = Modifier.size(12.dp))
                            }
                        }
                    }

                    // Song Style/Genre Selector
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Select Musical Genre",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            items(songGenres) { genre ->
                                val isSelected = selectedSongGenre == genre
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isSelected) NovaSecondary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface)
                                        .border(
                                            1.dp,
                                            if (isSelected) NovaSecondary else MaterialTheme.colorScheme.outline.copy(alpha = 0.1f),
                                            RoundedCornerShape(10.dp)
                                        )
                                        .clickable { selectedSongGenre = genre }
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = genre,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) NovaSecondary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    // Song Mood Selector
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Select Vocal Mood",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        val moods = listOf("Upbeat & Energetic", "Melancholy & Deep", "Dreamy & Ambient", "Epic & Grand")
                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(moods) { mood ->
                                val isSelected = selectedSongMood == mood
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isSelected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface)
                                        .border(
                                            1.dp,
                                            if (isSelected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outline.copy(alpha = 0.1f),
                                            RoundedCornerShape(10.dp)
                                        )
                                        .clickable { selectedSongMood = mood }
                                        .padding(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    Text(mood, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                }
                            }
                        }
                    }

                    // Generate Song Button
                    Button(
                        onClick = {
                            if (songPrompt.isBlank()) {
                                Toast.makeText(context, "Please describe your song concept first!", Toast.LENGTH_SHORT).show()
                            } else {
                                isGeneratingSong = true
                                val composerPrompt = "Compose professional, beautiful song lyrics, complete musical structure (Intro, Verse, Chorus, Bridge, Outro), tempo, mood, production keys, and liner notes for a $selectedSongGenre song about '$songPrompt' with an overall $selectedSongMood tone. Return beautifully formatted lyrics ready for production."
                                viewModel.triggerWritingAssistantWithSettings(
                                    prompt = composerPrompt,
                                    assistantType = "AI Song Composer & Lyricist"
                                ) { content ->
                                    generatedSongLyrics = content
                                    generatedSongTitle = if (songPrompt.length > 25) songPrompt.substring(0, 22) + "..." else songPrompt
                                    isGeneratingSong = false
                                    isSongPlaying = false
                                    songProgress = 0f
                                    
                                    val songCreation = VisionAiCreation(
                                        title = "Song: $generatedSongTitle [$selectedSongGenre]",
                                        prompt = songPrompt,
                                        style = selectedSongGenre,
                                        cameraAngle = selectedSongMood,
                                        resolution = "Audio Waveform",
                                        fps = 0,
                                        duration = 180,
                                        type = "VOICEOVER", // Re-use VOICEOVER so it gets visual spectrum player!
                                        visualUrl = "",
                                        responseText = generatedSongLyrics,
                                        generatedDescription = "A complete AI-composed musical theme: $selectedSongGenre ($selectedSongMood mood)."
                                    )
                                    lastGeneratedSongCreation = songCreation
                                    Toast.makeText(context, "AI Song composed and synthesizer synthesized successfully!", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("generate_song_button"),
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
                                Icon(Icons.Default.VolumeUp, contentDescription = null, tint = Color.White)
                                Text(
                                    text = if (isGeneratingSong) "Composing AI Harmonies..." else "Compose & Generate AI Song",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Playback states for composing song
                    if (lastGeneratedSongCreation != null) {
                        // Visual bouncing equalizer and control row
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "Composed Lyric Track: $generatedSongTitle",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Style: $selectedSongGenre  •  Mood: $selectedSongMood",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                    )
                                }

                                // Play / Pause
                                IconButton(
                                    onClick = { isSongPlaying = !isSongPlaying },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isSongPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = "Toggle playback",
                                        tint = NovaSecondary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            // Dynamic equalizer Canvas
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(60.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color.Black)
                                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(16.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Canvas(
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    val barCount = 15
                                    val barWidth = size.width / (barCount * 2)
                                    val h = size.height
                                    for (i in 0 until barCount) {
                                        val x = i * barWidth * 2 + barWidth / 2
                                        val progressFactor = sin(songProgress * 2 * Math.PI.toFloat() * 4 + i)
                                        val heightFactor = if (isSongPlaying) {
                                            (0.3f + 0.6f * Math.abs(progressFactor))
                                        } else {
                                            0.1f
                                        }
                                        val barHeight = h * heightFactor
                                        drawRect(
                                            color = NovaSecondary,
                                            topLeft = Offset(x, h - barHeight),
                                            size = androidx.compose.ui.geometry.Size(barWidth, barHeight)
                                        )
                                    }
                                }

                                // Song progress bar overlay
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .align(Alignment.BottomCenter)
                                        .height(4.dp)
                                        .background(Color.White.copy(alpha = 0.2f))
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .fillMaxWidth(songProgress)
                                            .background(NovaSecondary)
                                    )
                                }
                            }

                            // Lyrics text expand/collapse container
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 140.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surface)
                                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                                    .verticalScroll(rememberScrollState())
                                    .padding(12.dp)
                            ) {
                                Text(
                                    text = generatedSongLyrics,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    lineHeight = 16.sp
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        lastGeneratedSongCreation?.let { creation ->
                                            viewModel.addImageToWorkspace(creation)
                                            Toast.makeText(context, "Song added to Desk Workspace!", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp)
                                        .testTag("add_song_to_workspace_button"),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(16.dp))
                                        Text("Add to Desk", color = MaterialTheme.colorScheme.onSecondaryContainer, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                }

                                OutlinedButton(
                                    onClick = {
                                        lastGeneratedSongCreation?.let { creation ->
                                            triggerExport(creation)
                                        }
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp)
                                        .testTag("export_song_button"),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(Icons.Default.Download, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(16.dp))
                                        Text("Export", color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // SECTION 1.10: Multimodal AI Analyst & STT (Tab 5)
            AnimatedVisibility(
                visible = (activeTab == 5),
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                var selectedAnalysisMode by remember { mutableStateOf(0) } // 0 = Image, 1 = Video, 2 = Audio STT
                var analysisPromptImage by remember { mutableStateOf("Describe this image in detail, highlighting artistic composition, subjects, and color palette.") }
                var analysisPromptVideo by remember { mutableStateOf("Explain the action, pacing, and visual style of this video sequence.") }
                
                var imageAnalysisResult by remember { mutableStateOf("") }
                var videoAnalysisResult by remember { mutableStateOf("") }
                var audioSTTResult by remember { mutableStateOf("") }
                
                var isAudioRecording by remember { mutableStateOf(false) }
                var audioRecordTimer by remember { mutableStateOf(0) }
                
                LaunchedEffect(isAudioRecording) {
                    if (isAudioRecording) {
                        audioRecordTimer = 0
                        while (isAudioRecording) {
                            delay(1000)
                            audioRecordTimer++
                        }
                    }
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("analysis_workspace_card"),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        // Title Panel
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(NovaGradient, CircleShape)
                                    .padding(2.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(MaterialTheme.colorScheme.background, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Explore, contentDescription = null, tint = NovaPrimary, modifier = Modifier.size(22.dp))
                                }
                            }
                            Column {
                                Text("AI Multimodal Analyst", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface)
                                Text("Analyze visual media and transcribe speech with Gemini Pro", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                            }
                        }

                        // Horizontal Mode Selector Pills
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            listOf(
                                Triple(0, "Image", Icons.Default.Image),
                                Triple(1, "Video", Icons.Default.Videocam),
                                Triple(2, "Audio STT", Icons.Default.Mic)
                            ).forEach { (modeIdx, label, icon) ->
                                val isSelected = selectedAnalysisMode == modeIdx
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                                        .clickable { selectedAnalysisMode = modeIdx }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = null,
                                            tint = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            text = label,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }

                        // ANALYSIS MODE 0: Image Analysis
                        if (selectedAnalysisMode == 0) {
                            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                // Image Picker/Placeholder area
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(180.dp)
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                                        .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)), RoundedCornerShape(16.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (selectedMediaUriAnalysis != null) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(Icons.Default.Image, contentDescription = null, tint = NovaPrimary, modifier = Modifier.size(40.dp))
                                            Text(selectedMediaNameAnalysis ?: "Selected Image", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                            OutlinedButton(
                                                onClick = { selectedMediaUriAnalysis = null; selectedMediaNameAnalysis = null },
                                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                                shape = RoundedCornerShape(10.dp)
                                            ) {
                                                Text("Clear Image", fontSize = 11.sp)
                                            }
                                        }
                                    } else {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            Icon(Icons.Default.CloudUpload, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f), modifier = Modifier.size(36.dp))
                                            Text("No Image Uploaded Yet", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                                            Button(
                                                onClick = { activeFileTarget = "analysis"; filePickerLauncher.launch("image/*") },
                                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                                shape = RoundedCornerShape(10.dp)
                                            ) {
                                                Text("Choose Image Reference", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }

                                // Prompt Box
                                OutlinedTextField(
                                    value = analysisPromptImage,
                                    onValueChange = { analysisPromptImage = it },
                                    label = { Text("Analysis Focus / Prompt", fontSize = 12.sp) },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp)
                                )

                                // Action suggestion tags
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    listOf("Composition Breakdown", "Color & Lighting", "Mood Analysis").forEach { text ->
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                                .clickable { analysisPromptImage = "$text: $analysisPromptImage" }
                                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                        ) {
                                            Text(text, fontSize = 10.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.primary)
                                        }
                                    }
                                }

                                // Analyze Button
                                Button(
                                    onClick = {
                                        imageAnalysisResult = ""
                                        // Trigger viewModel image analysis (simulates/calls real backend)
                                        viewModel.triggerImageAnalysis(
                                            prompt = analysisPromptImage,
                                            imageBase64 = "image_reference_data_placeholder"
                                        ) { response ->
                                            imageAnalysisResult = response
                                            Toast.makeText(context, "Analysis complete!", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth().height(48.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color.White)
                                        Text("Analyze Image with Gemini Pro", fontWeight = FontWeight.Bold)
                                    }
                                }

                                // Output Box
                                if (imageAnalysisResult.isNotBlank()) {
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)),
                                        shape = RoundedCornerShape(16.dp),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                                    ) {
                                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text("ANALYSIS REPORT", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = NovaPrimary)
                                                IconButton(
                                                    onClick = {
                                                        val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                                        val clip = android.content.ClipData.newPlainText("Image Analysis", imageAnalysisResult)
                                                        clipboard.setPrimaryClip(clip)
                                                        Toast.makeText(context, "Copied report to clipboard!", Toast.LENGTH_SHORT).show()
                                                    },
                                                    modifier = Modifier.size(24.dp)
                                                ) {
                                                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy Report", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
                                                }
                                            }
                                            Text(
                                                text = imageAnalysisResult,
                                                fontSize = 13.sp,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                lineHeight = 18.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // ANALYSIS MODE 1: Video Analysis
                        if (selectedAnalysisMode == 1) {
                            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(180.dp)
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                                        .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)), RoundedCornerShape(16.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Icon(Icons.Default.Videocam, contentDescription = null, tint = NovaSecondary, modifier = Modifier.size(40.dp))
                                        Text("Video Analysis Engine (Multimodal Timeline)", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                        Text("Uses Gemini Pro 1.5/3.1 high-density sampling keyframe analysis.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                                    }
                                }

                                OutlinedTextField(
                                    value = analysisPromptVideo,
                                    onValueChange = { analysisPromptVideo = it },
                                    label = { Text("Analysis Prompt (Pacing/Timeline Focus)", fontSize = 12.sp) },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp)
                                )

                                Button(
                                    onClick = {
                                        videoAnalysisResult = ""
                                        viewModel.triggerVideoAnalysis(
                                            prompt = analysisPromptVideo,
                                            videoPlaceholderBase64 = "video_reference_timeline_data"
                                        ) { response ->
                                            videoAnalysisResult = response
                                            Toast.makeText(context, "Timeline Breakdown Complete!", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth().height(48.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color.White)
                                        Text("Analyze Video Timeline with Gemini Pro", fontWeight = FontWeight.Bold)
                                    }
                                }

                                if (videoAnalysisResult.isNotBlank()) {
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)),
                                        shape = RoundedCornerShape(16.dp),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                                    ) {
                                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text("SHOT BREAKDOWN TIMELINE", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = NovaSecondary)
                                                IconButton(
                                                    onClick = {
                                                        val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                                        val clip = android.content.ClipData.newPlainText("Video Analysis", videoAnalysisResult)
                                                        clipboard.setPrimaryClip(clip)
                                                        Toast.makeText(context, "Timeline copied!", Toast.LENGTH_SHORT).show()
                                                    },
                                                    modifier = Modifier.size(24.dp)
                                                ) {
                                                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy Timeline", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
                                                }
                                            }
                                            Text(
                                                text = videoAnalysisResult,
                                                fontSize = 13.sp,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                lineHeight = 18.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // ANALYSIS MODE 2: Audio Speech-to-Text Transcription
                        if (selectedAnalysisMode == 2) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(20.dp)
                            ) {
                                Text(
                                    text = "High-Fidelity Audio Transcriber",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                
                                // Glowing mic container button
                                Box(
                                    modifier = Modifier
                                        .size(110.dp)
                                        .background(
                                            if (isAudioRecording) NovaGradient else Brush.verticalGradient(
                                                listOf(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.surfaceVariant)
                                            ),
                                            CircleShape
                                        )
                                        .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)), CircleShape)
                                        .clickable {
                                            isAudioRecording = !isAudioRecording
                                            if (!isAudioRecording) {
                                                // Trigger transcribe Speech-to-Text
                                                viewModel.triggerAudioTranscription { text ->
                                                    audioSTTResult = text
                                                    Toast.makeText(context, "Transcription synced!", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (isAudioRecording) Icons.Default.Stop else Icons.Default.Mic,
                                        contentDescription = "Trigger Recording",
                                        tint = if (isAudioRecording) Color.White else MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(44.dp)
                                    )
                                }

                                if (isAudioRecording) {
                                    Text(
                                        text = "Recording... ${audioRecordTimer}s",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                    // Simulated Recording Waveform
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.height(24.dp)
                                    ) {
                                        repeat(8) { idx ->
                                            val animatedHeight = remember { mutableStateOf(10.dp) }
                                            LaunchedEffect(isAudioRecording) {
                                                while (isAudioRecording) {
                                                    delay((100..300).random().toLong())
                                                    animatedHeight.value = (5..24).random().dp
                                                }
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .width(4.dp)
                                                    .height(animatedHeight.value)
                                                    .background(NovaPrimary, RoundedCornerShape(2.dp))
                                            )
                                        }
                                    }
                                } else {
                                    Text(
                                        text = "Tap circle to record & transcribe voice real-time",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                    )
                                }

                                if (audioSTTResult.isNotBlank()) {
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)),
                                        shape = RoundedCornerShape(16.dp),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                                    ) {
                                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text("TRANSCRIBED TEXT", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                    IconButton(
                                                        onClick = {
                                                            val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                                            val clip = android.content.ClipData.newPlainText("Transcribed Speech", audioSTTResult)
                                                            clipboard.setPrimaryClip(clip)
                                                            Toast.makeText(context, "Copied transcription!", Toast.LENGTH_SHORT).show()
                                                        },
                                                        modifier = Modifier.size(24.dp)
                                                    ) {
                                                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy Text", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
                                                    }
                                                }
                                            }
                                            Text(
                                                text = audioSTTResult,
                                                fontSize = 13.sp,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                lineHeight = 18.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // SECTION 2: Creative Workspace Gallery
            Column(
                modifier = Modifier.fillMaxWidth(),
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
                            imageVector = Icons.Default.FolderSpecial,
                            contentDescription = "Workspace Folder",
                            tint = NovaSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Active Workspace Gallery",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    if (workspaceImages.isNotEmpty()) {
                        TextButton(
                            onClick = {
                                viewModel.clearWorkspace()
                                Toast.makeText(context, "Workspace cleared!", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Text("Clear All", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                if (workspaceImages.isEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Landscape,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                                modifier = Modifier.size(48.dp)
                            )
                            Text(
                                text = "Your creative workspace is empty.",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                            )
                            Text(
                                text = "Use Google Imagen 3 above to craft beautiful artwork, then add them to your desk.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    // Display Workspace items in a custom list/grid
                    Column(
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        workspaceImages.forEachIndexed { index, creation ->
                            val bitmap = remember(creation) {
                                val base64Str = creation.responseText
                                if (creation.type != "VOICEOVER" && !base64Str.isNullOrBlank()) {
                                    try {
                                        val decoded = Base64.decode(base64Str, Base64.DEFAULT)
                                        BitmapFactory.decodeByteArray(decoded, 0, decoded.size)
                                    } catch (e: Exception) {
                                        null
                                    }
                                } else null
                            }

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("workspace_item_$index"),
                                shape = RoundedCornerShape(18.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(72.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (bitmap != null) {
                                            Image(
                                                bitmap = bitmap.asImageBitmap(),
                                                contentDescription = "Workspace item image",
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Crop
                                            )
                                            if (creation.type == "VIDEO") {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .background(Color.Black.copy(alpha = 0.35f)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.PlayCircle,
                                                        contentDescription = "Video item",
                                                        tint = Color.White,
                                                        modifier = Modifier.size(24.dp)
                                                    )
                                                }
                                            }
                                        } else {
                                            Icon(
                                                imageVector = when (creation.type) {
                                                    "VIDEO" -> Icons.Default.Movie
                                                    "VOICEOVER" -> Icons.Default.Mic
                                                    else -> Icons.Default.Image
                                                },
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = creation.title.ifBlank { "Untitled Masterwork" },
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = creation.prompt,
                                            fontSize = 11.sp,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    // Action buttons for each Workspace Item
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        IconButton(
                                            onClick = { fullscreenCreation = creation },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.ZoomIn,
                                                contentDescription = "Inspect item",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                        IconButton(
                                            onClick = { triggerExport(creation) },
                                            modifier = Modifier
                                                .size(36.dp)
                                                .testTag("export_workspace_item_button_$index")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Download,
                                                contentDescription = "Export item to device",
                                                tint = MaterialTheme.colorScheme.secondary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                        IconButton(
                                            onClick = {
                                                viewModel.removeImageFromWorkspace(creation)
                                                Toast.makeText(context, "Removed from Workspace!", Toast.LENGTH_SHORT).show()
                                            },
                                            modifier = Modifier
                                                .size(36.dp)
                                                .testTag("remove_workspace_item_button_$index")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Remove item from workspace",
                                                tint = MaterialTheme.colorScheme.error,
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
        }

        // Generating / Processing HUD
        if (isGenerating) {
            ToolInputProcessingOverlay(
                message = generationStep.ifBlank { "Synthesizing artwork..." },
                modifier = Modifier.fillMaxSize()
            )
        }

        // Fullscreen Lightbox / Zoom dialog
        fullscreenCreation?.let { creation ->
            val bitmap = remember(creation) {
                val base64Str = creation.responseText
                if (creation.type != "VOICEOVER" && !base64Str.isNullOrBlank()) {
                    try {
                        val decoded = Base64.decode(base64Str, Base64.DEFAULT)
                        BitmapFactory.decodeByteArray(decoded, 0, decoded.size)
                    } catch (e: Exception) {
                        null
                    }
                } else null
            }

            Dialog(onDismissRequest = { fullscreenCreation = null }) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Black)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = creation.title.ifBlank { "Untitled Artwork" },
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                IconButton(
                                    onClick = { triggerExport(creation) },
                                    modifier = Modifier.testTag("lightbox_export_button")
                                ) {
                                    Icon(Icons.Default.Download, contentDescription = "Export artwork", tint = Color.White)
                                }
                                IconButton(onClick = { fullscreenCreation = null }) {
                                    Icon(Icons.Default.Close, contentDescription = "Close lightbox", tint = Color.White)
                                }
                            }
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(260.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.DarkGray),
                            contentAlignment = Alignment.Center
                        ) {
                            if (bitmap != null) {
                                Image(
                                    bitmap = bitmap.asImageBitmap(),
                                    contentDescription = "Fullscreen preview",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Fit
                                )
                            }
                            if (creation.type == "VIDEO") {
                                // Overlay animated playback canvas for high-fidelity feel inside lightbox!
                                var playProgress by remember { mutableStateOf(0.0f) }
                                LaunchedEffect(Unit) {
                                    while (true) {
                                        playProgress += 0.02f
                                        if (playProgress >= 1.0f) playProgress = 0.0f
                                        delay(100)
                                    }
                                }
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    val w = size.width
                                    val h = size.height
                                    val phase = playProgress * 2 * Math.PI.toFloat()
                                    
                                    // floating light particles
                                    for (i in 0 until 8) {
                                        val px = (sin(i.toDouble() + playProgress) * 0.4 + 0.5) * w
                                        val py = ((playProgress + i * 0.1f) % 1.0f) * h
                                        drawCircle(
                                            color = NovaSecondary.copy(alpha = 0.25f),
                                            radius = 8.dp.toPx(),
                                            center = Offset(px.toFloat(), py.toFloat())
                                        )
                                    }
                                }
                                // Play icon overlay to indicate it's a looping video
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(12.dp)
                                        .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                                        .padding(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Loop,
                                        contentDescription = "Looping video",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            
                            if (creation.type == "VOICEOVER") {
                                var isLightboxAudioPlaying by remember { mutableStateOf(false) }
                                var lightboxAudioProgress by remember { mutableStateOf(0f) }
                                val lightboxPlayer = remember { MediaPlayer() }
                                
                                DisposableEffect(Unit) {
                                    onDispose {
                                        try {
                                            lightboxPlayer.stop()
                                            lightboxPlayer.release()
                                        } catch (e: Exception) {}
                                    }
                                }

                                LaunchedEffect(isLightboxAudioPlaying) {
                                    if (isLightboxAudioPlaying) {
                                        while (isLightboxAudioPlaying) {
                                            try {
                                                val current = lightboxPlayer.currentPosition
                                                val duration = lightboxPlayer.duration.coerceAtLeast(100)
                                                lightboxAudioProgress = (current.toFloat() / duration).coerceIn(0f, 1f)
                                                if (!lightboxPlayer.isPlaying) {
                                                    isLightboxAudioPlaying = false
                                                    lightboxAudioProgress = 1.0f
                                                }
                                            } catch (e: Exception) {
                                                isLightboxAudioPlaying = false
                                            }
                                            delay(100)
                                        }
                                    }
                                }

                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.Black.copy(alpha = 0.8f))
                                        .padding(16.dp),
                                    verticalArrangement = Arrangement.Center,
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    IconButton(
                                        onClick = {
                                            val rawPath = creation.responseText
                                            if (rawPath.isNotEmpty()) {
                                                try {
                                                    if (isLightboxAudioPlaying) {
                                                        lightboxPlayer.pause()
                                                        isLightboxAudioPlaying = false
                                                    } else {
                                                        lightboxPlayer.reset()
                                                        lightboxPlayer.setDataSource(rawPath)
                                                        lightboxPlayer.prepare()
                                                        lightboxPlayer.start()
                                                        isLightboxAudioPlaying = true
                                                        lightboxPlayer.setOnCompletionListener {
                                                            isLightboxAudioPlaying = false
                                                            lightboxAudioProgress = 1.0f
                                                        }
                                                    }
                                                } catch (e: Exception) {
                                                    Toast.makeText(context, "Playback error", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        },
                                        modifier = Modifier
                                            .size(64.dp)
                                            .background(NovaTertiary.copy(alpha = 0.15f), CircleShape)
                                            .border(1.5.dp, NovaTertiary, CircleShape)
                                    ) {
                                        Icon(
                                            imageVector = if (isLightboxAudioPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                            contentDescription = "Play/Pause",
                                            tint = NovaTertiary,
                                            modifier = Modifier.size(32.dp)
                                        )
                                    }
                                    
                                    Spacer(modifier = Modifier.height(16.dp))

                                    Canvas(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(80.dp)
                                    ) {
                                        val w = size.width
                                        val h = size.height
                                        val phase = lightboxAudioProgress * 2 * Math.PI.toFloat()
                                        val waveAmplitude = if (isLightboxAudioPlaying) 30f else 5f
                                        
                                        val points = mutableListOf<Offset>()
                                        for (x in 0..60) {
                                            val t = x.toFloat() / 60
                                            val xPos = t * w
                                            val waveFactor = sin(t * 8 * Math.PI.toFloat() + phase)
                                            val yPos = (h / 2f) + waveFactor * waveAmplitude
                                            points.add(Offset(xPos, yPos))
                                        }
                                        for (p in 0 until points.size - 1) {
                                            drawLine(
                                                color = NovaTertiary.copy(alpha = if (isLightboxAudioPlaying) 0.9f else 0.4f),
                                                start = points[p],
                                                end = points[p + 1],
                                                strokeWidth = 3.dp.toPx()
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    Text(
                                        text = "ElevenLabs Vocal: ${creation.cameraAngle.ifBlank { "Serena Warm Narrator" }}",
                                        color = Color.White.copy(alpha = 0.7f),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }

                        Text(
                            text = creation.prompt,
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                    }
                }
            }
        }
    }
}
}
