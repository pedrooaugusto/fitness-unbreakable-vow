package com.august.fitnessvowsync.contract

import android.util.Log
import com.august.fitnessvowsync.physicalactivity.mapper.PhysicalActivityEventMapper
import com.august.fitnessvowsync.physicalactivity.model.GymVisitEvent
import com.august.fitnessvowsync.physicalactivity.model.RunningEvent
import com.august.fitnessvowsync.physicalactivity.model.SleepEvent
import com.august.fitnessvowsync.security.HardwareProtectedKeyService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.math.BigInteger
import javax.inject.Inject

class PhysicalActivityOracleService @Inject constructor(
    private val protectedKeyService: HardwareProtectedKeyService,
    private val signatureMapper: SignatureMapper,
    private val ipfsService: InterPlanetaryFileSystemService,
    private val physicalActivityOracle: ContractProvider<PhysicalActivityOracle>,
    private val physicalActivityEventMapper: PhysicalActivityEventMapper,
    private val balanceRetriever: AddressBalanceRetriever,
) {
    suspend fun publishPhysicalActivityEvents(
        runningEvents: List<RunningEvent>,
        sleepEvents: List<SleepEvent>,
        gymVisitEvents: List<GymVisitEvent>
    ): String {
        return withContext(Dispatchers.IO) {
            try {
                val running = buildRunningEvents(runningEvents)
                val sleep = buildSleepEvents(sleepEvents)
                val gymVisit = buildGymVisitEvents(gymVisitEvents)

                val request = PhysicalActivityOracle.PublishPhysicalActivityEventRequest(running, sleep, gymVisit)

                Log.i("FitVow - Sync", "Events signed with public key: ${protectedKeyService.getPublicKey()}")
                Log.i("FitVow - Sync", "Sending physical activity events to Oracle. Activities Running: $runningEvents; Sleep: $sleepEvents; Gym: $gymVisitEvents.")

                val transaction = physicalActivityOracle.get().publishPhysicalActivityEvent(request).send()

                Log.i("FitVow - Sync", "Physical activity events submitted. Transaction hash: ${transaction.transactionHash}.")

                transaction.transactionHash
            } catch (e: Exception) {
                Log.e("FitVow - Sync", "Failed to publish events: ${e.message}", e)
                throw e
            }
        }
    }

    suspend fun createPhysicalActivityPublisherPublicKey() {
        return withContext(Dispatchers.IO) {
            try {
                val response = protectedKeyService.initializeKeyStore()

                val appPublicKey = signatureMapper.toP256PublicKey(protectedKeyService.getPublicKey())
                val oraclePublicKey = signatureMapper.toP256PublicKey(physicalActivityOracle.get().PUBLIC_KEY().send())

                Log.i("FitVow - Sync", "Current public key: x: ${oraclePublicKey.x.toHexString()}; y: ${oraclePublicKey.y.toHexString()} ")

                if (samePublicKey(appPublicKey, oraclePublicKey)) return@withContext

                val keyAttestation = uploadKeyAttestation(response)
                val transaction = physicalActivityOracle.get().setPublicKey(appPublicKey, keyAttestation).send()

                Log.i("FitVow - Sync", "Public key has been registered. Transaction hash: ${transaction.transactionHash}")
                Log.i("FitVow - Sync", "Public key has been registered. Public key: x: ${appPublicKey.x.toHexString()}; y: ${appPublicKey.y.toHexString()}")
            } catch (e: Exception) {
                Log.e("FitVow - Sync", "Failed to register publisher: ${e.message}", e)
                throw e
            }
        }
    }

    suspend fun emergencyChangePhysicalActivityPublisherPublicKey(reason: String) {
        return withContext(Dispatchers.IO) {
            try {
                val response = protectedKeyService.initializeKeyStore()

                val appPublicKey = signatureMapper.toP256PublicKey(protectedKeyService.getPublicKey())
                val oraclePublicKey = signatureMapper.toP256PublicKey(physicalActivityOracle.get().PUBLIC_KEY().send())

                Log.i("FitVow - Sync", "Current public key: x: ${oraclePublicKey.x.toHexString()}; y: ${oraclePublicKey.y.toHexString()} ")

                val keyAttestation = uploadKeyAttestation(response)
                val fine = calculateEmergencyKeyChangeFine()
                val transaction = physicalActivityOracle.get().expensiveOneTimeEmergencyPublicKeyChange(
                    appPublicKey,
                    keyAttestation,
                    reason,
                    fine,
                ).send()

                Log.i("FitVow - Sync", "Public key has been changed. Transaction hash: ${transaction.transactionHash}")
                Log.i("FitVow - Sync", "Public key has been changed. Public key: x: ${appPublicKey.x.toHexString()}; y: ${appPublicKey.y.toHexString()}")
            } catch (e: Exception) {
                Log.e("FitVow - Sync", "Failed to change publisher public key: ${e.message}", e)
                throw e
            }
        }
    }

    suspend fun getPhysicalActivityStats(weekIndex: Int): PhysicalActivityOracle.PhysicalActivityStats {
        return withContext(Dispatchers.IO) {
            val result = physicalActivityOracle.get().physicalActivityStats(BigInteger.valueOf(weekIndex.toLong())).send()

            PhysicalActivityOracle.PhysicalActivityStats(result.component1()!!, result.component2()!!, result.component3()!!, result.component4()!!)
        }
    }

    suspend fun getGymVisitValidator(): PhysicalActivityOracle.GymVisitEventValidator {
        return withContext(Dispatchers.IO) {
            physicalActivityOracle.get().gymVisitValidator().send()
        }
    }

    suspend fun getOraclePublicKey(): PhysicalActivityOracle.P256PublicKey {
        return withContext(Dispatchers.IO) {
            signatureMapper.toP256PublicKey(physicalActivityOracle.get().PUBLIC_KEY().send())
        }
    }

    private fun buildRunningEvents(runningEvents: List<RunningEvent>): List<PhysicalActivityOracle.RunningEvent> {
        return runningEvents.map {
            physicalActivityEventMapper.toRunningEvent(
                it,
                protectedKeyService.sign(it.abiEncodePacked())
            )
        }
    }

    private fun buildSleepEvents(sleepEvents: List<SleepEvent>): List<PhysicalActivityOracle.SleepEvent> {
        return sleepEvents.map {
            physicalActivityEventMapper.toSleepEvent(
                it,
                protectedKeyService.sign(it.abiEncodePacked())
            )
        }
    }

    private fun buildGymVisitEvents(gymVisitEvents: List<GymVisitEvent>): List<PhysicalActivityOracle.GymVisitEvent> {
        return gymVisitEvents.map {
            physicalActivityEventMapper.toGymVisitEvent(
                it,
                protectedKeyService.sign(it.abiEncodePacked())
            )
        }
    }

    private fun uploadKeyAttestation(response: HardwareProtectedKeyService.InitializeKeyStoreResponse): PhysicalActivityOracle.AndroidKeyAttestation {
        val ipfsUploadResponse = ipfsService.upload(response.keyAttestationCertificateChain)
        val attestationChallenge = response.keyAttestationChallenge ?: ""

        return signatureMapper.toAndroidKeyAttestation(ipfsUploadResponse, attestationChallenge)
    }

    private fun samePublicKey(key1: PhysicalActivityOracle.P256PublicKey, key2: PhysicalActivityOracle.P256PublicKey): Boolean {
        return key1.x.contentEquals(key2.x) && key1.y.contentEquals(key2.y)
    }

    private fun ByteArray.toHexString(): String {
        return this.joinToString("") { "%02x".format(it) }
    }

    private fun ByteArray.isNull(): Boolean {
        val nullByte32Array = ByteArray(32)

        return nullByte32Array.toHexString() == this.toHexString()
    }

    private fun calculateEmergencyKeyChangeFine(): BigInteger {
        val vowBalance = balanceRetriever.getWeiBalance(physicalActivityOracle.get().FITNESS_UNBREAKABLE_VOW().send())
        val finePercentage = physicalActivityOracle.get().ONE_TIME_EMERGENCY_KEY_CHANGE_PRICE().send().plus(BigInteger.ONE)

        return (finePercentage * vowBalance) / BigInteger.valueOf(100)
    }
}