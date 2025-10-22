package com.august.fitnessvowsync.service

import android.util.Log
import com.august.fitnessvowsync.contract.ContractProvider
import com.august.fitnessvowsync.contract.PhysicalActivityOracle
import com.august.fitnessvowsync.mapper.OracleP256SignatureMapper
import com.august.fitnessvowsync.mapper.PhysicalActivityRecordMapper
import com.august.fitnessvowsync.model.ContractPhase
import com.august.fitnessvowsync.model.PhysicalActivityRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.math.BigInteger
import java.nio.charset.StandardCharsets
import java.time.Instant
import javax.inject.Inject

interface PhysicalActivityOracleService {
    suspend fun addPhysicalActivityRecord(record: PhysicalActivityRecord): String
    suspend fun registerAppAsRecordPublisher()

    suspend fun getCreationDate(): BigInteger

    suspend fun getExpirationDate(): BigInteger

    suspend fun getCurrentWeekIndex(): BigInteger

    suspend fun getSecondsInWeek(): BigInteger

    suspend fun getCurrentWeekStartAndEnd(): Pair<Instant, Instant>

    suspend fun getContractPhase(): ContractPhase

    class DefaultPhysicalActivityOracleService @Inject constructor(
        private val protectedKeyService: HardwareProtectedKeyService,
        private val signatureMapper: OracleP256SignatureMapper,
        private val ipfsService: InterPlanetaryFileSystemService,
        private val physicalActivityOracle: ContractProvider<PhysicalActivityOracle>,
        private val physicalActivityRecordMapper: PhysicalActivityRecordMapper
    ): PhysicalActivityOracleService {
        override suspend fun addPhysicalActivityRecord(record: PhysicalActivityRecord): String {
            return withContext(Dispatchers.IO) {
                Log.i("FitVow", "Sending activity record to oracle. Record ${record}.")

                val recordAsUint32ByteArray = physicalActivityRecordMapper.toUint32ByteArray(record)
                val signature = signatureMapper.toP256Signature(protectedKeyService.sign(recordAsUint32ByteArray))
                val newRecordToAdd = physicalActivityRecordMapper.toContractPhysicalActivityRecord(record)

                Log.i("FitVow", "Record signed with public key: ${protectedKeyService.getPublicKey()}")
                Log.i("FitVow", "Record signature: $signature")

                val transaction = physicalActivityOracle.get().pushPhysicalActivityRecord(signature, newRecordToAdd).send()

                Log.i("FitVow", "Activity record submitted. Transaction hash: ${transaction.transactionHash}.")

                transaction.transactionHash
            }
        }

        override suspend fun registerAppAsRecordPublisher() {
            return withContext(Dispatchers.IO) {
                try {
                    val response = protectedKeyService.initializeKeyStore()

                    val publicKey = signatureMapper.toP256PublicKey(protectedKeyService.getPublicKey())
                    val currentPublicKey = signatureMapper.toP256PublicKey(physicalActivityOracle.get().PUBLIC_KEY().send())

                    Log.i("FitVow", "Current public key: $currentPublicKey")

                    //if (samePublicKey(publicKey, currentPublicKey)) return@withContext

                    val keyAttestation = uploadKeyAttestation(response)
                    //TODO: Remove
                    val transaction = physicalActivityOracle.get().setPublicKey(publicKey, keyAttestation).send()

                    Log.i("FitVow", "Public key has been registered. Transaction hash: ${transaction.transactionHash}")
                    Log.i("FitVow", "Public key has been registered. Public key: $publicKey")
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

        override suspend fun getExpirationDate(): BigInteger {
            return withContext(Dispatchers.IO) {
                physicalActivityOracle.get().EXPIRATION_DATE().send()
            }
        }

        override suspend fun getCurrentWeekIndex(): BigInteger {
            return withContext(Dispatchers.IO) {
                physicalActivityOracle.get().currentWeekIndex.send()
            }
        }

        override suspend fun getSecondsInWeek(): BigInteger {
            return withContext(Dispatchers.IO) {
                physicalActivityOracle.get().SECONDS_IN_ONE_WEEK().send()
            }
        }

        override suspend fun getContractPhase(): ContractPhase {
            return withContext(Dispatchers.IO) {
                val phase = physicalActivityOracle.get().contractPhase.send().toInt()

                ContractPhase.entries.firstOrNull { it.phase == phase } as ContractPhase
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

        private fun uploadKeyAttestation(response: HardwareProtectedKeyService.InitializeKeyStoreResponse): PhysicalActivityOracle.AndroidKeyAttestation {
            val ipfsUploadResponse = ipfsService.upload(response.keyAttestationCertificateChain)
            val attestationChallenge = response.keyAttestationChallenge ?: ""

            return signatureMapper.toAndroidKeyAttestation(ipfsUploadResponse, attestationChallenge)
        }

        private fun samePublicKey(key1: PhysicalActivityOracle.P256PublicKey, key2: PhysicalActivityOracle.P256PublicKey): Boolean {
            return key1.x.contentEquals(key2.x) && key1.y.contentEquals(key2.y)
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

        override suspend fun getExpirationDate(): BigInteger {
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

        override suspend fun getContractPhase(): ContractPhase {
            TODO("Not yet implemented")
        }
    }
}