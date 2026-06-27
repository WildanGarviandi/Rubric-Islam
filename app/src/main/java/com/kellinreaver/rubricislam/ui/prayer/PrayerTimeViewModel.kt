package com.kellinreaver.rubricislam.ui.prayer

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kellinreaver.rubricislam.domain.model.PrayerTime
import com.kellinreaver.rubricislam.domain.usecase.GetLocationUseCase
import com.kellinreaver.rubricislam.domain.usecase.GetPrayerTimesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@HiltViewModel
class PrayerTimeViewModel
@Inject
constructor(
    private val getPrayerTimesUseCase: GetPrayerTimesUseCase,
    private val getLocationUseCase: GetLocationUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(PrayerTimeUiState())
    val uiState: StateFlow<PrayerTimeUiState> = _uiState.asStateFlow()

    init {
        loadPrayerTimes()
    }

    private fun loadPrayerTimes() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            getLocationUseCase().collectLatest { location ->
                Log.i(TAG, "Got location: $location")
                getPrayerTimesUseCase(
                    location.latitude,
                    location.longitude
                ).collectLatest { times ->
                    val predictedTimes = predictNextPrayer(times)
                    _uiState.value =
                        _uiState.value.copy(
                            prayerTimes = predictedTimes,
                            isLoading = false,
                            error = null,
                            todayDate = getTodayFormattedDate()
                        )
                }
            }
        }
    }

    private fun predictNextPrayer(times: List<PrayerTime>): List<PrayerTime> {
        if (times.isEmpty()) return times

        val currentTime = LocalTime.now()
        val timeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())

        // Try to find the first prayer that is after the current time
        var nextPrayerIndex = times.indexOfFirst {
            try {
                val prayerTime = LocalTime.parse(it.time, timeFormatter)
                prayerTime.isAfter(currentTime)
            } catch (_: Exception) {
                false
            }
        }

        // If no prayer is after current time, it means the next prayer is Fajr of the next day (index 0)
        if (nextPrayerIndex == -1) {
            nextPrayerIndex = 0
        }

        return times.mapIndexed { index, prayerTime ->
            prayerTime.copy(isNext = index == nextPrayerIndex)
        }
    }

    private fun getTodayFormattedDate(): String {
        val date = java.time.LocalDate.now()
        val formatter = DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", Locale.getDefault())
        return date.format(formatter)
    }

    companion object {
        private const val TAG = "PrayerTimeVM"
    }
}
