package com.august.fitnessvowsync.dagger

import dagger.Component
import javax.inject.Singleton
import com.august.fitnessvowsync.MyApplication
import com.august.fitnessvowsync.geofencing.GymGeofenceOnBootInitializer
import com.august.fitnessvowsync.physicalactivity.collection.GymVisitGeofenceEventReceiver

@Singleton
@Component(modules = [
    ApplicationModule::class,
    Web3jModule::class,
    KeyStoreModule::class,
    HealthConnectModule::class,
    EncryptedSharedPreferencesModule::class,
    GeofencingModule::class,
    PhysicalActivityRecordServicesModule::class,
    SettingsModule::class
])
interface ApplicationComponent {
    fun inject(application: MyApplication)

    fun inject(receiver: GymGeofenceOnBootInitializer)

    fun inject(receiver: GymVisitGeofenceEventReceiver)

    fun mainActivityComponentBuilder(): MainActivityComponent.Builder
}