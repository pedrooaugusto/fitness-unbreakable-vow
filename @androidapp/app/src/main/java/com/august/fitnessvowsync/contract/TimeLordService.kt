package com.august.fitnessvowsync.contract

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.math.BigInteger
import java.time.Instant
import javax.inject.Inject

class TimeLordService @Inject constructor(
    private val timeLord: ContractProvider<TheDoctor>
) {
    enum class ContractPhase(val phase: Int) {
        Active(0),
        Grace(1),
        FullyExpired(2)
    }

    suspend fun getCreationDate(): BigInteger {
        return withContext(Dispatchers.IO) {
            timeLord.get().CREATION_DATE().send()
        }
    }

    suspend fun getExpirationDate(): BigInteger {
        return withContext(Dispatchers.IO) {
            timeLord.get().EXPIRATION_DATE().send()
        }
    }

    suspend fun getCurrentWeekIndex(): BigInteger {
        return withContext(Dispatchers.IO) {
            timeLord.get().currentWeekIndex.send()
        }
    }

    suspend fun getSecondsInWeek(): BigInteger {
        return withContext(Dispatchers.IO) {
            timeLord.get().SECONDS_IN_ONE_WEEK().send()
        }
    }

    suspend fun getContractPhase(): ContractPhase {
        return withContext(Dispatchers.IO) {
            val phase = timeLord.get().contractPhase.send().toInt()

            ContractPhase.entries.firstOrNull { it.phase == phase } as ContractPhase
        }
    }

    suspend fun getCurrentWeekStartAndEnd(): Pair<Instant, Instant> {
        val secondsInWeek = getSecondsInWeek()
        val creationDate = getCreationDate()
        val currentWeekIndex = getCurrentWeekIndex()

        val currentWeekStartDate = creationDate + currentWeekIndex * secondsInWeek
        val currentWeekEndDate = currentWeekStartDate + secondsInWeek

        return Pair(Instant.ofEpochSecond(currentWeekStartDate.toLong()), Instant.ofEpochSecond(currentWeekEndDate.toLong()))
    }
}