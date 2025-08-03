package com.example.healthassistant.viewmodel

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.BloodPressureRecord
import androidx.health.connect.client.records.BodyFatRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.HeightRecord
import androidx.health.connect.client.records.HydrationRecord
import androidx.health.connect.client.records.OxygenSaturationRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.records.DistanceRecord
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.healthassistant.data.BodyCompositionData
import com.example.healthassistant.data.DetailedSleepData
import com.example.healthassistant.data.GoalsProgressData
import com.example.healthassistant.data.ActivitySummaryData
import com.example.healthassistant.data.HealthConnectAvailability
import com.example.healthassistant.data.HealthConnectManager
import kotlinx.coroutines.launch
import java.time.Duration

data class HealthDataUiState(
    val permissionsGranted: Boolean = false,
    val heartRate: Long? = null,
    val stepCount: Long = 0,
    val calories: Double = 0.0,
    val sleepDuration: Duration? = null,
    val exerciseCount: Int = 0,
    val hydration: Double = 0.0,
    val bloodPressure: Pair<Double, Double>? = null,
    val oxygenSaturation: Double? = null,
    val bodyComposition: BodyCompositionData? = null,
    val detailedSleep: DetailedSleepData? = null,
    val goalsProgress: GoalsProgressData? = null,
    val activitySummary: ActivitySummaryData? = null,
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
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class),
        HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class),
        HealthPermission.getReadPermission(SleepSessionRecord::class),
        HealthPermission.getReadPermission(ExerciseSessionRecord::class),
        HealthPermission.getReadPermission(HydrationRecord::class),
        HealthPermission.getReadPermission(BloodPressureRecord::class),
        HealthPermission.getReadPermission(OxygenSaturationRecord::class),
        HealthPermission.getReadPermission(WeightRecord::class),
        HealthPermission.getReadPermission(HeightRecord::class),
        HealthPermission.getReadPermission(BodyFatRecord::class),
        HealthPermission.getReadPermission(DistanceRecord::class)
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
                    errorMessage = "Error checking permissions: ${e.message}"
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
                val calories = healthConnectManager.getTodayCalories()
                val sleepDuration = healthConnectManager.getLastNightSleep()
                val exerciseSessions = healthConnectManager.getTodayExerciseSessions()
                val hydration = healthConnectManager.getTodayHydration()
                val bloodPressure = healthConnectManager.getLatestBloodPressure()
                val oxygenSaturation = healthConnectManager.getLatestOxygenSaturation()
                val bodyComposition = healthConnectManager.getBodyComposition()
                val detailedSleep = healthConnectManager.getDetailedSleepData()
                val goalsProgress = healthConnectManager.getTodayGoalsProgress()
                val activitySummary = healthConnectManager.getActivitySummary()
                
                uiState = uiState.copy(
                    heartRate = heartRate,
                    stepCount = stepCount,
                    calories = calories,
                    sleepDuration = sleepDuration,
                    exerciseCount = exerciseSessions.size,
                    hydration = hydration,
                    bloodPressure = bloodPressure,
                    oxygenSaturation = oxygenSaturation,
                    bodyComposition = bodyComposition,
                    detailedSleep = detailedSleep,
                    goalsProgress = goalsProgress,
                    activitySummary = activitySummary,
                    isLoading = false,
                    errorMessage = null
                )
            } catch (e: Exception) {
                Log.e("HealthDataViewModel", "Error loading health data", e)
                uiState = uiState.copy(
                    isLoading = false,
                    errorMessage = "Error loading health data: ${e.message}"
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
    
    fun addWater() {
        viewModelScope.launch {
            try {
                healthConnectManager.addWater(0.25) // 250ml = 0.25L
                val newHydration = healthConnectManager.getTodayHydration()
                uiState = uiState.copy(hydration = newHydration)
            } catch (e: Exception) {
                Log.e("HealthDataViewModel", "Error adding water", e)
                uiState = uiState.copy(
                    errorMessage = "Error adding water: ${e.message}"
                )
            }
        }
    }
}