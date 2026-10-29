package com.igotu.recording

import android.content.Context
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.*
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.time.Instant
import java.util.UUID
import java.util.concurrent.Executors
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages emergency video/audio recording using CameraX.
 *
 * ANDROID LIMITATIONS:
 * • Camera recording in the background requires a foreground service with
 *   foregroundServiceType="camera" (Android 10+, mandatory on Android 14+).
 * • The system will show a camera-in-use indicator — this cannot be hidden.
 *   IGOT-U does not attempt to suppress OS privacy indicators.
 * • CameraX requires a LifecycleOwner. During background recording the
 *   SosService acts as the lifecycle owner via ProcessCameraProvider.
 * • Screen-off recording: possible while foreground service is active,
 *   but some manufacturers restrict this further.
 */
@Singleton
class RecordingManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val configStore: RecordingConfigStore,
    private val uploadQueue: RecordingUploadQueue
) {
    private val tag = "RecordingManager"
    private var activeRecording: Recording? = null
    private var activeMetadata: RecordingMetadata? = null

    fun startEmergencyRecording(sosEventId: String) {
        val config = configStore.load()
        if (!config.enabled) return

        val recordingId = UUID.randomUUID().toString()
        val outputFile = createOutputFile(recordingId)

        val metadata = RecordingMetadata(
            recordingId = recordingId,
            sosEventId = sosEventId,
            userId = "current_user", // replaced by auth context
            createdAt = Instant.now(),
            startedAt = Instant.now(),
            mediaType = config.mode,
            localPath = outputFile.absolutePath,
            retentionExpiry = Instant.now().plusSeconds(config.retentionDays * 86400L)
        )
        activeMetadata = metadata

        when (config.mode) {
            RecordingMode.AUDIO_ONLY -> startAudioRecording(outputFile, metadata, config)
            RecordingMode.EVIDENCE_BURST -> startBurstRecording(sosEventId, config)
            else -> startVideoRecording(outputFile, metadata, config)
        }
    }

    private fun startVideoRecording(
        outputFile: File,
        metadata: RecordingMetadata,
        config: RecordingConfig
    ) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            try {
                val cameraProvider = cameraProviderFuture.get()
                val qualitySelector = QualitySelector.from(Quality.HD)
                val recorder = Recorder.Builder()
                    .setQualitySelector(qualitySelector)
                    .build()
                val videoCapture = VideoCapture.withOutput(recorder)

                val cameraSelector = if (config.cameraFacing == CameraFacing.FRONT)
                    CameraSelector.DEFAULT_FRONT_CAMERA
                else
                    CameraSelector.DEFAULT_BACK_CAMERA

                // NOTE: Requires a LifecycleOwner — SosService must implement
                // LifecycleOwner or use a ServiceLifecycleDispatcher
                val outputOptions = FileOutputOptions.Builder(outputFile).build()
                activeRecording = videoCapture.output
                    .prepareRecording(context, outputOptions)
                    .apply {
                        if (config.mode == RecordingMode.VIDEO_AUDIO) withAudioEnabled()
                    }
                    .start(ContextCompat.getMainExecutor(context)) { event ->
                        handleRecordingEvent(event, metadata, config)
                    }
            } catch (e: Exception) {
                Log.e(tag, "Failed to start video recording", e)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    private fun startAudioRecording(
        outputFile: File,
        metadata: RecordingMetadata,
        config: RecordingConfig
    ) {
        // Audio-only uses MediaRecorder for simplicity
        // Implementation omitted for brevity — follows same pattern
        Log.i(tag, "Audio-only recording started: ${outputFile.absolutePath}")
    }

    private fun startBurstRecording(sosEventId: String, config: RecordingConfig) {
        // Schedules periodic short clips via WorkManager
        Log.i(tag, "Burst recording started for SOS $sosEventId")
    }

    private fun handleRecordingEvent(
        event: VideoRecordEvent,
        metadata: RecordingMetadata,
        config: RecordingConfig
    ) {
        when (event) {
            is VideoRecordEvent.Start -> Log.i(tag, "Recording started")
            is VideoRecordEvent.Finalize -> {
                if (event.hasError()) {
                    Log.e(tag, "Recording error: ${event.error}")
                } else {
                    val finalMeta = metadata.copy(
                        endedAt = Instant.now(),
                        fileSizeBytes = outputFile(metadata).length()
                    )
                    if (config.uploadImmediately) uploadQueue.enqueue(finalMeta)
                }
            }
            else -> {}
        }
    }

    fun stopEmergencyRecording(sosEventId: String) {
        activeRecording?.stop()
        activeRecording = null
        activeMetadata = null
    }

    private fun createOutputFile(recordingId: String): File {
        val dir = File(context.filesDir, "emergency_recordings").also { it.mkdirs() }
        return File(dir, "$recordingId.mp4")
    }

    private fun outputFile(metadata: RecordingMetadata) =
        File(metadata.localPath ?: "")
}
