package com.august.fitnessvowsync.physicalactivity.collection

import com.august.fitnessvowsync.health.HealthConnectAggregator
import com.august.fitnessvowsync.physicalactivity.data.GymVisitTracker
import com.august.fitnessvowsync.physicalactivity.mapper.PhysicalActivityEventMapper
import com.august.fitnessvowsync.physicalactivity.model.PhysicalActivityEvents
import com.august.fitnessvowsync.physicalactivity.model.RunningEvent
import com.august.fitnessvowsync.physicalactivity.model.SleepEvent
import java.time.Instant
import javax.inject.Inject

class PhysicalActivityEventCollector @Inject constructor(
    private val healthConnectAggregator: HealthConnectAggregator,
    private val gymVisitTracker: GymVisitTracker,
    private val physicalActivityEventMapper: PhysicalActivityEventMapper,
) {

    suspend fun collect(periodStart: Instant, periodEnd: Instant): PhysicalActivityEvents {
        val sleepEvents = healthConnectAggregator.getSleepSessions(periodStart, periodEnd).map { SleepEvent(
            Instant.ofEpochMilli(it.startTime.toEpochMilli()),
            it.duration.toMinutes().toInt(),
            it.avgBpm,
            null,
        )}

        val runningEvents = healthConnectAggregator.getRunningSessions(periodStart, periodEnd).map {
            RunningEvent(
                Instant.ofEpochMilli(it.startTime.toEpochMilli()),
                it.distanceInMeters,
                it.pace.toSeconds().toInt(),
                it.avgBpm,
                null,
            )
        }

        val gymVisits = gymVisitTracker.getVisits(periodStart, periodEnd).map {
            val visit = it
            val trimmedAvgBpm = healthConnectAggregator.getTrimmedAverageHeartRate(visit.startTime, visit.endTime!!, 0.2)
            val avgBpm = trimmedAvgBpm ?: healthConnectAggregator.getAverageHeartRate(visit.startTime, visit.endTime)
            val maxBpm = healthConnectAggregator.getMaxHeartRate(visit.startTime, visit.endTime)

            physicalActivityEventMapper.toGymVisitEvent(visit, avgBpm, maxBpm)
        }

        return PhysicalActivityEvents(sleepEvents, runningEvents, gymVisits)
    }
}