package com.august.fitnessvowsync.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.august.fitnessvowsync.contract.PhysicalActivityOracle
import com.august.fitnessvowsync.contract.PhysicalActivityOracleService
import com.august.fitnessvowsync.helpers.SettingsService
import com.august.fitnessvowsync.physicalactivity.data.GymVisitTracker
import com.august.fitnessvowsync.physicalactivity.data.PhysicalActivityEventRepository

class DefaultSettingsScreenViewModel(
    private val physicalActivityOracleService: PhysicalActivityOracleService,
    private val settingsService: SettingsService,
    private val gymVisitTracker: GymVisitTracker,
    private val physicalActivityEventRepository: PhysicalActivityEventRepository
): ViewModel(), SettingsScreenViewModel {
    override fun settingsService(): SettingsService {
        return settingsService
    }

    override fun clearHistory() {
        gymVisitTracker.clearVisitHistory()
        physicalActivityEventRepository.clear()
    }

    override suspend fun emergencyPublicKeyChange(reason: String) {
        physicalActivityOracleService.emergencyChangePhysicalActivityPublisherPublicKey(reason)
    }

    override suspend fun arePublicKeysInSync(): Boolean {
        if (settingsService.getClientAccountPrivateKey().isNullOrEmpty()) return true

        val appPk = settingsService.getAppPublicKey()
        val oraclePk = physicalActivityOracleService.getOraclePublicKey()

        return samePublicKey(appPk, oraclePk)
    }

    private fun samePublicKey(key1: PhysicalActivityOracle.P256PublicKey, key2: PhysicalActivityOracle.P256PublicKey): Boolean {
        return key1.x.contentEquals(key2.x) && key1.y.contentEquals(key2.y)
    }
}

interface SettingsScreenViewModel {
    fun settingsService(): SettingsService
    fun clearHistory()
    suspend fun emergencyPublicKeyChange(reason: String)
    suspend fun arePublicKeysInSync(): Boolean
}

class PreviewSettingsScreenViewModel: SettingsScreenViewModel {
    override fun settingsService(): SettingsService {
        return SettingsService.PreviewSettingsService()
    }

    override fun clearHistory() {
        TODO("Not yet implemented")
    }

    override suspend fun emergencyPublicKeyChange(reason: String) {
        TODO("Not yet implemented")
    }

    override suspend fun arePublicKeysInSync(): Boolean {
        return true
    }

}