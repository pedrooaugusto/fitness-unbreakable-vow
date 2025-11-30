package com.august.fitnessvowsync.helpers

import android.content.SharedPreferences
import android.util.Log
import com.august.fitnessvowsync.BuildConfig
import com.august.fitnessvowsync.contract.SignatureMapper
import com.august.fitnessvowsync.security.HardwareProtectedKeyService
import javax.inject.Inject

interface SettingsService {
    fun saveClientAccountPrivateKey(key: String)
    fun getClientAccountPrivateKey(): String?
    fun getPhysicalActivityRecordOracleAddress(): String
    fun getNetwork(): String
    fun getRpcEndpoint(): String?
    fun getGasLimit(): Long
    fun getGasPriceMarkUp(): Long
    fun saveRpcEndpoint(rpcEndpoint: String)
    fun getAppFormattedPublicKey(): String?
    fun getPinataApiToken(): String?
    fun savePinataApiToken(token: String)
    fun saveGasLimit(limit: Long)
    fun saveGasPriceMarkUp(markup: Long)

    class SettingsServiceImpl @Inject constructor(
        private val encryptedPreferences: SharedPreferences,
        private val protectedKeyService: HardwareProtectedKeyService,
        private val signatureMapper: SignatureMapper,
    ): SettingsService {
        companion object {
            const val WALLET_PRIVATE_KEY_PREF_KEY = "WALLET_PRIVATE_KEY_PREF_KEY"
            const val RPC_ENDPOINT_PREF_KEY = "RPC_ENDPOINT_PREF_KEY"
            const val PINATA_API_TOKEN = "PINATA_API_TOKEN"
            const val GAS_LIMIT = "GAS_LIMIT"
            const val GAS_PRICE_MARKUP = "GAS_PRICE_MARKUP"
        }

        // Account that will be used to interact with any contracts deployed in the blockchain.
        override fun saveClientAccountPrivateKey(key: String) {
            Log.i("FitVow - Sync", "Saving app client account private key: ${key.substring(0, 10)}***")

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
            Log.i("FitVow - Sync", "Saving RPC endpoint: $rpcEndpoint")

            with(encryptedPreferences.edit()) {
                putString(RPC_ENDPOINT_PREF_KEY, rpcEndpoint)
                apply()
            }
        }

        override fun getRpcEndpoint(): String? {
            return encryptedPreferences.getString(RPC_ENDPOINT_PREF_KEY, null)
        }

        override fun getGasLimit(): Long {
            return encryptedPreferences.getLong(GAS_LIMIT, 3_000_000)
        }

        override fun getGasPriceMarkUp(): Long {
            return encryptedPreferences.getLong(GAS_PRICE_MARKUP, 150)
        }

        override fun getAppFormattedPublicKey(): String? {
            return try {
                signatureMapper
                    .toP256PublicKey(protectedKeyService.getPublicKey())
                    .let { "x: ${it.x.toHexString()}; y: ${it.y.toHexString()}" }
            } catch (ex: IllegalStateException) {
                null
            }
        }

        override fun getPinataApiToken(): String? {
            return encryptedPreferences.getString(PINATA_API_TOKEN, null)
        }

        override fun savePinataApiToken(token: String) {
            Log.i("FitVow - Sync", "Saving Pinata Api Token.")

            with(encryptedPreferences.edit()) {
                putString(PINATA_API_TOKEN, token)
                apply()
            }
        }

        override fun saveGasLimit(limit: Long) {
            Log.i("FitVow - Sync", "Saving gas limit: $limit")

            with(encryptedPreferences.edit()) {
                putLong(GAS_LIMIT, limit)
                apply()
            }
        }

        override fun saveGasPriceMarkUp(markup: Long) {
            Log.i("FitVow - Sync", "Saving gas price markup: $markup")

            with(encryptedPreferences.edit()) {
                putLong(GAS_PRICE_MARKUP, markup)
                apply()
            }
        }

        private fun ByteArray.toHexString(): String {
            return this.joinToString("") { "%02x".format(it) }
        }
    }

    class PreviewSettingsService: SettingsService {
        override fun saveClientAccountPrivateKey(key: String) {
            error("mock")
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

        override fun getAppFormattedPublicKey(): String? {
            return "Op0OFDG4456J90+-545M4656GE65k0-;0-566904N5GFDF67CM90"
        }

        override fun getPinataApiToken(): String? {
            return "hello-world"
        }

        override fun savePinataApiToken(token: String) {
            error("mock")
        }

        override fun saveGasLimit(limit: Long) {
            error("mock")
        }

        override fun saveGasPriceMarkUp(markup: Long) {
            error("mock")
        }

        override fun getRpcEndpoint(): String? {
            return "https://arb1.io"
        }

        override fun getGasLimit(): Long {
            return 3_000_000
        }

        override fun getGasPriceMarkUp(): Long {
            return 150
        }

        override fun saveRpcEndpoint(rpcEndpoint: String) {}
    }
}