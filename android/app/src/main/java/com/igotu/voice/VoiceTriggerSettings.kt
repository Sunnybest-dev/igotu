package com.igotu.voice

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

data class VoiceTriggerSettings(
    val enabled: Boolean = false,
    val triggerPhrase: String = "IGOT-U",
    val sensitivity: VoiceTriggerConfig.Sensitivity = VoiceTriggerConfig.Sensitivity.MEDIUM,
    val activationMode: ActivationMode = ActivationMode.COUNTDOWN,
    val countdownSeconds: Int = 5,
    val cooldownSeconds: Int = 30
) {
    enum class ActivationMode { IMMEDIATE, COUNTDOWN }
}

private val Context.voiceDataStore by preferencesDataStore("voice_settings")

@Singleton
class VoiceTriggerSettingsStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val ENABLED = booleanPreferencesKey("enabled")
        val PHRASE = stringPreferencesKey("phrase")
        val SENSITIVITY = stringPreferencesKey("sensitivity")
        val MODE = stringPreferencesKey("mode")
        val COUNTDOWN = intPreferencesKey("countdown")
        val COOLDOWN = intPreferencesKey("cooldown")
    }

    val settings: Flow<VoiceTriggerSettings> = context.voiceDataStore.data.map { p ->
        VoiceTriggerSettings(
            enabled = p[Keys.ENABLED] ?: false,
            triggerPhrase = p[Keys.PHRASE] ?: "IGOT-U",
            sensitivity = p[Keys.SENSITIVITY]?.let {
                VoiceTriggerConfig.Sensitivity.valueOf(it)
            } ?: VoiceTriggerConfig.Sensitivity.MEDIUM,
            activationMode = p[Keys.MODE]?.let {
                VoiceTriggerSettings.ActivationMode.valueOf(it)
            } ?: VoiceTriggerSettings.ActivationMode.COUNTDOWN,
            countdownSeconds = p[Keys.COUNTDOWN] ?: 5,
            cooldownSeconds = p[Keys.COOLDOWN] ?: 30
        )
    }

    suspend fun update(settings: VoiceTriggerSettings) {
        context.voiceDataStore.edit { p ->
            p[Keys.ENABLED] = settings.enabled
            p[Keys.PHRASE] = settings.triggerPhrase
            p[Keys.SENSITIVITY] = settings.sensitivity.name
            p[Keys.MODE] = settings.activationMode.name
            p[Keys.COUNTDOWN] = settings.countdownSeconds
            p[Keys.COOLDOWN] = settings.cooldownSeconds
        }
    }
}
