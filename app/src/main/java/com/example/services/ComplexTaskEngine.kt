package com.example.services

import com.example.data.models.CommandCategory
import com.example.data.models.ParsedCommand
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

data class TaskStep(
    val id: String = java.util.UUID.randomUUID().toString(),
    val description: String,
    val category: CommandCategory,
    var status: TaskStepStatus = TaskStepStatus.PENDING,
    var resultText: String = ""
)

enum class TaskStepStatus {
    PENDING,
    RUNNING,
    COMPLETED,
    FAILED
}

data class ComplexTaskExecution(
    val id: String = java.util.UUID.randomUUID().toString(),
    val rawPrompt: String,
    val steps: List<TaskStep>,
    val isRunning: Boolean = true,
    val completedSummary: String = ""
)

class ComplexTaskEngine(
    private val phoneController: PhoneController,
    private val scraperService: WebScraperService,
    private val aiService: AiAutomationService,
    private val voiceCommandService: VoiceCommandService
) {
    private val _currentExecution = MutableStateFlow<ComplexTaskExecution?>(null)
    val currentExecution: StateFlow<ComplexTaskExecution?> = _currentExecution.asStateFlow()

    suspend fun planAndExecuteTask(
        prompt: String,
        onStepUpdated: (TaskStep) -> Unit = {},
        onExecutionFinished: (String) -> Unit = {}
    ): String = withContext(Dispatchers.IO) {
        val plannedSteps = planSteps(prompt)
        val execution = ComplexTaskExecution(
            rawPrompt = prompt,
            steps = plannedSteps,
            isRunning = true
        )
        _currentExecution.value = execution

        val results = mutableListOf<String>()

        for (step in plannedSteps) {
            step.status = TaskStepStatus.RUNNING
            _currentExecution.value = execution.copy(steps = plannedSteps.map { it.copy() })
            onStepUpdated(step)

            try {
                val stepResult = executeSingleStep(step)
                step.status = TaskStepStatus.COMPLETED
                step.resultText = stepResult
                results.add(stepResult)
            } catch (e: Exception) {
                step.status = TaskStepStatus.FAILED
                step.resultText = "Error: ${e.message}"
                results.add("Step failed: ${e.message}")
            }

            _currentExecution.value = execution.copy(steps = plannedSteps.map { it.copy() })
            onStepUpdated(step)
        }

        val finalSummary = results.joinToString(". ")
        _currentExecution.value = execution.copy(
            isRunning = false,
            completedSummary = finalSummary
        )

        voiceCommandService.speakFeedback("Finished: $finalSummary")
        onExecutionFinished(finalSummary)
        finalSummary
    }

    private fun planSteps(prompt: String): List<TaskStep> {
        val lower = prompt.trim()
        val steps = mutableListOf<TaskStep>()

        // Split by common natural conjunctions: "and", "then", "after that", "also", ";"
        val clauses = lower.split(Regex("(?i)\\s+(and then|then|and|also|after that)\\s+|;\\s*"))
            .map { it.trim() }
            .filter { it.isNotBlank() }

        if (clauses.size > 1) {
            for (clause in clauses) {
                val parsed = voiceCommandService.parseOfflineCommand(clause)
                steps.add(
                    TaskStep(
                        description = clause,
                        category = parsed.category
                    )
                )
            }
        } else {
            // Single complex prompt or intent
            val p = clauses.firstOrNull() ?: prompt
            val parsed = voiceCommandService.parseOfflineCommand(p)
            steps.add(
                TaskStep(
                    description = p,
                    category = parsed.category
                )
            )
        }

        return steps
    }

    private suspend fun executeSingleStep(step: TaskStep): String {
        val desc = step.description.lowercase()

        return when {
            // Volume
            desc.contains("volume") || desc.contains("sound") || desc.contains("audio") || desc.contains("mute") -> {
                val percentRegex = Regex("(\\d+)\\s*(%|percent)")
                val match = percentRegex.find(desc)
                if (match != null) {
                    val percent = match.groupValues[1].toIntOrNull() ?: 50
                    phoneController.setMediaVolumePercent(percent)
                } else if (desc.contains("up") || desc.contains("increase") || desc.contains("louder")) {
                    phoneController.raiseMediaVolume()
                } else if (desc.contains("down") || desc.contains("decrease") || desc.contains("quieter")) {
                    phoneController.lowerMediaVolume()
                } else if (desc.contains("mute")) {
                    phoneController.muteMediaVolume()
                } else if (desc.contains("max") || desc.contains("full")) {
                    phoneController.maxMediaVolume()
                } else {
                    phoneController.setMediaVolumePercent(50)
                }
            }

            // Flashlight
            desc.contains("flashlight") || desc.contains("torch") -> {
                if (desc.contains("on") || desc.contains("enable")) {
                    phoneController.setTorch(true)
                } else if (desc.contains("off") || desc.contains("disable")) {
                    phoneController.setTorch(false)
                } else {
                    phoneController.toggleTorch()
                }
            }

            // Ringer Mode
            desc.contains("silent mode") || desc.contains("vibrate mode") || desc.contains("normal sound") -> {
                when {
                    desc.contains("silent") -> phoneController.setRingerMode("silent")
                    desc.contains("vibrate") -> phoneController.setRingerMode("vibrate")
                    else -> phoneController.setRingerMode("normal")
                }
            }

            // Image generation with gemini-nano-banana-2.1
            desc.contains("image") || desc.contains("draw") || desc.contains("photo") || desc.contains("picture") -> {
                val prompt = desc.replace(Regex("(?i)(generate image|create image|draw|make an image of|photo of)"), "").trim()
                aiService.generateOrEditImage(
                    prompt = prompt.ifBlank { "Futuristic assistant interface" },
                    model = "gemini-nano-banana-2.1"
                )
                "Created image with gemini-nano-banana-2.1"
            }

            // Web Scraping
            desc.contains("scrape") || desc.contains("extract") || desc.contains("headlines") || desc.contains("price tracker") -> {
                val tpl = scraperService.findTemplateByVoiceKeyword(desc) ?: scraperService.templates.value.first()
                val result = scraperService.runScraperTemplate(tpl)
                result.getOrNull()?.let {
                    "Scraped ${tpl.name}: ${it.extractedData.values.take(2).joinToString(", ")}"
                } ?: "Web extraction completed"
            }

            // App Launch
            desc.contains("open") || desc.contains("launch") || desc.contains("start") -> {
                val app = desc.replace(Regex("(?i)(open|launch|start)\\s+"), "").trim()
                val (_, msg) = phoneController.launchAppByName(app)
                msg
            }

            // Device Settings
            desc.contains("wifi") || desc.contains("bluetooth") || desc.contains("settings") -> {
                phoneController.openSettingsScreen(if (desc.contains("wifi")) "wifi" else if (desc.contains("bluetooth")) "bluetooth" else "settings")
            }

            // General AI Processing Fallback
            else -> {
                val aiResponse = aiService.executeAutomationPrompt(step.description)
                aiResponse.take(100)
            }
        }
    }

    fun clearCurrentExecution() {
        _currentExecution.value = null
    }
}
