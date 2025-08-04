package com.example.healthassistant.data

import android.content.Context
import android.util.Log
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.example.healthassistant.worker.ModelDownloadWorker
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import com.google.mediapipe.tasks.genai.llminference.LlmInferenceSession
import com.google.mediapipe.tasks.genai.llminference.GraphOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.util.UUID

// Gallery-style model instance holder
data class LlmModelInstance(val engine: LlmInference, var session: LlmInferenceSession)

class LLMManager(private val context: Context) {
    
    private val workManager = WorkManager.getInstance(context)
    private val huggingFaceAuth = HuggingFaceAuth(context)
    private val _modelsState = MutableStateFlow<Map<String, ModelDownloadProgress>>(emptyMap())
    val modelsState: StateFlow<Map<String, ModelDownloadProgress>> = _modelsState.asStateFlow()
    
    private val _selectedModel = MutableStateFlow<LLMModel?>(null)
    val selectedModel: StateFlow<LLMModel?> = _selectedModel.asStateFlow()
    
    private var currentLlmInference: LlmInference? = null
    
    companion object {
        private const val TAG = "LLMManager"
    }
    
    init {
        // Initialize models state
        updateModelsState()
    }
    
    fun downloadModel(model: LLMModel): UUID {
        val inputData = Data.Builder()
            .putString(ModelDownloadWorker.KEY_MODEL_NAME, model.name)
            .putString(ModelDownloadWorker.KEY_MODEL_URL, model.url)
            .putString(ModelDownloadWorker.KEY_MODEL_FILE_NAME, model.modelFile)
            .putLong(ModelDownloadWorker.KEY_MODEL_SIZE, model.sizeInBytes)
        
        // Add HuggingFace token if available
        huggingFaceAuth.getToken()?.let { token ->
            inputData.putString(ModelDownloadWorker.KEY_ACCESS_TOKEN, token)
        }
        
        val workData = inputData.build()
        
        val downloadRequest = OneTimeWorkRequestBuilder<ModelDownloadWorker>()
            .setInputData(workData)
            .addTag(model.name)
            .build()
        
        workManager.enqueue(downloadRequest)
        
        // Update state to downloading
        updateModelProgress(model.name, ModelDownloadProgress(
            status = ModelDownloadStatus.DOWNLOADING,
            totalBytes = model.sizeInBytes
        ))
        
        // Observe work progress
        observeWorkProgress(downloadRequest.id, model)
        
        return downloadRequest.id
    }
    
    private fun observeWorkProgress(workId: UUID, model: LLMModel) {
        workManager.getWorkInfoByIdLiveData(workId).observeForever { workInfo ->
            when (workInfo?.state) {
                WorkInfo.State.RUNNING -> {
                    val progress = workInfo.progress.getInt(ModelDownloadWorker.KEY_PROGRESS, 0)
                    val downloadedBytes = workInfo.progress.getLong(ModelDownloadWorker.KEY_DOWNLOADED_BYTES, 0L)
                    updateModelProgress(model.name, ModelDownloadProgress(
                        status = ModelDownloadStatus.DOWNLOADING,
                        progress = progress / 100f,
                        downloadedBytes = downloadedBytes,
                        totalBytes = model.sizeInBytes
                    ))
                }
                WorkInfo.State.SUCCEEDED -> {
                    updateModelProgress(model.name, ModelDownloadProgress(
                        status = ModelDownloadStatus.DOWNLOADED,
                        progress = 1f,
                        downloadedBytes = model.sizeInBytes,
                        totalBytes = model.sizeInBytes
                    ))
                    Log.d(TAG, "Model ${model.name} downloaded successfully")
                }
                WorkInfo.State.FAILED -> {
                    val errorMessage = workInfo.outputData.getString(ModelDownloadWorker.KEY_ERROR_MESSAGE)
                    updateModelProgress(model.name, ModelDownloadProgress(
                        status = ModelDownloadStatus.FAILED,
                        errorMessage = errorMessage
                    ))
                    Log.e(TAG, "Model ${model.name} download failed: $errorMessage")
                }
                WorkInfo.State.CANCELLED -> {
                    updateModelProgress(model.name, ModelDownloadProgress(
                        status = ModelDownloadStatus.NOT_DOWNLOADED
                    ))
                }
                else -> { /* Other states */ }
            }
        }
    }
    
    fun cancelDownload(modelName: String) {
        workManager.cancelAllWorkByTag(modelName)
        updateModelProgress(modelName, ModelDownloadProgress(
            status = ModelDownloadStatus.NOT_DOWNLOADED
        ))
    }
    
    fun deleteModel(model: LLMModel) {
        // Cancel any ongoing download
        cancelDownload(model.name)
        
        // Delete the file
        val modelFile = File(model.getFilePath(context))
        if (modelFile.exists()) {
            modelFile.delete()
        }
        
        // Clean up inference if this was the selected model
        if (_selectedModel.value?.name == model.name) {
            cleanupCurrentModel()
            _selectedModel.value = null
        }
        
        updateModelProgress(model.name, ModelDownloadProgress(
            status = ModelDownloadStatus.NOT_DOWNLOADED
        ))
    }
    
    suspend fun initializeModel(model: LLMModel): Boolean {
        return try {
            val modelFile = File(model.getFilePath(context))
            if (!modelFile.exists()) {
                Log.e(TAG, "Model ${model.name} file does not exist")
                updateModelProgress(model.name, ModelDownloadProgress(
                    status = ModelDownloadStatus.NOT_DOWNLOADED
                ))
                return false
            }
            
            // Check file size more strictly for initialization
            val actualSize = modelFile.length()
            val expectedSize = model.sizeInBytes
            val sizeDifference = kotlin.math.abs(actualSize - expectedSize)
            val tolerancePercent = 0.001 // 0.1% tolerance - stricter for init
            
            if (sizeDifference > expectedSize * tolerancePercent) {
                Log.e(TAG, "Model ${model.name} file size mismatch. Expected: $expectedSize, Actual: $actualSize")
                Log.e(TAG, "Size difference: $sizeDifference bytes (${(sizeDifference.toDouble() / expectedSize * 100)}%)")
                
                // File is likely corrupted, mark for re-download
                updateModelProgress(model.name, ModelDownloadProgress(
                    status = ModelDownloadStatus.NOT_DOWNLOADED,
                    errorMessage = "Model file corrupted (size mismatch). Please re-download."
                ))
                
                // Delete the corrupted file
                modelFile.delete()
                Log.i(TAG, "Deleted corrupted model file: ${modelFile.absolutePath}")
                
                return false
            }
            
            updateModelProgress(model.name, ModelDownloadProgress(
                status = ModelDownloadStatus.INITIALIZING
            ))
            
            // Clean up any existing model
            cleanupCurrentModel()
            
            // Initialize the new model with error handling
            val modelPath = model.getFilePath(context)
            Log.d(TAG, "Initializing model at path: $modelPath")
            Log.d(TAG, "Model config - MaxTokens: ${model.defaultConfig.maxTokens}, TopK: ${model.defaultConfig.topK}, Temp: ${model.defaultConfig.temperature}")
            
            // Gallery approach: Inference-level configuration (model path, max tokens, backend, max images)
            val llmOptions = LlmInference.LlmInferenceOptions.builder()
                .setModelPath(modelPath)
                .setMaxTokens(model.defaultConfig.maxTokens)
                .setPreferredBackend(LlmInference.Backend.GPU)
                .setMaxNumImages(if (model.llmSupportImage) 8 else 0)
                .build()
            
            Log.d(TAG, "Creating LlmInference with Gallery-style configuration...")
            currentLlmInference = LlmInference.createFromOptions(context, llmOptions)
            Log.d(TAG, "LlmInference created successfully")
            
            // Gallery approach: Create session with GraphOptions - CRITICAL for signature validation
            val sessionOptions = LlmInferenceSession.LlmInferenceSessionOptions.builder()
                .setTopK(model.defaultConfig.topK)
                .setTopP(model.defaultConfig.topP)
                .setTemperature(model.defaultConfig.temperature)
                .setGraphOptions(
                    GraphOptions.builder()
                        .setEnableVisionModality(model.llmSupportImage)
                        .build()
                )
                .build()
            
            Log.d(TAG, "Creating session with GraphOptions...")
            val inferenceInstance = currentLlmInference ?: throw IllegalStateException("Inference instance is null")
            val session = LlmInferenceSession.createFromOptions(inferenceInstance, sessionOptions)
            Log.d(TAG, "Session created successfully with vision modality: ${model.llmSupportImage}")
            
            // Store both inference and session
            _selectedModel.value = model.copy(instance = LlmModelInstance(inferenceInstance, session))
            
            updateModelProgress(model.name, ModelDownloadProgress(
                status = ModelDownloadStatus.READY
            ))
            
            Log.d(TAG, "Model ${model.name} initialized successfully")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize model ${model.name}", e)
            
            // Check if this is a ZIP archive error (corrupted file)
            val errorMessage = e.message ?: ""
            val isCorruptedFile = errorMessage.contains("zip archive", ignoreCase = true) ||
                                 errorMessage.contains("MediaPipeTAsksStatus='104'", ignoreCase = true) ||
                                 errorMessage.contains("Unable to open", ignoreCase = true)
            
            if (isCorruptedFile) {
                Log.e(TAG, "Model file appears to be corrupted, marking for re-download")
                
                // Delete the corrupted file
                val modelFile = File(model.getFilePath(context))
                if (modelFile.exists()) {
                    modelFile.delete()
                    Log.i(TAG, "Deleted corrupted model file: ${modelFile.absolutePath}")
                }
                
                updateModelProgress(model.name, ModelDownloadProgress(
                    status = ModelDownloadStatus.NOT_DOWNLOADED,
                    errorMessage = "Model file corrupted. Please re-download."
                ))
            } else {
                // Other initialization errors - keep file but show error
                updateModelProgress(model.name, ModelDownloadProgress(
                    status = ModelDownloadStatus.DOWNLOADED,
                    errorMessage = "Initialization failed: ${e.message}"
                ))
            }
            
            // Clean up failed instance
            cleanupCurrentModel()
            _selectedModel.value = null
            false
        }
    }
    
    suspend fun generateHealthReport(healthData: String): String? {
        return try {
            val model = _selectedModel.value ?: return null
            val instance = model.instance as? LlmModelInstance ?: return null
            
            val prompt = """
                Sen sağlık verilerini analiz eden bir AI asistanısın. Aşağıdaki sağlık verilerini analiz et ve kullanıcıya kısa, anlaşılır bir rapor sun:

                Sağlık Verileri:
                $healthData

                Lütfen şu noktalara odaklan:
                1. Genel sağlık durumu değerlendirmesi
                2. Dikkat edilmesi gereken noktalar
                3. Basit öneriler
                4. Olumlu yönler

                Raporu Türkçe, kısa ve anlaşılır bir şekilde hazırla.
            """.trimIndent()
            
            // Gallery approach: Use session for inference - check API changes
            Log.d(TAG, "Generating response with session, prompt length: ${prompt.length}")
            instance.session.addQueryChunk(prompt)
            val response = instance.session.generateResponse()
            Log.d(TAG, "Response generated successfully, length: ${response?.length ?: 0}")
            response
            
        } catch (e: Exception) {
            Log.e(TAG, "Error generating health report", e)
            null
        }
    }
    
    private fun cleanupCurrentModel() {
        try {
            val model = _selectedModel.value
            val instance = model?.instance as? LlmModelInstance
            if (instance != null) {
                Log.d(TAG, "Cleaning up session and inference")
                instance.session.close()
                instance.engine.close()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error during model cleanup", e)
        }
        
        currentLlmInference?.close()
        currentLlmInference = null
    }
    
    private fun updateModelProgress(modelName: String, progress: ModelDownloadProgress) {
        val currentState = _modelsState.value.toMutableMap()
        currentState[modelName] = progress
        _modelsState.value = currentState
    }
    
    private fun updateModelsState() {
        val currentState = _modelsState.value.toMutableMap()
        
        GemmaModels.ALL_MODELS.forEach { model ->
            // Don't overwrite existing states that are in progress
            val existingState = currentState[model.name]
            if (existingState?.status in listOf(
                ModelDownloadStatus.DOWNLOADING, 
                ModelDownloadStatus.INITIALIZING
            )) {
                return@forEach // Skip updating this model, keep current state
            }
            
            val modelFile = File(model.getFilePath(context))
            val status = when {
                modelFile.exists() -> {
                    // Be more lenient with file size check
                    val actualSize = modelFile.length()
                    val expectedSize = model.sizeInBytes
                    val sizeDifference = kotlin.math.abs(actualSize - expectedSize)
                    val tolerancePercent = 0.01 // 1% tolerance
                    
                    if (sizeDifference <= expectedSize * tolerancePercent) {
                        ModelDownloadStatus.DOWNLOADED
                    } else {
                        Log.w(TAG, "Model ${model.name} size mismatch but file exists. Size: $actualSize, Expected: $expectedSize")
                        ModelDownloadStatus.DOWNLOADED // Still consider it downloaded
                    }
                }
                else -> ModelDownloadStatus.NOT_DOWNLOADED
            }
            
            currentState[model.name] = ModelDownloadProgress(
                status = status,
                progress = if (status == ModelDownloadStatus.DOWNLOADED) 1f else 0f,
                downloadedBytes = if (status == ModelDownloadStatus.DOWNLOADED) modelFile.length() else 0L,
                totalBytes = model.sizeInBytes
            )
        }
        
        _modelsState.value = currentState
    }
    
    fun setSelectedModel(model: LLMModel?) {
        _selectedModel.value = model
    }
    
    fun setHuggingFaceToken(token: String) {
        huggingFaceAuth.saveToken(token)
    }
    
    fun getHuggingFaceToken(): String? {
        return huggingFaceAuth.getToken()
    }
    
    fun clearHuggingFaceToken() {
        huggingFaceAuth.clearToken()
    }
    
    fun hasHuggingFaceToken(): Boolean {
        return huggingFaceAuth.hasToken()
    }
}