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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.august.fitnessvowsync.ui.theme.FitnessVowSyncTheme
import com.august.fitnessvowsync.ui.viewmodel.PreviewSettingsScreenViewModel
import com.august.fitnessvowsync.ui.viewmodel.SettingsScreenViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Settings(
    navigateToPermission: () -> Unit,
    viewModel: SettingsScreenViewModel,
) {
    val focusManager = LocalFocusManager.current
    val coroutineScope = rememberCoroutineScope()

    var walletPrivateKey by remember { mutableStateOf(viewModel.settingsService().getClientAccountPrivateKey() ?: "") }
    var rpcEndpoint by remember { mutableStateOf(viewModel.settingsService().getRpcEndpoint() ?: "") }
    var pinataApiToken by remember { mutableStateOf(viewModel.settingsService().getPinataApiToken() ?: "") }
    var gasLimit by remember { mutableStateOf(viewModel.settingsService().getGasLimit().toString()) }
    var gasPriceMarkup by remember { mutableStateOf(viewModel.settingsService().getGasPriceMarkUp().toString()) }
    var emergencyKeyChangeReason by remember { mutableStateOf("") }
    var arePublicKeysSynced by remember { mutableStateOf(true) }

    val saveSettings: suspend () -> Unit = {
        viewModel.settingsService().saveClientAccountPrivateKey(walletPrivateKey)
        viewModel.settingsService().saveRpcEndpoint(rpcEndpoint)
        viewModel.settingsService().savePinataApiToken(pinataApiToken)
        viewModel.settingsService().saveGasLimit(gasLimit.toLong())
        viewModel.settingsService().saveGasPriceMarkUp(gasPriceMarkup.toLong())

        if (!arePublicKeysSynced && !emergencyKeyChangeReason.isEmpty()) {
            viewModel.emergencyPublicKeyChange(emergencyKeyChangeReason)
        }

        navigateToPermission()
    }

    LaunchedEffect(Unit) {
        arePublicKeysSynced = viewModel.arePublicKeysInSync()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F0F1A))
            .padding(16.dp)
            .pointerInput(Unit) { detectTapGestures(onTap = { focusManager.clearFocus() }) }
            .verticalScroll(rememberScrollState()),
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
            value = viewModel.settingsService().getAppFormattedPublicKey() ?: "<not registered>",
            readOnly = true,
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            onValueChange = {},
            label = { Text("App Public Key") } ,
            supportingText = {
                Text("The app will sign all publish physical activity requests to the oracle with this key.", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Start)
            }
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (!arePublicKeysSynced) {
            TextField(
                value = emergencyKeyChangeReason,
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                onValueChange = { value -> emergencyKeyChangeReason = value },
                label = { Text("Public Key Change Reason") },
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = Color.Red,
                    unfocusedIndicatorColor = Color.Red,
                ),
                supportingText = {
                    Text(
                        "The public key on this device does not match the registered key in the contract. A one time, emergency, expensive key change is allowed.",
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Start,
                        color = Color(0xFFFF474D),
                    )
                }
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        Spacer(modifier = Modifier.height(12.dp))

        TextField(
            value = viewModel.settingsService().getPhysicalActivityRecordOracleAddress(),
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
            value = viewModel.settingsService().getNetwork(),
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

        Spacer(modifier = Modifier.height(12.dp))

        TextField(
            value = gasLimit,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            onValueChange = { value -> gasLimit = value },
            label = { Text("Transaction Gas Limit") },
            supportingText = {
                Text("Gas limit for publish activity records transactions.", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Start)
            },
        )

        Spacer(modifier = Modifier.height(12.dp))

        TextField(
            value = gasPriceMarkup,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            onValueChange = { value -> gasPriceMarkup = value },
            label = { Text("Gas Price Markup") },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number
            ),
            supportingText = {
                Text("How much more we are willing to pay for the current price of gas (eg: 150%).", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Start)
            },
        )

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = { coroutineScope.launch { saveSettings() } },
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
        HorizontalDivider()
        Spacer(modifier = Modifier.height(20.dp))
        Button(
            onClick = { viewModel.clearHistory() },
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .clip(RoundedCornerShape(12.dp))
                .then(Modifier.background(
                    color = Color.DarkGray,
                    shape = RoundedCornerShape(12.dp))
                ),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Transparent,
                contentColor = Color.White,
                disabledContainerColor = Color.Transparent
            ),
            contentPadding = PaddingValues(5.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Clear,
                contentDescription = "Clear sync history",
                tint = Color.White,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Clear Sync History",
                fontWeight = FontWeight.SemiBold,
                style = TextStyle(fontSize = 14.sp, color = Color.White)
            )
        }
    }
}

@Preview(showBackground = true, device = "spec:width=1080px,height=3040px,dpi=440", showSystemUi = true)
@Composable
fun SettingsPreview() {
    FitnessVowSyncTheme {
        Settings({}, PreviewSettingsScreenViewModel())
    }
}