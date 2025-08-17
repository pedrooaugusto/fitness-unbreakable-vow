package com.august.fitnessvowsync.helpers

import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.time.TimeRangeFilter
import java.time.Duration
import java.time.Instant
import java.time.Period

class TimeRange {
    companion object {
        @JvmStatic
        fun lastSevenDays(): TimeRangeFilter {
            return last(Period.ofDays(7))
        }

        @JvmStatic
        fun oneWeekAgo(): Instant {
            return Instant.now().minus(Duration.ofDays(7))
        }

        @JvmStatic
        fun last(period: Period): TimeRangeFilter {
            val now = Instant.now()
            val someTimeAgo = now.minus(period)

            return TimeRangeFilter.between(someTimeAgo, now)
        }

        @JvmStatic
        fun ofRecord(record: ExerciseSessionRecord): TimeRangeFilter {
            return TimeRangeFilter.between(record.startTime, record.endTime)
        }

        @JvmStatic
        fun between(startTime: Long, endTime: Long): TimeRangeFilter {
            return TimeRangeFilter.between(Instant.ofEpochMilli(startTime), Instant.ofEpochMilli(endTime))
        }
    }
}