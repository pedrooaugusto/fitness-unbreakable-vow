package com.august.fitnessvowsync.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SyncNowButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isSyncing: Boolean
) {
    val gradientColors = listOf(Color(0xFF8A2BE2), Color(0xFF4B0082))
    val buttonBackground = if (isSyncing) {
        Modifier.background(Color.Gray)
    } else {
        Modifier.background(brush = Brush.linearGradient(gradientColors), shape = RoundedCornerShape(12.dp))
    }

    val infiniteTransition = rememberInfiniteTransition()
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .then(buttonBackground),
        enabled = !isSyncing,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            contentColor = Color.White,
            disabledContainerColor = Color.Transparent
        ),
        contentPadding = PaddingValues(12.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Sync,
            contentDescription = "Sync Icon",
            tint = Color.White,
            modifier = Modifier.size(21.dp).graphicsLayer { rotationZ = if (isSyncing) rotation else 0f }
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = if (isSyncing) "Syncing..." else "Sync Now",
            fontWeight = FontWeight.SemiBold,
            style = TextStyle(
                fontSize = 16.sp,
                color = Color.White
            )
        )
    }
}