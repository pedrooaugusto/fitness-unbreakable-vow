package com.august.fitnessvowsync.dagger;

import com.august.fitnessvowsync.contract.ContractProvider
import com.august.fitnessvowsync.contract.PhysicalActivityOracle
import com.august.fitnessvowsync.contract.SignatureMapper
import com.august.fitnessvowsync.security.HardwareProtectedKeyService
import com.august.fitnessvowsync.contract.InterPlanetaryFileSystemService
import com.august.fitnessvowsync.contract.PhysicalActivityOracleService
import com.august.fitnessvowsync.physicalactivity.mapper.PhysicalActivityEventMapper
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
class PhysicalActivityRecordServicesModule {

    @Provides
    @Singleton
    fun providePhysicalActivityRecordOracleService(
        protectedKeyService: HardwareProtectedKeyService,
        signatureMapper: SignatureMapper,
        ipfsService: InterPlanetaryFileSystemService,
        physicalActivityOracle: ContractProvider<PhysicalActivityOracle>,
        physicalActivityEventMapper: PhysicalActivityEventMapper,
    ): PhysicalActivityOracleService {
        return PhysicalActivityOracleService(
            protectedKeyService,
            signatureMapper,
            ipfsService,
            physicalActivityOracle,
            physicalActivityEventMapper
        )
    }
}
