package com.august.fitnessvowsync.dagger

import com.august.fitnessvowsync.contract.ContractProvider
import com.august.fitnessvowsync.contract.FitnessUnbreakableVow
import com.august.fitnessvowsync.contract.PhysicalActivityOracle
import dagger.Module
import dagger.Provides
import org.web3j.crypto.Credentials
import org.web3j.protocol.Web3j
import org.web3j.protocol.http.HttpService
import org.web3j.tx.gas.DefaultGasProvider
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
    fun provideFitnessUnbreakableVow(
        contractSettings: ContractSettingsService,
        web3j: Web3j,
    ): ContractProvider<FitnessUnbreakableVow> {
        val createContract = { credentials: Credentials -> FitnessUnbreakableVow.load(
            BuildConfig.FITNESS_UNBREAKABLE_VOW_ADDRESS,
            web3j,
            credentials,
            createGasProvider(web3j)
        )}

        return ContractProvider(contractSettings, createContract)
    }

    @Provides
    @Singleton
    fun providePhysicalActivityOracle(
        contractSettings: ContractSettingsService,
        web3j: Web3j,
    ): ContractProvider<PhysicalActivityOracle> {
        val createContract = { credentials: Credentials -> PhysicalActivityOracle.load(
            BuildConfig.PHYSICAL_ACTIVITY_ORACLE_ADDRESS,
            web3j,
            credentials,
            createGasProvider(web3j)
        )}

        return ContractProvider(contractSettings, createContract)
    }

    fun createGasProvider(web3j: Web3j): StaticGasProvider {
        // +10%
        val gasPrice = web3j.ethGasPrice().send().gasPrice
            .multiply(BigInteger.valueOf(110))
            .divide(BigInteger.valueOf(100))

        return StaticGasProvider(gasPrice, BigInteger.valueOf(2_000_000))
    }

    @Provides
    @Singleton
    fun provideWeb3j(@Named("NETWORK_RPC_URL") networkRpcUrl: String): Web3j {
        return Web3j.build(HttpService(networkRpcUrl))
    }

    @Provides
    @Singleton
    @Named("NETWORK_RPC_URL")
    fun provideNetworkRpcUrl(): String {
        // TODO: Add more options in case one url goes down
        return BuildConfig.RPC_URL;
    }

    @Provides
    @Singleton
    @Named("NETWORK")
    fun provideNetwork(): String {
        return BuildConfig.NETWORK;
    }
}