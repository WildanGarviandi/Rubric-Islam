package com.kellinreaver.rubricislam.ui.prayer

import android.util.Log
import com.kellinreaver.rubricislam.domain.model.PrayerTime
import com.kellinreaver.rubricislam.domain.usecase.GetLocationUseCase
import com.kellinreaver.rubricislam.domain.usecase.GetPrayerTimesUseCase
import com.kellinreaver.rubricislam.domain.usecase.LocationModel
import com.kellinreaver.rubricislam.domain.usecase.SchedulePrayerRemindersUseCase
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
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
    private val getPrayerTimesUseCase = mockk<GetPrayerTimesUseCase>()
    private val getLocationUseCase = mockk<GetLocationUseCase>()
    private val schedulePrayerRemindersUseCase =
        mockk<SchedulePrayerRemindersUseCase>(relaxed = true)

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        mockkStatic(Log::class)
        every { Log.i(any(), any()) } returns 0
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkStatic(Log::class)
    }

    @Test
    fun `loadPrayerTimes updates state with next prayer and date`() = runTest {
        val currentTime = LocalTime.now()
        val timeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())
        val prayerTimes = listOf(
            PrayerTime("Fajr", currentTime.minusMinutes(30).format(timeFormatter)),
            PrayerTime("Dhuhr", currentTime.plusMinutes(30).format(timeFormatter)),
            PrayerTime("Asr", currentTime.plusHours(2).format(timeFormatter))
        )
        every { getLocationUseCase() } returns flowOf(LocationModel(1.0, 2.0))
        every { getPrayerTimesUseCase(1.0, 2.0) } returns flowOf(prayerTimes)

        val viewModel = PrayerTimeViewModel(
            getPrayerTimesUseCase = getPrayerTimesUseCase,
            getLocationUseCase = getLocationUseCase,
            schedulePrayerRemindersUseCase = schedulePrayerRemindersUseCase
        )

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(1, state.prayerTimes.count { it.isNext })
        assertTrue(state.prayerTimes.any { it.name == "Dhuhr" && it.isNext })
        assertTrue(state.todayDate.isNotEmpty())
        coVerify(exactly = 1) { schedulePrayerRemindersUseCase(prayerTimes) }
    }

    @Test
    fun `loadPrayerTimes with empty list still schedules reminders and updates state`() = runTest {
        every { getLocationUseCase() } returns flowOf(LocationModel(1.0, 2.0))
        every { getPrayerTimesUseCase(1.0, 2.0) } returns flowOf(emptyList())

        val viewModel = PrayerTimeViewModel(
            getPrayerTimesUseCase = getPrayerTimesUseCase,
            getLocationUseCase = getLocationUseCase,
            schedulePrayerRemindersUseCase = schedulePrayerRemindersUseCase
        )

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertTrue(state.prayerTimes.isEmpty())
        assertTrue(state.todayDate.isNotEmpty())
        coVerify(exactly = 1) { schedulePrayerRemindersUseCase(emptyList()) }
    }
}
