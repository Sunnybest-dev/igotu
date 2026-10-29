package com.igotu.offline

import androidx.room.*
import kotlinx.coroutines.flow.Flow

// ── Entities ──────────────────────────────────────────────────────────────────

@Entity(tableName = "offline_sos_events")
data class OfflineSosEvent(
    @PrimaryKey val id: String,
    val triggerMethod: String,
    val voiceConfidence: Float?,
    val latitude: Double?,
    val longitude: Double?,
    val triggeredAt: Long,           // epoch millis
    val synced: Boolean = false,     // false = not yet sent to backend
    val shareUrl: String? = null
)

@Entity(tableName = "offline_contacts")
data class OfflineContact(
    @PrimaryKey val id: String,
    val name: String,
    val phone: String?,
    val email: String?,
    val relationship: String?,
    val notifySms: Boolean,
    val notifyEmail: Boolean,
    val notifyWhatsapp: Boolean,
    val priority: Int,
    val syncedFromServer: Boolean = false
)

@Entity(tableName = "offline_delivery_log")
data class OfflineDeliveryLog(
    @PrimaryKey(autoGenerate = true) val rowId: Long = 0,
    val sosEventId: String,
    val contactId: String,
    val channel: String,             // sms, whatsapp, email, push
    val status: String,              // SENT, FAILED, PENDING
    val sentAt: Long?,
    val error: String? = null
)

// ── DAOs ──────────────────────────────────────────────────────────────────────

@Dao
interface SosEventDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(event: OfflineSosEvent)

    @Query("SELECT * FROM offline_sos_events WHERE synced = 0")
    suspend fun getUnsynced(): List<OfflineSosEvent>

    @Query("UPDATE offline_sos_events SET synced = 1, shareUrl = :url WHERE id = :id")
    suspend fun markSynced(id: String, url: String)

    @Query("SELECT * FROM offline_sos_events ORDER BY triggeredAt DESC LIMIT 50")
    fun observeAll(): Flow<List<OfflineSosEvent>>
}

@Dao
interface ContactDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(contacts: List<OfflineContact>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(contact: OfflineContact)

    @Query("SELECT * FROM offline_contacts ORDER BY priority ASC")
    suspend fun getAll(): List<OfflineContact>

    @Query("SELECT * FROM offline_contacts WHERE notifyWhatsapp = 1 AND phone IS NOT NULL")
    suspend fun getWhatsAppContacts(): List<OfflineContact>

    @Query("SELECT * FROM offline_contacts WHERE notifySms = 1 AND phone IS NOT NULL")
    suspend fun getSmsContacts(): List<OfflineContact>

    @Query("DELETE FROM offline_contacts WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM offline_contacts")
    suspend fun deleteAll()
}

@Dao
interface DeliveryLogDao {
    @Insert
    suspend fun insert(log: OfflineDeliveryLog)

    @Query("SELECT * FROM offline_delivery_log WHERE sosEventId = :sosEventId")
    suspend fun getForEvent(sosEventId: String): List<OfflineDeliveryLog>
}
