package com.august.fitnessvowsync.dagger

import com.august.fitnessvowsync.contract.ContractProvider
import com.august.fitnessvowsync.contract.FitnessUnbreakableVow
import com.august.fitnessvowsync.contract.PhysicalActivityOracle
import dagger.Module
import dagger.Provides
import org.web3j.crypto.Credentials
import org.web3j.protocol.Web3j
import org.web3j.protocol.http.HttpService
import javax.inject.Named
import javax.inject.Singleton
import com.august.fitnessvowsync.BuildConfig
import com.august.fitnessvowsync.contract.ContractSettingsService
import org.web3j.tx.gas.StaticGasProvider
import java.math.BigInteger

@Module
class Web3jModule {
    @Provides
    @Singleton
    fun provideFitnessUnbreakableVow(contractSettings: ContractSettingsService): ContractProvider<FitnessUnbreakableVow> {
        return ContractProvider(contractSettings) { walletKey, rpcEndpoint ->
            val web3j = provideWeb3j(rpcEndpoint)
            val contractAddress = BuildConfig.FITNESS_UNBREAKABLE_VOW_ADDRESS
            val credentials = Credentials.create(walletKey)
            val contractGasProvider = createGasProvider(web3j)

            FitnessUnbreakableVow.load(contractAddress, web3j, credentials, contractGasProvider)
        }
    }

    @Provides
    @Singleton
    fun providePhysicalActivityOracle(contractSettings: ContractSettingsService): ContractProvider<PhysicalActivityOracle> {
        return ContractProvider(contractSettings) { walletKey, rpcEndpoint ->
            val web3j = provideWeb3j(rpcEndpoint)
            val contractAddress = BuildConfig.PHYSICAL_ACTIVITY_ORACLE_ADDRESS
            val credentials = Credentials.create(walletKey)
            val contractGasProvider = createGasProvider(web3j)

            PhysicalActivityOracle.load(contractAddress, web3j, credentials, contractGasProvider)
        }
    }

    @Provides
    @Singleton
    @Named("NETWORK_RPC_URL")
    fun provideNetworkRpcUrl(): String {
        return BuildConfig.RPC_URL;
    }

    @Provides
    @Singleton
    @Named("NETWORK")
    fun provideNetwork(): String {
        return BuildConfig.NETWORK;
    }

    private fun createGasProvider(web3j: Web3j): StaticGasProvider {
        // +10%
        val gasPrice = web3j.ethGasPrice().send().gasPrice
            .multiply(BigInteger.valueOf(110))
            .divide(BigInteger.valueOf(100))

        return StaticGasProvider(gasPrice, BigInteger.valueOf(2_000_000))
    }

    private fun provideWeb3j(rpcEndpoint: String): Web3j {
        return Web3j.build(HttpService(rpcEndpoint))
    }
}