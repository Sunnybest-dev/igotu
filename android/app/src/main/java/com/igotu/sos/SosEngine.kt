package com.igotu.sos

import android.content.Context
import android.content.Intent
import com.igotu.data.ApiService
import com.igotu.data.SosNotifyRequest
import com.igotu.messaging.MessagingOrchestrator
import com.igotu.recording.RecordingManager
import com.igotu.service.NetworkMonitor
import com.igotu.service.SosService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SosEngine @Inject constructor(
    @ApplicationContext private val context: Context,
    private val recordingManager: RecordingManager,
    private val messagingOrchestrator: MessagingOrchestrator,
    private val auditLogger: SosAuditLogger,
    private val api: ApiService,
    private val networkMonitor: NetworkMonitor
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _state = MutableStateFlow<SosEvent?>(null)
    val state: StateFlow<SosEvent?> = _state

    // Last known location — updated by SosService when location arrives
    var lastLatitude: Double? = null
    var lastLongitude: Double? = null

    fun activate(method: TriggerMethod, voiceConfidence: Float? = null) {
        if (_state.value?.state == SosState.ACTIVE) return

        val event = SosEvent(
            id = UUID.randomUUID().toString(),
            triggeredAt = Instant.now(),
            triggerMethod = method,
            voiceConfidence = voiceConfidence
        )
        _state.value = event
        auditLogger.logTrigger(event)

        context.startForegroundService(Intent(context, SosService::class.java).apply {
            putExtra(SosService.EXTRA_EVENT_ID, event.id)
        })

        // All actions run independently — one failure never stops others
        scope.launch { runAction(SosAction.ActionType.NOTIFY_IGOTU_CONTACTS) { notifyBackend(event) } }
        scope.launch { runAction(SosAction.ActionType.START_RECORDING) { recordingManager.startEmergencyRecording(event.id) } }
        scope.launch { runAction(SosAction.ActionType.NOTIFY_WHATSAPP) { messagingOrchestrator.shareViaWhatsApp(event) } }
        scope.launch { runAction(SosAction.ActionType.SEND_PUSH) { messagingOrchestrator.sendPush(event) } }

        // Log offline state — network actions will retry via WorkManager when connectivity returns
        if (!networkMonitor.isCurrentlyOnline()) {
            auditLogger.logAction(SosAction.ActionType.NOTIFY_IGOTU_CONTACTS,
                SosAction.ActionStatus.PENDING, "Device offline — will retry when connected")
        }
    }

    fun resolve(eventId: String) {
        val current = _state.value ?: return
        if (current.id != eventId) return
        _state.value = current.copy(state = SosState.RESOLVED, resolvedAt = Instant.now())
        auditLogger.logResolution(current.id)
        recordingManager.stopEmergencyRecording(eventId)
        context.stopService(Intent(context, SosService::class.java))
    }

    fun cancel(eventId: String) {
        val current = _state.value ?: return
        if (current.id != eventId) return
        _state.value = current.copy(state = SosState.CANCELLED, cancelledAt = Instant.now())
        auditLogger.logCancellation(current.id)
        recordingManager.stopEmergencyRecording(eventId)
        context.stopService(Intent(context, SosService::class.java))
    }

    private suspend fun notifyBackend(event: SosEvent) {
        val response = api.sosNotify(
            SosNotifyRequest(
                sosEventId = event.id,
                triggerMethod = event.triggerMethod.name,
                voiceConfidence = event.voiceConfidence,
                latitude = lastLatitude,
                longitude = lastLongitude
            )
        )
        // Update state with share URL once backend responds
        _state.value = _state.value?.copy(shareUrl = response.shareUrl)
    }

    private suspend fun runAction(type: SosAction.ActionType, block: suspend () -> Unit) {
        try {
            block()
            auditLogger.logAction(type, SosAction.ActionStatus.SUCCESS)
        } catch (e: Exception) {
            auditLogger.logAction(type, SosAction.ActionStatus.FAILED, e.message)
        }
    }
}
