package com.august.fitnessvowsync.physicalactivity.collection

import android.util.Log
import com.august.fitnessvowsync.BuildConfig
import com.august.fitnessvowsync.contract.PhysicalActivityOracleService
import com.august.fitnessvowsync.physicalactivity.data.PhysicalActivityEventRepository
import com.august.fitnessvowsync.physicalactivity.model.PhysicalActivityEvent
import java.time.Instant
import javax.inject.Inject

class PhysicalActivityEventPublisher @Inject constructor(
    private val repository: PhysicalActivityEventRepository,
    private val collector: PhysicalActivityEventCollector,
    private val oracle: PhysicalActivityOracleService,
) {
    suspend fun publish(): String {
        val (periodStart, periodEnd) = oracle.getCurrentWeekStartAndEnd()

        val events = collector.collect(periodStart, periodEnd)
        Log.i("FitVow - Sync", "Collected events: $events")

        repository.registerRunning(events.running)
        repository.registerSleep(events.sleep)
        repository.registerGymVisit(events.gymVisits)

        val runningEventsToPublish = repository.getRunningSessions().filter { it.timestamp.between(periodStart, periodEnd) && it.syncDetails == null }
        val sleepEventsToPublish = repository.getSleepSessions().filter { it.timestamp.between(periodStart, periodEnd) && it.syncDetails == null }
        val gymVisitEventsToPublish = repository.getGymVisitSessions().filter { it.timestamp.between(periodStart, periodEnd) && it.syncDetails == null }

        val timestamp = Instant.now()
        val weekIndex = oracle.getCurrentWeekIndex().toInt()
        val transactionHash = oracle.publishPhysicalActivityEvents(
            runningEventsToPublish,
            sleepEventsToPublish,
            gymVisitEventsToPublish
        )

        val syncDetails = PhysicalActivityEvent.SyncDetails(transactionHash, timestamp, BuildConfig.NETWORK, weekIndex)

        repository.updateRunning(runningEventsToPublish.map { it.copy(syncDetails = syncDetails) })
        repository.updateSleep(sleepEventsToPublish.map { it.copy(syncDetails = syncDetails) })
        repository.updateGymVisit(gymVisitEventsToPublish.map { it.copy(syncDetails = syncDetails) })

        return transactionHash
    }

    private fun Instant.between(periodStart: Instant, periodEnd: Instant): Boolean {
        return this >= periodStart && this <= periodEnd
    }
}