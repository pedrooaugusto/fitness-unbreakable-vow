package com.august.fitnessvowsync

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.august.fitnessvowsync.dagger.HealthConnectModule
import com.august.fitnessvowsync.health.HealthConnectAggregator
import com.august.fitnessvowsync.testing.HealthConnectTestHelper
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Duration
import java.time.Instant

@RunWith(AndroidJUnit4::class)
class HealthConnectAggregatorTest {
    private lateinit var helper: HealthConnectTestHelper
    private lateinit var periodStart: Instant
    private lateinit var periodEnd: Instant

    companion object {
        @BeforeClass
        @JvmStatic
        fun setPermissions() {
            HealthConnectTestHelper.openHealthConnectAndWait()
        }
    }

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val hcClient = HealthConnectModule().provideHealthConnectClient(context)
        helper = HealthConnectTestHelper(
            hcClient,
            com.august.fitnessvowsync.physicalactivity.data.GymVisitTracker(
                com.august.fitnessvowsync.dagger.EncryptedSharedPreferencesModule().let { it.provideEncryptedSharedPreferences(context, it.provideMainKeyAlias()) }
            )
        )

        val base = Instant.ofEpochMilli(Instant.now().toEpochMilli()).minusSeconds(3600)
        periodStart = base
        periodEnd = base.plus(Duration.ofHours(5))
    }

    @After
    fun tearDown() = runBlocking {
        helper.clearRecordsInRange(periodStart, periodEnd)
    }

    /*fun testRoute() = runBlocking {
        val hcClient = HealthConnectModule().provideHealthConnectClient(InstrumentationRegistry.getInstrumentation().targetContext)
        val client = com.august.fitnessvowsync.health.HealthConnectClient(hcClient)

        val r = client.readRecords(ExerciseSessionRecord::class, TimeRange.last(Period.ofDays(90)))

        println("Size: ${r.size}")

        for (record in r) {
            val origin = record.metadata.dataOrigin.packageName
            println(
                """
                ----
                Origin: $origin
                Type: ${record.exerciseType}
                Start: ${record.startTime}
                End:   ${record.endTime}
                hasRoute=${record.hasRoute}
                segments=${record.segments.size}
                laps=${record.laps.size}
                route=${record.route}
                notes=${record.notes}
                """.trimIndent()
            )
        }
    }*/

    @Test
    fun mergeSleepSessions_merges_within_threshold() = runBlocking {
        val hcClient = HealthConnectModule().provideHealthConnectClient(InstrumentationRegistry.getInstrumentation().targetContext)
        val agg = HealthConnectAggregator(com.august.fitnessvowsync.health.HealthConnectClient(hcClient), Duration.ofMinutes(40))

        val s0 = periodStart.plus(Duration.ofMinutes(10))
        val e0 = s0.plus(Duration.ofMinutes(30))
        val s1 = e0.plus(Duration.ofMinutes(15)) // gap 15 < 40 => merge with [s0,e0]
        val e1 = s1.plus(Duration.ofMinutes(30))
        val s2 = e1.plus(Duration.ofMinutes(50)) // gap 50 > 40 => new cluster
        val e2 = s2.plus(Duration.ofMinutes(30))

        helper.insertSleepSession(s0, e0, 50)
        helper.insertSleepSession(s1, e1, 50)
        helper.insertSleepSession(s2, e2, 50)

        val sessions = agg.getSleepSessions(periodStart, periodEnd)

        assertEquals(2, sessions.size)
        val first = sessions.minByOrNull { it.startTime }!!
        val second = sessions.maxByOrNull { it.startTime }!!
        assertEquals(s0, first.startTime)
        assertEquals(e1, first.endTime)
        assertEquals(s2, second.startTime)
        assertEquals(e2, second.endTime)
    }

    @Test
    fun getSleepSessions_uses_avgBpm_only_for_sleeping_stages() = runBlocking {
        val hcClient = HealthConnectModule().provideHealthConnectClient(InstrumentationRegistry.getInstrumentation().targetContext)
        val agg = HealthConnectAggregator(com.august.fitnessvowsync.health.HealthConnectClient(hcClient), Duration.ZERO)

        val s0 = periodStart.plus(Duration.ofMinutes(20))
        val e0 = s0.plus(Duration.ofMinutes(30))
        helper.insertSleepSession(s0, e0, 0)

        val st0s = s0
        val st0e = st0s.plus(Duration.ofMinutes(10))
        val st1s = st0e
        val st1e = st1s.plus(Duration.ofMinutes(10))
        val st2s = st1e
        val st2e = e0

        helper.insertSleepStages(
            Triple(st0s, st0e, androidx.health.connect.client.records.SleepStageRecord.STAGE_TYPE_LIGHT),
            Triple(st1s, st1e, androidx.health.connect.client.records.SleepStageRecord.STAGE_TYPE_AWAKE),
            Triple(st2s, st2e, androidx.health.connect.client.records.SleepStageRecord.STAGE_TYPE_DEEP),
        )

        helper.insertConstantHeartRate(st0s, st0e, 60)
        helper.insertConstantHeartRate(st1s, st1e, 200)
        helper.insertConstantHeartRate(st2s, st2e, 40)

        val sessions = agg.getSleepSessions(periodStart, periodEnd)
        assertEquals(1, sessions.size)

        val expectedAvg = 50 // (60*10 + 40*10) / 20
        assertEquals(expectedAvg, sessions[0].avgBpm)
    }

    @Test
    fun getTrimmedAverageHeartRate_trims_lower_fraction() = runBlocking {
        val hcClient = HealthConnectModule().provideHealthConnectClient(InstrumentationRegistry.getInstrumentation().targetContext)
        val agg = HealthConnectAggregator(com.august.fitnessvowsync.health.HealthConnectClient(hcClient))

        val s = periodStart.plus(Duration.ofMinutes(5))
        val e = s.plus(Duration.ofMinutes(50))

        val lowEnd = s.plus(Duration.ofMinutes(10)) // 20% of time window
        helper.insertConstantHeartRate(s, lowEnd, 80)
        helper.insertConstantHeartRate(lowEnd, e, 120)

        val trimmed = agg.getTrimmedAverageHeartRate(s, e, trimLowerFraction = 0.2)
        assertEquals(120, trimmed)
    }
}
