package com.august.fitnessvowsync.dagger

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
class HealthConnectModule {
    @Provides
    @Singleton
    fun provideHealthConnectClient(context: Context): HealthConnectClient {
        return HealthConnectClient.getOrCreate(context)
    }
}