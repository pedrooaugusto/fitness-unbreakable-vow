package com.august.fitnessvowsync.mapper

import com.august.fitnessvowsync.contract.PhysicalActivityOracle
import com.august.fitnessvowsync.model.AddPhysicalActivityRecordRequest
import com.august.fitnessvowsync.model.AddPhysicalActivityRecordTransaction
import com.august.fitnessvowsync.model.GetPhysicalActivityRecordResponse
import com.august.fitnessvowsync.model.PhysicalActivityRecordDefinition
import java.math.BigInteger
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class PhysicalActivityRecordMapper @Inject constructor(@Named("NETWORK") private val network: String) {
    fun toContractPhysicalActivityRecord(record: AddPhysicalActivityRecordRequest): PhysicalActivityOracle.PhysicalActivityRecord {
        return PhysicalActivityOracle.PhysicalActivityRecord(
            record.timestamp,
            BigInteger.valueOf(record.runDistanceMeters.toLong()),
            BigInteger.valueOf(record.healthySleepNights.toLong()),
            BigInteger.valueOf(record.gymVisits.toLong())
        )
    }

    fun toUint32ByteArray(record: AddPhysicalActivityRecordRequest): ByteArray {
        val buffer = ByteBuffer.allocate(4 * 4) // 4 uint32 = 16 bytes

        buffer.order(ByteOrder.LITTLE_ENDIAN)

        buffer.putInt(record.timestamp.toInt()) // 2038 problem?? Overridden in the contract anyway
        buffer.putInt(record.runDistanceMeters.toInt())
        buffer.putInt(record.healthySleepNights.toInt())
        buffer.putInt(record.gymVisits.toInt())

        return buffer.array()
    }

    fun toAddRecordTransaction(record: PhysicalActivityRecordDefinition, hash: String): AddPhysicalActivityRecordTransaction {
        return AddPhysicalActivityRecordTransaction(
            record.timestamp,
            record.runDistanceMeters,
            record.healthySleepNights,
            record.gymVisits,
            hash,
            transactionHashToBlockExplorerUrl(hash)
        )
    }

    fun toGetPhysicalActivityRecordResponse(weekNumber: BigInteger, record: PhysicalActivityOracle.PhysicalActivityRecord): GetPhysicalActivityRecordResponse {
        return GetPhysicalActivityRecordResponse(
            record.timestamp,
            record.runDistanceMeters.toInt(),
            record.healthySleepNights.toInt(),
            record.gymVisits.toInt(),
            weekNumber
        )
    }

    private fun transactionHashToBlockExplorerUrl(hash: String): String {
        if (hash.isEmpty()) return ""
        if (network == "SEPOLIA") return "http://sepolia.etherscan.io/tx/${hash}"

        return "https://www.arbiscan.io/tx/${hash}"
    }
}