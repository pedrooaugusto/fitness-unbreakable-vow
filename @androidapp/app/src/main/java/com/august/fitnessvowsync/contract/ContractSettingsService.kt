package com.august.fitnessvowsync.contract

import android.content.SharedPreferences
import android.util.Log
import com.august.fitnessvowsync.BuildConfig
import com.august.fitnessvowsync.service.AppPrivateKeyService
import javax.inject.Inject

interface ContractSettingsService {
    fun saveClientAccountPrivateKey(key: String)
    fun getClientAccountPrivateKey(): String?
    fun getPhysicalActivityRecordOracleAddress(): String
    fun getNetwork(): String
    fun getRpcEndpoint(): String?
    fun saveRpcEndpoint(rpcEndpoint: String)
    fun getRegisteredPublicKey(): String?

    class ContractSettingsServiceImpl @Inject constructor(
        private val encryptedPreferences: SharedPreferences,
        private val privateKeyService: AppPrivateKeyService,
    ): ContractSettingsService {
        companion object {
            const val WALLET_PRIVATE_KEY_PREF_KEY = "WALLET_PRIVATE_KEY_PREF_KEY"
            const val RPC_ENDPOINT_PREF_KEY = "RPC_ENDPOINT_PREF_KEY"
        }

        // Account that will be used to interact with any contracts deployed in the blockchain.
        override fun saveClientAccountPrivateKey(key: String) {
            Log.i("FitVow", "Saving app client account private key: ${key.substring(0, 10)}***")

            with(encryptedPreferences.edit()) {
                putString(WALLET_PRIVATE_KEY_PREF_KEY, key)
                apply()
            }
        }

        override fun getClientAccountPrivateKey(): String? {
            return encryptedPreferences.getString(WALLET_PRIVATE_KEY_PREF_KEY, null)
        }

        override fun getPhysicalActivityRecordOracleAddress(): String {
            return BuildConfig.PHYSICAL_ACTIVITY_ORACLE_ADDRESS
        }

        override fun getNetwork(): String {
            return BuildConfig.NETWORK
        }

        override fun saveRpcEndpoint(rpcEndpoint: String) {
            Log.i("FitVow", "Saving RPC endpoint: $rpcEndpoint")

            with(encryptedPreferences.edit()) {
                putString(RPC_ENDPOINT_PREF_KEY, rpcEndpoint)
                apply()
            }
        }

        override fun getRpcEndpoint(): String? {
            return encryptedPreferences.getString(RPC_ENDPOINT_PREF_KEY, BuildConfig.RPC_URL)
        }

        override fun getRegisteredPublicKey(): String? {
            return try {
                privateKeyService.getPublicKey()
            } catch (ex: IllegalStateException) {
                Log.e("FitVow", "Unable to retrieve public ket", ex)

                null;
            }
        }
    }

    class PreviewContractSettingsService: ContractSettingsService {
        override fun saveClientAccountPrivateKey(key: String) {
            TODO("Not yet implemented")
        }

        override fun getClientAccountPrivateKey(): String? {
            return "0x0000000000000000000000000000000000000"
        }

        override fun getPhysicalActivityRecordOracleAddress(): String {
            return "0x0000000000000000000000000000000000000"
        }

        override fun getNetwork(): String {
            return "localhost"
        }

        override fun getRegisteredPublicKey(): String? {
            return "Op0OFDG4456J90+-545M4656GE65k0-;0-566904N5GFDF67CM90"
        }

        override fun getRpcEndpoint(): String? {
            return "https://arb1.io"
        }

        override fun saveRpcEndpoint(rpcEndpoint: String) {}
    }
}