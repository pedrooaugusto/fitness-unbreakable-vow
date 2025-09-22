package com.august.fitnessvowsync.service

import android.Manifest
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.annotation.RequiresPermission
import com.august.fitnessvowsync.geofencing.GymGeofenceCreator
import com.august.fitnessvowsync.helpers.DurationTypeAdapter
import com.august.fitnessvowsync.helpers.InstantTypeAdapter
import com.august.fitnessvowsync.helpers.NotificationService
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.time.Instant
import javax.inject.Inject
import kotlin.time.Duration

interface GymVisitService {
    data class GymVisitRecord(val time: Instant, val duration: Duration)

    fun addGymVisit(visitRecord: GymVisitRecord)
    fun getGymVisits(periodStart: Instant, periodEnd: Instant): List<GymVisitRecord>
    fun setupGymGeofence(applicationContext: Context)

    class GymVisitServiceImpl @Inject constructor (
        private val encryptedPreferences: SharedPreferences,
        private val gymGeofenceCreator: GymGeofenceCreator,
        private val notificationService: NotificationService
    ): GymVisitService {
        private val GYM_VISIT_PREF_KEY = "GYM_VISIT_PREF_KEY"
        private val gson: Gson by lazy {
            GsonBuilder()
                .registerTypeAdapter(Instant::class.java, InstantTypeAdapter())
                .registerTypeAdapter(Duration::class.java, DurationTypeAdapter())
                .create()
        }

        override fun addGymVisit(visitRecord: GymVisitRecord) {
            val currentVisits = getAllGymVisits().toMutableList()

            currentVisits.add(0, visitRecord)

            val trimmedList = currentVisits.take(12)

            with(encryptedPreferences.edit()) {
                putString(GYM_VISIT_PREF_KEY, gson.toJson(trimmedList))
                apply()
            }

            Log.i("FitVow", "New gym visit added: ${visitRecord}")
        }

        override fun getGymVisits(periodStart: Instant, periodEnd: Instant): List<GymVisitRecord> {
            val a = getAllGymVisits().filter { it.time in periodStart..periodEnd}
            Log.i("FitVow", "Gym visit record $a")
            Log.i("FitVow", "Gym visit record $periodStart and $periodEnd")
            return a
        }

        @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_BACKGROUND_LOCATION])
        override fun setupGymGeofence(applicationContext: Context) {
            val gymGeofenceEnabled = gymGeofenceCreator.isGymGeofenceEnabled()

            if (!gymGeofenceEnabled) {
                gymGeofenceCreator.createGymGeofence(applicationContext)
                notificationService.showGeofenceNotification("Geofence monitoring has started.", applicationContext)
            }
        }

        private fun getAllGymVisits(): List<GymVisitRecord> {
            val gymVisitsListString = encryptedPreferences.getString(GYM_VISIT_PREF_KEY, null)

            if (gymVisitsListString.isNullOrBlank()) return emptyList()

            val type = object : com.google.gson.reflect.TypeToken<List<GymVisitRecord>>() {}.type

            return gson.fromJson(gymVisitsListString, type)
        }
    }

    class PreviewGymVisitService: GymVisitService {
        override fun addGymVisit(visitRecord: GymVisitRecord) {
            TODO("Not yet implemented")
        }

        override fun getGymVisits(
            periodStart: Instant,
            periodEnd: Instant
        ): List<GymVisitRecord> {
            TODO("Not yet implemented")
        }

        override fun setupGymGeofence(applicationContext: Context) {
            TODO("Not yet implemented")
        }
    }
}
