package com.august.fitnessvowsync.contract

import androidx.core.util.Function
import org.web3j.crypto.Credentials
import org.web3j.tx.Contract

class ContractProvider<T: Contract> constructor(
    private val contractSettings: ContractSettingsService,
    private val createContract: Function<Credentials, T>
) {
    private var contract: T? = null

    @Synchronized
    fun get(): T {
        if (contract == null) {
            contract = createContract.apply(getCredentials())
        }

        return contract!!
    }
    private fun getCredentials(): Credentials {
        val walletKey = contractSettings.getClientAccountPrivateKey()

        if (walletKey.isNullOrBlank()) throw RuntimeException("Unable to create credentials. Wallet Private key not provided.")

        return Credentials.create(walletKey)
    }
}