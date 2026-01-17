package com.august.fitnessvowsync.physicalactivity.collection

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.august.fitnessvowsync.MyApplication
import com.august.fitnessvowsync.helpers.NotificationService
import com.august.fitnessvowsync.physicalactivity.data.GymVisitTracker
import com.august.fitnessvowsync.physicalactivity.model.TrackedGymConfig
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import java.time.Duration
import java.time.Instant
import javax.inject.Inject

class GymVisitGeofenceEventReceiver : BroadcastReceiver() {
    @Inject
    lateinit var gymVisitTracker: GymVisitTracker
    @Inject
    lateinit var notificationService: NotificationService

    override fun onReceive(context: Context, intent: Intent) {
        Log.i("FitVow - Sync", "Geofence event received.")

        val app = context.applicationContext as MyApplication
        app.appComponent.inject(this)

        val geofencingEvent = GeofencingEvent.fromIntent(intent)

        if (geofencingEvent == null) {
            Log.i("FitVow - Sync", "Not a geofence event.")

            return
        }

        if (geofencingEvent.hasError()) {
            Log.i("FitVow - Sync", "Geofence event error. Error Code: ${geofencingEvent.errorCode}.")

            return
        }

        val triggeringGeofences = geofencingEvent.triggeringGeofences
        val triggeringLocation = geofencingEvent.triggeringLocation

        if (triggeringGeofences.isNullOrEmpty() || triggeringLocation == null) {
            Log.i("FitVow - Sync", "Geofence triggered with no fence data. ")

            return
        }

        if(triggeringLocation.isMock) {
            Log.i("FitVow - Sync", "Geofence event does not seem genuine. User is probably using mock locations.")

            return
        }

        Log.i("FitVow - Sync", "Geofence event transition type: ${geofencingEvent.geofenceTransition}")

        val timestamp = Instant.ofEpochMilli(triggeringLocation.time)
        val gym = gymVisitTracker.getTrackedGyms().find { it.id == triggeringGeofences[0].requestId }!!

        when(geofencingEvent.geofenceTransition) {
            Geofence.GEOFENCE_TRANSITION_ENTER -> {
                processOnEnter(gym, timestamp)
            }

            Geofence.GEOFENCE_TRANSITION_DWELL -> {
                processOnDwell(gym, app)
            }

            Geofence.GEOFENCE_TRANSITION_EXIT -> {
                processOnExit(gym, timestamp, app)
            }
        }
    }

    private fun processOnEnter(gym: TrackedGymConfig, timestamp: Instant) {
        Log.i("FitVow - Sync", "User entered the ${gym.id} gym.")

        gymVisitTracker.startVisit(timestamp, gym)
    }

    private fun processOnDwell(gym: TrackedGymConfig, context: Context) {
        Log.i("FitVow - Sync", "User stayed in the ${gym.id} gym long enough to be a valid visit.")

        notificationService.showGeofenceNotification("${gym.id} gym visit started.", context)
    }

    private fun processOnExit(gym: TrackedGymConfig, timestamp: Instant, context: Context) {
        Log.i("FitVow - Sync", "User exited the ${gym.id} gym.")

        try {
            val visit = gymVisitTracker.finishVisit(timestamp)

            if (!visit.isValid) return

            val duration = Duration.between(visit.startTime, visit.endTime)

            notificationService.showGeofenceNotification("${gym.id} gym visit ended. Visit duration time: ${duration.toMinutes()}min", context)
        } catch (ex: IllegalStateException) {
            Log.e("FitVow - Sync", "Unable complete gym visit.", ex)
        } catch (ex: Exception) {
            Log.e("FitVow - Sync", "Unexpected error on EXIT.", ex)
        }
    }
}