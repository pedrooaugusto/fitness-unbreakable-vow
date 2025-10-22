package com.august.fitnessvowsync.dagger

import android.content.SharedPreferences
import com.august.fitnessvowsync.contract.ContractSettingsService
import com.august.fitnessvowsync.mapper.OracleP256SignatureMapper
import com.august.fitnessvowsync.service.HardwareProtectedKeyService
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
        oracleP256SignatureMapper: OracleP256SignatureMapper
    ): ContractSettingsService {
        return ContractSettingsService.ContractSettingsServiceImpl(encryptedPreferences, protectedKeyService, oracleP256SignatureMapper)
    }
}