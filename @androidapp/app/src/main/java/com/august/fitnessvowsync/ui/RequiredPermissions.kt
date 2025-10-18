package com.august.fitnessvowsync.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.august.fitnessvowsync.service.PermissionService
import com.august.fitnessvowsync.ui.components.AppNameSection
import com.august.fitnessvowsync.ui.components.ErrorDialog
import com.august.fitnessvowsync.ui.components.LoadingGuard
import com.august.fitnessvowsync.ui.theme.FitnessVowSyncTheme
import com.august.fitnessvowsync.ui.viewmodel.PermissionsScreenViewModel
import com.august.fitnessvowsync.ui.viewmodel.PreviewPermissionsScreenViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RequiredPermissions(
    viewModel: PermissionsScreenViewModel,
    navigateToMain: () -> Unit,
    navigateToSettings: () -> Unit,
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val allPermissionsGranted = uiState.permissions.all { it.value }

    suspend fun updatePermissions() {
        viewModel.updatePermissions()
    }

    LaunchedEffect(allPermissionsGranted) {
        if (!allPermissionsGranted) return@LaunchedEffect

        viewModel.exchangeKeys()
    }

    LaunchedEffect(allPermissionsGranted, uiState.keysExchanged) {
        if (allPermissionsGranted && uiState.keysExchanged) {
            navigateToMain()
        }
    }

    // Observe lifecycle for ON_RESUME and run suspend logic properly
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                coroutineScope.launch {
                    updatePermissions()
                }
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F0F1A))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        SettingsButton(navigateToSettings)
        AppNameSection()

        Spacer(modifier = Modifier.height(36.dp))

        Text(
            text = "Permissions Required",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 8.dp).fillMaxWidth()
        )
        Text(
            text = "Please grant the following permissions to ensure the app can properly sync your activity data with the smart contract.",
            fontSize = 14.sp,
            color = Color.LightGray,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
        ) {
            items(getPermissionItems()) { item ->
                PermissionItem(
                    item = item,
                    isGranted = uiState.permissions[item.key] == true,
                    onRequest = { viewModel.requestPermission(item.key) }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
        }

        Spacer(modifier = Modifier.height(15.dp))

        LoadingGuard(isLoading = uiState.isExchangingKeys, modifier = Modifier.fillMaxWidth()) {}

        ErrorDialog(
            errorMessage = uiState.errorMessage,
            onDismiss = { viewModel.dismissErrorMessage() }
        )
    }
}


@Composable
fun PermissionItem(item: PermissionItemData, isGranted: Boolean, onRequest: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1E1E2D), shape = RoundedCornerShape(20.dp))
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Icon(
                imageVector = if (isGranted) Icons.Default.CheckCircle else Icons.Outlined.Cancel,
                contentDescription = if (isGranted) "Granted" else "Not Granted",
                tint = if (isGranted) Color(0xFF4CAF50) else Color(0xFFF44336),
                modifier = Modifier.size(26.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = item.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
                Text(
                    text = item.description,
                    fontSize = 14.sp,
                    color = Color.LightGray
                )
            }
        }

        if (!isGranted) {
            Button(
                onClick = onRequest,
                modifier = Modifier.clip(RoundedCornerShape(20.dp)),
                colors = ButtonDefaults.buttonColors(Color(0xFF8B5CF6)),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(text = "Grant", color = Color.White, fontSize = 14.sp)
            }
        }
    }
}

data class PermissionItemData(
    val key: PermissionService.Permission,
    val title: String,
    val description: String,
    val icon: ImageVector
)

fun getPermissionItems(): List<PermissionItemData> {
    return listOf(
        PermissionItemData(
            PermissionService.Permission.FINE_LOCATION,
            "Access Fine Location",
            "Required to accurately measure gym visits.",
            Icons.Default.CheckCircle
        ),
        PermissionItemData(
            PermissionService.Permission.BACKGROUND_LOCATION,
            "Access Background Location",
            "Required to accurately measure gym visits.",
            Icons.Default.CheckCircle
        ),
        PermissionItemData(
            PermissionService.Permission.NOTIFICATION,
            "Send Notifications",
            "Used to provide important alerts about sync status.",
            Icons.Default.CheckCircle
        ),
        PermissionItemData(
            PermissionService.Permission.HEALTH_CONNECT,
            "Connect to Health Connect",
            "Syncs physical activity data from your device.",
            Icons.Default.CheckCircle
        ),
        PermissionItemData(
            PermissionService.Permission.ETHER_WALLET,
            "Connect Wallet",
            "Wallet access is needed to publish records to oracle.",
            Icons.Default.CheckCircle
        ),
    )
}


@Preview(showBackground = true)
@Composable
fun RequiredPermissionsPreview() {
    FitnessVowSyncTheme {
        RequiredPermissions(PreviewPermissionsScreenViewModel(), {}, {})
    }
}