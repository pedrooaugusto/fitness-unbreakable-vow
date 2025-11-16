package com.august.fitnessvowsync

import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.august.fitnessvowsync.contract.SignatureMapper
import com.august.fitnessvowsync.dagger.EncryptedSharedPreferencesModule
import com.august.fitnessvowsync.dagger.HealthConnectModule
import com.august.fitnessvowsync.health.HealthConnectAggregator
import com.august.fitnessvowsync.physicalactivity.collection.PhysicalActivityEventCollector
import com.august.fitnessvowsync.physicalactivity.data.GymVisitTracker
import com.august.fitnessvowsync.physicalactivity.mapper.PhysicalActivityEventMapper
import com.august.fitnessvowsync.physicalactivity.model.GymVisitEvent
import com.august.fitnessvowsync.physicalactivity.model.PhysicalActivityEvents
import com.august.fitnessvowsync.physicalactivity.model.TrackedGymConfig
import com.august.fitnessvowsync.testing.HealthConnectTestHelper
import com.august.fitnessvowsync.testing.PeriodSpec
import com.august.fitnessvowsync.testing.RandomPhysicalActivityGenerator
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Duration
import java.time.Instant

@RunWith(AndroidJUnit4::class)
class PhysicalActivityEventCollectorTest {
    private lateinit var healthConnectTestHelper: HealthConnectTestHelper
    private lateinit var collector: PhysicalActivityEventCollector
    private lateinit var randomEvents: List<PeriodSpec>
    private var trackedGyms: List<TrackedGymConfig> = listOf(
            TrackedGymConfig("main", -22.5466, 44.54546, Duration.ofMinutes(10), 150.0),
            TrackedGymConfig("support", -22.2466, 44.14546, Duration.ofMinutes(10), 400.0),
    )

    companion object {
        @BeforeClass
        @JvmStatic
        fun setPermissions() {
            HealthConnectTestHelper.openHealthConnectAndWait()
        }
    }

    @Test
    fun collect_handles_multiple_periods_correctly() = runBlocking {
        for (randomEvent in randomEvents) {
            // Arrange
            randomEvent.runningSessions.forEach { healthConnectTestHelper.insertRunningSession(it.start, it.end, it.distanceMeters.toDouble(), it.avgBpm.toLong()) }
            randomEvent.sleepSessions.forEach { healthConnectTestHelper.insertSleepSession(it.start, it.end, it.avgBpm.toLong()) }
            randomEvent.gymVisits.forEach { healthConnectTestHelper.insertGymVisit(it.start, it.end, it.gym, it.avgBpm.toLong()) }

            // Act
            val events = collector.collect(randomEvent.start, randomEvent.end)

            // Assert
            assertRunningEvents(randomEvent, events)
            assertSleepEvents(randomEvent, events)
            assertGymVisitEvents(randomEvent, events)
        }
    }

    private fun assertGymVisitEvents(
        randomEvent: PeriodSpec,
        events: PhysicalActivityEvents
    ) {
        println("Asserting ${randomEvent.gymVisits} on ${events.gymVisits}")
        assertEquals(randomEvent.gymVisits.size, events.gymVisits.size)
        for (g in randomEvent.gymVisits) {
            val e = events.gymVisits.firstOrNull { it.timestamp == g.start }
            assertNotNull("Missing gym visit $g on list ${events.gymVisits}", e)
            val expectedMinutes = Duration.between(g.start, g.end).toMinutes().toInt()
            val location = GymVisitEvent.Location(g.gym.latitude, g.gym.longitude)
            assertEquals(expectedMinutes, e!!.durationInMinutes)
            assertEquals(g.avgBpm, e.avgBpm)
            assertEquals(g.maxBpm, e.maxBpm)
            assertEquals(location.latitudeNanoDegree, e.location.latitudeNanoDegree)
            assertEquals(location.longitudeNanoDegree, e.location.longitudeNanoDegree)
        }
    }

    private fun assertSleepEvents(
        randomEvent: PeriodSpec,
        events: PhysicalActivityEvents
    ) {
        println("Asserting ${randomEvent.sleepSessions} on ${events.sleep}")
        assertEquals(randomEvent.sleepSessions.size, events.sleep.size)
        if (randomEvent.sleepSessions.isNotEmpty()) {
            val s = randomEvent.sleepSessions.first()
            val e = events.sleep.firstOrNull { it.timestamp == s.start }
            assertNotNull("Missing sleep event $s on list ${events.sleep}", e)
            val expectedMinutes = Duration.between(s.start, s.end).toMinutes().toInt()
            assertEquals(expectedMinutes, e!!.durationInMinutes)
            assertEquals(s.avgBpm, e.avgBpm)
        }
    }

    private fun assertRunningEvents(
        randomEvent: PeriodSpec,
        events: PhysicalActivityEvents
    ) {
        println("Asserting ${randomEvent.runningSessions} on ${events.running}")
        assertEquals(randomEvent.runningSessions.size, events.running.size)
        for (run in randomEvent.runningSessions) {
            val e = events.running.firstOrNull { it.timestamp == run.start }
            assertNotNull("Missing running event $run on list ${events.running}", e)
            assertEquals(run.distanceMeters, e!!.distanceInMeters)
            assertEquals(run.paceInSecondsPerKm, e.paceInSecondsPerKm)
            assertEquals(run.avgBpm, e.avgBpm)
        }
    }

    @Before
    public fun init() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val healthConnectClient = HealthConnectModule().provideHealthConnectClient(context)
        val gymVisitTracker = GymVisitTracker(EncryptedSharedPreferencesModule().let { it.provideEncryptedSharedPreferences(context, it.provideMainKeyAlias()) })

        randomEvents = RandomPhysicalActivityGenerator.generate(Instant.ofEpochMilli(Instant.now().toEpochMilli()).minusSeconds(60 * 60), count = 4, trackedGyms, seed = 42L)
        healthConnectTestHelper = HealthConnectTestHelper(healthConnectClient, gymVisitTracker)
        collector = PhysicalActivityEventCollector(
            HealthConnectAggregator(com.august.fitnessvowsync.health.HealthConnectClient(healthConnectClient), Duration.ZERO),
            gymVisitTracker,
            PhysicalActivityEventMapper(SignatureMapper())
        )
    }

    @After
    public fun cleanup() {
        val globalStart = randomEvents.minOf { it.start }
        val globalEnd = randomEvents.maxOf { it.end }

        Log.i("FitVow - Sync", "Cleaning resources!")

        runBlocking {
            healthConnectTestHelper.clearRecordsInRange(globalStart, globalEnd)
        }
    }
}
