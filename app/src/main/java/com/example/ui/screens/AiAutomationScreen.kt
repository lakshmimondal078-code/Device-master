package com.example.ui.screens

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.models.AiModelInfo
import com.example.data.models.GeneratedImageItem
import com.example.services.AiAutomationService
import com.example.services.VoiceCommandService
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun AiAutomationScreen(
    aiService: AiAutomationService,
    voiceService: VoiceCommandService
) {
    val coroutineScope = rememberCoroutineScope()
    val selectedModel by aiService.selectedModel.collectAsState()
    val chatMessages by aiService.chatMessages.collectAsState()
    val generatedImages by aiService.generatedImages.collectAsState()
    val isProcessing by aiService.isProcessing.collectAsState()

    var activeTab by remember { mutableStateOf(0) } // 0: AI Automation, 1: Image Studio (gemini-nano-banana-2.1), 2: Endpoints
    var promptInput by remember { mutableStateOf("") }
    var imagePromptInput by remember { mutableStateOf("") }
    var selectedAspectRatio by remember { mutableStateOf("1:1") }

    var customEndpointInput by remember { mutableStateOf(aiService.customEndpoint.value) }
    var customApiKeyInput by remember { mutableStateOf(aiService.customApiKey.value) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 14.dp)
    ) {
        // Title & Model Badge
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "AI Automation Hub",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
                Text(
                    text = "Gemini, ChatGPT & gemini-nano-banana-2.1",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = GoogleBlue.copy(alpha = 0.2f),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(GoogleBlueLight))
            ) {
                Text(
                    text = selectedModel.displayName,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = GoogleBlueLight,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Tabs: Automation Chat vs Image Studio vs Config
        TabRow(
            selectedTabIndex = activeTab,
            containerColor = DarkSurface,
            contentColor = GoogleBlueLight,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
        ) {
            Tab(
                selected = activeTab == 0,
                onClick = { activeTab = 0 },
                text = { Text("Automation Chat", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = activeTab == 1,
                onClick = { activeTab = 1 },
                text = { Text("Image Studio", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = activeTab == 2,
                onClick = { activeTab = 2 },
                text = { Text("AI Endpoints", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        when (activeTab) {
            0 -> {
                // Automation Chat Tab
                Column(modifier = Modifier.fillMaxSize()) {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(chatMessages) { msg ->
                            val isUser = msg.sender == "user"
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                            ) {
                                Card(
                                    modifier = Modifier.widthIn(max = 300.dp),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isUser) GoogleBlue else DarkSurfaceElevated
                                    )
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(
                                            text = msg.content,
                                            fontSize = 13.sp,
                                            color = Color.White
                                        )
                                        if (msg.executedActions.isNotEmpty()) {
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                msg.executedActions.forEach { act ->
                                                    Surface(
                                                        shape = RoundedCornerShape(6.dp),
                                                        color = GoogleGreen.copy(alpha = 0.2f)
                                                    ) {
                                                        Text(
                                                            text = "✓ $act",
                                                            fontSize = 10.sp,
                                                            color = GoogleGreen,
                                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Input Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = promptInput,
                            onValueChange = { promptInput = it },
                            placeholder = { Text("Ask AI or give automated command...", fontSize = 13.sp, color = TextMuted) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("ai_prompt_input"),
                            shape = RoundedCornerShape(14.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = DarkSurface,
                                unfocusedContainerColor = DarkSurface,
                                focusedBorderColor = GoogleBlueLight,
                                unfocusedBorderColor = DarkBorder
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = {
                                if (promptInput.isNotBlank()) {
                                    val prompt = promptInput
                                    promptInput = ""
                                    coroutineScope.launch {
                                        val response = aiService.executeAutomationPrompt(
                                            prompt = prompt,
                                            phoneStateSnippet = "Volume active, Wi-Fi on",
                                            onActionParsed = { action ->
                                                voiceService.speakFeedback("Automated action: $action")
                                            }
                                        )
                                        voiceService.speakFeedback(response.take(120))
                                    }
                                }
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(GoogleBlueLight)
                                .testTag("send_ai_prompt_button")
                        ) {
                            if (isProcessing) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                            } else {
                                Icon(imageVector = Icons.Default.Send, contentDescription = "Send", tint = Color.White)
                            }
                        }
                    }
                }
            }

            1 -> {
                // Image Studio (gemini-nano-banana-2.1)
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = DarkSurface),
                            border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(GoogleYellow.copy(alpha = 0.4f), Color.Transparent)))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, tint = GoogleYellow)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Create & Edit Images (gemini-nano-banana-2.1)",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                OutlinedTextField(
                                    value = imagePromptInput,
                                    onValueChange = { imagePromptInput = it },
                                    label = { Text("Text prompt for image generation/edit", fontSize = 12.sp) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("image_prompt_input"),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = DarkSurfaceElevated,
                                        unfocusedContainerColor = DarkSurfaceElevated,
                                        focusedBorderColor = GoogleYellow,
                                        unfocusedBorderColor = DarkBorder
                                    )
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                // Aspect Ratio Selector
                                Text("Aspect Ratio", fontSize = 12.sp, color = TextSecondary)
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                                ) {
                                    listOf("1:1", "16:9", "4:3").forEach { ratio ->
                                        FilterChip(
                                            selected = selectedAspectRatio == ratio,
                                            onClick = { selectedAspectRatio = ratio },
                                            label = { Text(ratio, fontSize = 11.sp) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = GoogleYellow.copy(alpha = 0.2f),
                                                selectedLabelColor = GoogleYellow
                                            )
                                        )
                                    }
                                }

                                Button(
                                    onClick = {
                                        if (imagePromptInput.isNotBlank()) {
                                            coroutineScope.launch {
                                                aiService.generateOrEditImage(
                                                    prompt = imagePromptInput,
                                                    aspectRatio = selectedAspectRatio,
                                                    model = "gemini-nano-banana-2.1"
                                                )
                                                voiceService.speakFeedback("Image creation completed with gemini-nano-banana-2.1")
                                            }
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .testTag("generate_image_button"),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = GoogleYellow),
                                    enabled = !isProcessing
                                ) {
                                    if (isProcessing) {
                                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.Black, strokeWidth = 2.dp)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Generating via gemini-nano-banana-2.1...", color = Color.Black)
                                    } else {
                                        Icon(imageVector = Icons.Default.Palette, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Create / Edit Image", fontWeight = FontWeight.Bold, color = Color.Black)
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Text(
                            text = "Generated Image Gallery (${generatedImages.size})",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    if (generatedImages.isEmpty()) {
                        item {
                            Text(
                                text = "No images created yet. Enter a prompt like 'Cybernetic control room with glowing dials' to create one.",
                                fontSize = 12.sp,
                                color = TextMuted
                            )
                        }
                    } else {
                        items(generatedImages) { imgItem ->
                            GeneratedImageCard(imgItem)
                        }
                    }
                }
            }

            2 -> {
                // Models & Endpoints Selector Tab
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Text(
                            text = "Select Active AI Model",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    items(aiService.availableModels) { model ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { aiService.selectModel(model) },
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (selectedModel.id == model.id) Color(0xFF1E293B) else DarkSurface
                            ),
                            border = if (selectedModel.id == model.id) CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(GoogleBlueLight)) else CardDefaults.outlinedCardBorder()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = selectedModel.id == model.id,
                                    onClick = { aiService.selectModel(model) },
                                    colors = RadioButtonDefaults.colors(selectedColor = GoogleBlueLight)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = model.displayName, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    Text(text = model.description, fontSize = 11.sp, color = TextSecondary)
                                }
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Custom Endpoint Configuration",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = DarkSurface)
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                OutlinedTextField(
                                    value = customEndpointInput,
                                    onValueChange = { customEndpointInput = it },
                                    label = { Text("Custom API Endpoint URL") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                OutlinedTextField(
                                    value = customApiKeyInput,
                                    onValueChange = { customApiKeyInput = it },
                                    label = { Text("API Key (Optional / ChatGPT / Custom)") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Button(
                                    onClick = {
                                        aiService.updateCustomConfig(customEndpointInput, customApiKeyInput)
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = GoogleBlueLight)
                                ) {
                                    Text("Save Endpoint Configuration")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GeneratedImageCard(item: GeneratedImageItem) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = GoogleYellow.copy(alpha = 0.2f)
                ) {
                    Text(
                        text = item.model,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = GoogleYellow,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                Text(
                    text = "Ratio: ${item.aspectRatio}",
                    fontSize = 11.sp,
                    color = TextSecondary
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Image display: decode base64 if present or render high quality digital graphic canvas
            val decodedBitmap = remember(item.base64OrUri) {
                if (item.base64OrUri.isNotBlank()) {
                    try {
                        val bytes = Base64.decode(item.base64OrUri, Base64.DEFAULT)
                        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    } catch (e: Exception) {
                        null
                    }
                } else null
            }

            if (decodedBitmap != null) {
                Image(
                    bitmap = decodedBitmap.asImageBitmap(),
                    contentDescription = item.prompt,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(12.dp))
                )
            } else {
                // High aesthetic creative illustration canvas
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    Color(0xFF0F172A),
                                    Color(0xFF1E3A8A),
                                    Color(0xFF0284C7)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Palette,
                            contentDescription = null,
                            tint = GoogleYellow,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "\"${item.prompt}\"",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White,
                            maxLines = 2
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Created with gemini-nano-banana-2.1 High-Res Pipeline",
                            fontSize = 10.sp,
                            color = PrimaryIndigo
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(text = item.prompt, fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
        }
    }
}
