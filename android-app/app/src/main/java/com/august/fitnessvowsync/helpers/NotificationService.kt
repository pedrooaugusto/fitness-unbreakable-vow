package com.august.fitnessvowsync.helpers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import com.august.fitnessvowsync.R
import javax.inject.Inject

class NotificationService @Inject constructor(){
    public fun showGeofenceNotification(text: String, context: Context) {
        val channelId = "geofence_status"
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channel = NotificationChannel(
            channelId,
            "Geofence Status",
            NotificationManager.IMPORTANCE_LOW
        )

        manager.createNotificationChannel(channel)

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("FitVow - Sync")
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        manager.notify((100..999).random(), notification)
    }
}