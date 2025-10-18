package com.august.fitnessvowsync.ui

import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.HourglassTop
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.august.fitnessvowsync.helpers.TimeHelpers
import com.august.fitnessvowsync.model.ContractPhase
import com.august.fitnessvowsync.model.PhysicalActivityRecord
import com.august.fitnessvowsync.model.SyncedPhysicalActivityRecord
import com.august.fitnessvowsync.ui.components.AppNameSection
import com.august.fitnessvowsync.ui.components.ErrorDialog
import com.august.fitnessvowsync.ui.components.LoadingGuard
import com.august.fitnessvowsync.ui.components.PhysicalActivityDetailsCard
import com.august.fitnessvowsync.ui.components.SyncNowButton
import com.august.fitnessvowsync.ui.components.SyncedPhysicalActivityRecordCard
import com.august.fitnessvowsync.ui.components.TextWithIcon
import com.august.fitnessvowsync.ui.components.TransactionPanel
import com.august.fitnessvowsync.ui.theme.FitnessVowSyncTheme
import com.august.fitnessvowsync.ui.viewmodel.ContractOverview
import com.august.fitnessvowsync.ui.viewmodel.MainScreenViewModel
import com.august.fitnessvowsync.ui.viewmodel.PreviewMainScreenViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FitnessVowApp(
    viewModel: MainScreenViewModel,
    navigateToSettings: () -> Unit,
) {
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val state = rememberPullToRefreshState()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val __debugPleaseRemove__randomValueFor: suspend (String) -> Unit = { goal: String ->
        viewModel.__debugPleaseRemove__randomValueFor(goal)
    }

    val syncRecord: suspend () -> Unit = {
        viewModel.syncRecord()
    }

    LaunchedEffect(Unit) {
        viewModel.loadDetails()
        viewModel.setupGymGeofence(context)
    }

    LaunchedEffect(key1 = uiState.syncedRecord?.timestamp?.epochSecond) {
        delay(10000)
        viewModel.eraseSyncedRecord()
    }

    PullToRefreshBox(
        isRefreshing = uiState.isRefreshingScreen,
        onRefresh = { coroutineScope.launch { viewModel.refreshScreen() } },
        state = state
    ) {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) { detectTapGestures(onTap = { focusManager.clearFocus() }) },
            containerColor = Color(0xff111827)
        ) { padding ->
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
                    .fillMaxSize(),
            ) {
                SettingsButton(navigateToSettings)
                AppNameSection()
                Spacer(modifier = Modifier.height(36.dp))
                LoadingGuard(isLoading = uiState.isRefreshingScreen || uiState.isFetchingData) {
                    NextSyncSection(
                        uiState.nextRecordToSync,
                        uiState.contractOverview,
                        __debugPleaseRemove__randomValueFor,
                    )
                    Spacer(modifier = Modifier.height(36.dp))
                    SyncHistorySection(uiState.syncedRecordsList)
                    Spacer(modifier = Modifier.height(32.dp))
                    SyncNowSection(
                        syncNextRecord = syncRecord,
                        isSyncing = uiState.isSyncingRecord,
                        syncedRecord = uiState.syncedRecord
                    )
                }
                ErrorDialog(
                    errorMessage = uiState.errorMessage,
                    onDismiss = { viewModel.dismissErrorMessage() }
                )
            }
        }
    }
}

@Composable
fun NextSyncSection(
    nextRecordToSync: PhysicalActivityRecord?,
    contractOverview: ContractOverview?,
    onClickGoalCard: (suspend (goal: String) -> Unit)?
) {
    if (nextRecordToSync == null || contractOverview == null) return

    val isExpired = contractOverview.phase != ContractPhase.Active
    val currentWeek = contractOverview.currentWeek
    val timeRemaining = TimeHelpers.formatTimeRemaining(currentWeek.end)

    val weekTimeRemaining = when {
        isExpired -> "Contract has expired"
        timeRemaining.isEmpty() -> "Week #${currentWeek.index} has ended"
        else -> "Week #${currentWeek.index} ends in $timeRemaining"
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
    ) {
        TextWithIcon(icon = Icons.Default.Sync, text = "Next Sync Data")
        Text(
            text = "Physical activity records data that will be submitted for week #%s.".format(currentWeek.index),
            fontSize = 14.sp,
            fontWeight = FontWeight.Normal,
            color = Color(0xff9ca3af)
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val formattedDistance = if (isExpired) "--" else "%.2f km".format((nextRecordToSync.runDistanceMeters / 1000.0))
            val formattedHealthySleepNights = if (isExpired) "--" else "%02d".format(nextRecordToSync.healthySleepNights)
            val formattedGymVisits = if (isExpired) "--" else "%02d".format(nextRecordToSync.gymVisits)

            PhysicalActivityDetailsCard(
                icon = Icons.Default.DirectionsRun,
                iconColor = Color(0xff4ade80),
                title = "Distance Ran",
                value = formattedDistance,
                onClick = { onClickGoalCard?.invoke("run") }
            )
            PhysicalActivityDetailsCard(
                icon = Icons.Default.Bed,
                iconColor = Color(0xfff87171),
                title = "8h Sleep",
                value = formattedHealthySleepNights,
                onClick = { onClickGoalCard?.invoke("sleep") }
            )
            PhysicalActivityDetailsCard(
                icon = Icons.Default.FitnessCenter,
                iconColor = Color(0xfffacc15),
                title = "Gym Visits",
                value = formattedGymVisits,
                onClick = { onClickGoalCard?.invoke("gym") }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(imageVector = Icons.Outlined.HourglassTop, contentDescription = "Calendar", tint = Color(0xff9ca3af), modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(5.dp))
            Text(text = weekTimeRemaining, fontSize = 14.sp, fontWeight = FontWeight.Normal, color = Color(0xff9ca3af))
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SyncHistorySection(syncedRecords: List<SyncedPhysicalActivityRecord>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 325.dp)
    ) {
        TextWithIcon(icon = Icons.Outlined.Timer, text = "Sync History")

        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            reverseLayout = true
        ) {
            items(
                items = syncedRecords,
                key = { it.timestamp }
            ) { record ->
                SyncedPhysicalActivityRecordCard(record, Modifier.animateItemPlacement())
            }
        }
    }
}

@Composable
fun SettingsButton(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End,
    ) {
        Icon(
            imageVector = Icons.Default.Settings,
            contentDescription = Icons.Default.Settings.name,
            tint = Color.White,
            modifier = Modifier.size(24.dp).clickable { onClick() },
        )
    }
}

@Composable
fun SyncNowSection(syncNextRecord: suspend () -> Unit, isSyncing: Boolean, syncedRecord: SyncedPhysicalActivityRecord?) {
    val coroutineScope = rememberCoroutineScope()

    Column {
        SyncNowButton(
            isSyncing = isSyncing,
            onClick = { coroutineScope.launch { syncNextRecord() } }
        )

        Spacer(modifier = Modifier.height(24.dp))

        AnimatedVisibility(
            visible = !syncedRecord?.transaction.isNullOrEmpty(),
            enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it })
        ) {
            TransactionPanel(transactionUrl = syncedRecord?.transaction)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun FitnessVowPreview() {
   FitnessVowSyncTheme {
        FitnessVowApp(
            viewModel = PreviewMainScreenViewModel(),
            navigateToSettings = {}
        )
    }
}