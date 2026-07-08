package com.kellinreaver.rubricislam.ui.prayer

import android.util.Log
import app.cash.turbine.test
import com.kellinreaver.rubricislam.domain.model.PrayerTime
import com.kellinreaver.rubricislam.domain.usecase.GetLocationUseCase
import com.kellinreaver.rubricislam.domain.usecase.GetPrayerTimesUseCase
import com.kellinreaver.rubricislam.domain.usecase.LocationModel
import com.kellinreaver.rubricislam.domain.usecase.SchedulePrayerRemindersUseCase
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import java.time.LocalTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PrayerTimeViewModelTest {

    private val getPrayerTimesUseCase: GetPrayerTimesUseCase = mockk()
    private val getLocationUseCase: GetLocationUseCase = mockk()
    private val schedulePrayerRemindersUseCase: SchedulePrayerRemindersUseCase = mockk()
    private val testDispatcher = StandardTestDispatcher()

    private lateinit var viewModel: PrayerTimeViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        mockkStatic(Log::class)
        mockkStatic(LocalTime::class)
        mockkStatic(java.time.LocalDate::class)
        every { Log.i(any(), any()) } returns 0
        every { java.time.LocalDate.now() } returns java.time.LocalDate.of(2024, 1, 1)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkStatic(Log::class, LocalTime::class, java.time.LocalDate::class)
    }

    @Test
    fun `loadPrayerTimes should update uiState with prayer times and correct next prayer`() =
        runTest {
            // Arrange
            val location = LocationModel(latitude = 1.23, longitude = 4.56)
            val prayerTimes = listOf(
                PrayerTime("Fajr", "05:00"),
                PrayerTime("Dhuhr", "12:00"),
                PrayerTime("Asr", "15:30"),
                PrayerTime("Maghrib", "18:00"),
                PrayerTime("Isha", "19:30")
            )

            // Mock LocalTime.now() to be 13:00, so next prayer should be Asr (index 2)
            every { LocalTime.now() } returns LocalTime.of(13, 0)

            every { getLocationUseCase() } returns flowOf(location)
            every { getPrayerTimesUseCase(location.latitude, location.longitude) } returns flowOf(
                prayerTimes
            )

            // Act
            viewModel = PrayerTimeViewModel(
                getPrayerTimesUseCase,
                getLocationUseCase,
                schedulePrayerRemindersUseCase
            )

            // Assert
            viewModel.uiState.test {
                // 1. Initial State
                assertEquals(PrayerTimeUiState(), awaitItem())

                // 2. Loading State (from loadPrayerTimes)
                assertEquals(PrayerTimeUiState(isLoading = true), awaitItem())

                // 3. Final State with data
                val state = awaitItem()
                assertFalse(state.isLoading)
                assertEquals(5, state.prayerTimes.size)

                // Fajr
                assertFalse("Fajr should not be next", state.prayerTimes[0].isNext)
                // Dhuhr
                assertFalse("Dhuhr should not be next", state.prayerTimes[1].isNext)
                // Asr (Expected next at 13:00)
                assertTrue("Asr should be next", state.prayerTimes[2].isNext)

                assertEquals("Monday, 1 January 2024", state.todayDate)
            }
        }

    @Test
    fun `when current time is after last prayer, next prayer should be Fajr`() = runTest {
        // Arrange
        val location = LocationModel(latitude = 1.23, longitude = 4.56)
        val prayerTimes = listOf(
            PrayerTime("Fajr", "05:00"),
            PrayerTime("Isha", "19:30")
        )

        // Mock LocalTime.now() to be 21:00, so next prayer should be Fajr (index 0)
        every { LocalTime.now() } returns LocalTime.of(21, 0)

        every { getLocationUseCase() } returns flowOf(location)
        every { getPrayerTimesUseCase(location.latitude, location.longitude) } returns flowOf(
            prayerTimes
        )

        // Act
        viewModel = PrayerTimeViewModel(
            getPrayerTimesUseCase,
            getLocationUseCase,
            schedulePrayerRemindersUseCase
        )

        // Assert
        viewModel.uiState.test {
            skipItems(2) // Initial and Loading
            val state = awaitItem()
            assertTrue("Fajr should be next", state.prayerTimes[0].isNext)
            assertFalse("Isha should not be next", state.prayerTimes[1].isNext)
        }
    }
}
