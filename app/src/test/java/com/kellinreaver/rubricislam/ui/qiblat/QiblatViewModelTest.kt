package com.kellinreaver.rubricislam.ui.qiblat

import com.kellinreaver.rubricislam.domain.model.Qiblat
import com.kellinreaver.rubricislam.domain.usecase.GetDirectionForQiblatUseCase
import com.kellinreaver.rubricislam.domain.usecase.GetLocationUseCase
import com.kellinreaver.rubricislam.domain.usecase.GetQiblatDirectionUseCase
import com.kellinreaver.rubricislam.domain.usecase.LocationModel
import io.mockk.MockKAnnotations
import io.mockk.Runs
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.just
import io.mockk.verify
import java.lang.reflect.Method
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@ExperimentalCoroutinesApi
class QiblatViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @MockK
    private lateinit var getQiblatDirectionUseCase: GetQiblatDirectionUseCase

    @MockK
    private lateinit var getLocationUseCase: GetLocationUseCase

    @MockK
    private lateinit var getDirectionForQiblatUseCase: GetDirectionForQiblatUseCase

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun defaultMocks(
        locationFlow: kotlinx.coroutines.flow.Flow<LocationModel> = flowOf(LocationModel(0.0, 0.0)),
        directionFlow: kotlinx.coroutines.flow.Flow<Qiblat> = flowOf(Qiblat(0.0f)),
        deviceHeadingFlow: MutableStateFlow<Float> = MutableStateFlow(0.0f)
    ) {
        every { getLocationUseCase() } returns locationFlow
        every { getQiblatDirectionUseCase.invoke(any(), any()) } returns directionFlow
        every { getDirectionForQiblatUseCase.deviceHeadingStateFlow } returns deviceHeadingFlow
        every { getDirectionForQiblatUseCase.invoke() } just Runs
        every { getDirectionForQiblatUseCase.stopListening() } just Runs
    }

    private fun createViewModel(): QiblatViewModel = QiblatViewModel(
        getQiblatDirectionUseCase,
        getLocationUseCase,
        getDirectionForQiblatUseCase
    )

    @Test
    fun `initial ui state is loading and has no error`() = runTest {
        defaultMocks(locationFlow = flow { })

        val qiblatViewModel = createViewModel()

        assertTrue(qiblatViewModel.uiState.value.isLoading)
        assertNull(qiblatViewModel.uiState.value.error)
        assertEquals(0.0f, qiblatViewModel.uiState.value.deviceHeading, 0.0001f)
    }

    @Test
    fun `loadQiblatDirection updates ui state with direction and stops loading`() = runTest {
        // Given
        val mockLocation = LocationModel(0.0, 0.0)
        val mockQiblat = Qiblat(bearing = 45.0f)

        defaultMocks(
            locationFlow = flowOf(mockLocation),
            directionFlow = flowOf(mockQiblat)
        )
        val qiblatViewModel = createViewModel()

        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(!qiblatViewModel.uiState.value.isLoading)
        assertEquals(45.0f, qiblatViewModel.uiState.value.direction, 0.0001f)
        assertNull(qiblatViewModel.uiState.value.error)
    }

    @Test
    fun `observeDeviceHeading updates ui state with device heading`() = runTest {
        // Given
        val deviceHeadingFlow = MutableStateFlow(90.0f)

        defaultMocks(deviceHeadingFlow = deviceHeadingFlow)
        val qiblatViewModel = createViewModel()

        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(90.0f, qiblatViewModel.uiState.value.deviceHeading, 0.0001f)
    }

    @Test
    fun `onCleared stops listening to device heading`() {
        defaultMocks()
        val qiblatViewModel = createViewModel()

        val method: Method = QiblatViewModel::class.java.getDeclaredMethod("onCleared")
        method.isAccessible = true
        method.invoke(qiblatViewModel)

        verify { getDirectionForQiblatUseCase.stopListening() }
    }

    @Test
    fun `loadQiblatDirection updates ui state with error when location use case fails`() = runTest {
        // Given
        val errorMessage = "Location access failed"

        defaultMocks(locationFlow = flow { error(errorMessage) })
        val qiblatViewModel = createViewModel()

        // When
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertTrue(!qiblatViewModel.uiState.value.isLoading)
        assertEquals(errorMessage, qiblatViewModel.uiState.value.error)
    }

    @Test
    fun `loadQiblatDirection updates direction when location emits multiple values`() = runTest {
        // Given
        val locationFlow = MutableStateFlow(LocationModel(0.0, 0.0))

        every { getLocationUseCase() } returns locationFlow
        every { getQiblatDirectionUseCase.invoke(any(), any()) } returnsMany listOf(
            flowOf(Qiblat(10.0f)),
            flowOf(Qiblat(20.0f))
        )
        every { getDirectionForQiblatUseCase.deviceHeadingStateFlow } returns MutableStateFlow(0.0f)
        every { getDirectionForQiblatUseCase.invoke() } just Runs
        every { getDirectionForQiblatUseCase.stopListening() } just Runs

        val qiblatViewModel = createViewModel()

        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(10.0f, qiblatViewModel.uiState.value.direction, 0.0001f)

        locationFlow.value = LocationModel(1.0, 1.0)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(20.0f, qiblatViewModel.uiState.value.direction, 0.0001f)
    }
}
