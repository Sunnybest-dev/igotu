package com.igotu.sos

import java.time.Instant

enum class TriggerMethod { BUTTON, VOICE, HARDWARE, NOTIFICATION, WEARABLE }
enum class SosState { IDLE, COUNTDOWN, ACTIVE, RESOLVING, RESOLVED, CANCELLED }
enum class RecordingState { IDLE, STARTING, ACTIVE, STOPPING, UPLOADING, COMPLETE, FAILED }
enum class DeliveryStatus { PENDING, SENT, DELIVERED, READ, FAILED }

data class SosEvent(
    val id: String,
    val triggeredAt: Instant,
    val triggerMethod: TriggerMethod,
    val voiceConfidence: Float? = null,
    val state: SosState = SosState.ACTIVE,
    val locationCaptured: Boolean = false,
    val recordingState: RecordingState = RecordingState.IDLE,
    val contactsNotified: List<String> = emptyList(),
    val shareUrl: String? = null,
    val resolvedAt: Instant? = null,
    val cancelledAt: Instant? = null
)

data class SosAction(
    val type: ActionType,
    val status: ActionStatus = ActionStatus.PENDING,
    val error: String? = null
) {
    enum class ActionType {
        CAPTURE_LOCATION, START_RECORDING, NOTIFY_IGOTU_CONTACTS,
        NOTIFY_WHATSAPP, SEND_PUSH, SEND_SMS, PRIVACY_LOCKDOWN
    }
    enum class ActionStatus { PENDING, RUNNING, SUCCESS, FAILED }
}
