package com.august.fitnessvowsync.ui.viewmodel

import android.Manifest
import android.content.Context
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.lifecycle.ViewModel
import com.august.fitnessvowsync.BuildConfig
import com.august.fitnessvowsync.geofencing.GymGeofenceCreator
import com.august.fitnessvowsync.contract.PhysicalActivityOracleService
import com.august.fitnessvowsync.geofencing.GymConfig
import com.august.fitnessvowsync.helpers.SettingsService
import com.august.fitnessvowsync.physicalactivity.collection.PhysicalActivityEventCollector
import com.august.fitnessvowsync.physicalactivity.collection.PhysicalActivityEventPublisher
import com.august.fitnessvowsync.physicalactivity.data.PhysicalActivityEventRepository
import com.august.fitnessvowsync.physicalactivity.model.GymVisitEvent
import com.august.fitnessvowsync.physicalactivity.model.PhysicalActivityEvent
import com.august.fitnessvowsync.physicalactivity.model.PhysicalActivityEvents
import com.august.fitnessvowsync.physicalactivity.model.RunningEvent
import com.august.fitnessvowsync.physicalactivity.model.SleepEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import java.time.Duration
import java.time.Instant

open class DefaultMainScreenViewModel (
    private val oracleService: PhysicalActivityOracleService,
    private val physicalActivityCollector: PhysicalActivityEventCollector,
    private val physicalActivityRepository: PhysicalActivityEventRepository,
    private val physicalActivityPublisher: PhysicalActivityEventPublisher,
    private val gymGeofenceCreator: GymGeofenceCreator,
    private val settingsService: SettingsService,
): ViewModel(), MainScreenViewModel {
    private val _uiState = MutableStateFlow(MainUiState())
    override val uiState: StateFlow<MainUiState> = _uiState

    override suspend fun loadDetails() {
        try {
            _uiState.update { it.copy(isFetchingData = true) }

            val contractOverview = getContractOverview()
            val (_, periodStart, periodEnd) = contractOverview.currentWeek
            val thisWeekPhysicalActivities = physicalActivityCollector.collect(periodStart, periodEnd)
            val syncedPhysicalActivities = physicalActivityRepository.getSyncedSessions(16)

            _uiState.update { it.copy(
                contractOverview = contractOverview,
                thisWeekPhysicalActivities = thisWeekPhysicalActivities,
                syncedPhysicalActivities = syncedPhysicalActivities,
            ) }
        } catch (ex: Exception) {
            showErrorMessage("Fetch data error: ${(ex.message ?: "").take(40)}", ex)
        } finally {
            _uiState.update { it.copy(isFetchingData = false) }
        }
    }

    override suspend fun syncPhysicalActivities() {
        try {
            _uiState.update { it.copy(isSyncingPhysicalActivities = true) }

            val transaction = physicalActivityPublisher.publish()
            val syncedPhysicalActivities = physicalActivityRepository.getSyncedSessions(16)

            _uiState.update { it.copy(lastSyncTransactionHash = transaction, syncedPhysicalActivities = syncedPhysicalActivities) }
        } catch (ex: Exception) {
            showErrorMessage("Sync physical activities error: ${(ex.message ?: "").take(40)}", ex)
        } finally {
            _uiState.update { it.copy(isSyncingPhysicalActivities = false) }
        }
    }

    override suspend fun refreshScreen() {
        _uiState.update { it.copy(isRefreshingScreen = true) }

        loadDetails()

        _uiState.update { it.copy(isRefreshingScreen = false) }
    }

    override fun eraseLastSyncTransactionHash() {
        _uiState.update { it.copy(lastSyncTransactionHash = null) }
    }

    override fun dismissErrorMessage() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_BACKGROUND_LOCATION])
    override fun setupGymGeofence(context: Context) {
        try {
            if (!settingsService.gymGeofenceCreated()) {
                gymGeofenceCreator.create(context)
            }
        } catch (ex: Exception) {
            showErrorMessage("Failed to setup geofence.", ex)
        }
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
        val network = BuildConfig.NETWORK

        return ContractOverview(creationDate, expirationDate, currentWeek, secondsInOneWeek, phase, network)
    }
}

interface MainScreenViewModel {
    val uiState: StateFlow<MainUiState>

    suspend fun loadDetails()

    suspend fun syncPhysicalActivities()

    suspend fun refreshScreen()

    fun eraseLastSyncTransactionHash()

    fun dismissErrorMessage()

    fun setupGymGeofence(context: Context)
}

class PreviewMainScreenViewModel: MainScreenViewModel {
    override val uiState: StateFlow<MainUiState> = MutableStateFlow(MainUiState(
        contractOverview = ContractOverview(
            creationDate = Instant.now().minus(Duration.ofMinutes(90)),
            expirationDate = Instant.now(),
            currentWeek = Week(1, Instant.now().minus(Duration.ofMinutes(60)), Instant.now().minus(Duration.ofMinutes(30))),
            secondsInOneWeek = Duration.ofMinutes(30),
            phase = PhysicalActivityOracleService.ContractPhase.Active,
            network = "localhost",
        ),
        thisWeekPhysicalActivities = PhysicalActivityEvents(
            sleep = mutableListOf(SleepEvent(Instant.now().minus(Duration.ofMinutes(50)), 20, 50, null)),
            running = mutableListOf(RunningEvent(Instant.now().minus(Duration.ofMinutes(40)), 2100, 400, 110)),
            gymVisits = mutableListOf(
                GymVisitEvent(GymVisitEvent.Location(GymConfig.PRIMARY.latitude, GymConfig.PRIMARY.longitude), Instant.now().minus(Duration.ofMinutes(42)), 123, 110, 120)),
        ),
        syncedPhysicalActivities = PhysicalActivityEvents(
            sleep = mutableListOf(SleepEvent(Instant.now().minus(Duration.ofMinutes(50)), 20, 50, PhysicalActivityEvent.SyncDetails("0x000000000000000000001", Instant.now(), "localhost", 0))),
            running = mutableListOf(RunningEvent(Instant.now().minus(Duration.ofMinutes(40)), 2100, 400, 110, PhysicalActivityEvent.SyncDetails("0x000000000000000000001", Instant.now(), "localhost", 0))),
            gymVisits = mutableListOf(
                GymVisitEvent(GymVisitEvent.Location(GymConfig.PRIMARY.latitude, GymConfig.PRIMARY.longitude), Instant.now().minus(Duration.ofMinutes(42)), 123, 110, 120, PhysicalActivityEvent.SyncDetails("0x000000000000000000001", Instant.now(), "localhost", 0))),
        ),
        isFetchingData = false
    ))
    override suspend fun loadDetails() { error("mock") }

    override suspend fun syncPhysicalActivities() { error("mock") }

    override suspend fun refreshScreen() { error("mock") }

    override fun eraseLastSyncTransactionHash() { error("mock") }

    override fun dismissErrorMessage() { error("mock") }

    override fun setupGymGeofence(context: Context) { error("mock") }
}

data class MainUiState (
    val contractOverview: ContractOverview? = null,
    val thisWeekPhysicalActivities: PhysicalActivityEvents? = null,
    val syncedPhysicalActivities: PhysicalActivityEvents = PhysicalActivityEvents(emptyList(), emptyList(), emptyList()),
    val isSyncingPhysicalActivities: Boolean = false,
    val lastSyncTransactionHash: String? = null,
    val errorMessage: String? = null,
    val isFetchingData: Boolean = true,
    val isRefreshingScreen: Boolean = false,
    val network: String = BuildConfig.NETWORK,
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
    val phase: PhysicalActivityOracleService.ContractPhase,
    val network: String,
)