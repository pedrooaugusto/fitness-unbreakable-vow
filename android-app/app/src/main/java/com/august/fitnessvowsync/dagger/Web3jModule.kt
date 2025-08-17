package com.august.fitnessvowsync.dagger
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

@Module
class Web3jModule {
    @Provides
    @Singleton
    fun provideFitnessUnbreakableVow(credentials: Credentials, web3: Web3j): FitnessUnbreakableVow {
        return FitnessUnbreakableVow.load(
            "0x3c5792ca76248062188e35f9f8b61af3e1c4cc33",
            web3,
            credentials,
            DefaultGasProvider()
        )
    }

    @Provides
    @Singleton
    fun providePhysicalActivityOracle(credentials: Credentials, web3: Web3j): PhysicalActivityOracle {
        return PhysicalActivityOracle.load(
            "0xa8C7266038CBceab8ed7ad3253aB0a0f28172D37",
            web3,
            credentials,
            DefaultGasProvider()
        )
    }

    @Provides
    @Singleton
    fun provideWeb3j(@Named("NETWORK_RPC_URL") networkRpcUrl: String): Web3j {
        return Web3j.build(HttpService(networkRpcUrl))
    }

    @Provides
    @Singleton
    @Named("NETWORK_RPC_URL")
    fun provideNetworkRpcUrl(@Named("NETWORK") networkName: String): String {
        return BuildConfig.RPC_URL;
    }

    @Provides
    @Singleton
    fun provideWalletCredentials(@Named("NETWORK") networkName: String): Credentials {
        return Credentials.create(BuildConfig.WALLET_PRIVATE_KEY);
    }

    @Provides
    @Singleton
    @Named("NETWORK")
    fun provideNetwork(): String {
        return "SEPOLIA"
    }
}