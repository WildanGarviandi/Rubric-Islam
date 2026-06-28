package com.kellinreaver.rubricislam.domain.model

data class PrayerReminder(val prayerName: String, val isEnabled: Boolean, val time: String? = null)
