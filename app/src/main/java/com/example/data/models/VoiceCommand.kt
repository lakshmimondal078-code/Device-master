package com.example.data.models

enum class CommandCategory {
    VOLUME,
    FLASHLIGHT,
    APP_LAUNCH,
    DEVICE_SETTINGS,
    WEB_SCRAPER,
    AI_AUTOMATION,
    SYSTEM_INFO
}

data class ParsedCommand(
    val rawText: String,
    val category: CommandCategory,
    val action: String,
    val target: String? = null,
    val parameterValue: Int? = null,
    val feedbackMessage: String,
    val isHandledOffline: Boolean = true
)

data class CommandExecutionLog(
    val id: String = java.util.UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val commandText: String,
    val category: CommandCategory,
    val success: Boolean,
    val feedback: String,
    val executedOffline: Boolean = true
)

data class PredefinedVoiceTrigger(
    val phrase: String,
    val description: String,
    val category: CommandCategory,
    val example: String
)
