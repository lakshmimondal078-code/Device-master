package com.example.ui.screens

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.models.CommandExecutionLog
import com.example.data.models.UserAccount
import com.example.services.ComplexTaskEngine
import com.example.services.HotwordEventHub
import com.example.services.PhoneController
import com.example.services.VoiceCommandService
import com.example.services.VoiceHotwordService
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun DashboardScreen(
    userAccount: UserAccount,
    voiceService: VoiceCommandService,
    phoneController: PhoneController,
    complexTaskEngine: ComplexTaskEngine,
    onTriggerHotwordPopup: () -> Unit,
    onNavigateToScraper: () -> Unit,
    onNavigateToAi: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val isListening by voiceService.isListening.collectAsState()
    val lastSpokenText by voiceService.lastSpokenText.collectAsState()
    val commandLogs by voiceService.commandLogs.collectAsState()
    val isBackgroundListening by HotwordEventHub.isBackgroundListening.collectAsState()

    var commandInput by remember { mutableStateOf("") }
    var mediaVolume by remember { mutableStateOf(phoneController.getMediaVolumePercent()) }
    var isFlashlightOn by remember { mutableStateOf(phoneController.isTorchActive()) }
    var ringerMode by remember { mutableStateOf(phoneController.getRingerModeString()) }

    // Pulsing animation for mic orb
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isListening) 1.2f else 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Simple Clean Top Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                com.example.ui.components.DeviceMasterLogo(size = 32.dp)
                Spacer(modifier = Modifier.width(10.dp))
                com.example.ui.components.DeviceMasterBrandText(fontSize = 19, textColor = Color.White)
            }

            // Simple Profile Avatar
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(GoogleBlue)
                    .clickable { onNavigateToSettings() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = userAccount.displayName.take(1).uppercase(),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Simple Background Status Banner
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isBackgroundListening) Color(0xFF132338) else DarkSurface
            ),
            border = CardDefaults.outlinedCardBorder().copy(
                brush = Brush.horizontalGradient(
                    listOf(
                        if (isBackgroundListening) GoogleGreen else DarkBorder,
                        if (isBackgroundListening) GoogleBlueLight else DarkBorder
                    )
                )
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (isBackgroundListening) GoogleGreen else TextMuted)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isBackgroundListening) "Listening for 'Device Master'" else "Background Mode Paused",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    Text(
                        text = if (isBackgroundListening) "Say 'Device Master' or 'Google' anytime" else "Turn on to use hands-free",
                        fontSize = 11.sp,
                        color = TextSecondary,
                        modifier = Modifier.padding(start = 16.dp, top = 2.dp)
                    )
                }

                Switch(
                    checked = isBackgroundListening,
                    onCheckedChange = { enable ->
                        if (enable) VoiceHotwordService.start(context) else VoiceHotwordService.stop(context)
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = GoogleGreen,
                        checkedTrackColor = GoogleGreen.copy(alpha = 0.4f)
                    ),
                    modifier = Modifier.testTag("background_switch")
                )
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Large Central Assistant Voice Orb
        Box(
            modifier = Modifier.size(160.dp),
            contentAlignment = Alignment.Center
        ) {
            // Ambient glow
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .scale(pulseScale)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(
                                if (isListening) GoogleBlueLight.copy(alpha = 0.35f) else GoogleBlue.copy(alpha = 0.12f),
                                Color.Transparent
                            )
                        )
                    )
            )

            // Center interactive microphone button
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            if (isListening)
                                listOf(GoogleBlueLight, Color(0xFF0284C7))
                            else
                                listOf(Color(0xFF1E3A8A), Color(0xFF0F172A))
                        )
                    )
                    .border(
                        width = 3.dp,
                        brush = Brush.linearGradient(
                            listOf(GoogleBlueLight, GoogleGreen)
                        ),
                        shape = CircleShape
                    )
                    .clickable {
                        onTriggerHotwordPopup()
                    }
                    .testTag("voice_orb_button"),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (isListening) {
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = "Voice Assistant",
                            tint = Color.White,
                            modifier = Modifier.size(46.dp)
                        )
                    } else {
                        com.example.ui.components.DeviceMasterLogo(size = 52.dp)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (isListening) "Listening..." else "Tap to Speak",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Say \"Device Master\" or tap above to give a command",
            fontSize = 13.sp,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Simple Text Command Bar
        OutlinedTextField(
            value = commandInput,
            onValueChange = { commandInput = it },
            placeholder = { Text("Ask anything or give a complex task...", fontSize = 13.sp, color = TextMuted) },
            trailingIcon = {
                IconButton(
                    onClick = {
                        if (commandInput.isNotBlank()) {
                            val text = commandInput
                            commandInput = ""
                            onTriggerHotwordPopup()
                            coroutineScope.launch {
                                complexTaskEngine.planAndExecuteTask(text)
                                mediaVolume = phoneController.getMediaVolumePercent()
                                isFlashlightOn = phoneController.isTorchActive()
                                ringerMode = phoneController.getRingerModeString()
                            }
                        }
                    },
                    modifier = Modifier.testTag("send_command")
                ) {
                    Icon(imageVector = Icons.Default.ArrowForward, contentDescription = "Run", tint = GoogleBlueLight)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("main_task_input"),
            shape = RoundedCornerShape(16.dp),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = DarkSurface,
                unfocusedContainerColor = DarkSurface,
                focusedBorderColor = GoogleBlueLight,
                unfocusedBorderColor = DarkBorder
            )
        )

        Spacer(modifier = Modifier.height(22.dp))

        // Simple Quick Controls Row (Volume, Flashlight, Sound Mode)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Flashlight Quick Tile
            Card(
                modifier = Modifier
                    .weight(1f)
                    .clickable {
                        phoneController.toggleTorch()
                        isFlashlightOn = phoneController.isTorchActive()
                    },
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isFlashlightOn) Color(0xFF2E2405) else DarkSurface
                ),
                border = if (isFlashlightOn) CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(GoogleYellow)) else CardDefaults.outlinedCardBorder()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.FlashlightOn,
                        contentDescription = null,
                        tint = if (isFlashlightOn) GoogleYellow else TextMuted,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text("Torch", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text(if (isFlashlightOn) "ON" else "OFF", fontSize = 10.sp, color = if (isFlashlightOn) GoogleYellow else TextMuted)
                    }
                }
            }

            // Sound Mode Quick Tile
            Card(
                modifier = Modifier
                    .weight(1f)
                    .clickable {
                        val next = when (ringerMode.lowercase()) {
                            "normal" -> "vibrate"
                            "vibrate" -> "silent"
                            else -> "normal"
                        }
                        phoneController.setRingerMode(next)
                        ringerMode = phoneController.getRingerModeString()
                    },
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = null,
                        tint = GoogleGreen,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text("Sound", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text(ringerMode, fontSize = 10.sp, color = GoogleGreen)
                    }
                }
            }

            // Camera Quick Tile
            Card(
                modifier = Modifier
                    .weight(1f)
                    .clickable { phoneController.launchAppByName("camera") },
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoCamera,
                        contentDescription = null,
                        tint = PrimaryIndigo,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text("Camera", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text("Open", fontSize = 10.sp, color = TextMuted)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Simple Volume Bar
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(imageVector = Icons.Default.VolumeUp, contentDescription = null, tint = GoogleBlueLight, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Slider(
                    value = mediaVolume.toFloat(),
                    onValueChange = {
                        mediaVolume = it.toInt()
                        phoneController.setMediaVolumePercent(mediaVolume)
                    },
                    valueRange = 0f..100f,
                    modifier = Modifier.weight(1f),
                    colors = SliderDefaults.colors(
                        thumbColor = GoogleBlueLight,
                        activeTrackColor = GoogleBlueLight,
                        inactiveTrackColor = DarkBorder
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "$mediaVolume%", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Simple Complex Tasks Suggestions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Try Complex Tasks",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = "1-Tap Multi-Action",
                fontSize = 11.sp,
                color = GoogleYellow
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        val simpleTasks = listOf(
            "Turn on flashlight and volume to 75%",
            "Mute audio and open settings",
            "Scrape tech news and volume to 50%",
            "Silent mode, flashlight off, and open camera",
            "Generate image of cyber city and volume 80%"
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(simpleTasks) { task ->
                SuggestionChip(
                    onClick = {
                        onTriggerHotwordPopup()
                        coroutineScope.launch {
                            complexTaskEngine.planAndExecuteTask(task)
                            mediaVolume = phoneController.getMediaVolumePercent()
                            isFlashlightOn = phoneController.isTorchActive()
                            ringerMode = phoneController.getRingerModeString()
                        }
                    },
                    label = { Text(task, fontSize = 11.sp, color = Color.White) },
                    colors = SuggestionChipDefaults.suggestionChipColors(
                        containerColor = DarkSurface,
                        labelColor = Color.White
                    ),
                    border = SuggestionChipDefaults.suggestionChipBorder(
                        enabled = true,
                        borderColor = DarkBorder
                    ),
                    shape = RoundedCornerShape(10.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(22.dp))

        // Simple Recent Activity List
        if (commandLogs.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent Activity",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "Offline Verified",
                    fontSize = 11.sp,
                    color = GoogleGreen
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            commandLogs.take(3).forEach { log ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = GoogleGreen,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = log.commandText,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = log.feedback,
                                fontSize = 11.sp,
                                color = TextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
