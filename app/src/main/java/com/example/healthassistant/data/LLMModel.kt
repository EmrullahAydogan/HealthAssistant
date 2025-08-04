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
        name = "Gemma-3n-E2B-it-int4",
        modelId = "google/gemma-3n-E2B-it-litert-preview",
        modelFile = "gemma-3n-E2B-it-int4.task",
        description = "Preview version of Gemma 3n E2B ready for deployment on Android using the MediaPipe LLM Inference API. The current checkpoint only supports text and vision input, with 4096 context length.",
        sizeInBytes = 3136226711L,
        estimatedPeakMemoryInBytes = 5905580032L,
        version = "20250520",
        llmSupportImage = true,
        defaultConfig = ModelConfig(
            topK = 64,
            topP = 0.95f,
            temperature = 1.0f,
            maxTokens = 4096,
            accelerators = "cpu,gpu"
        ),
        url = "https://huggingface.co/google/gemma-3n-E2B-it-litert-preview/resolve/main/gemma-3n-E2B-it-int4.task?download=true"
    )
    
    val GEMMA_3N_E4B_INT4 = LLMModel(
        name = "Gemma-3n-E4B-it-int4",
        modelId = "google/gemma-3n-E4B-it-litert-preview",
        modelFile = "gemma-3n-E4B-it-int4.task",
        description = "Preview version of Gemma 3n E4B ready for deployment on Android using the MediaPipe LLM Inference API. The current checkpoint only supports text and vision input, with 4096 context length.",
        sizeInBytes = 4405655031L,
        estimatedPeakMemoryInBytes = 6979321856L,
        version = "20250520",
        llmSupportImage = true,
        defaultConfig = ModelConfig(
            topK = 64,
            topP = 0.95f,
            temperature = 1.0f,
            maxTokens = 4096,
            accelerators = "cpu,gpu"
        ),
        url = "https://huggingface.co/google/gemma-3n-E4B-it-litert-preview/resolve/main/gemma-3n-E4B-it-int4.task?download=true"
    )
    
    val GEMMA3_1B_IT_Q4 = LLMModel(
        name = "Gemma3-1B-IT q4",
        modelId = "litert-community/Gemma3-1B-IT",
        modelFile = "Gemma3-1B-IT_multi-prefill-seq_q4_ekv2048.task",
        description = "A variant of google/Gemma-3-1B-IT with 4-bit quantization ready for deployment on Android using the MediaPipe LLM Inference API",
        sizeInBytes = 554661246L,
        estimatedPeakMemoryInBytes = 2147483648L,
        version = "20250514",
        llmSupportImage = false,
        defaultConfig = ModelConfig(
            topK = 64,
            topP = 0.95f,
            temperature = 1.0f,
            maxTokens = 1024,
            accelerators = "gpu,cpu"
        ),
        url = "https://huggingface.co/litert-community/Gemma3-1B-IT/resolve/main/Gemma3-1B-IT_multi-prefill-seq_q4_ekv2048.task?download=true"
    )
    
    val ALL_MODELS = listOf(GEMMA_3N_E2B_INT4, GEMMA_3N_E4B_INT4)
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