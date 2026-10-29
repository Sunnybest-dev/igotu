package com.igotu.contacts

data class EmergencyContact(
    val id: String = "",
    val name: String,
    val phone: String? = null,
    val email: String? = null,
    val relationship: String? = null,
    val notifySms: Boolean = true,
    val notifyEmail: Boolean = false,
    val notifyWhatsapp: Boolean = false,
    val priority: Int = 1
)
