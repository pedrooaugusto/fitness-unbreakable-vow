package com.august.fitnessvowsync

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.august.fitnessvowsync.contract.InterPlanetaryFileSystemService
import com.august.fitnessvowsync.contract.PhysicalActivityOracleService
import com.august.fitnessvowsync.contract.SignatureMapper
import com.august.fitnessvowsync.contract.TimeLordService
import com.august.fitnessvowsync.dagger.EncryptedSharedPreferencesModule
import com.august.fitnessvowsync.dagger.HealthConnectModule
import com.august.fitnessvowsync.dagger.KeyStoreModule
import com.august.fitnessvowsync.dagger.Web3jModule
import com.august.fitnessvowsync.health.HealthConnectAggregator
import com.august.fitnessvowsync.helpers.SettingsService
import com.august.fitnessvowsync.physicalactivity.collection.PhysicalActivityEventCollector
import com.august.fitnessvowsync.physicalactivity.collection.PhysicalActivityEventPublisher
import com.august.fitnessvowsync.physicalactivity.data.GymVisitTracker
import com.august.fitnessvowsync.physicalactivity.data.PhysicalActivityEventRepository
import com.august.fitnessvowsync.physicalactivity.mapper.GymVisitValidatorMapper
import com.august.fitnessvowsync.physicalactivity.mapper.PhysicalActivityEventMapper
import com.august.fitnessvowsync.security.HardwareProtectedKeyService
import com.august.fitnessvowsync.testing.HealthConnectTestHelper
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PhysicalActivityEventPublisherTest {
    private lateinit var healthConnectTestHelper: HealthConnectTestHelper
    private lateinit var collector: PhysicalActivityEventCollector
    private lateinit var repository: PhysicalActivityEventRepository
    private lateinit var publisher: PhysicalActivityEventPublisher
    private lateinit var oracle: PhysicalActivityOracleService
    private lateinit var timeLord: TimeLordService
    private lateinit var gymVisitTracker: GymVisitTracker
    private lateinit var periodStart: java.time.Instant
    private lateinit var periodEnd: java.time.Instant

    companion object {
        @BeforeClass
        @JvmStatic
        fun setPermissions() { HealthConnectTestHelper.openHealthConnectAndWait() }
    }

    @Test
    public fun publish_sends_events_to_oracle() = runBlocking {
        val (weekIndex, weekStart, _) = getWeekWindow()
        val statsBefore = oracle.getPhysicalActivityStats(weekIndex)

        val timestamps = seedOneOfEachEvent(weekStart)
        val txHash = publisher.publish()

        assertRepositorySyncedWith(txHash, timestamps)

        val statsAfter = oracle.getPhysicalActivityStats(weekIndex)
        assertOracleCountsIncrementedByOne(statsBefore, statsAfter)
    }

    @Before
    public fun setUp() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val encryptedPreferences = EncryptedSharedPreferencesModule().let { it.provideEncryptedSharedPreferences(context, it.provideMainKeyAlias()) }
        val protectedKeyService = HardwareProtectedKeyService(KeyStoreModule().provideKeyStore())
        val signatureMapper = SignatureMapper()
        val physicalActivityEventMapper = PhysicalActivityEventMapper(signatureMapper)
        val settingsService = initSettings(SettingsService.SettingsServiceImpl(encryptedPreferences, protectedKeyService, signatureMapper))
        val healthConnectClient = HealthConnectModule().provideHealthConnectClient(context)
        val healthConnectAggregator = HealthConnectAggregator(com.august.fitnessvowsync.health.HealthConnectClient(healthConnectClient))
        val physicalActivityOracle = Web3jModule().providePhysicalActivityOracle(settingsService)
        val timeLordContact = Web3jModule().provideTimeLord(settingsService, physicalActivityOracle)
        val ipfsService = InterPlanetaryFileSystemService(settingsService)

        gymVisitTracker = GymVisitTracker(encryptedPreferences)
        oracle = PhysicalActivityOracleService(
            protectedKeyService,
            signatureMapper,
            ipfsService,
            physicalActivityOracle,
            physicalActivityEventMapper,
        )
        timeLord = TimeLordService(timeLordContact)
        repository = PhysicalActivityEventRepository(encryptedPreferences)
        healthConnectTestHelper = HealthConnectTestHelper(healthConnectClient, gymVisitTracker)
        collector = PhysicalActivityEventCollector(healthConnectAggregator, gymVisitTracker, physicalActivityEventMapper)
        publisher = PhysicalActivityEventPublisher(repository, collector, oracle, timeLord)

        oracle.registerAppAsRecordPublisher()
        GymVisitValidatorMapper()
            .toTrackedGyms(oracle.getGymVisitValidator())
            .forEach { gymVisitTracker.addTrackedGym(it) }

        // Cache the current week for later cleanup
        val (start, end) = timeLord.getCurrentWeekStartAndEnd()
        periodStart = start
        periodEnd = end
    }

    // Helpers
    private suspend fun getWeekWindow(): Triple<Int, java.time.Instant, java.time.Instant> {
        val (start, end) = timeLord.getCurrentWeekStartAndEnd()
        periodStart = start
        periodEnd = end
        val index = timeLord.getCurrentWeekIndex().toInt()
        return Triple(index, start, end)
    }

    private data class SeededTimestamps(
        val runStart: java.time.Instant,
        val sleepStart: java.time.Instant,
        val gymStart: java.time.Instant,
    )

    private suspend fun seedOneOfEachEvent(weekStart: java.time.Instant): SeededTimestamps {
        val base = weekStart.plusSeconds(309)

        val runStart = base
        val runEnd = runStart.plusSeconds(600)
        healthConnectTestHelper.insertRunningSession(runStart, runEnd, distanceMeters = 3000.0, avgBpm = 120)

        val sleepStart = base.plusSeconds(3600 * 3)
        val sleepEnd = sleepStart.plusSeconds(3600 * 8)
        healthConnectTestHelper.insertSleepSession(sleepStart, sleepEnd, avgBpm = 60)

        val gymStart = base.plusSeconds(3600)
        val gymEnd = gymStart.plusSeconds(1800)
        healthConnectTestHelper.insertGymVisit(
            start = gymStart,
            end = gymEnd,
            gym = gymVisitTracker.getTrackedGyms().first(),
            avgBpm = 120
        )

        return SeededTimestamps(runStart, sleepStart, gymStart)
    }

    private fun assertRepositorySyncedWith(txHash: String, ts: SeededTimestamps) {
        val run = repository.getRunningSessions().firstOrNull { it.timestamp.epochSecond == ts.runStart.epochSecond }
        val sleep = repository.getSleepSessions().firstOrNull { it.timestamp.epochSecond == ts.sleepStart.epochSecond }
        val gym = repository.getGymVisitSessions().firstOrNull { it.timestamp.epochSecond == ts.gymStart.epochSecond }

        println("Repository events, sleep: ${repository.getSleepSessions()}")
        println("Repository events, running: ${repository.getRunningSessions()}")
        println("Repository events, gym: ${repository.getGymVisitSessions()}")

        org.junit.Assert.assertNotNull("Running event not found", run)
        org.junit.Assert.assertNotNull("Sleep event not found", sleep)
        org.junit.Assert.assertNotNull("Gym visit event not found", gym)

        org.junit.Assert.assertEquals(txHash, run!!.syncDetails?.transactionHash)
        org.junit.Assert.assertEquals(txHash, sleep!!.syncDetails?.transactionHash)
        org.junit.Assert.assertEquals(txHash, gym!!.syncDetails?.transactionHash)
    }

    private fun assertOracleCountsIncrementedByOne(
        before: com.august.fitnessvowsync.contract.PhysicalActivityOracle.PhysicalActivityStats,
        after: com.august.fitnessvowsync.contract.PhysicalActivityOracle.PhysicalActivityStats,
    ) {
        org.junit.Assert.assertEquals(before.running.count.toInt() + 1, after.running.count.toInt())
        org.junit.Assert.assertEquals(before.gym.count.toInt() + 1, after.gym.count.toInt())
        org.junit.Assert.assertEquals(before.sleep.count.toInt() + 1, after.sleep.count.toInt())
    }

    @After
    fun tearDown() = runBlocking {
        healthConnectTestHelper.clearRecordsInRange(periodStart, periodEnd)
    }

    private fun initSettings(settings: SettingsService): SettingsService {
        settings.saveRpcEndpoint("http://192.168.0.105:8545")
        settings.savePinataApiToken("test")
        // Hardhat development private key, no issues
        settings.saveClientAccountPrivateKey("")

        return settings
    }
}
