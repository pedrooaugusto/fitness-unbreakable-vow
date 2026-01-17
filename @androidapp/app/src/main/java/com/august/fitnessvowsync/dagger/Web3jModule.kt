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
import com.august.fitnessvowsync.contract.AddressBalanceRetriever
import com.august.fitnessvowsync.contract.TheDoctor
import com.august.fitnessvowsync.helpers.SettingsService
import org.web3j.tx.gas.StaticGasProvider
import java.math.BigInteger

@Module
class Web3jModule {
    @Provides
    @Singleton
    fun provideFitnessUnbreakableVow(contractSettings: SettingsService): ContractProvider<FitnessUnbreakableVow> {
        return ContractProvider(contractSettings) { web3Settings ->
            val web3j = provideWeb3j(web3Settings.rpcEndpoint)
            val contractAddress = BuildConfig.FITNESS_UNBREAKABLE_VOW_ADDRESS
            val credentials = Credentials.create(web3Settings.credentials)
            val contractGasProvider = createGasProvider(web3j, web3Settings.gasPriceMarkUp, web3Settings.gasLimit)

            FitnessUnbreakableVow.load(contractAddress, web3j, credentials, contractGasProvider)
        }
    }

    @Provides
    @Singleton
    fun providePhysicalActivityOracle(contractSettings: SettingsService): ContractProvider<PhysicalActivityOracle> {
        return ContractProvider(contractSettings) { web3Settings ->
            val web3j = provideWeb3j(web3Settings.rpcEndpoint)
            val contractAddress = contractSettings.getPhysicalActivityRecordOracleAddress()
            val credentials = Credentials.create(web3Settings.credentials)
            val contractGasProvider = createGasProvider(web3j, web3Settings.gasPriceMarkUp, web3Settings.gasLimit)

            PhysicalActivityOracle.load(contractAddress, web3j, credentials, contractGasProvider)
        }
    }

    @Provides
    @Singleton
    fun provideTimeLord(contractSettings: SettingsService, oracle: ContractProvider<PhysicalActivityOracle>): ContractProvider<TheDoctor> {
        return ContractProvider(contractSettings) { web3Settings ->
            val web3j = provideWeb3j(web3Settings.rpcEndpoint)
            val contractAddress = oracle.get().TIME_LORD().send()!!
            val credentials = Credentials.create(web3Settings.credentials)
            val contractGasProvider = createGasProvider(web3j, web3Settings.gasPriceMarkUp, web3Settings.gasLimit)

            TheDoctor.load(contractAddress, web3j, credentials, contractGasProvider)
        }
    }

    @Provides
    @Singleton
    @Named("NETWORK")
    fun provideNetwork(): String {
        return BuildConfig.NETWORK;
    }

    @Provides
    @Singleton
    fun provideBalanceRetriever(contractSettings: SettingsService): AddressBalanceRetriever {
        return AddressBalanceRetriever(contractSettings)
    }

    private fun createGasProvider(web3j: Web3j, gasPriceMarkup: Long, gasLimit: Long): StaticGasProvider {
        val gasPrice = web3j.ethGasPrice().send().gasPrice
            .multiply(BigInteger.valueOf(gasPriceMarkup))
            .divide(BigInteger.valueOf(100))

        return StaticGasProvider(gasPrice, BigInteger.valueOf(gasLimit))
    }

    private fun provideWeb3j(rpcEndpoint: String): Web3j {
        return Web3j.build(HttpService(rpcEndpoint))
    }
}