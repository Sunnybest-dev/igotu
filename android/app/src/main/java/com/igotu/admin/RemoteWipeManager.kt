package com.igotu.admin

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

sealed class WipeResult {
    object Locked : WipeResult()
    object WipeInitiated : WipeResult()
    object AdminNotGranted : WipeResult()
    data class Error(val message: String) : WipeResult()
}

@Singleton
class RemoteWipeManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dpm = context.getSystemService(DevicePolicyManager::class.java)
    private val adminComponent = ComponentName(context, IgotuDeviceAdminReceiver::class.java)

    /** True if the user has already granted Device Administrator to IGot-U. */
    fun isAdminActive(): Boolean = dpm.isAdminActive(adminComponent)

    /**
     * Returns an Intent the Activity must launch to request Device Admin.
     * Cannot be done silently — Android always shows a system confirmation screen.
     */
    fun buildAdminRequestIntent(reason: String): Intent =
        Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
            putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent)
            putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, reason)
        }

    /**
     * Immediately lock the screen.
     * Requires Device Admin. If not granted, returns AdminNotGranted.
     */
    fun lockScreen(): WipeResult {
        if (!isAdminActive()) return WipeResult.AdminNotGranted
        return try {
            dpm.lockNow()
            WipeResult.Locked
        } catch (e: Exception) {
            WipeResult.Error(e.message ?: "Lock failed")
        }
    }

    /**
     * Factory reset the device — ALL data is erased.
     * This is irreversible. Only call after explicit user confirmation.
     *
     * flags = 0 means wipe internal storage only.
     * DevicePolicyManager.WIPE_EXTERNAL_STORAGE can be OR'd in to also wipe SD card.
     */
    fun wipeDevice(includeExternalStorage: Boolean = false): WipeResult {
        if (!isAdminActive()) return WipeResult.AdminNotGranted
        return try {
            val flags = if (includeExternalStorage)
                DevicePolicyManager.WIPE_EXTERNAL_STORAGE else 0
            dpm.wipeData(flags)
            WipeResult.WipeInitiated // device resets before this returns in practice
        } catch (e: Exception) {
            WipeResult.Error(e.message ?: "Wipe failed")
        }
    }
}
