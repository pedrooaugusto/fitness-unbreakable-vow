package com.august.fitnessvowsync.ui.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import com.august.fitnessvowsync.donotuse.FakeDataProducerDoNotUse
import com.august.fitnessvowsync.model.ContractPhase
import com.august.fitnessvowsync.model.PhysicalActivityRecord
import com.august.fitnessvowsync.model.SyncedPhysicalActivityRecord
import com.august.fitnessvowsync.service.GymVisitService
import com.august.fitnessvowsync.service.PhysicalActivityOracleService
import com.august.fitnessvowsync.service.SyncPhysicalActivityRecordService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import java.time.Duration
import java.time.Instant

open class DefaultMainScreenViewModel (
    private val oracleService: PhysicalActivityOracleService,
    private val syncPhysicalActivityService: SyncPhysicalActivityRecordService,
    private val gymVisitService: GymVisitService,
    private val donNotUse: FakeDataProducerDoNotUse?,
): ViewModel(), MainScreenViewModel {
    private val _uiState = MutableStateFlow(MainUiState())
    override val uiState: StateFlow<MainUiState> = _uiState

    override suspend fun loadDetails() {
        try {
            _uiState.update { it.copy(isFetchingData = true) }

            val contractOverview = getContractOverview()
            val nextRecordToSync = syncPhysicalActivityService.getCurrentPhysicalActivityRecord()
            val syncedRecordsList = syncPhysicalActivityService.getSyncedPhysicalActivityRecords()

            _uiState.update { it.copy(
                contractOverview = contractOverview,
                nextRecordToSync = nextRecordToSync,
                syncedRecordsList = syncedRecordsList,
            ) }
        } catch (ex: Exception) {
            showErrorMessage("Fetch data error: ${(ex.message ?: "").take(40)}", ex)
        } finally {
            _uiState.update { it.copy(isFetchingData = false) }
        }
    }

    override suspend fun syncRecord() {
        try {
            _uiState.update { it.copy(isSyncingRecord = true) }

            val syncedRecord = syncPhysicalActivityService.syncCurrentPhysicalActivityRecord()
            val syncedRecordsList = syncPhysicalActivityService.getSyncedPhysicalActivityRecords()

            _uiState.update { it.copy(syncedRecord = syncedRecord, syncedRecordsList = syncedRecordsList) }
        } catch (ex: Exception) {
            showErrorMessage("Sync record error: ${(ex.message ?: "").take(40)}", ex)
        } finally {
            _uiState.update { it.copy(isSyncingRecord = false) }
        }
    }

    override suspend fun refreshScreen() {
        _uiState.update { it.copy(isRefreshingScreen = true) }

        loadDetails()

        _uiState.update { it.copy(isRefreshingScreen = false) }
    }

    override fun eraseSyncedRecord() {
        _uiState.update { it.copy(syncedRecord = null) }
    }

    override fun dismissErrorMessage() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    override fun setupGymGeofence(context: Context) {
        try {
            gymVisitService.setupGymGeofence(context)
        } catch (ex: Exception) {
            showErrorMessage("Failed to setup geofence.", ex)
        }
    }

    //TODO: Remove this
    override suspend fun __debugPleaseRemove__randomValueFor(goal: String): Unit {
        when (goal) {
            "run" -> donNotUse?.addFakeRunningSession(((_uiState.value.nextRecordToSync?.runDistanceMeters ?: 0) + 500).toLong())
            "sleep" -> donNotUse?.addFakeSleepSession((60).toLong())
            "gym" -> donNotUse?.addFakeGymVisit()
        }

        val nextRecordToSync = syncPhysicalActivityService.getCurrentPhysicalActivityRecord()

        _uiState.update { it.copy(nextRecordToSync = nextRecordToSync) }
    }

    private fun showErrorMessage(message: String, ex: Exception) {
        _uiState.update { it.copy(errorMessage = message) }

        Log.e("FitVow", "Unexpected error: ", ex)
    }

    private suspend fun getContractOverview(): ContractOverview {
        val creationDate = Instant.ofEpochSecond(oracleService.getCreationDate().toLong())
        val expirationDate = Instant.ofEpochSecond(oracleService.getExpirationDate().toLong())
        val currentWeekStartEndPair = oracleService.getCurrentWeekStartAndEnd()
        val currentWeek = Week(oracleService.getCurrentWeekIndex().toInt(), currentWeekStartEndPair.first, currentWeekStartEndPair.second)
        val secondsInOneWeek = Duration.ofSeconds(oracleService.getSecondsInWeek().toLong())
        val phase = oracleService.getContractPhase()

        return ContractOverview(creationDate, expirationDate, currentWeek, secondsInOneWeek, phase)
    }
}

interface MainScreenViewModel {
    val uiState: StateFlow<MainUiState>

    suspend fun loadDetails()

    suspend fun syncRecord()

    suspend fun refreshScreen()

    fun eraseSyncedRecord()

    fun dismissErrorMessage()

    fun setupGymGeofence(context: Context)

    //TODO: Remove this
    suspend fun __debugPleaseRemove__randomValueFor(goal: String): Unit
}

class PreviewMainScreenViewModel: MainScreenViewModel {
    override val uiState: StateFlow<MainUiState> = MutableStateFlow(MainUiState())

    override suspend fun loadDetails() { TODO("Not yet implemented") }

    override suspend fun syncRecord() { TODO("Not yet implemented") }

    override suspend fun refreshScreen() { TODO("Not yet implemented") }

    override fun eraseSyncedRecord() { TODO("Not yet implemented") }

    override fun dismissErrorMessage() { TODO("Not yet implemented") }

    override fun setupGymGeofence(context: Context) { TODO("Not yet implemented") }

    //TODO: Remove this
    override suspend fun __debugPleaseRemove__randomValueFor(goal: String): Unit { TODO("todo") }
}

data class MainUiState (
    val contractOverview: ContractOverview? = null,
    val nextRecordToSync: PhysicalActivityRecord? = null,
    val syncedRecord: SyncedPhysicalActivityRecord? = null,
    val syncedRecordsList: List<SyncedPhysicalActivityRecord> = emptyList(),
    val errorMessage: String? = null,
    val isFetchingData: Boolean = true,
    val isSyncingRecord: Boolean = false,
    val isRefreshingScreen: Boolean = false,
)

data class Week (
    val index: Int,
    val start: Instant,
    val end: Instant,
)

data class ContractOverview (
    val creationDate: Instant,
    val expirationDate: Instant,
    val currentWeek: Week,
    val secondsInOneWeek: Duration,
    val phase: ContractPhase,
)