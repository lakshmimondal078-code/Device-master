package com.example.data.models

data class ExtractionRule(
    val fieldName: String,
    val selector: String, // CSS Selector, e.g. "h2.title", ".price", "article"
    val attribute: String = "text", // "text", "href", "src", "content"
    val regexPattern: String = "" // Optional post-processing regex
)

data class ScraperTemplate(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val description: String,
    val targetUrl: String,
    val voiceKeywords: List<String>,
    val rules: List<ExtractionRule>,
    val userAgent: String = "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0 Mobile Safari/537.36",
    val isBuiltIn: Boolean = false,
    val lastRunTimestamp: Long = 0L
)

data class ScrapedResultItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val templateId: String,
    val templateName: String,
    val url: String,
    val extractedData: Map<String, String>,
    val rawTextSnippet: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
