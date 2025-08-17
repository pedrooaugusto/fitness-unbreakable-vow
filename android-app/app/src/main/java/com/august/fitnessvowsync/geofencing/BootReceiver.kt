package com.august.fitnessvowsync.geofencing

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.annotation.RequiresPermission
import com.august.fitnessvowsync.MyApplication
import javax.inject.Inject
import com.august.fitnessvowsync.helpers.NotificationService

class BootReceiver : BroadcastReceiver() {
    @Inject
    lateinit var gymGeofenceCreator: GymGeofenceCreator
    @Inject
    lateinit var notificationService: NotificationService

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_BACKGROUND_LOCATION])
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            val app = context.applicationContext as MyApplication

            app.appComponent.inject(this)

            if (gymGeofenceCreator.hasRequiredPermissions(context)) {
                gymGeofenceCreator.createGymGeofence(context)
                notificationService.showGeofenceNotification("Geofence monitoring has resumed after reboot.", context)
            } else {
                notificationService.showGeofenceNotification("Unable to register geofence monitoring.", context)
            }
        }
    }
}