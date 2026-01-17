package com.august.fitnessvowsync.dagger

import androidx.activity.ComponentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.august.fitnessvowsync.contract.PhysicalActivityOracleService
import com.august.fitnessvowsync.contract.TimeLordService
import com.august.fitnessvowsync.geofencing.GymGeofenceCreator
import com.august.fitnessvowsync.helpers.SettingsService
import com.august.fitnessvowsync.physicalactivity.collection.PhysicalActivityEventCollector
import com.august.fitnessvowsync.physicalactivity.collection.PhysicalActivityEventPublisher
import com.august.fitnessvowsync.physicalactivity.data.GymVisitTracker
import com.august.fitnessvowsync.physicalactivity.data.PhysicalActivityEventRepository
import com.august.fitnessvowsync.physicalactivity.mapper.GymVisitValidatorMapper
import com.august.fitnessvowsync.security.PermissionService
import com.august.fitnessvowsync.ui.viewmodel.DefaultMainScreenViewModel
import com.august.fitnessvowsync.ui.viewmodel.DefaultPermissionsScreenViewModel
import com.august.fitnessvowsync.ui.viewmodel.DefaultSettingsScreenViewModel
import dagger.Module
import dagger.Provides
import javax.inject.Named

@Module
class MainActivityModule {
    @Provides
    @ActivityScope
    fun providePermissionService(
        activity: ComponentActivity,
        healthConnectClient: androidx.health.connect.client.HealthConnectClient,
        settingsService: SettingsService,
    ): PermissionService {
        return PermissionService(activity, healthConnectClient, settingsService)
    }

    @Provides
    @ActivityScope
    @Named("PERMISSIONS_VIEW_MODEL")
    fun providePermissionsViewModelFactory(
        physicalActivityOracleService: PhysicalActivityOracleService,
        permissionService: PermissionService,
    ): ViewModelProvider.Factory {
        return GenericViewModelFactory({
            DefaultPermissionsScreenViewModel(
                physicalActivityOracleService,
                permissionService
            )
        })
    }

    @Provides
    @ActivityScope
    @Named("MAIN_VIEW_MODEL")
    fun provideMainViewModelFactory(
        oracleService: PhysicalActivityOracleService,
        timeLordService: TimeLordService,
        gymGeofenceCreator: GymGeofenceCreator,
        physicalActivityCollector: PhysicalActivityEventCollector,
        physicalActivityRepository: PhysicalActivityEventRepository,
        physicalActivityPublisher: PhysicalActivityEventPublisher,
        gymVisitTracker: GymVisitTracker,
        gymVisitValidatorMapper: GymVisitValidatorMapper
    ): ViewModelProvider.Factory {
        return GenericViewModelFactory({
            DefaultMainScreenViewModel(
                oracleService,
                timeLordService,
                physicalActivityCollector,
                physicalActivityRepository,
                physicalActivityPublisher,
                gymGeofenceCreator,
                gymVisitTracker,
                gymVisitValidatorMapper
            )
        })
    }

    @Provides
    @ActivityScope
    @Named("SETTINGS_VIEW_MODEL")
    fun provideSettingsViewModelFactory(
        physicalActivityOracleService: PhysicalActivityOracleService,
        settingsService: SettingsService,
        gymVisitTracker: GymVisitTracker,
        physicalActivityEventRepository: PhysicalActivityEventRepository
    ): ViewModelProvider.Factory {
        return GenericViewModelFactory({
            DefaultSettingsScreenViewModel(
                physicalActivityOracleService,
                settingsService,
                gymVisitTracker,
                physicalActivityEventRepository,
            )
        })
    }

    class GenericViewModelFactory <T : ViewModel>(private val viewProvider: () -> T): ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return viewProvider.invoke() as T
        }
    }
}

