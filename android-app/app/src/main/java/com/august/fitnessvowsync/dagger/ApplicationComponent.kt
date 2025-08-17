package com.august.fitnessvowsync.dagger

import dagger.Component
import javax.inject.Singleton
import com.august.fitnessvowsync.MyApplication
import com.august.fitnessvowsync.geofencing.BootReceiver
import com.august.fitnessvowsync.geofencing.GymGeofenceReceiver

@Singleton
@Component(modules = [
    ApplicationModule::class,
    Web3jModule::class,
    KeyStoreModule::class,
    HealthConnectModule::class,
    EncryptedSharedPreferencesModule::class,
    GeofencingModule::class
])
interface ApplicationComponent {
    fun inject(application: MyApplication)

    fun inject(receiver: BootReceiver)

    fun inject(receiver: GymGeofenceReceiver)

    fun mainActivityComponentBuilder(): MainActivityComponent.Builder
}