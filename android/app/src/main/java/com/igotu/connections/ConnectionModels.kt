package com.igotu.connections

import java.time.Instant

enum class ConnectionStatus { PENDING_SENT, PENDING_RECEIVED, CONNECTED, BLOCKED }
enum class VerificationMethod { PHONE, EMAIL, IGOTU_ACCOUNT, INVITATION_CODE, QR_CODE }

data class IgotuConnection(
    val connectionId: String,
    val userId: String,
    val connectedUserId: String,
    val status: ConnectionStatus,
    val permissions: ConnectionPermissions,
    val createdAt: Instant,
    val verificationMethod: VerificationMethod? = null
)

data class ConnectionPermissions(
    val receiveSosAlerts: Boolean = true,
    val receiveLiveLocation: Boolean = false,
    val receiveJourneyAlerts: Boolean = false,
    val receiveCheckInAlerts: Boolean = false,
    val receiveEmergencyRecordings: Boolean = false
)

data class ConnectionRequest(
    val requestId: String,
    val fromUserId: String,
    val toUserId: String,
    val invitationToken: String,
    val createdAt: Instant,
    val expiresAt: Instant,
    val accepted: Boolean? = null  // null = pending
)

data class ConnectionInvitation(
    val token: String,          // short-lived, single-use
    val createdAt: Instant,
    val expiresAt: Instant,
    val qrCodeData: String,     // contains only the token, no PII
    val shareUrl: String        // https://igotu.app/connect?token=...
)
