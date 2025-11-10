package com.august.fitnessvowsync.dagger

import android.content.Context
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
}