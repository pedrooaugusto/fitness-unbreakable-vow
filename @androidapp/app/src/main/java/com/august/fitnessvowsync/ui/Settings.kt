package com.august.fitnessvowsync.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.august.fitnessvowsync.helpers.SettingsService
import com.august.fitnessvowsync.ui.theme.FitnessVowSyncTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Settings(
    navigateToPermission: () -> Unit,
    settingsService: SettingsService
) {
    val focusManager = LocalFocusManager.current
    var walletPrivateKey by remember { mutableStateOf(settingsService.getClientAccountPrivateKey() ?: "") }
    var rpcEndpoint by remember { mutableStateOf(settingsService.getRpcEndpoint() ?: "") }
    var pinataApiToken by remember { mutableStateOf(settingsService.getPinataApiToken() ?: "") }

    val saveSettings = {
        settingsService.saveClientAccountPrivateKey(walletPrivateKey)
        settingsService.saveRpcEndpoint(rpcEndpoint)
        settingsService.savePinataApiToken(pinataApiToken)
        navigateToPermission()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F0F1A))
            .padding(16.dp)
            .pointerInput(Unit) { detectTapGestures(onTap = { focusManager.clearFocus() }) },
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
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Account Private Key") } ,
            supportingText = {
                Text("Account used to interact with contracts in the blockchain.", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Start)
            },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            ),
        )


        Spacer(modifier = Modifier.height(12.dp))

        TextField(
            value = settingsService.getAppFormattedPublicKey() ?: "<not registered>",
            readOnly = true,
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            onValueChange = {},
            label = { Text("Oracle Public Key") } ,
            supportingText = {
                Text("The oracle will only accept physical activity data signed with this key.", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Start)
            }
        )

        Spacer(modifier = Modifier.height(12.dp))

        TextField(
            value = settingsService.getPhysicalActivityRecordOracleAddress(),
            readOnly = true,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            onValueChange = {},
            label = { Text("Physical Activity Record Oracle Address") } ,
            supportingText = {
                Text("Blockchain address of the contract responsible for storing user data.", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Start)
            },
        )

        Spacer(modifier = Modifier.height(12.dp))

        TextField(
            value = settingsService.getNetwork(),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            readOnly = true,
            onValueChange = {},
            label = { Text("Network") },
            supportingText = {
                Text("Ethereum network the contract was deployed to.", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Start)
            },
        )

        Spacer(modifier = Modifier.height(12.dp))

        TextField(
            value = rpcEndpoint,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            onValueChange = { value -> rpcEndpoint = value },
            label = { Text("RPC Endpoint") },
            supportingText = {
                Text("Point of entry to the blockchain.", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Start)
            },
        )

        Spacer(modifier = Modifier.height(12.dp))

        TextField(
            value = pinataApiToken,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            onValueChange = { value -> pinataApiToken = value },
            label = { Text("Pinata API Token") },
            supportingText = {
                Text("Key attestation files are stored in the IPFS.", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Start)
            },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            ),
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
        Settings({}, SettingsService.PreviewSettingsService())
    }
}