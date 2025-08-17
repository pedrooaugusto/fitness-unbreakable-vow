package com.august.fitnessvowsync

import android.app.Application
import android.util.Log
import com.august.fitnessvowsync.dagger.ApplicationComponent
import com.august.fitnessvowsync.dagger.ApplicationModule
import com.august.fitnessvowsync.dagger.DaggerApplicationComponent

class MyApplication: Application() {
    lateinit var appComponent: ApplicationComponent

    override fun onCreate() {
        super.onCreate()
        appComponent = DaggerApplicationComponent
            .builder()
            .applicationModule(ApplicationModule(this))
            .build()
        Log.d("FitnessVow - Sync", "App started!")
    }
}