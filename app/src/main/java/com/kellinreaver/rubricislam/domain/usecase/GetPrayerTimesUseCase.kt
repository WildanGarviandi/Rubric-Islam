package com.kellinreaver.rubricislam.domain.usecase

import com.kellinreaver.rubricislam.domain.model.PrayerTime
import com.kellinreaver.rubricislam.domain.repository.PrayerTimeRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class GetPrayerTimesUseCase @Inject constructor(private val repository: PrayerTimeRepository) {
    operator fun invoke(lat: Double, lon: Double): Flow<List<PrayerTime>> =
        repository.getPrayerTimes(lat, lon)
}
