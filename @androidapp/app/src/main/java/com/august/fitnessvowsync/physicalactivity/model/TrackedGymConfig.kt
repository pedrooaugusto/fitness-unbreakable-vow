package com.august.fitnessvowsync.physicalactivity.model

import java.time.Duration

data class TrackedGymConfig(
    val id: String,
    val latitude: Double,
    val longitude: Double,
    val minimumPermanence: Duration,
    val radius: Double
)