package com.august.fitnessvowsync.geofencing

import java.time.Duration

enum class GymConfig (
    val id: String,
    val latitude: Double,
    val longitude: Double,
    val requiredMinimumPermanence: Duration,
    val requiredMaximumHeartRate: Int, // No average??? Fraco!!
) {
    PRIMARY("ALE_TOP", -22.896957745611775, -43.27265899080379, Duration.ofSeconds(20))
}