package com.august.fitnessvowsync

import android.Manifest
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresPermission
import androidx.core.app.ActivityCompat
import androidx.health.connect.client.HealthConnectClient
import androidx.lifecycle.lifecycleScope
import com.august.fitnessvowsync.contract.FitnessUnbreakableVow
import com.august.fitnessvowsync.geofencing.GymGeofenceCreator
import com.august.fitnessvowsync.helpers.NotificationService
import com.august.fitnessvowsync.service.PhysicalActivityOracleService
import com.august.fitnessvowsync.service.PhysicalActivityRecordsService
import com.august.fitnessvowsync.ui.FitnessVowApp
import com.august.fitnessvowsync.ui.RequiredPermissions
import com.august.fitnessvowsync.ui.theme.FitnessVowSyncTheme
import kotlinx.coroutines.launch
import javax.inject.Inject

class MainActivity : ComponentActivity() {
    private val LOCATION_PERMISSION_REQUEST_CODE = 1001
    @Inject
    lateinit var fitnessUnbreakableVow: FitnessUnbreakableVow
    @Inject
    lateinit var physicalActivityOracleService: PhysicalActivityOracleService
    @Inject
    lateinit var physicalActivityRecordsService: PhysicalActivityRecordsService
    @Inject
    lateinit var gymGeofenceCreator: GymGeofenceCreator
    @Inject
    lateinit var notificationService: NotificationService
    @Inject
    lateinit var healthConnectClient: HealthConnectClient
    @Inject
    lateinit var encryptedPreferences: SharedPreferences

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_BACKGROUND_LOCATION])
    override fun onCreate(savedInstanceState: Bundle?) {
        val appComponent = (application as MyApplication).appComponent
        val mainActivityComponent = appComponent.mainActivityComponentBuilder().build()

        mainActivityComponent.inject(this)

        super.onCreate(savedInstanceState)

        lifecycleScope.launch {
            physicalActivityOracleService.registerAppAsRecordPublisher()
        }

        enableEdgeToEdge()

        setContent {
            FitnessVowSyncTheme {
                RequiredPermissions(
                    client = healthConnectClient,
                    requestPermission = { id -> requestPermission(id) }
                ) {
                    FitnessVowApp(physicalActivityRecordsService, { setupGeofence() })
                }
            }
        }
    }

    private fun requestPermission(permissionName: String) {
        if (permissionName == "fineLocation") return showPermissionDialog(Manifest.permission.ACCESS_FINE_LOCATION)
        if (permissionName == "backgroundLocation") return showPermissionDialog(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        if (permissionName == "notifications") return showPermissionDialog(Manifest.permission.POST_NOTIFICATIONS)
        if (permissionName == "healthConnect") return openHealthConnectApp()

    }

    private fun showPermissionDialog(permissionId: String) {
        ActivityCompat.requestPermissions(this, arrayOf(permissionId),LOCATION_PERMISSION_REQUEST_CODE)
    }

    private fun openHealthConnectApp() {
        val intent = packageManager.getLaunchIntentForPackage("com.google.android.apps.healthdata")

        if (intent != null) {
            startActivity(intent)
        } else {
            Toast.makeText(this, "Health Connect app is not installed", Toast.LENGTH_SHORT).show()
        }
    }

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_BACKGROUND_LOCATION])
    private fun setupGeofence() {
        val gymGeofenceEnabled = gymGeofenceCreator.isGymGeofenceEnabled()

        if (!gymGeofenceEnabled) {
            gymGeofenceCreator.createGymGeofence(applicationContext)
            notificationService.showGeofenceNotification("Geofence monitoring has started.", applicationContext)
        }
    }
}

