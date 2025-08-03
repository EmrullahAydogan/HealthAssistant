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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Hotel
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.Add
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
import com.example.healthassistant.data.HealthConnectAvailability
import com.example.healthassistant.ui.theme.onWarningContainer
import com.example.healthassistant.ui.theme.warningContainer
import com.example.healthassistant.viewmodel.HealthDataViewModel
import java.time.Duration
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: HealthDataViewModel
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
            TopAppBar(
                title = { Text("Health Assistant", color = Color.White) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                actions = {
                    IconButton(onClick = { viewModel.refreshData() }) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Refresh Data",
                            tint = Color.White
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
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
                                isLoading = uiState.isLoading,
                                onAddWater = { viewModel.addWater() }
                            )
                        }
                    }
                }
            }
        }
    }
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
    isLoading: Boolean,
    onAddWater: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Your Health Data",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
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
                iconColor = Color.Red
            )

            HealthMetricCard(
                modifier = Modifier.weight(1f),
                title = "Daily Steps",
                value = if (isLoading) "..." else stepCount.toString(),
                unit = "steps",
                icon = Icons.Default.DirectionsWalk,
                iconColor = Color.Blue
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
                iconColor = Color(0xFFFF6B35)
            )

            HealthMetricCard(
                modifier = Modifier.weight(1f),
                title = "Sleep",
                value = if (isLoading) "..." else formatSleepDuration(sleepDuration),
                unit = "hours",
                icon = Icons.Default.Hotel,
                iconColor = Color(0xFF6B73FF)
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
                iconColor = Color(0xFF4CAF50)
            )

            WaterIntakeCard(
                modifier = Modifier.weight(1f),
                hydration = hydration,
                isLoading = isLoading,
                onAddWater = onAddWater
            )
        }

        // Fourth row: Blood Pressure (single card)
        if (bloodPressure != null || isLoading) {
            HealthMetricCard(
                modifier = Modifier.fillMaxWidth(),
                title = "Blood Pressure",
                value = if (isLoading) "..." else formatBloodPressure(bloodPressure),
                unit = "mmHg",
                icon = Icons.Default.MonitorHeart,
                iconColor = Color(0xFFE91E63)
            )
        }

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
    onAddWater: () -> Unit
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
                    tint = Color(0xFF2196F3)
                )
                
                SmallFloatingActionButton(
                    onClick = onAddWater,
                    modifier = Modifier.size(24.dp),
                    containerColor = Color(0xFF2196F3)
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
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                modifier = Modifier.size(32.dp),
                tint = iconColor
            )
            
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