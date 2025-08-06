package com.example.healthassistant.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import kotlin.math.pow

// Health App Premium Colors - Light Theme
val HealthPrimary = Color(0xFF2E7D32)        // Medical Green
val HealthPrimaryVariant = Color(0xFF1B5E20)  // Darker Green
val HealthSecondary = Color(0xFF1976D2)       // Medical Blue
val HealthSecondaryVariant = Color(0xFF0D47A1) // Darker Blue
val HealthAccent = Color(0xFF00ACC1)          // Cyan Accent

// Health Metric Colors - Light Theme
val HeartRateColor = Color(0xFFE53E3E)        // Red for heart
val StepsColor = Color(0xFF3182CE)            // Blue for steps
val CaloriesColor = Color(0xFFD69E2E)         // Orange for calories
val SleepColor = Color(0xFF805AD5)            // Purple for sleep
val WaterColor = Color(0xFF00B5D8)            // Cyan for water
val ExerciseColor = Color(0xFF38A169)         // Green for exercise

// Dark Theme Colors
val HealthPrimaryDark = Color(0xFF4CAF50)     // Brighter green for dark
val HealthPrimaryVariantDark = Color(0xFF2E7D32)
val HealthSecondaryDark = Color(0xFF42A5F5)   // Brighter blue for dark
val HealthSecondaryVariantDark = Color(0xFF1976D2)
val HealthAccentDark = Color(0xFF26C6DA)      // Brighter cyan for dark

// Health Metric Colors - Dark Theme
val HeartRateColorDark = Color(0xFFFF6B6B)
val StepsColorDark = Color(0xFF4FC3F7)
val CaloriesColorDark = Color(0xFFFFB74D)
val SleepColorDark = Color(0xFFBA68C8)
val WaterColorDark = Color(0xFF4DD0E1)
val ExerciseColorDark = Color(0xFF66BB6A)

// Background Colors
val LightBackground = Color(0xFFF8F9FA)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceVariant = Color(0xFFF1F3F4)

val DarkBackground = Color(0xFF121212)
val DarkSurface = Color(0xFF1E1E1E)
val DarkSurfaceVariant = Color(0xFF2C2C2C)

// Extension colors for warnings and info
val ColorScheme.warningContainer: Color
    get() = if (this.background.luminance() > 0.5) {
        Color(0xFFFFF3E0)  // Light warning
    } else {
        Color(0xFF3E2723)  // Dark warning
    }

val ColorScheme.onWarningContainer: Color
    get() = if (this.background.luminance() > 0.5) {
        Color(0xFFE65100)  // Light warning text
    } else {
        Color(0xFFFFAB40)  // Dark warning text
    }

// Extension to get color luminance
fun Color.luminance(): Float {
    val r = if (red <= 0.03928f) red / 12.92f else ((red + 0.055f) / 1.055f).pow(2.4f)
    val g = if (green <= 0.03928f) green / 12.92f else ((green + 0.055f) / 1.055f).pow(2.4f)
    val b = if (blue <= 0.03928f) blue / 12.92f else ((blue + 0.055f) / 1.055f).pow(2.4f)
    return 0.2126f * r + 0.7152f * g + 0.0722f * b
}

// Health Status Colors
val HealthyGreen = Color(0xFF4CAF50)
val WarningOrange = Color(0xFFFF9800)
val AlertRed = Color(0xFFE53935)
val InfoBlue = Color(0xFF2196F3)

val HealthyGreenDark = Color(0xFF66BB6A)
val WarningOrangeDark = Color(0xFFFFB74D)
val AlertRedDark = Color(0xFFEF5350)
val InfoBlueDark = Color(0xFF42A5F5)