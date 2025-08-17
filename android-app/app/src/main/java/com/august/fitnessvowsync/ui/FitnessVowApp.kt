package com.august.fitnessvowsync.ui

import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.august.fitnessvowsync.model.AddPhysicalActivityRecordRequest
import com.august.fitnessvowsync.model.AddPhysicalActivityRecordTransaction
import com.august.fitnessvowsync.service.IPhysicalActivityRecordsService
import com.august.fitnessvowsync.service.PreviewPhysicalActivityRecordsService
import com.august.fitnessvowsync.ui.components.PhysicalActivityDetailsCard
import com.august.fitnessvowsync.ui.components.SyncNowButton
import com.august.fitnessvowsync.ui.components.SyncedPhysicalActivityTransaction
import com.august.fitnessvowsync.ui.components.TextWithIcon
import com.august.fitnessvowsync.ui.components.TransactionPanel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FitnessVowApp(activityRecordsService: IPhysicalActivityRecordsService, setupGeofencing: () -> Unit) {
    val focusManager = LocalFocusManager.current

    var nextRecordToSync by remember { mutableStateOf(AddPhysicalActivityRecordRequest()) }
    var isSyncing by remember { mutableStateOf(false) }
    var synOperationTransaction by remember { mutableStateOf(AddPhysicalActivityRecordTransaction()) }
    var syncedRecordsTransactions by remember { mutableStateOf<List<AddPhysicalActivityRecordTransaction>>(emptyList()) }


    val syncRecord: suspend () -> Unit = {
        isSyncing = true
        synOperationTransaction = activityRecordsService.syncPhysicalActivityRecord()
        syncedRecordsTransactions = activityRecordsService.getSyncedRecordsTransactions()
        isSyncing = false
    }

    LaunchedEffect(activityRecordsService) {
        nextRecordToSync = activityRecordsService.getNextSyncPhysicalActivityRecord()
        syncedRecordsTransactions = activityRecordsService.getSyncedRecordsTransactions()
    }

    LaunchedEffect(key1 = synOperationTransaction) {
        delay(10000)
        synOperationTransaction = AddPhysicalActivityRecordTransaction()
    }

    LaunchedEffect(Unit) {
        setupGeofencing()
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) { detectTapGestures(onTap = { focusManager.clearFocus() }) },
        containerColor = Color(0xff111827)
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxSize(),
        ) {
            Spacer(modifier = Modifier.height(14.dp))
            AppNameSection()
            Spacer(modifier = Modifier.height(36.dp))
            NextSyncSection(nextRecordToSync)
            Spacer(modifier = Modifier.height(36.dp))
            SyncHistorySection(syncedRecordsTransactions)
            Spacer(modifier = Modifier.height(36.dp))
            SyncNowSection(
                syncNextRecord = syncRecord,
                isSyncing = isSyncing,
                transactionUrlOnBlockExplorer = synOperationTransaction.blockExplorerUrl
            )
        }
    }
}

@Composable
fun AppNameSection() {
    val gradient = Brush.linearGradient(
        colors = listOf(Color(0xff818cf8), Color(0xffc084fc))
    )

    TextWithIcon(
        icon = Icons.Default.CloudSync,
        text = "FitVow - Sync",
        iconColor = Color.White,
        iconModifier = Modifier.size(40.dp),
        fontWeight = FontWeight.Bold,
        textStyle = TextStyle(fontSize = 36.sp, brush = gradient),
        horizontalArrangement = Arrangement.Center
    )
}

@Composable
fun NextSyncSection(nextRecordToSync: AddPhysicalActivityRecordRequest) {
    Column(
        modifier = Modifier.fillMaxWidth(),
    ) {
        TextWithIcon(icon = Icons.Default.Sync, text = "Next Sync Data")

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val formattedDistance = "%.2f km".format((nextRecordToSync.runDistanceMeters / 1000.0))
            val formattedHealthySleepNights = "%02d".format(nextRecordToSync.healthySleepNights)
            val formattedGymVisits = "%02d".format(nextRecordToSync.gymVisits)

            PhysicalActivityDetailsCard(icon = Icons.Default.DirectionsRun, Color(0xff4ade80), title = "Distance Ran", value = formattedDistance)
            PhysicalActivityDetailsCard(icon = Icons.Default.Bed, Color(0xfff87171), title = "7h30m Sleep", value = formattedHealthySleepNights)
            PhysicalActivityDetailsCard(icon = Icons.Default.FitnessCenter, Color(0xfffacc15), title = "Gym Visits", value = formattedGymVisits)
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Timer,
                contentDescription = "Clock",
                tint = Color(0xff9ca3af),
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(text = "Next automatic sync in: 00:00:00", fontSize = 14.sp, fontWeight = FontWeight.Normal, color = Color(0xff9ca3af))
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SyncHistorySection(syncedRecordsTransactions: List<AddPhysicalActivityRecordTransaction>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 350.dp) // Limit height so it scrolls,
    ) {
        TextWithIcon(icon = Icons.Outlined.Timer, text = "Sync History")

        Spacer(modifier = Modifier.height(12.dp))

        // Scrollable list with animation on item placement
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            reverseLayout = true
        ) {
            items(
                items = syncedRecordsTransactions,
                key = { it.timestamp }
            ) { record ->
                SyncedPhysicalActivityTransaction(record, Modifier.animateItemPlacement())
            }
        }
    }
}

@Composable
fun SyncNowSection(syncNextRecord: suspend () -> Unit, isSyncing: Boolean, transactionUrlOnBlockExplorer: String) {
    val coroutineScope = rememberCoroutineScope()

    Column {
        SyncNowButton(
            isSyncing = isSyncing,
            onClick = { coroutineScope.launch { syncNextRecord() } }
        )

        Spacer(modifier = Modifier.height(24.dp))

        AnimatedVisibility(
            visible = transactionUrlOnBlockExplorer.isNotEmpty(),
            enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it })
        ) {
            TransactionPanel(transactionUrlOnBlockExplorer = transactionUrlOnBlockExplorer)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun FitnessVowPreview() {
    _root_ide_package_.com.august.fitnessvowsync.ui.theme.FitnessVowSyncTheme {
        FitnessVowApp(PreviewPhysicalActivityRecordsService()) {}
    }
}