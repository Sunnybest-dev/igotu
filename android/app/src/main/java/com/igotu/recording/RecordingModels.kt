package com.igotu.recording

import java.time.Instant

enum class RecordingMode { VIDEO_AUDIO, AUDIO_ONLY, VIDEO_ONLY, EVIDENCE_BURST }
enum class CameraFacing { FRONT, REAR }
enum class UploadStatus { PENDING, UPLOADING, COMPLETE, FAILED }

data class RecordingConfig(
    val enabled: Boolean = true,
    val mode: RecordingMode = RecordingMode.VIDEO_AUDIO,
    val cameraFacing: CameraFacing = CameraFacing.REAR,
    val maxDurationMinutes: Int = 30,
    val startDelaySeconds: Int = 0,
    val autoStartWithSos: Boolean = true,
    val maxStorageMb: Int = 500,
    val uploadImmediately: Boolean = true,
    val retentionDays: Int = 7,
    val burstIntervalSeconds: Int = 60  // for EVIDENCE_BURST mode
)

data class RecordingMetadata(
    val recordingId: String,
    val sosEventId: String,
    val userId: String,
    val createdAt: Instant,
    val startedAt: Instant? = null,
    val endedAt: Instant? = null,
    val durationSeconds: Long? = null,
    val mediaType: RecordingMode,
    val fileSizeBytes: Long? = null,
    val localPath: String? = null,
    val uploadStatus: UploadStatus = UploadStatus.PENDING,
    val encryptionStatus: Boolean = false,
    val retentionExpiry: Instant? = null
)
