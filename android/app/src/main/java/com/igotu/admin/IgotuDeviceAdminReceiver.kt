package com.igotu.admin

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * Required by Android to use DevicePolicyManager APIs (lock screen, remote wipe).
 * The user must explicitly grant Device Administrator permission — this cannot
 * be done silently. IGot-U requests it once during setup and explains why.
 *
 * Capabilities unlocked by Device Admin:
 *  - lockNow()       → immediately lock the screen
 *  - wipeData()      → factory reset the device (remote wipe)
 *  - resetPassword() → force a new PIN (deprecated API 26+, removed API 31)
 */
class IgotuDeviceAdminReceiver : DeviceAdminReceiver() {

    override fun onEnabled(context: Context, intent: Intent) {
        Log.i(TAG, "Device admin enabled")
    }

    override fun onDisabled(context: Context, intent: Intent) {
        Log.i(TAG, "Device admin disabled")
    }

    override fun onPasswordChanged(context: Context, intent: Intent) {}

    companion object {
        const val TAG = "IgotuDeviceAdmin"
    }
}
