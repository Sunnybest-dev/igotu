package com.igotu.voice

import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.sqrt

data class EnrollmentState(
    val samplesRecorded: Int = 0,
    val samplesRequired: Int = 5,
    val isComplete: Boolean = false,
    val error: String? = null
)

@Singleton
class VoiceEnrollmentManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val profileStore: VoiceProfileStore
) {
    companion object {
        private const val SAMPLE_RATE = 16000
        private const val REQUIRED_SAMPLES = 5
        private const val SAMPLE_DURATION_MS = 2000
    }

    private val _state = MutableStateFlow(EnrollmentState())
    val state: StateFlow<EnrollmentState> = _state

    private val collectedEmbeddings = mutableListOf<FloatArray>()

    fun reset() {
        collectedEmbeddings.clear()
        _state.value = EnrollmentState()
    }

    /**
     * Record one enrollment sample. Call this once per spoken phrase.
     * Raw audio is processed in-memory and discarded — only the embedding is kept.
     */
    suspend fun recordSample(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val pcm = recordPcmSample()
            val embedding = extractEmbedding(pcm)
            collectedEmbeddings.add(embedding)
            val count = collectedEmbeddings.size
            _state.value = EnrollmentState(
                samplesRecorded = count,
                samplesRequired = REQUIRED_SAMPLES,
                isComplete = count >= REQUIRED_SAMPLES
            )
            if (count >= REQUIRED_SAMPLES) finalizeProfile()
            Result.success(Unit)
        } catch (e: Exception) {
            _state.value = _state.value.copy(error = e.message)
            Result.failure(e)
        }
    }

    private fun recordPcmSample(): ShortArray {
        val bufferSize = AudioRecord.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        val totalSamples = SAMPLE_RATE * SAMPLE_DURATION_MS / 1000
        val pcm = ShortArray(totalSamples)
        val recorder = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferSize
        )
        recorder.startRecording()
        recorder.read(pcm, 0, totalSamples)
        recorder.stop()
        recorder.release()
        // Raw PCM is NOT stored — only the derived embedding below
        return pcm
    }

    /**
     * Minimal embedding: RMS energy per 100ms frame.
     * In production replace with a proper speaker embedding model
     * (e.g. SpeakerNet, ECAPA-TDNN via TFLite) for better accuracy.
     */
    private fun extractEmbedding(pcm: ShortArray): FloatArray {
        val frameSize = SAMPLE_RATE / 10 // 100ms frames
        val frames = pcm.size / frameSize
        return FloatArray(frames) { i ->
            val start = i * frameSize
            val end = minOf(start + frameSize, pcm.size)
            val rms = sqrt(pcm.slice(start until end).sumOf { it.toLong() * it }.toDouble() / (end - start))
            rms.toFloat()
        }
    }

    private fun finalizeProfile() {
        // Average embeddings across all samples
        val len = collectedEmbeddings.first().size
        val averaged = FloatArray(len) { i ->
            collectedEmbeddings.sumOf { it.getOrElse(i) { 0f }.toDouble() }.toFloat() / collectedEmbeddings.size
        }
        profileStore.save(VoiceProfile(
            id = UUID.randomUUID().toString(),
            createdAt = System.currentTimeMillis(),
            embedding = averaged
        ))
        collectedEmbeddings.clear()
    }
}
