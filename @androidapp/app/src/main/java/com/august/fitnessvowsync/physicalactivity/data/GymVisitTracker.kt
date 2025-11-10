package com.august.fitnessvowsync.physicalactivity.data

import android.content.SharedPreferences
import androidx.annotation.VisibleForTesting
import com.august.fitnessvowsync.geofencing.GymConfig
import com.august.fitnessvowsync.helpers.DurationTypeAdapter
import com.august.fitnessvowsync.helpers.InstantTypeAdapter
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.time.Instant
import javax.inject.Inject
import kotlin.time.Duration

class GymVisitTracker @Inject constructor (private val encryptedPreferences: SharedPreferences) {
    data class GymVisitSession(val startTime: Instant, val endTime: Instant?, val isValid: Boolean, val gym: GymConfig)

    private val CURRENT_GYM_VISIT_PREF_KEY = "CURRENT_GYM_VISIT_PREF_KEY"
    private val GYM_VISITS_PREF_KEY = "GYM_VISITS_PREF_KEY"
    private val gson: Gson by lazy {
        GsonBuilder()
            .registerTypeAdapter(Instant::class.java, InstantTypeAdapter().nullSafe())
            .registerTypeAdapter(Duration::class.java, DurationTypeAdapter().nullSafe())
            .create()
    }

    fun startVisit(startTime: Instant, gym: GymConfig) {
        updateCurrentGymVisit(null)

        val gymVisitSession = GymVisitSession(startTime, null, false, gym)

        updateCurrentGymVisit(gymVisitSession)
    }

    fun markVisitAsValid() {
        val session = getCurrentGymVisit()

        if (session == null) throw IllegalStateException("Unable to mark an inexistent gym visit as valid.")

        updateCurrentGymVisit(GymVisitSession(
            session.startTime,
            session.endTime,
            true,
            session.gym)
        )
    }

    fun finishVisit(endTime: Instant): GymVisitSession {
        val session = getCurrentGymVisit()

        if (session == null) throw IllegalStateException("Unable to complete an inexistent gym visit.")

        updateCurrentGymVisit(null)

        val newSession = session.copy(endTime = endTime)

        updateGymVisitsList(newSession)

        return newSession
    }

    fun getVisits(periodStart: Instant, periodEnd: Instant): List<GymVisitSession> {
        return getGymVisitsList().filter { it.startTime >= periodStart && it.startTime <= periodEnd }
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
