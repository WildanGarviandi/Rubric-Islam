package com.kellinreaver.rubricislam.domain.usecase

import com.kellinreaver.rubricislam.data.sensor.CompassSensorManager
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class GetDirectionForQiblatUseCaseTest {

    private val compassManager: CompassSensorManager = mockk(relaxed = true)

    @Test
    fun `deviceHeadingStateFlow exposes compass manager heading`() = runTest {
        val expected = MutableStateFlow(42.0f)
        every { compassManager.deviceHeading } returns expected

        val useCase = GetDirectionForQiblatUseCase(compassManager)

        assertEquals(expected, useCase.deviceHeadingStateFlow)
    }

    @Test
    fun `invoke starts compass listening`() {
        val useCase = GetDirectionForQiblatUseCase(compassManager)

        useCase.invoke()

        verify { compassManager.startListening() }
    }

    @Test
    fun `stopListening stops compass listening`() {
        val useCase = GetDirectionForQiblatUseCase(compassManager)

        useCase.stopListening()

        verify { compassManager.stopListening() }
    }
}
