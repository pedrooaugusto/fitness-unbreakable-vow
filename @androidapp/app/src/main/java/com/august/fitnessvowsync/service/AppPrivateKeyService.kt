package com.august.fitnessvowsync.service

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyInfo
import android.security.keystore.KeyProperties
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.Signature
import android.util.Base64
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.math.BigInteger
import java.security.AlgorithmParameters
import java.security.InvalidKeyException
import java.security.KeyFactory
import java.security.PrivateKey
import java.security.PublicKey
import java.security.Security
import java.security.interfaces.ECPublicKey
import java.security.spec.ECGenParameterSpec
import java.security.spec.ECParameterSpec
import java.security.spec.ECPoint
import java.security.spec.ECPublicKeySpec
import java.security.spec.X509EncodedKeySpec
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.inject.Inject

class AppPrivateKeyService @Inject constructor(private val keyStore: KeyStore) {
    private companion object { private const val KEY_ALIAS = "Physical_Activity_Oracle_Client" }

    fun createIfNotExists() {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) Security.addProvider(BouncyCastleProvider())

        if (keyStore.containsAlias(KEY_ALIAS)) return

        val keyPairGenerator = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC, keyStore.provider)
        val parameterSpec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY
        )
            .setUserAuthenticationRequired(false)
            .setAlgorithmParameterSpec(ECGenParameterSpec("secp256r1")) // Equivalent to P-256
            .setDigests(KeyProperties.DIGEST_SHA256)
            .build()

        keyPairGenerator.initialize(parameterSpec)
        keyPairGenerator.generateKeyPair()

        // POLICE!!
        assertKeyIsInsideSecureHardware()
    }

    fun sign(data: ByteArray): String {
        val privateKeyEntry = keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.PrivateKeyEntry ?: throw IllegalStateException("Key not found, call createPrivateKey first")
        val signature = Signature.getInstance("SHA256withECDSA")

        signature.initSign(privateKeyEntry.privateKey)
        signature.update(data)

        return Base64.encodeToString(derToP1363(signature.sign()), Base64.NO_WRAP)
    }

    fun getPublicKey(): String {
        val publicKey = keyStore.getCertificate(KEY_ALIAS)?.publicKey ?: throw IllegalStateException("Key not found, call createPrivateKey first")

        return Base64.encodeToString(publicKey.rawEncoding(), Base64.NO_WRAP)
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

    private fun PublicKey.rawEncoding(): ByteArray {
        val ecPublicKey = this as ECPublicKey

        val x = ecPublicKey.w.affineX.toByteArray()
        val y = ecPublicKey.w.affineY.toByteArray()

        // Pad to 32 bytes if needed
        val x32 = ByteArray(32) { i -> if (i < 32 - x.size) 0 else x[i - (32 - x.size)] }
        val y32 = ByteArray(32) { i -> if (i < 32 - y.size) 0 else y[i - (32 - y.size)] }

        // Concatenate 0x04 + x32 + y32
        return byteArrayOf(0x04) + x32 + y32
    }

    private fun derToP1363(signature: ByteArray): ByteArray {
        val seq = org.bouncycastle.asn1.ASN1Sequence.getInstance(signature)
        val r = (seq.getObjectAt(0) as org.bouncycastle.asn1.ASN1Integer).positiveValue.toByteArray()
        val s = (seq.getObjectAt(1) as org.bouncycastle.asn1.ASN1Integer).positiveValue.toByteArray()

        // Pad with leading zeros to make 32 bytes each (for P-256)
        val rPadded = ByteArray(32)
        val sPadded = ByteArray(32)
        System.arraycopy(r, Math.max(0, r.size - 32), rPadded, 32 - Math.min(32, r.size), Math.min(32, r.size))
        System.arraycopy(s, Math.max(0, s.size - 32), sPadded, 32 - Math.min(32, s.size), Math.min(32, s.size))

        return rPadded + sPadded
    }
}
