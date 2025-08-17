package com.august.fitnessvowsync.dagger

import android.content.Context
import android.content.SharedPreferences
import com.august.fitnessvowsync.service.GeofencingService
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
    fun provideGeofencingService(sharedPreferences: SharedPreferences): GeofencingService {
        return GeofencingService(sharedPreferences)
    }
}