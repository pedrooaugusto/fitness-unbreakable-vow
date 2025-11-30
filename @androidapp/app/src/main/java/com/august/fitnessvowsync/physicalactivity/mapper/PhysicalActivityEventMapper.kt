package com.august.fitnessvowsync.physicalactivity.mapper

import com.august.fitnessvowsync.contract.PhysicalActivityOracle
import com.august.fitnessvowsync.contract.SignatureMapper
import com.august.fitnessvowsync.physicalactivity.data.GymVisitTracker
import com.august.fitnessvowsync.physicalactivity.model.GymVisitEvent
import com.august.fitnessvowsync.physicalactivity.model.RunningEvent
import com.august.fitnessvowsync.physicalactivity.model.SleepEvent
import java.math.BigInteger
import java.time.Duration
import javax.inject.Inject

class PhysicalActivityEventMapper @Inject constructor(private val signatureMapper: SignatureMapper) {
    fun toGymVisitEvent(gymVisitSession: GymVisitTracker.GymVisitSession, avgBpm: Int, maxBpm: Int): GymVisitEvent {
        val location = GymVisitEvent.Location(gymVisitSession.gym.latitude, gymVisitSession.gym.longitude)
        val timestamp = gymVisitSession.startTime
        val endTime = gymVisitSession.endTime
        val durationInMinutes = Duration.between(timestamp, endTime).toMinutes().toInt().coerceAtMost(255)
        val gymLocationId = gymVisitSession.gym.id

        return GymVisitEvent(
            location,
            timestamp,
            durationInMinutes,
            avgBpm,
            maxBpm,
            gymLocationId,
            null,
        )
    }

    fun toGymVisitEvent(gymVisitEvent: GymVisitEvent, signatureBytes: ByteArray): PhysicalActivityOracle.GymVisitEvent {
        val signature = signatureMapper.toP256Signature(signatureBytes)
        val location = PhysicalActivityOracle.Location(
            BigInteger.valueOf(gymVisitEvent.location.latitudeNanoDegree),
            BigInteger.valueOf(gymVisitEvent.location.longitudeNanoDegree)
        )
        val timestamp = BigInteger.valueOf(gymVisitEvent.timestamp.epochSecond)
        val durationInMinutes = BigInteger.valueOf(gymVisitEvent.durationInMinutes.toLong())
        val avgBpm = BigInteger.valueOf(gymVisitEvent.avgBpm.toLong())
        val maxBpm = BigInteger.valueOf(gymVisitEvent.maxBpm.toLong())

        return PhysicalActivityOracle.GymVisitEvent(
            signature,
            location,
            timestamp,
            durationInMinutes,
            avgBpm,
            maxBpm
        )
    }

    fun toSleepEvent(sleepEvent: SleepEvent, signatureBytes: ByteArray): PhysicalActivityOracle.SleepEvent {
        val signature = signatureMapper.toP256Signature(signatureBytes)
        val timestamp = BigInteger.valueOf(sleepEvent.timestamp.epochSecond)
        val durationInMinutes = BigInteger.valueOf(sleepEvent.durationInMinutes.toLong())
        val avgBpm = BigInteger.valueOf(sleepEvent.avgBpm.toLong())

        return PhysicalActivityOracle.SleepEvent(
            signature,
            timestamp,
            durationInMinutes,
            avgBpm,
        )
    }

    fun toRunningEvent(runningEvent: RunningEvent, signatureBytes: ByteArray): PhysicalActivityOracle.RunningEvent {
        val signature = signatureMapper.toP256Signature(signatureBytes)
        val timestamp = BigInteger.valueOf(runningEvent.timestamp.epochSecond)
        val distanceInMeters = BigInteger.valueOf(runningEvent.distanceInMeters.toLong())
        val paceInSecondsPerKm = BigInteger.valueOf(runningEvent.paceInSecondsPerKm.toLong())
        val avgBpm = BigInteger.valueOf(runningEvent.avgBpm.toLong())

        return PhysicalActivityOracle.RunningEvent(
            signature,
            timestamp,
            distanceInMeters,
            paceInSecondsPerKm,
            avgBpm,
        )
    }
}