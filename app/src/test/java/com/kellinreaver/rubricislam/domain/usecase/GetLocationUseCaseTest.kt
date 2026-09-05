package com.kellinreaver.rubricislam.domain.usecase

import com.kellinreaver.rubricislam.domain.repository.PrayerTimeRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class GetLocationUseCaseTest {

    private val repository: PrayerTimeRepository = mockk()
    private val useCase = GetLocationUseCase(repository)

    @Test
    fun `invoke returns current location from repository`() = runTest {
        val expected = LocationModel(1.0, 2.0)
        every { repository.getCurrentLocation() } returns flowOf(expected)

        val result = useCase().toList()

        assertEquals(listOf(expected), result)
        verify { repository.getCurrentLocation() }
    }
}
