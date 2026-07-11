package com.example.ui.screens

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.NovaGradient
import com.example.ui.theme.NovaPrimary
import com.example.ui.theme.NovaSecondary
import kotlinx.coroutines.delay
import kotlin.math.sin

object AudioSynth {
    fun playWelcomeChime() {
        Thread {
            val sampleRate = 44100
            val numSamples = sampleRate * 3 // 3 seconds duration
            val generatedSnd = ByteArray(2 * numSamples)
            
            // Generate a beautiful chord: C (261.63 Hz), E (329.63 Hz), G (392.00 Hz), C2 (523.25 Hz)
            val freqs = doubleArrayOf(261.63, 329.63, 392.00, 523.25)
            for (i in 0 until numSamples) {
                val t = i.toDouble() / sampleRate
                // Combine sin waves with decay envelope
                val envelope = if (t < 0.2) t / 0.2 else (1.0 - (t - 0.2) / 2.8).coerceIn(0.0, 1.0)
                var sampleVal = 0.0
                for (freq in freqs) {
                    sampleVal += sin(2.0 * Math.PI * freq * t)
                }
                sampleVal = sampleVal / freqs.size * envelope * 28000.0
                val sample = sampleVal.toInt().toShort()
                // 16-bit PCM, little-endian format
                generatedSnd[2 * i] = (sample.toInt() and 0x00ff).toByte()
                generatedSnd[2 * i + 1] = ((sample.toInt() and 0xff00) ushr 8).toByte()
            }

            try {
                val audioTrack = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(generatedSnd.size)
                    .build()

                audioTrack.write(generatedSnd, 0, generatedSnd.size)
                audioTrack.play()
                Thread.sleep(3000)
                audioTrack.stop()
                audioTrack.release()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }
}

@Composable
fun SplashScreen(
    onFinished: () -> Unit
) {
    val context = LocalContext.current
    
    // Play music once on launch
    LaunchedEffect(Unit) {
        AudioSynth.playWelcomeChime()
        
        // 5 seconds total duration for the clockwise loading theme
        delay(5000)
        onFinished()
    }

    // Animation for Clockwise Loading (0 to 360 degrees in 5 seconds)
    val progressAnim = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        progressAnim.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 5000, easing = LinearEasing)
        )
    }

    // Interactive bouncing visual wave animations
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val waveScale1 by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "wave1"
    )
    val waveScale2 by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "wave2"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0C091F),
                        Color(0xFF161233),
                        Color(0xFF04020A)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(24.dp)
        ) {
            // Clockwise Loader and Brand Icon Container
            Box(
                modifier = Modifier
                    .size(200.dp),
                contentAlignment = Alignment.Center
            ) {
                // Background decorative aura
                Box(
                    modifier = Modifier
                        .size(160.dp)
                        .scale(waveScale1)
                        .alpha(0.12f)
                        .background(NovaGradient, CircleShape)
                )

                // Outer glowing circle
                Box(
                    modifier = Modifier
                        .size(140.dp)
                        .scale(waveScale2)
                        .alpha(0.2f)
                        .background(NovaSecondary.copy(alpha = 0.3f), CircleShape)
                )

                // 5-Second Clockwise Loading Canvas
                Canvas(
                    modifier = Modifier.size(170.dp)
                ) {
                    val strokeWidth = 5.dp.toPx()
                    // Background track track
                    drawCircle(
                        color = Color.White.copy(alpha = 0.08f),
                        radius = size.minDimension / 2 - strokeWidth,
                        style = Stroke(width = strokeWidth)
                    )

                    // Clockwise filling arc
                    drawArc(
                        brush = NovaGradient,
                        startAngle = -90f, // Starting from the very top (12 o'clock)
                        sweepAngle = progressAnim.value * 360f, // Sweep clockwise
                        useCenter = false,
                        style = Stroke(width = strokeWidth + 2.dp.toPx(), cap = StrokeCap.Round)
                    )
                }

                // Central brand icon
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(Color(0xFF1F1A3F), Color(0xFF0D0B1A))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Nova",
                        tint = NovaPrimary,
                        modifier = Modifier
                            .size(54.dp)
                            .scale(waveScale1)
                    )
                }
            }

            Spacer(modifier = Modifier.height(36.dp))

            // App Name & Creative Subtitle
            Text(
                text = "NovaAI Studio",
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                letterSpacing = 1.5.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Visual & Auditory Masterpiece Engine",
                fontSize = 13.sp,
                color = Color.White.copy(alpha = 0.6f),
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Animated Audio Waveform (Visual Music)
            Row(
                modifier = Modifier
                    .width(120.dp)
                    .height(30.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (i in 0 until 8) {
                    val waveHeight by infiniteTransition.animateFloat(
                        initialValue = 4f,
                        targetValue = 28f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(durationMillis = 300 + (i * 70), easing = FastOutSlowInEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "bar_$i"
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(waveHeight.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(NovaPrimary, NovaSecondary)
                                )
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Playing Splash Theme Chime...",
                fontSize = 11.sp,
                color = NovaPrimary.copy(alpha = 0.8f),
                fontWeight = FontWeight.Bold
            )
        }
    }
}
