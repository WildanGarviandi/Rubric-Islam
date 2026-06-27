package com.kellinreaver.rubricislam.domain.repository

import com.kellinreaver.rubricislam.domain.model.PrayerTime
import com.kellinreaver.rubricislam.domain.usecase.LocationModel
import kotlinx.coroutines.flow.Flow

interface PrayerTimeRepository {
    fun getCurrentLocation(): Flow<LocationModel>
    fun getPrayerTimes(lat: Double, lon: Double): Flow<List<PrayerTime>>
}
