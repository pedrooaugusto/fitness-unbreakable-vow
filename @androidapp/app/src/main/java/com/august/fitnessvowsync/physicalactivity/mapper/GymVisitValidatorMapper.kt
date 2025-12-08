package com.august.fitnessvowsync.physicalactivity.mapper

import com.august.fitnessvowsync.contract.PhysicalActivityOracle
import com.august.fitnessvowsync.physicalactivity.model.TrackedGymConfig
import java.time.Duration
import javax.inject.Inject

class GymVisitValidatorMapper @Inject constructor() {
    fun toTrackedGyms(validator: PhysicalActivityOracle.GymVisitEventValidator): List<TrackedGymConfig> {
        val gymLocations = mutableMapOf<String, PhysicalActivityOracle.Geofence>(
            "Meier SM" to validator.meierSF,
            "Cachambi SM" to validator.cachambiSF,
            "Sandton PL" to validator.sandtonPL,
            "Sandton VA" to validator.sandtonVA,
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