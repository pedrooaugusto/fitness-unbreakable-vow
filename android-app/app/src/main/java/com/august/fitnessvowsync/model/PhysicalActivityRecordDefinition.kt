package com.august.fitnessvowsync.model

import java.math.BigInteger

interface PhysicalActivityRecordDefinition {
    val timestamp: BigInteger
    val runDistanceMeters: Int
    val healthySleepNights: Int
    val gymVisits: Int
}

data class AddPhysicalActivityRecordRequest(
    override val timestamp: BigInteger = BigInteger.ZERO,
    override val runDistanceMeters: Int = 0,
    override val healthySleepNights: Int = 0,
    override val gymVisits: Int = 0
): PhysicalActivityRecordDefinition

data class GetPhysicalActivityRecordResponse (
    override val timestamp: BigInteger,
    override val runDistanceMeters: Int,
    override val healthySleepNights: Int,
    override val gymVisits: Int,
    val weekNumber: BigInteger
): PhysicalActivityRecordDefinition

data class AddPhysicalActivityRecordTransaction (
    override val timestamp: BigInteger = BigInteger.ZERO,
    override val runDistanceMeters: Int = 0,
    override val healthySleepNights: Int = 0,
    override val gymVisits: Int = 0,
    val transactionHash: String = "",
    val blockExplorerUrl: String = ""
): PhysicalActivityRecordDefinition