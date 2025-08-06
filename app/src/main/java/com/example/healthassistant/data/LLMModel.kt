package com.example.healthassistant.data

import android.content.Context
import java.io.File

data class LLMModel(
    val name: String,
    val modelId: String,
    val modelFile: String,
    val description: String,
    val sizeInBytes: Long,
    val estimatedPeakMemoryInBytes: Long? = null,
    val version: String,
    val llmSupportImage: Boolean = false,
    val defaultConfig: ModelConfig,
    val url: String = "",
    
    // Runtime fields
    var instance: Any? = null,
    var initializing: Boolean = false,
    var downloadStatus: ModelDownloadStatus = ModelDownloadStatus.NOT_DOWNLOADED
)

data class ModelConfig(
    val topK: Int,
    val topP: Float,
    val temperature: Float,
    val maxTokens: Int,
    val accelerators: String
)

enum class ModelDownloadStatus {
    NOT_DOWNLOADED,
    DOWNLOADING,
    DOWNLOADED,
    FAILED,
    INITIALIZING,
    READY
}

sealed class ModelInitState {
    object NotInitialized : ModelInitState()
    object Initializing : ModelInitState()
    object Ready : ModelInitState()
    data class Error(val message: String) : ModelInitState()
}

data class ModelDownloadProgress(
    val status: ModelDownloadStatus,
    val progress: Float = 0f,
    val downloadedBytes: Long = 0L,
    val totalBytes: Long = 0L,
    val errorMessage: String? = null
)

// Predefined Gemma models
object GemmaModels {
    val GEMMA_3N_E2B_INT4 = LLMModel(
        name = "Gemma 3n E2B (Balanced)",
        modelId = "google/gemma-3n-E2B-it-litert-preview",
        modelFile = "gemma-3n-E2B-it-int4.task",
        description = "Gemma 3n E2B optimized for balanced health analysis. Good performance with vision support for health charts and data visualization.",
        sizeInBytes = 3136226711L,
        estimatedPeakMemoryInBytes = 5905580032L,
        version = "20250520",
        llmSupportImage = true,
        defaultConfig = ModelConfig(
            topK = 35,  // Optimized for Gemma 3n architecture
            topP = 0.8f,  // Lower for more consistent health analysis
            temperature = 0.6f,  // Reduced for focused, English responses
            maxTokens = 1536,  // Balanced for health reports
            accelerators = "gpu,cpu"  // GPU priority for Gemma 3n
        ),
        url = "https://huggingface.co/google/gemma-3n-E2B-it-litert-preview/resolve/main/gemma-3n-E2B-it-int4.task?download=true"
    )
    
    val GEMMA_3N_E4B_INT4 = LLMModel(
        name = "Gemma 3n E4B (Premium)",
        modelId = "google/gemma-3n-E4B-it-litert-preview",
        modelFile = "gemma-3n-E4B-it-int4.task",
        description = "Premium Gemma 3n E4B for comprehensive health analysis. Higher quality insights with advanced vision capabilities for detailed health data interpretation.",
        sizeInBytes = 4405655031L,
        estimatedPeakMemoryInBytes = 6979321856L,
        version = "20250520",
        llmSupportImage = true,
        defaultConfig = ModelConfig(
            topK = 30,  // Lower for more focused, consistent responses
            topP = 0.75f,  // Reduced for better English consistency
            temperature = 0.5f,  // Very low temperature for consistent health analysis
            maxTokens = 1280,  // Optimized for detailed but fast health reports
            accelerators = "gpu,cpu"  // GPU optimized for Gemma 3n architecture
        ),
        url = "https://huggingface.co/google/gemma-3n-E4B-it-litert-preview/resolve/main/gemma-3n-E4B-it-int4.task?download=true"
    )
    
    
    val ALL_MODELS = listOf(GEMMA_3N_E2B_INT4, GEMMA_3N_E4B_INT4)  // Gemma 3n models only, ordered by speed
}

// Extension function to get model file path
fun LLMModel.getFilePath(context: Context): String {
    val externalFilesDir = context.getExternalFilesDir(null)
    return File(externalFilesDir, "models/${this.modelFile}").absolutePath
}

// Extension function to check if model is downloaded
fun LLMModel.isDownloaded(context: Context): Boolean {
    val file = File(getFilePath(context))
    return file.exists() && file.length() == sizeInBytes
}