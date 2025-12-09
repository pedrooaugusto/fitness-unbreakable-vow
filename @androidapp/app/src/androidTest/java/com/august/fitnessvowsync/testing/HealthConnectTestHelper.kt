package com.august.fitnessvowsync.testing

import android.content.Intent
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.SleepStageRecord
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.health.connect.client.units.Length
import androidx.test.platform.app.InstrumentationRegistry
import com.august.fitnessvowsync.physicalactivity.data.GymVisitTracker
import com.august.fitnessvowsync.physicalactivity.model.TrackedGymConfig
import java.time.Duration
import java.time.Instant

class HealthConnectTestHelper(
    private val client: HealthConnectClient,
    private val gymVisitTracker: GymVisitTracker,
) {

    suspend fun insertGymVisit(start: Instant, end: Instant, gym: TrackedGymConfig, avgBpm: Long) {
        insertConstantHeartRate(start, end, avgBpm)

        gymVisitTracker.startVisit(start, gym)
        gymVisitTracker.finishVisit(end)
    }

    suspend fun insertRunningSession(start: Instant, end: Instant, distanceMeters: Double, avgBpm: Long) {
        val session: Record = ExerciseSessionRecord(
            startTime = start,
            endTime = end,
            exerciseType = ExerciseSessionRecord.EXERCISE_TYPE_RUNNING,
            startZoneOffset = null,
            endZoneOffset = null,
        )

        val distance: Record = DistanceRecord(
            startTime = start,
            endTime = end,
            distance = Length.meters(distanceMeters),
            startZoneOffset = null,
            endZoneOffset = null,
        )

        val hr: Record = HeartRateRecord(
            startTime = start,
            endTime = end,
            samples = listOf(
                HeartRateRecord.Sample(start.plus(Duration.ofSeconds(5)), avgBpm),
                HeartRateRecord.Sample(end.minus(Duration.ofSeconds(5)), avgBpm)
            ),
            startZoneOffset = null,
            endZoneOffset = null,
        )

        client.insertRecords(listOf(session, distance, hr))
    }

    suspend fun insertSleepSession(start: Instant, end: Instant, avgBpm: Long) {
        val sleep: Record = SleepSessionRecord(
            startTime = start,
            endTime = end,
            startZoneOffset = null,
            endZoneOffset = null,
        )

        if (avgBpm == 0L) {
            client.insertRecords(listOf(sleep))

            return
        }

        // Two samples with same bpm => aggregate avg equals bpm
        val hr: Record = HeartRateRecord(
            startTime = start,
            endTime = end,
            samples = listOf(
                HeartRateRecord.Sample(start.plus(Duration.ofSeconds(5)), avgBpm),
                HeartRateRecord.Sample(end.minus(Duration.ofSeconds(5)), avgBpm)
            ),
            startZoneOffset = null,
            endZoneOffset = null,
        )

        client.insertRecords(listOf(sleep, hr))
    }

    suspend fun insertSleepStages(vararg stages: Triple<Instant, Instant, Int>) {
        val records = stages.map { (s, e, stageType) ->
            SleepStageRecord(
                startTime = s,
                endTime = e,
                stage = stageType,
                startZoneOffset = null,
                endZoneOffset = null,
            ) as Record
        }
        if (records.isNotEmpty()) {
            client.insertRecords(records)
        }
    }

    suspend fun insertConstantHeartRate(start: Instant, end: Instant, bpm: Long, sampleIntervalSeconds: Long = 10) {
        val samples = mutableListOf<HeartRateRecord.Sample>()
        var t = start

        // Emit samples on [start, end) to avoid duplicate boundary samples
        while (t.isBefore(end)) {
            samples += HeartRateRecord.Sample(t, bpm)
            t = t.plusSeconds(sampleIntervalSeconds)
        }
        
        if (samples.isEmpty()) return

        val hr: Record = HeartRateRecord(
            startTime = start,
            endTime = end,
            samples = samples,
            startZoneOffset = null,
            endZoneOffset = null,
        )

        client.insertRecords(listOf(hr))
    }

    suspend fun clearRecordsInRange(start: Instant, end: Instant) {
        val range = TimeRangeFilter.between(start, end)

        client.deleteRecords(recordType = ExerciseSessionRecord::class, timeRangeFilter = range)
        client.deleteRecords(recordType = DistanceRecord::class, timeRangeFilter = range)
        client.deleteRecords(recordType = SleepSessionRecord::class, timeRangeFilter = range)
        client.deleteRecords(recordType = SleepStageRecord::class, timeRangeFilter = range)
        client.deleteRecords(recordType = HeartRateRecord::class, timeRangeFilter = range)

        gymVisitTracker.removeVisits(start, end)
        gymVisitTracker.clearCurrentVisit()
    }

    companion object {
        private var permissionGranted = false
        @JvmStatic
        fun openHealthConnectAndWait() {
            if (permissionGranted) return

            val inst = InstrumentationRegistry.getInstrumentation()
            val ctx = inst.targetContext
            val intent = ctx.packageManager
                .getLaunchIntentForPackage("com.google.android.apps.healthdata")
                ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

            if (intent != null) {
                ctx.startActivity(intent)
                android.os.SystemClock.sleep(10_000)
                permissionGranted = true
            }
        }
    }
}
