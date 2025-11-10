package com.august.fitnessvowsync.physicalactivity.model

data class PhysicalActivityEvents(val sleep: List<SleepEvent>, val running: List<RunningEvent>, val gymVisits: List<GymVisitEvent>) {
    data class GroupedEvents(
        val syncDetails: PhysicalActivityEvent.SyncDetails,
        val sleep: List<SleepEvent>,
        val running: List<RunningEvent>,
        val gymVisits: List<GymVisitEvent>
    )

    fun groupByTransactionHash(): List<GroupedEvents> {
        val allEvents = sequence {
            sleep.forEach { yield("sleep" to it) }
            running.forEach { yield("running" to it) }
            gymVisits.forEach { yield("gymVisits" to it) }
        }

        return allEvents
            .filter { !it.second.syncDetails?.transactionHash.isNullOrEmpty() }
            .groupBy { it.second.syncDetails!!.transactionHash }
            .map { (transactionHash, events) ->
                GroupedEvents(
                    syncDetails = events.first().second.syncDetails!!,
                    sleep = events.filter { it.first == "sleep" }.map { it.second as SleepEvent },
                    running = events.filter { it.first == "running" }.map { it.second as RunningEvent },
                    gymVisits = events.filter { it.first == "gymVisits" }.map { it.second as GymVisitEvent }
                )
            }
    }
}
