package com.august.fitnessvowsync.service

import android.content.SharedPreferences
import android.util.Log
import com.august.fitnessvowsync.helpers.BigIntegerTypeAdapter
import com.august.fitnessvowsync.helpers.InstantTypeAdapter
import com.august.fitnessvowsync.mapper.PhysicalActivityRecordMapper
import com.august.fitnessvowsync.model.PhysicalActivityRecord
import com.august.fitnessvowsync.model.PhysicalActivityRecordImpl
import com.august.fitnessvowsync.model.SyncedPhysicalActivityRecord
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.math.BigInteger
import java.time.Instant
import javax.inject.Inject

interface SyncPhysicalActivityRecordService {
    suspend fun getCurrentPhysicalActivityRecord(): PhysicalActivityRecord
    suspend fun syncCurrentPhysicalActivityRecord(): SyncedPhysicalActivityRecord
    suspend fun getSyncedPhysicalActivityRecords(): List<SyncedPhysicalActivityRecord>

    class SyncPhysicalActivityRecordServiceImpl @Inject constructor(
        private val healthConnectAggregator: HealthConnectAggregationService,
        private val oracleService: PhysicalActivityOracleService,
        private val gymVisitService: GymVisitService,
        private val recordMapper: PhysicalActivityRecordMapper,
        private val encryptedPreferences: SharedPreferences
    ): SyncPhysicalActivityRecordService {
        private val SYNCED_RECORDS_PREF_KEY = "SYNCED_RECORDS_PREF_KEY"
        private val gson: Gson by lazy {
            GsonBuilder()
                .registerTypeAdapter(BigInteger::class.java, BigIntegerTypeAdapter())
                .registerTypeAdapter(Instant::class.java, InstantTypeAdapter())
                .create()
        }
        override suspend fun getCurrentPhysicalActivityRecord(): PhysicalActivityRecord {
            val (periodStart, periodEnd) = oracleService.getCurrentWeekStartAndEnd()

            val healthySleepNights = healthConnectAggregator.numberOfHealthyNightsOfSleep(periodStart, periodEnd)
            val runDistanceMeters = healthConnectAggregator.longestDistanceRan(periodStart, periodEnd)
            val gymVisits = gymVisitService.getGymVisits(periodStart, periodEnd).size
            val timestamp = Instant.ofEpochSecond(System.currentTimeMillis() / 1000)

            return PhysicalActivityRecordImpl(timestamp, runDistanceMeters, healthySleepNights, gymVisits)
        }

        override suspend fun syncCurrentPhysicalActivityRecord(): SyncedPhysicalActivityRecord {
            val record = getCurrentPhysicalActivityRecord()
            val currentWeekIndex = oracleService.getCurrentWeekIndex()

            Log.i("FitVow", "Syncing record with oracle: ${record}")

            val transactionHash = oracleService.addPhysicalActivityRecord(record)

            val syncedPhysicalActivityRecord = recordMapper.toSyncedPhysicalActivityRecord(record, currentWeekIndex, transactionHash)

            val syncedRecords = getSyncedPhysicalActivityRecords().plus(syncedPhysicalActivityRecord)

            with(encryptedPreferences.edit()) {
                putString(SYNCED_RECORDS_PREF_KEY, gson.toJson(syncedRecords))
                apply()
            }

            Log.i("FitVow", "Synced record: ${syncedPhysicalActivityRecord}")

            return syncedPhysicalActivityRecord
        }

        override suspend fun getSyncedPhysicalActivityRecords(): List<SyncedPhysicalActivityRecord> {
            val addedRecords = encryptedPreferences.getString(SYNCED_RECORDS_PREF_KEY, null)

            if (addedRecords.isNullOrBlank()) return emptyList()

            val type = object : com.google.gson.reflect.TypeToken<List<SyncedPhysicalActivityRecord>>() {}.type

            return gson.fromJson(addedRecords, type)
        }
    }

    class PreviewSyncPhysicalActivityRecordsService: SyncPhysicalActivityRecordService {
        override suspend fun getCurrentPhysicalActivityRecord(): PhysicalActivityRecord { TODO("Not yet implemented") }
        override suspend fun syncCurrentPhysicalActivityRecord(): SyncedPhysicalActivityRecord { TODO("Not yet implemented") }
        override suspend fun getSyncedPhysicalActivityRecords(): List<SyncedPhysicalActivityRecord> { TODO("Not yet implemented") }
    }
}
