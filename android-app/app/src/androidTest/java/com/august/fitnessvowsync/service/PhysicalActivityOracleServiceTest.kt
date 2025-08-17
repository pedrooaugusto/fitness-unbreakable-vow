package com.august.fitnessvowsync.service

import com.august.fitnessvowsync.dagger.KeyStoreModule
import com.august.fitnessvowsync.dagger.Web3jModule
import com.august.fitnessvowsync.mapper.PhysicalActivityRecordMapper
import com.august.fitnessvowsync.model.AddPhysicalActivityRecordRequest
import org.junit.Test
import java.math.BigInteger

import org.junit.Assert.*

class PhysicalActivityOracleServiceTest {
    private val web3jModule = Web3jModule()
    private val networkName = web3jModule.provideNetwork()
    private val web3j = web3jModule.provideWeb3j(web3jModule.provideNetworkRpcUrl(networkName))
    private val credentials = web3jModule.provideWalletCredentials(networkName)
    private val physicalActivityOracle = web3jModule.providePhysicalActivityOracle(credentials, web3j)
    private val privateKeyService = AppPrivateKeyService(KeyStoreModule().provideKeyStore())
    private val physicalActivityRecordMapper = PhysicalActivityRecordMapper("HARDHAT")

    private val physicalActivityOracleService = PhysicalActivityOracleService(privateKeyService, physicalActivityOracle, physicalActivityRecordMapper)

    /*@Test
    fun shouldSendActivityRecord() {
        physicalActivityOracleService.registerAppAsRecordPublisher()

        val record = AddPhysicalActivityRecordRequest(BigInteger.valueOf(1000), BigInteger.valueOf(9000), BigInteger.valueOf(100), false)

        physicalActivityOracleService.addPhysicalActivityRecord(record)

        Thread.sleep(1000 * 60)

        val savedRecord = physicalActivityOracleService.getLatestPhysicalActivityRecord()

        assertTrue(savedRecord.runDistanceMeters == record.runDistanceMeters)
    }*/
}