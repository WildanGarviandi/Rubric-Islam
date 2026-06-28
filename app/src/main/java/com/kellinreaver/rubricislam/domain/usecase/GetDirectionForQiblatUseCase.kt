package com.kellinreaver.rubricislam.domain.usecase

import com.kellinreaver.rubricislam.data.sensor.CompassSensorManager
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow

class GetDirectionForQiblatUseCase @Inject constructor(
    private val compassManager: CompassSensorManager
) {
    private val deviceHeading = compassManager.deviceHeading
    val deviceHeadingStateFlow: StateFlow<Float> = deviceHeading

    operator fun invoke() {
        compassManager.startListening()
    }

    fun stopListening() = compassManager.stopListening()
}
