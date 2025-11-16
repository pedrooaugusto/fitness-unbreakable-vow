package com.august.fitnessvowsync.health

import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.SleepStageRecord
import com.august.fitnessvowsync.helpers.TimeRange
import java.time.Duration
import java.time.Instant
import javax.inject.Inject
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.roundToLong

class HealthConnectAggregator @Inject constructor(private val client: HealthConnectClient) {
    // Fix cases where you get up to pee and then goes back to sleep ~20min later.
    // When that happens your SmartWatch might count 2 separate sleep sessions.
    private var MERGE_SLEEP_SESSION_THRESHOLD = Duration.ofMinutes(70)

    // Secondary constructor for tests to override merge threshold
    constructor(client: HealthConnectClient, mergeThreshold: Duration): this(client) {
        this.MERGE_SLEEP_SESSION_THRESHOLD = mergeThreshold
    }

    data class RunningSession(
        val startTime: Instant,
        val endTime: Instant,
        val duration: Duration,
        val pace: Duration,
        val distanceInMeters: Int,
        val avgBpm: Int,
        val maxBpm: Int,
    )

    data class SleepSession(
        val startTime: Instant,
        val endTime: Instant,
        val duration: Duration,
        val avgBpm: Int,
    )

    suspend fun getRunningSessions(periodStart: Instant, periodEnd: Instant): List<RunningSession> {
        val exerciseSessions = client.readRunningSessionRecords(TimeRange.between(periodStart, periodEnd))
        val runningSessions = mutableListOf<RunningSession>()

        for (runSession in exerciseSessions) {
            val timeRangeFilter = TimeRange.between(runSession.startTime, runSession.endTime)
            val metrics = setOf(DistanceRecord.DISTANCE_TOTAL, ExerciseSessionRecord.EXERCISE_DURATION_TOTAL, HeartRateRecord.BPM_AVG, HeartRateRecord.BPM_MAX)
            val result = client.aggregate(timeRangeFilter, metrics)

            val duration = result[ExerciseSessionRecord.EXERCISE_DURATION_TOTAL] ?: Duration.ofSeconds(0)
            val distanceInMeters = max((result[DistanceRecord.DISTANCE_TOTAL]?.inMeters ?: 0).toInt(), 1) // kill me baby
            val avgBpm = (result[HeartRateRecord.BPM_AVG] ?: 0).toInt()
            val maxBpm = (result[HeartRateRecord.BPM_MAX] ?: 0).toInt()
            val distanceInKm = distanceInMeters / 1000.0
            val pace = Duration.ofSeconds((duration.toSeconds() / distanceInKm).roundToLong())

            runningSessions.add(RunningSession(
                runSession.startTime,
                runSession.endTime,
                duration,
                pace,
                distanceInMeters,
                avgBpm,
                maxBpm
            ))
        }

        return runningSessions
    }

    suspend fun getSleepSessions(periodStart: Instant, periodEnd: Instant): List<SleepSession> {
        val sleepRecords = mergeSleepSessions(client.readSleepSessionRecords(TimeRange.between(periodStart, periodEnd)))
        val sleepSessions = mutableListOf<SleepSession>()

        for ((startTime, endTime) in sleepRecords) {
            val timeRangeFilter = TimeRange.between(startTime, endTime)
            val metrics = setOf(SleepSessionRecord.SLEEP_DURATION_TOTAL, HeartRateRecord.BPM_AVG)

            val result = client.aggregate(timeRangeFilter, metrics)

            // SLEEP_DURATION_TOTAL:
            //      Total time spent not in the `SleepStageRecord.STAGE_TYPE_AWAKE`.
            //      Sometimes the watch might interpret a sudden hand movement during sleep
            //      as "awake". For this reason this value is not the best metric for total
            //      sleep duration.
            //      On the other hand it is a good indicator that a sleep session actually
            //      occurred. A valid sleep session must have `SLEEP_DURATION_TOTAL`.
            if (result[SleepSessionRecord.SLEEP_DURATION_TOTAL] == null) continue

            val duration = Duration.between(startTime, endTime)
            val avgBpmDuringSleep = getAverageHeartRateDuringSleep(startTime, endTime)
            val avgBpm = avgBpmDuringSleep ?: (result[HeartRateRecord.BPM_AVG] ?: 0).toInt()

            sleepSessions.add(SleepSession(
                startTime,
                endTime,
                duration,
                avgBpm,
            ))
        }

        return sleepSessions
    }

    suspend fun getAverageHeartRate(periodStart: Instant, periodEnd: Instant): Int {
        val timeRangeFilter = TimeRange.between(periodStart, periodEnd)
        val metrics = setOf(HeartRateRecord.BPM_AVG)

        return client.aggregate(timeRangeFilter, metrics)[HeartRateRecord.BPM_AVG]?.toInt() ?: 0
    }

    suspend fun getMaxHeartRate(periodStart: Instant, periodEnd: Instant): Int {
        val timeRangeFilter = TimeRange.between(periodStart, periodEnd)
        val metrics = setOf(HeartRateRecord.BPM_MAX)

        return client.aggregate(timeRangeFilter, metrics)[HeartRateRecord.BPM_MAX]?.toInt() ?: 0
    }

    suspend fun getAverageHeartRateDuringSleep(periodStart: Instant, periodEnd: Instant): Int? {
        val timeRangeFilter = TimeRange.between(periodStart, periodEnd)
        val stages = client.readSleepStageRecords(timeRangeFilter).filter { it.stage != SleepStageRecord.STAGE_TYPE_AWAKE }

        if (stages.isEmpty()) return null

        var sleepBpmWeightedSum = 0.0
        var totalSleepDurationInMs = 0L

        for (stage in stages) {
            val start = maxOf(stage.startTime, periodStart)
            val end = minOf(stage.endTime, periodEnd)

            if (!start.isBefore(end)) continue

            val result = client.aggregate(TimeRange.between(start, end),setOf(HeartRateRecord.BPM_AVG))
            val avgBpmInSleepStage = result[HeartRateRecord.BPM_AVG] ?: continue
            val sleepStageDurationInMs = Duration.between(start, end).toMillis()

            sleepBpmWeightedSum += avgBpmInSleepStage * sleepStageDurationInMs
            totalSleepDurationInMs += sleepStageDurationInMs
        }

        return if (totalSleepDurationInMs > 0) (sleepBpmWeightedSum / totalSleepDurationInMs).roundToInt() else null
    }

    /**
     * Returns the duration-weighted average heart rate, excluding the lowest
     * [trimLowerFraction] portion of TIME (not sample count). Example: 0.20 = drop lowest 20% time.
     */
    suspend fun getTrimmedAverageHeartRate(start: Instant, end: Instant, trimLowerFraction: Double = 0.20): Int? {
        require(trimLowerFraction in 0.0..0.9) { "trimLowerFraction must be between 0.0 and 0.9" }

        // 1. Load raw heart rate samples in the time window
        val records = client.readRecords(HeartRateRecord::class, TimeRange.between(start, end))
        val samples = records.flatMap { it.samples }
            .filter { !it.time.isBefore(start) && it.time.isBefore(end) }
            .sortedBy { it.time }

        if (samples.isEmpty()) return null

        // 2. Convert discrete samples into continuous segments [time(i) -> time(i+1)) with a stable bpm
        //    Add a virtual closing sample at 'end' to close the last segment.
        data class Segment(val bpm: Int, val durationMs: Long)

        val last = samples.last()
        val closedSamples = if (last.time < end) samples + HeartRateRecord.Sample(end, last.beatsPerMinute) else samples

        val segments = closedSamples.zipWithNext { element1, element2 ->
            val time1 = element1.time
            val bpm1 = element1.beatsPerMinute.toInt()
            val time2 = element2.time

            val segStart = if (time1 < start) start else time1
            val segEnd = if (time2 > end) end else time2

            if (segStart.isBefore(segEnd)) {
                Segment(bpm = bpm1, durationMs = Duration.between(segStart, segEnd).toMillis())
            } else {
                null
            }
        }.filterNotNull().sortedBy { it.bpm }

        if (segments.isEmpty()) return null

        val totalDurationMs = segments.sumOf { it.durationMs }

        if (totalDurationMs == 0L) return null

        // 3. Sort segments by bpm (ascending) and remove the LOWEST X% of time
        val trimTargetMs = (totalDurationMs * trimLowerFraction).toLong()
        var remainingTrimMs = trimTargetMs
        val keptSegments = mutableListOf<Segment>()

        for (segment in segments) {
            if (remainingTrimMs <= 0) {
                keptSegments += segment
                continue
            }
            if (segment.durationMs <= remainingTrimMs) {
                remainingTrimMs -= segment.durationMs
            } else {
                val keptMs = segment.durationMs - remainingTrimMs
                keptSegments += segment.copy(durationMs = keptMs)
                remainingTrimMs = 0
            }
        }

        val keptTotalMs = keptSegments.sumOf { it.durationMs }
        if (keptTotalMs == 0L) return null

        // 4. Time-weighted average of remaining (top 80%) segments
        val weightedSum = keptSegments.sumOf { it.bpm.toLong() * it.durationMs }

        return (weightedSum.toDouble() / keptTotalMs).roundToInt()
    }

    private fun mergeSleepSessions(sessions: List<SleepSessionRecord>): List<Pair<Instant, Instant>> {
        if (sessions.isEmpty()) return emptyList()

        val mergedSessions = mutableListOf<Pair<Instant, Instant>>()
        val sortedSessions = sessions.sortedBy { it.startTime }

        var currentStart = sortedSessions.first().startTime
        var currentEnd = sortedSessions.first().endTime

        for (i in 1 until sortedSessions.size) {
            val thisStart = sortedSessions[i].startTime
            val thisEnd = sortedSessions[i].endTime

            val gap = Duration.ofMillis(thisStart.toEpochMilli() - currentEnd.toEpochMilli())

            if (gap > MERGE_SLEEP_SESSION_THRESHOLD) {
                mergedSessions.add(currentStart to currentEnd)
                currentStart = thisStart
                currentEnd = thisEnd
            } else if (thisEnd > currentEnd) {
                currentEnd = thisEnd
            }
        }

        mergedSessions.add(currentStart to currentEnd)

        return mergedSessions
    }
}
