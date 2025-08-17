package com.august.fitnessvowsync.service

import android.content.SharedPreferences
import com.august.fitnessvowsync.helpers.DurationTypeAdapter
import com.august.fitnessvowsync.helpers.InstantTypeAdapter
import com.august.fitnessvowsync.helpers.TimeRange
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.time.Instant
import javax.inject.Inject
import kotlin.time.Duration

class GeofencingService @Inject constructor (private val encryptedPreferences: SharedPreferences) {
    private val GYM_VISIT_PREF_KEY = "GYM_VISIT_PREF_KEY"
    private val gson: Gson by lazy {
        GsonBuilder()
            .registerTypeAdapter(Instant::class.java, InstantTypeAdapter())
            .registerTypeAdapter(Duration::class.java, DurationTypeAdapter())
            .create()
    }

    fun addGymVisit(visitRecord: GymVisitRecord) {
        val currentVisits = getAllGymVisits().toMutableList()

        currentVisits.add(0, visitRecord)

        val trimmedList = currentVisits.take(12)

        with(encryptedPreferences.edit()) {
            putString(GYM_VISIT_PREF_KEY, gson.toJson(trimmedList))
            apply()
        }
    }

    fun getGymVisits(): List<GymVisitRecord> {
        return getAllGymVisits().filter { it.time.isAfter(TimeRange.oneWeekAgo()) }
    }

    private fun getAllGymVisits(): List<GymVisitRecord> {
        val gymVisitsListString = encryptedPreferences.getString(GYM_VISIT_PREF_KEY, null)

        if (gymVisitsListString.isNullOrBlank()) return emptyList()

        val type = object : com.google.gson.reflect.TypeToken<List<GymVisitRecord>>() {}.type

        return gson.fromJson(gymVisitsListString, type)
    }
}

data class GymVisitRecord(
    public val time: Instant,
    public val duration: Duration
)