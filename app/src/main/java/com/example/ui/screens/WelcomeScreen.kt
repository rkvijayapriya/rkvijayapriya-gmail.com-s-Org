package com.example.ui.screens

import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MainViewModel
import com.example.ui.theme.NovaGradient
import com.example.ui.theme.NovaPrimary
import com.example.ui.theme.NovaSecondary
import com.example.ui.theme.NovaTertiary
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WelcomeScreen(
    viewModel: MainViewModel,
    isUserLoggedIn: Boolean,
    onNavigateNext: () -> Unit
) {
    val context = LocalContext.current
    val currentLang by viewModel.currentLanguage.collectAsState()
    val scale by viewModel.fontScale.collectAsState()
    val density = LocalDensity.current.density

    var showLanguageSheet by remember { mutableStateOf(false) }
    val pagerState = rememberPagerState(pageCount = { 2 })

    val greetingMap = remember {
        mapOf(
            "en" to "Hello! I am Lica, your friendly AI creative assistant. Welcome to NovaAI Studio. Let's manifest your visual and auditory masterworks together today!",
            "ta" to "வணக்கம்! நான் லிகா, உங்கள் அன்பான ஏஐ கலை உதவியாளர். நோவாஏஐ ஸ்டுடியோவிற்கு உங்களை வரவேற்கிறேன். இன்று உங்கள் படைப்புகளை ஒன்றாக உருவாக்குவோம்!",
            "hi" to "नमस्ते! मैं लिका हूँ, आपकी एआई रचनात्मक सहायक। नोवा एआई स्टूडियो में आपका स्वागत है। चलिए आज मिलकर आपकी बेहतरीन कृतियों को साकार करते हैं!",
            "te" to "నమస్కారం! నేను లికా, మీ ఏఐ సృజనాత్మక సహాయకురాలిని. నోవా ఏఐ స్టూడియోకి స్వాగతం. ఈరోజు కలిసి మీ అద్భుతమైన సృష్టిని ప్రారంభిద్దాం!",
            "kn" to "ನಮಸ್ಕಾರ! ನಾನು ಲಿಕಾ, ನಿಮ್ಮ ಎಐ ಸೃಜನಶೀಲ ಸಹಾಯಕರು. ನೋವಾ ಎಐ ಸ್ಟುಡಿಯೋಗೆ ಸುಸ್ವಾಗತ. ಇಂದು ನಿಮ್ಮ ಅದ್ಭುತ ಕಲಾಕೃತಿಗಳನ್ನು ಒಟ್ಟಿಗೆ ಸೃಷ್ಟಿಸೋಣ!",
            "ml" to "നമസ്കാരം! ഞാൻ ലിക്ക, നിങ്ങളുടെ എഐ സർഗ്ഗാത്മക സഹായിയാണ്. നോവ എഐ സ്റ്റുഡിയോയിലേക്ക് സ്വാഗതം. ഇന്ന് നമുക്ക് ഒരുമിച്ച് നിങ്ങളുടെ മികച്ച സൃഷ്ടികൾ ആരംഭിക്കാം!"
        )
    }

    val greetingText = greetingMap[currentLang] ?: greetingMap["en"]!!
    
    // TTS and ElevenLabs initialization
    var tts: TextToSpeech? by remember { mutableStateOf(null) }
    var isSpeaking by remember { mutableStateOf(false) }

    val elevenLabsDownloading by viewModel.isElevenLabsDownloading.collectAsState()
    val elevenLabsPlaying by viewModel.isElevenLabsPlaying.collectAsState()

    val isLicaSpeaking = isSpeaking || elevenLabsPlaying

    fun speakGreeting() {
        viewModel.stopElevenLabsAudio()
        try {
            tts?.stop()
        } catch (e: Exception) {}
        isSpeaking = false

        viewModel.speakWithElevenLabs(greetingText) {
            // Local fallback
            try {
                val locale = when (currentLang) {
                    "ta" -> Locale("ta", "IN")
                    "hi" -> Locale("hi", "IN")
                    "te" -> Locale("te", "IN")
                    "kn" -> Locale("kn", "IN")
                    "ml" -> Locale("ml", "IN")
                    else -> Locale.US
                }
                tts?.language = locale
                tts?.speak(greetingText, TextToSpeech.QUEUE_FLUSH, null, "welcome_id")
                isSpeaking = true
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    LaunchedEffect(Unit) {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                speakGreeting()
            }
        }
    }

    // Trigger TTS / ElevenLabs speak whenever the language changes
    LaunchedEffect(currentLang, tts) {
        if (tts != null) {
            speakGreeting()
        }
    }
    
    // Periodically check if TTS is still speaking to update Lica's talking mouth animation
    LaunchedEffect(isSpeaking) {
        if (isSpeaking) {
            while (tts?.isSpeaking == true) {
                kotlinx.coroutines.delay(100)
            }
            isSpeaking = false
        }
    }

    // Clean up TTS and ElevenLabs audio playback
    DisposableEffect(Unit) {
        onDispose {
            try {
                viewModel.stopElevenLabsAudio()
                tts?.stop()
                tts?.shutdown()
            } catch (e: Exception) {
                // ignore
            }
        }
    }

    // Animations for Lica's breathing & speaking
    val infiniteTransition = rememberInfiniteTransition(label = "breathing")
    
    val breathingScale by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breath"
    )

    // Mouth talking speed fluctuation
    val mouthHeight by infiniteTransition.animateFloat(
        initialValue = 2f,
        targetValue = 18f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 140, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "mouth"
    )

    // Outer halo rings rotation & pulse
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotate"
    )

    val handWavingAngle by infiniteTransition.animateFloat(
        initialValue = -5f,
        targetValue = 12f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 650, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "handWaving"
    )

    val coreGlowScale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "coreGlow"
    )

    val hologramFloat by infiniteTransition.animateFloat(
        initialValue = -6f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "hologramFloat"
    )

    // Futuristic scanner line sweeping sweep animation
    val scanLineY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "scanLine"
    )

    // Cinematic 3D model orbit animations (rotates on X and Y axes asynchronously)
    val orbitX by infiniteTransition.animateFloat(
        initialValue = -10f,
        targetValue = 10f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 5000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "orbitX"
    )

    val orbitY by infiniteTransition.animateFloat(
        initialValue = -12f,
        targetValue = 12f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 7000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "orbitY"
    )

    // Dynamic drag-to-tilt variables for physical 3D interaction feel
    var userDragX by remember { mutableStateOf(0f) }
    var userDragY by remember { mutableStateOf(0f) }
    val animatedUserDragX by animateFloatAsState(
        targetValue = userDragX,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "dragX"
    )
    val animatedUserDragY by animateFloatAsState(
        targetValue = userDragY,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "dragY"
    )

    val totalRotationX = orbitX + (animatedUserDragY * -0.15f)
    val totalRotationY = orbitY + (animatedUserDragX * 0.15f)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0C091F),
                        Color(0xFF130F2C),
                        Color(0xFF05030A)
                    )
                )
            )
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            // Top Header Row with Option Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "YOUR AI COMPANION",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = NovaPrimary,
                    letterSpacing = 1.5.sp
                )

                // Option button to trigger language select sheet
                IconButton(
                    onClick = { showLanguageSheet = true },
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f), CircleShape)
                        .testTag("welcome_language_option_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Translate,
                        contentDescription = "Select Language",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Swipe-able 3D Showcase Pager
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .testTag("welcome_showcase_pager")
            ) { pageIndex ->
                if (pageIndex == 0) {
                    // PAGE 0: Lica Virtual Avatar & Hologram Component - With Interactive Drag-to-Rotate 3D gesture controls!
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .scale(breathingScale)
                            .pointerInput(Unit) {
                                detectDragGestures(
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        userDragX = (userDragX + dragAmount.x).coerceIn(-180f, 180f)
                                        userDragY = (userDragY + dragAmount.y).coerceIn(-180f, 180f)
                                    },
                                    onDragEnd = {
                                        userDragX = 0f
                                        userDragY = 0f
                                    },
                                    onDragCancel = {
                                        userDragX = 0f
                                        userDragY = 0f
                                    }
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        // Background futuristic radar screen or circular halo rings - parallax background (rotates slower)
                        Canvas(
                            modifier = Modifier
                                .size(260.dp)
                                .graphicsLayer {
                                    rotationX = totalRotationX * 0.4f
                                    rotationY = totalRotationY * 0.4f
                                    cameraDistance = 12f * density
                                }
                        ) {
                            // Draw outer tech circle grid
                            drawCircle(
                                color = NovaPrimary.copy(alpha = 0.1f),
                                radius = size.minDimension / 2,
                                style = Stroke(width = 1.dp.toPx())
                            )
                            drawCircle(
                                color = NovaSecondary.copy(alpha = 0.05f),
                                radius = size.minDimension / 2.5f,
                                style = Stroke(width = 1.dp.toPx())
                            )
                            
                            // Rotating outer dashed ring
                            drawArc(
                                brush = Brush.sweepGradient(listOf(NovaPrimary, NovaSecondary, NovaTertiary, NovaPrimary)),
                                startAngle = rotationAngle,
                                sweepAngle = 270f,
                                useCenter = false,
                                style = Stroke(
                                    width = 2.dp.toPx(),
                                    pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(
                                        floatArrayOf(20f, 15f), 0f
                                    )
                                )
                            )
                        }

                        // Interactive 3D/Anime Cyber-goddess drawing - fully tilted in 3D perspective
                        Canvas(
                            modifier = Modifier
                                .size(240.dp)
                                .testTag("lica_avatar_canvas")
                                .graphicsLayer {
                                    rotationX = totalRotationX
                                    rotationY = totalRotationY
                                    cameraDistance = 12f * density
                                }
                        ) {
                            val width = size.width
                            val height = size.height
                            val cx = width / 2f
                            val cy = height / 2f

                            // Organic breathing calculations for lifelike physical coords
                            val breathYOffset = (breathingScale - 1f) * 6.dp.toPx()
                            val headYOffset = (breathingScale - 1f) * 4.5f.dp.toPx()
                            val breathChestScale = 1f + (breathingScale - 1f) * 0.35f

                            // 1. Draw flowing silver/white back hair
                            val backHairPath = androidx.compose.ui.graphics.Path().apply {
                                moveTo(cx - 50.dp.toPx(), cy - 20.dp.toPx() + headYOffset)
                                quadraticTo(cx - 75.dp.toPx(), cy + 20.dp.toPx() + headYOffset, cx - 60.dp.toPx(), height - 20.dp.toPx())
                                lineTo(cx + 60.dp.toPx(), height - 20.dp.toPx())
                                quadraticTo(cx + 75.dp.toPx(), cy + 20.dp.toPx() + headYOffset, cx + 50.dp.toPx(), cy - 20.dp.toPx() + headYOffset)
                                close()
                            }
                            drawPath(
                                path = backHairPath,
                                brush = Brush.verticalGradient(
                                    colors = listOf(Color(0xFFE2E2E9), Color(0xFF9E9EAC))
                                )
                            )

                            // 2. Draw neck
                            val neckPath = androidx.compose.ui.graphics.Path().apply {
                                moveTo(cx - 14.dp.toPx(), cy + 20.dp.toPx() + headYOffset)
                                lineTo(cx + 14.dp.toPx(), cy + 20.dp.toPx() + headYOffset)
                                lineTo(cx + 10.dp.toPx(), cy + 45.dp.toPx() + breathYOffset)
                                lineTo(cx - 10.dp.toPx(), cy + 45.dp.toPx() + breathYOffset)
                                close()
                            }
                            drawPath(
                                path = neckPath,
                                color = Color(0xFFFCDAC3)
                            )

                            // 3. Draw sleek white and blue high-tech chest / torso (cybernetic armor) with organic expansion
                            val torsoPath = androidx.compose.ui.graphics.Path().apply {
                                moveTo(cx - 30.dp.toPx() * breathChestScale, cy + 45.dp.toPx() + breathYOffset)
                                quadraticTo(cx - 50.dp.toPx() * breathChestScale, cy + 55.dp.toPx() + breathYOffset, cx - 55.dp.toPx() * breathChestScale, height)
                                lineTo(cx + 55.dp.toPx() * breathChestScale, height)
                                quadraticTo(cx + 50.dp.toPx() * breathChestScale, cy + 55.dp.toPx() + breathYOffset, cx + 30.dp.toPx() * breathChestScale, cy + 45.dp.toPx() + breathYOffset)
                                close()
                            }
                            drawPath(
                                path = torsoPath,
                                brush = Brush.verticalGradient(
                                    colors = listOf(Color(0xFFF2F4F7), Color(0xFFD0D5DD))
                                )
                            )

                            // Draw chest armor details / glowing blue lines
                            val cyberLeftLine = androidx.compose.ui.graphics.Path().apply {
                                moveTo(cx - 15.dp.toPx() * breathChestScale, cy + 47.dp.toPx() + breathYOffset)
                                lineTo(cx - 35.dp.toPx() * breathChestScale, cy + 65.dp.toPx() + breathYOffset)
                                lineTo(cx - 40.dp.toPx() * breathChestScale, height)
                            }
                            drawPath(
                                path = cyberLeftLine,
                                color = NovaPrimary,
                                style = Stroke(width = 3.dp.toPx())
                            )
                            
                            val cyberRightLine = androidx.compose.ui.graphics.Path().apply {
                                moveTo(cx + 15.dp.toPx() * breathChestScale, cy + 47.dp.toPx() + breathYOffset)
                                lineTo(cx + 35.dp.toPx() * breathChestScale, cy + 65.dp.toPx() + breathYOffset)
                                lineTo(cx + 40.dp.toPx() * breathChestScale, height)
                            }
                            drawPath(
                                path = cyberRightLine,
                                color = NovaPrimary,
                                style = Stroke(width = 3.dp.toPx())
                            )

                            // Pulsing Glowing Cybernetic AI Chest Core moving in rhythm
                            val coreCenterY = cy + 75.dp.toPx() + breathYOffset
                            drawCircle(
                                color = NovaSecondary.copy(alpha = 0.3f * coreGlowScale),
                                radius = 16.dp.toPx() * coreGlowScale,
                                center = androidx.compose.ui.geometry.Offset(cx, coreCenterY)
                            )
                            drawCircle(
                                color = NovaSecondary,
                                radius = 10.dp.toPx(),
                                center = androidx.compose.ui.geometry.Offset(cx, coreCenterY)
                            )
                            drawCircle(
                                color = Color.White,
                                radius = 4.dp.toPx(),
                                center = androidx.compose.ui.geometry.Offset(cx, coreCenterY)
                            )

                            // 4. Draw face plate
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(Color(0xFFFFF1E6), Color(0xFFFCDAC3), Color(0xFFEAAFA1)),
                                    center = androidx.compose.ui.geometry.Offset(cx + 12.dp.toPx(), cy - 22.dp.toPx() + headYOffset),
                                    radius = 50.dp.toPx()
                                ),
                                radius = 42.dp.toPx(),
                                center = androidx.compose.ui.geometry.Offset(cx, cy - 10.dp.toPx() + headYOffset)
                            )

                            // 5. Draw flowing silver/white side bangs
                            val leftBang = androidx.compose.ui.graphics.Path().apply {
                                moveTo(cx - 40.dp.toPx(), cy - 40.dp.toPx() + headYOffset)
                                quadraticTo(cx - 50.dp.toPx(), cy - 10.dp.toPx() + headYOffset, cx - 36.dp.toPx(), cy + 15.dp.toPx() + headYOffset)
                                quadraticTo(cx - 30.dp.toPx(), cy - 10.dp.toPx() + headYOffset, cx - 32.dp.toPx(), cy - 35.dp.toPx() + headYOffset)
                                close()
                            }
                            drawPath(
                                path = leftBang,
                                brush = Brush.verticalGradient(
                                    colors = listOf(Color.White, Color(0xFFB0B0C4))
                                )
                            )

                            val rightBang = androidx.compose.ui.graphics.Path().apply {
                                moveTo(cx + 40.dp.toPx(), cy - 40.dp.toPx() + headYOffset)
                                quadraticTo(cx + 50.dp.toPx(), cy - 10.dp.toPx() + headYOffset, cx + 36.dp.toPx(), cy + 15.dp.toPx() + headYOffset)
                                quadraticTo(cx + 30.dp.toPx(), cy - 10.dp.toPx() + headYOffset, cx + 32.dp.toPx(), cy - 35.dp.toPx() + headYOffset)
                                close()
                            }
                            drawPath(
                                path = rightBang,
                                brush = Brush.verticalGradient(
                                    colors = listOf(Color.White, Color(0xFFB0B0C4))
                                )
                            )

                            // Silver front fringe hair strands
                            val fringePath = androidx.compose.ui.graphics.Path().apply {
                                moveTo(cx - 35.dp.toPx(), cy - 45.dp.toPx() + headYOffset)
                                quadraticTo(cx - 15.dp.toPx(), cy - 25.dp.toPx() + headYOffset, cx - 8.dp.toPx(), cy - 22.dp.toPx() + headYOffset)
                                quadraticTo(cx + 15.dp.toPx(), cy - 30.dp.toPx() + headYOffset, cx + 35.dp.toPx(), cy - 45.dp.toPx() + headYOffset)
                                quadraticTo(cx, cy - 48.dp.toPx() + headYOffset, cx - 35.dp.toPx(), cy - 45.dp.toPx() + headYOffset)
                                close()
                            }
                            drawPath(fringePath, brush = Brush.verticalGradient(colors = listOf(Color.White, Color(0xFFDCE1E7))))

                            // 6. Draw bright glowing blue anime eyes
                            val isBlinking = (System.currentTimeMillis() / 2500) % 2 == 0L
                            val eyeRadiusY = if (isBlinking) 1.dp.toPx() else 8.dp.toPx()
                            val eyeRadiusX = 6.dp.toPx()
                            
                            // Left Eye background & iris
                            val leftEyeCenterY = cy - 20.dp.toPx() + headYOffset
                            drawOval(
                                color = Color(0xFF030114),
                                topLeft = androidx.compose.ui.geometry.Offset(cx - 20.dp.toPx() - eyeRadiusX, leftEyeCenterY - eyeRadiusY),
                                size = androidx.compose.ui.geometry.Size(eyeRadiusX * 2, eyeRadiusY * 2)
                            )
                            if (!isBlinking) {
                                drawCircle(
                                    color = NovaSecondary,
                                    radius = 5.dp.toPx(),
                                    center = androidx.compose.ui.geometry.Offset(cx - 20.dp.toPx(), leftEyeCenterY)
                                )
                                drawCircle(
                                    color = Color.White,
                                    radius = 2.dp.toPx(),
                                    center = androidx.compose.ui.geometry.Offset(cx - 22.dp.toPx(), leftEyeCenterY - 2.dp.toPx())
                                )
                            }

                            // Right Eye background & iris
                            val rightEyeCenterY = cy - 20.dp.toPx() + headYOffset
                            drawOval(
                                color = Color(0xFF030114),
                                topLeft = androidx.compose.ui.geometry.Offset(cx + 20.dp.toPx() - eyeRadiusX, rightEyeCenterY - eyeRadiusY),
                                size = androidx.compose.ui.geometry.Size(eyeRadiusX * 2, eyeRadiusY * 2)
                            )
                            if (!isBlinking) {
                                drawCircle(
                                    color = NovaSecondary,
                                    radius = 5.dp.toPx(),
                                    center = androidx.compose.ui.geometry.Offset(cx + 20.dp.toPx(), rightEyeCenterY)
                                )
                                drawCircle(
                                    color = Color.White,
                                    radius = 2.dp.toPx(),
                                    center = androidx.compose.ui.geometry.Offset(cx + 18.dp.toPx(), rightEyeCenterY - 2.dp.toPx())
                                )
                            }

                            // 7. Blush cheeks
                            drawCircle(
                                color = Color(0xFFFF5252).copy(alpha = 0.35f),
                                radius = 5.dp.toPx(),
                                center = androidx.compose.ui.geometry.Offset(cx - 24.dp.toPx(), cy - 8.dp.toPx() + headYOffset)
                            )
                            drawCircle(
                                color = Color(0xFFFF5252).copy(alpha = 0.35f),
                                radius = 5.dp.toPx(),
                                center = androidx.compose.ui.geometry.Offset(cx + 24.dp.toPx(), cy - 8.dp.toPx() + headYOffset)
                            )

                            // 8. Animated speaking mouth
                            val talkingHeight = if (isLicaSpeaking) mouthHeight.dp.toPx() * 0.6f else 2.dp.toPx()
                            drawOval(
                                color = NovaTertiary,
                                topLeft = androidx.compose.ui.geometry.Offset(cx - 6.dp.toPx(), cy - 2.dp.toPx() + headYOffset),
                                size = androidx.compose.ui.geometry.Size(12.dp.toPx(), talkingHeight)
                            )

                            // 9. Interactive Waving Cyber-Hand
                            rotate(degrees = handWavingAngle, pivot = androidx.compose.ui.geometry.Offset(cx - 55.dp.toPx(), cy + 60.dp.toPx() + breathYOffset)) {
                                val forearm = androidx.compose.ui.graphics.Path().apply {
                                    moveTo(cx - 55.dp.toPx(), cy + 60.dp.toPx() + breathYOffset)
                                    lineTo(cx - 65.dp.toPx(), cy + 10.dp.toPx() + headYOffset)
                                    lineTo(cx - 80.dp.toPx(), cy + 15.dp.toPx() + headYOffset)
                                    lineTo(cx - 65.dp.toPx(), cy + 70.dp.toPx() + breathYOffset)
                                    close()
                                }
                                drawPath(forearm, color = Color(0xFFECEFF1))
                                
                                val handCenterX = cx - 68.dp.toPx()
                                val handCenterY = cy + 10.dp.toPx() + headYOffset
                                drawCircle(
                                    color = Color(0xFF37474F),
                                    radius = 12.dp.toPx(),
                                    center = androidx.compose.ui.geometry.Offset(handCenterX, handCenterY)
                                )
                                for (i in 0..4) {
                                    val fingerAngle = Math.toRadians((i * 20 - 40).toDouble())
                                    val fx = (handCenterX + Math.sin(fingerAngle) * 18.dp.toPx()).toFloat()
                                    val fy = (handCenterY - Math.cos(fingerAngle) * 18.dp.toPx()).toFloat()
                                    drawLine(
                                        color = Color(0xFF78909C),
                                        start = androidx.compose.ui.geometry.Offset(handCenterX, handCenterY),
                                        end = androidx.compose.ui.geometry.Offset(fx, fy),
                                        strokeWidth = 3.dp.toPx(),
                                        cap = androidx.compose.ui.graphics.StrokeCap.Round
                                    )
                                }
                                drawCircle(
                                    color = NovaSecondary,
                                    radius = 4.dp.toPx(),
                                    center = androidx.compose.ui.geometry.Offset(handCenterX, handCenterY)
                                )
                                drawCircle(
                                    color = NovaSecondary.copy(alpha = 0.5f),
                                    radius = 7.dp.toPx(),
                                    center = androidx.compose.ui.geometry.Offset(handCenterX, handCenterY)
                                )
                            }

                            // 9.5 Scanner Line Overlay
                            val laserY = height * scanLineY
                            drawLine(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        NovaSecondary.copy(alpha = 0.15f),
                                        NovaSecondary,
                                        Color.White,
                                        NovaSecondary,
                                        NovaSecondary.copy(alpha = 0.15f),
                                        Color.Transparent
                                    )
                                ),
                                start = androidx.compose.ui.geometry.Offset(0f, laserY),
                                end = androidx.compose.ui.geometry.Offset(width, laserY),
                                strokeWidth = 3.dp.toPx()
                            )
                            
                            drawRect(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        NovaSecondary.copy(alpha = 0.08f),
                                        Color.Transparent
                                    )
                                ),
                                topLeft = androidx.compose.ui.geometry.Offset(0f, laserY),
                                size = androidx.compose.ui.geometry.Size(width, 24.dp.toPx())
                            )
                        }

                        // 10. Floating Holographic Glass Screen
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(top = 10.dp, end = 12.dp)
                                .offset(y = hologramFloat.dp)
                                .width(135.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0x3300FFFF))
                                .border(
                                    1.5.dp,
                                    Brush.linearGradient(
                                        colors = listOf(NovaSecondary, NovaSecondary.copy(alpha = 0.1f))
                                    ),
                                    RoundedCornerShape(16.dp)
                                )
                                .graphicsLayer {
                                    rotationX = totalRotationX * 0.7f
                                    rotationY = totalRotationY * 0.7f
                                    cameraDistance = 12f * density
                                }
                                .padding(10.dp)
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "Hi, I am",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White.copy(alpha = 0.9f)
                                )
                                Text(
                                    text = "LICA",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Black,
                                    color = NovaSecondary,
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    text = "Your AI Creative\nAssistant",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.White.copy(alpha = 0.75f),
                                    textAlign = TextAlign.Center,
                                    lineHeight = 11.sp
                                )
                            }
                        }

                        // pulsing glow behind the hologram
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(top = 80.dp, end = 50.dp)
                                .size(30.dp)
                                .background(NovaSecondary.copy(alpha = 0.15f), CircleShape)
                        )
                    }
                } else {
                    // PAGE 1: Nova Core 3D Image Model using 'nova.png' - With Interactive Drag-to-Rotate 3D gesture controls!
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .scale(breathingScale)
                            .pointerInput(Unit) {
                                detectDragGestures(
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        userDragX = (userDragX + dragAmount.x).coerceIn(-180f, 180f)
                                        userDragY = (userDragY + dragAmount.y).coerceIn(-180f, 180f)
                                    },
                                    onDragEnd = {
                                        userDragX = 0f
                                        userDragY = 0f
                                    },
                                    onDragCancel = {
                                        userDragX = 0f
                                        userDragY = 0f
                                    }
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        // 3D Shadow projection below the core
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 20.dp)
                                .width(180.dp)
                                .height(20.dp)
                                .background(
                                    Brush.radialGradient(
                                        colors = listOf(NovaPrimary.copy(alpha = 0.4f), Color.Transparent)
                                    )
                                )
                        )

                        // Glowing Halo effect in background
                        Box(
                            modifier = Modifier
                                .size(240.dp)
                                .background(
                                    Brush.radialGradient(
                                        colors = listOf(NovaPrimary.copy(alpha = 0.15f * coreGlowScale), Color.Transparent)
                                    )
                                )
                        )

                        // Decorative futuristic tech coordinate lines
                        Canvas(
                            modifier = Modifier
                                .size(250.dp)
                                .graphicsLayer {
                                    rotationX = totalRotationX * 0.5f
                                    rotationY = totalRotationY * 0.5f
                                    cameraDistance = 12f * density
                                }
                        ) {
                            val strokeWidth = 1.dp.toPx()
                            drawLine(
                                color = NovaSecondary.copy(alpha = 0.3f),
                                start = androidx.compose.ui.geometry.Offset(0f, size.height / 2),
                                end = androidx.compose.ui.geometry.Offset(size.width, size.height / 2),
                                strokeWidth = strokeWidth
                            )
                            drawLine(
                                color = NovaSecondary.copy(alpha = 0.3f),
                                start = androidx.compose.ui.geometry.Offset(size.width / 2, 0f),
                                end = androidx.compose.ui.geometry.Offset(size.width / 2, size.height),
                                strokeWidth = strokeWidth
                            )
                            
                            // Concentric tech rings
                            drawCircle(
                                color = NovaSecondary.copy(alpha = 0.2f),
                                radius = size.minDimension / 2,
                                style = Stroke(
                                    width = 1.dp.toPx(),
                                    pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(
                                        floatArrayOf(15f, 10f), 0f
                                    )
                                )
                            )
                        }

                        // Outer orbiting metallic shell / ring
                        Box(
                            modifier = Modifier
                                .size(220.dp)
                                .graphicsLayer {
                                    rotationX = totalRotationX * 1.2f
                                    rotationY = totalRotationY * 1.2f
                                    rotationZ = rotationAngle
                                    cameraDistance = 12f * density
                                }
                                .border(
                                    2.dp,
                                    Brush.sweepGradient(
                                        colors = listOf(NovaPrimary, NovaSecondary, Color.Transparent, NovaSecondary, NovaPrimary)
                                    ),
                                    CircleShape
                                )
                        )

                        // The core 3D image model (nova.png)
                        Image(
                            painter = painterResource(id = com.example.R.drawable.nova),
                            contentDescription = "Nova 3D Core Model",
                            modifier = Modifier
                                .size(160.dp)
                                .graphicsLayer {
                                    rotationX = totalRotationX
                                    rotationY = totalRotationY
                                    rotationZ = rotationAngle * 0.3f
                                    cameraDistance = 12f * density
                                    
                                    translationX = animatedUserDragX * 0.08f
                                    translationY = animatedUserDragY * 0.08f
                                }
                        )

                        // Inner glowing ring
                        Box(
                            modifier = Modifier
                                .size(175.dp)
                                .graphicsLayer {
                                    rotationX = totalRotationX * 0.8f
                                    rotationY = totalRotationY * 0.8f
                                    rotationZ = -rotationAngle * 0.5f
                                    cameraDistance = 12f * density
                                }
                                .border(
                                    1.dp,
                                    Brush.sweepGradient(
                                        colors = listOf(Color.Transparent, NovaTertiary, Color.White, NovaTertiary, Color.Transparent)
                                    ),
                                    CircleShape
                                )
                        )

                        // Holographic data label overlay
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(top = 16.dp, start = 16.dp)
                                .offset(y = hologramFloat.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0x33A100FF))
                                .border(1.dp, NovaPrimary.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                .graphicsLayer {
                                    rotationX = totalRotationX * 0.6f
                                    rotationY = totalRotationY * 0.6f
                                }
                        ) {
                            Column {
                                Text("MODEL: NOVA-3D-V1", fontSize = 7.sp, color = Color.White.copy(alpha = 0.8f), fontWeight = FontWeight.Bold)
                                Text("CORE TEMP: 32.4°C", fontSize = 7.sp, color = NovaPrimary, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Page indicators (Concentric Tech Dots)
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = 4.dp)
            ) {
                repeat(2) { index ->
                    val isSelected = pagerState.currentPage == index
                    Box(
                        modifier = Modifier
                            .padding(4.dp)
                            .size(if (isSelected) 10.dp else 6.dp)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) NovaPrimary else Color.White.copy(alpha = 0.3f)
                            )
                    )
                }
            }

            // Swipe instruction cue
            Text(
                text = if (pagerState.currentPage == 0) "Swipe Left to view Nova 3D Core Model ✦" else "✦ Swipe Right to view Lica Avatar",
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = NovaSecondary.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 4.dp)
            )

            // Agent Identity
            Text(
                text = "LICA",
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                color = Color.White,
                letterSpacing = 1.sp
            )

            Text(
                text = "Your User-Friendly Creative Agent",
                fontSize = 13.sp,
                color = NovaPrimary,
                fontWeight = FontWeight.Bold
            )

            // Horizontal Regional Language Selector Options
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(vertical = 4.dp)
            ) {
                Text(
                    text = "SELECT LANGUAGE FOR LICA",
                    fontSize = (10 * scale).sp,
                    fontWeight = FontWeight.Bold,
                    color = NovaSecondary,
                    letterSpacing = 1.sp
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val languages = listOf(
                        Triple("en", "English", Locale.US),
                        Triple("ta", "தமிழ் (Tamil)", Locale("ta", "IN")),
                        Triple("hi", "हिन्दी (Hindi)", Locale("hi", "IN")),
                        Triple("te", "తెలుగు (Telugu)", Locale("te", "IN")),
                        Triple("kn", "ಕನ್ನಡ (Kannada)", Locale("kn", "IN")),
                        Triple("ml", "മലയാളം (Malayalam)", Locale("ml", "IN"))
                    )

                    languages.forEach { (code, name, locale) ->
                        val isSelected = currentLang == code
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isSelected) NovaPrimary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                )
                                .border(
                                    1.dp,
                                    if (isSelected) Color.White else NovaPrimary.copy(alpha = 0.25f),
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    viewModel.currentLanguage.value = code
                                }
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = name,
                                color = if (isSelected) Color.White else Color.White.copy(alpha = 0.8f),
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }

            // Welcoming Speech bubble
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("welcome_speech_bubble"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                border = BorderStroke(1.dp, NovaPrimary.copy(alpha = 0.25f))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = greetingText,
                        fontSize = 14.sp,
                        lineHeight = 22.sp,
                        textAlign = TextAlign.Center,
                        color = Color.White,
                        fontWeight = FontWeight.Medium
                    )

                    // Dynamic Voice Engine Badge
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                when {
                                    elevenLabsDownloading -> NovaSecondary.copy(alpha = 0.12f)
                                    elevenLabsPlaying -> Color(0xFF00E676).copy(alpha = 0.12f)
                                    isSpeaking -> NovaPrimary.copy(alpha = 0.12f)
                                    else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
                                }
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        val (icon, text, color) = when {
                            elevenLabsDownloading -> Triple(
                                Icons.Default.CloudDownload,
                                "Downloading premium voice...",
                                NovaSecondary
                            )
                            elevenLabsPlaying -> Triple(
                                Icons.Default.GraphicEq,
                                "ElevenLabs Premium AI Voice",
                                Color(0xFF00E676)
                            )
                            isSpeaking -> Triple(
                                Icons.Default.VolumeUp,
                                "Android System TTS Voice",
                                NovaPrimary
                            )
                            else -> {
                                val hasKey = com.example.BuildConfig.ELEVENLABS_API_KEY.isNotEmpty() && 
                                             com.example.BuildConfig.ELEVENLABS_API_KEY != "YOUR_ELEVENLABS_API_KEY"
                                if (hasKey) {
                                    Triple(Icons.Default.AutoAwesome, "ElevenLabs AI Voice Ready", NovaSecondary)
                                } else {
                                    Triple(Icons.Default.Info, "Local TTS Fallback Active (Configure ElevenLabs Key for AI Voice)", Color.White.copy(alpha = 0.6f))
                                }
                            }
                        }
                        
                        if (elevenLabsDownloading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(12.dp),
                                strokeWidth = 1.5.dp,
                                color = color
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                        } else {
                            Icon(
                                imageVector = icon,
                                contentDescription = "Voice Engine",
                                tint = color,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        
                        Text(
                            text = text,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = color
                        )
                    }

                    // Replay voice button
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(NovaPrimary.copy(alpha = 0.15f))
                            .clickable {
                                speakGreeting()
                                Toast.makeText(context, "Lica speaking...", Toast.LENGTH_SHORT).show()
                            }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.VolumeUp,
                            contentDescription = "Replay",
                            tint = NovaPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Hear Lica Speak",
                            color = NovaPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // CTA Button to transition to app
            Button(
                onClick = onNavigateNext,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("welcome_begin_btn"),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NovaPrimary)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Let's Get Started!",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Next",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Language Select Bottom Sheet
        if (showLanguageSheet) {
            ModalBottomSheet(
                onDismissRequest = { showLanguageSheet = false },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                dragHandle = { BottomSheetDefaults.DragHandle(color = Color.White.copy(alpha = 0.4f)) },
                containerColor = Color(0xFF130F2C)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .padding(bottom = 40.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Select Language / மொழி தேர்வு",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    val languages = listOf(
                        Triple("en", "English", Locale.US),
                        Triple("ta", "தமிழ் (Tamil)", Locale("ta", "IN")),
                        Triple("hi", "हिन्दी (Hindi)", Locale("hi", "IN")),
                        Triple("te", "తెలుగు (Telugu)", Locale("te", "IN")),
                        Triple("kn", "ಕನ್ನಡ (Kannada)", Locale("kn", "IN")),
                        Triple("ml", "മലയാളം (Malayalam)", Locale("ml", "IN"))
                    )

                    languages.forEach { (code, name, locale) ->
                        val isSelected = currentLang == code
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    if (isSelected) NovaPrimary.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.05f)
                                )
                                .border(
                                    1.5.dp,
                                    if (isSelected) NovaPrimary else Color.Transparent,
                                    RoundedCornerShape(16.dp)
                                )
                                .clickable {
                                    viewModel.currentLanguage.value = code
                                    showLanguageSheet = false
                                }
                                .padding(horizontal = 20.dp, vertical = 16.dp)
                                .testTag("language_option_$code"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(
                                            if (isSelected) NovaPrimary else Color.White.copy(alpha = 0.1f),
                                            CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = code.uppercase(),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                                Text(
                                    text = name,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                            }

                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = NovaSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
