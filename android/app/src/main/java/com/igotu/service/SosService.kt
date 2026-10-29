package com.igotu.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.igotu.ui.EmergencyStatusActivity
import dagger.hilt.android.AndroidEntryPoint

/**
 * Foreground service that keeps the SOS event alive.
 * Declared with foregroundServiceType="location|microphone|camera" so Android
 * permits all three simultaneously during an active emergency.
 */
@AndroidEntryPoint
class SosService : Service() {

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val eventId = intent?.getStringExtra(EXTRA_EVENT_ID) ?: ""
        startForeground(NOTIFICATION_ID, buildNotification(eventId))
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun buildNotification(eventId: String): Notification {
        val channelId = "sos_active"
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(channelId, "SOS Active", NotificationManager.IMPORTANCE_HIGH)
        )
        val openIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, EmergencyStatusActivity::class.java).putExtra(EXTRA_EVENT_ID, eventId),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("🚨 SOS ACTIVE")
            .setContentText("Emergency event in progress. Tap to view.")
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setOngoing(true)
            .setContentIntent(openIntent)
            .build()
    }

    companion object {
        const val NOTIFICATION_ID = 1002
        const val EXTRA_EVENT_ID = "event_id"
    }
}
