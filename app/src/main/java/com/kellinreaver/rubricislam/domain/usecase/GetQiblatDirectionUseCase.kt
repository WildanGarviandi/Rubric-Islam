package com.kellinreaver.rubricislam.domain.usecase

import com.kellinreaver.rubricislam.domain.model.Qiblat
import com.kellinreaver.rubricislam.domain.repository.QiblatRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetQiblatDirectionUseCase @Inject constructor(
    private val repository: QiblatRepository
) {
    operator fun invoke(lat: Double, lon: Double): Flow<Qiblat> {
        return repository.getQiblatDirection(lat, lon)
    }
}
