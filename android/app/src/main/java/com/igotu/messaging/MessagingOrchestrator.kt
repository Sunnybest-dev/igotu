package com.igotu.messaging

import com.igotu.connections.ConnectionsRepository
import com.igotu.contacts.EmergencyContactsRepository
import com.igotu.data.ApiService
import com.igotu.sos.SosEvent
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MessagingOrchestrator @Inject constructor(
    private val connectionsRepository: ConnectionsRepository,
    private val emergencyContactsRepository: EmergencyContactsRepository,
    private val whatsAppProvider: WhatsAppProvider,
    private val smsProvider: SmsProvider,
    private val pushProvider: PushProvider,
    private val api: ApiService
) {
    /**
     * Notify all IGOT-U app connections that have SOS permission via push.
     */
    suspend fun notifyContacts(event: SosEvent): List<DeliveryResult> = coroutineScope {
        val connections = connectionsRepository.getConnections().getOrElse { emptyList() }
        connections
            .filter { it.permissions.receiveSosAlerts }
            .map { connection ->
                async { pushProvider.send(buildMessage(event, connection.connectedUserId, "")) }
            }
            .map { it.await() }
    }

    /**
     * Send WhatsApp alert to every emergency contact that has notifyWhatsapp = true.
     * Each contact gets a direct wa.me deep link to their number.
     * Falls back to generic share sheet if no WhatsApp contacts are configured.
     */
    suspend fun shareViaWhatsApp(event: SosEvent) {
        if (!whatsAppProvider.isAvailable()) return
        val shareUrl = event.shareUrl ?: fetchShareUrl(event.id)

        val whatsAppContacts = emergencyContactsRepository.getContacts()
            .getOrElse { emptyList() }
            .filter { it.notifyWhatsapp && !it.phone.isNullOrBlank() }

        if (whatsAppContacts.isNotEmpty()) {
            whatsAppContacts.forEach { contact ->
                whatsAppProvider.send(EmergencyMessage(
                    sosEventId = event.id,
                    recipientId = contact.id,
                    recipientAddress = contact.phone!!,
                    shareUrl = shareUrl,
                    locationText = null,
                    bodyText = "🚨 Emergency alert from IGOT-U. I need help."
                ))
            }
        } else {
            // No contacts configured — open generic WhatsApp share sheet
            whatsAppProvider.send(EmergencyMessage(
                sosEventId = event.id,
                recipientId = "whatsapp_share",
                recipientAddress = "",
                shareUrl = shareUrl,
                locationText = null,
                bodyText = "🚨 Emergency alert from IGOT-U. I need help."
            ))
        }
    }

    suspend fun sendPush(event: SosEvent) {
        val shareUrl = event.shareUrl ?: fetchShareUrl(event.id)
        pushProvider.send(EmergencyMessage(
            sosEventId = event.id,
            recipientId = "broadcast",
            recipientAddress = "",
            shareUrl = shareUrl,
            locationText = null,
            bodyText = "Emergency alert"
        ))
    }

    private suspend fun fetchShareUrl(sosEventId: String): String {
        return try {
            api.createEmergencyShareLink(mapOf("sosEventId" to sosEventId))["shareUrl"]
                ?: "https://igotu.app"
        } catch (e: Exception) {
            "https://igotu.app"
        }
    }

    private fun buildMessage(event: SosEvent, recipientId: String, address: String) =
        EmergencyMessage(
            sosEventId = event.id,
            recipientId = recipientId,
            recipientAddress = address,
            shareUrl = event.shareUrl ?: "",
            locationText = null,
            bodyText = "Emergency alert from IGOT-U"
        )
}
