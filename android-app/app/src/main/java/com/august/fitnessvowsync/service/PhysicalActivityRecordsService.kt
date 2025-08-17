package com.august.fitnessvowsync.service

import android.content.SharedPreferences
import com.august.fitnessvowsync.helpers.BigIntegerTypeAdapter
import com.august.fitnessvowsync.mapper.PhysicalActivityRecordMapper
import com.august.fitnessvowsync.model.AddPhysicalActivityRecordRequest
import com.august.fitnessvowsync.model.AddPhysicalActivityRecordTransaction
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.math.BigInteger
import javax.inject.Inject

interface IPhysicalActivityRecordsService {
    suspend fun getNextSyncPhysicalActivityRecord(): AddPhysicalActivityRecordRequest
    suspend fun syncPhysicalActivityRecord(): AddPhysicalActivityRecordTransaction

    suspend fun getSyncedRecordsTransactions(): List<AddPhysicalActivityRecordTransaction>
}

class PhysicalActivityRecordsService @Inject constructor(
    private val aggregator: HealthConnectAggregationService,
    private val oracleService: PhysicalActivityOracleService,
    private val geofencingService: GeofencingService,
    private val recordMapper: PhysicalActivityRecordMapper,
    private val encryptedPreferences: SharedPreferences
): IPhysicalActivityRecordsService {
    private val SYNCED_RECORDS_PREF_KEY = "SYNCED_RECORDS_PREF_KEY"
    private val gson: Gson by lazy {
        GsonBuilder()
            .registerTypeAdapter(BigInteger::class.java, BigIntegerTypeAdapter())
            .create()
    }

    override suspend fun getNextSyncPhysicalActivityRecord(): AddPhysicalActivityRecordRequest {
        val healthySleepNights = aggregator.numberOfHealthyNightsOfSleep()
        val maxDistanceRan = aggregator.longestDistanceRan().toInt()
        val gymVisits = geofencingService.getGymVisits().size
        val timestamp = BigInteger.valueOf(System.currentTimeMillis() / 1000)

        return AddPhysicalActivityRecordRequest(timestamp, maxDistanceRan, healthySleepNights, gymVisits)
    }

    override suspend fun syncPhysicalActivityRecord(): AddPhysicalActivityRecordTransaction {
        val recordToAdd = getNextSyncPhysicalActivityRecord()

        val newTransaction = recordMapper.toAddRecordTransaction(recordToAdd, oracleService.addPhysicalActivityRecord(recordToAdd))

        val transactions = getSyncedRecordsTransactions().plus(newTransaction)

        with(encryptedPreferences.edit()) {
            putString(SYNCED_RECORDS_PREF_KEY, gson.toJson(transactions))
            apply()
        }

        return newTransaction
    }

    override suspend fun getSyncedRecordsTransactions(): List<AddPhysicalActivityRecordTransaction> {
        val syncedRecords = encryptedPreferences.getString(SYNCED_RECORDS_PREF_KEY, null)

        if (syncedRecords.isNullOrBlank()) return emptyList()

        val type = object : com.google.gson.reflect.TypeToken<List<AddPhysicalActivityRecordTransaction>>() {}.type

        return gson.fromJson(syncedRecords, type)
    }
}

class PreviewPhysicalActivityRecordsService: IPhysicalActivityRecordsService {
    override suspend fun getNextSyncPhysicalActivityRecord(): AddPhysicalActivityRecordRequest { TODO("Not yet implemented") }
    override suspend fun syncPhysicalActivityRecord(): AddPhysicalActivityRecordTransaction { TODO("Not yet implemented") }
    override suspend fun getSyncedRecordsTransactions(): List<AddPhysicalActivityRecordTransaction> { TODO("Not yet implemented") }
}
