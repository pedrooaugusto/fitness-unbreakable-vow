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
import com.august.fitnessvowsync.model.SyncedPhysicalActivityRecord
import kotlinx.coroutines.launch
import java.math.BigInteger
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun RowScope.PhysicalActivityDetailsCard(icon: ImageVector, iconColor: Color, title: String, value: String, onClick: suspend () -> Unit) {
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
        Text(text = value, fontSize = 18.sp, fontWeight = FontWeight.Black, color = Color.White)
        Text(text = title, fontSize = 14.sp, fontWeight = FontWeight.Normal, color = Color(0xff9ca3af))
    }
}

@Composable
fun SyncedPhysicalActivityRecordCard(record: SyncedPhysicalActivityRecord, modifier: Modifier) {
    val context = LocalContext.current
    val formattedDate = formatInstant(record.timestamp)
    val formattedDetails = String.format("Week: #%s, Run: %sm, 8h Sleep: %s, Gym Visits: %s", record.weekIndex, record.runDistanceMeters, record.healthySleepNights, record.gymVisits)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xff1f2937))
            .clickable {
                val intent = Intent(Intent.ACTION_VIEW, record.transactionUrl.toUri())

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