package com.august.fitnessvowsync.contract

import com.august.fitnessvowsync.helpers.SettingsService
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.DefaultBlockParameterName
import org.web3j.protocol.http.HttpService
import java.math.BigInteger
import javax.inject.Inject

class AddressBalanceRetriever @Inject constructor(private val contractSettings: SettingsService) {
    private var web3j: Web3j? = null

    fun getWeiBalance(address: String): BigInteger {
        if (web3j == null) setUpWeb3j()

        val ethGetBalance = web3j!!.ethGetBalance(address, DefaultBlockParameterName.LATEST).send()

        return ethGetBalance.balance
    }

    private fun setUpWeb3j() {
        web3j = Web3j.build(HttpService(getRpcEndpoint()))
    }

    private fun getRpcEndpoint(): String {
        val endpoint = contractSettings.getRpcEndpoint()

        if (endpoint.isNullOrBlank()) throw RuntimeException("Unable to retrieve RPC endpoint.")

        return endpoint
    }
}