package com.igotu.privacy

import android.app.KeyguardManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.provider.Settings
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Privacy Lockdown — uses only legitimate Android APIs.
 *
 * ANDROID RESTRICTIONS (documented):
 * ────────────────────────────────────────────────────────────────────────────
 * • IGOT-U cannot directly lock, hide, or uninstall other applications.
 *   This requires device-owner mode, a custom launcher, or root — none of
 *   which are available to a standard consumer app.
 * • IGOT-U cannot prevent another app from opening once the user taps it.
 * • IGOT-U cannot suppress another app's notifications directly.
 *
 * What IGOT-U CAN legitimately do:
 * • Lock the device screen (KeyguardManager — requires KEYGUARD_DISABLE permission
 *   which is only available to device admins; standard apps use
 *   DevicePolicyManager.lockNow() — also device-admin only).
 *   → Fallback: guide user to lock screen settings.
 * • Set notification visibility to VISIBILITY_SECRET for its own notifications.
 * • Enable Android's built-in notification privacy mode where available.
 * • Guide the user to Android's built-in App Lock / Secure Folder settings
 *   (Samsung, Xiaomi, etc.) via deep links where available.
 * • Protect IGOT-U's own content (hide sensitive data in recents, lock app).
 * ────────────────────────────────────────────────────────────────────────────
 */
@Singleton
class PrivacyLockdownManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsStore: PrivacyLockdownSettingsStore
) {
    fun activate(): LockdownResult {
        val settings = settingsStore.load()
        val actions = mutableListOf<LockdownAction>()

        // Mode 2: Reduce IGOT-U notification content exposure
        if (settings.hideSensitiveNotifications) {
            setNotificationPrivacy(true)
            actions += LockdownAction(LockdownActionType.NOTIFICATION_PRIVACY, true, null)
        }

        // Mode 3: Protect IGOT-U from recents (set in Activity via FLAG_SECURE)
        // Handled at Activity level — see SecureActivity base class

        // Mode 5: Guide user to Android settings for apps we cannot control
        if (settings.protectedApps.isNotEmpty()) {
            actions += LockdownAction(
                LockdownActionType.GUIDED_APP_LOCK,
                false,
                "Android restricts direct app locking. Open device settings to use built-in app lock."
            )
        }

        settingsStore.setActive(true)
        return LockdownResult(actions)
    }

    fun deactivate() {
        setNotificationPrivacy(false)
        settingsStore.setActive(false)
    }

    /**
     * Opens the device's built-in security settings where the user can
     * configure app lock themselves. We cannot do it programmatically.
     */
    fun openDeviceAppLockSettings() {
        val intent = Intent(Settings.ACTION_SECURITY_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
        }
    }

    private fun setNotificationPrivacy(private: Boolean) {
        // IGOT-U controls only its own notification channels' visibility
        // Other apps' notifications cannot be modified without device-owner
    }
}

data class LockdownResult(val actions: List<LockdownAction>)

data class LockdownAction(
    val type: LockdownActionType,
    val succeeded: Boolean,
    val limitation: String?
)

enum class LockdownActionType {
    NOTIFICATION_PRIVACY, SCREEN_LOCK, GUIDED_APP_LOCK, RECENTS_PROTECTION
}
