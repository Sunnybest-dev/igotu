package com.igotu.voice

import org.junit.Assert.*
import org.junit.Test

class VoiceDetectionResultTest {

    private fun result(
        phraseConf: Float,
        voiceConf: Float? = null
    ) = VoiceDetectionResult(
        phraseDetected = phraseConf > 0,
        phraseConfidence = phraseConf,
        voiceMatchConfidence = voiceConf,
        rawTranscript = null
    )

    @Test
    fun `phrase below threshold does not trigger`() {
        val r = result(phraseConf = 0.4f)
        assertFalse(r.meetsThreshold(VoiceTriggerConfig.Sensitivity.MEDIUM))
    }

    @Test
    fun `phrase above threshold triggers when no voice profile`() {
        val r = result(phraseConf = 0.8f, voiceConf = null)
        assertTrue(r.meetsThreshold(VoiceTriggerConfig.Sensitivity.MEDIUM))
    }

    @Test
    fun `phrase above threshold but weak voice match does not trigger`() {
        val r = result(phraseConf = 0.9f, voiceConf = 0.2f)
        assertFalse(r.meetsThreshold(VoiceTriggerConfig.Sensitivity.MEDIUM))
    }

    @Test
    fun `phrase above threshold and sufficient voice match triggers`() {
        val r = result(phraseConf = 0.9f, voiceConf = 0.6f)
        assertTrue(r.meetsThreshold(VoiceTriggerConfig.Sensitivity.MEDIUM))
    }

    @Test
    fun `high sensitivity requires higher phrase confidence`() {
        val r = result(phraseConf = 0.75f)
        assertFalse(r.meetsThreshold(VoiceTriggerConfig.Sensitivity.HIGH))
        assertTrue(r.meetsThreshold(VoiceTriggerConfig.Sensitivity.LOW))
    }
}
