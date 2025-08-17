package com.august.fitnessvowsync.dagger

import dagger.Module
import dagger.Provides
import java.security.KeyStore
import javax.inject.Singleton

@Module
class KeyStoreModule {
    @Provides
    @Singleton
    fun provideKeyStore(): KeyStore {
        return KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
    }
}