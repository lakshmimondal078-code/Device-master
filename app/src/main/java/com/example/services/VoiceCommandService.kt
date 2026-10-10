package com.example.services

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.util.Log
import com.example.data.models.CommandCategory
import com.example.data.models.CommandExecutionLog
import com.example.data.models.ParsedCommand
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class VoiceCommandService(
    private val context: Context,
    private val phoneController: PhoneController,
    private val onScrapeCommandTriggered: (String) -> Unit = {},
    private val onAiCommandTriggered: (String) -> Unit = {},
    private val onImageGenCommandTriggered: (String) -> Unit = {},
    private val onHotwordTriggered: (String) -> Unit = {}
) : TextToSpeech.OnInitListener {

    private var textToSpeech: TextToSpeech? = null
    private var speechRecognizer: SpeechRecognizer? = null
    private var isTtsReady = false

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _lastSpokenText = MutableStateFlow("")
    val lastSpokenText: StateFlow<String> = _lastSpokenText.asStateFlow()

    private val _commandLogs = MutableStateFlow<List<CommandExecutionLog>>(emptyList())
    val commandLogs: StateFlow<List<CommandExecutionLog>> = _commandLogs.asStateFlow()

    private val _speechRmsDb = MutableStateFlow(0f)
    val speechRmsDb: StateFlow<Float> = _speechRmsDb.asStateFlow()

    init {
        try {
            textToSpeech = TextToSpeech(context, this)
        } catch (e: Exception) {
            Log.e("VoiceCommandService", "TTS Init Error: ${e.message}")
        }
        initSpeechRecognizer()
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            textToSpeech?.language = Locale.US
            isTtsReady = true
        }
    }

    private fun initSpeechRecognizer() {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
            speechRecognizer?.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    _isListening.value = true
                }

                override fun onBeginningOfSpeech() {}

                override fun onRmsChanged(rmsdB: Float) {
                    _speechRmsDb.value = rmsdB
                }

                override fun onBufferReceived(buffer: ByteArray?) {}

                override fun onEndOfSpeech() {
                    _isListening.value = false
                }

                override fun onError(error: Int) {
                    _isListening.value = false
                    val errorDesc = getErrorText(error)
                    Log.w("VoiceCommandService", "Speech Error: $errorDesc")
                }

                override fun onResults(results: Bundle?) {
                    _isListening.value = false
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val bestMatch = matches?.firstOrNull() ?: return
                    val lower = bestMatch.lowercase().trim()
                    if (lower.contains("device master") || lower.startsWith("master") || lower == "google" || lower == "hey google" || lower.startsWith("google ")) {
                        onHotwordTriggered(bestMatch)
                    } else {
                        processVoiceCommand(bestMatch)
                    }
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    val partialMatches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val partial = partialMatches?.firstOrNull() ?: ""
                    _lastSpokenText.value = partial
                    val lower = partial.lowercase().trim()
                    if (lower.contains("device master") || lower.startsWith("master") || lower == "google" || lower == "hey google") {
                        onHotwordTriggered(partial)
                    }
                }

                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
        }
    }

    fun startListening() {
        if (speechRecognizer == null) {
            initSpeechRecognizer()
        }
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.US)
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
        }
        try {
            speechRecognizer?.startListening(intent)
            _isListening.value = true
        } catch (e: Exception) {
            Log.e("VoiceCommandService", "Start listening error: ${e.message}")
            _isListening.value = false
        }
    }

    fun stopListening() {
        try {
            speechRecognizer?.stopListening()
            _isListening.value = false
        } catch (e: Exception) {
            // Ignore
        }
    }

    // Process a voice command string (supports both real voice & test input)
    fun processVoiceCommand(commandText: String): CommandExecutionLog {
        _lastSpokenText.value = commandText
        val parsed = parseOfflineCommand(commandText)
        val resultFeedback = executeParsedCommand(parsed)

        // Text-to-speech audio feedback
        speakFeedback(resultFeedback)

        val log = CommandExecutionLog(
            commandText = commandText,
            category = parsed.category,
            success = true,
            feedback = resultFeedback,
            executedOffline = parsed.isHandledOffline
        )

        _commandLogs.value = listOf(log) + _commandLogs.value.take(49)
        return log
    }

    fun parseOfflineCommand(text: String): ParsedCommand {
        val query = text.trim().lowercase()

        // 1. Volume commands
        if (query.contains("volume") || query.contains("sound") || query.contains("audio") || query.contains("louder") || query.contains("quieter")) {
            val percentRegex = Regex("(\\d+)\\s*(%|percent)")
            val percentMatch = percentRegex.find(query)
            if (percentMatch != null) {
                val value = percentMatch.groupValues[1].toIntOrNull() ?: 50
                return ParsedCommand(
                    rawText = text,
                    category = CommandCategory.VOLUME,
                    action = "SET_PERCENT",
                    parameterValue = value,
                    feedbackMessage = "Setting volume to $value percent"
                )
            }
            if (query.contains("up") || query.contains("increase") || query.contains("raise") || query.contains("louder")) {
                return ParsedCommand(
                    rawText = text,
                    category = CommandCategory.VOLUME,
                    action = "INCREASE",
                    feedbackMessage = "Increasing media volume"
                )
            }
            if (query.contains("down") || query.contains("decrease") || query.contains("lower") || query.contains("reduce") || query.contains("quieter")) {
                return ParsedCommand(
                    rawText = text,
                    category = CommandCategory.VOLUME,
                    action = "DECREASE",
                    feedbackMessage = "Decreasing media volume"
                )
            }
            if (query.contains("mute") || query.contains("silence") || query.contains("zero")) {
                return ParsedCommand(
                    rawText = text,
                    category = CommandCategory.VOLUME,
                    action = "MUTE",
                    feedbackMessage = "Muting media volume"
                )
            }
            if (query.contains("max") || query.contains("full") || query.contains("100")) {
                return ParsedCommand(
                    rawText = text,
                    category = CommandCategory.VOLUME,
                    action = "MAX",
                    feedbackMessage = "Setting volume to maximum"
                )
            }
        }

        // 2. Flashlight commands
        if (query.contains("flashlight") || query.contains("torch") || query.contains("light")) {
            if (query.contains("on") || query.contains("enable") || query.contains("start")) {
                return ParsedCommand(
                    rawText = text,
                    category = CommandCategory.FLASHLIGHT,
                    action = "ON",
                    feedbackMessage = "Turning on flashlight"
                )
            }
            if (query.contains("off") || query.contains("disable") || query.contains("stop")) {
                return ParsedCommand(
                    rawText = text,
                    category = CommandCategory.FLASHLIGHT,
                    action = "OFF",
                    feedbackMessage = "Turning off flashlight"
                )
            }
            return ParsedCommand(
                rawText = text,
                category = CommandCategory.FLASHLIGHT,
                action = "TOGGLE",
                feedbackMessage = "Toggling flashlight"
            )
        }

        // 3. Sound / Ringer Mode
        if (query.contains("silent mode") || query.contains("silent")) {
            return ParsedCommand(
                rawText = text,
                category = CommandCategory.DEVICE_SETTINGS,
                action = "RINGER_MODE",
                target = "silent",
                feedbackMessage = "Activating silent mode"
            )
        }
        if (query.contains("vibrate mode") || query.contains("vibration")) {
            return ParsedCommand(
                rawText = text,
                category = CommandCategory.DEVICE_SETTINGS,
                action = "RINGER_MODE",
                target = "vibrate",
                feedbackMessage = "Activating vibrate mode"
            )
        }
        if (query.contains("normal sound") || query.contains("sound mode") || query.contains("unmute phone")) {
            return ParsedCommand(
                rawText = text,
                category = CommandCategory.DEVICE_SETTINGS,
                action = "RINGER_MODE",
                target = "normal",
                feedbackMessage = "Restoring normal sound mode"
            )
        }

        // 4. Device Settings Shortcuts
        if (query.contains("wifi") || query.contains("wi-fi")) {
            return ParsedCommand(
                rawText = text,
                category = CommandCategory.DEVICE_SETTINGS,
                action = "OPEN_SETTINGS",
                target = "wifi",
                feedbackMessage = "Opening Wi-Fi settings"
            )
        }
        if (query.contains("bluetooth")) {
            return ParsedCommand(
                rawText = text,
                category = CommandCategory.DEVICE_SETTINGS,
                action = "OPEN_SETTINGS",
                target = "bluetooth",
                feedbackMessage = "Opening Bluetooth settings"
            )
        }
        if (query.contains("battery") || query.contains("power saver")) {
            return ParsedCommand(
                rawText = text,
                category = CommandCategory.DEVICE_SETTINGS,
                action = "OPEN_SETTINGS",
                target = "battery",
                feedbackMessage = "Opening Battery Saver settings"
            )
        }
        if (query.contains("display") || query.contains("brightness")) {
            return ParsedCommand(
                rawText = text,
                category = CommandCategory.DEVICE_SETTINGS,
                action = "OPEN_SETTINGS",
                target = "display",
                feedbackMessage = "Opening Display and Brightness settings"
            )
        }

        // 5. Image Creation & Editing via prompt (gemini-nano-banana-2.1)
        if (query.startsWith("generate image") || query.startsWith("create image") || query.startsWith("draw") || query.contains("edit image")) {
            val promptTarget = query.replace("generate image", "")
                .replace("create image", "")
                .replace("draw", "")
                .replace("edit image", "")
                .trim()
            return ParsedCommand(
                rawText = text,
                category = CommandCategory.AI_AUTOMATION,
                action = "GENERATE_IMAGE",
                target = promptTarget.ifEmpty { "futuristic robotic control center" },
                feedbackMessage = "Generating image using gemini-nano-banana-2.1",
                isHandledOffline = false
            )
        }

        // 6. Web Scraping triggers
        if (query.contains("scrape") || query.contains("extract") || query.contains("fetch web")) {
            return ParsedCommand(
                rawText = text,
                category = CommandCategory.WEB_SCRAPER,
                action = "SCRAPE_TRIGGER",
                target = text,
                feedbackMessage = "Triggering web extraction template"
            )
        }

        // 7. General AI query
        if (query.startsWith("ask gemini") || query.startsWith("ask ai") || query.startsWith("ai ") || query.startsWith("gemini ")) {
            val aiPrompt = text.replace(Regex("^(ask gemini|ask ai|ai|gemini)\\s*", RegexOption.IGNORE_CASE), "")
            return ParsedCommand(
                rawText = text,
                category = CommandCategory.AI_AUTOMATION,
                action = "AI_QUERY",
                target = aiPrompt,
                feedbackMessage = "Connecting to AI model for real-time processing",
                isHandledOffline = false
            )
        }

        // 8. App Launching (e.g. "open camera", "launch youtube", "open maps")
        if (query.startsWith("open ") || query.startsWith("launch ") || query.startsWith("start ")) {
            val appTarget = query.replace("open ", "").replace("launch ", "").replace("start ", "").trim()
            return ParsedCommand(
                rawText = text,
                category = CommandCategory.APP_LAUNCH,
                action = "LAUNCH_APP",
                target = appTarget,
                feedbackMessage = "Opening $appTarget"
            )
        }

        // Fallback: try app launch or generic system query
        return ParsedCommand(
            rawText = text,
            category = CommandCategory.APP_LAUNCH,
            action = "LAUNCH_APP",
            target = query,
            feedbackMessage = "Attempting to locate action for '$text'"
        )
    }

    private fun executeParsedCommand(command: ParsedCommand): String {
        return when (command.category) {
            CommandCategory.VOLUME -> {
                when (command.action) {
                    "SET_PERCENT" -> phoneController.setMediaVolumePercent(command.parameterValue ?: 50)
                    "INCREASE" -> phoneController.raiseMediaVolume()
                    "DECREASE" -> phoneController.lowerMediaVolume()
                    "MUTE" -> phoneController.muteMediaVolume()
                    "MAX" -> phoneController.maxMediaVolume()
                    else -> "Volume adjusted"
                }
            }
            CommandCategory.FLASHLIGHT -> {
                when (command.action) {
                    "ON" -> phoneController.setTorch(true)
                    "OFF" -> phoneController.setTorch(false)
                    else -> phoneController.toggleTorch()
                }
            }
            CommandCategory.DEVICE_SETTINGS -> {
                when (command.action) {
                    "RINGER_MODE" -> phoneController.setRingerMode(command.target ?: "normal")
                    "OPEN_SETTINGS" -> phoneController.openSettingsScreen(command.target ?: "settings")
                    else -> phoneController.openSettingsScreen("settings")
                }
            }
            CommandCategory.APP_LAUNCH -> {
                val appTarget = command.target ?: "camera"
                val (success, message) = phoneController.launchAppByName(appTarget)
                message
            }
            CommandCategory.WEB_SCRAPER -> {
                onScrapeCommandTriggered(command.target ?: command.rawText)
                "Executing web scraping template for '${command.target ?: command.rawText}'"
            }
            CommandCategory.AI_AUTOMATION -> {
                if (command.action == "GENERATE_IMAGE") {
                    onImageGenCommandTriggered(command.target ?: "")
                    "Generating image for prompt '${command.target}'"
                } else {
                    onAiCommandTriggered(command.target ?: command.rawText)
                    "Sending prompt to AI model: '${command.target}'"
                }
            }
            CommandCategory.SYSTEM_INFO -> {
                "Battery and system status active"
            }
        }
    }

    fun speakFeedback(message: String) {
        if (isTtsReady) {
            try {
                textToSpeech?.speak(message, TextToSpeech.QUEUE_FLUSH, null, "UTTERANCE_ID")
            } catch (e: Exception) {
                Log.e("VoiceCommandService", "TTS speak failed: ${e.message}")
            }
        }
    }

    fun destroy() {
        try {
            textToSpeech?.stop()
            textToSpeech?.shutdown()
            speechRecognizer?.destroy()
        } catch (e: Exception) {
            // Ignore
        }
    }

    private fun getErrorText(errorCode: Int): String {
        return when (errorCode) {
            SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
            SpeechRecognizer.ERROR_CLIENT -> "Client side error"
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
            SpeechRecognizer.ERROR_NETWORK -> "Network error"
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
            SpeechRecognizer.ERROR_NO_MATCH -> "No speech match"
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognition service busy"
            SpeechRecognizer.ERROR_SERVER -> "Server error"
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input"
            else -> "Speech recognizer error: $errorCode"
        }
    }
}
