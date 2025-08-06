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
import kotlinx.coroutines.delay
import java.io.File
import java.util.UUID
import com.example.healthassistant.common.LLMUtils

// Gallery-style model instance holder
data class LlmModelInstance(val engine: LlmInference, var session: LlmInferenceSession)

typealias ResultListener = (partialResult: String, done: Boolean) -> Unit
typealias CleanUpListener = () -> Unit

class LLMManager(private val context: Context) {
    
    private val workManager = WorkManager.getInstance(context)
    private val huggingFaceAuth = HuggingFaceAuth(context)
    private val _modelsState = MutableStateFlow<Map<String, ModelDownloadProgress>>(emptyMap())
    val modelsState: StateFlow<Map<String, ModelDownloadProgress>> = _modelsState.asStateFlow()
    
    private val _selectedModel = MutableStateFlow<LLMModel?>(null)
    val selectedModel: StateFlow<LLMModel?> = _selectedModel.asStateFlow()
    
    private val _modelInitializationState = MutableStateFlow<ModelInitState>(ModelInitState.NotInitialized)
    val modelInitializationState: StateFlow<ModelInitState> = _modelInitializationState.asStateFlow()
    
    private var currentLlmInference: LlmInference? = null
    private val cleanUpListeners: MutableMap<String, CleanUpListener> = mutableMapOf()
    
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
            Log.d(TAG, "=== Starting Gemma 3n Model Initialization: ${model.name} ===")
            _modelInitializationState.value = ModelInitState.Initializing
            
            // Check if model is already initialized and ready
            val currentModel = _selectedModel.value
            if (currentModel?.name == model.name && currentModel.instance != null) {
                Log.d(TAG, "Model ${model.name} already initialized, skipping...")
                _modelInitializationState.value = ModelInitState.Ready
                return true
            }
            
            val modelFile = File(model.getFilePath(context))
            if (!modelFile.exists()) {
                Log.e(TAG, "Model ${model.name} file does not exist")
                _modelInitializationState.value = ModelInitState.Error("Model file not found")
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
            
            // Gemma 3n specific optimizations: GPU backend with optimal settings for health analysis
            val preferredBackend = LlmInference.Backend.GPU
            
            // Gemma 3n specific token limits based on model variant
            val optimizedMaxTokens = when {
                model.name.contains("E4B") -> 1280  // Premium model: higher quality, controlled length
                model.name.contains("E2B") -> 1536  // Balanced model: good balance
                else -> model.defaultConfig.maxTokens
            }
            
            val llmOptions = LlmInference.LlmInferenceOptions.builder()
                .setModelPath(modelPath)
                .setMaxTokens(optimizedMaxTokens)
                .setPreferredBackend(preferredBackend)
                .setMaxNumImages(if (model.llmSupportImage) 3 else 0) // Optimized for Gemma 3n vision processing
                .build()
            
            Log.d(TAG, "Creating Gemma 3n LlmInference with optimized configuration (maxTokens: $optimizedMaxTokens)...")
            currentLlmInference = LlmInference.createFromOptions(context, llmOptions)
            Log.d(TAG, "Gemma 3n LlmInference created successfully")
            
            // Gemma 3n specific session configuration - optimized for consistent English health analysis
            val gemma3nOptimizedTemp = when {
                model.name.contains("E4B") -> 0.5f  // Premium: very focused responses
                model.name.contains("E2B") -> 0.6f  // Balanced: good consistency
                else -> model.defaultConfig.temperature
            }
            
            val sessionOptions = LlmInferenceSession.LlmInferenceSessionOptions.builder()
                .setTopK(model.defaultConfig.topK)
                .setTopP(model.defaultConfig.topP)
                .setTemperature(gemma3nOptimizedTemp)
                .setGraphOptions(
                    GraphOptions.builder()
                        .setEnableVisionModality(model.llmSupportImage)
                        .build()
                )
                .build()
            
            Log.d(TAG, "Creating Gemma 3n session with optimized GraphOptions...")
            val inferenceInstance = currentLlmInference ?: throw IllegalStateException("Gemma 3n inference instance is null")
            val session = LlmInferenceSession.createFromOptions(inferenceInstance, sessionOptions)
            Log.d(TAG, "Gemma 3n session created successfully with vision modality: ${model.llmSupportImage}, temp: $gemma3nOptimizedTemp")
            
            // Store both inference and session
            val modelWithInstance = model.copy(instance = LlmModelInstance(inferenceInstance, session))
            _selectedModel.value = modelWithInstance
            
            updateModelProgress(model.name, ModelDownloadProgress(
                status = ModelDownloadStatus.READY
            ))
            
            _modelInitializationState.value = ModelInitState.Ready
            Log.d(TAG, "Gemma 3n Model ${model.name} initialized successfully and ready for inference")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize model ${model.name}", e)
            
            // Check if this is a ZIP archive error (corrupted file)
            val errorMessage = LLMUtils.cleanUpMediaPipeError(e.message ?: "")
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
            _modelInitializationState.value = ModelInitState.Error(errorMessage)
            false
        }
    }
    
    
    fun runHealthInference(
        model: LLMModel,
        healthData: String,
        resultListener: ResultListener,
        cleanUpListener: CleanUpListener
    ) {
        val instance = model.instance as? LlmModelInstance ?: return
        
        // Set cleanup listener
        cleanUpListeners[model.name] = cleanUpListener
        
        // Use optimized prompt from LLMUtils
        val optimizedPrompt = LLMUtils.createOptimizedHealthPrompt(healthData)
        
        Log.d(TAG, "Starting optimized health report generation, prompt length: ${optimizedPrompt.length}")
        
        try {
            val session = instance.session
            session.addQueryChunk(optimizedPrompt)
            
            val processedResultCallback: ResultListener = { partialResult: String, done: Boolean ->
                val processed = if (done) {
                    val cleanedResult = LLMUtils.processHealthResponse(partialResult)
                    // Validate response quality
                    if (LLMUtils.validateHealthResponse(cleanedResult)) {
                        cleanedResult
                    } else {
                        Log.w(TAG, "Response validation failed, using original")
                        cleanedResult // Still use processed version even if validation fails
                    }
                } else {
                    partialResult
                }
                resultListener(processed, done)
            }
            
            session.generateResponseAsync(processedResultCallback)
        } catch (e: Exception) {
            Log.e(TAG, "Error in health inference", e)
            resultListener("Error generating report: ${e.message}", true)
        }
    }
    
    suspend fun generateHealthReport(
        healthData: String,
        onPartialResult: (String, Boolean) -> Unit
    ) {
        try {
            Log.d(TAG, "=== generateHealthReport called ===")
            
            val model = _selectedModel.value ?: run {
                Log.e(TAG, "No model selected!")
                onPartialResult("No model selected. Please select an LLM model first.", true)
                return
            }
            Log.d(TAG, "Selected model: ${model.name}")
            
            val instance = model.instance as? LlmModelInstance ?: run {
                Log.e(TAG, "Model not initialized! Model: ${model.name}")
                onPartialResult("Model not initialized. Please initialize the model first.", true)
                return
            }
            
            // Wait for model to be ready (similar to Gallery approach)
            while (model.instance == null) {
                delay(100)
            }
            delay(200) // Short delay for stability
            
            Log.d(TAG, "Model instance ready: ${model.name}")
            
            var accumulatedResult = StringBuilder()
            
            runHealthInference(
                model = model,
                healthData = healthData,
                resultListener = { partialResult: String, done: Boolean ->
                    Log.d(TAG, "=== Optimized LLM Callback ===")
                    Log.d(TAG, "done: $done, partial length: ${partialResult.length}")
                    
                    if (partialResult.isNotBlank()) {
                        accumulatedResult.append(partialResult)
                    }
                    
                    if (done) {
                        val finalResult = accumulatedResult.toString()
                        Log.d(TAG, "FINAL RESULT - length: ${finalResult.length}")
                        
                        if (finalResult.isNotBlank()) {
                            onPartialResult(finalResult, true)
                        } else {
                            onPartialResult("No response generated. Please try again.", true)
                        }
                    }
                },
                cleanUpListener = {
                    Log.d(TAG, "Health inference cleanup completed")
                }
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "Error generating health report", e)
            onPartialResult("Error generating report: ${e.message}", true)
        }
    }
    
    fun cancelHealthReportGeneration() {
        try {
            val model = _selectedModel.value
            val instance = model?.instance as? LlmModelInstance
            if (instance != null) {
                Log.d(TAG, "Cancelling health report generation")
                instance.session.cancelGenerateResponseAsync()
                
                // Trigger cleanup listener
                model.name.let { modelName ->
                    cleanUpListeners[modelName]?.invoke()
                    cleanUpListeners.remove(modelName)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error cancelling health report generation", e)
        }
    }
    
    fun resetSession(model: LLMModel) {
        try {
            Log.d(TAG, "Resetting Gemma 3n session for model '${model.name}'")
            
            val instance = model.instance as? LlmModelInstance ?: return
            val session = instance.session
            session.close()
            
            val inference = instance.engine
            
            // Apply same Gemma 3n optimizations as in initialization
            val gemma3nOptimizedTemp = when {
                model.name.contains("E4B") -> 0.5f  // Premium: very focused responses
                model.name.contains("E2B") -> 0.6f  // Balanced: good consistency
                else -> model.defaultConfig.temperature
            }
            
            val sessionOptions = LlmInferenceSession.LlmInferenceSessionOptions.builder()
                .setTopK(model.defaultConfig.topK)
                .setTopP(model.defaultConfig.topP)
                .setTemperature(gemma3nOptimizedTemp)
                .setGraphOptions(
                    GraphOptions.builder()
                        .setEnableVisionModality(model.llmSupportImage)
                        .build()
                )
                .build()
                
            val newSession = LlmInferenceSession.createFromOptions(inference, sessionOptions)
            
            // Update the instance with the new session
            _selectedModel.value = model.copy(instance = LlmModelInstance(inference, newSession))
            
            Log.d(TAG, "Gemma 3n session reset completed with optimized temperature: $gemma3nOptimizedTemp")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to reset Gemma 3n session", e)
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
                
                // Clean up listener
                model.name.let { modelName ->
                    cleanUpListeners.remove(modelName)
                }
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
    
    fun isModelReady(): Boolean {
        val model = _selectedModel.value
        return model?.instance != null && _modelInitializationState.value is ModelInitState.Ready
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