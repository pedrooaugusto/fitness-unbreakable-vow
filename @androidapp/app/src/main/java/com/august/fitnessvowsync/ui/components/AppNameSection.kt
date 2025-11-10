package com.august.fitnessvowsync.ui.components

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Outlet
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri

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
fun DashboardLink(network: String) {
    val context = LocalContext.current
    val dashboardUrl = when (network) {
        "localhost" -> "http://192.168.0.105:5173/"
        else -> "https://fitvow.pedroaugusto.dev"
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Link,
            contentDescription = "Link icon",
            tint = Color.White,
            modifier = Modifier.size(18.dp).rotate(45f),
        )

        Spacer(modifier = Modifier.width(4.dp))

        Text(
            text = "fitvow.com",
            fontSize = 16.sp,
            color = Color.LightGray,
            textAlign = TextAlign.Center,
            modifier = Modifier.clickable {
                context.startActivity(Intent(Intent.ACTION_VIEW, dashboardUrl.toUri()))
            }
        )
    }
}