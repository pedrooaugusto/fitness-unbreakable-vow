package com.august.fitnessvowsync.health

import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.request.AggregateRequest
import com.august.fitnessvowsync.helpers.WeakHashMapDelegate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.WeakHashMap
import javax.inject.Inject
import kotlin.reflect.KClass

private val totalDistanceMap = WeakHashMap<ExerciseSessionRecord, Double>()

class HealthConnectClient @Inject constructor(private var healthConnectClient: androidx.health.connect.client.HealthConnectClient) {
    suspend fun <T: Record> readRecords(recordType: KClass<T>, timeRangeFilter: TimeRangeFilter): List<T> {
        return withContext(Dispatchers.IO) {
            healthConnectClient.readRecords(ReadRecordsRequest(recordType, timeRangeFilter)).records
        }
    }

    suspend fun readRunningSessionRecords(timeRangeFilter: TimeRangeFilter): List<ExerciseSessionRecord> {
        return readRecords(ExerciseSessionRecord::class, timeRangeFilter).filter {
            it.exerciseType == ExerciseSessionRecord.EXERCISE_TYPE_RUNNING
        }
    }

    suspend fun readDistanceRecords(timeRangeFilter: TimeRangeFilter): List<DistanceRecord> {
        return readRecords(DistanceRecord::class, timeRangeFilter)
    }

    suspend fun readSleepSessionRecords(timeRangeFilter: TimeRangeFilter): List<SleepSessionRecord> {
        return readRecords(SleepSessionRecord::class, timeRangeFilter)
    }

    suspend fun readHeartRateRecords(timeRangeFilter: TimeRangeFilter): List<HeartRateRecord> {
        return readRecords(HeartRateRecord::class, timeRangeFilter)
    }

    suspend fun getAverageHeartRate(timeRangeFilter: TimeRangeFilter): Long {
        return withContext(Dispatchers.IO) {
            val response = healthConnectClient.aggregate(AggregateRequest(metrics = setOf(HeartRateRecord.BPM_AVG), timeRangeFilter = timeRangeFilter))

            response[HeartRateRecord.BPM_AVG] ?: 0
        }
    }
}

var ExerciseSessionRecord.totalDistance: Double by WeakHashMapDelegate(totalDistanceMap, 0.0)
