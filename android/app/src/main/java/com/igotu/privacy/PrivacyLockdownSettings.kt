package com.igotu.privacy

import android.content.Context
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

data class PrivacyLockdownSettings(
    val enabled: Boolean = false,
    val protectedApps: List<String> = emptyList(), // package names
    val hideSensitiveNotifications: Boolean = true,
    val protectIgotuContent: Boolean = true,
    val activateWithSos: Boolean = true,
    val isCurrentlyActive: Boolean = false
)

@Singleton
class PrivacyLockdownSettingsStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs = context.getSharedPreferences("privacy_lockdown", Context.MODE_PRIVATE)
    private val gson = Gson()

    fun load(): PrivacyLockdownSettings {
        val json = prefs.getString("settings", null) ?: return PrivacyLockdownSettings()
        return gson.fromJson(json, PrivacyLockdownSettings::class.java)
    }

    fun save(settings: PrivacyLockdownSettings) {
        prefs.edit().putString("settings", gson.toJson(settings)).apply()
    }

    fun setActive(active: Boolean) {
        save(load().copy(isCurrentlyActive = active))
    }
}
