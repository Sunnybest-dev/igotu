package com.igotu.voice

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * Uses Android's on-device SpeechRecognizer.
 *
 * ANDROID LIMITATIONS (documented as required):
 * ─────────────────────────────────────────────
 * • Continuous background listening is NOT permitted without a foreground
 *   service with foregroundServiceType="microphone" (Android 10+, enforced
 *   strictly on Android 14+).
 * • A persistent user-visible notification is MANDATORY while listening.
 * • SpeechRecognizer has a session timeout (~5–10 s of silence); the service
 *   must restart the session to maintain continuous availability.
 * • On some manufacturer ROMs (Xiaomi MIUI, Huawei EMUI, Samsung One UI)
 *   background services are aggressively killed. Users may need to grant
 *   "unrestricted battery" permission in device settings.
 * • On-device recognition quality varies by device and Android version.
 * • This provider does NOT perform speaker verification — it detects the
 *   phrase only. VoiceProfileVerifier handles speaker matching separately.
 */
class OnDeviceVoiceTriggerProvider @Inject constructor(
    @ApplicationContext private val context: Context
) : VoiceTriggerProvider {

    private var recognizer: SpeechRecognizer? = null
    private var currentConfig: VoiceTriggerConfig? = null
    private var resultCallback: ((VoiceDetectionResult) -> Unit)? = null

    override fun isAvailable(): Boolean =
        SpeechRecognizer.isRecognitionAvailable(context)

    override fun availabilityNote(): String? {
        if (!isAvailable()) return "On-device speech recognition is not available on this device."
        if (Build.VERSION.SDK_INT >= 34) {
            return "Voice activation requires a visible notification on Android 14+. " +
                    "Background microphone access without a foreground service is not permitted."
        }
        return null
    }

    override fun startListening(config: VoiceTriggerConfig, onResult: (VoiceDetectionResult) -> Unit) {
        currentConfig = config
        resultCallback = onResult
        createAndStartRecognizer(config, onResult)
    }

    override fun stopListening() {
        recognizer?.destroy()
        recognizer = null
        resultCallback = null
    }

    private fun createAndStartRecognizer(
        config: VoiceTriggerConfig,
        onResult: (VoiceDetectionResult) -> Unit
    ) {
        recognizer?.destroy()
        recognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onResults(results: Bundle) {
                    val matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val scores = results.getFloatArray(SpeechRecognizer.CONFIDENCE_SCORES)
                    processResults(matches, scores, config, onResult)
                    // Restart session to maintain continuous listening
                    if (currentConfig != null) createAndStartRecognizer(config, onResult)
                }

                override fun onError(error: Int) {
                    // Restart on timeout/no-match errors to keep listening
                    if (currentConfig != null) createAndStartRecognizer(config, onResult)
                }

                override fun onReadyForSpeech(params: Bundle?) {}
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {}
                override fun onPartialResults(partialResults: Bundle?) {}
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
            startListening(buildRecognizerIntent())
        }
    }

    private fun buildRecognizerIntent() = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
        // Prefer on-device processing where available (Android 11+)
        if (Build.VERSION.SDK_INT >= 31) {
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
        }
    }

    private fun processResults(
        matches: List<String>?,
        scores: FloatArray?,
        config: VoiceTriggerConfig,
        onResult: (VoiceDetectionResult) -> Unit
    ) {
        if (matches.isNullOrEmpty()) return
        val phrase = config.triggerPhrase.lowercase()
        matches.forEachIndexed { i, match ->
            if (match.lowercase().contains(phrase)) {
                val confidence = scores?.getOrNull(i) ?: 0.6f
                onResult(VoiceDetectionResult(
                    phraseDetected = true,
                    phraseConfidence = confidence,
                    voiceMatchConfidence = null, // handled by VoiceProfileVerifier
                    rawTranscript = match
                ))
                return
            }
        }
    }
}
