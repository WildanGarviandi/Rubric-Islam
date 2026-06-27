package com.kellinreaver.rubricislam.domain.usecase

import com.kellinreaver.rubricislam.domain.model.Qiblat
import com.kellinreaver.rubricislam.domain.repository.QiblatRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class GetQiblatDirectionUseCase
@Inject
constructor(private val repository: QiblatRepository) {
    operator fun invoke(lat: Double, lon: Double): Flow<Qiblat> =
        repository.getQiblatDirection(lat, lon)
}
