package com.august.fitnessvowsync.dagger

import android.content.SharedPreferences
import com.august.fitnessvowsync.contract.ContractSettingsService
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
class SettingsModule {
    @Provides
    @Singleton
    fun provideContractSettingsService(encryptedPreferences: SharedPreferences): ContractSettingsService {
        return ContractSettingsService.ContractSettingsServiceImpl(encryptedPreferences)
    }
}