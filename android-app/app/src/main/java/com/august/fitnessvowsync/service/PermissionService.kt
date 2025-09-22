package com.august.fitnessvowsync.service

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.core.app.ActivityCompat
import androidx.health.connect.client.HealthConnectClient
import com.august.fitnessvowsync.contract.ContractSettingsService

interface PermissionService {
    enum class Permission {
        FINE_LOCATION,
        BACKGROUND_LOCATION,
        NOTIFICATION,
        HEALTH_CONNECT,
        ETHER_WALLET
    }

    fun requestPermission(permission: Permission)
    suspend fun hasPermission(permission: Permission): Boolean

    class PermissionServiceImpl(
        private val activity: ComponentActivity,
        private val navigateToSettings: () -> Unit,
        private val healthConnectClient: HealthConnectClient,
        private val settingsService: ContractSettingsService,
    ): PermissionService {
        private val LOCATION_PERMISSION_REQUEST_CODE = 1001

        override fun requestPermission(permission: Permission) {
            when (permission) {
                Permission.HEALTH_CONNECT -> showHealthConnectPermissionDialog()
                Permission.FINE_LOCATION -> showStandardPermissionDialog(Manifest.permission.ACCESS_FINE_LOCATION)
                Permission.BACKGROUND_LOCATION -> showStandardPermissionDialog(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                Permission.NOTIFICATION -> showStandardPermissionDialog(Manifest.permission.POST_NOTIFICATIONS)
                Permission.ETHER_WALLET -> navigateToSettings()
            }
        }

        override suspend fun hasPermission(permission: Permission): Boolean {
            return when (permission) {
                Permission.HEALTH_CONNECT -> hasHealthConnectPermissions()
                Permission.FINE_LOCATION -> hasStandardPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                Permission.BACKGROUND_LOCATION -> hasStandardPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                Permission.NOTIFICATION -> hasStandardPermission(Manifest.permission.POST_NOTIFICATIONS)
                Permission.ETHER_WALLET -> !settingsService.getClientAccountPrivateKey().isNullOrEmpty()
            }
        }

        private fun showHealthConnectPermissionDialog() {
            val intent = activity.packageManager.getLaunchIntentForPackage("com.google.android.apps.healthdata")

            if (intent != null) {
                activity.startActivity(intent)
            } else {
                Toast.makeText(activity, "Health Connect app is not installed", Toast.LENGTH_SHORT).show()
            }
        }

        private fun showStandardPermissionDialog(permissionId: String) {
            Log.i("FitVow", "Requesting standard permission for $permissionId")

            ActivityCompat.requestPermissions(activity, arrayOf(permissionId),LOCATION_PERMISSION_REQUEST_CODE)
        }

        private suspend fun hasHealthConnectPermissions(): Boolean {
            val requiredPermissions = setOf(
                "android.permission.health.READ_EXERCISE",
                "android.permission.health.READ_DISTANCE",
                "android.permission.health.READ_HEART_RATE",
                "android.permission.health.READ_SLEEP"
            )

            val grantedPermissions = healthConnectClient.permissionController.getGrantedPermissions();

            return requiredPermissions.all { it in grantedPermissions }
        }

        private fun hasStandardPermission(permission: String): Boolean {
            val result = ActivityCompat.checkSelfPermission(activity, permission)

            Log.i("FitVow", "Permission for $permission is $result. Granted: ${result == PackageManager.PERMISSION_GRANTED}")

            return result == PackageManager.PERMISSION_GRANTED
        }
    }

    class PreviewPermissionService: PermissionService {
        override fun requestPermission(permission: Permission) { TODO("Not yet implemented") }
        override suspend fun hasPermission(permission: Permission): Boolean { TODO("Not yet implemented") }
    }
}