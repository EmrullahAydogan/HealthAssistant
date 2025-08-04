package com.example.healthassistant.ui.models

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.healthassistant.data.*
import com.example.healthassistant.viewmodel.ModelsViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelsScreen(
    viewModel: ModelsViewModel
) {
    val modelsState by viewModel.modelsState.collectAsState()
    val selectedModel by viewModel.selectedHealthModel.collectAsState()
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showTokenDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "AI Models",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            
            Row {
                IconButton(onClick = { showTokenDialog = true }) {
                    Icon(
                        Icons.Default.VpnKey,
                        contentDescription = "HuggingFace Token",
                        tint = if (viewModel.hasHuggingFaceToken()) 
                            MaterialTheme.colorScheme.primary 
                        else 
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                IconButton(onClick = { showSettingsDialog = true }) {
                    Icon(
                        Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Selected model for health analysis card
        SelectedModelCard(
            selectedModel = selectedModel,
            onModelSelect = { showSettingsDialog = true }
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Available Models",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Models list
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(GemmaModels.ALL_MODELS) { model ->
                val progress = modelsState[model.name]
                ModelCard(
                    model = model,
                    progress = progress,
                    onDownload = { viewModel.downloadModel(model) },
                    onCancel = { viewModel.cancelDownload(model) },
                    onDelete = { viewModel.deleteModel(model) },
                    onSelect = { viewModel.setSelectedHealthModel(model) },
                    isSelected = selectedModel?.name == model.name
                )
            }
        }
    }
    
    // Settings dialog
    if (showSettingsDialog) {
        HealthModelSettingsDialog(
            availableModels = GemmaModels.ALL_MODELS.filter { 
                modelsState[it.name]?.status == ModelDownloadStatus.DOWNLOADED 
            },
            selectedModel = selectedModel,
            onModelSelected = { model ->
                viewModel.setSelectedHealthModel(model)
                showSettingsDialog = false
            },
            onDismiss = { showSettingsDialog = false }
        )
    }
    
    // Token dialog
    if (showTokenDialog) {
        HuggingFaceTokenDialog(
            currentToken = viewModel.getHuggingFaceToken(),
            onTokenSaved = { token ->
                viewModel.setHuggingFaceToken(token)
                showTokenDialog = false
            },
            onTokenCleared = {
                viewModel.clearHuggingFaceToken()
                showTokenDialog = false
            },
            onDismiss = { showTokenDialog = false }
        )
    }
}

@Composable
fun SelectedModelCard(
    selectedModel: LLMModel?,
    onModelSelect: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selectedModel != null) 
                MaterialTheme.colorScheme.primaryContainer 
            else 
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Health Analysis Model",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (selectedModel != null) 
                            MaterialTheme.colorScheme.onPrimaryContainer
                        else 
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Text(
                        text = selectedModel?.name ?: "No model selected",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (selectedModel != null) 
                            MaterialTheme.colorScheme.onPrimaryContainer
                        else 
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Button(
                    onClick = onModelSelect,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selectedModel != null) 
                            MaterialTheme.colorScheme.primary
                        else 
                            MaterialTheme.colorScheme.secondary
                    )
                ) {
                    Text(if (selectedModel != null) "Change" else "Select")
                }
            }
            
            if (selectedModel != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "This model will analyze your health data and provide insights.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

@Composable
fun ModelCard(
    model: LLMModel,
    progress: ModelDownloadProgress?,
    onDownload: () -> Unit,
    onCancel: () -> Unit,
    onDelete: () -> Unit,
    onSelect: () -> Unit,
    isSelected: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) 
                MaterialTheme.colorScheme.secondaryContainer
            else 
                MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = model.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Text(
                        text = "Size: ${formatFileSize(model.sizeInBytes)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    if (model.llmSupportImage) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            Icon(
                                Icons.Default.Image,
                                contentDescription = "Supports images",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Image support",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
                
                ModelActionButton(
                    progress = progress,
                    onDownload = onDownload,
                    onCancel = onCancel,
                    onDelete = onDelete,
                    onSelect = onSelect,
                    isSelected = isSelected
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = model.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            
            // Progress bar for downloading
            if (progress?.status == ModelDownloadStatus.DOWNLOADING) {
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { progress.progress },
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = "Downloading... ${(progress.progress * 100).toInt()}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            
            // Error message
            if (progress?.errorMessage != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Error,
                                contentDescription = "Error",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Error",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = progress.errorMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                        
                        // Show retry suggestion for corrupted files
                        if (progress.errorMessage.contains("corrupted", ignoreCase = true)) {
                            Text(
                                text = "💡 Try deleting and re-downloading the model",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(top = 4.dp),
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ModelActionButton(
    progress: ModelDownloadProgress?,
    onDownload: () -> Unit,
    onCancel: () -> Unit,
    onDelete: () -> Unit,
    onSelect: () -> Unit,
    isSelected: Boolean
) {
    when (progress?.status) {
        ModelDownloadStatus.NOT_DOWNLOADED -> {
            Button(
                onClick = onDownload,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    Icons.Default.Download,
                    contentDescription = "Download",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Download")
            }
        }
        
        ModelDownloadStatus.DOWNLOADING -> {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { },
                    enabled = false,
                    colors = ButtonDefaults.buttonColors(
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Downloading")
                }
                
                OutlinedButton(
                    onClick = onCancel,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(
                        Icons.Default.Cancel,
                        contentDescription = "Cancel",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Cancel")
                }
            }
        }
        
        ModelDownloadStatus.INITIALIZING -> {
            Button(
                onClick = { },
                enabled = false,
                colors = ButtonDefaults.buttonColors(
                    disabledContainerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Initializing...")
            }
        }
        
        ModelDownloadStatus.DOWNLOADED, ModelDownloadStatus.READY -> {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    onClick = onDelete,
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
                
                Button(
                    onClick = onSelect,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSelected) 
                            MaterialTheme.colorScheme.secondary
                        else 
                            MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        if (isSelected) Icons.Default.Check else Icons.Default.Psychology,
                        contentDescription = if (isSelected) "Selected" else "Select",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (isSelected) "Selected" else "Select")
                }
            }
        }
        
        else -> {
            Button(
                onClick = onDownload,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text("Download")
            }
        }
    }
}

@Composable
fun HealthModelSettingsDialog(
    availableModels: List<LLMModel>,
    selectedModel: LLMModel?,
    onModelSelected: (LLMModel?) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Select Health Analysis Model")
        },
        text = {
            Column {
                Text(
                    text = "Choose which model to use for analyzing your health data:",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                if (availableModels.isEmpty()) {
                    Text(
                        text = "No models available. Please download a model first.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    LazyColumn {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = selectedModel == null,
                                    onClick = { onModelSelected(null) }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("None (Disable AI Analysis)")
                            }
                        }
                        
                        items(availableModels) { model ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = selectedModel?.name == model.name,
                                    onClick = { onModelSelected(model) }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = model.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = formatFileSize(model.sizeInBytes),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done")
            }
        }
    )
}

@Composable
fun HuggingFaceTokenDialog(
    currentToken: String?,
    onTokenSaved: (String) -> Unit,
    onTokenCleared: () -> Unit,
    onDismiss: () -> Unit
) {
    var tokenText by remember { mutableStateOf(if (currentToken == null) "" else currentToken.take(8) + "...") }
    var isEditing by remember { mutableStateOf(currentToken == null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("HuggingFace Access Token")
        },
        text = {
            Column {
                Text(
                    text = "Some models require a HuggingFace access token. You can get one from huggingface.co/settings/tokens",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                if (isEditing) {
                    OutlinedTextField(
                        value = tokenText,
                        onValueChange = { tokenText = it },
                        label = { Text("Access Token") },
                        placeholder = { Text("hf_...") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Token: ${currentToken?.take(8)}...",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        
                        Row {
                            TextButton(onClick = { isEditing = true }) {
                                Text("Edit")
                            }
                            TextButton(
                                onClick = onTokenCleared,
                                colors = ButtonDefaults.textButtonColors(
                                    contentColor = MaterialTheme.colorScheme.error
                                )
                            ) {
                                Text("Clear")
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (isEditing) {
                TextButton(
                    onClick = {
                        if (tokenText.isNotBlank()) {
                            onTokenSaved(tokenText.trim())
                        }
                    },
                    enabled = tokenText.isNotBlank()
                ) {
                    Text("Save")
                }
            } else {
                TextButton(onClick = onDismiss) {
                    Text("Done")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private fun formatFileSize(bytes: Long): String {
    val kb = 1024.0
    val mb = kb * 1024
    val gb = mb * 1024
    
    return when {
        bytes >= gb -> String.format(Locale.US, "%.1f GB", bytes / gb)
        bytes >= mb -> String.format(Locale.US, "%.1f MB", bytes / mb)
        bytes >= kb -> String.format(Locale.US, "%.1f KB", bytes / kb)
        else -> "$bytes B"
    }
}