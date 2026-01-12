package com.august.fitnessvowsync.health

import androidx.health.connect.client.aggregate.AggregateMetric
import androidx.health.connect.client.aggregate.AggregationResult
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.SleepStageRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.metadata.DataOrigin
import androidx.health.connect.client.request.AggregateRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.reflect.KClass

class HealthConnectClient @Inject constructor(
    private var healthConnectClient: androidx.health.connect.client.HealthConnectClient
) {
    private val dataOriginFilter = setOf(
        DataOrigin("com.sec.android.app.shealth"),
        // TODO: Remove support fake data during development
        //DataOrigin("com.august.fitnessvowsync")
    )

    suspend fun <T: Record> readRecords(recordType: KClass<T>, timeRangeFilter: TimeRangeFilter): List<T> {
        return withContext(Dispatchers.IO) {
            healthConnectClient.readRecords(ReadRecordsRequest(recordType, timeRangeFilter, dataOriginFilter)).records
        }
    }

    suspend fun readRunningSessionRecords(timeRangeFilter: TimeRangeFilter): List<ExerciseSessionRecord> {
        return readRecords(ExerciseSessionRecord::class, timeRangeFilter).filter {
            it.exerciseType == ExerciseSessionRecord.EXERCISE_TYPE_RUNNING
        }
    }

    suspend fun readSleepSessionRecords(timeRangeFilter: TimeRangeFilter): List<SleepSessionRecord> {
        return readRecords(SleepSessionRecord::class, timeRangeFilter)
    }

    suspend fun readSleepStageRecords(timeRangeFilter: TimeRangeFilter): List<SleepStageRecord> {
        return readRecords(SleepStageRecord::class, timeRangeFilter)
    }

    suspend fun aggregate(timeRangeFilter: TimeRangeFilter, metrics: Set<AggregateMetric<*>>): AggregationResult {
        return withContext(Dispatchers.IO) {
            healthConnectClient.aggregate(AggregateRequest(
                metrics = metrics,
                timeRangeFilter = timeRangeFilter,
                dataOriginFilter = dataOriginFilter
            ))
        }
    }
}
