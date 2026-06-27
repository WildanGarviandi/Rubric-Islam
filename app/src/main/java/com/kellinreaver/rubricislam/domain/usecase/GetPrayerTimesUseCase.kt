package com.kellinreaver.rubricislam.domain.usecase

import com.kellinreaver.rubricislam.domain.model.PrayerTime
import com.kellinreaver.rubricislam.domain.repository.PrayerTimeRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetPrayerTimesUseCase @Inject constructor(
    private val repository: PrayerTimeRepository
) {
    operator fun invoke(lat: Double, lon: Double): Flow<List<PrayerTime>> {
        return repository.getPrayerTimes(lat, lon)
    }
}
