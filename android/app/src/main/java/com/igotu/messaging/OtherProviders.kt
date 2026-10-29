package com.igotu.messaging

import android.content.Context
import android.telephony.SmsManager
import com.igotu.data.ApiService
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class SmsProvider @Inject constructor(
    @ApplicationContext private val context: Context
) : MessagingProvider {
    override val channelId = "sms"
    override fun isAvailable() = true

    override suspend fun send(message: EmergencyMessage): DeliveryResult {
        return try {
            val smsManager = context.getSystemService(SmsManager::class.java)
            smsManager.sendTextMessage(
                message.recipientAddress, null,
                "🚨 IGOT-U SOS: ${message.shareUrl}",
                null, null
            )
            // SMS delivery confirmation requires delivery intent — simplified here
            DeliveryResult(channelId, message.recipientId, DeliveryStatus.SENT)
        } catch (e: Exception) {
            DeliveryResult(channelId, message.recipientId, DeliveryStatus.FAILED, error = e.message)
        }
    }
}

class PushProvider @Inject constructor(
    private val api: ApiService
) : MessagingProvider {
    override val channelId = "push"
    override fun isAvailable() = true

    override suspend fun send(message: EmergencyMessage): DeliveryResult {
        return try {
            api.sendPushNotification(mapOf(
                "sosEventId" to message.sosEventId,
                "recipientId" to message.recipientId,
                "shareUrl" to message.shareUrl
            ))
            DeliveryResult(channelId, message.recipientId, DeliveryStatus.SENT)
        } catch (e: Exception) {
            DeliveryResult(channelId, message.recipientId, DeliveryStatus.FAILED, error = e.message)
        }
    }
}
