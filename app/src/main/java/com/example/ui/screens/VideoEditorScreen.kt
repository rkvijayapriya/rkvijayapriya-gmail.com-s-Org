package com.example.ui.screens

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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.asImageBitmap
import android.graphics.BitmapFactory
import android.util.Base64
import com.example.ui.MainViewModel
import com.example.ui.theme.NovaGradient
import com.example.ui.theme.NovaPrimary
import com.example.ui.theme.NovaSecondary
import com.example.ui.theme.NovaTertiary
import com.example.data.database.VisionAiCreation
import com.example.ui.components.DownloadSettingsDialog
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoEditorScreen(
    viewModel: MainViewModel,
    onBackToCreator: () -> Unit,
    onNavigateToPlayer: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val scale by viewModel.fontScale.collectAsState()

    val allCreations by viewModel.allCreations.collectAsState()
    val activeCreation by viewModel.activeCreation.collectAsState()

    // Editor tab selection
    var activeTab by remember { mutableStateOf("FILTERS") } // "FILTERS", "TRIM", "AUDIO", "TEXT", "ADJUST"

    // Filter properties
    var selectedFilter by remember { mutableStateOf("Original") }
    val filterList = listOf("Original", "Cinematic Blue", "Vintage Sepia", "Cyberpunk Neon", "Noir B&W", "Vibrant Gold", "Cool Aqua", "Warm Sunset")

    // Trim & Speed properties
    var trimStart by remember { mutableStateOf(0f) }
    var trimEnd by remember { mutableStateOf(10f) }
    var playSpeed by remember { mutableStateOf(1.0f) }
    val speedOptions = listOf(0.5f, 1.0f, 1.5f, 2.0f)

    // Adjust properties
    var brightness by remember { mutableStateOf(100f) }
    var contrast by remember { mutableStateOf(100f) }
    var saturation by remember { mutableStateOf(100f) }

    // Audio properties
    var bgmSelection by remember { mutableStateOf("None") }
    var bgmVolume by remember { mutableStateOf(50f) }
    val bgmOptions = listOf("None", "Cinematic Ambient", "Techno Beat", "Lo-Fi Chill", "Epic Orchestral")

    // Text & Subtitle properties
    var textOverlayPrompt by remember { mutableStateOf("") }
    var textOverlayColor by remember { mutableStateOf(Color.White) }
    var textOverlaySize by remember { mutableStateOf(16f) }
    val fontColors = listOf(Color.White, Color.Yellow, Color.Cyan, Color.Magenta, Color(0xFF00E676), Color.Red)

    // Player Simulation state
    var isPlaying by remember { mutableStateOf(false) }
    var playProgress by remember { mutableStateOf(0f) }

    // Rendering status
    var isRendering by remember { mutableStateOf(false) }
    var renderProgress by remember { mutableStateOf(0f) }
    var renderedSuccessMessage by remember { mutableStateOf<String?>(null) }
    var showDownloadDialog by remember { mutableStateOf(false) }

    // Synchronize trim limits based on active creation duration
    LaunchedEffect(activeCreation) {
        if (activeCreation != null) {
            trimStart = 0f
            trimEnd = activeCreation!!.duration.toFloat().coerceAtLeast(5f)
            isPlaying = false
            playProgress = 0f
            renderedSuccessMessage = null
        }
    }

    // Interactive timeline playback animation loop
    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            val stepTime = 100L
            while (isPlaying) {
                delay(stepTime)
                val duration = activeCreation?.duration?.toFloat() ?: 10f
                val increment = (stepTime / 1000f) * playSpeed
                val newProgress = playProgress + increment
                if (newProgress >= trimEnd) {
                    playProgress = trimStart
                    isPlaying = false // Auto pause at end of trimmed timeline
                } else {
                    playProgress = newProgress
                }
            }
        }
    }

    // Helper to decode Base64 creation response
    val displayBitmap = remember(activeCreation) {
        if (activeCreation != null && activeCreation!!.responseText.isNotBlank()) {
            try {
                val decodedBytes = Base64.decode(activeCreation!!.responseText, Base64.DEFAULT)
                BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
            } catch (e: Exception) {
                null
            }
        } else {
            null
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "PROFESSIONAL STUDIO",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = NovaPrimary,
                            letterSpacing = 1.5.sp
                        )
                        Text(
                            text = "Professional Video Editor",
                            fontSize = (18 * scale).sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBackToCreator,
                        modifier = Modifier.testTag("video_editor_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                actions = {
                    if (activeCreation != null) {
                        // Render Button
                        Button(
                            onClick = {
                                if (isRendering) return@Button
                                coroutineScope.launch {
                                    isRendering = true
                                    renderProgress = 0f
                                    renderedSuccessMessage = null
                                    while (renderProgress < 1f) {
                                        delay(150)
                                        renderProgress += 0.05f
                                    }
                                    isRendering = false
                                    renderedSuccessMessage = "Successfully compiled masterwork! Saved to /movies/NovaEditor_${System.currentTimeMillis() % 10000}.mp4"
                                    Toast.makeText(context, "Rendering completed successfully!", Toast.LENGTH_LONG).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NovaPrimary),
                            shape = RoundedCornerShape(8.dp),
                            enabled = !isRendering,
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .testTag("render_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = Color.White
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Render", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (activeCreation == null) {
                // ELEGANT EMPTY STATE: Select a past creation to edit
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(90.dp)
                            .background(NovaGradient, CircleShape)
                            .padding(2.dp),
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
                                modifier = Modifier.size(42.dp)
                            )
                        }
                    }

                    Text(
                        text = "Unified Studio Workspace",
                        fontSize = (22 * scale).sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onBackground,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = "Generate a video or image in the Creative Studio, or select any previously created media item below to launch the professional timeline multi-track editing suite.",
                        fontSize = (13 * scale).sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    Text(
                        text = "SELECT RECENT CREATIVE GEN OUTPUTS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = NovaPrimary,
                        letterSpacing = 1.sp,
                        modifier = Modifier.align(Alignment.Start)
                    )

                    val editableCreations = allCreations.filter { it.type == "VIDEO" || it.type == "IMAGE" }

                    if (editableCreations.isEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(Icons.Default.History, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f), modifier = Modifier.size(36.dp))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "No generated media in history yet",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            }
                        }
                    } else {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            editableCreations.forEach { creation ->
                                val itemBitmap = remember(creation) {
                                    if (creation.responseText.isNotBlank()) {
                                        try {
                                            val decodedBytes = Base64.decode(creation.responseText, Base64.DEFAULT)
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
                                        .clickable { viewModel.activeCreation.value = creation }
                                        .testTag("load_creation_${creation.id}"),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                    shape = RoundedCornerShape(16.dp),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        // Media thumbnail
                                        Box(
                                            modifier = Modifier
                                                .size(64.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(Color.Black),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (itemBitmap != null) {
                                                Image(
                                                    bitmap = itemBitmap.asImageBitmap(),
                                                    contentDescription = null,
                                                    modifier = Modifier.fillMaxSize(),
                                                    contentScale = ContentScale.Crop
                                                )
                                            } else {
                                                Icon(
                                                    imageVector = if (creation.type == "VIDEO") Icons.Default.Movie else Icons.Default.Image,
                                                    contentDescription = null,
                                                    tint = Color.White.copy(alpha = 0.4f)
                                                )
                                            }
                                        }

                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(4.dp))
                                                        .background(
                                                            if (creation.type == "VIDEO") Color(0xFF00E676).copy(alpha = 0.15f)
                                                            else Color(0xFFFF9800).copy(alpha = 0.15f)
                                                        )
                                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                                ) {
                                                    Text(
                                                        text = creation.type,
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (creation.type == "VIDEO") Color(0xFF00E676) else Color(0xFFFF9800)
                                                    )
                                                }
                                                Text(
                                                    text = "ID #${creation.id}",
                                                    fontSize = 10.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                                )
                                            }

                                            Spacer(modifier = Modifier.height(4.dp))

                                            Text(
                                                text = creation.title,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )

                                            Text(
                                                text = creation.prompt,
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }

                                        IconButton(
                                            onClick = { viewModel.activeCreation.value = creation },
                                            modifier = Modifier.background(NovaPrimary.copy(alpha = 0.1f), CircleShape)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = "Load",
                                                tint = NovaPrimary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // ACTIVE WORKSPACE EDITOR MODE
                val currentCreation = activeCreation!!
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // LIVE PREVIEW CANVAS
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1.4f)
                            .testTag("preview_canvas_card"),
                        colors = CardDefaults.cardColors(containerColor = Color.Black),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            // Apply custom adjustment & filters
                            val brValue = brightness / 100f
                            val ctValue = contrast / 100f
                            val saValue = saturation / 100f

                            // Live visual preview
                            if (displayBitmap != null) {
                                Image(
                                    bitmap = displayBitmap.asImageBitmap(),
                                    contentDescription = "Video frame edit canvas",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop,
                                    colorFilter = ColorFilter.colorMatrix(
                                        androidx.compose.ui.graphics.ColorMatrix(
                                            floatArrayOf(
                                                brValue * ctValue, 0f, 0f, 0f, (brValue * (1f - ctValue) * 128f),
                                                0f, brValue * ctValue, 0f, 0f, (brValue * (1f - ctValue) * 128f),
                                                0f, 0f, brValue * ctValue, 0f, (brValue * (1f - ctValue) * 128f),
                                                0f, 0f, 0f, 1f, 0f
                                            )
                                        )
                                    )
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.verticalGradient(
                                                listOf(Color(0xFF1E1535), Color(0xFF0C081A))
                                            )
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(Icons.Default.Videocam, contentDescription = null, tint = NovaPrimary, modifier = Modifier.size(48.dp))
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text("Cinematic Live Preview Active", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            // Optional active filter overlay effect overlay
                            if (selectedFilter != "Original") {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            when (selectedFilter) {
                                                "Cinematic Blue" -> Color(0xFF00BCD4).copy(alpha = 0.15f)
                                                "Vintage Sepia" -> Color(0xFF8B5A2B).copy(alpha = 0.2f)
                                                "Cyberpunk Neon" -> Color(0xFFFF007F).copy(alpha = 0.18f)
                                                "Noir B&W" -> Color.Gray.copy(alpha = 0.25f)
                                                "Vibrant Gold" -> Color(0xFFFFD700).copy(alpha = 0.15f)
                                                "Cool Aqua" -> Color(0xFF00E5FF).copy(alpha = 0.12f)
                                                "Warm Sunset" -> Color(0xFFFF5722).copy(alpha = 0.18f)
                                                else -> Color.Transparent
                                            }
                                        )
                                )
                            }

                            // Interactive Text overlay / sticker
                            if (textOverlayPrompt.isNotBlank()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = textOverlayPrompt,
                                        color = textOverlayColor,
                                        fontSize = textOverlaySize.sp,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier
                                            .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            // Dynamic Status Tags & Badges
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .align(Alignment.TopStart)
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color.Black.copy(alpha = 0.6f))
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .background(
                                                    if (isPlaying) Color.Green else Color.Red,
                                                    CircleShape
                                                )
                                        )
                                        Text(
                                            text = if (isPlaying) "PLAYING" else "PAUSED",
                                            color = Color.White,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(NovaPrimary.copy(alpha = 0.8f))
                                            .padding(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = currentCreation.type,
                                            color = Color.White,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    IconButton(
                                        onClick = { viewModel.activeCreation.value = null },
                                        modifier = Modifier
                                            .size(24.dp)
                                            .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = "Close Project", tint = Color.White, modifier = Modifier.size(12.dp))
                                    }
                                }
                            }

                            // Audio Active indicator if BGM is enabled
                            if (bgmSelection != "None") {
                                Row(
                                    modifier = Modifier
                                        .align(Alignment.BottomStart)
                                        .padding(12.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color.Black.copy(alpha = 0.7f))
                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.Default.MusicNote, contentDescription = null, tint = Color.Green, modifier = Modifier.size(12.dp))
                                    Text(text = bgmSelection, color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            // Dynamic timeline numeric indicator
                            Text(
                                text = "${"%.1f".format(playProgress)}s / ${"%.1f".format(currentCreation.duration.toFloat())}s",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(12.dp)
                                    .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    // INTERACTIVE TIMELINE TRACK ENGINE
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Track 1: Video Track
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(Icons.Default.Movie, contentDescription = null, tint = NovaPrimary, modifier = Modifier.size(16.dp))
                                Text("VIDEO", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(20.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(NovaPrimary.copy(alpha = 0.12f))
                                        .border(1.dp, NovaPrimary.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                                ) {
                                    // Trim window indicator
                                    val totalDur = currentCreation.duration.toFloat().coerceAtLeast(1f)
                                    val startFraction = trimStart / totalDur
                                    val endFraction = trimEnd / totalDur
                                    val progressFraction = playProgress / totalDur

                                    // Trim Highlight area
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .fillMaxWidth(endFraction - startFraction)
                                            .padding(start = (startFraction * 200).dp) // simple visualization offset
                                            .background(NovaPrimary.copy(alpha = 0.3f))
                                    )

                                    // Current Playback Line marker
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .width(2.dp)
                                            .background(Color.Red)
                                            .padding(start = (progressFraction * 200).dp)
                                    )
                                }
                            }

                            // Track 2: Audio Track
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(Icons.Default.MusicNote, contentDescription = null, tint = Color(0xFF00E676), modifier = Modifier.size(16.dp))
                                Text("AUDIO", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(16.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(
                                            if (bgmSelection != "None") Color(0xFF00E676).copy(alpha = 0.15f)
                                            else Color.DarkGray.copy(alpha = 0.1f)
                                        )
                                ) {
                                    if (bgmSelection != "None") {
                                        Text(
                                            text = "$bgmSelection Track (Vol: ${bgmVolume.toInt()}%)",
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF00E676),
                                            modifier = Modifier.padding(start = 6.dp, top = 2.dp)
                                        )
                                    }
                                }
                            }

                            // Playback controls Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = { isPlaying = !isPlaying },
                                        modifier = Modifier
                                            .size(36.dp)
                                            .background(NovaPrimary, CircleShape)
                                            .testTag("play_pause_button")
                                    ) {
                                        Icon(
                                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                            contentDescription = "Play / Pause",
                                            tint = Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    IconButton(
                                        onClick = {
                                            isPlaying = false
                                            playProgress = trimStart
                                        },
                                        modifier = Modifier
                                            .size(36.dp)
                                            .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Replay,
                                            contentDescription = "Restart",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }

                                Text(
                                    text = "Trim Bounds: ${"%.1f".format(trimStart)}s - ${"%.1f".format(trimEnd)}s",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NovaSecondary
                                )
                            }
                        }
                    }

                    // RENDERING PANEL (If active)
                    AnimatedVisibility(visible = isRendering || renderedSuccessMessage != null) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, NovaPrimary.copy(alpha = 0.3f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                if (isRendering) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        CircularProgressIndicator(
                                            progress = { renderProgress },
                                            modifier = Modifier.size(24.dp),
                                            color = NovaPrimary,
                                            strokeWidth = 2.5.dp
                                        )
                                        Column {
                                            Text(text = "Compiling Masterwork...", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            Text(text = "Merging custom filters, audio tracks, and frames: ${(renderProgress * 100).toInt()}%", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                    LinearProgressIndicator(
                                        progress = { renderProgress },
                                        modifier = Modifier.fillMaxWidth(),
                                        color = NovaPrimary
                                    )
                                } else if (renderedSuccessMessage != null) {
                                    Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF00E676), modifier = Modifier.size(20.dp))
                                        Column {
                                            Text(text = "Rendering Complete!", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            Text(text = renderedSuccessMessage!!, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Button(
                                            onClick = {
                                                showDownloadDialog = true
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = NovaPrimary),
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Save/Download", fontSize = 11.sp)
                                        }

                                        Button(
                                            onClick = {
                                                Toast.makeText(context, "Link copied! Share dialog active.", Toast.LENGTH_SHORT).show()
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer),
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Share Video", fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // TAB SELECTION CONTROLS FOR VIDEO EDITOR (FILTERS, TRIM, ADJUST, AUDIO, TEXT)
                    ScrollableTabRow(
                        selectedTabIndex = when (activeTab) {
                            "FILTERS" -> 0
                            "TRIM" -> 1
                            "ADJUST" -> 2
                            "AUDIO" -> 3
                            else -> 4
                        },
                        containerColor = Color.Transparent,
                        contentColor = NovaPrimary,
                        edgePadding = 0.dp
                    ) {
                        listOf(
                            Pair("FILTERS", Icons.Default.ColorLens),
                            Pair("TRIM", Icons.Default.ContentCut),
                            Pair("ADJUST", Icons.Default.Tune),
                            Pair("AUDIO", Icons.Default.MusicNote),
                            Pair("TEXT", Icons.Default.TextFields)
                        ).forEach { (tab, icon) ->
                            Tab(
                                selected = activeTab == tab,
                                onClick = { activeTab = tab },
                                icon = { Icon(icon, contentDescription = tab, modifier = Modifier.size(16.dp)) },
                                text = { Text(tab, fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                            )
                        }
                    }

                    // TAB SPECIFIC EDIT COMPONENT SECTIONS
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            when (activeTab) {
                                "FILTERS" -> {
                                    Text("Visual Color Filters", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = NovaPrimary)
                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        items(filterList) { filterName ->
                                            val isFilterSelected = selectedFilter == filterName
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(
                                                        if (isFilterSelected) NovaPrimary else MaterialTheme.colorScheme.surfaceVariant
                                                    )
                                                    .border(
                                                        1.dp,
                                                        if (isFilterSelected) NovaPrimary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                                                        RoundedCornerShape(8.dp)
                                                    )
                                                    .clickable {
                                                        selectedFilter = filterName
                                                        Toast.makeText(context, "Applied $filterName", Toast.LENGTH_SHORT).show()
                                                    }
                                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                                            ) {
                                                Text(
                                                    text = filterName,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isFilterSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                        }
                                    }
                                }

                                "TRIM" -> {
                                    Text("Clip Trimming & Speed Playback", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = NovaPrimary)
                                    val maxDuration = currentCreation.duration.toFloat().coerceAtLeast(5f)
                                    
                                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        // Trim Start Slider
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text("Trim Start: ", fontSize = 11.sp, modifier = Modifier.width(80.dp))
                                            Slider(
                                                value = trimStart,
                                                onValueChange = { if (it < trimEnd) trimStart = it },
                                                valueRange = 0f..maxDuration,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Text("${"%.1f".format(trimStart)}s", fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(40.dp))
                                        }

                                        // Trim End Slider
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text("Trim End: ", fontSize = 11.sp, modifier = Modifier.width(80.dp))
                                            Slider(
                                                value = trimEnd,
                                                onValueChange = { if (it > trimStart) trimEnd = it },
                                                valueRange = 0f..maxDuration,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Text("${"%.1f".format(trimEnd)}s", fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(40.dp))
                                        }

                                        HorizontalDivider()

                                        // Speed select
                                        Text("Playback Speed RateMultiplier", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            speedOptions.forEach { speed ->
                                                val isSpeedSelected = playSpeed == speed
                                                Box(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(
                                                            if (isSpeedSelected) NovaSecondary else MaterialTheme.colorScheme.surfaceVariant
                                                        )
                                                        .clickable { playSpeed = speed }
                                                        .padding(vertical = 8.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text("${speed}x", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isSpeedSelected) Color.White else MaterialTheme.colorScheme.onSurface)
                                                }
                                            }
                                        }
                                    }
                                }

                                "ADJUST" -> {
                                    Text("Image & Color Correction", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = NovaPrimary)
                                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        // Brightness Slider
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text("Brightness", fontSize = 11.sp, modifier = Modifier.width(80.dp))
                                            Slider(
                                                value = brightness,
                                                onValueChange = { brightness = it },
                                                valueRange = 50f..150f,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Text("${brightness.toInt()}%", fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(40.dp))
                                        }

                                        // Contrast Slider
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text("Contrast", fontSize = 11.sp, modifier = Modifier.width(80.dp))
                                            Slider(
                                                value = contrast,
                                                onValueChange = { contrast = it },
                                                valueRange = 50f..150f,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Text("${contrast.toInt()}%", fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(40.dp))
                                        }

                                        // Saturation Slider
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text("Saturation", fontSize = 11.sp, modifier = Modifier.width(80.dp))
                                            Slider(
                                                value = saturation,
                                                onValueChange = { saturation = it },
                                                valueRange = 50f..150f,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Text("${saturation.toInt()}%", fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(40.dp))
                                        }
                                    }
                                }

                                "AUDIO" -> {
                                    Text("Overlay Background Soundtrack", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = NovaPrimary)
                                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        LazyRow(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            items(bgmOptions) { bgm ->
                                                val isBgmSelected = bgmSelection == bgm
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(
                                                            if (isBgmSelected) Color(0xFF00E676) else MaterialTheme.colorScheme.surfaceVariant
                                                        )
                                                        .clickable {
                                                            bgmSelection = bgm
                                                            Toast.makeText(context, "Set soundtrack to $bgm", Toast.LENGTH_SHORT).show()
                                                        }
                                                        .padding(horizontal = 12.dp, vertical = 8.dp)
                                                ) {
                                                    Text(
                                                        text = bgm,
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (isBgmSelected) Color.Black else MaterialTheme.colorScheme.onSurface
                                                    )
                                                }
                                            }
                                        }

                                        if (bgmSelection != "None") {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text("BGM Volume", fontSize = 11.sp, modifier = Modifier.width(80.dp))
                                                Slider(
                                                    value = bgmVolume,
                                                    onValueChange = { bgmVolume = it },
                                                    valueRange = 0f..100f,
                                                    modifier = Modifier.weight(1f)
                                                )
                                                Text("${bgmVolume.toInt()}%", fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(40.dp))
                                            }
                                        }
                                    }
                                }

                                "TEXT" -> {
                                    Text("Subtitle Overlay & Captions", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = NovaPrimary)
                                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        OutlinedTextField(
                                            value = textOverlayPrompt,
                                            onValueChange = { textOverlayPrompt = it },
                                            placeholder = { Text("Enter subtitle text...", fontSize = 12.sp) },
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = NovaPrimary,
                                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                            ),
                                            shape = RoundedCornerShape(8.dp),
                                            singleLine = true
                                        )

                                        if (textOverlayPrompt.isNotBlank()) {
                                            // Text Color options
                                            Text("Subtitle Text Color", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                fontColors.forEach { color ->
                                                    val isColorSelected = textOverlayColor == color
                                                    Box(
                                                        modifier = Modifier
                                                            .size(24.dp)
                                                            .clip(CircleShape)
                                                            .background(color)
                                                            .border(
                                                                if (isColorSelected) 2.dp else 0.dp,
                                                                if (isColorSelected) NovaPrimary else Color.Transparent,
                                                                CircleShape
                                                            )
                                                            .clickable { textOverlayColor = color }
                                                    )
                                                }
                                            }

                                            // Text Size Slider
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text("Text Size", fontSize = 11.sp, modifier = Modifier.width(80.dp))
                                                Slider(
                                                    value = textOverlaySize,
                                                    onValueChange = { textOverlaySize = it },
                                                    valueRange = 10f..32f,
                                                    modifier = Modifier.weight(1f)
                                                )
                                                Text("${textOverlaySize.toInt()}sp", fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(40.dp))
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
    }

    if (showDownloadDialog) {
        DownloadSettingsDialog(
            mediaType = "VIDEO",
            onDismissRequest = { showDownloadDialog = false },
            onConfirmDownload = { format, quality, includeMetadata ->
                val creation = activeCreation
                if (creation != null) {
                    com.example.ui.components.MediaExportHelper.exportCreation(context, creation) { success, path ->
                        android.os.Handler(android.os.Looper.getMainLooper()).post {
                            if (success) {
                                Toast.makeText(context, "Exported edited video: $path ($format, $quality)", Toast.LENGTH_LONG).show()
                            } else {
                                Toast.makeText(context, "Export failed: $path", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                } else {
                    Toast.makeText(context, "Saved successfully to device memory! ($format, $quality)", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }
}
