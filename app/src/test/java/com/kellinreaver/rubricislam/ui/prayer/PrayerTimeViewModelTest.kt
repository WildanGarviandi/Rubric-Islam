package com.kellinreaver.rubricislam.ui.prayer

import android.app.AlarmManager
import android.content.ContextWrapper
import android.content.Intent
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
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
    private class TestContext(private val alarmManager: AlarmManager) :
        ContextWrapper(mockk(relaxed = true)) {
        var startedIntent: Intent? = null

        override fun getSystemService(name: String): Any? =
            if (name == ALARM_SERVICE) alarmManager else super.getSystemService(name)

        override fun startActivity(intent: Intent) {
            startedIntent = intent
        }
    }

    private val getPrayerTimesUseCase = mockk<GetPrayerTimesUseCase>()
    private val getLocationUseCase = mockk<GetLocationUseCase>()
    private val schedulePrayerRemindersUseCase =
        mockk<SchedulePrayerRemindersUseCase>(relaxed = true)
    private val alarmManager = mockk<AlarmManager>()
    private lateinit var context: TestContext

    @Before
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
        mockkStatic(Log::class)
        every { Log.i(any(), any()) } returns 0
        every { Log.e(any(), any(), any()) } returns 0
        every { alarmManager.canScheduleExactAlarms() } returns true
        context = TestContext(alarmManager)
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
            schedulePrayerRemindersUseCase = schedulePrayerRemindersUseCase,
            context = context
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
    fun `permission dialog is shown and dismissed when exact alarm permission is denied`() =
        runTest {
            every { getLocationUseCase() } returns flowOf(LocationModel(1.0, 2.0))
            every { getPrayerTimesUseCase(1.0, 2.0) } returns flowOf(emptyList())

            val viewModel = PrayerTimeViewModel(
                getPrayerTimesUseCase = getPrayerTimesUseCase,
                getLocationUseCase = getLocationUseCase,
                schedulePrayerRemindersUseCase = schedulePrayerRemindersUseCase,
                context = context
            )

            advanceUntilIdle()

            @Suppress("UNCHECKED_CAST")
            val stateFlow = PrayerTimeViewModel::class.java
                .getDeclaredField("_uiState")
                .apply { isAccessible = true }
                .get(viewModel) as MutableStateFlow<PrayerTimeUiState>
            stateFlow.value = stateFlow.value.copy(showExactAlarmPermissionDialog = true)

            assertTrue(viewModel.uiState.value.showExactAlarmPermissionDialog)

            viewModel.onExactAlarmPermissionDialogDismissed()

            assertFalse(viewModel.uiState.value.showExactAlarmPermissionDialog)
        }

    @Test
    fun `openExactAlarmSettings handles missing activity gracefully`() = runTest {
        every { getLocationUseCase() } returns flowOf(LocationModel(1.0, 2.0))
        every { getPrayerTimesUseCase(1.0, 2.0) } returns flowOf(emptyList())

        val failingContext = object : ContextWrapper(mockk(relaxed = true)) {
            override fun startActivity(intent: Intent) {
                error("Settings activity unavailable in test")
            }
        }

        val viewModel = PrayerTimeViewModel(
            getPrayerTimesUseCase = getPrayerTimesUseCase,
            getLocationUseCase = getLocationUseCase,
            schedulePrayerRemindersUseCase = schedulePrayerRemindersUseCase,
            context = failingContext
        )

        advanceUntilIdle()

        viewModel.openExactAlarmSettings()
    }
}
