package com.august.fitnessvowsync.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.august.fitnessvowsync.BuildConfig
import com.august.fitnessvowsync.contract.ContractSettingsService
import com.august.fitnessvowsync.ui.theme.FitnessVowSyncTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Settings(
    navigateToPermission: () -> Unit,
    settingsService: ContractSettingsService
) {
    var walletPrivateKey by remember { mutableStateOf(BuildConfig.WALLET_PRIVATE_KEY) }

    val saveSettings = {
        settingsService.saveClientAccountPrivateKey(walletPrivateKey)
        navigateToPermission()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F0F1A))
            .padding(16.dp),
    ) {
        Spacer(modifier = Modifier.height(36.dp))

        Text(
            text = "App Settings",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            textAlign = TextAlign.Start,
            modifier = Modifier.padding(bottom = 8.dp).fillMaxWidth()
        )

        Text(
            text = "Please fill all the required fields to ensure the app can properly sync with the smart contract.",
            fontSize = 14.sp,
            color = Color.LightGray,
            textAlign = TextAlign.Start,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))

        TextField(
            value = walletPrivateKey,
            onValueChange = { value -> walletPrivateKey = value },
            label = { Text("Account Private Key") } ,
            supportingText = {
                Text("Account used to interact with contracts in the blockchain.", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Start)
            }
        )

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = saveSettings,
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .clip(RoundedCornerShape(12.dp))
                .then(Modifier.background(
                    brush = Brush.linearGradient(listOf(Color(0xFF8A2BE2), Color(0xFF4B0082))),
                    shape = RoundedCornerShape(12.dp))
                ),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Transparent,
                contentColor = Color.White,
                disabledContainerColor = Color.Transparent
            ),
            contentPadding = PaddingValues(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Save,
                contentDescription = "Save Icon",
                tint = Color.White,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Save",
                fontWeight = FontWeight.SemiBold,
                style = TextStyle(fontSize = 16.sp, color = Color.White)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SettingsPreview() {
    FitnessVowSyncTheme {
        Settings({}, ContractSettingsService.PreviewContractSettingsService())
    }
}