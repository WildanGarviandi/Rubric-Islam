package com.kellinreaver.rubricislam.di

import android.content.Context
import androidx.room.Room
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.kellinreaver.rubricislam.data.local.RubricDatabase
import com.kellinreaver.rubricislam.data.local.dao.PrayerTimeDao
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
import javax.inject.Singleton
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): RubricDatabase =
        Room.databaseBuilder(
            context,
            RubricDatabase::class.java,
            "rubric_islam.db"
        ).build()

    @Provides
    @Singleton
    fun providePrayerTimeDao(database: RubricDatabase): PrayerTimeDao = database.prayerTimeDao()
}

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides
    @Singleton
    fun provideAladhanApiService(): AladhanApiService = Retrofit
        .Builder()
        .baseUrl("https://api.aladhan.com/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(AladhanApiService::class.java)
}

@Module
@InstallIn(SingletonComponent::class)
object LocationModule {
    @Provides
    @Singleton
    fun provideFusedLocationProviderClient(
        @ApplicationContext context: Context
    ): FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)
}

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {
    @Provides
    @Singleton
    fun providePrayerTimeRepository(
        @ApplicationContext context: Context,
        apiService: AladhanApiService,
        fusedLocationProviderClient: FusedLocationProviderClient,
        prayerTimeDao: PrayerTimeDao
    ): PrayerTimeRepository =
        PrayerTimeRepositoryImpl(context, apiService, fusedLocationProviderClient, prayerTimeDao)

    @Provides
    @Singleton
    fun provideQiblatRepository(
        @ApplicationContext context: Context,
        fusedLocationProviderClient: FusedLocationProviderClient
    ): QiblatRepository = QiblatRepositoryImpl(context, fusedLocationProviderClient)

    @Provides
    @Singleton
    fun provideReminderRepository(@ApplicationContext context: Context): ReminderRepository =
        ReminderRepositoryImpl(context)
}
