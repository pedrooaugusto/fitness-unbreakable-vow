package com.august.fitnessvowsync.dagger

import com.august.fitnessvowsync.MainActivity
import dagger.Subcomponent

@Subcomponent(modules = [])
interface MainActivityComponent {
    fun inject(mainActivity: MainActivity)

    @Subcomponent.Builder
    interface Builder {
        fun build(): MainActivityComponent
    }
}