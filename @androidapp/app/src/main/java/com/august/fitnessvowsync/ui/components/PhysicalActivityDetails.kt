package com.august.fitnessvowsync.ui.components

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import com.august.fitnessvowsync.physicalactivity.model.PhysicalActivityEvents
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun RowScope.PhysicalActivityDetailsCard(icon: ImageVector, iconColor: Color, title: String, count: String, metric: String, onClick: suspend () -> Unit) {
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xff1f2937))
            .clickable { coroutineScope.launch { onClick() } }
            .padding(12.dp)
            .weight(1f),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = icon.name,
            tint = iconColor,
            modifier = Modifier.size(24.dp)
        )
        Row() {
            Text(text = count, fontSize = 18.sp, fontWeight = FontWeight.Black, color = Color.White)
            Spacer(modifier = Modifier.width(2.dp))
            Text(text = "($metric)", fontSize = 12.sp, fontWeight = FontWeight.Normal, color = Color(0xff9ca3af))
        }
        Text(text = title, fontSize = 12.sp, fontWeight = FontWeight.Normal, color = Color(0xff9ca3af))
    }
}

@Composable
fun SyncedPhysicalActivityRecordCard(activities:  PhysicalActivityEvents.GroupedEvents, modifier: Modifier) {
    val context = LocalContext.current
    val formattedDate = formatInstant(activities.syncDetails.timestamp)
    val formattedDetails = String.format("Week: #%s, Runs: %s, Sleep: %s, Gym Visits: %s", activities.syncDetails.weekIndex, activities.running.size, activities.sleep.size, activities.gymVisits.size)
    val transactionUrl = if (activities.syncDetails.network == "sepolia") {
        "http://sepolia.arbiscan.io/tx/${activities.syncDetails.transactionHash}"
    } else {
        "https://www.arbiscan.io/tx/${activities.syncDetails.transactionHash}"
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xff1f2937))
            .clickable {
                val intent = Intent(Intent.ACTION_VIEW, transactionUrl.toUri())

                context.startActivity(intent)
            }
            .border(
                width = 1.dp,
                color = Color(0xff374151),
                shape = RoundedCornerShape(10.dp)
            )
            .padding(6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.CheckCircle,
                contentDescription = "Success",
                tint = Color(0xff22c55e),
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = formattedDate,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(1.dp))

                Text(
                    text = formattedDetails,
                    color = Color.LightGray,
                    fontSize = 14.sp
                )
            }
        }
    }
}

private fun formatInstant(instant: Instant): String {
    val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss").withZone(ZoneId.systemDefault())

    return formatter.format(instant)
}