package com.kellinreaver.rubricislam.domain.usecase

import com.kellinreaver.rubricislam.domain.model.Qiblat
import com.kellinreaver.rubricislam.domain.repository.QiblatRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class GetQiblatDirectionUseCaseTest {

    private val repository: QiblatRepository = mockk()
    private val useCase = GetQiblatDirectionUseCase(repository)

    @Test
    fun `invoke returns qiblat direction from repository`() = runTest {
        val expected = Qiblat(bearing = 123.4f)
        every { repository.getQiblatDirection(1.0, 2.0) } returns flowOf(expected)

        val result = useCase(1.0, 2.0).toList()

        assertEquals(listOf(expected), result)
        verify { repository.getQiblatDirection(1.0, 2.0) }
    }
}
