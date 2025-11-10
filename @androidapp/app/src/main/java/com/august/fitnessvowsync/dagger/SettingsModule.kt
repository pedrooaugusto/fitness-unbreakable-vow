package com.august.fitnessvowsync.dagger

import android.content.SharedPreferences
import com.august.fitnessvowsync.helpers.SettingsService
import com.august.fitnessvowsync.contract.SignatureMapper
import com.august.fitnessvowsync.security.HardwareProtectedKeyService
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
class SettingsModule {
    @Provides
    @Singleton
    fun provideContractSettingsService(
        encryptedPreferences: SharedPreferences,
        protectedKeyService: HardwareProtectedKeyService,
        signatureMapper: SignatureMapper
    ): SettingsService {
        return SettingsService.SettingsServiceImpl(encryptedPreferences, protectedKeyService, signatureMapper)
    }
}