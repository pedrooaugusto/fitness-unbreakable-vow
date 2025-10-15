package com.august.fitnessvowsync.donotuse

import android.util.Log
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.units.Length
import com.august.fitnessvowsync.service.GymVisitService
import java.time.Duration
import java.time.Instant
import javax.inject.Inject
import kotlin.time.Duration.Companion.minutes

/**
 * All the methods in this file are used to test the application
 * without having real data.
 *
 * This should not be used in production.
 */

class FakeDataProducerDoNotUse @Inject constructor(
    private val healthConnectClient: HealthConnectClient,
    private val gymVisitService: GymVisitService,
) {
    suspend fun addFakeRunningSession(distance: Long) {
        val startTime = Instant.now()
        val endTime = Instant.now().plus(Duration.ofSeconds(5))

        val runningSessionRecord: Record = ExerciseSessionRecord(
            startTime = startTime,
            endTime = endTime,
            exerciseType = ExerciseSessionRecord.EXERCISE_TYPE_RUNNING,
            startZoneOffset = null,
            endZoneOffset = null,
        )

        val distanceRecord: Record = DistanceRecord(
            startTime = startTime.plus(Duration.ofSeconds(2)),
            endTime = endTime,
            distance = Length.meters(distance.toDouble()),
            startZoneOffset = null,
            endZoneOffset = null,
        )

        Log.i("FitVow", "Adding fake running session with session: $runningSessionRecord")
        Log.i("FitVow", "Adding fake running session with distance: $distanceRecord")

        healthConnectClient.insertRecords(listOf(runningSessionRecord, distanceRecord))
    }

    suspend fun addFakeSleepSession(avgHeartRate: Long) {
        val startTime = Instant.now()
        val endTime = Instant.now().plus(Duration.ofSeconds(5))

        val sleepSessionRecord: Record = SleepSessionRecord(
            startTime = startTime,
            endTime = endTime,
            startZoneOffset = null,
            endZoneOffset = null,
        )

        val heartRateRecord: Record = HeartRateRecord(
            startTime = startTime,
            endTime = endTime,
            samples = listOf(
                HeartRateRecord.Sample(startTime.plus(Duration.ofSeconds(2)), avgHeartRate),
                HeartRateRecord.Sample(startTime.plus(Duration.ofSeconds(3)), avgHeartRate)
            ),
            startZoneOffset = null,
            endZoneOffset = null,
        )

        Log.i("FitVow", "Adding fake sleep session with session: $sleepSessionRecord")
        Log.i("FitVow", "Adding fake sleep session with heart rate: $heartRateRecord")

        healthConnectClient.insertRecords(listOf(sleepSessionRecord, heartRateRecord))
    }

    suspend fun addFakeGymVisit() {
        val startTime = Instant.now()
        val visitRecord = GymVisitService.GymVisitRecord(startTime.plus(Duration.ofSeconds(2)), 20.minutes)

        gymVisitService.addGymVisit(visitRecord)

        Log.i("FitVow", "Adding fake gym visit: $visitRecord")
    }
}