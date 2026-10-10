package com.example.data.models

enum class AiProvider {
    GEMINI,
    OPENAI_COMPATIBLE,
    CUSTOM_WEBHOOK
}

data class AiModelInfo(
    val id: String,
    val displayName: String,
    val provider: AiProvider,
    val description: String,
    val supportsImages: Boolean = false
)

data class GeneratedImageItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val prompt: String,
    val model: String = "gemini-nano-banana-2.1",
    val base64OrUri: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val aspectRatio: String = "1:1"
)

data class AiAutomationMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val sender: String, // "user", "assistant", "system"
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val executedActions: List<String> = emptyList()
)
