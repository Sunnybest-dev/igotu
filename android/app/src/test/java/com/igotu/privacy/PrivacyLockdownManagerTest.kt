package com.igotu.privacy

import android.content.Context
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.*

class PrivacyLockdownManagerTest {

    private val context: Context = mock()
    private val settingsStore: PrivacyLockdownSettingsStore = mock()
    private lateinit var manager: PrivacyLockdownManager

    @Before
    fun setup() {
        manager = PrivacyLockdownManager(context, settingsStore)
        whenever(settingsStore.load()).thenReturn(PrivacyLockdownSettings(
            enabled = true,
            protectedApps = listOf("com.whatsapp", "com.android.gallery3d"),
            hideSensitiveNotifications = true
        ))
    }

    @Test
    fun `activate returns notification privacy action as succeeded`() {
        val result = manager.activate()
        val notifAction = result.actions.find { it.type == LockdownActionType.NOTIFICATION_PRIVACY }
        assertNotNull(notifAction)
        assertTrue(notifAction!!.succeeded)
    }

    @Test
    fun `activate returns guided app lock with limitation message`() {
        val result = manager.activate()
        val guidedAction = result.actions.find { it.type == LockdownActionType.GUIDED_APP_LOCK }
        assertNotNull(guidedAction)
        // Must NOT claim success — Android restricts direct app locking
        assertFalse(guidedAction!!.succeeded)
        assertNotNull(guidedAction.limitation)
    }

    @Test
    fun `activate does not claim to have deleted or hidden apps`() {
        val result = manager.activate()
        // No action should claim to have hidden or deleted another app
        result.actions.forEach { action ->
            if (action.type == LockdownActionType.GUIDED_APP_LOCK) {
                assertFalse("Must not falsely claim app was hidden", action.succeeded)
            }
        }
    }

    @Test
    fun `deactivate marks lockdown inactive`() {
        manager.activate()
        manager.deactivate()
        verify(settingsStore).setActive(false)
    }
}
