package com.example

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.services.AiAutomationService
import com.example.services.ComplexTaskEngine
import com.example.services.GoogleAuthManager
import com.example.services.HotwordEventHub
import com.example.services.PhoneController
import com.example.services.VoiceCommandService
import com.example.services.VoiceHotwordService
import com.example.services.WebScraperService
import com.example.ui.components.VoiceAssistantPopup
import com.example.ui.screens.AiAutomationScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.PrivacySettingsScreen
import com.example.ui.screens.WebScrapingScreen
import com.example.ui.theme.DarkBg
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DeviceMasterTheme
import com.example.ui.theme.GoogleBlueLight
import com.example.ui.theme.TextMuted
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var phoneController: PhoneController
    private lateinit var authManager: GoogleAuthManager
    private lateinit var scraperService: WebScraperService
    private lateinit var aiService: AiAutomationService
    private lateinit var voiceService: VoiceCommandService
    private lateinit var complexTaskEngine: ComplexTaskEngine

    private val showVoicePopup = MutableStateFlow(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        phoneController = PhoneController(this)
        authManager = GoogleAuthManager(this)
        scraperService = WebScraperService()
        aiService = AiAutomationService()

        voiceService = VoiceCommandService(
            context = this,
            phoneController = phoneController,
            onScrapeCommandTriggered = { query ->
                val template = scraperService.findTemplateByVoiceKeyword(query)
                if (template != null) {
                    kotlinx.coroutines.GlobalScope.launch {
                        scraperService.runScraperTemplate(template)
                    }
                }
            },
            onAiCommandTriggered = { prompt ->
                kotlinx.coroutines.GlobalScope.launch {
                    aiService.executeAutomationPrompt(prompt)
                }
            },
            onImageGenCommandTriggered = { prompt ->
                kotlinx.coroutines.GlobalScope.launch {
                    aiService.generateOrEditImage(prompt, model = "gemini-nano-banana-2.1")
                }
            },
            onHotwordTriggered = { spokenText ->
                showVoicePopup.value = true
                val clean = spokenText.replace(Regex("(?i)^(hey google|ok google|google|device master|master)\\s*"), "").trim()
                if (clean.isNotBlank()) {
                    kotlinx.coroutines.GlobalScope.launch {
                        complexTaskEngine.planAndExecuteTask(clean)
                    }
                }
            }
        )

        complexTaskEngine = ComplexTaskEngine(
            phoneController = phoneController,
            scraperService = scraperService,
            aiService = aiService,
            voiceCommandService = voiceService
        )

        if (intent?.getBooleanExtra("SHOW_VOICE_POPUP", false) == true) {
            showVoicePopup.value = true
        }

        setContent {
            DeviceMasterTheme(darkTheme = true) {
                MainAppContainer(
                    phoneController = phoneController,
                    authManager = authManager,
                    scraperService = scraperService,
                    aiService = aiService,
                    voiceService = voiceService,
                    complexTaskEngine = complexTaskEngine,
                    showVoicePopupFlow = showVoicePopup
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.getBooleanExtra("SHOW_VOICE_POPUP", false)) {
            showVoicePopup.value = true
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        voiceService.destroy()
    }
}

enum class ScreenDestination {
    DASHBOARD,
    SCRAPER,
    AI_STUDIO,
    SETTINGS
}

@Composable
fun MainAppContainer(
    phoneController: PhoneController,
    authManager: GoogleAuthManager,
    scraperService: WebScraperService,
    aiService: AiAutomationService,
    voiceService: VoiceCommandService,
    complexTaskEngine: ComplexTaskEngine,
    showVoicePopupFlow: MutableStateFlow<Boolean>
) {
    val currentUser by authManager.currentUser.collectAsState()
    var currentScreen by remember { mutableStateOf(ScreenDestination.DASHBOARD) }
    val showVoicePopup by showVoicePopupFlow.collectAsState()

    // Listen to background service hotword events
    LaunchedEffect(Unit) {
        HotwordEventHub.hotwordTriggered.collect { _ ->
            showVoicePopupFlow.value = true
        }
    }

    // Request necessary runtime permissions (RECORD_AUDIO, CAMERA, and POST_NOTIFICATIONS on Android 13+)
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        // Permissions granted
    }

    LaunchedEffect(Unit) {
        val permissions = mutableListOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CAMERA
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        permissionLauncher.launch(permissions.toTypedArray())
    }

    if (!currentUser.isLoggedIn) {
        LoginScreen(
            authManager = authManager,
            onLoginSuccess = {
                currentScreen = ScreenDestination.DASHBOARD
            }
        )
    } else {
        // Back navigation handling
        BackHandler(enabled = currentScreen != ScreenDestination.DASHBOARD) {
            currentScreen = ScreenDestination.DASHBOARD
        }

        Box(modifier = Modifier.fillMaxSize()) {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                containerColor = DarkBg,
                bottomBar = {
                    NavigationBar(
                        containerColor = DarkSurface,
                        contentColor = Color.White,
                        tonalElevation = 8.dp,
                        windowInsets = WindowInsets.navigationBars,
                        modifier = Modifier.testTag("main_bottom_nav")
                    ) {
                        NavigationBarItem(
                            selected = currentScreen == ScreenDestination.DASHBOARD,
                            onClick = { currentScreen = ScreenDestination.DASHBOARD },
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.Mic,
                                    contentDescription = "Assistant"
                                )
                            },
                            label = { Text("Assistant", fontSize = 11.sp) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color.White,
                                selectedTextColor = GoogleBlueLight,
                                indicatorColor = GoogleBlueLight.copy(alpha = 0.3f),
                                unselectedIconColor = TextMuted,
                                unselectedTextColor = TextMuted
                            )
                        )

                        NavigationBarItem(
                            selected = currentScreen == ScreenDestination.SCRAPER,
                            onClick = { currentScreen = ScreenDestination.SCRAPER },
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.Language,
                                    contentDescription = "Scraper"
                                )
                            },
                            label = { Text("Scraper", fontSize = 11.sp) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color.White,
                                selectedTextColor = GoogleBlueLight,
                                indicatorColor = GoogleBlueLight.copy(alpha = 0.3f),
                                unselectedIconColor = TextMuted,
                                unselectedTextColor = TextMuted
                            )
                        )

                        NavigationBarItem(
                            selected = currentScreen == ScreenDestination.AI_STUDIO,
                            onClick = { currentScreen = ScreenDestination.AI_STUDIO },
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = "AI & Images"
                                )
                            },
                            label = { Text("AI Studio", fontSize = 11.sp) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color.White,
                                selectedTextColor = GoogleBlueLight,
                                indicatorColor = GoogleBlueLight.copy(alpha = 0.3f),
                                unselectedIconColor = TextMuted,
                                unselectedTextColor = TextMuted
                            )
                        )

                        NavigationBarItem(
                            selected = currentScreen == ScreenDestination.SETTINGS,
                            onClick = { currentScreen = ScreenDestination.SETTINGS },
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = "Settings"
                                )
                            },
                            label = { Text("Settings", fontSize = 11.sp) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color.White,
                                selectedTextColor = GoogleBlueLight,
                                indicatorColor = GoogleBlueLight.copy(alpha = 0.3f),
                                unselectedIconColor = TextMuted,
                                unselectedTextColor = TextMuted
                            )
                        )
                    }
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    when (currentScreen) {
                        ScreenDestination.DASHBOARD -> {
                            DashboardScreen(
                                userAccount = currentUser,
                                voiceService = voiceService,
                                phoneController = phoneController,
                                complexTaskEngine = complexTaskEngine,
                                onTriggerHotwordPopup = {
                                    showVoicePopupFlow.value = true
                                },
                                onNavigateToScraper = { currentScreen = ScreenDestination.SCRAPER },
                                onNavigateToAi = { currentScreen = ScreenDestination.AI_STUDIO },
                                onNavigateToSettings = { currentScreen = ScreenDestination.SETTINGS }
                            )
                        }
                        ScreenDestination.SCRAPER -> {
                            WebScrapingScreen(
                                scraperService = scraperService,
                                voiceService = voiceService
                            )
                        }
                        ScreenDestination.AI_STUDIO -> {
                            AiAutomationScreen(
                                aiService = aiService,
                                voiceService = voiceService
                            )
                        }
                        ScreenDestination.SETTINGS -> {
                            PrivacySettingsScreen(
                                userAccount = currentUser,
                                authManager = authManager,
                                onSignOut = {
                                    currentScreen = ScreenDestination.DASHBOARD
                                }
                            )
                        }
                    }
                }
            }

            // Google Voice Assistant Pop-Up Modal when "Google" is spoken
            VoiceAssistantPopup(
                isVisible = showVoicePopup,
                onDismiss = {
                    showVoicePopupFlow.value = false
                    complexTaskEngine.clearCurrentExecution()
                },
                voiceService = voiceService,
                complexTaskEngine = complexTaskEngine
            )
        }
    }
}
