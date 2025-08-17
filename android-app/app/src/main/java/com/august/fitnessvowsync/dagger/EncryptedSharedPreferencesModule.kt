package com.august.fitnessvowsync.dagger

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import dagger.Module
import dagger.Provides
import javax.inject.Named
import javax.inject.Singleton

@Module
class EncryptedSharedPreferencesModule {
    @Provides
    @Singleton
    @Named("MAIN_KEY_ALIAS")
    fun provideMainKeyAlias(): String {
        return MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
    }

    @Provides
    @Singleton
    fun provideEncryptedSharedPreferences(context: Context, @Named("MAIN_KEY_ALIAS") mainKeyAlias: String): SharedPreferences {
        return EncryptedSharedPreferences.create(
            "FitVow.EncryptedPrefs2",
            mainKeyAlias,
            context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }
}