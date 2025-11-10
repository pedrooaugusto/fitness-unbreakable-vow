package com.august.fitnessvowsync.dagger

import androidx.activity.ComponentActivity
import com.august.fitnessvowsync.MainActivity
import dagger.BindsInstance
import dagger.Subcomponent

@ActivityScope
@Subcomponent(modules = [MainActivityModule::class])
interface MainActivityComponent {
    fun inject(mainActivity: MainActivity)

    @Subcomponent.Builder
    interface Builder {
        fun activity(@BindsInstance activity: ComponentActivity): Builder
        fun build(): MainActivityComponent
    }
}
