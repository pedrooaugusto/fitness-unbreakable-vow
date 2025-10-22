package com.august.fitnessvowsync.mapper

import android.util.Base64
import com.august.fitnessvowsync.contract.PhysicalActivityOracle
import com.august.fitnessvowsync.service.InterPlanetaryFileSystemService
import org.web3j.tuples.generated.Tuple2
import java.math.BigInteger
import java.security.interfaces.ECPublicKey
import javax.inject.Inject

class OracleP256SignatureMapper @Inject constructor() {
    fun toP256PublicKey(key: ECPublicKey): PhysicalActivityOracle.P256PublicKey {
        val x = key.w.affineX.toByteArray()
        val y = key.w.affineY.toByteArray()

        // Pad to 32 bytes if needed
        val x32 = ByteArray(32) { i -> if (i < 32 - x.size) 0 else x[i - (32 - x.size)] }
        val y32 = ByteArray(32) { i -> if (i < 32 - y.size) 0 else y[i - (32 - y.size)] }

        return PhysicalActivityOracle.P256PublicKey(x32, y32)
    }

    fun toP256PublicKey(key: Tuple2<ByteArray, ByteArray>): PhysicalActivityOracle.P256PublicKey {
        return PhysicalActivityOracle.P256PublicKey(key.component1(), key.component2())
    }

    fun toP256Signature(signature: ByteArray): PhysicalActivityOracle.P256Signature {
        val seq = org.bouncycastle.asn1.ASN1Sequence.getInstance(signature)
        val r = (seq.getObjectAt(0) as org.bouncycastle.asn1.ASN1Integer).positiveValue.toByteArray()
        val s = (seq.getObjectAt(1) as org.bouncycastle.asn1.ASN1Integer).positiveValue.toByteArray()

        // Pad with leading zeros to make 32 bytes each (for P-256)
        val rPadded = ByteArray(32)
        val sPadded = ByteArray(32)

        System.arraycopy(r, Math.max(0, r.size - 32), rPadded, 32 - Math.min(32, r.size), Math.min(32, r.size))
        System.arraycopy(s, Math.max(0, s.size - 32), sPadded, 32 - Math.min(32, s.size), Math.min(32, s.size))

        return PhysicalActivityOracle.P256Signature(rPadded, normalizeLowS(sPadded))
    }

    fun toXYBase64(key: ECPublicKey): String {
        val oracleKey = toP256PublicKey(key)

        return Base64.encodeToString(oracleKey.x + oracleKey.y, Base64.NO_WRAP)
    }

    fun toAndroidKeyAttestation(ipfsUploadResponse: InterPlanetaryFileSystemService.UploadResponse, attestationChallenge: String): PhysicalActivityOracle.AndroidKeyAttestation {
        return PhysicalActivityOracle.AndroidKeyAttestation(
            ipfsUploadResponse.sha256,
            attestationChallenge,
            ipfsUploadResponse.cid
        )
    }

    private object P256 {
        val N: BigInteger = BigInteger("FFFFFFFF00000000FFFFFFFFFFFFFFFFBCE6FAADA7179E84F3B9CAC2FC632551", 16)
        val HALF_N: BigInteger = N.shiftRight(1)
    }

    private fun normalizeLowS(s: ByteArray): ByteArray {
        val sBig = BigInteger(1, s) // unsigned big-endian
        val normalized = if (sBig > P256.HALF_N) P256.N.subtract(sBig) else sBig

        return toUnsignedFixed(normalized, 32)
    }

    private fun toUnsignedFixed(x: BigInteger, size: Int): ByteArray {
        require(x.signum() >= 0) { "Value must be non-negative" }

        val twosComp = x.toByteArray() // may contain leading 0x00

        val unsigned = if (twosComp.size > 1 && twosComp[0] == 0.toByte()) {
            twosComp.copyOfRange(1, twosComp.size)
        } else {
            twosComp
        }

        require(unsigned.size <= size) { "Value does not fit in $size bytes" }

        val out = ByteArray(size)

        System.arraycopy(unsigned, 0, out, size - unsigned.size, unsigned.size)

        return out
    }
}