package com.august.fitnessvowsync.physicalactivity.mapper

import com.august.fitnessvowsync.contract.PhysicalActivityOracle
import com.august.fitnessvowsync.physicalactivity.model.TrackedGymConfig
import java.time.Duration
import javax.inject.Inject

class GymVisitValidatorMapper @Inject constructor() {
    fun toTrackedGyms(validator: PhysicalActivityOracle.GymVisitEventValidator): List<TrackedGymConfig> {
        val gymLocations = mutableListOf<PhysicalActivityOracle.Geofence>(validator.gym1Location, validator.gym2Location, validator.gym3Location)

        return gymLocations.mapIndexed {index, gym -> TrackedGymConfig(
            id = "GYM_#$index",
            latitude = gym.latitudeNanoDegree.toDouble() / 1e7,
            longitude = gym.longitudeNanoDegree.toDouble() / 1e7f,
            radius = gym.radiusInMeters.toDouble(),
            minimumPermanence = Duration.ofMinutes(validator.minimumVisitTimeInMinutes.toLong()),
        )}
    }
}