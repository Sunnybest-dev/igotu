package com.igotu.data

import com.igotu.connections.ConnectionInvitation
import com.igotu.connections.ConnectionPermissions
import com.igotu.connections.ConnectionRequest
import com.igotu.connections.IgotuConnection
import com.igotu.contacts.EmergencyContact
import com.igotu.recording.RecordingMetadata
import retrofit2.http.*

// ── Auth models ───────────────────────────────────────────────────────────────
data class RegisterRequest(val email: String, val password: String, val fullName: String, val phone: String? = null)
data class LoginRequest(val email: String, val password: String)
data class AuthUser(val id: String, val email: String, val fullName: String, val phone: String?)
data class AuthResponse(val token: String, val user: AuthUser)

// ── SOS notify request ────────────────────────────────────────────────────────
data class SosNotifyRequest(
    val sosEventId: String,
    val triggerMethod: String,
    val voiceConfidence: Float? = null,
    val latitude: Double? = null,
    val longitude: Double? = null
)
data class SosNotifyResponse(val shareUrl: String, val contactsNotified: Int)

interface ApiService {

    // ── Auth ──────────────────────────────────────────────────────────────────
    @POST("auth/register")
    suspend fun register(@Body body: RegisterRequest): AuthResponse

    @POST("auth/login")
    suspend fun login(@Body body: LoginRequest): AuthResponse

    // ── Emergency Contacts ────────────────────────────────────────────────────
    @GET("emergency-contacts")
    suspend fun getEmergencyContacts(): List<EmergencyContact>

    @POST("emergency-contacts")
    suspend fun addEmergencyContact(@Body contact: EmergencyContact): EmergencyContact

    @PATCH("emergency-contacts/{id}")
    suspend fun updateEmergencyContact(
        @Path("id") id: String,
        @Body contact: EmergencyContact
    ): EmergencyContact

    @DELETE("emergency-contacts/{id}")
    suspend fun deleteEmergencyContact(@Path("id") id: String)

    // ── Connections ───────────────────────────────────────────────────────────
    @GET("connections")
    suspend fun getConnections(): List<IgotuConnection>

    @POST("connections/request")
    suspend fun sendConnectionRequest(@Body body: Map<String, String>): ConnectionRequest

    @POST("connections/request/{id}/accept")
    suspend fun acceptConnectionRequest(@Path("id") requestId: String): IgotuConnection

    @POST("connections/request/{id}/reject")
    suspend fun rejectConnectionRequest(@Path("id") requestId: String)

    @DELETE("connections/{id}")
    suspend fun removeConnection(@Path("id") connectionId: String)

    @PATCH("connections/{id}/permissions")
    suspend fun updateConnectionPermissions(
        @Path("id") connectionId: String,
        @Body permissions: ConnectionPermissions
    ): IgotuConnection

    @POST("connections/{id}/block")
    suspend fun blockConnection(@Path("id") connectionId: String)

    @POST("connections/invitation")
    suspend fun createConnectionInvitation(): ConnectionInvitation

    // ── Voice ─────────────────────────────────────────────────────────────────
    @POST("voice/enrollment")
    suspend fun submitVoiceEnrollment(@Body body: Map<String, Any>)

    @GET("voice/settings")
    suspend fun getVoiceSettings(): Map<String, Any>

    // ── Recordings ────────────────────────────────────────────────────────────
    @POST("recordings")
    suspend fun createRecording(@Body metadata: RecordingMetadata): RecordingMetadata

    @POST("recordings/{id}/upload")
    suspend fun uploadRecordingChunk(
        @Path("id") recordingId: String,
        @Body chunk: Map<String, Any>
    )

    @GET("recordings/{id}")
    suspend fun getRecording(@Path("id") recordingId: String): RecordingMetadata

    @DELETE("recordings/{id}")
    suspend fun deleteRecording(@Path("id") recordingId: String)

    // ── Messaging ─────────────────────────────────────────────────────────────
    @POST("messaging/sos-notify")
    suspend fun sosNotify(@Body body: SosNotifyRequest): SosNotifyResponse

    @POST("messaging/whatsapp/invite")
    suspend fun createWhatsAppInvite(@Body body: Map<String, String>): Map<String, String>

    @POST("messaging/share-link")
    suspend fun createEmergencyShareLink(@Body body: Map<String, String>): Map<String, String>

    @POST("messaging/push/notify")
    suspend fun sendPushNotification(@Body body: Map<String, String>)

    @GET("messaging/emergency/{id}/delivery-status")
    suspend fun getDeliveryStatus(@Path("id") sosEventId: String): List<Map<String, Any>>

    // ── Privacy Lockdown ──────────────────────────────────────────────────────
    @POST("privacy-lockdown/activate")
    suspend fun activateLockdown(@Body body: Map<String, Any>)

    @POST("privacy-lockdown/deactivate")
    suspend fun deactivateLockdown()
}
