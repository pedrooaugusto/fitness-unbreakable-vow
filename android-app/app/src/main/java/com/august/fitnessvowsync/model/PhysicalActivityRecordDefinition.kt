package com.august.fitnessvowsync.model

import java.math.BigInteger
import java.time.Instant

interface PhysicalActivityRecord {
    val timestamp: Instant
    val runDistanceMeters: Int
    val healthySleepNights: Int
    val gymVisits: Int
}

data class PhysicalActivityRecordImpl (
    override val timestamp: Instant = Instant.MIN,
    override val runDistanceMeters: Int = 0,
    override val healthySleepNights: Int = 0,
    override val gymVisits: Int = 0,
): PhysicalActivityRecord

data class SyncedPhysicalActivityRecord (
    override val timestamp: Instant,
    override val runDistanceMeters: Int,
    override val healthySleepNights: Int,
    override val gymVisits: Int,
    val transaction: String,
    val transactionUrl: String,
    val weekIndex: Int,
): PhysicalActivityRecord