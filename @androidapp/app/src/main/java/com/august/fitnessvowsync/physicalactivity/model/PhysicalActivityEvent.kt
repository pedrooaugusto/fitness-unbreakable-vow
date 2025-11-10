package com.august.fitnessvowsync.physicalactivity.model

import java.time.Instant

abstract class PhysicalActivityEvent (
    @Transient open val timestamp: Instant,
    @Transient open val syncDetails: SyncDetails? = null,
) {
    data class SyncDetails(val transactionHash: String, val timestamp: Instant, val network: String, val weekIndex: Int)
    abstract fun abiEncodePacked(): ByteArray
    abstract fun sha256(): ByteArray
}
