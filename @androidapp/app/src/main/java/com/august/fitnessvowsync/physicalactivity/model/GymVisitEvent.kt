package com.august.fitnessvowsync.physicalactivity.model

import java.nio.ByteBuffer
import java.security.MessageDigest
import java.time.Instant

data class GymVisitEvent(
    val location: Location,
    override val timestamp: Instant,
    val durationInMinutes: Int,
    val avgBpm: Int,
    val maxBpm: Int,
    val gymLocationId: String,
    override var syncDetails: SyncDetails? = null,
): PhysicalActivityEvent(timestamp, syncDetails) {
    data class Location(val latitudeNanoDegree: Long, val longitudeNanoDegree: Long) {
        constructor(latitude: Double, longitude: Double) : this((latitude * 1e7).toLong(), (longitude * 1e7).toLong())
    }
    companion object { const val GYM: Int = 0x17 }

    override fun abiEncodePacked(): ByteArray {
        // Match solidity: lat/lon as int64, remaining as uint32
        val buffer = ByteBuffer.allocate(4 + 8 + 8 + 4 + 4 + 4 + 4)
        buffer.putInt(GYM)
        buffer.putLong(location.latitudeNanoDegree)
        buffer.putLong(location.longitudeNanoDegree)
        buffer.putInt(timestamp.epochSecond.toInt())
        buffer.putInt(durationInMinutes)
        buffer.putInt(avgBpm)
        buffer.putInt(maxBpm)
        return buffer.array()
    }

    override fun sha256(): ByteArray = MessageDigest.getInstance("SHA-256").digest(abiEncodePacked())
}