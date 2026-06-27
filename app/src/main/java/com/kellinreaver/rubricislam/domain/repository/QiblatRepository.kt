package com.kellinreaver.rubricislam.domain.repository

import com.kellinreaver.rubricislam.domain.model.Qiblat
import kotlinx.coroutines.flow.Flow

interface QiblatRepository {
    fun getQiblatDirection(lat: Double, lon: Double): Flow<Qiblat>
}
