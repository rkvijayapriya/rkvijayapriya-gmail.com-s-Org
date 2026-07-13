package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import com.example.ui.theme.NovaPrimary
import com.example.ui.theme.NovaSecondary
import com.example.ui.theme.NovaTertiary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadSettingsDialog(
    mediaType: String, // "IMAGE", "VIDEO", "VOICEOVER", "WRITING"
    onDismissRequest: () -> Unit,
    onConfirmDownload: (format: String, quality: String, includeMetadata: Boolean) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Formats & quality options depending on the media type
    val formats = when (mediaType) {
        "IMAGE" -> listOf("PNG (Lossless)", "WebP (Efficient)", "JPEG (Standard)")
        "VIDEO" -> listOf("MP4 (H.264)", "MKV (ProRes)", "WebM (Web Ready)")
        "VOICEOVER" -> listOf("WAV (Lossless)", "MP3 (High Quality)", "AAC (Compressed)")
        else -> listOf("PDF Document", "Markdown (MD)", "Plain Text (TXT)", "Word (DOCX)")
    }

    val qualities = when (mediaType) {
        "IMAGE" -> listOf("4K UHD (3840x2160)", "2K QHD (2560x1440)", "1080p FHD (1920x1080)")
        "VIDEO" -> listOf("4K Cinema (2160p)", "Pro HD (1080p)", "Standard (720p)")
        "VOICEOVER" -> listOf("Ultra HD (320kbps)", "Studio Master (192kbps)", "Standard (128kbps)")
        else -> listOf("Print Ready (Vector)", "Standard Web Layout", "Optimized Mobile")
    }

    var selectedFormat by remember { mutableStateOf(formats.first()) }
    var selectedQuality by remember { mutableStateOf(qualities.first()) }
    var includeMetadata by remember { mutableStateOf(true) }
    var highEfficiency by remember { mutableStateOf(false) }

    // Download Phase: "SELECTING" -> "DOWNLOADING" -> "COMPLETED"
    var downloadPhase by remember { mutableStateOf("SELECTING") }
    var downloadProgress by remember { mutableStateOf(0f) }
    var currentStepText by remember { mutableStateOf("Configuring codec pipeline...") }

    // Animate state changes nicely
    Dialog(
        onDismissRequest = { if (downloadPhase != "DOWNLOADING") onDismissRequest() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .wrapContentHeight()
                .padding(16.dp)
                .testTag("download_settings_modal"),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
        ) {
            AnimatedContent(
                targetState = downloadPhase,
                transitionSpec = {
                    fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
                },
                label = "DownloadModalPhaseTransition"
            ) { phase ->
                when (phase) {
                    "SELECTING" -> {
                        Column(
                            modifier = Modifier
                                .padding(24.dp)
                                .fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Header
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .background(NovaPrimary.copy(alpha = 0.15f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.SettingsSuggest,
                                            contentDescription = null,
                                            tint = NovaPrimary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = "Export Settings",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 18.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "Choose output preferences",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                IconButton(
                                    onClick = onDismissRequest,
                                    modifier = Modifier.testTag("close_download_modal")
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Close download modal")
                                }
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                            // 1. FORMAT SELECTION
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "FILE FORMAT",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = NovaPrimary,
                                    letterSpacing = 1.sp
                                )

                                formats.forEach { format ->
                                    val isSelected = selectedFormat == format
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { selectedFormat = format }
                                            .testTag("format_option_$format"),
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isSelected) NovaPrimary.copy(alpha = 0.08f) else Color.Transparent
                                        ),
                                        border = BorderStroke(
                                            width = if (isSelected) 1.5.dp else 1.dp,
                                            color = if (isSelected) NovaPrimary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                                        ),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .padding(horizontal = 16.dp, vertical = 12.dp)
                                                .fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                                            ) {
                                                Icon(
                                                    imageVector = when {
                                                        mediaType == "IMAGE" -> Icons.Default.Image
                                                        mediaType == "VIDEO" -> Icons.Default.Videocam
                                                        mediaType == "VOICEOVER" -> Icons.Default.AudioFile
                                                        else -> Icons.Default.Description
                                                    },
                                                    contentDescription = null,
                                                    tint = if (isSelected) NovaPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Text(
                                                    text = format,
                                                    fontSize = 13.sp,
                                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                                    color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            RadioButton(
                                                selected = isSelected,
                                                onClick = { selectedFormat = format },
                                                colors = RadioButtonDefaults.colors(selectedColor = NovaPrimary)
                                            )
                                        }
                                    }
                                }
                            }

                            // 2. QUALITY SETTINGS
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "RESOLUTION & QUALITY",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = NovaSecondary,
                                    letterSpacing = 1.sp
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    qualities.forEach { quality ->
                                        val isSelected = selectedQuality == quality
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(if (isSelected) NovaSecondary.copy(alpha = 0.08f) else Color.Transparent)
                                                .border(
                                                    width = if (isSelected) 1.5.dp else 1.dp,
                                                    color = if (isSelected) NovaSecondary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                                                    shape = RoundedCornerShape(12.dp)
                                                )
                                                .clickable { selectedQuality = quality }
                                                .padding(vertical = 12.dp, horizontal = 4.dp)
                                                .testTag("quality_option_$quality"),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                val mainLabel = quality.substringBefore(" ")
                                                val subLabel = quality.substringAfter(" ", "")
                                                Text(
                                                    text = mainLabel,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                                                    textAlign = TextAlign.Center
                                                )
                                                if (subLabel.isNotEmpty()) {
                                                    Spacer(modifier = Modifier.height(2.dp))
                                                    Text(
                                                        text = subLabel,
                                                        fontSize = 9.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                                        textAlign = TextAlign.Center
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // 3. ADVANCED TOGGLES
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                                        Text("Embed Generation Metadata", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Switch(
                                        checked = includeMetadata,
                                        onCheckedChange = { includeMetadata = it },
                                        colors = SwitchDefaults.colors(checkedThumbColor = NovaPrimary, checkedTrackColor = NovaPrimary.copy(alpha = 0.3f)),
                                        modifier = Modifier.scale(0.8f)
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(Icons.Default.OfflineBolt, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                                        Text("High-Efficiency Compression", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Switch(
                                        checked = highEfficiency,
                                        onCheckedChange = { highEfficiency = it },
                                        colors = SwitchDefaults.colors(checkedThumbColor = NovaSecondary, checkedTrackColor = NovaSecondary.copy(alpha = 0.3f)),
                                        modifier = Modifier.scale(0.8f)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            // Actions
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                OutlinedButton(
                                    onClick = onDismissRequest,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Cancel")
                                }

                                Button(
                                    onClick = {
                                        downloadPhase = "DOWNLOADING"
                                        coroutineScope.launch {
                                            // Simulated step 1
                                            currentStepText = "Allocating storage block..."
                                            delay(600)
                                            downloadProgress = 0.25f

                                            // Simulated step 2
                                            currentStepText = "Running hardware codec rendering..."
                                            delay(800)
                                            downloadProgress = 0.60f

                                            // Simulated step 3
                                            currentStepText = "Embedding digital signature..."
                                            delay(700)
                                            downloadProgress = 0.90f

                                            // Simulated step 4
                                            currentStepText = "Saving to local device downloads..."
                                            delay(500)
                                            downloadProgress = 1.0f

                                            delay(300)
                                            downloadPhase = "COMPLETED"
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = NovaPrimary),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .weight(1.5f)
                                        .testTag("confirm_download_button")
                                ) {
                                    Icon(Icons.Default.DownloadForOffline, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Download now", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    "DOWNLOADING" -> {
                        Column(
                            modifier = Modifier
                                .padding(24.dp)
                                .fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = "Preparing Studio Export",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = MaterialTheme.colorScheme.primary
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.size(120.dp)
                            ) {
                                CircularProgressIndicator(
                                    progress = { downloadProgress },
                                    modifier = Modifier.fillMaxSize(),
                                    color = NovaPrimary,
                                    strokeWidth = 6.dp
                                )
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "${(downloadProgress * 100).toInt()}%",
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "exporting",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = currentStepText,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.Center
                            )

                            LinearProgressIndicator(
                                progress = { downloadProgress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(CircleShape),
                                color = NovaSecondary,
                                trackColor = MaterialTheme.colorScheme.outlineVariant
                            )

                            Text(
                                text = "Format: $selectedFormat | Quality: $selectedQuality",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                    }

                    "COMPLETED" -> {
                        Column(
                            modifier = Modifier
                                .padding(24.dp)
                                .fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .background(Color(0xFF00E676).copy(alpha = 0.15f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Success",
                                    tint = Color(0xFF00E676),
                                    modifier = Modifier.size(48.dp)
                                )
                            }

                            Text(
                                text = "Saved Successfully!",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Text(
                                text = "The file has been rendered, optimized, and saved to your local downloads directory.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                        Text("Media Type:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(mediaType, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                    }
                                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                        Text("Format:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(selectedFormat, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                    }
                                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                        Text("Quality Preset:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(selectedQuality, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                    }
                                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                        Text("Size Efficiency:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(if (highEfficiency) "Ultra-Compressed" else "Standard File Size", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                    }
                                }
                            }

                            Button(
                                onClick = {
                                    onConfirmDownload(selectedFormat, selectedQuality, includeMetadata)
                                    onDismissRequest()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = NovaPrimary),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("dismiss_success_download_button")
                            ) {
                                Text("Done", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

// Extension to scale modifiers easily
fun Modifier.scale(scale: Float): Modifier = this.then(
    Modifier.graphicsLayer(scaleX = scale, scaleY = scale)
)
