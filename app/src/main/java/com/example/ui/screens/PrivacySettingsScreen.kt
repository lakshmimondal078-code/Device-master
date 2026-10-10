package com.example.ui.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.models.UserAccount
import com.example.services.GoogleAuthManager
import com.example.ui.theme.*

@Composable
fun PrivacySettingsScreen(
    userAccount: UserAccount,
    authManager: GoogleAuthManager,
    onSignOut: () -> Unit
) {
    var offlineOnlyMode by remember { mutableStateOf(true) }
    var voiceFeedbackEnabled by remember { mutableStateOf(true) }
    var hapticFeedbackEnabled by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 14.dp)
    ) {
        Text(
            text = "Privacy & Settings",
            fontSize = 22.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color.White
        )
        Text(
            text = "Google Account security & on-device control preferences",
            fontSize = 12.sp,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(18.dp))

        // Google Account Profile Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(GoogleBlue),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = userAccount.displayName.take(1).uppercase(),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = userAccount.displayName,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = userAccount.email,
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = GoogleGreen.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = userAccount.securityLevel,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = GoogleGreen,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                Divider(color = DarkBorder)
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = {
                        authManager.signOut()
                        onSignOut()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("sign_out_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = GoogleRed)
                ) {
                    Icon(imageVector = Icons.Default.ExitToApp, contentDescription = "Sign Out", modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Sign Out of Device Master")
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Privacy Guarantees Section
        Text(
            text = "Offline Privacy Architecture",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Strict Offline Mode", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                        Text("Forces voice commands to execute without any cloud network requests.", fontSize = 12.sp, color = TextSecondary)
                    }
                    Switch(
                        checked = offlineOnlyMode,
                        onCheckedChange = { offlineOnlyMode = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = GoogleGreen, checkedTrackColor = GoogleGreen.copy(alpha = 0.4f))
                    )
                }

                Divider(color = DarkBorder.copy(alpha = 0.5f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Spoken Audio Confirmation (TTS)", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                        Text("Read command execution results hands-free via Android Text-to-Speech.", fontSize = 12.sp, color = TextSecondary)
                    }
                    Switch(
                        checked = voiceFeedbackEnabled,
                        onCheckedChange = { voiceFeedbackEnabled = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = GoogleBlueLight, checkedTrackColor = GoogleBlueLight.copy(alpha = 0.4f))
                    )
                }

                Divider(color = DarkBorder.copy(alpha = 0.5f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Haptic Feedback Vibration", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                        Text("Vibrate device on volume, flashlight, and app triggers.", fontSize = 12.sp, color = TextSecondary)
                    }
                    Switch(
                        checked = hapticFeedbackEnabled,
                        onCheckedChange = { hapticFeedbackEnabled = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = PrimaryIndigo, checkedTrackColor = PrimaryIndigo.copy(alpha = 0.4f))
                    )
                }

                Divider(color = DarkBorder.copy(alpha = 0.5f))

                // Background 'Google' Service Toggle
                val context = androidx.compose.ui.platform.LocalContext.current
                val isBgListening by com.example.services.HotwordEventHub.isBackgroundListening.collectAsState()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Background Voice Hotword", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                        Text("Keep microphone service active in background to trigger popup when saying 'Device Master' or 'Google'.", fontSize = 12.sp, color = TextSecondary)
                    }
                    Switch(
                        checked = isBgListening,
                        onCheckedChange = { enable ->
                            if (enable) {
                                com.example.services.VoiceHotwordService.start(context)
                            } else {
                                com.example.services.VoiceHotwordService.stop(context)
                            }
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = GoogleGreen, checkedTrackColor = GoogleGreen.copy(alpha = 0.4f))
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Device Permissions Audit
        Text(
            text = "Device Permissions Audit",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                PermissionRow("Microphone (RECORD_AUDIO)", "Voice command recognition", true)
                PermissionRow("Audio Manager (MODIFY_AUDIO_SETTINGS)", "Volume controls", true)
                PermissionRow("Camera & Flashlight (CAMERA)", "Flashlight toggle", true)
                PermissionRow("Internet (INTERNET)", "Web scraper & AI endpoints", true)
                PermissionRow("Vibration (VIBRATE)", "Tactile haptic confirmation", true)
            }
        }

        Spacer(modifier = Modifier.height(30.dp))
    }
}

@Composable
private fun PermissionRow(name: String, purpose: String, granted: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(text = name, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
            Text(text = purpose, fontSize = 11.sp, color = TextSecondary)
        }
        Icon(
            imageVector = if (granted) Icons.Default.CheckCircle else Icons.Default.Cancel,
            contentDescription = null,
            tint = if (granted) GoogleGreen else GoogleRed,
            modifier = Modifier.size(18.dp)
        )
    }
}
