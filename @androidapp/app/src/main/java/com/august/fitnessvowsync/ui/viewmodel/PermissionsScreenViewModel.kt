package com.august.fitnessvowsync.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import com.august.fitnessvowsync.service.PermissionService
import com.august.fitnessvowsync.service.PermissionService.Permission
import com.august.fitnessvowsync.service.PhysicalActivityOracleService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlin.collections.set

class DefaultPermissionsScreenViewModel(
    private val physicalActivityOracleService: PhysicalActivityOracleService,
    private val permissionService: PermissionService,
): ViewModel(), PermissionsScreenViewModel {
    private val _uiState = MutableStateFlow(PermissionsUiState(mapOf(
        Permission.FINE_LOCATION to false,
        Permission.BACKGROUND_LOCATION to false,
        Permission.NOTIFICATION to false,
        Permission.HEALTH_CONNECT to false,
        Permission.ETHER_WALLET to false
    )))

    override val uiState: StateFlow<PermissionsUiState> = _uiState

    override fun requestPermission(permission: Permission) { permissionService.requestPermission(permission) }

    override fun dismissErrorMessage() { _uiState.update { it.copy(errorMessage = null) } }

    override suspend fun exchangeKeys() {
        Log.i("FitVow", "Exchanging keys with the Oracle.")

        try {
            _uiState.update { it.copy(isExchangingKeys = true, keysExchanged = false) }
            physicalActivityOracleService.registerAppAsRecordPublisher()
            _uiState.update { it.copy(keysExchanged = true) }
            updatePermissions()
        } catch (ex: Exception) {
            showErrorMessage("Unable to exchange keys with the Oracle. Go to Settings and check if the wallet private key is correct.\n\nError: ${(ex.message ?: "").take(40)}.", ex)
        } finally {
            _uiState.update { it.copy(isExchangingKeys = false) }
        }
    }

    override suspend fun updatePermissions() {
        val permissions = mutableMapOf<Permission, Boolean>()

        for (permissionName: Permission in _uiState.value.permissions.keys) {
            permissions[permissionName] = permissionService.hasPermission(permissionName)
        }

        _uiState.update { it.copy(permissions = permissions) }
    }

    private fun showErrorMessage(message: String, ex: Exception) {
        _uiState.update { it.copy(errorMessage = message) }

        Log.e("FitVow", "Unexpected error: ", ex)
    }
}

interface PermissionsScreenViewModel {
    val uiState: StateFlow<PermissionsUiState>

    fun requestPermission(permission: Permission)

    suspend fun exchangeKeys()

    suspend fun updatePermissions()

    fun dismissErrorMessage()
}

class PreviewPermissionsScreenViewModel: PermissionsScreenViewModel {
    override val uiState: StateFlow<PermissionsUiState> = MutableStateFlow(PermissionsUiState(emptyMap()))
    override fun requestPermission(permission: Permission) {
        TODO("Not yet implemented")
    }

    override suspend fun exchangeKeys() {
        TODO("Not yet implemented")
    }

    override suspend fun updatePermissions() {
        TODO("Not yet implemented")
    }

    override fun dismissErrorMessage() {
        TODO("Not yet implemented")
    }
}

data class PermissionsUiState(
    val permissions: Map<Permission, Boolean>,
    val errorMessage: String? = null,
    val isExchangingKeys: Boolean = false,
    val keysExchanged: Boolean = false,
)