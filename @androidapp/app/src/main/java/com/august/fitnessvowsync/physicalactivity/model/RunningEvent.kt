package com.august.fitnessvowsync.physicalactivity.model

import java.nio.ByteBuffer
import java.security.MessageDigest
import java.time.Instant

data class RunningEvent(
    override val timestamp: Instant,
    val distanceInMeters: Int,
    val paceInSecondsPerKm: Int,
    val avgBpm: Int,
    override var syncDetails: SyncDetails? = null,
): PhysicalActivityEvent(timestamp, syncDetails) {
    companion object { const val RUNNING: Int = 0x11 }

    override fun abiEncodePacked(): ByteArray {
        // Match solidity: all packed as uint32
        val buffer = ByteBuffer.allocate(4 + 4 + 4 + 4 + 4)
        buffer.putInt(RUNNING)
        buffer.putInt(timestamp.epochSecond.toInt())
        buffer.putInt(distanceInMeters)
        buffer.putInt(paceInSecondsPerKm)
        buffer.putInt(avgBpm)
        return buffer.array()
    }

    override fun sha256(): ByteArray = MessageDigest.getInstance("SHA-256").digest(abiEncodePacked())
}
