package com.igotu.recording

import android.content.Context
import androidx.work.*
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecordingConfigStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs = context.getSharedPreferences("recording_config", Context.MODE_PRIVATE)
    private val gson = Gson()

    fun load(): RecordingConfig {
        val json = prefs.getString("config", null) ?: return RecordingConfig()
        return gson.fromJson(json, RecordingConfig::class.java)
    }

    fun save(config: RecordingConfig) {
        prefs.edit().putString("config", gson.toJson(config)).apply()
    }
}

/**
 * Enqueues recording uploads via WorkManager.
 * WorkManager retries automatically when network returns — recordings are
 * never lost due to temporary connectivity loss.
 */
@Singleton
class RecordingUploadQueue @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun enqueue(metadata: RecordingMetadata) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = OneTimeWorkRequestBuilder<RecordingUploadWorker>()
            .setConstraints(constraints)
            .setInputData(workDataOf(
                "recording_id" to metadata.recordingId,
                "local_path" to metadata.localPath,
                "sos_event_id" to metadata.sosEventId
            ))
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, java.util.concurrent.TimeUnit.SECONDS)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            "upload_${metadata.recordingId}",
            ExistingWorkPolicy.KEEP,
            request
        )
    }
}

class RecordingUploadWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val recordingId = inputData.getString("recording_id") ?: return Result.failure()
        val localPath = inputData.getString("local_path") ?: return Result.failure()
        val sosEventId = inputData.getString("sos_event_id") ?: return Result.failure()

        return try {
            // TODO: chunked upload to backend /recordings/:id/upload
            // On partial upload, server tracks chunks and resumes
            Result.success()
        } catch (e: Exception) {
            if (runAttemptCount < 5) Result.retry() else Result.failure()
        }
    }
}
