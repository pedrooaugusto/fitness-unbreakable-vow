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
import java.time.Instant
import javax.inject.Inject

class PhysicalActivityOracleService @Inject constructor(
    private val protectedKeyService: HardwareProtectedKeyService,
    private val signatureMapper: SignatureMapper,
    private val ipfsService: InterPlanetaryFileSystemService,
    private val physicalActivityOracle: ContractProvider<PhysicalActivityOracle>,
    private val physicalActivityEventMapper: PhysicalActivityEventMapper,
) {
    enum class ContractPhase(val phase: Int) {
        Active(0),
        Grace(1),
        FullyExpired(2)
    }

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

    suspend fun registerAppAsRecordPublisher() {
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

    suspend fun getCreationDate(): BigInteger {
        return withContext(Dispatchers.IO) {
            physicalActivityOracle.get().CREATION_DATE().send()
        }
    }

    suspend fun getExpirationDate(): BigInteger {
        return withContext(Dispatchers.IO) {
            physicalActivityOracle.get().EXPIRATION_DATE().send()
        }
    }

    suspend fun getCurrentWeekIndex(): BigInteger {
        return withContext(Dispatchers.IO) {
            physicalActivityOracle.get().currentWeekIndex.send()
        }
    }

    suspend fun getSecondsInWeek(): BigInteger {
        return withContext(Dispatchers.IO) {
            physicalActivityOracle.get().SECONDS_IN_ONE_WEEK().send()
        }
    }

    suspend fun getContractPhase(): ContractPhase {
        return withContext(Dispatchers.IO) {
            val phase = physicalActivityOracle.get().contractPhase.send().toInt()

            ContractPhase.entries.firstOrNull { it.phase == phase } as ContractPhase
        }
    }

    suspend fun getCurrentWeekStartAndEnd(): Pair<Instant, Instant> {
        val secondsInWeek = getSecondsInWeek()
        val creationDate = getCreationDate()
        val currentWeekIndex = getCurrentWeekIndex()

        val currentWeekStartDate = creationDate + currentWeekIndex * secondsInWeek
        val currentWeekEndDate = currentWeekStartDate + secondsInWeek

        return Pair(Instant.ofEpochSecond(currentWeekStartDate.toLong()), Instant.ofEpochSecond(currentWeekEndDate.toLong()))
    }

    suspend fun getPhysicalActivityStats(weekIndex: Int): PhysicalActivityOracle.PhysicalActivityStats {
        return withContext(Dispatchers.IO) {
            val result = physicalActivityOracle.get().physicalActivityStats(BigInteger.valueOf(weekIndex.toLong())).send()

            PhysicalActivityOracle.PhysicalActivityStats(result.component1()!!, result.component2()!!, result.component3()!!, result.component4()!!)
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
}