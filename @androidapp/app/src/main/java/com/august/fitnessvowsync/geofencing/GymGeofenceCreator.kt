package com.august.fitnessvowsync.geofencing

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.core.content.ContextCompat
import com.august.fitnessvowsync.helpers.NotificationService
import com.august.fitnessvowsync.physicalactivity.collection.GymVisitGeofenceEventReceiver
import com.august.fitnessvowsync.physicalactivity.data.GymVisitTracker
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import java.time.Duration
import javax.inject.Inject

class GymGeofenceCreator @Inject constructor (
    private val geofencingClient: GeofencingClient,
    private val notificationService: NotificationService,
    private val gymVisitTracker: GymVisitTracker,
) {
    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_BACKGROUND_LOCATION])
    fun create(context: Context) {
        Log.i("FitVow", "Trying to create gym geofences.")

        gymVisitTracker.setGymGeofenceCreated(false)

        if (!hasRequiredPermissions(context)) {
            notificationService.showGeofenceNotification("Not enough permissions to start gym geofences monitoring.", context)

            return
        }

        val trackedGyms = gymVisitTracker.getTrackedGyms()
        val geofences = mutableListOf<Geofence>()

        for (gym in trackedGyms) {
            geofences.add(
                Geofence.Builder()
                    .setRequestId(gym.id)
                    .setCircularRegion(gym.latitude, gym.longitude, gym.radius.toFloat())
                    .setLoiteringDelay(GymVisitTracker.LOITERING_DELAY.toMillis().toInt())
                    .setTransitionTypes(
                        Geofence.GEOFENCE_TRANSITION_ENTER or
                        Geofence.GEOFENCE_TRANSITION_EXIT or
                        Geofence.GEOFENCE_TRANSITION_DWELL
                    )
                    .setExpirationDuration(Geofence.NEVER_EXPIRE)
                    .build()
            )
        }

        val geofencingRequest = GeofencingRequest.Builder()
            .setInitialTrigger(Geofence.GEOFENCE_TRANSITION_ENTER)
            .addGeofences(geofences)
            .build()

        val intent = Intent(context, GymVisitGeofenceEventReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )

        geofencingClient
            .addGeofences(geofencingRequest, pendingIntent)
            .addOnSuccessListener {
                Log.i("FitVow", "Gym Geofence added!")

                gymVisitTracker.setGymGeofenceCreated(true)
                notificationService.showGeofenceNotification("Gym geofences monitoring has started.", context)
            }
            .addOnFailureListener {
                Log.i("FitVow", "Failed to add gym geofence: ${it.message}")

                gymVisitTracker.setGymGeofenceCreated(false)
                notificationService.showGeofenceNotification("Unable to start gym geofences.", context)
            }
    }

    private fun hasRequiredPermissions(context: Context): Boolean {
        val fineLocation = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
        val backgroundLocation = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION)

        return arrayOf(fineLocation, backgroundLocation).all { it == PackageManager.PERMISSION_GRANTED }
    }
}