package com.august.fitnessvowsync.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.august.fitnessvowsync.helpers.TimeHelpers
import com.august.fitnessvowsync.physicalactivity.model.GymVisitEvent
import com.august.fitnessvowsync.physicalactivity.model.PhysicalActivityEvents
import com.august.fitnessvowsync.physicalactivity.model.RunningEvent
import com.august.fitnessvowsync.physicalactivity.model.SleepEvent
import kotlinx.coroutines.launch
import java.time.ZoneId
import java.time.format.DateTimeFormatter

enum class PhysicalActivityDialogType { RUNNING, SLEEP, GYM }

@Composable
fun PhysicalActivityDetailsDialogSwitcher(
    selectedDialog: PhysicalActivityDialogType?,
    events: PhysicalActivityEvents,
    onDismiss: () -> Unit,
    onConfirm: (suspend (activity: PhysicalActivityDialogType) -> Unit)?,
) {
    val coroutineScope = rememberCoroutineScope()

    val safeOnConfirm: (activity: PhysicalActivityDialogType) -> Unit = { activity ->
        if (onConfirm == null) onDismiss.invoke()
        else coroutineScope.launch { onConfirm.invoke(activity) }
    }

    when (selectedDialog) {
        PhysicalActivityDialogType.RUNNING -> RunningDetailsDialog(
            events = events.running,
            onDismiss = onDismiss,
            onConfirm = { safeOnConfirm(PhysicalActivityDialogType.RUNNING) },
        )

        PhysicalActivityDialogType.SLEEP -> SleepDetailsDialog(
            events = events.sleep,
            onDismiss = onDismiss,
            onConfirm = { safeOnConfirm(PhysicalActivityDialogType.SLEEP) },
        )

        PhysicalActivityDialogType.GYM -> GymVisitDetailsDialog(
            events = events.gymVisits,
            onDismiss = onDismiss,
            onConfirm = { safeOnConfirm(PhysicalActivityDialogType.GYM) },
        )

        null -> Unit
    }
}

@Composable
fun RunningDetailsDialog(events: List<RunningEvent>, onDismiss: () -> Unit, onConfirm: (() -> Unit)) {
    ActivityDetailsDialog(
        title = "Running sessions",
        items = events,
        emptyMessage = "No running sessions detected for this week.",
        columns = listOf(
            ActivityDialogColumn("Date", weight = 1.3f) { formatInstant(it.timestamp) },
            ActivityDialogColumn("Distance", weight = 1f) { formatDistance(it.distanceInMeters) },
            ActivityDialogColumn("Pace", weight = 1f) { formatPace(it.paceInSecondsPerKm) },
            ActivityDialogColumn("Avg BPM", weight = 0.8f) { it.avgBpm.toString() }
        ),
        onDismiss = onDismiss,
        onConfirm = onConfirm,
    )
}

@Composable
fun SleepDetailsDialog(events: List<SleepEvent>, onDismiss: () -> Unit, onConfirm: (() -> Unit)) {
    ActivityDetailsDialog(
        title = "Sleep sessions",
        items = events,
        emptyMessage = "No sleep sessions detected for this week.",
        columns = listOf(
            ActivityDialogColumn("Date", weight = 1.3f) { formatInstant(it.timestamp) },
            ActivityDialogColumn("Length", weight = 1f) { TimeHelpers.formatMinutes(it.durationInMinutes.toLong()) },
            ActivityDialogColumn("Avg BPM", weight = 0.8f) { it.avgBpm.toString() }
        ),
        onDismiss = onDismiss,
        onConfirm = onConfirm,
    )
}

@Composable
fun GymVisitDetailsDialog(events: List<GymVisitEvent>, onDismiss: () -> Unit, onConfirm: (() -> Unit)) {
    ActivityDetailsDialog(
        title = "Gym visits",
        items = events,
        emptyMessage = "No gym visits detected for this week.",
        columns = listOf(
            ActivityDialogColumn("Date", weight = 0.25f) { formatInstant(it.timestamp) },
            ActivityDialogColumn("Length", weight = 0.2f) { TimeHelpers.formatMinutes(it.durationInMinutes.toLong()) },
            ActivityDialogColumn("Avg ♥", weight = 0.17f) { it.avgBpm.toString() },
            ActivityDialogColumn("Max ♥", weight = 0.17f) { it.maxBpm.toString() },
            ActivityDialogColumn("Gym", weight = 0.2f) { it.gymLocationId.toString() }
        ),
        onDismiss = onDismiss,
        onConfirm = onConfirm,
    )
}

@Composable
private fun <T> ActivityDetailsDialog(
    title: String,
    items: List<T>,
    emptyMessage: String,
    columns: List<ActivityDialogColumn<T>>,
    onDismiss: () -> Unit,
    onConfirm: (() -> Unit),
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            if (items.isEmpty()) {
                Text(emptyMessage)
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ActivityDialogHeader(columns)
                    Divider()
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 360.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(items) { item ->
                            ActivityDialogRow(item, columns)
                            Divider()
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("Ok") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
private fun <T> ActivityDialogHeader(columns: List<ActivityDialogColumn<T>>) {
    val headerColor = MaterialTheme.colorScheme.onSurfaceVariant

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 2.dp),
        //horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        columns.forEach { column ->
            Text(
                text = column.title,
                modifier = Modifier.weight(column.weight),
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = headerColor
            )
        }
    }
}

@Composable
private fun <T> ActivityDialogRow(item: T, columns: List<ActivityDialogColumn<T>>) {
    val textColor = MaterialTheme.colorScheme.onSurface

    Row(
        modifier = Modifier.fillMaxWidth(),
        //horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        columns.forEach { column ->
            Text(
                text = column.valueProvider(item),
                modifier = Modifier.weight(column.weight),
                fontSize = 11.sp,
                fontWeight = FontWeight.Normal,
                color = textColor
            )
        }
    }
}

private data class ActivityDialogColumn<T>(
    val title: String,
    val weight: Float = 1f,
    val valueProvider: (T) -> String
)

private fun formatInstant(instant: java.time.Instant): String {
    return dialogDateFormatter.format(instant)
}

private fun formatDistance(distanceMeters: Int): String {
    val km = distanceMeters / 1000.0
    return String.format("%.1f km", km)
}

private fun formatPace(secondsPerKm: Int): String {
    val minutes = secondsPerKm / 60
    val seconds = secondsPerKm % 60
    return "%d:%02d /km".format(minutes, seconds)
}

private fun formatLocation(location: GymVisitEvent.Location): String {
    val latitude = location.latitudeNanoDegree / 1e7
    val longitude = location.longitudeNanoDegree / 1e7
    return String.format("%.5f, %.5f", latitude, longitude)
}

private val dialogDateFormatter: DateTimeFormatter = DateTimeFormatter
    .ofPattern("dd/MM HH:mm")
    .withZone(ZoneId.systemDefault())
