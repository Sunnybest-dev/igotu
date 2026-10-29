package com.igotu.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.igotu.voice.VoiceTriggerSettingsStore
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import javax.inject.Inject

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject lateinit var settingsStore: VoiceTriggerSettingsStore

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        CoroutineScope(Dispatchers.IO).launch {
            val settings = settingsStore.settings.first()
            if (settings.enabled) {
                context.startForegroundService(Intent(context, VoiceTriggerService::class.java))
            }
        }
    }
}
