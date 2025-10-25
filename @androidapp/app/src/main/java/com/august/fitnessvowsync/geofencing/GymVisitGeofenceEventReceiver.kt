package com.august.fitnessvowsync.geofencing

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.august.fitnessvowsync.MyApplication
import com.august.fitnessvowsync.service.GymVisitService
import com.august.fitnessvowsync.service.GymVisitService.GymVisitRecord
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import java.time.Instant
import javax.inject.Inject
import kotlin.time.Duration.Companion.minutes

class GymVisitGeofenceEventReceiver : BroadcastReceiver() {
    @Inject
    lateinit var gymVisitService: GymVisitService

    override fun onReceive(context: Context, intent: Intent) {
        Log.i("FitVow - Sync", "Geofence event received.")

        val app = context.applicationContext as MyApplication
        app.appComponent.inject(this)

        val geofencingEvent = GeofencingEvent.fromIntent(intent)

        if (geofencingEvent == null) {
            Log.i("FitVow - Sync", "Not a geofence event.")

            return
        }

        Log.i("FitVow - Sync", "Geofence event transition type: ${geofencingEvent.geofenceTransition}")

        if (geofencingEvent.hasError()) {
            Log.i("FitVow - Sync", "Geofence event error. Error Code: ${geofencingEvent.errorCode}.")

            return
        }

        val transitionType = geofencingEvent.geofenceTransition
        if (transitionType == Geofence.GEOFENCE_TRANSITION_DWELL) {
            Log.i("FitVow - Sync", "User went to the gym for 20 minutes.")
            // geofencingEvent.triggeringGeofences.get(0).requestId

            val time = Instant.ofEpochMilli(geofencingEvent.triggeringLocation!!.time)
            geofencingEvent.triggeringGeofences.get(0).requestId

            gymVisitService.addGymVisit(GymVisitRecord(time, 20.minutes))
        }
    }
}