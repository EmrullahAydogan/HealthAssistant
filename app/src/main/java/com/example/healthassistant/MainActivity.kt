package com.example.healthassistant

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.healthassistant.data.HealthConnectManager
import com.example.healthassistant.ui.dashboard.DashboardScreen
import com.example.healthassistant.ui.theme.HealthAssistantTheme
import com.example.healthassistant.viewmodel.HealthDataViewModel

class MainActivity : ComponentActivity() {
    
    private lateinit var healthConnectManager: HealthConnectManager
    private lateinit var healthDataViewModel: HealthDataViewModel
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        healthConnectManager = HealthConnectManager(this)
        healthDataViewModel = HealthDataViewModel(healthConnectManager)
        
        enableEdgeToEdge()
        setContent {
            HealthAssistantTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    DashboardScreen(viewModel = healthDataViewModel)
                }
            }
        }
    }
}