package com.august.fitnessvowsync.service

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyInfo
import android.security.keystore.KeyProperties
import android.util.Base64
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.nio.charset.StandardCharsets
import java.security.InvalidKeyException
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PrivateKey
import java.security.Security
import java.security.Signature
import java.security.cert.X509Certificate
import java.security.interfaces.ECPublicKey
import java.security.spec.ECGenParameterSpec
import javax.inject.Inject
import kotlin.random.Random

class HardwareProtectedKeyService @Inject constructor(private val keyStore: KeyStore) {
    private companion object {
        private const val KEY_ALIAS = "Physical_Activity_Oracle_Client"
        public const val KEY_SPEC = "secp256r1" // P-256
        public const val SIGNATURE_ALGORITHM = "SHA256withECDSA"
    }

    fun initializeKeyStore(): InitializeKeyStoreResponse {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) Security.addProvider(BouncyCastleProvider())

        if (keyStore.containsAlias(KEY_ALIAS)) return InitializeKeyStoreResponse(false, getKeyAttestationPem())

        val keyPairGenerator = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC, keyStore.provider)
        val challenge = randomPass()
        val parameterSpec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY
        )
            .setUserAuthenticationRequired(false)
            .setAlgorithmParameterSpec(ECGenParameterSpec(KEY_SPEC))
            .setDigests(KeyProperties.DIGEST_SHA256)
            .setAttestationChallenge(challenge)
            .build()

        keyPairGenerator.initialize(parameterSpec)
        keyPairGenerator.generateKeyPair()

        // Prevents non-secure keys.
        assertKeyIsInsideSecureHardware()

        return InitializeKeyStoreResponse(true, getKeyAttestationPem(), challenge.decodeToString())
    }

    fun sign(data: ByteArray): ByteArray {
        val privateKeyEntry = keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.PrivateKeyEntry ?: throw IllegalStateException("Key not found, call createPrivateKey first")
        val signature = Signature.getInstance(SIGNATURE_ALGORITHM)

        signature.initSign(privateKeyEntry.privateKey)
        signature.update(data)

        return signature.sign()
    }

    fun getPublicKey(): ECPublicKey {
        val publicKey = keyStore.getCertificate(KEY_ALIAS)?.publicKey ?: throw IllegalStateException("Key not found, call createPrivateKey first")

        return publicKey as ECPublicKey
    }

    fun getKeyAttestationPem(): ByteArray {
        val chain = keyStore.getCertificateChain(KEY_ALIAS) ?: throw IllegalStateException("No certificate chain for alias: $KEY_ALIAS")
        val x509Chain = chain.map { it as X509Certificate }

        val pem = x509Chain.joinToString(separator = "\n") { cert ->
            val base64 = Base64.encodeToString(cert.encoded, Base64.NO_WRAP)

            "-----BEGIN CERTIFICATE-----\n$base64\n-----END CERTIFICATE-----"
        }

        return pem.toByteArray(StandardCharsets.UTF_8)
    }

    // Only TEE-backed keys (Trusted Execution Environment)
    private fun assertKeyIsInsideSecureHardware() {
        val secretKey = keyStore.getKey(KEY_ALIAS, null) as PrivateKey
        val factory = KeyFactory.getInstance(secretKey.algorithm, "AndroidKeyStore")
        val keyInfo = factory.getKeySpec(secretKey, KeyInfo::class.java) as KeyInfo

        if (keyInfo.securityLevel != KeyProperties.SECURITY_LEVEL_TRUSTED_ENVIRONMENT) {
            throw InvalidKeyException("Only TEE keys are supported.")
        }
    }

    private fun randomPass(): ByteArray {
        val allowedChars = ('A'..'Z') + ('a'..'z') + ('0'..'9')
        val randomString = (1..5)
            .map { allowedChars.random(Random.Default) }
            .joinToString("")

        return randomString.toByteArray(StandardCharsets.UTF_8)
    }

    data class InitializeKeyStoreResponse(
        val createdNewKey: Boolean,
        val keyAttestationCertificateChain: ByteArray,
        val keyAttestationChallenge: String? = null,
    )
}