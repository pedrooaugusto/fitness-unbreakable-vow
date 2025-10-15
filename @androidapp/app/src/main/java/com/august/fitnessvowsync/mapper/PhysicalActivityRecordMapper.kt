package com.august.fitnessvowsync.mapper

import com.august.fitnessvowsync.contract.PhysicalActivityOracle
import com.august.fitnessvowsync.model.PhysicalActivityRecord
import com.august.fitnessvowsync.model.SyncedPhysicalActivityRecord
import java.math.BigInteger
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PhysicalActivityRecordMapper @Inject constructor(private val blockExplorerUrlMapper: BlockExplorerUrlMapper) {
    fun toContractPhysicalActivityRecord(record: PhysicalActivityRecord): PhysicalActivityOracle.PhysicalActivityRecord {
        return PhysicalActivityOracle.PhysicalActivityRecord(
            BigInteger.valueOf(record.timestamp.epochSecond),
            BigInteger.valueOf(record.runDistanceMeters.toLong()),
            BigInteger.valueOf(record.healthySleepNights.toLong()),
            BigInteger.valueOf(record.gymVisits.toLong())
        )
    }

    fun toUint32ByteArray(record: PhysicalActivityRecord): ByteArray {
        val buffer = ByteBuffer.allocate(4 * 4) // 4 uint32 = 16 bytes

        buffer.order(ByteOrder.LITTLE_ENDIAN)

        buffer.putInt(record.timestamp.epochSecond.toInt()) // 2038 problem?? This value is overridden in the contract anyway (I think...)
        buffer.putInt(record.runDistanceMeters.toInt())
        buffer.putInt(record.healthySleepNights.toInt())
        buffer.putInt(record.gymVisits.toInt())

        return buffer.array()
    }

    fun toSyncedPhysicalActivityRecord(record: PhysicalActivityRecord, weekIndex: BigInteger, transactionHash: String): SyncedPhysicalActivityRecord {
        return SyncedPhysicalActivityRecord(
            record.timestamp,
            record.runDistanceMeters,
            record.healthySleepNights,
            record.gymVisits,
            transactionHash,
            blockExplorerUrlMapper.toTransactionUrl(transactionHash),
            weekIndex.toInt()
        )
    }
}