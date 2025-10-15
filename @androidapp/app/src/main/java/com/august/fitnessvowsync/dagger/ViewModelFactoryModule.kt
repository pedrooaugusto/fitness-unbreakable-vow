package com.august.fitnessvowsync.dagger

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.august.fitnessvowsync.donotuse.FakeDataProducerDoNotUse
import com.august.fitnessvowsync.service.GymVisitService
import com.august.fitnessvowsync.service.PhysicalActivityOracleService
import com.august.fitnessvowsync.service.SyncPhysicalActivityRecordService
import com.august.fitnessvowsync.ui.viewmodel.MainScreenViewModel
import dagger.Module
import dagger.Provides
import javax.inject.Named
import javax.inject.Singleton

@Module
class ViewModelFactoryModule {
    @Provides
    @Singleton
    @Named("MAIN_VIEW_MODEL")
    fun provideCounterViewModelFactory(
        oracleService: PhysicalActivityOracleService,
        syncPhysicalActivityService: SyncPhysicalActivityRecordService,
        gymVisitService: GymVisitService,
        donNotUse: FakeDataProducerDoNotUse?,
    ): ViewModelProvider.Factory {
        return GenericViewModelFactory({
            MainScreenViewModel(
                oracleService,
                syncPhysicalActivityService,
                gymVisitService,
                donNotUse
            )
        })
    }
}

class GenericViewModelFactory <T : ViewModel>(private val viewProvider: () -> T): ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return viewProvider.invoke() as T
    }
}