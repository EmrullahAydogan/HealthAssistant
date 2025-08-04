package com.example.healthassistant.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

class ModelDownloadWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        const val KEY_MODEL_NAME = "model_name"
        const val KEY_MODEL_URL = "model_url"
        const val KEY_MODEL_FILE_NAME = "model_file_name"
        const val KEY_MODEL_SIZE = "model_size"
        const val KEY_ACCESS_TOKEN = "access_token"
        const val KEY_PROGRESS = "progress"
        const val KEY_DOWNLOADED_BYTES = "downloaded_bytes"
        const val KEY_ERROR_MESSAGE = "error_message"
        private const val TAG = "ModelDownloadWorker"
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val modelName = inputData.getString(KEY_MODEL_NAME) ?: return@withContext Result.failure()
        val modelUrl = inputData.getString(KEY_MODEL_URL) ?: return@withContext Result.failure()
        val modelFileName = inputData.getString(KEY_MODEL_FILE_NAME) ?: return@withContext Result.failure()
        val modelSize = inputData.getLong(KEY_MODEL_SIZE, 0L)
        val accessToken = inputData.getString(KEY_ACCESS_TOKEN)

        try {
            downloadModel(modelName, modelUrl, modelFileName, modelSize, accessToken)
        } catch (e: Exception) {
            Log.e(TAG, "Error downloading model $modelName", e)
            Result.failure(
                workDataOf(KEY_ERROR_MESSAGE to (e.message ?: "Unknown error"))
            )
        }
    }

    private suspend fun downloadModel(
        modelName: String,
        modelUrl: String,
        modelFileName: String,
        modelSize: Long,
        accessToken: String?
    ): Result {
        return try {
            // Create models directory
            val modelsDir = File(applicationContext.getExternalFilesDir(null), "models")
            if (!modelsDir.exists()) {
                modelsDir.mkdirs()
            }

            val modelFile = File(modelsDir, modelFileName)
            val url = URL(modelUrl)
            val connection = url.openConnection() as HttpURLConnection
            
            // Add authorization header if token is provided
            if (accessToken != null) {
                connection.setRequestProperty("Authorization", "Bearer $accessToken")
            }
            
            connection.connect()

            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                val errorMessage = when (connection.responseCode) {
                    HttpURLConnection.HTTP_NOT_FOUND -> "Model not found (404). Check the model URL or your HuggingFace token permissions."
                    HttpURLConnection.HTTP_UNAUTHORIZED -> "Unauthorized (401). Please check your HuggingFace token."
                    HttpURLConnection.HTTP_FORBIDDEN -> "Forbidden (403). This model may require special permissions."
                    else -> "Server returned HTTP ${connection.responseCode}: ${connection.responseMessage}"
                }
                Log.e(TAG, "Download failed for $modelName: $errorMessage")
                Log.e(TAG, "URL: $modelUrl")
                return Result.failure(
                    workDataOf(KEY_ERROR_MESSAGE to errorMessage)
                )
            }

            val fileLength = connection.contentLength.toLong()
            val inputStream = connection.inputStream
            val outputStream = FileOutputStream(modelFile)

            val buffer = ByteArray(64 * 1024) // 64KB buffer for better performance
            var downloadedBytes = 0L
            var bytesRead: Int
            var lastProgressUpdate = 0L

            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
                downloadedBytes += bytesRead

                // Update progress every 1MB to avoid too frequent updates
                if (downloadedBytes - lastProgressUpdate > 1_000_000 || downloadedBytes == fileLength) {
                    val progress = if (fileLength > 0) {
                        (downloadedBytes * 100 / fileLength).toInt()
                    } else {
                        ((downloadedBytes * 100) / modelSize).toInt()
                    }

                    Log.d(TAG, "Download progress for $modelName: $progress% ($downloadedBytes/$fileLength bytes)")

                    // Update progress
                    val progressData = workDataOf(
                        KEY_PROGRESS to progress,
                        KEY_DOWNLOADED_BYTES to downloadedBytes
                    )
                    setProgress(progressData)
                    lastProgressUpdate = downloadedBytes
                }
            }

            outputStream.close()
            inputStream.close()
            connection.disconnect()

            // Verify file integrity
            val finalFileSize = modelFile.length()
            Log.d(TAG, "Download completed. Final file size: $finalFileSize, Expected: $modelSize")
            
            if (finalFileSize != modelSize && modelSize > 0) {
                Log.e(TAG, "File size mismatch after download. Downloaded: $finalFileSize, Expected: $modelSize")
                
                // Delete the incomplete file
                if (modelFile.exists()) {
                    modelFile.delete()
                }
                
                return Result.failure(
                    workDataOf(KEY_ERROR_MESSAGE to "Download incomplete. File size mismatch.")
                )
            }
            
            // Additional integrity check - try to read first few bytes to ensure it's not empty/corrupted
            try {
                val testBuffer = ByteArray(1024)
                val testInputStream = modelFile.inputStream()
                val bytesRead = testInputStream.read(testBuffer)
                testInputStream.close()
                
                if (bytesRead < 100) { // File too small to be valid
                    Log.e(TAG, "Downloaded file appears to be corrupted (too small)")
                    modelFile.delete()
                    return Result.failure(
                        workDataOf(KEY_ERROR_MESSAGE to "Downloaded file appears to be corrupted.")
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error verifying downloaded file", e)
                modelFile.delete()
                return Result.failure(
                    workDataOf(KEY_ERROR_MESSAGE to "Downloaded file verification failed.")
                )
            }

            Log.d(TAG, "Successfully downloaded and verified model: $modelName")
            Result.success()

        } catch (e: Exception) {
            Log.e(TAG, "Error downloading model $modelName", e)
            Result.failure(
                workDataOf(KEY_ERROR_MESSAGE to (e.message ?: "Unknown error"))
            )
        }
    }
}