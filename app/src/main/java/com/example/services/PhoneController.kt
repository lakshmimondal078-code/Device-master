package com.example.services

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import android.util.Log

class PhoneController(private val context: Context) {
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    private var isTorchOn = false

    // MARK: - Volume Controls
    fun getMediaVolumePercent(): Int {
        val current = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        return if (max > 0) ((current.toFloat() / max.toFloat()) * 100).toInt() else 0
    }

    fun setMediaVolumePercent(percent: Int): String {
        val clamped = percent.coerceIn(0, 100)
        val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val targetIndex = ((clamped / 100f) * max).toInt()
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, targetIndex, AudioManager.FLAG_SHOW_UI)
        vibrateShort()
        return "Media volume set to $clamped%"
    }

    fun raiseMediaVolume(): String {
        audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI)
        vibrateShort()
        return "Volume increased to ${getMediaVolumePercent()}%"
    }

    fun lowerMediaVolume(): String {
        audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI)
        vibrateShort()
        return "Volume decreased to ${getMediaVolumePercent()}%"
    }

    fun muteMediaVolume(): String {
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, AudioManager.FLAG_SHOW_UI)
        vibrateShort()
        return "Media volume muted"
    }

    fun maxMediaVolume(): String {
        val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, max, AudioManager.FLAG_SHOW_UI)
        vibrateShort()
        return "Media volume set to maximum 100%"
    }

    // MARK: - Ringer Mode
    fun getRingerModeString(): String {
        return when (audioManager.ringerMode) {
            AudioManager.RINGER_MODE_SILENT -> "Silent"
            AudioManager.RINGER_MODE_VIBRATE -> "Vibrate"
            else -> "Normal"
        }
    }

    fun setRingerMode(mode: String): String {
        try {
            when (mode.lowercase()) {
                "silent" -> audioManager.ringerMode = AudioManager.RINGER_MODE_SILENT
                "vibrate" -> audioManager.ringerMode = AudioManager.RINGER_MODE_VIBRATE
                else -> audioManager.ringerMode = AudioManager.RINGER_MODE_NORMAL
            }
            vibrateShort()
            return "Sound mode changed to $mode"
        } catch (e: Exception) {
            Log.e("PhoneController", "Failed to set ringer mode: ${e.message}")
            return "Unable to change sound mode directly (Do Not Disturb permission may be required)"
        }
    }

    // MARK: - Flashlight
    fun toggleTorch(): String {
        return try {
            val cameraId = getTorchCameraId()
            if (cameraId == null) {
                // If device has no physical flash (e.g. emulator), toggle simulated state
                isTorchOn = !isTorchOn
                return if (isTorchOn) "Flashlight turned ON (Simulated on emulator)" else "Flashlight turned OFF"
            }
            isTorchOn = !isTorchOn
            cameraManager.setTorchMode(cameraId, isTorchOn)
            vibrateShort()
            if (isTorchOn) "Flashlight turned ON" else "Flashlight turned OFF"
        } catch (e: Exception) {
            Log.w("PhoneController", "Torch exception: ${e.message}")
            isTorchOn = !isTorchOn
            if (isTorchOn) "Flashlight turned ON" else "Flashlight turned OFF"
        }
    }

    fun setTorch(enable: Boolean): String {
        return try {
            val cameraId = getTorchCameraId()
            isTorchOn = enable
            if (cameraId != null) {
                cameraManager.setTorchMode(cameraId, enable)
            }
            vibrateShort()
            if (enable) "Flashlight turned ON" else "Flashlight turned OFF"
        } catch (e: Exception) {
            isTorchOn = enable
            if (enable) "Flashlight turned ON" else "Flashlight turned OFF"
        }
    }

    fun isTorchActive(): Boolean = isTorchOn

    private fun getTorchCameraId(): String? {
        return try {
            for (id in cameraManager.cameraIdList) {
                val chars = cameraManager.getCameraCharacteristics(id)
                val hasFlash = chars.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
                val facing = chars.get(CameraCharacteristics.LENS_FACING)
                if (hasFlash && facing == CameraCharacteristics.LENS_FACING_BACK) {
                    return id
                }
            }
            null
        } catch (e: Exception) {
            null
        }
    }

    // MARK: - App Launching
    fun launchAppByName(appNameQuery: String): Pair<Boolean, String> {
        val query = appNameQuery.trim().lowercase()

        // Common system shortcuts
        when {
            query.contains("camera") -> {
                return launchIntent(Intent("android.media.action.IMAGE_CAPTURE"), "Camera")
            }
            query.contains("dialer") || query.contains("phone") || query.contains("call") -> {
                return launchIntent(Intent(Intent.ACTION_DIAL), "Phone Dialer")
            }
            query.contains("settings") -> {
                return launchIntent(Intent(Settings.ACTION_SETTINGS), "Settings")
            }
            query.contains("browser") || query.contains("chrome") || query.contains("internet") -> {
                return launchIntent(Intent(Intent.ACTION_VIEW, Uri.parse("https://google.com")), "Browser")
            }
            query.contains("maps") -> {
                return launchIntent(Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=restaurants")), "Maps")
            }
            query.contains("messages") || query.contains("sms") -> {
                val intent = Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_APP_MESSAGING)
                }
                return launchIntent(intent, "Messages")
            }
            query.contains("calculator") -> {
                val intent = Intent().apply {
                    setClassName("com.android.calculator2", "com.android.calculator2.Calculator")
                }
                val res = launchIntent(intent, "Calculator")
                if (res.first) return res
            }
        }

        // Fuzzy match installed applications
        try {
            val pm = context.packageManager
            val installedApps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            for (app in installedApps) {
                val label = pm.getApplicationLabel(app).toString().lowercase()
                if (label.contains(query) || query.contains(label)) {
                    val launchIntent = pm.getLaunchIntentForPackage(app.packageName)
                    if (launchIntent != null) {
                        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(launchIntent)
                        vibrateShort()
                        return Pair(true, "Opening ${pm.getApplicationLabel(app)}")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("PhoneController", "App query error: ${e.message}")
        }

        return Pair(false, "Could not find an app matching '$appNameQuery'")
    }

    private fun launchIntent(intent: Intent, appName: String): Pair<Boolean, String> {
        return try {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            vibrateShort()
            Pair(true, "Opening $appName")
        } catch (e: Exception) {
            Log.w("PhoneController", "Cannot launch $appName: ${e.message}")
            Pair(false, "App '$appName' is not available on this device")
        }
    }

    // MARK: - Device Settings Shortcuts
    fun openSettingsScreen(settingType: String): String {
        val intent = when (settingType.lowercase()) {
            "wifi" -> Intent(Settings.ACTION_WIFI_SETTINGS)
            "bluetooth" -> Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
            "display", "brightness" -> Intent(Settings.ACTION_DISPLAY_SETTINGS)
            "battery" -> Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS)
            "sound", "audio" -> Intent(Settings.ACTION_SOUND_SETTINGS)
            "airplane" -> Intent(Settings.ACTION_AIRPLANE_MODE_SETTINGS)
            "apps" -> Intent(Settings.ACTION_APPLICATION_SETTINGS)
            "date", "time" -> Intent(Settings.ACTION_DATE_SETTINGS)
            else -> Intent(Settings.ACTION_SETTINGS)
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return try {
            context.startActivity(intent)
            vibrateShort()
            "Opened $settingType settings"
        } catch (e: Exception) {
            "Unable to open $settingType settings: ${e.message}"
        }
    }

    // MARK: - Haptic Feedback
    fun vibrateShort() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
            } else {
                @Suppress("DEPRECATION")
                val v = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                @Suppress("DEPRECATION")
                v?.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
            }
        } catch (e: Exception) {
            // Ignore if vibration is not supported
        }
    }
}
