package com.august.fitnessvowsync.testing

import com.august.fitnessvowsync.geofencing.GymConfig
import java.time.Duration
import java.time.Instant
import kotlin.math.roundToLong
import kotlin.random.Random

data class PeriodSpec(
    val start: Instant,
    val end: Instant,
    val runningSessions: List<RunSpec>,
    val sleepSessions: List<SleepSpec>,
    val gymVisits: List<GymSpec>,
)

data class RunSpec(val start: Instant, val end: Instant, val distanceMeters: Int, val paceInSecondsPerKm: Int, val avgBpm: Int)
data class SleepSpec(val start: Instant, val end: Instant, val avgBpm: Int)
data class GymSpec(val start: Instant, val end: Instant, val avgBpm: Int, val maxBpm: Int, val gym: GymConfig)

object RandomPhysicalActivityGenerator {
    fun generate(base: Instant, count: Int, seed: Long? = null): List<PeriodSpec> {
        val rnd = if (seed != null) Random(seed) else Random.Default
        val periods = mutableListOf<PeriodSpec>()

        var cursor = base
        repeat(count) { _ ->
            val periodStart = cursor
            val periodEnd = periodStart.plus(Duration.ofHours(3)).plus(Duration.ofMinutes(5))

            // 1) Running block: 60 minutes
            val runsStart = periodStart
            val runsEnd = runsStart.plus(Duration.ofMinutes(60))
            val runs = addRunningSessions(start = runsStart, end = runsEnd, rnd = rnd)

            // 2) Sleep block: 60 minutes, ensure multiple sleep sessions don't merge in tests
            val sleepsStart = runsEnd.plus(Duration.ofMinutes(1))
            val sleepsEnd = sleepsStart.plus(Duration.ofMinutes(60))
            val sleeps = addSleepSessions(start = sleepsStart, end = sleepsEnd, rnd = rnd)

            // 3) Gym block: 60 minutes
            val gymsStart = sleepsEnd.plus(Duration.ofMinutes(1))
            val gymsEnd = gymsStart.plus(Duration.ofMinutes(60))
            val gyms = addGymVisits(start = gymsStart, end = gymsEnd, rnd = rnd)

            periods += PeriodSpec(periodStart, periodEnd, runs, sleeps, gyms)

            cursor = periodEnd.plus(Duration.ofSeconds(60)) // small gap between periods
        }

        return periods
    }

    private fun addRunningSessions(start: Instant, end: Instant, rnd: Random): List<RunSpec> {
        val totalMin = Duration.between(start, end).toMinutes().toInt()
        val sepMin = 2
        val minDur = 10
        if (totalMin < minDur) return emptyList()

        val maxSessions = ((totalMin + sepMin) / (minDur + sepMin)).coerceAtLeast(0)
        val n = if (maxSessions == 0) 0 else rnd.nextInt(0, maxSessions + 1)
        if (n == 0) return emptyList()

        val availableForSessions = totalMin - sepMin * (n - 1)
        val baseLen = (availableForSessions / n).coerceAtLeast(minDur)
        var remainder = (availableForSessions - baseLen * n).coerceAtLeast(0)

        val sessions = mutableListOf<RunSpec>()
        var s = start
        repeat(n) { i ->
            val thisLen = baseLen + if (remainder > 0) { remainder--; 1 } else 0
            val e = s.plus(Duration.ofMinutes(thisLen.toLong()))

            // Derive distance from duration and a plausible pace (3.5–8.0 min/km)
            val paceSecPerKm = rnd.nextInt(210, 480)
            val durationSec = Duration.between(s, e).seconds
            val distance = ((durationSec.toDouble() / paceSecPerKm) * 1000.0).toInt().coerceAtLeast(1)
            val bpm = rnd.nextInt(92, 125)
            val expectedPace = calcExpectedPaceSecondsPerKm(s, e, distance)
            sessions += RunSpec(s, e, distance, expectedPace, bpm)

            s = if (i == n - 1) e else e.plus(Duration.ofMinutes(sepMin.toLong()))
        }

        return sessions
    }

    private fun addSleepSessions(start: Instant, end: Instant, rnd: Random): List<SleepSpec> {
        val totalMin = Duration.between(start, end).toMinutes().toInt()
        val sepMin = 2
        val minDur = 10
        if (totalMin < minDur) return emptyList()

        val maxSessions = ((totalMin + sepMin) / (minDur + sepMin)).coerceAtLeast(0)
        val n = if (maxSessions == 0) 0 else rnd.nextInt(0, maxSessions + 1)
        if (n == 0) return emptyList()

        val availableForSessions = totalMin - sepMin * (n - 1)
        val baseLen = (availableForSessions / n).coerceAtLeast(minDur)
        var remainder = (availableForSessions - baseLen * n).coerceAtLeast(0)

        val sessions = mutableListOf<SleepSpec>()
        var s = start
        repeat(n) { i ->
            val thisLen = baseLen + if (remainder > 0) { remainder--; 1 } else 0
            val e = s.plus(Duration.ofMinutes(thisLen.toLong()))
            val bpm = rnd.nextInt(44, 72)
            sessions += SleepSpec(s, e, bpm)
            s = if (i == n - 1) e else e.plus(Duration.ofMinutes(sepMin.toLong()))
        }

        return sessions
    }

    private fun addGymVisits(start: Instant, end: Instant, rnd: Random): List<GymSpec> {
        val totalMin = Duration.between(start, end).toMinutes().toInt()
        val sepMin = 2
        val minDur = 5
        if (totalMin < minDur) return emptyList()

        val maxSessions = ((totalMin + sepMin) / (minDur + sepMin)).coerceAtLeast(0)
        val n = if (maxSessions == 0) 0 else rnd.nextInt(0, maxSessions + 1)
        if (n == 0) return emptyList()

        val availableForSessions = totalMin - sepMin * (n - 1)
        val baseLen = (availableForSessions / n).coerceAtLeast(minDur)
        var remainder = (availableForSessions - baseLen * n).coerceAtLeast(0)

        val sessions = mutableListOf<GymSpec>()
        var s = start
        repeat(n) { i ->
            val thisLen = baseLen + if (remainder > 0) { remainder--; 1 } else 0
            val e = s.plus(Duration.ofMinutes(thisLen.toLong()))
            val bpm = rnd.nextInt(100, 150)
            val gymCfg = if (rnd.nextInt(0, 2) == 0) GymConfig.PRIMARY else GymConfig.SECONDARY
            sessions += GymSpec(s, e, bpm, bpm, gymCfg)
            s = if (i == n - 1) e else e.plus(Duration.ofMinutes(sepMin.toLong()))
        }

        return sessions
    }
}

fun calcExpectedPaceSecondsPerKm(start: Instant, end: Instant, distanceMeters: Int): Int {
    val durationSec = Duration.between(start, end).seconds.toDouble()
    val pace = (durationSec / (distanceMeters / 1000.0)).roundToLong()
    return pace.toInt()
}
