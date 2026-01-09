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
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.august.fitnessvowsync.helpers.SettingsService

import com.august.fitnessvowsync.ui.FitnessVowApp
import com.august.fitnessvowsync.ui.RequiredPermissions
import com.august.fitnessvowsync.ui.Settings
import com.august.fitnessvowsync.ui.theme.FitnessVowSyncTheme
import com.august.fitnessvowsync.ui.viewmodel.DefaultMainScreenViewModel
import com.august.fitnessvowsync.ui.viewmodel.DefaultPermissionsScreenViewModel
import com.august.fitnessvowsync.ui.viewmodel.DefaultSettingsScreenViewModel
import javax.inject.Inject
import javax.inject.Named


class MainActivity : ComponentActivity() {
    @Inject
    lateinit var settingsService: SettingsService
    @Inject
    @Named("MAIN_VIEW_MODEL")
    lateinit var mainScreenViewModelFactory: ViewModelProvider.Factory
    @Inject
    @Named("PERMISSIONS_VIEW_MODEL")
    lateinit var permissionsViewModelFactory: ViewModelProvider.Factory
    @Inject
    @Named("SETTINGS_VIEW_MODEL")
    lateinit var settingsViewModelFactory: ViewModelProvider.Factory

    //TODO: Remove support fake data during development
    /*@Inject
    lateinit var doNotUse: FakeDataProducerDoNotUse
    suspend fun __debug_PleaseRemove__randomValueFor(goal: PhysicalActivityDialogType): Unit {
        when (goal) {
            PhysicalActivityDialogType.RUNNING -> doNotUse.addFakeRunningSession(500)
            PhysicalActivityDialogType.SLEEP -> doNotUse.addFakeSleepSession((60).toLong())
            PhysicalActivityDialogType.GYM -> doNotUse.addFakeGymVisit()
        }
    }*/

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_BACKGROUND_LOCATION])
    override fun onCreate(savedInstanceState: Bundle?) {
        val appComponent = (application as MyApplication).appComponent
        val mainActivityComponent = appComponent.mainActivityComponentBuilder().activity(this).build()

        mainActivityComponent.inject(this)

        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContent {
            val navigationController = rememberNavController()
            val navigateToSettings = { navigationController.navigate("settings") }
            val navigateToMain = { navigationController.navigate("main") }
            val navigateToPermission = { navigationController.navigate("permissions") }

            val mainScreenViewModel: DefaultMainScreenViewModel = viewModel(factory = mainScreenViewModelFactory)
            val permissionScreenViewModel: DefaultPermissionsScreenViewModel = viewModel(factory = permissionsViewModelFactory)
            val settingsScreenViewModel: DefaultSettingsScreenViewModel = viewModel(factory = settingsViewModelFactory)

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
                            // TODO: Remove support fake data during development
                            /*onClickPhysicalActivity = { activity ->
                                __debug_PleaseRemove__randomValueFor(activity)
                                //mainScreenViewModel.refreshScreen()
                            }*/
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
                            viewModel = settingsScreenViewModel,
                        )
                    }
                }
            }
        }
    }
}
