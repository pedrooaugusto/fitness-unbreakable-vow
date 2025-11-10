package com.august.fitnessvowsync.physicalactivity.data

import android.content.SharedPreferences
import android.util.Log
import com.august.fitnessvowsync.helpers.DurationTypeAdapter
import com.august.fitnessvowsync.helpers.InstantTypeAdapter
import com.august.fitnessvowsync.physicalactivity.model.GymVisitEvent
import com.august.fitnessvowsync.physicalactivity.model.PhysicalActivityEvent
import com.august.fitnessvowsync.physicalactivity.model.PhysicalActivityEvents
import com.august.fitnessvowsync.physicalactivity.model.RunningEvent
import com.august.fitnessvowsync.physicalactivity.model.SleepEvent
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type
import java.time.Duration
import java.time.Instant
import javax.inject.Inject

class PhysicalActivityEventRepository @Inject constructor(private val encryptedPreferences: SharedPreferences) {
    private val GYM_VISIT_PREF_KEY = "GYM_VISIT_EVENT_PREF_KEY"
    private val RUNNING_PREF_KEY = "RUNNING_EVENT_PREF_KEY"
    private val SLEEP_PREF_KEY = "SLEEP_EVENT_PREF_KEY"

    private val gson: Gson by lazy {
        GsonBuilder()
            .registerTypeAdapter(Instant::class.java, InstantTypeAdapter())
            .registerTypeAdapter(Duration::class.java, DurationTypeAdapter())
            .create()
    }

    fun getGymVisitSessions(): List<GymVisitEvent> {
        return list<GymVisitEvent>(GYM_VISIT_PREF_KEY, object : TypeToken<List<GymVisitEvent>>() {}.type)
    }

    fun getSleepSessions(): List<SleepEvent> {
        return list<SleepEvent>(SLEEP_PREF_KEY, object : TypeToken<List<SleepEvent>>() {}.type)
    }

    fun getRunningSessions(): List<RunningEvent> {
        return list<RunningEvent>(RUNNING_PREF_KEY, object : TypeToken<List<RunningEvent>>() {}.type)
    }

    fun registerGymVisit(eventsToAdd: List<GymVisitEvent>) {
        val events = getGymVisitSessions().toMutableList()

        for (newEvent in eventsToAdd) {
            if (!containsEvent(events, newEvent)) {
                events.add(newEvent.copy(syncDetails = null))
            }
        }

        editPreferences(GYM_VISIT_PREF_KEY, gson.toJson(events))

        Log.i("FitVow - Sync", "New gym visits added: $eventsToAdd")
    }

    fun registerSleep(eventsToAdd: List<SleepEvent>) {
        val events = getSleepSessions().toMutableList()

        for (newEvent in eventsToAdd) {
            if (!containsEvent(events, newEvent)) {
                events.add(newEvent.copy(syncDetails = null))
            }
        }

        editPreferences(SLEEP_PREF_KEY, gson.toJson(events))

        Log.i("FitVow - Sync", "New sleep sessions added: $eventsToAdd")
    }

    fun registerRunning(eventsToAdd: List<RunningEvent>) {
        val events = getRunningSessions().toMutableList()

        for (newEvent in eventsToAdd) {
            if (!containsEvent(events, newEvent)) {
                events.add(newEvent.copy(syncDetails = null))
            }
        }

        editPreferences(RUNNING_PREF_KEY, gson.toJson(events))

        Log.i("FitVow - Sync", "New running sessions added: $events")
    }

    fun updateRunning(eventsToUpdate: List<RunningEvent>) {
        val events = getRunningSessions().toMutableList()

        for (i in 0 until eventsToUpdate.size) {
            events[events.retrieveIndexOf(eventsToUpdate[i])] = eventsToUpdate[i]
        }

        editPreferences(RUNNING_PREF_KEY, gson.toJson(events))

        Log.i("FitVow - Sync", "Running sessions edited: $eventsToUpdate")
    }

    fun updateSleep(eventsToUpdate: List<SleepEvent>) {
        val events = getSleepSessions().toMutableList()

        for (i in 0 until eventsToUpdate.size) {
            events[events.retrieveIndexOf(eventsToUpdate[i])] = eventsToUpdate[i]
        }

        editPreferences(SLEEP_PREF_KEY, gson.toJson(events))

        Log.i("FitVow - Sync", "Sleep sessions edited: $eventsToUpdate")
    }

    fun updateGymVisit(eventsToUpdate: List<GymVisitEvent>) {
        val events = getGymVisitSessions().toMutableList()

        for (i in 0 until eventsToUpdate.size) {
            events[events.retrieveIndexOf(eventsToUpdate[i])] = eventsToUpdate[i]
        }

        editPreferences(GYM_VISIT_PREF_KEY, gson.toJson(events))

        Log.i("FitVow - Sync", "Gym visit sessions edited: $eventsToUpdate")
    }

    fun getSyncedSessions(limit: Int): PhysicalActivityEvents {
        val gymVisits = getGymVisitSessions().filter { it.syncDetails != null }.takeLast(limit)
        val sleepSessions = getSleepSessions().filter { it.syncDetails != null }.takeLast(limit)
        val runningSessions = getRunningSessions().filter { it.syncDetails != null }.takeLast(limit)

        return PhysicalActivityEvents(sleepSessions, runningSessions, gymVisits)
    }

    private fun <T> list(preferencesKey: String, type: Type): List<T> {
        try {
            val str = encryptedPreferences.getString(preferencesKey, null)

            if (str.isNullOrBlank()) return mutableListOf()

            return gson.fromJson<List<T>>(str, type)
        } catch (ex: Exception) {
            Log.e("FitVow - Sync", "Unable to deserialize ${preferencesKey}.", ex)

            return mutableListOf()
        }
    }

    private fun editPreferences(key: String, value: String) {
        with(encryptedPreferences.edit()) {
            putString(key, value)
            apply()
        }
    }

    private fun containsEvent(events: List<PhysicalActivityEvent>, event: PhysicalActivityEvent): Boolean {
        return events.any { it.sha256().contentEquals(event.sha256()) }
    }

    private fun List<PhysicalActivityEvent>.retrieveIndexOf(event: PhysicalActivityEvent): Int {
        val index = this.indexOfFirst { it.sha256().contentEquals(event.sha256()) }

        if (index == -1) throw IllegalStateException("Unable to update ${event}. It does not exists.")

        return index
    }
}