package com.august.fitnessvowsync.geofencing

import java.time.Duration

enum class GymConfig (
    val id: String,
    val latitude: Double,
    val longitude: Double,
    val minimumPermanence: Duration,
) {
    PRIMARY("ALE_TOP", -22.896957745611775, -43.27265899080379, Duration.ofMinutes(10)),
    SECONDARY("SMART_FIT", -22.893444688409225, -43.29257247192793, Duration.ofMinutes(10))
}