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
import com.example.healthassistant.data.LLMManager
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
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
    val errorMessage: String? = null,
    val isGeneratingReport: Boolean = false,
    val healthReport: String? = null,
    val streamingHealthReport: String = "",
    val canCancelReport: Boolean = false,
    val showReportDialog: Boolean = false
)

class HealthDataViewModel(
    val healthConnectManager: HealthConnectManager,
    private val llmManager: LLMManager
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
    
    fun generateHealthReport() {
        // Gallery approach: Use Dispatchers.Default for heavy operations
        viewModelScope.launch(Dispatchers.Default) {
            uiState = uiState.copy(
                isGeneratingReport = true, 
                canCancelReport = true,
                showReportDialog = false // Close any open dialog
            )
            
            try {
                Log.d("HealthDataViewModel", "=== Starting Health Report Generation ===")
                val healthData = buildHealthDataString()
                Log.d("HealthDataViewModel", "Health data prepared, length: ${healthData.length}")
                Log.d("HealthDataViewModel", "Health data preview: ${healthData.take(200)}")
                
                // Gallery approach: Async streaming with callbacks
                // But don't show streaming text, just final result
                llmManager.generateHealthReport(healthData) { partialResult, done ->
                    Log.d("HealthDataViewModel", "LLM Callback - done: $done, result length: ${partialResult.length}")
                    Log.d("HealthDataViewModel", "LLM Callback - result preview: ${partialResult.take(100)}")
                    
                    if (done) {
                        uiState = uiState.copy(
                            isGeneratingReport = false,
                            healthReport = partialResult,
                            canCancelReport = false,
                            showReportDialog = true // Auto-show dialog when done
                        )
                        Log.d("HealthDataViewModel", "Report saved to uiState, length: ${partialResult.length}")
                    }
                    // Don't update streamingHealthReport - keep it hidden
                }
                
            } catch (e: Exception) {
                Log.e("HealthDataViewModel", "Error generating health report", e)
                uiState = uiState.copy(
                    isGeneratingReport = false,
                    canCancelReport = false,
                    errorMessage = "Error generating report: ${e.message}"
                )
            }
        }
    }
    
    fun cancelHealthReport() {
        viewModelScope.launch(Dispatchers.Default) {
            llmManager.cancelHealthReportGeneration()
            uiState = uiState.copy(
                isGeneratingReport = false,
                canCancelReport = false,
                streamingHealthReport = ""
            )
        }
    }
    
    fun showHealthReport() {
        uiState = uiState.copy(showReportDialog = true)
    }
    
    fun hideHealthReport() {
        uiState = uiState.copy(showReportDialog = false)
    }
    
    fun clearHealthReport() {
        uiState = uiState.copy(
            healthReport = null, 
            streamingHealthReport = "",
            showReportDialog = false
        )
    }
    
    private fun buildHealthDataString(): String {
        return buildString {
            append("=== DAILY HEALTH DATA ===\n\n")
            
            append("💓 HEART RATE:\n")
            append("Latest heart rate: ${uiState.heartRate?.let { "$it BPM" } ?: "No data"}\n\n")
            
            append("🚶 ACTIVITY DATA:\n")
            append("Daily steps: ${uiState.stepCount} steps\n")
            append("Calories burned: ${String.format("%.0f", uiState.calories)} kcal\n")
            append("Exercise sessions: ${uiState.exerciseCount} sessions\n")
            uiState.activitySummary?.let { summary ->
                append("Distance: ${String.format("%.1f", summary.distance)} km\n")
                append("Active minutes: ${summary.activeMinutes} minutes\n")
            }
            append("\n")
            
            append("😴 SLEEP DATA:\n")
            uiState.sleepDuration?.let { duration ->
                append("Total sleep: ${duration.toHours()}h ${duration.toMinutes() % 60}m\n")
            } ?: append("No sleep data\n")
            
            uiState.detailedSleep?.let { sleep ->
                append("Deep sleep: ${sleep.deepSleep.toHours()}h ${sleep.deepSleep.toMinutes() % 60}m\n")
                append("REM sleep: ${sleep.remSleep.toHours()}h ${sleep.remSleep.toMinutes() % 60}m\n")
                append("Light sleep: ${sleep.lightSleep.toHours()}h ${sleep.lightSleep.toMinutes() % 60}m\n")
            }
            append("\n")
            
            append("💧 HYDRATION:\n")
            append("Water intake: ${String.format("%.1f", uiState.hydration)} L\n\n")
            
            uiState.bloodPressure?.let { (systolic, diastolic) ->
                append("🩸 BLOOD PRESSURE:\n")
                append("${systolic.toInt()}/${diastolic.toInt()} mmHg\n\n")
            }
            
            uiState.oxygenSaturation?.let { spo2 ->
                append("🫁 BLOOD OXYGEN:\n")
                append("SpO2: ${spo2.toInt()}%\n\n")
            }
            
            uiState.bodyComposition?.let { body ->
                append("⚖️ BODY COMPOSITION:\n")
                append("Weight: ${String.format("%.1f", body.weight)} kg\n")
                append("BMI: ${String.format("%.1f", body.bmi)}\n")
                body.bodyFatPercentage?.let { fat ->
                    append("Body fat percentage: ${String.format("%.1f", fat)}%\n")
                }
                append("\n")
            }
            
            uiState.goalsProgress?.let { goals ->
                append("🎯 GOAL PROGRESS:\n")
                append("Step goal: ${goals.stepCurrent}/${goals.stepGoal} (${String.format("%.1f", (goals.stepCurrent.toFloat() / goals.stepGoal.toFloat() * 100))}%)\n")
                append("Calorie goal: ${String.format("%.0f", goals.calorieCurrent)}/${String.format("%.0f", goals.calorieGoal)} (${String.format("%.1f", (goals.calorieCurrent.toFloat() / goals.calorieGoal.toFloat() * 100))}%)\n")
                append("Active minute goal: ${goals.activeMinuteCurrent}/${goals.activeMinuteGoal} (${String.format("%.1f", (goals.activeMinuteCurrent.toFloat() / goals.activeMinuteGoal.toFloat() * 100))}%)\n")
            }
        }
    }
}