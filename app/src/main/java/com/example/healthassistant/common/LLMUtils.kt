package com.example.healthassistant.common

import android.util.Log

private const val TAG = "LLMUtils"
private const val START_THINKING = "***Analyzing...***"
private const val DONE_THINKING = "***Analysis Complete***"

object LLMUtils {
    
    /**
     * Process LLM response to ensure clean, English-only output
     * Based on Google Gallery's processLlmResponse utility
     */
    fun processHealthResponse(response: String): String {
        var processedResponse = response
        
        // Handle thinking tags - replace with cleaner alternatives
        processedResponse = processedResponse
            .replace("<think>", "$START_THINKING\n")
            .replace("</think>", "\n$DONE_THINKING")
        
        // Remove empty thinking content
        val endThinkingIndex = processedResponse.indexOf(DONE_THINKING)
        if (endThinkingIndex >= 0) {
            val thinkingContent = processedResponse
                .substring(0, endThinkingIndex + DONE_THINKING.length)
                .replace(START_THINKING, "")
                .replace(DONE_THINKING, "")
            if (thinkingContent.isBlank()) {
                processedResponse = processedResponse.substring(endThinkingIndex + DONE_THINKING.length)
            }
        }
        
        // Clean up formatting
        processedResponse = processedResponse
            .replace("\\n", "\n")
            .trim()
        
        // Filter non-English characters (Chinese, Arabic, etc.)
        processedResponse = filterToEnglish(processedResponse)
        
        return processedResponse
    }
    
    /**
     * Filter response to ensure English-only output
     */
    private fun filterToEnglish(text: String): String {
        var filtered = text
        
        // Pattern for Chinese characters
        val chinesePattern = Regex("[\u4e00-\u9fff]+")
        if (chinesePattern.containsMatchIn(filtered)) {
            Log.w(TAG, "Detected Chinese characters, filtering...")
            filtered = chinesePattern.replace(filtered) { "[Health Analysis]" }
        }
        
        // Pattern for Arabic characters
        val arabicPattern = Regex("[\u0600-\u06ff]+")
        if (arabicPattern.containsMatchIn(filtered)) {
            Log.w(TAG, "Detected Arabic characters, filtering...")
            filtered = arabicPattern.replace(filtered) { "[Health Analysis]" }
        }
        
        // Pattern for Cyrillic characters
        val cyrillicPattern = Regex("[\u0400-\u04ff]+")
        if (cyrillicPattern.containsMatchIn(filtered)) {
            Log.w(TAG, "Detected Cyrillic characters, filtering...")
            filtered = cyrillicPattern.replace(filtered) { "[Health Analysis]" }
        }
        
        // Pattern for other non-Latin scripts
        val nonLatinPattern = Regex("[\u3000-\u30ff\u4e00-\u9fff\u0590-\u05ff\u0600-\u06ff\u0700-\u074f\u0750-\u077f\u0900-\u097f\u0980-\u09ff\u0a00-\u0a7f\u0a80-\u0aff\u0b00-\u0b7f\u0b80-\u0bff\u0c00-\u0c7f\u0c80-\u0cff\u0d00-\u0d7f]+")
        if (nonLatinPattern.containsMatchIn(filtered)) {
            Log.w(TAG, "Detected non-Latin characters, filtering...")
            filtered = nonLatinPattern.replace(filtered) { "" }
        }
        
        return filtered.trim()
    }
    
    /**
     * Create Gemma 3n optimized health analysis prompt
     * Specially tuned for Gemma 3n's architecture and training
     */
    fun createOptimizedHealthPrompt(healthData: String): String {
        return """As a health data analyst, analyze this health metrics data and provide insights in English.

Data to analyze:
$healthData

Provide analysis in this structured format:

STATUS: [Overall health assessment in 1-2 sentences]

STRENGTHS:
- [Positive metric or trend 1]
- [Positive metric or trend 2]

ATTENTION AREAS:
- [Metric needing improvement or monitoring]

ACTIONS:
- [Specific actionable recommendation 1]
- [Specific actionable recommendation 2]
- [Specific actionable recommendation 3]

PATTERNS:
- [Notable correlation or trend observation]

Respond only in clear English. Focus on practical health guidance based on the data provided."""
    }
    
    /**
     * Validate Gemma 3n response contains expected sections
     */
    fun validateHealthResponse(response: String): Boolean {
        val requiredSections = listOf(
            "STATUS",
            "STRENGTHS",
            "ACTIONS"
        )
        
        // Check if response contains required sections and is primarily in English
        val hasRequiredSections = requiredSections.all { section ->
            response.contains(section, ignoreCase = true)
        }
        
        // Check if response is primarily English (basic check)
        val englishRatio = response.count { it.isLetter() && it.code < 128 }.toFloat() / 
                          maxOf(response.count { it.isLetter() }, 1)
        
        return hasRequiredSections && englishRatio > 0.8f
    }
    
    /**
     * Clean up MediaPipe task error messages
     */
    fun cleanUpMediaPipeError(message: String): String {
        val index = message.indexOf("=== Source Location Trace")
        return if (index >= 0) {
            message.substring(0, index).trim()
        } else {
            message
        }
    }
}