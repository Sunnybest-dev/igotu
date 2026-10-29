package com.igotu.contacts

import com.igotu.data.ApiService
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EmergencyContactsRepository @Inject constructor(
    private val api: ApiService
) {
    suspend fun getContacts(): Result<List<EmergencyContact>> = runCatching {
        api.getEmergencyContacts()
    }

    suspend fun addContact(contact: EmergencyContact): Result<EmergencyContact> = runCatching {
        api.addEmergencyContact(contact)
    }

    suspend fun updateContact(contact: EmergencyContact): Result<EmergencyContact> = runCatching {
        api.updateEmergencyContact(contact.id, contact)
    }

    suspend fun deleteContact(contactId: String): Result<Unit> = runCatching {
        api.deleteEmergencyContact(contactId)
    }
}
