package com.august.fitnessvowsync.dagger

import android.content.SharedPreferences
import com.august.fitnessvowsync.contract.ContractSettingsService
import com.august.fitnessvowsync.service.AppPrivateKeyService
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
class SettingsModule {
    @Provides
    @Singleton
    fun provideContractSettingsService(encryptedPreferences: SharedPreferences, privateKeyService: AppPrivateKeyService): ContractSettingsService {
        return ContractSettingsService.ContractSettingsServiceImpl(encryptedPreferences, privateKeyService)
    }
}