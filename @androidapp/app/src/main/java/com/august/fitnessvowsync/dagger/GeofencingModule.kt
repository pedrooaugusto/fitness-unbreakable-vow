package com.august.fitnessvowsync.dagger

import android.content.Context
import android.content.SharedPreferences
import com.august.fitnessvowsync.geofencing.GymGeofenceCreator
import com.august.fitnessvowsync.helpers.NotificationService
import com.august.fitnessvowsync.service.GymVisitService
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.LocationServices
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
class GeofencingModule {
    @Provides
    @Singleton
    fun provideGeofencingClient(context: Context): GeofencingClient {
        return LocationServices.getGeofencingClient(context)
    }

    @Provides
    @Singleton
    fun provideGymVisitService(sharedPreferences: SharedPreferences, gymGeofenceCreator: GymGeofenceCreator, notificationService: NotificationService): GymVisitService {
        return GymVisitService.GymVisitServiceImpl(sharedPreferences, gymGeofenceCreator, notificationService)
    }
}