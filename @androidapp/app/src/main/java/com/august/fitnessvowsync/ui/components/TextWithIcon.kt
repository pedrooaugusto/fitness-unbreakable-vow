package com.august.fitnessvowsync.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp


@Composable
fun TextWithIcon(
    icon: ImageVector,
    text: String,
    iconColor: Color = Color(0xffa5b4fc),
    iconModifier: Modifier = Modifier.size(21.dp),
    textStyle: TextStyle = TextStyle(fontSize = 16.sp, color = Color(0xffa5b4fc)),
    fontWeight: FontWeight = FontWeight.SemiBold,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = horizontalArrangement
    ) {
        Icon(
            imageVector = icon,
            contentDescription = icon.name,
            tint = iconColor,
            modifier = iconModifier
        )

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = text,
            fontWeight = fontWeight,
            style = textStyle
        )
    }
}