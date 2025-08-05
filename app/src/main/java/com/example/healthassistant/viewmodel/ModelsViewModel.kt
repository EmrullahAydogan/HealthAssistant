package com.example.healthassistant.viewmodel

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.healthassistant.data.LLMManager
import com.example.healthassistant.data.LLMModel
import com.example.healthassistant.data.ModelDownloadProgress
import com.example.healthassistant.data.getFilePath
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ModelsViewModel(
    private val context: Context,
    private val llmManager: LLMManager
) : ViewModel() {
    
    private val prefs: SharedPreferences = context.getSharedPreferences("health_models", Context.MODE_PRIVATE)
    
    val modelsState: StateFlow<Map<String, ModelDownloadProgress>> = llmManager.modelsState
    
    private val _selectedHealthModel = MutableStateFlow<LLMModel?>(null)
    val selectedHealthModel: StateFlow<LLMModel?> = _selectedHealthModel.asStateFlow()
    
    init {
        loadSelectedModel()
    }
    
    fun downloadModel(model: LLMModel) {
        llmManager.downloadModel(model)
    }
    
    fun cancelDownload(model: LLMModel) {
        llmManager.cancelDownload(model.name)
    }
    
    fun deleteModel(model: LLMModel) {
        viewModelScope.launch {
            // If this was the selected health model, clear it
            if (_selectedHealthModel.value?.name == model.name) {
                setSelectedHealthModel(null)
            }
            llmManager.deleteModel(model)
        }
    }
    
    fun setSelectedHealthModel(model: LLMModel?) {
        _selectedHealthModel.value = model
        saveSelectedModel(model)
        
        // If a model is selected, initialize it
        model?.let { selectedModel ->
            viewModelScope.launch {
                llmManager.initializeModel(selectedModel)
            }
        }
    }
    
    fun generateHealthReport(healthData: String, onResult: (String?) -> Unit) {
        viewModelScope.launch {
            var finalResult: String? = null
            llmManager.generateHealthReport(healthData) { partialResult, done ->
                if (done) {
                    finalResult = partialResult
                    onResult(finalResult)
                }
            }
        }
    }
    
    fun setHuggingFaceToken(token: String) {
        llmManager.setHuggingFaceToken(token)
    }
    
    fun getHuggingFaceToken(): String? {
        return llmManager.getHuggingFaceToken()
    }
    
    fun clearHuggingFaceToken() {
        llmManager.clearHuggingFaceToken()
    }
    
    fun hasHuggingFaceToken(): Boolean {
        return llmManager.hasHuggingFaceToken()
    }
    
    private fun saveSelectedModel(model: LLMModel?) {
        prefs.edit()
            .putString("selected_health_model", model?.name)
            .apply()
    }
    
    private fun loadSelectedModel() {
        val modelName = prefs.getString("selected_health_model", null)
        if (modelName != null) {
            val model = com.example.healthassistant.data.GemmaModels.ALL_MODELS.find { it.name == modelName }
            if (model != null) {
                val modelFile = java.io.File(model.getFilePath(context))
                if (modelFile.exists() && modelFile.length() == model.sizeInBytes) {
                    _selectedHealthModel.value = model
                    viewModelScope.launch {
                        llmManager.initializeModel(model)
                    }
                } else {
                    // Model not found or not downloaded, clear the preference
                    prefs.edit().remove("selected_health_model").apply()
                }
            } else {
                // Model not found, clear the preference
                prefs.edit().remove("selected_health_model").apply()
            }
        }
    }
}