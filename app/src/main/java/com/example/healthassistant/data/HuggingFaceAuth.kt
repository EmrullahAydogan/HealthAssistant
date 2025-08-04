package com.example.healthassistant.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log

class HuggingFaceAuth(private val context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences("huggingface_auth", Context.MODE_PRIVATE)
    
    companion object {
        private const val TAG = "HuggingFaceAuth"
        private const val KEY_ACCESS_TOKEN = "access_token"
    }
    
    fun saveToken(token: String) {
        prefs.edit()
            .putString(KEY_ACCESS_TOKEN, token)
            .apply()
        Log.d(TAG, "HuggingFace token saved")
    }
    
    fun getToken(): String? {
        return prefs.getString(KEY_ACCESS_TOKEN, null)
    }
    
    fun clearToken() {
        prefs.edit()
            .remove(KEY_ACCESS_TOKEN)
            .apply()
        Log.d(TAG, "HuggingFace token cleared")
    }
    
    fun hasToken(): Boolean {
        return getToken() != null
    }
}