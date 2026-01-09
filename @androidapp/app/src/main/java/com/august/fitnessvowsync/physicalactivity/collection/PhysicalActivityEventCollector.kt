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
        val sleepEvents = healthConnectAggregator.getSleepSessions(periodStart, periodEnd).map {
            SleepEvent(
                Instant.ofEpochMilli(it.startTime.toEpochMilli()),
                it.duration.toMinutes().toInt(),
                it.avgBpm,
                null,
            )
        }

        val gymVisitSessions = gymVisitTracker.getVisits(periodStart, periodEnd)
        val gymVisits = gymVisitSessions.map {
            val visit = it
            val trimmedAvgBpm = healthConnectAggregator.getTrimmedAverageHeartRate(visit.startTime, visit.endTime!!, 0.2)
            val avgBpm = trimmedAvgBpm ?: healthConnectAggregator.getAverageHeartRate(visit.startTime, visit.endTime)
            val maxBpm = healthConnectAggregator.getMaxHeartRate(visit.startTime, visit.endTime)

            physicalActivityEventMapper.toGymVisitEvent(visit, avgBpm, maxBpm)
        }

        // For some reason Samsung Health does not store the running routes on health connect
        // so we have to exclude potential threadmills runs in the gym manually.
        // Running in a threadmill inside the gym would count toward two goals:
        // 1) gym visit
        // 2) outdoor running
        // This filter makes it so it only counts towards gym visits.
        val outdoorRunningEvents = healthConnectAggregator.getRunningSessions(periodStart, periodEnd)
            .filter { runningSession ->
                val runStart = runningSession.startTime
                val runEnd = runningSession.endTime

                gymVisitSessions.none { visit ->
                    val visitStart = visit.startTime
                    val visitEnd = visit.endTime ?: Instant.MAX

                    // Overlap check: run interval intersects an active gym visit interval.
                    !runEnd.isBefore(visitStart) && !runStart.isAfter(visitEnd)
                }
            }
            .map {
                RunningEvent(
                    Instant.ofEpochMilli(it.startTime.toEpochMilli()),
                    it.distanceInMeters,
                    it.pace.toSeconds().toInt(),
                    it.avgBpm,
                    null,
                )
            }

        return PhysicalActivityEvents(sleepEvents, outdoorRunningEvents, gymVisits)
    }
}
