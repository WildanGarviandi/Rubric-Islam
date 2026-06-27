package com.kellinreaver.rubricislam.domain.usecase

import com.kellinreaver.rubricislam.domain.repository.PrayerTimeRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

data class LocationModel(
    val latitude: Double,
    val longitude: Double
)

class GetLocationUseCase @Inject constructor(
    private val prayerTimeRepository: PrayerTimeRepository
) {
    operator fun invoke(): Flow<LocationModel> {
        return prayerTimeRepository.getCurrentLocation()
    }
}
