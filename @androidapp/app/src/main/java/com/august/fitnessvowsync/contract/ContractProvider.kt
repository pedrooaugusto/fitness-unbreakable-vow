package com.august.fitnessvowsync.contract

import android.util.Log
import com.august.fitnessvowsync.helpers.SettingsService
import org.web3j.tx.Contract
import java.util.function.Function

class ContractProvider<T: Contract> constructor(
    private val contractSettings: SettingsService,
    private val createContract: Function<Web3Settings, T>
) {
    private var contract: T? = null
    private var web3Settings: Web3Settings? = null

    @Synchronized
    fun get(): T {
        val currentWeb3Settings = getWeb3Settings()

        if (contract == null || currentWeb3Settings != web3Settings) {
            Log.i("FitVow", "Creating new contract client with rpc: ${currentWeb3Settings.rpcEndpoint} ")
            Log.i("FitVow", "Creating new contract client with credentials: ${currentWeb3Settings.credentials.substring(0, 10)}***")

            contract = createContract.apply(currentWeb3Settings)
            web3Settings = currentWeb3Settings
        }

        return contract!!
    }

    private fun getWeb3Settings(): Web3Settings {
        return Web3Settings(
            getCredentials(),
            getRpcEndpoint(),
            contractSettings.getGasLimit(),
            contractSettings.getGasPriceMarkUp()
        )
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

    data class Web3Settings(
        val credentials: String,
        val rpcEndpoint: String,
        val gasLimit: Long,
        val gasPriceMarkUp: Long,
    )
}