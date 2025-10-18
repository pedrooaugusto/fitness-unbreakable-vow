package com.august.fitnessvowsync

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresPermission
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.health.connect.client.HealthConnectClient
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.august.fitnessvowsync.contract.ContractSettingsService
import com.august.fitnessvowsync.dagger.ViewModelFactoryModule
import com.august.fitnessvowsync.service.PermissionService
import com.august.fitnessvowsync.service.PhysicalActivityOracleService
import com.august.fitnessvowsync.ui.FitnessVowApp
import com.august.fitnessvowsync.ui.RequiredPermissions
import com.august.fitnessvowsync.ui.Settings
import com.august.fitnessvowsync.ui.theme.FitnessVowSyncTheme
import com.august.fitnessvowsync.ui.viewmodel.DefaultMainScreenViewModel
import com.august.fitnessvowsync.ui.viewmodel.DefaultPermissionsScreenViewModel
import javax.inject.Inject
import javax.inject.Named


class MainActivity : ComponentActivity() {
    @Inject
    lateinit var physicalActivityOracleService: PhysicalActivityOracleService
    @Inject
    lateinit var healthConnectClient: HealthConnectClient
    @Inject
    lateinit var settingsService: ContractSettingsService.ContractSettingsServiceImpl
    @Inject
    @Named("MAIN_VIEW_MODEL")
    lateinit var mainScreenViewModelFactory: ViewModelProvider.Factory

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_BACKGROUND_LOCATION])
    override fun onCreate(savedInstanceState: Bundle?) {
        val appComponent = (application as MyApplication).appComponent
        val mainActivityComponent = appComponent.mainActivityComponentBuilder().build()

        mainActivityComponent.inject(this)

        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContent {
            val navigationController = rememberNavController()
            val navigateToSettings = { navigationController.navigate("settings") }
            val navigateToMain = { navigationController.navigate("main") }
            val navigateToPermission = { navigationController.navigate("permissions") }

            val permissionService = PermissionService.PermissionServiceImpl(this, navigateToSettings, healthConnectClient, settingsService)
            val mainScreenViewModel: DefaultMainScreenViewModel = viewModel(factory = mainScreenViewModelFactory)
            // Kill me if you don't like it. https://www.youtube.com/watch?v=yjRagoONBcc
            val permissionScreenViewModel: DefaultPermissionsScreenViewModel = viewModel(factory =  ViewModelFactoryModule.providePermissionViewModelFactory(physicalActivityOracleService, permissionService))

            FitnessVowSyncTheme {
                NavHost(
                    navController = navigationController,
                    startDestination = "permissions",
                    enterTransition = { slideInHorizontally(animationSpec = tween(300)) + fadeIn(animationSpec = tween(300)) },
                    exitTransition = { slideOutHorizontally(animationSpec = tween(300)) + fadeOut(animationSpec = tween(300)) },
                    popEnterTransition = { slideInHorizontally(initialOffsetX = { -it }, animationSpec = tween(300)) + fadeIn(animationSpec = tween(300)) },
                    popExitTransition = { slideOutHorizontally(targetOffsetX = { -it }, animationSpec = tween(300)) + fadeOut(animationSpec = tween(300)) }
                ) {
                    composable("main") {
                        FitnessVowApp(
                            viewModel = mainScreenViewModel,
                            navigateToSettings = navigateToSettings,
                        )
                    }
                    composable("permissions") {
                        RequiredPermissions(
                            permissionScreenViewModel,
                            navigateToMain = navigateToMain,
                            navigateToSettings = navigateToSettings
                        )
                    }
                    composable("settings") {
                        Settings(
                            navigateToPermission = navigateToPermission,
                            settingsService = settingsService
                        )
                    }
                }
            }
        }
    }
}

