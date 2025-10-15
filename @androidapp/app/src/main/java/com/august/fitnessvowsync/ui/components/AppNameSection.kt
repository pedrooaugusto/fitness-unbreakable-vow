package com.august.fitnessvowsync.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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