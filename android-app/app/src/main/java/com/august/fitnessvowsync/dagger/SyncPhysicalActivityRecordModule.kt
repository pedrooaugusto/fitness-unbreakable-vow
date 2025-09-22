package com.august.fitnessvowsync.dagger;

import android.content.SharedPreferences
import com.august.fitnessvowsync.mapper.PhysicalActivityRecordMapper
import com.august.fitnessvowsync.service.GymVisitService
import com.august.fitnessvowsync.service.HealthConnectAggregationService
import com.august.fitnessvowsync.service.PhysicalActivityOracleService
import com.august.fitnessvowsync.service.SyncPhysicalActivityRecordService
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
class SyncPhysicalActivityRecordModule {

    @Provides
    @Singleton
    fun provideSyncPhysicalActivityRecordService(
        encryptedPreferences: SharedPreferences,
        gymVisitService: GymVisitService,
        healthConnectAggregator: HealthConnectAggregationService,
        oracleService: PhysicalActivityOracleService.DefaultPhysicalActivityOracleService,
        recordMapper: PhysicalActivityRecordMapper
    ): SyncPhysicalActivityRecordService {
        return SyncPhysicalActivityRecordService.SyncPhysicalActivityRecordServiceImpl(
            healthConnectAggregator,
            oracleService,
            gymVisitService,
            recordMapper,
            encryptedPreferences
        )
    }
}
