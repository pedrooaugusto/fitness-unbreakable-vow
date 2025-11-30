package com.august.fitnessvowsync

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
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant

@RunWith(AndroidJUnit4::class)
class PhysicalActivityEventRepositoryTest {
    private lateinit var repository: PhysicalActivityEventRepository

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val encryptedPreferences = EncryptedSharedPreferencesModule().let { it.provideEncryptedSharedPreferences(context, it.provideMainKeyAlias()) }
        encryptedPreferences.edit().clear().apply()
        repository = PhysicalActivityEventRepository(encryptedPreferences)
    }

    @After
    fun tearDown() {
        // Clear stored state to keep tests isolated
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val encryptedPreferences = EncryptedSharedPreferencesModule().let { it.provideEncryptedSharedPreferences(context, it.provideMainKeyAlias()) }
        encryptedPreferences.edit().clear().apply()
    }

    @Test
    fun register_and_retrieve_events() {
        val now = Instant.ofEpochSecond(Instant.now().epochSecond) // second precision for deterministic equality

        val sleep = SleepEvent(now, durationInMinutes = 90, avgBpm = 55, syncDetails = null)
        val run = RunningEvent(now.plusSeconds(60), distanceInMeters = 3000, paceInSecondsPerKm = 300, avgBpm = 120, syncDetails = null)
        val gym = GymVisitEvent(GymVisitEvent.Location(0.0, 0.0), now.plusSeconds(120), durationInMinutes = 45, avgBpm = 110, maxBpm = 150, "GYM#1", syncDetails = null)

        repository.registerSleep(listOf(sleep))
        repository.registerRunning(listOf(run))
        repository.registerGymVisit(listOf(gym))

        val sleeps = repository.getSleepSessions()
        val runs = repository.getRunningSessions()
        val gyms = repository.getGymVisitSessions()

        assertEquals(1, sleeps.size)
        assertEquals(1, runs.size)
        assertEquals(1, gyms.size)

        assertEquals(null, sleeps[0].syncDetails)
        assertEquals(null, runs[0].syncDetails)
        assertEquals(null, gyms[0].syncDetails)

        // Object equality checks
        assertEquals(sleep, sleeps[0])
        assertEquals(run, runs[0])
        assertEquals(gym, gyms[0])
    }

    @Test
    fun duplicates_are_skipped() {
        val t = Instant.ofEpochSecond(Instant.now().epochSecond)

        val sleep = SleepEvent(t, 60, 50, null)
        val run = RunningEvent(t.plusSeconds(1), 2500, 320, 118, null)
        val gym = GymVisitEvent(GymVisitEvent.Location(1.0, 1.0), t.plusSeconds(2), 30, 115, 140, "GYM#0",null)

        repository.registerSleep(listOf(sleep))
        repository.registerSleep(listOf(sleep)) // duplicate
        repository.registerRunning(listOf(run, run)) // duplicate in same call
        repository.registerGymVisit(listOf(gym))
        repository.registerGymVisit(listOf(gym)) // duplicate

        assertEquals(1, repository.getSleepSessions().size)
        assertEquals(1, repository.getRunningSessions().size)
        assertEquals(1, repository.getGymVisitSessions().size)
    }

    @Test
    fun update_sync_details_from_null_to_non_null() {
        val t = Instant.ofEpochSecond(Instant.now().epochSecond)

        val sleep = SleepEvent(t, 50, 52, null)
        val run = RunningEvent(t.plusSeconds(1), 2200, 295, 119, null)
        val gym = GymVisitEvent(GymVisitEvent.Location(2.0, 2.0), t.plusSeconds(2), 40, 112, 155,"GYM#1", null)

        repository.registerSleep(listOf(sleep))
        repository.registerRunning(listOf(run))
        repository.registerGymVisit(listOf(gym))

        val details = PhysicalActivityEvent.SyncDetails(
            transactionHash = "0xabc123",
            timestamp = t.plusSeconds(10),
            network = "sepolia",
            weekIndex = 7,
        )

        repository.updateSleep(listOf(sleep.copy(syncDetails = details)))
        repository.updateRunning(listOf(run.copy(syncDetails = details)))
        repository.updateGymVisit(listOf(gym.copy(syncDetails = details)))

        val sleepUpdated = repository.getSleepSessions().firstOrNull { it.timestamp == t }
        val runUpdated = repository.getRunningSessions().firstOrNull { it.timestamp == t.plusSeconds(1) }
        val gymUpdated = repository.getGymVisitSessions().firstOrNull { it.timestamp == t.plusSeconds(2) }

        assertNotNull(sleepUpdated)
        assertNotNull(runUpdated)
        assertNotNull(gymUpdated)

        assertEquals(details, sleepUpdated!!.syncDetails)
        assertEquals(details, runUpdated!!.syncDetails)
        assertEquals(details, gymUpdated!!.syncDetails)
    }
}
