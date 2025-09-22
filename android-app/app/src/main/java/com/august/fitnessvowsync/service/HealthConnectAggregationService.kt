package com.august.fitnessvowsync.service

import android.util.Log
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.SleepSessionRecord
import com.august.fitnessvowsync.health.HealthConnectClient
import com.august.fitnessvowsync.health.totalDistance
import com.august.fitnessvowsync.helpers.TimeRange
import java.time.Instant
import javax.inject.Inject

class HealthConnectAggregationService @Inject constructor(private val client: HealthConnectClient) {
    companion object {
        // TODO: Change this (allow fake data)
        private const val HEALTHY_SLEEP_DURATION_MS = 0 //(6.5 * 60 * 60 * 1000).toLong() // 7,5 hours
        // TODO: Change this (allow fake data)
        private const val MERGE_SLEEP_SESSION_THRESHOLD_MS = 0 //40 * 60 * 1000
        private const val SLEEP_HEART_RATE_LOWER_BOUND = 55.0
        private const val SLEEP_HEART_RATE_UPPER_BOUND = 75.0
    }

    suspend fun longestDistanceRan(periodStart: Instant, periodEnd: Instant): Int {
        val runSessions = client.readRunningSessionRecords(TimeRange.between(periodStart, periodEnd))

        if (runSessions.isEmpty()) return 0

        var longestRunSession: ExerciseSessionRecord? = null

        for (session in runSessions) {
            val distanceRecord = client.readDistanceRecords(TimeRange.ofRecord(session)).firstOrNull()

            if (distanceRecord == null) continue

            session.totalDistance = distanceRecord.distance.inMeters

            if (longestRunSession == null || session.totalDistance > longestRunSession.totalDistance) {
                longestRunSession = session
            }
        }

        if (longestRunSession == null) return 0;

        Log.i("FitVow", "The longest running session by distance happened between ${longestRunSession.startTime} and ${longestRunSession.endTime} with ${longestRunSession.totalDistance}m")

        return longestRunSession.totalDistance.toInt()
    }

    suspend fun numberOfHealthyNightsOfSleep(periodStart: Instant, periodEnd: Instant): Int {
        val sleepSessions = client.readSleepSessionRecords(TimeRange.between(periodStart, periodEnd)).sortedBy { it.startTime }

        if (sleepSessions.isEmpty()) return 0

        val mergedSessions = mergeSleepSessions(sleepSessions)

        Log.i("FitVow", "Sleep Sessions: ${mergedSessions}")

        var healthyNightsCount = 0

        for ((startMs, endMs) in mergedSessions) {
            if ((endMs - startMs) < HEALTHY_SLEEP_DURATION_MS) continue

            val avgBpm = client.getAverageHeartRate(TimeRange.between(startMs, endMs))

            Log.i("FitVow", "Avg Bpm: ${avgBpm}")

            if (isHeartRateInHealthyRange(avgBpm)) healthyNightsCount++
        }

        Log.i("FitVow", "Number of healthy nights of sleep in the last 7 days: $healthyNightsCount")

        return healthyNightsCount
    }

    private fun mergeSleepSessions(sessions: List<SleepSessionRecord>): List<Pair<Long, Long>> {
        fun toEpochMillis(time: java.time.Instant) = time.toEpochMilli()

        val mergedSessions = mutableListOf<Pair<Long, Long>>()

        var currentStart = toEpochMillis(sessions[0].startTime)
        var currentEnd = toEpochMillis(sessions[0].endTime)

        for (i in 1 until sessions.size) {
            val thisStart = toEpochMillis(sessions[i].startTime)
            val thisEnd = toEpochMillis(sessions[i].endTime)

            val gap = thisStart - currentEnd
            if (gap <= MERGE_SLEEP_SESSION_THRESHOLD_MS) {
                if (thisEnd > currentEnd) currentEnd = thisEnd
            } else {
                mergedSessions.add(currentStart to currentEnd)
                currentStart = thisStart
                currentEnd = thisEnd
            }
        }

        mergedSessions.add(currentStart to currentEnd)

        return mergedSessions
    }

    private fun isHeartRateInHealthyRange(avgBpm: Long) = avgBpm.toDouble() in SLEEP_HEART_RATE_LOWER_BOUND..SLEEP_HEART_RATE_UPPER_BOUND
}