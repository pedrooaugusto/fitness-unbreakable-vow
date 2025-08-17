package com.august.fitnessvowsync.service

import android.util.Log
import com.august.fitnessvowsync.contract.PhysicalActivityOracle
import com.august.fitnessvowsync.mapper.PhysicalActivityRecordMapper
import com.august.fitnessvowsync.model.AddPhysicalActivityRecordRequest
import com.august.fitnessvowsync.model.GetPhysicalActivityRecordResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class PhysicalActivityOracleService @Inject constructor(
    private val privateKeyService: AppPrivateKeyService,
    private val physicalActivityOracle: PhysicalActivityOracle,
    private val physicalActivityRecordMapper: PhysicalActivityRecordMapper
) {
    suspend fun addPhysicalActivityRecord(record: AddPhysicalActivityRecordRequest): String {
        return withContext(Dispatchers.IO) {
            Log.i("FitVow", "Sending activity record to oracle. Record ${record}.")

            val recordAsUint32ByteArray = physicalActivityRecordMapper.toUint32ByteArray(record)
            val signature = privateKeyService.sign(recordAsUint32ByteArray)
            val contractRecord = physicalActivityRecordMapper.toContractPhysicalActivityRecord(record)

            Log.i("FitVow", "Record signed with public key: ${privateKeyService.getPublicKey()}")
            Log.i("FitVow", "Record signature: ${signature}")

            val transaction = physicalActivityOracle.pushPhysicalActivityRecord(signature, contractRecord).send()

            Log.i("FitVow", "Activity record submitted. Transaction hash: ${transaction.transactionHash}.")

            transaction.transactionHash
        }
    }

    suspend fun registerAppAsRecordPublisher() {
        return withContext(Dispatchers.IO) {
            try {
                privateKeyService.createIfNotExists()

                val publicKey = privateKeyService.getPublicKey()
                val currentPublicKey = physicalActivityOracle.BASE64_PUBLIC_KEY().send()

                if (publicKey == currentPublicKey) return@withContext

                val transaction = physicalActivityOracle.setPublicKey(publicKey).send()

                Log.i("FitVow", "Public key has been registered. Transaction hash: ${transaction.transactionHash}")
                Log.i("FitVow", "Public key has been registered. Public key: ${publicKey}")
            } catch (e: Exception) {
                Log.e("FitVow", "Failed to register publisher: ${e.message}", e)

                throw e
            }
        }
    }

    suspend fun getLatestPhysicalActivityRecord(): GetPhysicalActivityRecordResponse {
        return withContext(Dispatchers.IO) {
            val result = physicalActivityOracle.currentWeekPhysicalActivityRecord.send()

            physicalActivityRecordMapper.toGetPhysicalActivityRecordResponse(result.component1(), result.component2())
        }
    }
}
