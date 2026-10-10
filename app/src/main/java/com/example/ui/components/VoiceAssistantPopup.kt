package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.services.ComplexTaskEngine
import com.example.services.TaskStep
import com.example.services.TaskStepStatus
import com.example.services.VoiceCommandService
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.sin

@Composable
fun VoiceAssistantPopup(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    voiceService: VoiceCommandService,
    complexTaskEngine: ComplexTaskEngine
) {
    if (!isVisible) return

    val coroutineScope = rememberCoroutineScope()
    val isListening by voiceService.isListening.collectAsState()
    val speechRmsDb by voiceService.speechRmsDb.collectAsState()
    val lastSpokenText by voiceService.lastSpokenText.collectAsState()
    val currentExecution by complexTaskEngine.currentExecution.collectAsState()

    var manualTextInput by remember { mutableStateOf("") }

    // Predefined complex tasks for quick demonstration
    val sampleComplexTasks = listOf(
        "Turn on flashlight and volume to 75%",
        "Mute audio and open settings",
        "Scrape tech news and set volume to 50%",
        "Silent mode and open camera",
        "Generate image of cyber assistant and volume 90%"
    )

    // When popup becomes visible, auto-start speech recognizer to capture the command
    LaunchedEffect(isVisible) {
        voiceService.startListening()
    }

    // Rotating sweeping gradient animation for card border
    val infiniteTransition = rememberInfiniteTransition(label = "popupTransition")
    val borderRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "borderRotation"
    )

    Dialog(
        onDismissRequest = {
            voiceService.stopListening()
            onDismiss()
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.75f))
                .clickable {
                    voiceService.stopListening()
                    onDismiss()
                },
            contentAlignment = Alignment.BottomCenter
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .clickable(enabled = false) {} // Prevent click-through
                    .padding(horizontal = 14.dp, vertical = 18.dp)
                    .testTag("voice_assistant_popup_card"),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF071120)),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = if (isListening) {
                        Brush.sweepGradient(
                            listOf(
                                Color(0xFF00E5FF),
                                Color(0xFF0284C7),
                                Color(0xFFF97316),
                                Color(0xFFF59E0B),
                                Color(0xFF00E5FF)
                            )
                        )
                    } else {
                        Brush.horizontalGradient(
                            listOf(Color(0xFF0284C7), Color(0xFF1E293B))
                        )
                    }
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Header with Device Master DM insignia
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            DeviceMasterLogo(size = 32.dp)
                            Spacer(modifier = Modifier.width(10.dp))
                            DeviceMasterBrandText(fontSize = 17, textColor = Color.White)
                        }

                        IconButton(
                            onClick = {
                                voiceService.stopListening()
                                onDismiss()
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // ADVANCED VISUAL LISTENING ANIMATION (Acoustic Ripples + Waves + Equalizer + Glowing Mic Orb)
                    EnhancedVoiceListeningVisualizer(
                        isListening = isListening,
                        speechRmsDb = speechRmsDb,
                        onMicToggle = {
                            if (isListening) voiceService.stopListening() else voiceService.startListening()
                        }
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Assistant State & Live Spoken Phrase Box
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(DarkSurfaceElevated)
                            .border(
                                1.dp,
                                if (isListening) Color(0xFF00E5FF).copy(alpha = 0.5f) else Color(0xFF1E293B),
                                RoundedCornerShape(16.dp)
                            )
                            .padding(14.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    // Animated Pulsing Recording Indicator
                                    PulsingLiveDot(isListening = isListening)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (currentExecution?.isRunning == true) "Executing complex task..."
                                        else if (isListening) "Listening to your voice input..."
                                        else "Listening paused (tap mic to speak)",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isListening) Color(0xFF00E5FF) else TextSecondary
                                    )
                                }

                                // Animated Bouncing Equalizer Dots
                                if (isListening) {
                                    BouncingSoundDots()
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = if (lastSpokenText.isNotBlank()) "\"$lastSpokenText\"" else "Say your command (e.g. 'Turn on flashlight and volume to 75%')",
                                fontSize = 14.sp,
                                color = if (lastSpokenText.isNotBlank()) Color.White else TextMuted,
                                fontWeight = if (lastSpokenText.isNotBlank()) FontWeight.SemiBold else FontWeight.Normal
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Step-by-Step Complex Task Plan Execution Visualizer
                    if (currentExecution != null && currentExecution?.steps?.isNotEmpty() == true) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1E33))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "Executing Task Plan (${currentExecution?.steps?.size} Steps)",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF00B4D8)
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                currentExecution?.steps?.forEachIndexed { index, step ->
                                    TaskStepItem(index + 1, step)
                                    if (index < (currentExecution?.steps?.size ?: 0) - 1) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                    }
                                }

                                if (currentExecution?.completedSummary?.isNotBlank() == true) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = GoogleGreen.copy(alpha = 0.15f),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = "✓ ${currentExecution?.completedSummary}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = GoogleGreen,
                                            modifier = Modifier.padding(8.dp)
                                        )
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    // Quick-Pick Complex Tasks Row
                    Text(
                        text = "Or tap a sample task:",
                        fontSize = 11.sp,
                        color = TextSecondary,
                        modifier = Modifier.align(Alignment.Start).padding(bottom = 6.dp)
                    )

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(sampleComplexTasks) { sample ->
                            SuggestionChip(
                                onClick = {
                                    coroutineScope.launch {
                                        complexTaskEngine.planAndExecuteTask(sample)
                                    }
                                },
                                label = { Text(sample, fontSize = 11.sp, color = Color.White) },
                                colors = SuggestionChipDefaults.suggestionChipColors(
                                    containerColor = Color(0xFF132238),
                                    labelColor = Color.White
                                ),
                                border = SuggestionChipDefaults.suggestionChipBorder(
                                    enabled = true,
                                    borderColor = Color(0xFF0284C7).copy(alpha = 0.5f)
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Text Input Bar for manual complex task submission
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = manualTextInput,
                            onValueChange = { manualTextInput = it },
                            placeholder = { Text("Type any voice command or task...", fontSize = 12.sp, color = TextMuted) },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("popup_task_input"),
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = DarkSurfaceElevated,
                                unfocusedContainerColor = DarkSurfaceElevated,
                                focusedBorderColor = Color(0xFF00B4D8),
                                unfocusedBorderColor = DarkBorder
                            )
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        FilledIconButton(
                            onClick = {
                                if (manualTextInput.isNotBlank()) {
                                    val prompt = manualTextInput
                                    manualTextInput = ""
                                    coroutineScope.launch {
                                        complexTaskEngine.planAndExecuteTask(prompt)
                                    }
                                } else {
                                    if (isListening) voiceService.stopListening() else voiceService.startListening()
                                }
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("popup_execute_button"),
                            shape = CircleShape,
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = if (manualTextInput.isNotBlank()) Color(0xFFF97316) else Color(0xFF0284C7)
                            )
                        ) {
                            Icon(
                                imageVector = if (manualTextInput.isNotBlank()) Icons.Default.PlayArrow else if (isListening) Icons.Default.Stop else Icons.Default.Mic,
                                contentDescription = "Run",
                                tint = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Advanced Multi-Layer Visual Voice Listening Animation.
 * Combines:
 * 1. Concentric acoustic ripple waves radiating outward.
 * 2. Multi-frequency continuous sinusoidal waveforms.
 * 3. 19-bar audio spectrum equalizer reacting to real-time speech RMS dB.
 * 4. Central pulsing holographic microphone orb with rotating energy ring.
 * 5. Live audio decibel sensitivity meter.
 */
@Composable
fun EnhancedVoiceListeningVisualizer(
    isListening: Boolean,
    speechRmsDb: Float,
    onMicToggle: () -> Unit
) {
    val transition = rememberInfiniteTransition(label = "listeningVisualizer")

    // Continuous wave progression phase
    val wavePhase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 6.28318f, // 2 * PI
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wavePhase"
    )

    // Concentric acoustic wave ring 1
    val rippleScale1 by transition.animateFloat(
        initialValue = 0.9f,
        targetValue = 2.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rippleScale1"
    )
    val rippleAlpha1 by transition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rippleAlpha1"
    )

    // Concentric acoustic wave ring 2 (staggered phase)
    val rippleScale2 by transition.animateFloat(
        initialValue = 0.9f,
        targetValue = 2.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, delayMillis = 530, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rippleScale2"
    )
    val rippleAlpha2 by transition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, delayMillis = 530, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rippleAlpha2"
    )

    // Rotating orbital ring around center orb
    val orbitAngle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "orbitAngle"
    )

    // Dynamic RMS audio energy calculation
    val normalizedRms = if (isListening) {
        // speechRmsDb ranges typically from -2f to 10f in Android SpeechRecognizer
        val clamped = speechRmsDb.coerceIn(0f, 12f)
        (clamped / 12f).coerceIn(0.15f, 1f)
    } else {
        0.05f
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(130.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF030D1A),
                        Color(0xFF08192E),
                        Color(0xFF030D1A)
                    )
                )
            )
            .border(
                1.dp,
                if (isListening) Brush.horizontalGradient(
                    listOf(Color(0xFF00E5FF).copy(alpha = 0.4f), Color(0xFFF97316).copy(alpha = 0.4f))
                ) else Brush.horizontalGradient(
                    listOf(Color(0xFF1E293B), Color(0xFF1E293B))
                ),
                RoundedCornerShape(22.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        // LAYER 1: Full-width continuous Sinusoidal Waves and Spectrum Equalizer Bars
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val centerY = h * 0.5f

            if (isListening) {
                // Wave 1: Cyan High-Frequency Primary Voice Wave
                val path1 = Path()
                path1.moveTo(0f, centerY)
                for (x in 0..w.toInt() step 4) {
                    val xRatio = x / w
                    val envelope = sin((xRatio * Math.PI).toDouble()).toFloat() // Center peak envelope
                    val y = centerY + sin((wavePhase + xRatio * 14f).toDouble()).toFloat() * (22f * normalizedRms * envelope + 4f)
                    path1.lineTo(x.toFloat(), y)
                }
                drawPath(
                    path = path1,
                    color = Color(0xFF00E5FF).copy(alpha = 0.75f),
                    style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                )

                // Wave 2: Sky Blue Harmonic Wave (inverted phase)
                val path2 = Path()
                path2.moveTo(0f, centerY)
                for (x in 0..w.toInt() step 4) {
                    val xRatio = x / w
                    val envelope = sin((xRatio * Math.PI).toDouble()).toFloat()
                    val y = centerY - sin((wavePhase * 0.9f + xRatio * 10f).toDouble()).toFloat() * (18f * normalizedRms * envelope + 3f)
                    path2.lineTo(x.toFloat(), y)
                }
                drawPath(
                    path = path2,
                    color = Color(0xFF38BDF8).copy(alpha = 0.55f),
                    style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                )

                // Wave 3: Amber Warm Dynamic Accent Wave
                val path3 = Path()
                path3.moveTo(0f, centerY)
                for (x in 0..w.toInt() step 5) {
                    val xRatio = x / w
                    val envelope = sin((xRatio * Math.PI).toDouble()).toFloat()
                    val y = centerY + sin((wavePhase * 1.3f - xRatio * 16f).toDouble()).toFloat() * (14f * normalizedRms * envelope + 2f)
                    path3.lineTo(x.toFloat(), y)
                }
                drawPath(
                    path = path3,
                    color = Color(0xFFF97316).copy(alpha = 0.45f),
                    style = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round)
                )
            }

            // Dynamic 19-Bar Sound Equalizer across the bottom
            val barCount = 19
            val barWidth = 4.dp.toPx()
            val totalSpacing = w - (barCount * barWidth)
            val gap = totalSpacing / (barCount + 1)

            for (i in 0 until barCount) {
                val normalizedIndex = i / (barCount - 1f)
                val centerWeight = 1f - abs(normalizedIndex - 0.5f) * 1.5f
                val sineMod = sin((wavePhase + i * 0.42f).toDouble()).toFloat()

                val barHeight = if (isListening) {
                    (8f + (36f * normalizedRms + 14f * centerWeight * (0.5f + 0.5f * sineMod))).coerceIn(6f, 52f).dp.toPx()
                } else {
                    6.dp.toPx()
                }

                val barX = gap + i * (barWidth + gap)
                val barY = h - barHeight - 12.dp.toPx()

                val barColor = when {
                    i < 4 -> Color(0xFF00E5FF)
                    i < 9 -> Color(0xFF0284C7)
                    i < 14 -> Color(0xFF38BDF8)
                    else -> Color(0xFFF97316)
                }

                drawRoundRect(
                    color = if (isListening) barColor else Color(0xFF1E293B),
                    topLeft = Offset(barX, barY),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                )
            }
        }

        // LAYER 2: Concentric Acoustic Ripple Waves (Centered)
        if (isListening) {
            // Ripple 1
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .scale(rippleScale1)
                    .clip(CircleShape)
                    .background(Color(0xFF00E5FF).copy(alpha = rippleAlpha1))
            )
            // Ripple 2
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .scale(rippleScale2)
                    .clip(CircleShape)
                    .background(Color(0xFFF97316).copy(alpha = rippleAlpha2))
            )
        }

        // LAYER 3: Rotating Orbital Dashed Ring
        if (isListening) {
            Canvas(
                modifier = Modifier
                    .size(76.dp)
                    .rotate(orbitAngle)
            ) {
                drawCircle(
                    brush = Brush.sweepGradient(
                        listOf(
                            Color(0xFF00E5FF),
                            Color.Transparent,
                            Color(0xFFF97316),
                            Color.Transparent,
                            Color(0xFF00E5FF)
                        )
                    ),
                    style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                )
            }
        }

        // LAYER 4: Central Holographic Microphone Orb (Interactive: Tap to toggle)
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(
                            if (isListening) Color(0xFF00E5FF) else Color(0xFF0284C7),
                            Color(0xFF0C2A4A),
                            Color(0xFF04101F)
                        )
                    )
                )
                .border(
                    2.dp,
                    if (isListening) Color(0xFF00E5FF) else Color(0xFF38BDF8),
                    CircleShape
                )
                .clickable { onMicToggle() }
                .testTag("popup_visualizer_mic_orb"),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isListening) Icons.Default.Mic else Icons.Default.MicOff,
                contentDescription = "Voice Input Indicator",
                tint = Color.White,
                modifier = Modifier
                    .size(28.dp)
                    .scale(if (isListening) 1f + 0.15f * normalizedRms else 1f)
            )
        }

        // LAYER 5: Live Audio Decibel / Status Tag at Top of Visualizer
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 8.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0x900B1320))
                .border(0.8.dp, Color(0xFF00B4D8).copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                .padding(horizontal = 10.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(if (isListening) Color(0xFF00E5FF) else TextMuted)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = if (isListening) {
                    val dbValue = (speechRmsDb * 6).toInt().coerceIn(12, 94)
                    "VOICE INPUT ACTIVE • $dbValue dB"
                } else {
                    "VOICE INPUT PAUSED"
                },
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = if (isListening) Color(0xFF00E5FF) else TextMuted
            )
        }
    }
}

/**
 * Pulsing Live Recording Dot with smooth glowing beacon
 */
@Composable
private fun PulsingLiveDot(isListening: Boolean) {
    val transition = rememberInfiniteTransition(label = "liveDot")
    val scale by transition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "liveDotScale"
    )

    Box(
        modifier = Modifier
            .size(10.dp)
            .scale(if (isListening) scale else 1f)
            .clip(CircleShape)
            .background(if (isListening) Color(0xFF00E5FF) else TextMuted)
    )
}

/**
 * Three Bouncing Audio Dots indicating live speech transcription
 */
@Composable
private fun BouncingSoundDots() {
    val transition = rememberInfiniteTransition(label = "bouncingDots")

    val offset1 by transition.animateFloat(
        initialValue = 0f,
        targetValue = -6f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot1"
    )

    val offset2 by transition.animateFloat(
        initialValue = 0f,
        targetValue = -6f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, delayMillis = 130, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot2"
    )

    val offset3 by transition.animateFloat(
        initialValue = 0f,
        targetValue = -6f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, delayMillis = 260, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot3"
    )

    Row(
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .offset(y = offset1.dp)
                .size(4.5.dp)
                .clip(CircleShape)
                .background(Color(0xFF00E5FF))
        )
        Box(
            modifier = Modifier
                .offset(y = offset2.dp)
                .size(4.5.dp)
                .clip(CircleShape)
                .background(Color(0xFF38BDF8))
        )
        Box(
            modifier = Modifier
                .offset(y = offset3.dp)
                .size(4.5.dp)
                .clip(CircleShape)
                .background(Color(0xFFF97316))
        )
    }
}

@Composable
private fun TaskStepItem(stepNumber: Int, step: TaskStep) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(
                    when (step.status) {
                        TaskStepStatus.COMPLETED -> GoogleGreen
                        TaskStepStatus.RUNNING -> Color(0xFF00E5FF)
                        TaskStepStatus.FAILED -> GoogleRed
                        TaskStepStatus.PENDING -> DarkBorder
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            when (step.status) {
                TaskStepStatus.COMPLETED -> Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = Color.Black, modifier = Modifier.size(13.dp))
                TaskStepStatus.RUNNING -> CircularProgressIndicator(modifier = Modifier.size(13.dp), color = Color.Black, strokeWidth = 2.dp)
                TaskStepStatus.FAILED -> Icon(imageVector = Icons.Default.Close, contentDescription = null, tint = Color.White, modifier = Modifier.size(13.dp))
                TaskStepStatus.PENDING -> Text(text = "$stepNumber", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = step.description,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
            if (step.resultText.isNotBlank()) {
                Text(
                    text = step.resultText,
                    fontSize = 11.sp,
                    color = GoogleGreen
                )
            }
        }
    }
}
