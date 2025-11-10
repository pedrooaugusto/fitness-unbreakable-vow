package com.august.fitnessvowsync.physicalactivity.model

import java.nio.ByteBuffer
import java.security.MessageDigest
import java.time.Instant

data class SleepEvent(
    override val timestamp: Instant,
    val durationInMinutes: Int,
    val avgBpm: Int,
    override var syncDetails: SyncDetails? = null,
): PhysicalActivityEvent(timestamp, syncDetails) {
    companion object { const val SLEEP: Int = 0x13 }

    override fun abiEncodePacked(): ByteArray {
        // Match solidity: uint32 for all fields in encodePacked
        val buffer = ByteBuffer.allocate(4 + 4 + 4 + 4)
        buffer.putInt(SLEEP)
        buffer.putInt(timestamp.epochSecond.toInt())
        buffer.putInt(durationInMinutes)
        buffer.putInt(avgBpm)
        return buffer.array()
    }

    override fun sha256(): ByteArray = MessageDigest.getInstance("SHA-256").digest(abiEncodePacked())
}
