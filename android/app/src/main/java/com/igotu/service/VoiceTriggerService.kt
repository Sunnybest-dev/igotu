package com.igotu.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.igotu.sos.SosEngine
import com.igotu.sos.TriggerMethod
import com.igotu.voice.*
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Foreground service required for background microphone access.
 *
 * ANDROID REQUIREMENTS (enforced by OS):
 * • Must show a persistent notification — cannot be hidden.
 * • Declared with foregroundServiceType="microphone" in manifest.
 * • On Android 14+ the system enforces the microphone type at runtime.
 * • Battery optimization: users on some ROMs must grant "unrestricted"
 *   battery permission for reliable background operation.
 *
 * This service does NOT attempt to bypass any of these restrictions.
 */
@AndroidEntryPoint
class VoiceTriggerService : Service() {

    @Inject lateinit var provider: VoiceTriggerProvider
    @Inject lateinit var settingsStore: VoiceTriggerSettingsStore
    @Inject lateinit var verifier: VoiceProfileVerifier
    @Inject lateinit var sosEngine: SosEngine

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var lastTriggerTime = 0L

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, buildNotification())
        scope.launch { startVoiceListening() }
        return START_STICKY
    }

    private suspend fun startVoiceListening() {
        val settings = settingsStore.settings.first()
        if (!settings.enabled || !provider.isAvailable()) {
            stopSelf()
            return
        }
        val config = VoiceTriggerConfig(
            triggerPhrase = settings.triggerPhrase,
            sensitivity = settings.sensitivity,
            voiceProfileId = null
        )
        provider.startListening(config) { result ->
            handleDetection(result, settings)
        }
    }

    private fun handleDetection(result: VoiceDetectionResult, settings: VoiceTriggerSettings) {
        val now = System.currentTimeMillis()
        if (now - lastTriggerTime < settings.cooldownSeconds * 1000L) return

        val config = VoiceTriggerConfig(settings.triggerPhrase, settings.sensitivity, null)
        if (!result.meetsThreshold(config.sensitivity)) return

        lastTriggerTime = now

        when (settings.activationMode) {
            VoiceTriggerSettings.ActivationMode.IMMEDIATE ->
                sosEngine.activate(TriggerMethod.VOICE, result.phraseConfidence)

            VoiceTriggerSettings.ActivationMode.COUNTDOWN ->
                scope.launch { runCountdown(settings.countdownSeconds, result.phraseConfidence) }
        }
    }

    private suspend fun runCountdown(seconds: Int, confidence: Float) {
        // Broadcast countdown so UI can show cancel option
        for (remaining in seconds downTo 1) {
            sendBroadcast(Intent(ACTION_COUNTDOWN).putExtra(EXTRA_REMAINING, remaining))
            delay(1000)
        }
        sosEngine.activate(TriggerMethod.VOICE, confidence)
    }

    override fun onDestroy() {
        provider.stopListening()
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun buildNotification(): Notification {
        val channelId = "voice_trigger"
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(channelId, "Voice Trigger", NotificationManager.IMPORTANCE_LOW)
        )
        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("IGOT-U Voice Trigger Active")
            .setContentText("Listening for emergency phrase")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setOngoing(true)
            .build()
    }

    companion object {
        const val NOTIFICATION_ID = 1001
        const val ACTION_COUNTDOWN = "com.igotu.VOICE_COUNTDOWN"
        const val EXTRA_REMAINING = "remaining_seconds"
    }
}
