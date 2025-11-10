package com.august.fitnessvowsync.ui.components

import android.content.Intent
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri

@Composable
fun TransactionPanel(transactionUrl: String?) {
    val context = LocalContext.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = Color(0xff374151),
                shape = RoundedCornerShape(8.dp)
            ),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xff2562b7).copy(alpha = 0.95f))
    ) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Sync complete! Check on block explorer",
                fontSize = 14.sp,
                color = Color.White,
                textDecoration = TextDecoration.Underline,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 21.dp).clickable {
                    if (transactionUrl == null) return@clickable

                    val intent = Intent(Intent.ACTION_VIEW, transactionUrl.toUri())

                    context.startActivity(intent)
                }
            )
        }
    }
}