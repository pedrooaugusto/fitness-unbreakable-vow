package com.august.fitnessvowsync.contract

import android.content.SharedPreferences
import android.util.Log
import javax.inject.Inject
import kotlin.apply

interface ContractSettingsService {
    fun saveClientAccountPrivateKey(key: String)
    fun getClientAccountPrivateKey(): String?

    class ContractSettingsServiceImpl @Inject constructor(private val encryptedPreferences: SharedPreferences): ContractSettingsService {
        companion object { const val WALLET_PRIVATE_KEY_PREF_KEY = "WALLET_PRIVATE_KEY_PREF_KEY"; }

        // Account that will be used to interact with any contracts deployed in the blockchain.
        override fun saveClientAccountPrivateKey(key: String) {
            Log.i("FitVow", "Saving app client account private key: $key")

            with(encryptedPreferences.edit()) {
                putString(WALLET_PRIVATE_KEY_PREF_KEY, key)
                apply()
            }
        }

        override fun getClientAccountPrivateKey(): String? {
            return encryptedPreferences.getString(WALLET_PRIVATE_KEY_PREF_KEY, null)
        }
    }

    class PreviewContractSettingsService: ContractSettingsService {
        override fun saveClientAccountPrivateKey(key: String) {
            TODO("Not yet implemented")
        }

        override fun getClientAccountPrivateKey(): String? {
            TODO("Not yet implemented")
        }
    }
}