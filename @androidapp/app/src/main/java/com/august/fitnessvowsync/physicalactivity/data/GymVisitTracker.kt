package com.august.fitnessvowsync.physicalactivity.data

import android.content.SharedPreferences
import androidx.annotation.VisibleForTesting
import com.august.fitnessvowsync.helpers.DurationTypeAdapter
import com.august.fitnessvowsync.helpers.InstantTypeAdapter
import com.august.fitnessvowsync.physicalactivity.model.TrackedGymConfig
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.time.Duration
import java.time.Instant
import javax.inject.Inject

class GymVisitTracker(private val encryptedPreferences: SharedPreferences, public val validVisitLoiteringDuration: Duration) {

    @Inject constructor (encryptedPreferences: SharedPreferences) : this(encryptedPreferences, Duration.ofMinutes(15))

    data class GymVisitSession(val startTime: Instant, val endTime: Instant?, val isValid: Boolean, val gym: TrackedGymConfig)

    private val CURRENT_GYM_VISIT_PREF_KEY = "CURRENT_GYM_VISIT_PREF_KEY"
    private val GYM_VISITS_PREF_KEY = "GYM_VISITS_PREF_KEY"
    private val GYM_CONFIG_PREF_KEY = "GYM_CONFIG_PREF_KEY"
    private val gson: Gson by lazy {
        GsonBuilder()
            .registerTypeAdapter(Instant::class.java, InstantTypeAdapter().nullSafe())
            .registerTypeAdapter(Duration::class.java, DurationTypeAdapter().nullSafe())
            .create()
    }

    fun startVisit(startTime: Instant, gym: TrackedGymConfig) {
        updateCurrentGymVisit(null)

        val gymVisitSession = GymVisitSession(startTime, null, false, gym)

        updateCurrentGymVisit(gymVisitSession)
    }

    fun finishVisit(endTime: Instant): GymVisitSession {
        val session = getCurrentGymVisit()

        if (session == null) throw IllegalStateException("Unable to complete an inexistent gym visit.")

        updateCurrentGymVisit(null)

        val newSession = session.copy(endTime = endTime, isValid = Duration.between(session.startTime, endTime) >= validVisitLoiteringDuration)

        if (!newSession.isValid) return newSession

        updateGymVisitsList(newSession)

        return newSession
    }

    fun getVisits(periodStart: Instant, periodEnd: Instant): List<GymVisitSession> {
        return getGymVisitsList().filter { it.startTime >= periodStart && it.startTime <= periodEnd }
    }

    fun addTrackedGym(trackedGymConfig: TrackedGymConfig) {
        val trackedGyms = getTrackedGyms()

        if (trackedGyms.find { it.id == trackedGymConfig.id } != null) return

        trackedGyms.add(trackedGymConfig)

        with(encryptedPreferences.edit()) {
            putString(GYM_CONFIG_PREF_KEY, gson.toJson(trackedGyms))
            apply()
        }
    }

    fun getTrackedGyms(): MutableList<TrackedGymConfig> {
        val json = encryptedPreferences.getString(GYM_CONFIG_PREF_KEY, "[]")

        if (json.isNullOrBlank()) return mutableListOf<TrackedGymConfig>()

        val typeOfT = object : com.google.gson.reflect.TypeToken<List<TrackedGymConfig>>() {}.type

        return gson.fromJson(json, typeOfT)
    }

    fun gymGeofenceCreated(): Boolean {
        return encryptedPreferences.getBoolean("GYM_GEOFENCE_CREATED", false)
    }

    fun setGymGeofenceCreated(created: Boolean) {
        with(encryptedPreferences.edit()) {
            putBoolean("GYM_GEOFENCE_CREATED", created)
            apply()
        }
    }

    fun clearVisitHistory() {
        updateCurrentGymVisit(null)
        with(encryptedPreferences.edit()) {
            putString(GYM_VISITS_PREF_KEY, gson.toJson(mutableListOf<GymVisitSession>()))
            apply()
        }
    }

    private fun updateCurrentGymVisit(gymVisitSession: GymVisitSession?) {
        with(encryptedPreferences.edit()) {
            putString(CURRENT_GYM_VISIT_PREF_KEY, gson.toJson(gymVisitSession))
            apply()
        }
    }

    private fun getCurrentGymVisit(): GymVisitSession? {
        val json = encryptedPreferences.getString(CURRENT_GYM_VISIT_PREF_KEY, "null")

        if (json.isNullOrBlank()) return null

        val typeOfT = object : com.google.gson.reflect.TypeToken<GymVisitSession>() {}.type

        return gson.fromJson(json, typeOfT)
    }

    private fun updateGymVisitsList(newVisit: GymVisitSession) {
        val visits = getGymVisitsList() + newVisit

        with(encryptedPreferences.edit()) {
            putString(GYM_VISITS_PREF_KEY, gson.toJson(visits))
            apply()
        }
    }

    private fun getGymVisitsList(): List<GymVisitSession> {
        val json = encryptedPreferences.getString(GYM_VISITS_PREF_KEY, "[]")

        if (json.isNullOrBlank()) return mutableListOf<GymVisitSession>()

        val typeOfT = object : com.google.gson.reflect.TypeToken<List<GymVisitSession>>() {}.type

        return gson.fromJson(json, typeOfT)
    }

    @VisibleForTesting
    fun removeVisits(periodStart: Instant, periodEnd: Instant) {
        val filtered = getGymVisitsList().filterNot { it.startTime >= periodStart && it.startTime <= periodEnd }
        with(encryptedPreferences.edit()) {
            putString(GYM_VISITS_PREF_KEY, gson.toJson(filtered))
            apply()
        }
    }

    @VisibleForTesting
    fun clearCurrentVisit() {
        with(encryptedPreferences.edit()) {
            putString(CURRENT_GYM_VISIT_PREF_KEY, gson.toJson(null))
            apply()
        }
    }
}
