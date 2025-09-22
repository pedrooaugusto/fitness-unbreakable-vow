package com.august.fitnessvowsync.service

import android.util.Log
import com.august.fitnessvowsync.contract.ContractProvider
import com.august.fitnessvowsync.contract.PhysicalActivityOracle
import com.august.fitnessvowsync.mapper.PhysicalActivityRecordMapper
import com.august.fitnessvowsync.model.PhysicalActivityRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.math.BigInteger
import java.time.Instant
import javax.inject.Inject

interface PhysicalActivityOracleService {
    suspend fun addPhysicalActivityRecord(record: PhysicalActivityRecord): String
    suspend fun registerAppAsRecordPublisher()

    suspend fun getCreationDate(): BigInteger

    suspend fun getCurrentWeekIndex(): BigInteger

    suspend fun getSecondsInWeek(): BigInteger

    suspend fun getCurrentWeekStartAndEnd(): Pair<Instant, Instant>

    class DefaultPhysicalActivityOracleService @Inject constructor(
        private val privateKeyService: AppPrivateKeyService,
        private val physicalActivityOracle: ContractProvider<PhysicalActivityOracle>,
        private val physicalActivityRecordMapper: PhysicalActivityRecordMapper
    ): PhysicalActivityOracleService {
        override suspend fun addPhysicalActivityRecord(record: PhysicalActivityRecord): String {
            return withContext(Dispatchers.IO) {
                Log.i("FitVow", "Sending activity record to oracle. Record ${record}.")

                val recordAsUint32ByteArray = physicalActivityRecordMapper.toUint32ByteArray(record)
                val signature = privateKeyService.sign(recordAsUint32ByteArray)
                val contractRecord = physicalActivityRecordMapper.toContractPhysicalActivityRecord(record)

                Log.i("FitVow", "Record signed with public key: ${privateKeyService.getPublicKey()}")
                Log.i("FitVow", "Record signature: ${signature}")

                val transaction = physicalActivityOracle.get().pushPhysicalActivityRecord(signature, contractRecord).send()

                Log.i("FitVow", "Activity record submitted. Transaction hash: ${transaction.transactionHash}.")

                transaction.transactionHash
            }
        }

        override suspend fun registerAppAsRecordPublisher() {
            return withContext(Dispatchers.IO) {
                try {
                    privateKeyService.createIfNotExists()

                    val publicKey = privateKeyService.getPublicKey()
                    val currentPublicKey = physicalActivityOracle.get().BASE64_PUBLIC_KEY().send()

                    if (publicKey == currentPublicKey) return@withContext

                    //TODO: Remove
                    val transaction = physicalActivityOracle.get().setPublicKey(publicKey).send()

                    Log.i("FitVow", "Public key has been registered. Transaction hash: ${transaction.transactionHash}")
                    Log.i("FitVow", "Public key has been registered. Public key: ${publicKey}")
                } catch (e: Exception) {
                    Log.e("FitVow", "Failed to register publisher: ${e.message}", e)

                    throw e
                }
            }
        }

        override suspend fun getCreationDate(): BigInteger {
            return withContext(Dispatchers.IO) {
                physicalActivityOracle.get().CREATION_DATE().send()
            }
        }

        override suspend fun getCurrentWeekIndex(): BigInteger {
            return withContext(Dispatchers.IO) {
                physicalActivityOracle.get().currentWeekIndex.send()
            }
        }

        override suspend fun getSecondsInWeek(): BigInteger {
            return withContext(Dispatchers.IO) {
                physicalActivityOracle.get().SECONDS_IN_A_WEEK().send()
            }
        }

        override suspend fun getCurrentWeekStartAndEnd(): Pair<Instant, Instant> {
            val secondsInWeek = getSecondsInWeek()
            val creationDate = getCreationDate()
            val currentWeekIndex = getCurrentWeekIndex()

            val currentWeekStartDate = creationDate + currentWeekIndex * secondsInWeek
            val currentWeekEndDate = currentWeekStartDate + secondsInWeek

            return Pair(Instant.ofEpochSecond(currentWeekStartDate.toLong()), Instant.ofEpochSecond(currentWeekEndDate.toLong()))
        }
    }

    class PreviewPhysicalActivityOracleService: PhysicalActivityOracleService {
        override suspend fun addPhysicalActivityRecord(record: PhysicalActivityRecord): String {
            TODO("Not yet implemented")
        }

        override suspend fun registerAppAsRecordPublisher() {
            TODO("Not yet implemented")
        }

        override suspend fun getCreationDate(): BigInteger {
            TODO("Not yet implemented")
        }

        override suspend fun getCurrentWeekIndex(): BigInteger {
            TODO("Not yet implemented")
        }

        override suspend fun getSecondsInWeek(): BigInteger {
            TODO("Not yet implemented")
        }

        override suspend fun getCurrentWeekStartAndEnd(): Pair<Instant, Instant> {
            TODO("Not yet implemented")
        }
    }
}