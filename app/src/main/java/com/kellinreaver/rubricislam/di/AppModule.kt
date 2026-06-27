package com.kellinreaver.rubricislam.di

import android.content.Context
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.kellinreaver.rubricislam.data.remote.AladhanApiService
import com.kellinreaver.rubricislam.data.repository.PrayerTimeRepositoryImpl
import com.kellinreaver.rubricislam.data.repository.QiblatRepositoryImpl
import com.kellinreaver.rubricislam.data.repository.ReminderRepositoryImpl
import com.kellinreaver.rubricislam.domain.repository.PrayerTimeRepository
import com.kellinreaver.rubricislam.domain.repository.QiblatRepository
import com.kellinreaver.rubricislam.domain.repository.ReminderRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideAladhanApiService(): AladhanApiService {
        return Retrofit.Builder()
            .baseUrl("https://api.aladhan.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AladhanApiService::class.java)
    }
}

@Module
@InstallIn(SingletonComponent::class)
object LocationModule {
    @Provides
    @Singleton
    fun provideFusedLocationProviderClient(
        @ApplicationContext context: Context
    ): FusedLocationProviderClient {
        return LocationServices.getFusedLocationProviderClient(context)
    }
}

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun providePrayerTimeRepository(
        @ApplicationContext context: Context,
        apiService: AladhanApiService,
        fusedLocationProviderClient: FusedLocationProviderClient
    ): PrayerTimeRepository {
        return PrayerTimeRepositoryImpl(context, apiService, fusedLocationProviderClient)
    }

    @Provides
    @Singleton
    fun provideQiblatRepository(
        @ApplicationContext context: Context
    ): QiblatRepository {
        return QiblatRepositoryImpl(context)
    }

    @Provides
    @Singleton
    fun provideReminderRepository(
        @ApplicationContext context: Context
    ): ReminderRepository {
        return ReminderRepositoryImpl(context)
    }
}
