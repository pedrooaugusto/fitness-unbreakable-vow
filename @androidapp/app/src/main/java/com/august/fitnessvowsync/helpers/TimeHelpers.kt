package com.august.fitnessvowsync.helpers

import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit

class TimeHelpers {
    companion object {
        @JvmStatic
        fun formatTimeRemaining(endInstant: Instant): String {
            val now = Instant.now()
            val duration = Duration.between(now, endInstant)

            if (duration.isNegative) return ""

            val days = duration.toDays()
            val hours = duration.minus(days, ChronoUnit.DAYS).toHours()
            val minutes = duration
                .minus(days, ChronoUnit.DAYS)
                .minus(hours, ChronoUnit.HOURS)
                .toMinutes()
            val seconds = duration
                .minus(days, ChronoUnit.DAYS)
                .minus(hours, ChronoUnit.HOURS)
                .minus(minutes, ChronoUnit.MINUTES)
                .toSeconds()

            val parts = mutableListOf<String>()
            if (days > 0) parts += "${days}d"
            if (hours > 0) parts += "${hours}h"
            if (minutes > 0) parts += "${minutes}min"
            if (days == 0L && hours == 0L && minutes <= 3L && seconds > 0L) parts += "${seconds}sec"

            if (parts.isEmpty()) parts += "0sec"

            return parts.joinToString(" ")
        }
    }
}