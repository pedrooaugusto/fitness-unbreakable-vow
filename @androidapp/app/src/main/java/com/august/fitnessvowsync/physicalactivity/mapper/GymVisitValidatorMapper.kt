package com.august.fitnessvowsync.physicalactivity.mapper

import com.august.fitnessvowsync.contract.PhysicalActivityOracle
import com.august.fitnessvowsync.physicalactivity.model.TrackedGymConfig
import java.time.Duration
import javax.inject.Inject

class GymVisitValidatorMapper @Inject constructor() {
    fun toTrackedGyms(validator: PhysicalActivityOracle.GymVisitEventValidator): List<TrackedGymConfig> {
        val gymLocations = mutableMapOf<String, PhysicalActivityOracle.Geofence>(
            "Meier SM" to validator.gymLoc1,
            "Cachambi SM" to validator.gymLoc2,
            "Sandton PL" to validator.gymLoc3,
            "Sandton VA" to validator.gymLoc4,
        )

        return gymLocations.map {gym -> TrackedGymConfig(
            id = gym.key,
            latitude = gym.value.latitudeNanoDegree.toDouble() / 1e7,
            longitude = gym.value.longitudeNanoDegree.toDouble() / 1e7f,
            radius = gym.value.radiusInMeters.toDouble(),
            minimumPermanence = Duration.ofMinutes(validator.minimumVisitTimeInMinutes.toLong()),
        )}
    }
}