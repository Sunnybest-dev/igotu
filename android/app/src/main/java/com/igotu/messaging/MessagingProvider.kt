package com.igotu.messaging

import com.igotu.sos.SosEvent

/**
 * Abstraction over notification channels.
 * Add new channels (Telegram, Signal, etc.) without changing SosEngine.
 */
interface MessagingProvider {
    val channelId: String
    suspend fun send(message: EmergencyMessage): DeliveryResult
    fun isAvailable(): Boolean
}

data class EmergencyMessage(
    val sosEventId: String,
    val recipientId: String,
    val recipientAddress: String,  // phone, email, push token, etc.
    val shareUrl: String,
    val locationText: String?,
    val bodyText: String
)

data class DeliveryResult(
    val channel: String,
    val recipientId: String,
    val status: DeliveryStatus,
    val providerMessageId: String? = null,
    val error: String? = null
)

enum class DeliveryStatus { SENT, DELIVERED, READ, FAILED }
