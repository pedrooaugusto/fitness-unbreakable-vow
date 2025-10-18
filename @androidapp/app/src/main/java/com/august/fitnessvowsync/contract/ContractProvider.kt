package com.august.fitnessvowsync.contract

import android.util.Log
import org.web3j.tx.Contract
import java.util.function.BiFunction

class ContractProvider<T: Contract> constructor(
    private val contractSettings: ContractSettingsService,
    private val createContract: BiFunction<String, String, T>
) {
    private var contract: T? = null
    private var credentials: String? = null
    private var rpcEndpoint: String? = null

    @Synchronized
    fun get(): T {
        val currentCredentials = getCredentials()
        val currentRpcEndpoint = getRpcEndpoint()

        if (contract == null || currentCredentials != credentials || currentRpcEndpoint != rpcEndpoint) {
            Log.i("FitVow", "Creating new contract client with rpc: $currentRpcEndpoint")
            Log.i("FitVow", "Creating new contract client with credentials: ${currentCredentials.substring(0, 10)}***")

            contract = createContract.apply(currentCredentials, currentRpcEndpoint)

            credentials = currentCredentials
            rpcEndpoint = currentRpcEndpoint
        }

        return contract!!
    }
    private fun getCredentials(): String {
        val walletKey = contractSettings.getClientAccountPrivateKey()

        if (walletKey.isNullOrBlank()) throw RuntimeException("Unable to create credentials. Wallet Private key not provided.")

        return walletKey
    }

    private fun getRpcEndpoint(): String {
        val endpoint = contractSettings.getRpcEndpoint()

        if (endpoint.isNullOrBlank()) throw RuntimeException("Unable to retrieve RPC endpoint.")

        return endpoint
    }
}