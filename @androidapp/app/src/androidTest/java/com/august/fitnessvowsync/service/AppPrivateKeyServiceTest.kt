package com.august.fitnessvowsync.service

import com.august.fitnessvowsync.dagger.KeyStoreModule
import com.august.fitnessvowsync.mapper.PhysicalActivityRecordMapper
import org.junit.Ignore
import org.junit.Test
import java.math.BigInteger

class AppPrivateKeyServiceTest {
    /*private val privateKeyService = AppPrivateKeyService(KeyStoreModule().provideKeyStore())
    private val physicalActivityRecordMapper = PhysicalActivityRecordMapper("HARDHAT")
    private val record = AddPhysicalActivityRecordRequest(BigInteger.valueOf(1000), (2000), (3), (0))

    @Test
    fun shouldSign() {
        val byteArrayRecord = physicalActivityRecordMapper.toUint32ByteArray(record)

        privateKeyService.createIfNotExists()
        privateKeyService.sign(byteArrayRecord)
    }

    @Test
    @Ignore("Debug only")
    fun shouldSignAndVerify2() {
        privateKeyService.createIfNotExists()
        val byteArrayRecord = physicalActivityRecordMapper.toUint32ByteArray(record)
        val signature = privateKeyService.sign(byteArrayRecord)

        println(signature)
        println(privateKeyService.getPublicKey())
    }*/
}