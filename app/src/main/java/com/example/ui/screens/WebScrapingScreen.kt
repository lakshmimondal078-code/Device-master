package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.models.ExtractionRule
import com.example.data.models.ScrapedResultItem
import com.example.data.models.ScraperTemplate
import com.example.services.VoiceCommandService
import com.example.services.WebScraperService
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun WebScrapingScreen(
    scraperService: WebScraperService,
    voiceService: VoiceCommandService
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val templates by scraperService.templates.collectAsState()
    val history by scraperService.scrapedHistory.collectAsState()
    val isScraping by scraperService.isScraping.collectAsState()

    var showCreateDialog by remember { mutableStateOf(false) }
    var selectedTemplateForRun by remember { mutableStateOf<ScraperTemplate?>(templates.firstOrNull()) }
    var customUrlInput by remember { mutableStateOf("https://news.ycombinator.com/") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 14.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Web Data Extractor",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
                Text(
                    text = "Customizable scraping templates with voice triggers",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }

            FloatingActionButton(
                onClick = { showCreateDialog = true },
                containerColor = GoogleBlueLight,
                contentColor = Color.White,
                modifier = Modifier
                    .size(46.dp)
                    .testTag("add_template_button")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Template")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            // Live Quick Run Bar
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Execute Extraction Template",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        // Target URL input
                        OutlinedTextField(
                            value = customUrlInput,
                            onValueChange = { customUrlInput = it },
                            label = { Text("Target Web URL", fontSize = 12.sp) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = DarkSurfaceElevated,
                                unfocusedContainerColor = DarkSurfaceElevated,
                                focusedBorderColor = GoogleBlueLight,
                                unfocusedBorderColor = DarkBorder
                            )
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = {
                                val tpl = selectedTemplateForRun ?: templates.first()
                                val updatedTpl = tpl.copy(targetUrl = customUrlInput)
                                coroutineScope.launch {
                                    val res = scraperService.runScraperTemplate(updatedTpl)
                                    res.onSuccess { item ->
                                        voiceService.speakFeedback("Extraction complete. Extracted ${item.extractedData.size} data fields.")
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("run_scraper_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = GoogleBlueLight),
                            enabled = !isScraping
                        ) {
                            if (isScraping) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Scraping Web Page...")
                            } else {
                                Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Extract Live Web Data", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Template Library Section
            item {
                Text(
                    text = "Customizable Templates (${templates.size})",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            items(templates) { template ->
                TemplateCard(
                    template = template,
                    isSelected = selectedTemplateForRun?.id == template.id,
                    onSelect = {
                        selectedTemplateForRun = template
                        customUrlInput = template.targetUrl
                    },
                    onRun = {
                        coroutineScope.launch {
                            val res = scraperService.runScraperTemplate(template)
                            res.onSuccess { item ->
                                voiceService.speakFeedback("Scraped ${template.name}. Found ${item.extractedData.size} items.")
                            }
                        }
                    },
                    onDelete = {
                        scraperService.deleteTemplate(template.id)
                    }
                )
            }

            // Extracted Results History
            item {
                Text(
                    text = "Extraction Results History (${history.size})",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }

            if (history.isEmpty()) {
                item {
                    Text(
                        text = "No web data extracted yet. Select a template and click Extract Live Web Data.",
                        fontSize = 12.sp,
                        color = TextMuted,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                }
            } else {
                items(history) { resultItem ->
                    ScrapedResultCard(
                        result = resultItem,
                        onReadAloud = {
                            val speech = resultItem.extractedData.entries.joinToString(". ") { "${it.key}: ${it.value}" }
                            voiceService.speakFeedback("Extracted results for ${resultItem.templateName}: $speech")
                        },
                        onCopy = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Scraped Data", resultItem.extractedData.toString())
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "Copied data to clipboard", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Dialog for creating custom scraping template
    if (showCreateDialog) {
        CreateTemplateDialog(
            onDismiss = { showCreateDialog = false },
            onSave = { newTemplate ->
                scraperService.addCustomTemplate(newTemplate)
                showCreateDialog = false
            }
        )
    }
}

@Composable
private fun TemplateCard(
    template: ScraperTemplate,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onRun: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color(0xFF162544) else DarkSurfaceElevated
        ),
        border = if (isSelected) CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(GoogleBlueLight)) else CardDefaults.outlinedCardBorder()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(GoogleBlue.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Default.Code, contentDescription = null, tint = GoogleBlueLight, modifier = Modifier.size(16.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(text = template.name, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text(
                            text = template.targetUrl,
                            fontSize = 11.sp,
                            color = TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Row {
                    IconButton(onClick = onRun) {
                        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "Run", tint = GoogleGreen)
                    }
                    if (!template.isBuiltIn) {
                        IconButton(onClick = onDelete) {
                            Icon(imageVector = Icons.Default.DeleteOutline, contentDescription = "Delete", tint = GoogleRed)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(text = template.description, fontSize = 12.sp, color = TextSecondary)

            Spacer(modifier = Modifier.height(10.dp))

            // Extraction rules pills
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                template.rules.take(3).forEach { rule ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = DarkSurface
                    ) {
                        Text(
                            text = "${rule.fieldName} [${rule.selector}]",
                            fontSize = 10.sp,
                            color = PrimaryIndigo,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ScrapedResultCard(
    result: ScrapedResultItem,
    onReadAloud: () -> Unit,
    onCopy: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = result.templateName,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = GoogleGreen
                )

                Row {
                    IconButton(onClick = onReadAloud) {
                        Icon(imageVector = Icons.Default.VolumeUp, contentDescription = "Speak", tint = PrimaryIndigo)
                    }
                    IconButton(onClick = onCopy) {
                        Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy", tint = TextSecondary)
                    }
                }
            }

            Divider(color = DarkBorder, modifier = Modifier.padding(vertical = 6.dp))

            result.extractedData.forEach { (key, value) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = key, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextSecondary)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = value,
                        fontSize = 12.sp,
                        color = Color.White,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun CreateTemplateDialog(
    onDismiss: () -> Unit,
    onSave: (ScraperTemplate) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var targetUrl by remember { mutableStateOf("https://") }
    var desc by remember { mutableStateOf("") }
    var field1Name by remember { mutableStateOf("title") }
    var field1Selector by remember { mutableStateOf("h1") }
    var field2Name by remember { mutableStateOf("price") }
    var field2Selector by remember { mutableStateOf(".price") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Scraping Template", color = Color.White) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Template Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = targetUrl,
                    onValueChange = { targetUrl = it },
                    label = { Text("Target URL") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("Description") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Extraction Rule #1", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PrimaryIndigo)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = field1Name,
                        onValueChange = { field1Name = it },
                        label = { Text("Field") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = field1Selector,
                        onValueChange = { field1Selector = it },
                        label = { Text("CSS Selector") },
                        modifier = Modifier.weight(1f)
                    )
                }

                Text("Extraction Rule #2", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PrimaryIndigo)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = field2Name,
                        onValueChange = { field2Name = it },
                        label = { Text("Field") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = field2Selector,
                        onValueChange = { field2Selector = it },
                        label = { Text("CSS Selector") },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank() && targetUrl.isNotBlank()) {
                        val rules = mutableListOf<ExtractionRule>()
                        if (field1Name.isNotBlank() && field1Selector.isNotBlank()) {
                            rules.add(ExtractionRule(fieldName = field1Name, selector = field1Selector))
                        }
                        if (field2Name.isNotBlank() && field2Selector.isNotBlank()) {
                            rules.add(ExtractionRule(fieldName = field2Name, selector = field2Selector))
                        }
                        val template = ScraperTemplate(
                            name = name,
                            description = desc.ifBlank { "Custom Web Extractor" },
                            targetUrl = targetUrl,
                            voiceKeywords = listOf(name.lowercase()),
                            rules = rules
                        )
                        onSave(template)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = GoogleBlueLight)
            ) {
                Text("Save Template")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        containerColor = DarkSurface
    )
}
