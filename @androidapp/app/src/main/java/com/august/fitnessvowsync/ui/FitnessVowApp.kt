package com.august.fitnessvowsync.ui

import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.august.fitnessvowsync.contract.TimeLordService
import com.august.fitnessvowsync.helpers.TimeHelpers
import com.august.fitnessvowsync.helpers.TimeHelpers.Companion.formatMinutes
import com.august.fitnessvowsync.physicalactivity.model.PhysicalActivityEvents
import com.august.fitnessvowsync.ui.components.AppNameSection
import com.august.fitnessvowsync.ui.components.DashboardLink
import com.august.fitnessvowsync.ui.components.ErrorDialog
import com.august.fitnessvowsync.ui.components.LoadingGuard
import com.august.fitnessvowsync.ui.components.PhysicalActivityDetailsCard
import com.august.fitnessvowsync.ui.components.PhysicalActivityDialogType
import com.august.fitnessvowsync.ui.components.PhysicalActivityDetailsDialogSwitcher
import com.august.fitnessvowsync.ui.components.SettingsButton
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
    onClickPhysicalActivity: (suspend (PhysicalActivityDialogType) -> Unit)? = null
) {
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val state = rememberPullToRefreshState()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val syncPhysicalActivities: suspend () -> Unit = {
        viewModel.syncPhysicalActivities()
    }

    LaunchedEffect(Unit) {
        viewModel.loadDetails()
        viewModel.setupGymGeofence(context)
    }

    LaunchedEffect(key1 = uiState.lastSyncTransactionHash) {
        delay(10000)
        viewModel.eraseLastSyncTransactionHash()
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

                Box(modifier = Modifier.fillMaxWidth().statusBarsPadding()) {
                    SettingsButton(navigateToSettings, modifier = Modifier.align(Alignment.TopEnd).zIndex(1f).offset(y = 6.dp))
                    Column {
                        AppNameSection()
                        DashboardLink(uiState.network)
                    }
                }
                Spacer(modifier = Modifier.height(21.dp))
                LoadingGuard(isLoading = uiState.isRefreshingScreen || uiState.isFetchingData) {
                    CurrentWeekPhysicalActivitiesSection(
                        physicalActivities = uiState.thisWeekPhysicalActivities,
                        contractOverview = uiState.contractOverview,
                        onClickPhysicalActivity = onClickPhysicalActivity,
                    )
                    Spacer(modifier = Modifier.height(21.dp))
                    SyncHistorySection(uiState.syncedPhysicalActivities)
                    Spacer(modifier = Modifier.height(26.dp))
                    SyncNowSection(
                        syncNextRecord = syncPhysicalActivities,
                        isSyncing = uiState.isSyncingPhysicalActivities,
                        lastSyncTransactionHash = uiState.lastSyncTransactionHash,
                        network = uiState.contractOverview?.network ?: ""
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
fun CurrentWeekPhysicalActivitiesSection(
    physicalActivities: PhysicalActivityEvents?,
    contractOverview: ContractOverview?,
    onClickPhysicalActivity: (suspend (activity: PhysicalActivityDialogType) -> Unit)?
) {
    if (physicalActivities == null || contractOverview == null) return

    var selectedActivityDialog by remember { mutableStateOf<PhysicalActivityDialogType?>(null) }
    val isExpired = contractOverview.phase != TimeLordService.ContractPhase.Active
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
        TextWithIcon(icon = Icons.Default.Sync, text = "Physical Activities")
        Text(
            text = "Physical activities detected in week #%s that will be submitted to the oracle.".format(currentWeek.index),
            fontSize = 14.sp,
            fontWeight = FontWeight.Normal,
            color = Color(0xff9ca3af)
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val formattedRunningSessions = if (isExpired) "--" else "%02d".format(physicalActivities.running.size)
            val formattedSleepSessions = if (isExpired) "--" else "%02d".format(physicalActivities.sleep.size)
            val formattedGymVisits = if (isExpired) "--" else "%02d".format(physicalActivities.gymVisits.size)
            val formattedTotalDistance = if (isExpired) ".." else "%.1fkm".format(physicalActivities.running.sumOf { it.distanceInMeters } / 1000.0)
            val formattedSleepTotalTime = if (isExpired) ".." else formatMinutes(physicalActivities.sleep.sumOf { it.durationInMinutes }.toLong())
            val formattedGymVisitTotalTime = if (isExpired) ".." else formatMinutes(physicalActivities.gymVisits.sumOf { it.durationInMinutes }.toLong())

            PhysicalActivityDetailsCard(
                icon = Icons.Default.DirectionsRun,
                iconColor = Color(0xff4ade80),
                title = "Run Sessions",
                count = formattedRunningSessions,
                metric = formattedTotalDistance,
                onClick = { selectedActivityDialog = PhysicalActivityDialogType.RUNNING }
            )
            PhysicalActivityDetailsCard(
                icon = Icons.Default.Bed,
                iconColor = Color(0xfff87171),
                title = "Sleep Sessions",
                count = formattedSleepSessions,
                metric = formattedSleepTotalTime,
                onClick = { selectedActivityDialog = PhysicalActivityDialogType.SLEEP }
            )
            PhysicalActivityDetailsCard(
                icon = Icons.Default.FitnessCenter,
                iconColor = Color(0xfffacc15),
                title = "Gym Visits",
                count = formattedGymVisits,
                metric = formattedGymVisitTotalTime,
                onClick = { selectedActivityDialog = PhysicalActivityDialogType.GYM }
            )
        }

        PhysicalActivityDetailsDialogSwitcher(
            selectedDialog = selectedActivityDialog,
            events = physicalActivities,
            onDismiss = { selectedActivityDialog = null },
            onConfirm = onClickPhysicalActivity,
        )

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
fun SyncHistorySection(syncedPhysicalActivities: PhysicalActivityEvents) {
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
                items = syncedPhysicalActivities.groupByTransactionHash(),
                key = { it.syncDetails.transactionHash }
            ) { events ->
                SyncedPhysicalActivityRecordCard(events, Modifier.animateItemPlacement())
            }
        }
    }
}

@Composable
fun SyncNowSection(syncNextRecord: suspend () -> Unit, isSyncing: Boolean, network: String, lastSyncTransactionHash: String?) {
    val coroutineScope = rememberCoroutineScope()

    val transactionUrl = when {
        lastSyncTransactionHash.isNullOrEmpty() -> null
        network == "sepolia" -> "https://sepolia.arbiscan.io/tx/$lastSyncTransactionHash"
        else -> "https://arbiscan.io/tx/$lastSyncTransactionHash"
    }

    Box(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth()) {
            SyncNowButton(
                isSyncing = isSyncing,
                onClick = { coroutineScope.launch { syncNextRecord() } }
            )
        }

        AnimatedVisibility(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = (-15).dp)
                .zIndex(1f),
            visible = !lastSyncTransactionHash.isNullOrEmpty(),
            enter = fadeIn() + slideInVertically(initialOffsetY = { -it / 2 }),
            exit  = fadeOut() + slideOutVertically(targetOffsetY = { -it / 2 })
        ) {
            TransactionPanel(transactionUrl = transactionUrl)
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
