package com.august.fitnessvowsync.geofencing

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.core.content.ContextCompat
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import javax.inject.Inject

class GymGeofenceCreator @Inject constructor (private val geofencingClient: GeofencingClient, private val encryptedPreferences: SharedPreferences) {
    private val GYM_VISIT_MINIMUM_DURATION = 1 * 35 * 1000

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_BACKGROUND_LOCATION])
    fun createGymGeofence(context: Context) {
        Log.i("FitVow", "Trying to create gym geofence.")

        val gymLat = -22.895458330852602
        val gymLng = -43.27282316079158
        val radiusInMeters = 150f

        val geofence = Geofence.Builder()
            .setRequestId("GYM_GEOFENCE_ID")
            .setCircularRegion(gymLat, gymLng, radiusInMeters)
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_DWELL)
            .setLoiteringDelay(GYM_VISIT_MINIMUM_DURATION)
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
            .build()

        val geofencingRequest = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_DWELL)
            .addGeofence(geofence)
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

                with(encryptedPreferences.edit()) {
                    putBoolean("GYM_GEOFENCE_ENABLED", true)
                    apply()
                }
            }
            .addOnFailureListener { Log.i("FitVow", "Failed to add gym geofence: ${it.message}") }
    }

    fun isGymGeofenceEnabled(): Boolean {
        return encryptedPreferences.getBoolean("GYM_GEOFENCE_ENABLED", false)
    }

    fun hasRequiredPermissions(context: Context): Boolean {
        val fineLocation = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
        val backgroundLocation = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION)

        return arrayOf(fineLocation, backgroundLocation).all { it == PackageManager.PERMISSION_GRANTED }
    }
}