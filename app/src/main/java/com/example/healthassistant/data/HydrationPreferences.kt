package com.example.healthassistant.data

import android.content.Context
import android.content.SharedPreferences
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class HydrationPreferences(private val context: Context) {
    
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(
        PREF_NAME, Context.MODE_PRIVATE
    )
    
    fun getTodayHydration(): Double {
        val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        return sharedPreferences.getFloat("hydration_$today", 0f).toDouble()
    }
    
    fun addWater(amount: Double) {
        val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        val currentAmount = getTodayHydration()
        val newAmount = currentAmount + amount
        
        sharedPreferences.edit()
            .putFloat("hydration_$today", newAmount.toFloat())
            .apply()
    }
    
    fun resetTodayHydration() {
        val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        sharedPreferences.edit()
            .putFloat("hydration_$today", 0f)
            .apply()
    }
    
    companion object {
        private const val PREF_NAME = "hydration_preferences"
    }
}