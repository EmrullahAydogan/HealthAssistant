package com.example.healthassistant.data

import android.content.Context
import android.os.Build
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.runtime.mutableStateOf
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.HealthConnectClient.Companion.SDK_AVAILABLE
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.aggregate.AggregationResult
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.BloodPressureRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.HydrationRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.health.connect.client.units.Energy
import androidx.health.connect.client.units.Volume
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

const val MIN_SUPPORTED_SDK = Build.VERSION_CODES.O_MR1

class HealthConnectManager(private val context: Context) {
    private val healthConnectClient by lazy { HealthConnectClient.getOrCreate(context) }
    private val hydrationPreferences = HydrationPreferences(context)

    var availability = mutableStateOf(HealthConnectAvailability.NOT_SUPPORTED)
        private set

    init {
        checkAvailability()
    }

    fun checkAvailability() {
        availability.value = when {
            HealthConnectClient.getSdkStatus(context) == SDK_AVAILABLE -> HealthConnectAvailability.INSTALLED
            isSupported() -> HealthConnectAvailability.NOT_INSTALLED
            else -> HealthConnectAvailability.NOT_SUPPORTED
        }
    }

    suspend fun hasAllPermissions(permissions: Set<String>): Boolean {
        return healthConnectClient.permissionController.getGrantedPermissions().containsAll(permissions)
    }

    fun requestPermissionsActivityContract(): ActivityResultContract<Set<String>, Set<String>> {
        return PermissionController.createRequestPermissionResultContract()
    }

    suspend fun readHeartRateData(start: Instant, end: Instant): List<HeartRateRecord> {
        val request = ReadRecordsRequest(
            recordType = HeartRateRecord::class,
            timeRangeFilter = TimeRangeFilter.between(start, end)
        )
        val response = healthConnectClient.readRecords(request)
        return response.records
    }

    suspend fun readStepsData(start: Instant, end: Instant): List<StepsRecord> {
        val request = ReadRecordsRequest(
            recordType = StepsRecord::class,
            timeRangeFilter = TimeRangeFilter.between(start, end)
        )
        val response = healthConnectClient.readRecords(request)
        return response.records
    }

    suspend fun aggregateStepsInTimeRange(start: Instant, end: Instant): Long {
        val request = AggregateRequest(
            metrics = setOf(StepsRecord.COUNT_TOTAL),
            timeRangeFilter = TimeRangeFilter.between(start, end)
        )
        val response = healthConnectClient.aggregate(request)
        return response[StepsRecord.COUNT_TOTAL] ?: 0L
    }

    suspend fun getLatestHeartRate(): Long? {
        val end = Instant.now()
        val start = end.minusSeconds(3600) // Son 1 saat
        
        val heartRateRecords = readHeartRateData(start, end)
        return heartRateRecords
            .flatMap { it.samples }
            .maxByOrNull { it.time }
            ?.beatsPerMinute
    }

    suspend fun getTodaySteps(): Long {
        val today = LocalDateTime.now().toLocalDate()
        val start = today.atStartOfDay(ZoneId.systemDefault()).toInstant()
        val end = today.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()
        
        return aggregateStepsInTimeRange(start, end)
    }

    suspend fun getTodayCalories(): Double {
        val today = LocalDateTime.now().toLocalDate()
        val start = today.atStartOfDay(ZoneId.systemDefault()).toInstant()
        val end = today.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()
        
        val activeCaloriesRequest = AggregateRequest(
            metrics = setOf(ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL),
            timeRangeFilter = TimeRangeFilter.between(start, end)
        )
        val totalCaloriesRequest = AggregateRequest(
            metrics = setOf(TotalCaloriesBurnedRecord.ENERGY_TOTAL),
            timeRangeFilter = TimeRangeFilter.between(start, end)
        )
        
        val activeResponse = healthConnectClient.aggregate(activeCaloriesRequest)
        val totalResponse = healthConnectClient.aggregate(totalCaloriesRequest)
        
        // Health Connect API returns energy in kilojoules, convert to calories
        val activeCalories = activeResponse[ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL]?.inKilocalories ?: 0.0
        val totalCalories = totalResponse[TotalCaloriesBurnedRecord.ENERGY_TOTAL]?.inKilocalories ?: 0.0
        
        return maxOf(activeCalories, totalCalories)
    }

    suspend fun getLastNightSleep(): Duration? {
        val now = LocalDateTime.now()
        val yesterday = now.minusDays(1)
        val start = yesterday.toLocalDate().atTime(18, 0).atZone(ZoneId.systemDefault()).toInstant()
        val end = now.toLocalDate().atTime(12, 0).atZone(ZoneId.systemDefault()).toInstant()
        
        val request = ReadRecordsRequest(
            recordType = SleepSessionRecord::class,
            timeRangeFilter = TimeRangeFilter.between(start, end)
        )
        val response = healthConnectClient.readRecords(request)
        
        return response.records
            .maxByOrNull { it.endTime }
            ?.let { Duration.between(it.startTime, it.endTime) }
    }

    suspend fun getTodayExerciseSessions(): List<ExerciseSessionRecord> {
        val today = LocalDateTime.now().toLocalDate()
        val start = today.atStartOfDay(ZoneId.systemDefault()).toInstant()
        val end = today.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()
        
        val request = ReadRecordsRequest(
            recordType = ExerciseSessionRecord::class,
            timeRangeFilter = TimeRangeFilter.between(start, end)
        )
        val response = healthConnectClient.readRecords(request)
        return response.records
    }

    suspend fun getTodayHydration(): Double {
        return hydrationPreferences.getTodayHydration()
    }
    
    fun addWater(amount: Double) {
        hydrationPreferences.addWater(amount)
    }
    
    fun resetTodayHydration() {
        hydrationPreferences.resetTodayHydration()
    }

    suspend fun getLatestBloodPressure(): Pair<Double, Double>? {
        val end = Instant.now()
        val start = end.minusSeconds(86400 * 7) // Son 7 gün
        
        val request = ReadRecordsRequest(
            recordType = BloodPressureRecord::class,
            timeRangeFilter = TimeRangeFilter.between(start, end)
        )
        val response = healthConnectClient.readRecords(request)
        
        return response.records
            .maxByOrNull { it.time }
            ?.let { it.systolic.inMillimetersOfMercury to it.diastolic.inMillimetersOfMercury }
    }

    private fun isSupported() = Build.VERSION.SDK_INT >= MIN_SUPPORTED_SDK
}

enum class HealthConnectAvailability {
    INSTALLED,
    NOT_INSTALLED,
    NOT_SUPPORTED
}