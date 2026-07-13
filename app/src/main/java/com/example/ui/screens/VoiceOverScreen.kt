package com.example.ui.screens

import android.speech.RecognizerIntent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MainViewModel
import com.example.ui.theme.Translation
import com.example.ui.components.AudioPlayerSkeleton
import com.example.ui.components.ToolInputProcessingOverlay
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.sin

data class VoiceProfile(
    val id: String,
    val name: String,
    val gender: String, // "MALE" or "FEMALE"
    val language: String,
    val styleDesc: String,
    val description: String,
    val elevenLabsVoiceId: String
)

val voiceProfiles = listOf(
    // English Profiles
    VoiceProfile("en_serena", "Serena", "FEMALE", "English", "Warm Narrator", "Soft, expressive, and warm; perfect for storytelling, tutorials, and audiobooks.", "21m00Tcm4TlvDq8ikWAM"), // Rachel
    VoiceProfile("en_arthur", "Arthur", "MALE", "English", "Bold Broadcaster", "Deep, resonant, and highly authoritative; excellent for presentations, news, and documentaries.", "pNInz6obpg7656pky15p"), // Adam
    VoiceProfile("en_sophia", "Sophia", "FEMALE", "English", "Upbeat Conversational", "Bright, enthusiastic, and friendly; ideal for vlogs, social videos, and advertisements.", "EXAVITQu4vr4xnSDxMaL"), // Bella
    VoiceProfile("en_james", "James", "MALE", "English", "Professional Executive", "Clear, formal, and polished; perfect for business pitches, online courses, and guides.", "ErXwobaYiN019PkySvjV"), // Antoni
    
    // Tamil Profiles
    VoiceProfile("ta_kamala", "Kamala", "FEMALE", "Tamil", "Inspirational Narrator", "Sweet, clear, and highly expressive; perfect for poetry, storytelling, and warm greetings.", "MF3mGyEYCl7YOf7k8ebS"), // Ellie
    VoiceProfile("ta_kathir", "Kathir", "MALE", "Tamil", "Resonant Speaker", "Bold, classical, and deep; excellent for dramatic storytelling, historical logs, and news.", "GBv7mTt0atIp3u8iThgY"), // Thomas
    
    // Hindi Profiles
    VoiceProfile("hi_aanya", "Aanya", "FEMALE", "Hindi", "Warm Storyteller", "Melodious, slow, and clear; perfect for audio stories, children's books, and guides.", "zrHiExvYt7vI1AlS7xgB"), // Mimi
    VoiceProfile("hi_aarav", "Aarav", "MALE", "Hindi", "Lively Presenter", "Energetic, clear, and modern; perfect for promotions, announcements, and advertisements.", "VR6AHRvj6537Ph4xm44Z"), // Arnold
    
    // Spanish Profiles
    VoiceProfile("es_lucia", "Lucia", "FEMALE", "Spanish", "Expressive & Natural", "Vibrant, friendly, and authentic; ideal for podcasts, dialogues, and social media content.", "AZnzlk1XvdvUeBnXmlld"), // Domi
    VoiceProfile("es_mateo", "Mateo", "MALE", "Spanish", "Smooth Documentarian", "Warm, measured, and highly professional; excellent for documentaries and video courses.", "N2lVS1wndSFIXED67Yrt"), // Callum
    
    // French Profiles
    VoiceProfile("fr_chloe", "Chloé", "FEMALE", "French", "Elegant Poet", "Poetic, soft, and classic French tone; perfect for travel narration and luxury branding.", "LcfcDJN61GQbYFf87WHR"), // Emily
    VoiceProfile("fr_pierre", "Pierre", "MALE", "French", "Low-pitched Voiceover", "Deep, slow, and dramatic; ideal for theatrical video guides and artistic projects.", "SOYhlZCCsc0v7uU743iC"), // Harry
    
    // German Profiles
    VoiceProfile("de_hannah", "Hannah", "FEMALE", "German", "Precise Educator", "Articulate, rhythmic, and clear; ideal for tutorials, training sessions, and technical guides.", "ThT50A1aJnIuY3Yt7S90"), // Dorothy
    VoiceProfile("de_lukas", "Lukas", "MALE", "German", "Strong Leader", "Deep, direct, and authoritative; perfect for corporate pitches, business news, and overviews.", "TxGEqn7nU6kb7b4GNpHu"), // Josh
    
    // Japanese Profiles
    VoiceProfile("ja_sakura", "Sakura", "FEMALE", "Japanese", "Polite Conversational", "Warm, respectful, and friendly; excellent for localized audio guides and product tours.", "jBpfuIE2acHA8zMs6bLa"), // Gigi
    VoiceProfile("ja_hiroto", "Hiroto", "MALE", "Japanese", "Calm & Dignified", "Steady, professional, and clear; perfect for news bulletins and formal business summaries.", "2EiwXgQa2GZKpFr9UMXM") // Clyde
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceOverScreen(
    viewModel: MainViewModel,
    onNavigateToPlayer: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val lang by viewModel.currentLanguage.collectAsState()
    val scale by viewModel.fontScale.collectAsState()
    val isGenerating by viewModel.isGenerating.collectAsState()
    val generationProgress by viewModel.generationProgress.collectAsState()
    val generationStep by viewModel.generationStep.collectAsState()
    val activeItem by viewModel.activeCreation.collectAsState()

    var textInput by remember { mutableStateOf("") }

    var selectedMediaUriText by remember { mutableStateOf<Uri?>(null) }
    var selectedMediaNameText by remember { mutableStateOf<String?>(null) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val fName = uri.lastPathSegment ?: "Selected File"
            selectedMediaUriText = uri
            selectedMediaNameText = fName
            Toast.makeText(context, "File selected: $fName", Toast.LENGTH_SHORT).show()
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
                textInput = if (textInput.isBlank()) spokenText else "$textInput $spokenText"
                Toast.makeText(context, "Voice input appended!", Toast.LENGTH_SHORT).show()
            }
        }
    }
    var liveConversationalActive by remember { mutableStateOf(false) }
    val liveChatHistory = remember { mutableStateListOf<Pair<String, String>>() }
    var micMuted by remember { mutableStateOf(false) }

    val languages = listOf("English", "Tamil", "Hindi", "Spanish", "French", "German", "Japanese")
    var selectedLanguage by remember { mutableStateOf("English") }

    val filteredProfiles = remember(selectedLanguage) {
        voiceProfiles.filter { it.language == selectedLanguage }
    }

    var selectedProfile by remember { mutableStateOf(filteredProfiles.first()) }
    LaunchedEffect(selectedLanguage) {
        val matching = voiceProfiles.filter { it.language == selectedLanguage }
        if (matching.isNotEmpty()) {
            selectedProfile = matching.first()
        }
    }

    val emotions = listOf("Friendly", "Energetic", "Relaxed", "Whisper", "Dramatic / Deep", "Monotone News")
    var selectedEmotion by remember { mutableStateOf("Dramatic / Deep") }

    var selectedEngine by remember { mutableStateOf("CLOUD") } // "CLOUD" or "ON_DEVICE"
    var ttsPitch by remember { mutableStateOf(1.0f) }
    var ttsSpeechRate by remember { mutableStateOf(1.0f) }
    var isTtsSpeaking by remember { mutableStateOf(false) }

    // Built-in Audio Player States
    var isPlaying by remember { mutableStateOf(false) }
    var currentMillis by remember { mutableStateOf(0) }
    var durationMillis by remember { mutableStateOf(100) }
    var playPercent by remember { mutableStateOf(0f) }
    var playbackSpeed by remember { mutableStateOf(1.0f) }

    val mediaPlayer = remember { MediaPlayer() }

    // Resolve current local file path
    val audioPath = remember(activeItem) {
        val path = activeItem?.responseText.orEmpty()
        if (path.isNotBlank()) {
            path
        } else if (activeItem?.visualUrl?.startsWith("file://") == true) {
            activeItem?.visualUrl?.removePrefix("file://").orEmpty()
        } else {
            ""
        }
    }

    // Refresh player when a new audio is synthesized
    LaunchedEffect(audioPath) {
        if (audioPath.isNotBlank()) {
            try {
                mediaPlayer.reset()
                mediaPlayer.setDataSource(audioPath)
                mediaPlayer.prepare()
                durationMillis = mediaPlayer.duration.coerceAtLeast(100)
                currentMillis = 0
                playPercent = 0f
                isPlaying = false

                mediaPlayer.setOnCompletionListener {
                    isPlaying = false
                    currentMillis = durationMillis
                    playPercent = 1f
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Keep state flowing in sync when voice plays
    LaunchedEffect(isPlaying, audioPath) {
        if (isPlaying && audioPath.isNotBlank()) {
            while (isPlaying) {
                try {
                    currentMillis = mediaPlayer.currentPosition
                    playPercent = (currentMillis.toFloat() / durationMillis).coerceIn(0f, 1f)
                } catch (e: Exception) {
                    // avoid crash
                }
                delay(80)
            }
        }
    }

    // CleanMediaPlayer closure when navigating out
    DisposableEffect(Unit) {
        onDispose {
            try {
                mediaPlayer.stop()
                mediaPlayer.release()
            } catch (e: Exception) {
                // cleanup ignore
            }
        }
    }

    // Helpers to switch playback speed
    val updatePlaybackSpeed: (Float) -> Unit = { speed ->
        playbackSpeed = speed
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                mediaPlayer.playbackParams = mediaPlayer.playbackParams.setSpeed(speed)
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Playback speed modifier not supported on this container.", Toast.LENGTH_SHORT).show()
        }
    }

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
                    modifier = Modifier.testTag("voiceover_back_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Go Back",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
                Column {
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
            }

            // Gemini 3.1 Live Conversational AI Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (liveConversationalActive) 
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
                    else 
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                ),
                border = BorderStroke(
                    width = 1.dp,
                    color = if (liveConversationalActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(
                                imageVector = Icons.Default.VolumeUp,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Column {
                                Text(
                                    text = "Gemini Live Voice Space",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = (14 * scale).sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "gemini-3.1-flash-live-preview",
                                    fontSize = (10 * scale).sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            }
                        }

                        // Live Pulse Indicator
                        if (liveConversationalActive) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFFE8F5E9))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .background(Color(0xFF2E7D32), CircleShape)
                                    )
                                    Text("LIVE ROOM ACTIVE", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1B5E20))
                                }
                            }
                        }
                    }

                    if (!liveConversationalActive) {
                        Text(
                            text = "Engage in real-time, low-latency verbal voice dialogue with Gemini Live. Supports natural talking speed, interruption, and low-latency synthesis.",
                            fontSize = (12 * scale).sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )

                        Button(
                            onClick = {
                                liveConversationalActive = true
                                if (liveChatHistory.isEmpty()) {
                                    liveChatHistory.add("gemini" to "Hello! I am your real-time Gemini Live vocal companion. Tell me, what shall we create today?")
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Mic, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Enter Live Voice Space", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        // Pulsing Wave Animation
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(60.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                            contentAlignment = Alignment.Center
                        ) {
                            // Simple high-tech Canvas pulse animation
                            val infiniteTransition = rememberInfiniteTransition()
                            val pulseProgress by infiniteTransition.animateFloat(
                                initialValue = 0f,
                                targetValue = 2f * Math.PI.toFloat(),
                                animationSpec = infiniteRepeatable(
                                    animation = tween(1200, easing = LinearEasing),
                                    repeatMode = RepeatMode.Restart
                                )
                            )

                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val width = size.width
                                val height = size.height
                                val path = androidx.compose.ui.graphics.Path()
                                path.moveTo(0f, height / 2)
                                for (x in 0..width.toInt() step 5) {
                                    val y = height / 2 + sin(x * 0.03 + pulseProgress) * if (micMuted) 1f else 15f
                                    path.lineTo(x.toFloat(), y.toFloat())
                                }
                                drawPath(
                                    path = path,
                                    color = if (micMuted) Color.Gray else Color(0xFF6200EE),
                                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3.dp.toPx())
                                )
                            }

                            Text(
                                text = if (micMuted) "Microphone Muted" else "Listening for speech...",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                            )
                        }

                        // Terminal Dialog History Box
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 140.dp)
                                .background(Color.Black.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                                .padding(8.dp)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            liveChatHistory.forEach { (role, message) ->
                                val isUser = role == "user"
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isUser) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant)
                                            .padding(8.dp)
                                    ) {
                                        Text(
                                            text = "${if (isUser) "You" else "Gemini"}: $message",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }

                        // Preset speak tags
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(
                                "Who are you?",
                                "Tell me a space joke",
                                "Explain quantum computing in Tamil",
                                "What's the meaning of creative freedom?"
                            ).forEach { tag ->
                                SuggestionChip(
                                    onClick = {
                                        liveChatHistory.add("user" to tag)
                                        viewModel.triggerLiveVoiceChat(tag, liveChatHistory) { reply ->
                                            liveChatHistory.add("gemini" to reply)
                                            viewModel.speakTextWithTts(reply, voiceName = null, pitch = 1.0f, speechRate = 1.0f) {}
                                        }
                                    },
                                    label = { Text(tag, fontSize = 10.sp) }
                                )
                            }
                        }

                        // Interactive Controls row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { micMuted = !micMuted },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (micMuted) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = if (micMuted) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    imageVector = if (micMuted) Icons.Default.MicOff else Icons.Default.Mic,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (micMuted) "Unmute" else "Mute Mic", fontSize = 11.sp)
                            }

                            Button(
                                onClick = {
                                    liveConversationalActive = false
                                    viewModel.stopTtsSpeech()
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error,
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Exit Live Room", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }

            // Wrapped inputs with processing skeleton/spinner overlay
            Box(modifier = Modifier.fillMaxWidth()) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Engine Selector Segmented Control Tab Row
                    Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(4.dp)
                    .testTag("voiceover_engine_selector"),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf(
                    Pair("CLOUD", "Cloud Studio"),
                    Pair("ON_DEVICE", "On-Device Engine")
                ).forEach { (engineKey, label) ->
                    val isSelected = selectedEngine == engineKey
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                            .clickable { 
                                selectedEngine = engineKey 
                                if (engineKey == "CLOUD") {
                                    viewModel.stopTtsSpeech()
                                    isTtsSpeaking = false
                                } else {
                                    if (isPlaying) {
                                        try {
                                            mediaPlayer.pause()
                                        } catch (e: Exception) {}
                                        isPlaying = false
                                    }
                                }
                            }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold,
                            fontSize = (13 * scale).sp
                        )
                    }
                }
            }

            // ElevenLabs premium voiceover status badge/card
            val apiKey = com.example.BuildConfig.ELEVENLABS_API_KEY
            val hasElevenLabsKey = apiKey.isNotEmpty() && apiKey != "YOUR_ELEVENLABS_API_KEY"

            if (selectedEngine == "CLOUD") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (hasElevenLabsKey) 
                            Color(0xFFE8F5E9) 
                        else 
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(
                        width = 1.dp,
                        color = if (hasElevenLabsKey) Color(0xFFC8E6C9) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = if (hasElevenLabsKey) Icons.Default.AutoAwesome else Icons.Default.Info,
                            contentDescription = "ElevenLabs Status",
                            tint = if (hasElevenLabsKey) Color(0xFF2E7D32) else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Column {
                            Text(
                                text = if (hasElevenLabsKey) "ElevenLabs Premium AI Active" else "ElevenLabs Key Not Found",
                                fontWeight = FontWeight.Bold,
                                fontSize = (13 * scale).sp,
                                color = if (hasElevenLabsKey) Color(0xFF1B5E20) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = if (hasElevenLabsKey) 
                                    "Your voiceovers will be synthesized using premium natural AI voices." 
                                else 
                                    "Add ELEVENLABS_API_KEY in the Secrets panel for premium voices. Falling back to local TTS.",
                                fontSize = (11 * scale).sp,
                                color = if (hasElevenLabsKey) Color(0xFF2E7D32).copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
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
                shape = RoundedCornerShape(24.dp),
                trailingIcon = {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 4.dp)) {
                        IconButton(onClick = { filePickerLauncher.launch("*/*") }) {
                            Icon(Icons.Default.Add, contentDescription = "Add Attachment", tint = MaterialTheme.colorScheme.primary)
                        }
                        IconButton(onClick = {
                            try {
                                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                    putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak now...")
                                }
                                speechRecognizerLauncher.launch(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "Speech recognition not supported on this device", Toast.LENGTH_SHORT).show()
                            }
                        }) {
                            Icon(Icons.Default.Mic, contentDescription = "Speak text", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            )

            selectedMediaUriText?.let { uri ->
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
                        text = "Attached: $selectedMediaNameText",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    IconButton(onClick = { selectedMediaUriText = null; selectedMediaNameText = null }, modifier = Modifier.size(16.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Remove", modifier = Modifier.size(12.dp))
                    }
                }
            }

            // Interactive Clipboard Assist Row for TTS Input Text
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. Paste Button
                VoiceAssistActionButton(
                    label = "Paste",
                    icon = Icons.Default.Add,
                    onClick = {
                        try {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            if (clipboard.hasPrimaryClip()) {
                                val item = clipboard.primaryClip?.getItemAt(0)
                                val textToPaste = item?.text?.toString() ?: ""
                                if (textToPaste.isNotBlank()) {
                                    textInput = textToPaste
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

                // 2. Select All
                VoiceAssistActionButton(
                    label = "Select All",
                    icon = Icons.Default.Info,
                    onClick = {
                        if (textInput.isNotBlank()) {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = android.content.ClipData.newPlainText("All voice input content", textInput)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "Select All Action: Text entirely copied!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Input is empty.", Toast.LENGTH_SHORT).show()
                        }
                    },
                    scale = scale
                )

                // 3. Copy Button
                VoiceAssistActionButton(
                    label = "Copy",
                    icon = Icons.Default.Check,
                    onClick = {
                        if (textInput.isNotBlank()) {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = android.content.ClipData.newPlainText("Copied voice input", textInput)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "Copied voice input to clipboard!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Input is empty.", Toast.LENGTH_SHORT).show()
                        }
                    },
                    scale = scale
                )

                // 4. Cut Button
                VoiceAssistActionButton(
                    label = "Cut",
                    icon = Icons.Default.Edit,
                    onClick = {
                        if (textInput.isNotBlank()) {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = android.content.ClipData.newPlainText("Copied voice input", textInput)
                            clipboard.setPrimaryClip(clip)
                            textInput = ""
                            Toast.makeText(context, "Voice input cut to clipboard!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Input is empty.", Toast.LENGTH_SHORT).show()
                        }
                    },
                    scale = scale
                )

                // 5. Share Button
                VoiceAssistActionButton(
                    label = "Share",
                    icon = Icons.Default.Share,
                    onClick = {
                        if (textInput.isNotBlank()) {
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, textInput)
                            }
                            context.startActivity(Intent.createChooser(intent, "Share Voice Input"))
                        } else {
                            Toast.makeText(context, "Nothing to share.", Toast.LENGTH_SHORT).show()
                        }
                    },
                    scale = scale
                )

                // 6. Clear Button
                VoiceAssistActionButton(
                    label = "Clear",
                    icon = Icons.Default.Delete,
                    onClick = {
                        textInput = ""
                        Toast.makeText(context, "Cleared input field.", Toast.LENGTH_SHORT).show()
                    },
                    scale = scale,
                    isDanger = true
                )
            }

            if (selectedEngine == "ON_DEVICE") {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("on_device_tts_parameters_card")
                        .animateContentSize(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    ),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "On-Device Vocal Parameters",
                            fontWeight = FontWeight.Bold,
                            fontSize = (14 * scale).sp,
                            color = MaterialTheme.colorScheme.primary
                        )

                        // 1. Voice Pitch Slider
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Vocal Pitch (Tone)",
                                    fontSize = (12 * scale).sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = String.format("%.2fx", ttsPitch),
                                    fontSize = (12 * scale).sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Slider(
                                value = ttsPitch,
                                onValueChange = { ttsPitch = it },
                                valueRange = 0.5f..2.0f,
                                modifier = Modifier.testTag("tts_pitch_slider"),
                                colors = SliderDefaults.colors(
                                    thumbColor = MaterialTheme.colorScheme.primary,
                                    activeTrackColor = MaterialTheme.colorScheme.primary,
                                    inactiveTrackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f)
                                )
                            )
                        }

                        // 2. Speech Rate (Speed) Slider
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Speech Pace (Speed)",
                                    fontSize = (12 * scale).sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = String.format("%.2fx", ttsSpeechRate),
                                    fontSize = (12 * scale).sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Slider(
                                value = ttsSpeechRate,
                                onValueChange = { ttsSpeechRate = it },
                                valueRange = 0.5f..2.0f,
                                modifier = Modifier.testTag("tts_rate_slider"),
                                colors = SliderDefaults.colors(
                                    thumbColor = MaterialTheme.colorScheme.primary,
                                    activeTrackColor = MaterialTheme.colorScheme.primary,
                                    inactiveTrackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f)
                                )
                            )
                        }
                    }
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

            // Voice Profiles Selector
            Text(
                text = "Select Voice Profile",
                fontWeight = FontWeight.Bold,
                fontSize = (14 * scale).sp,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.align(Alignment.Start)
            )
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                filteredProfiles.forEach { profile ->
                    val isProfileSelected = selectedProfile.id == profile.id
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedProfile = profile }
                            .testTag("voice_profile_card_${profile.id}"),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isProfileSelected) 
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f) 
                            else 
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        ),
                        border = BorderStroke(
                            width = if (isProfileSelected) 2.dp else 1.dp,
                            color = if (isProfileSelected) 
                                MaterialTheme.colorScheme.primary 
                            else 
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f)
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        if (profile.gender == "FEMALE") 
                                            Color(0xFFFFEBF2) 
                                        else 
                                            Color(0xFFE3F2FD)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (profile.gender == "FEMALE") Icons.Default.Face else Icons.Default.Person,
                                    contentDescription = profile.gender,
                                    tint = if (profile.gender == "FEMALE") Color(0xFFD81B60) else Color(0xFF1565C0),
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = profile.name,
                                        fontSize = (13 * scale).sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(
                                                if (profile.gender == "FEMALE") 
                                                    Color(0xFFFCE4EC) 
                                                else 
                                                    Color(0xFFE3F2FD)
                                            )
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = profile.styleDesc,
                                            fontSize = (9 * scale).sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (profile.gender == "FEMALE") Color(0xFFC2185B) else Color(0xFF0D47A1)
                                        )
                                    }
                                }
                                
                                Text(
                                    text = profile.description,
                                    fontSize = (11 * scale).sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                    lineHeight = 15.sp
                                )
                            }
                            
                            RadioButton(
                                selected = isProfileSelected,
                                onClick = { selectedProfile = profile },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = MaterialTheme.colorScheme.primary
                                )
                            )
                        }
                    }
                }
            }
                }
                if (isGenerating) {
                    ToolInputProcessingOverlay(
                        message = "Synthesizing voice configurations...",
                        modifier = Modifier.matchParentSize()
                    )
                }
            }

            if (selectedEngine == "CLOUD") {
                // Speech Mood Presets
                Box(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
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
                    }
                    if (isGenerating) {
                        ToolInputProcessingOverlay(
                            message = "Synthesizing emotion presets...",
                            modifier = Modifier.matchParentSize()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Trigger Button
                Button(
                    onClick = {
                        if (textInput.isBlank()) {
                            Toast.makeText(context, "Please enter narration text.", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        val voiceIdToUse = if (selectedEngine == "CLOUD") selectedProfile.elevenLabsVoiceId else null
                        viewModel.triggerVoiceOver(
                            text = textInput,
                            voiceGender = selectedProfile.gender,
                            accent = "${selectedProfile.name} - ${selectedProfile.styleDesc} ($selectedLanguage, $selectedEmotion Accent)",
                            voiceId = voiceIdToUse
                        ) {
                            // Keep on current screen so user can preview right here!
                            Toast.makeText(context, "Voicing Synthesized! Preview below.", Toast.LENGTH_SHORT).show()
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
                                imageVector = Icons.Default.Mic,
                                contentDescription = "Voice generation",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Text(
                            "Synthesize Audio File",
                            fontSize = (14 * scale).sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }

            if (selectedEngine == "ON_DEVICE") {
                // On-Device speech synthesis actions
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Row for play and stop
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = {
                                if (textInput.isBlank()) {
                                    Toast.makeText(context, "Please enter narration text first.", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                if (isPlaying) {
                                    try {
                                        mediaPlayer.pause()
                                    } catch (e: Exception) {}
                                    isPlaying = false
                                }
                                isTtsSpeaking = true
                                viewModel.speakTextWithTts(
                                    text = textInput,
                                    voiceName = null, // uses selected locale and pitch/rate dynamically
                                    pitch = ttsPitch,
                                    speechRate = ttsSpeechRate,
                                    onDone = {
                                        isTtsSpeaking = false
                                        Toast.makeText(context, "Finished playing!", Toast.LENGTH_SHORT).show()
                                    }
                                )
                                Toast.makeText(context, "Speaking instantly on-device...", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("tts_speak_instantly_button")
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (isTtsSpeaking) Icons.Default.Refresh else Icons.Default.PlayArrow,
                                    contentDescription = "Speak Instantly",
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = if (isTtsSpeaking) "Speaking..." else "Speak Instantly",
                                    fontSize = (13 * scale).sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Button(
                            onClick = {
                                viewModel.stopTtsSpeech()
                                isTtsSpeaking = false
                                Toast.makeText(context, "Stopped speech.", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                contentColor = MaterialTheme.colorScheme.onErrorContainer
                            ),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("tts_stop_speech_button")
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Stop Speech",
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "Stop Speech",
                                    fontSize = (13 * scale).sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Button to save to Studio History
                    OutlinedButton(
                        onClick = {
                            if (textInput.isBlank()) {
                                Toast.makeText(context, "Please enter narration text.", Toast.LENGTH_SHORT).show()
                                return@OutlinedButton
                            }
                            viewModel.triggerVoiceOver(
                                text = textInput,
                                voiceGender = selectedProfile.gender,
                                accent = "On-Device Synthesizer (Pitch: ${String.format("%.2f", ttsPitch)}x, Speed: ${String.format("%.2f", ttsSpeechRate)}x, ${selectedProfile.name} profile)"
                            ) {
                                Toast.makeText(context, "Voiceover Saved to Studio History!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.primary
                        ),
                        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("tts_save_history_button")
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.AddCircle,
                                contentDescription = "Save to History",
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                "Save Local Synthesis to Studio History",
                                fontSize = (13 * scale).sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // BUILT-IN PREMIUM AUDIO PLAYER PREVIEW
            if (isGenerating) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    AudioPlayerSkeleton()
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.65f), RoundedCornerShape(16.dp))
                            .padding(16.dp)
                    ) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = generationStep.ifBlank { "Synthesizing audio..." },
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
            } else if (activeItem != null && activeItem?.type == "VOICEOVER") {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("voiceover_audio_player_card")
                        .animateContentSize(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Header info of player
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = "Voiceover",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Column {
                                    Text(
                                        text = activeItem?.title ?: "Synthesized Preview",
                                        fontSize = (14 * scale).sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "${activeItem?.style} • ${activeItem?.cameraAngle}",
                                        fontSize = (11 * scale).sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                    )
                                }
                            }

                            // Share Action Button
                            IconButton(
                                onClick = {
                                    try {
                                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(Intent.EXTRA_SUBJECT, activeItem?.title)
                                            putExtra(Intent.EXTRA_TEXT, "Look at this synthesized AI voiceover: \"${activeItem?.prompt}\"\n\nAnalyzed Tone Details: ${activeItem?.generatedDescription}")
                                        }
                                        context.startActivity(Intent.createChooser(shareIntent, "Share Voiceover Details"))
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Problem triggering sharing hub", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = "Share track",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        // REALISTIC SPEECH DANCING WAVEFORM VISUALIZER
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .background(Color.Black.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val barCount = 28
                            for (i in 0 until barCount) {
                                // Static representation or dynamic bouncing animation
                                val baseHeight = remember(i) { (12..40).random() }
                                val fluctuation = if (isPlaying) {
                                    sin((currentMillis / 80.0) + i * 0.4).toFloat() * 5f
                                } else {
                                    0f
                                }
                                val finalHeight = (baseHeight + fluctuation).coerceIn(6f, 44f)
                                val isActive = (i.toFloat() / barCount) <= playPercent
                                val barColor = if (isActive) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f)
                                }

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(finalHeight.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(barColor)
                                )
                            }
                        }

                        // Playback Tracker info
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val elapsedSec = currentMillis / 1000
                            val totalSec = durationMillis / 1000
                            Text(
                                text = String.format("%02d:%02d", elapsedSec / 60, elapsedSec % 60),
                                fontSize = (11 * scale).sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                            Text(
                                text = String.format("%02d:%02d", totalSec / 60, totalSec % 60),
                                fontSize = (11 * scale).sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }

                        // Precision seekbar slider
                        Slider(
                            value = playPercent,
                            onValueChange = { percent ->
                                playPercent = percent
                                val targetMs = (percent * durationMillis).toInt()
                                currentMillis = targetMs
                                try {
                                    mediaPlayer.seekTo(targetMs)
                                } catch (e: Exception) {
                                    // catch state crashes
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(16.dp)
                                .testTag("voiceover_audio_seekbar"),
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = MaterialTheme.colorScheme.primary,
                                inactiveTrackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f)
                            )
                        )

                        // Controlling Row (Play / Speed modifier)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Speeds chips selector
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val speeds = listOf(0.75f, 1.0f, 1.25f, 1.5f)
                                speeds.forEach { speed ->
                                    val isSelectedSpeed = playbackSpeed == speed
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(
                                                if (isSelectedSpeed) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent
                                            )
                                            .border(
                                                1.dp,
                                                if (isSelectedSpeed) MaterialTheme.colorScheme.primary else Color.Transparent,
                                                RoundedCornerShape(8.dp)
                                            )
                                            .clickable { updatePlaybackSpeed(speed) }
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = "${speed}x",
                                            fontSize = (10 * scale).sp,
                                            fontWeight = if (isSelectedSpeed) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelectedSpeed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                        )
                                    }
                                }
                            }

                            // Centered Play/Pause Button
                            Button(
                                onClick = {
                                    if (audioPath.isBlank()) {
                                        Toast.makeText(context, "Synthesis not complete yet.", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    try {
                                        viewModel.stopTtsSpeech()
                                        isTtsSpeaking = false
                                        if (isPlaying) {
                                            mediaPlayer.pause()
                                            isPlaying = false
                                        } else {
                                            mediaPlayer.start()
                                            isPlaying = true
                                        }
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Playback error or file corrupt.", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                                modifier = Modifier.testTag("voiceover_play_preview_button")
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (isPlaying) Icons.Default.Close else Icons.Default.PlayArrow,
                                        contentDescription = "Playback Action",
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = if (isPlaying) "Pause" else "Play",
                                        fontSize = (11 * scale).sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}

@Composable
fun VoiceAssistActionButton(
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
