package com.example.healthassistant.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Premium Health App Dark Theme
private val HealthDarkColorScheme = darkColorScheme(
    primary = HealthPrimaryDark,
    onPrimary = Color.White,
    primaryContainer = HealthPrimaryVariantDark,
    onPrimaryContainer = Color.White,
    
    secondary = HealthSecondaryDark,
    onSecondary = Color.White,
    secondaryContainer = HealthSecondaryVariantDark,
    onSecondaryContainer = Color.White,
    
    tertiary = HealthAccentDark,
    onTertiary = Color.White,
    
    background = DarkBackground,
    onBackground = Color(0xFFE1E3E6),
    
    surface = DarkSurface,
    onSurface = Color(0xFFE1E3E6),
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = Color(0xFFBDC1C6),
    
    outline = Color(0xFF8B9297),
    outlineVariant = Color(0xFF43474E),
    
    error = AlertRedDark,
    onError = Color.White,
    errorContainer = Color(0xFF601410),
    onErrorContainer = Color(0xFFFFDAD6)
)

// Premium Health App Light Theme
private val HealthLightColorScheme = lightColorScheme(
    primary = HealthPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE8F5E8),
    onPrimaryContainer = HealthPrimaryVariant,
    
    secondary = HealthSecondary,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE3F2FD),
    onSecondaryContainer = HealthSecondaryVariant,
    
    tertiary = HealthAccent,
    onTertiary = Color.White,
    
    background = LightBackground,
    onBackground = Color(0xFF1A1C1E),
    
    surface = LightSurface,
    onSurface = Color(0xFF1A1C1E),
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = Color(0xFF43474E),
    
    outline = Color(0xFF73777F),
    outlineVariant = Color(0xFFC3C7CF),
    
    error = AlertRed,
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF601410)
)

@Composable
fun HealthAssistantTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color disabled for consistent health app branding
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        // Use dynamic colors only if explicitly enabled (disabled by default for health branding)
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        
        // Use our custom health-themed color schemes
        darkTheme -> HealthDarkColorScheme
        else -> HealthLightColorScheme
    }
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

// Convenience function to get health metric colors based on current theme
@Composable
fun getHealthMetricColors(): HealthMetricColors {
    val isDark = !MaterialTheme.colorScheme.isLight()
    return if (isDark) {
        HealthMetricColors(
            heartRate = HeartRateColorDark,
            steps = StepsColorDark,
            calories = CaloriesColorDark,
            sleep = SleepColorDark,
            water = WaterColorDark,
            exercise = ExerciseColorDark
        )
    } else {
        HealthMetricColors(
            heartRate = HeartRateColor,
            steps = StepsColor,
            calories = CaloriesColor,
            sleep = SleepColor,
            water = WaterColor,
            exercise = ExerciseColor
        )
    }
}

// Helper to check if the color scheme is light
fun androidx.compose.material3.ColorScheme.isLight(): Boolean {
    return this.background.luminance() > 0.5
}

// Data class for health metric colors
data class HealthMetricColors(
    val heartRate: Color,
    val steps: Color,
    val calories: Color,
    val sleep: Color,
    val water: Color,
    val exercise: Color
)

