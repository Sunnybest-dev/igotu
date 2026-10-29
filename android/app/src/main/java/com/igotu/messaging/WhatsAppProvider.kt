package com.igotu.messaging

import android.content.Context
import android.content.Intent
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * WhatsApp integration via legitimate Android share mechanisms.
 *
 * LIMITATIONS (documented):
 * ─────────────────────────────────────────────────────────────────────────
 * • IGOT-U does NOT use unofficial WhatsApp automation, web scraping,
 *   or undocumented APIs.
 * • Automated background messaging requires WhatsApp Business API
 *   (cloud-hosted, requires Meta approval). That path is isolated behind
 *   this interface so it can be added later without changing the rest of
 *   the app.
 * • The consumer share flow (below) opens WhatsApp's share sheet and
 *   requires the user to confirm sending. It cannot send silently.
 * • If WhatsApp is not installed, the share falls back to the system
 *   share sheet.
 * ─────────────────────────────────────────────────────────────────────────
 */
class WhatsAppProvider @Inject constructor(
    @ApplicationContext private val context: Context
) : MessagingProvider {

    override val channelId = "whatsapp"

    override fun isAvailable(): Boolean {
        return try {
            context.packageManager.getPackageInfo("com.whatsapp", 0)
            true
        } catch (e: Exception) { false }
    }

    /**
     * Opens WhatsApp share intent with the emergency message.
     * The user must confirm sending — this cannot be automated without
     * WhatsApp Business API approval.
     */
    override suspend fun send(message: EmergencyMessage): DeliveryResult {
        return try {
            val text = buildShareText(message)
            val intent = buildShareIntent(message.recipientAddress, text)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(intent)
            DeliveryResult(channelId, message.recipientId, DeliveryStatus.SENT,
                error = "User confirmation required — automated sending not available in consumer mode")
        } catch (e: Exception) {
            DeliveryResult(channelId, message.recipientId, DeliveryStatus.FAILED, error = e.message)
        }
    }

    private fun buildShareText(message: EmergencyMessage) = buildString {
        appendLine("🚨 IGOT-U Emergency Alert")
        appendLine()
        message.locationText?.let { appendLine("📍 $it") }
        appendLine("🔗 ${message.shareUrl}")
        appendLine()
        appendLine(message.bodyText)
    }

    private fun buildShareIntent(phoneNumber: String, text: String): Intent {
        // Direct WhatsApp share to a specific number if provided
        return if (phoneNumber.isNotBlank()) {
            Intent(Intent.ACTION_VIEW,
                Uri.parse("https://wa.me/${phoneNumber.replace("+", "")}?text=${Uri.encode(text)}"))
        } else {
            Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                setPackage("com.whatsapp")
                putExtra(Intent.EXTRA_TEXT, text)
            }
        }
    }
}
