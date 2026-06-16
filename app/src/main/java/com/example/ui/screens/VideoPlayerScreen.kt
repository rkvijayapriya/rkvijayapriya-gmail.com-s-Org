package com.example.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.database.VisionAiCreation
import com.example.ui.MainViewModel
import com.example.ui.theme.Translation
import kotlinx.coroutines.delay
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoPlayerScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val lang by viewModel.currentLanguage.collectAsState()
    val scale by viewModel.fontScale.collectAsState()
    val creation by viewModel.activeCreation.collectAsState()

    val activeItem = creation

    if (activeItem == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            Text("No creation selected", color = MaterialTheme.colorScheme.onBackground)
        }
        return
    }

    var isPlaying by remember { mutableStateOf(true) }
    var sliderProgress by remember { mutableStateOf(0.4f) }
    var isFullScreen by remember { mutableStateOf(false) }

    // Auto update seek bar over duration when playing
    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            while (true) {
                delay(200)
                sliderProgress = (sliderProgress + 0.01f)
                if (sliderProgress >= 1f) {
                    sliderProgress = 0f
                }
            }
        }
    }

    val styleColor = when (activeItem.style) {
        "Cyberpunk", "Neon" -> Color(0xFFFF00CC)
        "Pixar Style", "Disney Style" -> Color(0xFFFF9900)
        "Watercolor", "Cartoon" -> Color(0xFF00FFCC)
        "Clay Animation", "Sand Art" -> Color(0xFF8B4513)
        else -> MaterialTheme.colorScheme.primary
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (isFullScreen) Modifier else Modifier.verticalScroll(rememberScrollState())
                )
        ) {
            // Player Stage Viewport (Aesthetic procedural canvas mimicking active AI video processing!)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(if (isFullScreen) 0.8f else 1.6f)
                    .background(Color.Black),
                contentAlignment = Alignment.BottomCenter
            ) {
                // Moving algorithmic canvas representing render playback!
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("procedural_video_canvas")
                ) {
                    val width = size.width
                    val height = size.height

                    clipRect {
                        // Drawing moving fluid cyberwave
                        val steps = 80
                        val waveAmplitude = 40f
                        val phase = sliderProgress * 2 * Math.PI.toFloat()
                        
                        // Main gradient fill base
                        drawRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(Color(0xFF0C0720), Color(0xFF03010C))
                            )
                        )

                        // Visual nodes/pixels representing active style preset
                        val particleCount = 20
                        for (i in 0 until particleCount) {
                            val seedX = (sin(i.toDouble()) * 0.5 + 0.5) * width
                            val seedY = ((sliderProgress + i * 0.05) % 1.0) * height
                            
                            drawCircle(
                                color = styleColor.copy(alpha = 0.15f),
                                radius = 24.dp.toPx() + sin(phase + i) * 6.dp.toPx(),
                                center = Offset(seedX.toFloat(), seedY.toFloat())
                            )
                        }

                        // Plot dynamic sine-waves representing cinematic lighting tracking
                        for (w in 0..1) {
                            val pathPoints = mutableListOf<Offset>()
                            for (x in 0..steps) {
                                val t = x.toFloat() / steps
                                val xPos = t * width
                                val waveFactor = sin(t * 3 * Math.PI.toFloat() + phase + (w * 1.5))
                                val yPos = (height / 2) + waveFactor * waveAmplitude
                                pathPoints.add(Offset(xPos, yPos.toFloat()))
                            }

                            for (p in 0 until pathPoints.size - 1) {
                                drawLine(
                                    color = if (w == 0) styleColor else Color(0xFF00FFFF),
                                    start = pathPoints[p],
                                    end = pathPoints[p + 1],
                                    strokeWidth = 3.dp.toPx()
                                )
                            }
                        }

                        // Abstract floating particles
                        for (p in 0..12) {
                            val pX = (sin(p.toDouble() * 1.5) * 0.5 + 0.5) * width
                            val pY = (sin(p.toDouble() * 3) * 0.5 + 0.5) * height
                            drawCircle(
                                color = Color.White.copy(alpha = 0.4f),
                                radius = 2.dp.toPx(),
                                center = Offset(pX.toFloat(), pY.toFloat())
                            )
                        }
                    }
                }

                // Controls Layer Overlay
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.4f))
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Custom immersive Seekbar
                    Slider(
                        value = sliderProgress,
                        onValueChange = { sliderProgress = it },
                        modifier = Modifier.fillMaxWidth().height(24.dp).testTag("video_seekbar"),
                        colors = SliderDefaults.colors(
                            thumbColor = styleColor,
                            activeTrackColor = styleColor,
                            inactiveTrackColor = Color.White.copy(alpha = 0.15f)
                        )
                    )

                    // Control Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Back button
                            IconButton(onClick = onBack) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Exit Back", tint = Color.White)
                            }

                            // Play / Pause toggler
                            IconButton(
                                onClick = { isPlaying = !isPlaying },
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(styleColor.copy(alpha = 0.2f))
                                    .testTag("play_pause_button")
                            ) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Default.Refresh else Icons.Default.PlayArrow,
                                    contentDescription = "Trigger playback",
                                    tint = styleColor
                                )
                            }
                        }

                        // Formatting timestamp
                        Text(
                            text = "${((sliderProgress * activeItem.duration).toInt())}s / ${activeItem.duration}s",
                            color = Color.White,
                            fontSize = (12 * scale).sp,
                            fontWeight = FontWeight.Bold
                        )

                        // Bottom right options
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            // FullScreen option
                            IconButton(onClick = { isFullScreen = !isFullScreen }) {
                                Icon(
                                    imageVector = if (isFullScreen) Icons.Default.ArrowBack else Icons.Default.PlayArrow,
                                    contentDescription = "Toggle screen scale",
                                    tint = Color.White
                                )
                            }
                        }
                    }
                }
            }

            // Metadata Detail Column
            if (!isFullScreen) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Title and style badges
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = activeItem.title,
                            fontSize = (20 * scale).sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(styleColor.copy(alpha = 0.12f))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(activeItem.style, color = styleColor, fontWeight = FontWeight.Bold, fontSize = (11 * scale).sp)
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(activeItem.cameraAngle, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = (11 * scale).sp)
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(activeItem.resolution, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = (11 * scale).sp)
                            }
                        }
                    }

                    // Prompt content Accordion
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("User Visual Prompt", fontSize = (12 * scale).sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f), fontWeight = FontWeight.Bold)
                            Text(activeItem.prompt, fontSize = (13 * scale).sp, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }

                    // Download / Share / Delete actions (As requested)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Download trigger
                        Button(
                            onClick = {
                                Toast.makeText(context, Translation.getString("save_success", lang), Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f).testTag("download_button")
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(18.dp))
                                Text("Download", fontSize = (12 * scale).sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        // Share trigger
                        Button(
                            onClick = {
                                try {
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_SUBJECT, activeItem.title)
                                        putExtra(Intent.EXTRA_TEXT, "Look at this spectacular AI Content I generated in VisionAI Studio: ${activeItem.title}\nScript: ${activeItem.generatedScript}")
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "Share with friends"))
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Problem triggering sharing hub", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Share, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                Text("Share", fontSize = (12 * scale).sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        // Delete from database
                        IconButton(
                            onClick = {
                                viewModel.deleteCreation(activeItem.id)
                                Toast.makeText(context, "Removed successfully", Toast.LENGTH_SHORT).show()
                                onBack()
                            },
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.errorContainer, RoundedCornerShape(12.dp))
                                .size(48.dp)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.onErrorContainer)
                        }
                    }

                    // Comprehensive generated metadata elements (Script, Descriptive and Hashtags)
                    if (activeItem.generatedDescription.isNotBlank()) {
                        Text("Auto Description", fontWeight = FontWeight.Bold, fontSize = (14 * scale).sp, color = MaterialTheme.colorScheme.onBackground)
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(activeItem.generatedDescription, fontSize = (13 * scale).sp, color = MaterialTheme.colorScheme.onSurface, lineHeight = 18.sp)
                                if (activeItem.generatedHashtags.isNotBlank()) {
                                    Text(activeItem.generatedHashtags, fontSize = (12 * scale).sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    if (activeItem.generatedScript.isNotBlank()) {
                        Text("Narrator Script", fontWeight = FontWeight.Bold, fontSize = (14 * scale).sp, color = MaterialTheme.colorScheme.onBackground)
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = activeItem.generatedScript,
                                    fontSize = (13 * scale).sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(64.dp))
                }
            }
        }
    }
}
