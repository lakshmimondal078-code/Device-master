package com.example.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale

object HotwordEventHub {
    private val _hotwordTriggered = MutableSharedFlow<String>(extraBufferCapacity = 5)
    val hotwordTriggered: SharedFlow<String> = _hotwordTriggered.asSharedFlow()

    private val _isBackgroundListening = MutableStateFlow(false)
    val isBackgroundListening: StateFlow<Boolean> = _isBackgroundListening.asStateFlow()

    fun emitHotwordDetected(phrase: String = "Google") {
        _hotwordTriggered.tryEmit(phrase)
    }

    fun setBackgroundListening(active: Boolean) {
        _isBackgroundListening.value = active
    }
}

class VoiceHotwordService : Service() {
    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private var speechRecognizer: SpeechRecognizer? = null
    private var isListeningLoopRunning = false

    companion object {
        const val CHANNEL_ID = "google_control_hotword_channel"
        const val NOTIFICATION_ID = 1001
        const val ACTION_START = "ACTION_START_HOTWORD"
        const val ACTION_STOP = "ACTION_STOP_HOTWORD"
        const val ACTION_TRIGGER_NOW = "ACTION_TRIGGER_HOTWORD_NOW"

        fun start(context: Context) {
            val intent = Intent(context, VoiceHotwordService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, VoiceHotwordService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopListening()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                HotwordEventHub.setBackgroundListening(false)
                return START_NOT_STICKY
            }
            ACTION_TRIGGER_NOW -> {
                // User tapped notification action to trigger "Google" hotword pop-up directly
                triggerHotwordAction()
            }
            else -> {
                startForeground(NOTIFICATION_ID, buildForegroundNotification())
                startHotwordListening()
                HotwordEventHub.setBackgroundListening(true)
            }
        }
        return START_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Device Master Background Listener",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Runs in background to detect 'Device Master' or 'Google' voice activation hotword"
                setShowBadge(false)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildForegroundNotification(): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingOpenApp = PendingIntent.getActivity(
            this, 0, openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val triggerIntent = Intent(this, VoiceHotwordService::class.java).apply {
            action = ACTION_TRIGGER_NOW
        }
        val pendingTrigger = PendingIntent.getService(
            this, 1, triggerIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Device Master Active")
            .setContentText("Listening for 'Device Master' or 'Google' in background")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentIntent(pendingOpenApp)
            .addAction(android.R.drawable.ic_btn_speak_now, "Activate Assistant", pendingTrigger)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun startHotwordListening() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            Log.w("VoiceHotwordService", "Speech recognition unavailable in background")
            return
        }

        isListeningLoopRunning = true
        setupRecognizer()
        restartListening()
    }

    private fun setupRecognizer() {
        try {
            speechRecognizer?.destroy()
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
            speechRecognizer?.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {}
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {}

                override fun onError(error: Int) {
                    if (isListeningLoopRunning) {
                        serviceScope.launch {
                            delay(1200)
                            restartListening()
                        }
                    }
                }

                override fun onResults(results: Bundle?) {
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val text = matches?.joinToString(" ")?.lowercase() ?: ""

                    if (text.contains("device master") || text.contains("master") || text.contains("google")) {
                        triggerHotwordAction()
                    }

                    if (isListeningLoopRunning) {
                        serviceScope.launch {
                            delay(500)
                            restartListening()
                        }
                    }
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    val partialMatches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val text = partialMatches?.joinToString(" ")?.lowercase() ?: ""
                    if (text.contains("device master") || text.contains("master") || text.contains("google")) {
                        triggerHotwordAction()
                    }
                }

                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
        } catch (e: Exception) {
            Log.e("VoiceHotwordService", "Error setting up recognizer: ${e.message}")
        }
    }

    private fun restartListening() {
        if (!isListeningLoopRunning) return
        try {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.US)
                putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            }
            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            serviceScope.launch {
                delay(2000)
                restartListening()
            }
        }
    }

    private fun triggerHotwordAction() {
        HotwordEventHub.emitHotwordDetected("Google")

        // Bring Activity to foreground if needed
        val activityIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("SHOW_VOICE_POPUP", true)
        }
        startActivity(activityIntent)
    }

    private fun stopListening() {
        isListeningLoopRunning = false
        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.destroy()
            speechRecognizer = null
        } catch (e: Exception) {
            // Ignore
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopListening()
        HotwordEventHub.setBackgroundListening(false)
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
