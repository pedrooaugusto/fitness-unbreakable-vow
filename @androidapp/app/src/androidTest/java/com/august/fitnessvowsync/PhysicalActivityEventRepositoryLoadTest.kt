package com.august.fitnessvowsync

import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.august.fitnessvowsync.dagger.EncryptedSharedPreferencesModule
import com.august.fitnessvowsync.physicalactivity.data.PhysicalActivityEventRepository
import com.august.fitnessvowsync.physicalactivity.model.GymVisitEvent
import com.august.fitnessvowsync.physicalactivity.model.PhysicalActivityEvent
import com.august.fitnessvowsync.physicalactivity.model.RunningEvent
import com.august.fitnessvowsync.physicalactivity.model.SleepEvent
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant

@RunWith(AndroidJUnit4::class)
class PhysicalActivityEventRepositoryLoadTest {
    private lateinit var repository: PhysicalActivityEventRepository

    private val TAG = "FitVow-RepoLoadTest"

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val encryptedPreferences = EncryptedSharedPreferencesModule().let { it.provideEncryptedSharedPreferences(context, it.provideMainKeyAlias()) }
        encryptedPreferences.edit().clear().apply()
        repository = PhysicalActivityEventRepository(encryptedPreferences)
    }

    @After
    fun tearDown() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val encryptedPreferences = EncryptedSharedPreferencesModule().let { it.provideEncryptedSharedPreferences(context, it.provideMainKeyAlias()) }
        encryptedPreferences.edit().clear().apply()
    }

    @Test
    fun load_register_retrieve_update_90_each() {
        val totalStart = System.nanoTime()

        val base = Instant.ofEpochSecond(Instant.now().epochSecond)

        val sleepEvents = (0 until 90).map { i ->
            SleepEvent(
                timestamp = base.plusSeconds(i.toLong()),
                durationInMinutes = 60 + (i % 30),
                avgBpm = 45 + (i % 20),
                syncDetails = null
            )
        }

        val runningEvents = (0 until 90).map { i ->
            RunningEvent(
                timestamp = base.plusSeconds(1000 + i.toLong()),
                distanceInMeters = 2000 + (i * 5),
                paceInSecondsPerKm = 250 + (i % 120),
                avgBpm = 110 + (i % 30),
                syncDetails = null
            )
        }

        val gymEvents = (0 until 90).map { i ->
            GymVisitEvent(
                location = GymVisitEvent.Location(0.0, 0.0),
                timestamp = base.plusSeconds(2000 + i.toLong()),
                durationInMinutes = 30 + (i % 45),
                avgBpm = 105 + (i % 40),
                maxBpm = 150 + (i % 30),
                "GYM#1",
                syncDetails = null
            )
        }

        // Register
        val regStart = System.nanoTime()
        val regSleepStart = System.nanoTime(); repository.registerSleep(sleepEvents); val regSleepMs = (System.nanoTime() - regSleepStart) / 1_000_000
        val regRunStart = System.nanoTime(); repository.registerRunning(runningEvents); val regRunMs = (System.nanoTime() - regRunStart) / 1_000_000
        val regGymStart = System.nanoTime(); repository.registerGymVisit(gymEvents); val regGymMs = (System.nanoTime() - regGymStart) / 1_000_000
        val regMs = (System.nanoTime() - regStart) / 1_000_000

        // Retrieve
        val retStart = System.nanoTime()
        val sleeps = repository.getSleepSessions(); val retSleepMs = (System.nanoTime() - retStart) / 1_000_000
        val runs = repository.getRunningSessions(); val retRunMs = (System.nanoTime() - retStart) / 1_000_000
        val gyms = repository.getGymVisitSessions(); val retGymMs = (System.nanoTime() - retStart) / 1_000_000
        val retMs = (System.nanoTime() - retStart) / 1_000_000

        assertEquals(90, sleeps.size)
        assertEquals(90, runs.size)
        assertEquals(90, gyms.size)

        // Update
        val updStart = System.nanoTime()
        val detailsBase = base.plusSeconds(5000)
        val details = { i: Int ->
            PhysicalActivityEvent.SyncDetails(
                transactionHash = "0xload_${'$'}i",
                timestamp = detailsBase.plusSeconds(i.toLong()),
                network = "local",
                weekIndex = i % 52
            )
        }
        val sleepUpdates = sleepEvents.mapIndexed { i, e -> e.copy(syncDetails = details(i)) }
        val runUpdates = runningEvents.mapIndexed { i, e -> e.copy(syncDetails = details(i)) }
        val gymUpdates = gymEvents.mapIndexed { i, e -> e.copy(syncDetails = details(i)) }

        val updSleepStart = System.nanoTime(); repository.updateSleep(sleepUpdates); val updSleepMs = (System.nanoTime() - updSleepStart) / 1_000_000
        val updRunStart = System.nanoTime(); repository.updateRunning(runUpdates); val updRunMs = (System.nanoTime() - updRunStart) / 1_000_000
        val updGymStart = System.nanoTime(); repository.updateGymVisit(gymUpdates); val updGymMs = (System.nanoTime() - updGymStart) / 1_000_000
        val updMs = (System.nanoTime() - updStart) / 1_000_000

        val totalMs = (System.nanoTime() - totalStart) / 1_000_000

        Log.i(TAG, "Register: total=$regMs ms (sleep=$regSleepMs, run=$regRunMs, gym=$regGymMs)")
        Log.i(TAG, "Retrieve: total=$retMs ms (sleep=$retSleepMs, run=$retRunMs, gym=$retGymMs)")
        Log.i(TAG, "Update: total=$updMs ms (sleep=$updSleepMs, run=$updRunMs, gym=$updGymMs)")
        Log.i(TAG, "Overall time: $totalMs ms for 90x3 events")

        println("Register: total=$regMs ms (sleep=$regSleepMs, run=$regRunMs, gym=$regGymMs)")
        println("Retrieve: total=$retMs ms (sleep=$retSleepMs, run=$retRunMs, gym=$retGymMs)")
        println("Update: total=$updMs ms (sleep=$updSleepMs, run=$updRunMs, gym=$updGymMs)")
        println("Overall time: $totalMs ms for 90x3 events")
    }
}

