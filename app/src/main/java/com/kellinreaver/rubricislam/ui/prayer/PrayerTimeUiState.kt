package com.kellinreaver.rubricislam.ui.prayer

import com.kellinreaver.rubricislam.domain.model.PrayerTime

data class PrayerTimeUiState(
    val prayerTimes: List<PrayerTime> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val todayDate: String = ""
)
