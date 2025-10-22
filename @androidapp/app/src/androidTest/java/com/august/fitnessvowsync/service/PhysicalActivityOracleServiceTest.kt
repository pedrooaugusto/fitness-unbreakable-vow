package com.august.fitnessvowsync.service

import androidx.test.ext.junit.runners.AndroidJUnit4

import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PhysicalActivityOracleServiceTest {
    /*private val web3j = Web3j.build(HttpService(("http://192.168.0.105:8545/")))
    private val credentials = Credentials.create("")
    private val contract = CheckP256Precompile.load("", web3j, credentials, DefaultGasProvider())
    private val privateKeyService = AppPrivateKeyService(KeyStoreModule().provideKeyStore())

    @Test
    fun shouldSendActivityRecord() {
        privateKeyService.createIfNotExists()

        val publicKey = privateKeyService.getRawPublicKey()

        val transaction = contract.setPublicKey2(CheckP256Precompile.P256PublicKey(publicKey.first, publicKey.second)).send()

        val text = "pedro"
        val bytes = text.toByteArray(Charsets.UTF_8)
        val data = ByteArray(32)
        System.arraycopy(bytes, 0, data, 32 - bytes.size, bytes.size)

        val signature = privateKeyService.signP1363(data)

        val result = contract.verifySignature2(CheckP256Precompile.P256Signature(signature.first, signature.second), data).send()

        assertTrue(result)
    }*/
}