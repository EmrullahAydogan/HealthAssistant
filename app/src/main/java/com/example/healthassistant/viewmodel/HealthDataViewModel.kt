package com.example.healthassistant.viewmodel

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.healthassistant.data.HealthConnectAvailability
import com.example.healthassistant.data.HealthConnectManager
import kotlinx.coroutines.launch

data class HealthDataUiState(
    val permissionsGranted: Boolean = false,
    val heartRate: Long? = null,
    val stepCount: Long = 0,
    val availability: HealthConnectAvailability = HealthConnectAvailability.NOT_SUPPORTED,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class HealthDataViewModel(
    val healthConnectManager: HealthConnectManager
) : ViewModel() {

    var uiState by mutableStateOf(HealthDataUiState())
        private set

    val permissions = setOf(
        HealthPermission.getReadPermission(HeartRateRecord::class),
        HealthPermission.getReadPermission(StepsRecord::class)
    )

    init {
        uiState = uiState.copy(availability = healthConnectManager.availability.value)
        checkPermissions()
    }

    fun checkPermissions() {
        viewModelScope.launch {
            uiState = uiState.copy(isLoading = true)
            try {
                val granted = healthConnectManager.hasAllPermissions(permissions)
                uiState = uiState.copy(
                    permissionsGranted = granted,
                    isLoading = false
                )
                
                if (granted) {
                    loadHealthData()
                }
            } catch (e: Exception) {
                Log.e("HealthDataViewModel", "Error checking permissions", e)
                uiState = uiState.copy(
                    isLoading = false,
                    errorMessage = "İzinler kontrol edilirken hata oluştu: ${e.message}"
                )
            }
        }
    }

    fun onPermissionsResult() {
        checkPermissions()
    }

    private fun loadHealthData() {
        viewModelScope.launch {
            uiState = uiState.copy(isLoading = true)
            try {
                val heartRate = healthConnectManager.getLatestHeartRate()
                val stepCount = healthConnectManager.getTodaySteps()
                
                uiState = uiState.copy(
                    heartRate = heartRate,
                    stepCount = stepCount,
                    isLoading = false,
                    errorMessage = null
                )
            } catch (e: Exception) {
                Log.e("HealthDataViewModel", "Error loading health data", e)
                uiState = uiState.copy(
                    isLoading = false,
                    errorMessage = "Sağlık verileri yüklenirken hata oluştu: ${e.message}"
                )
            }
        }
    }

    fun refreshData() {
        if (uiState.permissionsGranted) {
            loadHealthData()
        }
    }

    fun clearError() {
        uiState = uiState.copy(errorMessage = null)
    }
}