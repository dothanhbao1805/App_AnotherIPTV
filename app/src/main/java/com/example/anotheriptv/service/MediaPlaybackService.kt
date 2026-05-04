package com.example.anotheriptv.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.anotheriptv.R
import com.example.anotheriptv.presentation.player.xstream.PlayerLiveXstreamActivity

class MediaPlaybackService : Service() {

    companion object {
        const val CHANNEL_ID = "media_playback_channel"
        const val NOTIFICATION_ID = 1001
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val channelName = intent?.getStringExtra("channelName") ?: ""
        startForeground(NOTIFICATION_ID, buildNotification(channelName))
        return START_STICKY
    }

    private fun buildNotification(channelName: String): Notification {
        // Click notification → mở lại player
        val openIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, PlayerLiveXstreamActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(channelName.ifEmpty { getString(R.string.app_name) })
            .setContentText("Playing in background")
            .setSmallIcon(R.drawable.ic_play_circle)
            .setContentIntent(openIntent)
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Media Playback",
            NotificationManager.IMPORTANCE_LOW // LOW = không phát âm thanh thông báo
        )
        getSystemService(NotificationManager::class.java)
            .createNotificationChannel(channel)
    }

    override fun onBind(intent: Intent?): IBinder? = null
}