package com.igotu.voice

/**
 * Abstraction over voice recognition backends.
 * Swap implementations without changing the rest of the app.
 */
interface VoiceTriggerProvider {
    /** Start listening. Calls [onResult] when a detection occurs. */
    fun startListening(config: VoiceTriggerConfig, onResult: (VoiceDetectionResult) -> Unit)
    fun stopListening()
    fun isAvailable(): Boolean
    /** Human-readable description of any availability limitation. */
    fun availabilityNote(): String?
}

data class VoiceTriggerConfig(
    val triggerPhrase: String,
    val sensitivity: Sensitivity,
    val voiceProfileId: String?
) {
    enum class Sensitivity { LOW, MEDIUM, HIGH }
}

data class VoiceDetectionResult(
    val phraseDetected: Boolean,
    val phraseConfidence: Float,       // 0..1
    val voiceMatchConfidence: Float?,  // null if no profile enrolled
    val rawTranscript: String?
) {
    /**
     * Combined confidence gate. Voice match is advisory — it reduces false
     * positives but is NOT a guaranteed identity mechanism.
     */
    fun meetsThreshold(sensitivity: VoiceTriggerConfig.Sensitivity): Boolean {
        val phraseThreshold = when (sensitivity) {
            VoiceTriggerConfig.Sensitivity.LOW -> 0.5f
            VoiceTriggerConfig.Sensitivity.MEDIUM -> 0.7f
            VoiceTriggerConfig.Sensitivity.HIGH -> 0.85f
        }
        if (phraseConfidence < phraseThreshold) return false
        // If a voice profile exists, require at least a weak match
        voiceMatchConfidence?.let { if (it < 0.4f) return false }
        return true
    }
}
