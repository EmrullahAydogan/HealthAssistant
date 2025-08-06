package com.example.healthassistant.ui.dashboard

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Hotel
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bloodtype
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.AssignmentInd
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material3.Switch
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.draw.shadow
import com.example.healthassistant.data.HealthConnectAvailability
import com.example.healthassistant.ui.theme.onWarningContainer
import com.example.healthassistant.ui.theme.warningContainer
import com.example.healthassistant.ui.theme.getHealthMetricColors
import com.example.healthassistant.ui.theme.HealthMetricColors
import com.example.healthassistant.data.BodyCompositionData
import com.example.healthassistant.data.DetailedSleepData
import com.example.healthassistant.data.GoalsProgressData
import com.example.healthassistant.data.ActivitySummaryData
import com.example.healthassistant.viewmodel.HealthDataViewModel
import java.time.Duration
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: HealthDataViewModel,
    isDarkTheme: Boolean,
    onThemeToggle: () -> Unit
) {
    val uiState = viewModel.uiState
    val snackbarHostState = remember { SnackbarHostState() }

    val permissionsLauncher = rememberLauncherForActivityResult(
        contract = viewModel.healthConnectManager.requestPermissionsActivityContract()
    ) { _ ->
        viewModel.onPermissionsResult()
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            PremiumTopAppBar(
                onRefresh = { viewModel.refreshData() },
                isLoading = uiState.isLoading,
                isDarkTheme = isDarkTheme,
                onThemeToggle = onThemeToggle
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = if (isDarkTheme) {
                            listOf(
                                MaterialTheme.colorScheme.background,
                                MaterialTheme.colorScheme.surface
                            )
                        } else {
                            listOf(
                                Color(0xFFF8F9FA),
                                Color(0xFFE9ECEF)
                            )
                        }
                    )
                )
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
            item {
                when (uiState.availability) {
                    HealthConnectAvailability.NOT_SUPPORTED -> {
                        NotSupportedMessage()
                    }
                    HealthConnectAvailability.NOT_INSTALLED -> {
                        NotInstalledMessage()
                    }
                    HealthConnectAvailability.INSTALLED -> {
                        if (!uiState.permissionsGranted) {
                            PermissionCard {
                                permissionsLauncher.launch(viewModel.permissions)
                            }
                        } else {
                            DashboardContent(
                                heartRate = uiState.heartRate,
                                stepCount = uiState.stepCount,
                                calories = uiState.calories,
                                sleepDuration = uiState.sleepDuration,
                                exerciseCount = uiState.exerciseCount,
                                hydration = uiState.hydration,
                                bloodPressure = uiState.bloodPressure,
                                oxygenSaturation = uiState.oxygenSaturation,
                                bodyComposition = uiState.bodyComposition,
                                detailedSleep = uiState.detailedSleep,
                                goalsProgress = uiState.goalsProgress,
                                activitySummary = uiState.activitySummary,
                                isLoading = uiState.isLoading,
                                onAddWater = { viewModel.addWater() },
                                onGenerateReport = { viewModel.generateHealthReport() },
                                isGeneratingReport = uiState.isGeneratingReport,
                                healthReport = uiState.healthReport,
                                canCancel = uiState.canCancelReport,
                                onCancelReport = { viewModel.cancelHealthReport() },
                                onReadReport = { viewModel.showHealthReport() },
                                showReportDialog = uiState.showReportDialog,
                                onHideReport = { viewModel.hideHealthReport() }
                            )
                        }
                    }
                }
            }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PremiumTopAppBar(
    onRefresh: () -> Unit,
    isLoading: Boolean,
    isDarkTheme: Boolean,
    onThemeToggle: () -> Unit
) {
    TopAppBar(
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(vertical = 4.dp)
            ) {
                // Premium health icon with gradient background
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFF4CAF50).copy(alpha = 0.2f),
                                    Color(0xFF2196F3).copy(alpha = 0.2f)
                                )
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.HealthAndSafety,
                        contentDescription = "Health Assistant",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
                
                Column {
                    Text(
                        "Health Assistant",
                        color = Color.White,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "AI-Powered Health Analytics",
                        color = Color.White.copy(alpha = 0.8f),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Normal
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primary
        ),
        actions = {
            // Theme toggle switch
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    if (isDarkTheme) Icons.Default.DarkMode else Icons.Default.LightMode,
                    contentDescription = if (isDarkTheme) "Dark Mode" else "Light Mode",
                    tint = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.size(20.dp)
                )
                Switch(
                    checked = isDarkTheme,
                    onCheckedChange = { onThemeToggle() },
                    modifier = Modifier.size(40.dp)
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                IconButton(
                    onClick = onRefresh,
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            Color.White.copy(alpha = 0.1f),
                            RoundedCornerShape(12.dp)
                        )
                ) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = "Refresh Data",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        },
        modifier = Modifier.shadow(
            elevation = 8.dp,
            spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
        )
    )
}

@Composable
fun DashboardContent(
    heartRate: Long?,
    stepCount: Long,
    calories: Double,
    sleepDuration: Duration?,
    exerciseCount: Int,
    hydration: Double,
    bloodPressure: Pair<Double, Double>?,
    oxygenSaturation: Double?,
    bodyComposition: BodyCompositionData?,
    detailedSleep: DetailedSleepData?,
    goalsProgress: GoalsProgressData?,
    activitySummary: ActivitySummaryData?,
    isLoading: Boolean,
    onAddWater: () -> Unit,
    onGenerateReport: () -> Unit,
    isGeneratingReport: Boolean,
    healthReport: String?,
    canCancel: Boolean,
    onCancelReport: () -> Unit,
    onReadReport: () -> Unit,
    showReportDialog: Boolean,
    onHideReport: () -> Unit
) {
    val healthColors = getHealthMetricColors()
    
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Premium section header
        PremiumSectionHeader(
            title = "Your Health Metrics",
            subtitle = "Real-time data from your connected devices"
        )

        // First row: Heart Rate and Steps
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            HealthMetricCard(
                modifier = Modifier.weight(1f),
                title = "Heart Rate",
                value = if (isLoading) "..." else (heartRate?.toString() ?: "---"),
                unit = "BPM",
                icon = Icons.Default.Favorite,
                iconColor = healthColors.heartRate
            )

            HealthMetricCard(
                modifier = Modifier.weight(1f),
                title = "Daily Steps",
                value = if (isLoading) "..." else stepCount.toString(),
                unit = "steps",
                icon = Icons.AutoMirrored.Filled.DirectionsWalk,
                iconColor = healthColors.steps
            )
        }

        // Second row: Calories and Sleep
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            HealthMetricCard(
                modifier = Modifier.weight(1f),
                title = "Calories",
                value = if (isLoading) "..." else String.format(Locale.US, "%.0f", calories),
                unit = "kcal",
                icon = Icons.Default.LocalFireDepartment,
                iconColor = healthColors.calories
            )

            HealthMetricCard(
                modifier = Modifier.weight(1f),
                title = "Sleep",
                value = if (isLoading) "..." else formatSleepDuration(sleepDuration),
                unit = "hours",
                icon = Icons.Default.Hotel,
                iconColor = healthColors.sleep
            )
        }

        // Third row: Exercise and Water
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            HealthMetricCard(
                modifier = Modifier.weight(1f),
                title = "Exercise",
                value = if (isLoading) "..." else exerciseCount.toString(),
                unit = "sessions",
                icon = Icons.Default.FitnessCenter,
                iconColor = healthColors.exercise
            )

            WaterIntakeCard(
                modifier = Modifier.weight(1f),
                hydration = hydration,
                isLoading = isLoading,
                onAddWater = onAddWater,
                waterColor = healthColors.water
            )
        }

        // Fourth row: SpO2 and Blood Pressure
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            HealthMetricCard(
                modifier = Modifier.weight(1f),
                title = "Blood Oxygen",
                value = if (isLoading) "..." else (oxygenSaturation?.let { "${it.roundToInt()}" } ?: "---"),
                unit = "%",
                icon = Icons.Default.Bloodtype,
                iconColor = healthColors.heartRate // Use heart rate color for blood oxygen
            )

            if (bloodPressure != null || isLoading) {
                HealthMetricCard(
                    modifier = Modifier.weight(1f),
                    title = "Blood Pressure",
                    value = if (isLoading) "..." else formatBloodPressure(bloodPressure),
                    unit = "mmHg",
                    icon = Icons.Default.MonitorHeart,
                    iconColor = healthColors.heartRate // Use heart rate color for blood pressure
                )
            } else {
                // Empty space to maintain layout
                Spacer(modifier = Modifier.weight(1f))
            }
        }

        // Fifth row: Body Composition
        if (bodyComposition != null || isLoading) {
            BodyCompositionCard(
                bodyComposition = bodyComposition,
                isLoading = isLoading,
                healthColors = healthColors
            )
        }

        // Sixth row: Detailed Sleep Analysis
        if (detailedSleep != null || isLoading) {
            DetailedSleepCard(
                detailedSleep = detailedSleep,
                isLoading = isLoading,
                healthColors = healthColors
            )
        }

        // Seventh row: Goals Progress
        if (goalsProgress != null || isLoading) {
            GoalsProgressCard(
                goalsProgress = goalsProgress,
                isLoading = isLoading,
                healthColors = healthColors
            )
        }

        // Eighth row: Activity Summary
        if (activitySummary != null || isLoading) {
            ActivitySummaryCard(
                activitySummary = activitySummary,
                isLoading = isLoading,
                healthColors = healthColors
            )
        }

        // LLM Health Report Section
        LLMHealthReportCard(
            onGenerateReport = onGenerateReport,
            isGeneratingReport = isGeneratingReport,
            healthReport = healthReport,
            canCancel = canCancel,
            onCancelReport = onCancelReport,
            onReadReport = onReadReport,
            showReportDialog = showReportDialog,
            onHideReport = onHideReport
        )

        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
    }
}

fun formatSleepDuration(duration: Duration?): String {
    return duration?.let {
        val hours = it.toHours()
        val minutes = (it.toMinutes() % 60)
        "${hours}h ${minutes}m"
    } ?: "---"
}

fun formatBloodPressure(bloodPressure: Pair<Double, Double>?): String {
    return bloodPressure?.let {
        "${it.first.roundToInt()}/${it.second.roundToInt()}"
    } ?: "---"
}

@Composable
fun WaterIntakeCard(
    modifier: Modifier = Modifier,
    hydration: Double,
    isLoading: Boolean,
    onAddWater: () -> Unit,
    waterColor: Color
) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.WaterDrop,
                    contentDescription = "Water",
                    modifier = Modifier.size(24.dp),
                    tint = waterColor
                )
                
                SmallFloatingActionButton(
                    onClick = onAddWater,
                    modifier = Modifier.size(24.dp),
                    containerColor = waterColor
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Add Water",
                        modifier = Modifier.size(16.dp),
                        tint = Color.White
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Water",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = if (isLoading) "..." else String.format(Locale.US, "%.1f", hydration),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Text(
                text = "L",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Text(
                text = "+250ml",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF2196F3),
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

@Composable
fun HealthMetricCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    unit: String,
    icon: ImageVector,
    iconColor: Color
) {
    Card(
        modifier = modifier.shadow(
            elevation = 6.dp,
            shape = RoundedCornerShape(16.dp),
            spotColor = iconColor.copy(alpha = 0.2f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Premium icon background
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                iconColor.copy(alpha = 0.15f),
                                iconColor.copy(alpha = 0.05f)
                            )
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    modifier = Modifier.size(28.dp),
                    tint = iconColor
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Text(
                text = unit,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun PermissionCard(onRequestPermissions: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Permissions Required",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "We need Health Connect permissions to display your health data.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(onClick = onRequestPermissions) {
                Text("Grant Permissions")
            }
        }
    }
}

@Composable
fun NotSupportedMessage() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Not Supported",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Text(
                text = "This device does not support Health Connect.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun NotInstalledMessage() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.warningContainer)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Health Connect Not Installed",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onWarningContainer
            )
            Text(
                text = "Please install Health Connect app from Play Store.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onWarningContainer,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun BodyCompositionCard(
    bodyComposition: BodyCompositionData?,
    isLoading: Boolean,
    healthColors: HealthMetricColors
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Scale,
                    contentDescription = "Body Composition",
                    modifier = Modifier.size(24.dp),
                    tint = healthColors.exercise
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Text(
                    text = "Body Composition",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                BodyMetricItem(
                    label = "Weight",
                    value = if (isLoading) "..." else bodyComposition?.let { "${String.format(Locale.US, "%.1f", it.weight)} kg" } ?: "---"
                )
                
                BodyMetricItem(
                    label = "BMI",
                    value = if (isLoading) "..." else bodyComposition?.let { String.format(Locale.US, "%.1f", it.bmi) } ?: "---"
                )
                
                BodyMetricItem(
                    label = "Body Fat",
                    value = if (isLoading) "..." else bodyComposition?.bodyFatPercentage?.let { "${String.format(Locale.US, "%.1f", it)}%" } ?: "---"
                )
            }
        }
    }
}

@Composable
fun BodyMetricItem(
    label: String,
    value: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun DetailedSleepCard(
    detailedSleep: DetailedSleepData?,
    isLoading: Boolean,
    healthColors: HealthMetricColors
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Nightlight,
                    contentDescription = "Sleep Analysis",
                    modifier = Modifier.size(24.dp),
                    tint = healthColors.sleep
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Text(
                    text = "Sleep Analysis",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                SleepStageItem(
                    label = "Total",
                    value = if (isLoading) "..." else detailedSleep?.let { formatSleepDuration(it.totalSleep) } ?: "---"
                )
                
                SleepStageItem(
                    label = "Deep",
                    value = if (isLoading) "..." else detailedSleep?.let { formatSleepDuration(it.deepSleep) } ?: "---"
                )
                
                SleepStageItem(
                    label = "REM",
                    value = if (isLoading) "..." else detailedSleep?.let { formatSleepDuration(it.remSleep) } ?: "---"
                )
                
                SleepStageItem(
                    label = "Light",
                    value = if (isLoading) "..." else detailedSleep?.let { formatSleepDuration(it.lightSleep) } ?: "---"
                )
            }
        }
    }
}

@Composable
fun SleepStageItem(
    label: String,
    value: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun GoalsProgressCard(
    goalsProgress: GoalsProgressData?,
    isLoading: Boolean,
    healthColors: HealthMetricColors
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.EmojiEvents,
                    contentDescription = "Goals Progress",
                    modifier = Modifier.size(24.dp),
                    tint = healthColors.calories
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Text(
                    text = "Daily Goals",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                GoalProgressItem(
                    label = "Steps",
                    current = if (isLoading) "..." else goalsProgress?.stepCurrent?.toString() ?: "0",
                    goal = goalsProgress?.stepGoal?.toString() ?: "10000",
                    progress = if (goalsProgress != null) (goalsProgress.stepCurrent.toFloat() / goalsProgress.stepGoal.toFloat()).coerceAtMost(1f) else 0f,
                    isLoading = isLoading
                )
                
                GoalProgressItem(
                    label = "Calories",
                    current = if (isLoading) "..." else goalsProgress?.let { String.format(Locale.US, "%.0f", it.calorieCurrent) } ?: "0",
                    goal = goalsProgress?.let { String.format(Locale.US, "%.0f", it.calorieGoal) } ?: "2200",
                    progress = if (goalsProgress != null) (goalsProgress.calorieCurrent.toFloat() / goalsProgress.calorieGoal.toFloat()).coerceAtMost(1f) else 0f,
                    isLoading = isLoading
                )
                
                GoalProgressItem(
                    label = "Active Minutes",
                    current = if (isLoading) "..." else goalsProgress?.activeMinuteCurrent?.toString() ?: "0",
                    goal = goalsProgress?.activeMinuteGoal?.toString() ?: "30",
                    progress = if (goalsProgress != null) (goalsProgress.activeMinuteCurrent.toFloat() / goalsProgress.activeMinuteGoal.toFloat()).coerceAtMost(1f) else 0f,
                    isLoading = isLoading
                )
            }
        }
    }
}

@Composable
fun GoalProgressItem(
    label: String,
    current: String,
    goal: String,
    progress: Float,
    isLoading: Boolean
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "$current / $goal",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .background(
                    MaterialTheme.colorScheme.surfaceVariant,
                    RoundedCornerShape(4.dp)
                )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(if (isLoading) 0f else progress)
                    .height(8.dp)
                    .background(
                        Color(0xFFFFD700),
                        RoundedCornerShape(4.dp)
                    )
            )
        }
    }
}

@Composable
fun ActivitySummaryCard(
    activitySummary: ActivitySummaryData?,
    isLoading: Boolean,
    healthColors: HealthMetricColors
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.DirectionsRun,
                    contentDescription = "Activity Summary",
                    modifier = Modifier.size(24.dp),
                    tint = healthColors.steps
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Text(
                    text = "Activity Summary",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ActivityMetricItem(
                    label = "Distance",
                    value = if (isLoading) "..." else activitySummary?.let { String.format(Locale.US, "%.1f km", it.distance) } ?: "0.0 km"
                )
                
                ActivityMetricItem(
                    label = "Active Minutes",
                    value = if (isLoading) "..." else activitySummary?.let { "${it.activeMinutes} min" } ?: "0 min"
                )
                
                ActivityMetricItem(
                    label = "Workouts",
                    value = if (isLoading) "..." else activitySummary?.workoutCount?.toString() ?: "0"
                )
            }
        }
    }
}

@Composable
fun ActivityMetricItem(
    label: String,
    value: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun LLMHealthReportCard(
    onGenerateReport: () -> Unit,
    isGeneratingReport: Boolean,
    healthReport: String?,
    canCancel: Boolean,
    onCancelReport: () -> Unit,
    onReadReport: () -> Unit,
    showReportDialog: Boolean,
    onHideReport: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.AssignmentInd,
                    contentDescription = "AI Sağlık Raporu",
                    modifier = Modifier.size(24.dp),
                    tint = Color(0xFF673AB7)
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Text(
                    text = "AI Health Report",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = "Your selected Gemma 3n model can analyze your health data and generate a personalized health report using AI.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Button Row - dynamically show available actions
            when {
                // When generating - show cancel and generate (disabled) buttons
                isGeneratingReport -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (canCancel) {
                            Button(
                                onClick = onCancelReport,
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Cancel")
                            }
                        }
                        
                        Button(
                            onClick = onGenerateReport,
                            enabled = false,
                            modifier = if (canCancel) Modifier.weight(1f) else Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = Color.White
                                )
                                Text("Generating Report...")
                            }
                        }
                    }
                }
                
                // When report exists - show read and generate new buttons (clear removed)
                healthReport != null -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onReadReport,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                        ) {
                            Text("Read Report")
                        }
                        
                        Button(
                            onClick = onGenerateReport,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Generate New")
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Text(
                        text = "💡 Generating a new report will replace the current one",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }
                
                // When no report - show only generate button
                else -> {
                    Button(
                        onClick = onGenerateReport,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Generate Health Report")
                    }
                }
            }
        }
    }
    
    // Health Report Dialog - controlled by showReportDialog state
    if (showReportDialog && healthReport != null) {
        HealthReportDialog(
            report = healthReport,
            onDismiss = onHideReport
        )
    }
}

@Composable
fun HealthReportDialog(
    report: String,
    onDismiss: () -> Unit
) {
    val healthColors = getHealthMetricColors()
    // Full-screen dialog with premium styling
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Premium icon with gradient background
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        Color(0xFF673AB7).copy(alpha = 0.2f),
                                        Color(0xFF9C27B0).copy(alpha = 0.1f)
                                    )
                                ),
                                shape = RoundedCornerShape(16.dp)
                            )
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Analytics,
                            contentDescription = "AI Health Report",
                            tint = healthColors.heartRate,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "AI Health Report",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            "Generated by Gemma 3n AI",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Premium divider
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color(0xFF673AB7).copy(alpha = 0.3f),
                                    Color.Transparent
                                )
                            )
                        )
                )
            }
        },
        text = {
            if (report.isBlank()) {
                // Premium error state
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "❌ Report is empty or blank",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Please try generating a new report",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                // Premium report display with card background
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 200.dp, max = 600.dp)
                            .verticalScroll(rememberScrollState())
                            .padding(20.dp)
                    ) {
                        Text(
                            text = formatHealthReport(report),
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.fillMaxWidth(),
                            lineHeight = 24.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF673AB7)
                ),
                shape = RoundedCornerShape(12.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Close Report",
                    fontWeight = FontWeight.Medium
                )
            }
        },
        modifier = Modifier
            .fillMaxWidth(0.98f)
            .fillMaxSize(0.9f),
        shape = RoundedCornerShape(20.dp),
        containerColor = MaterialTheme.colorScheme.background,
        tonalElevation = 8.dp
    )
}

@Composable
fun PremiumSectionHeader(
    title: String,
    subtitle: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

fun formatHealthReport(rawReport: String): String {
    return rawReport
        // Remove ALL markdown symbols completely
        .replace(Regex("#{1,6}\\s*"), "") // Remove all headers
        .replace(Regex("\\*\\*(.*?)\\*\\*"), "$1") // Remove bold markdown
        .replace(Regex("\\*(.*?)\\*"), "$1") // Remove italic markdown
        .replace(Regex("\\*+"), "") // Remove any remaining asterisks
        .replace(Regex("_+(.*?)_+"), "$1") // Remove underscores
        
        // Fix bullet point formatting
        .replace(Regex("^•\\s*", RegexOption.MULTILINE), "• ") // Ensure proper bullet spacing
        .replace(Regex("^-\\s*", RegexOption.MULTILINE), "• ") // Convert dashes to bullets
        .replace(Regex("^\\s*\\*\\s*", RegexOption.MULTILINE), "• ") // Convert asterisks to bullets
        
        // Clean up spacing
        .replace(Regex("\n{3,}"), "\n\n") // Max 2 newlines
        .replace(Regex("\\s+\n"), "\n") // Remove trailing spaces
        
        // Ensure proper emoji section spacing
        .replace(Regex("([🎯⚡⚠️💡🏆📈🏥📊📋])"), "\n\n$1")
        
        // Clean up and trim
        .replace(Regex("^\\s+", RegexOption.MULTILINE), "") // Remove leading spaces from lines
        .trim()
}

