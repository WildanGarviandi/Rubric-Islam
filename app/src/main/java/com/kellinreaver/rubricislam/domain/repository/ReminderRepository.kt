package com.kellinreaver.rubricislam.domain.repository

import com.kellinreaver.rubricislam.domain.model.PrayerReminder
import kotlinx.coroutines.flow.Flow

interface ReminderRepository {
    fun getPrayerReminders(): Flow<List<PrayerReminder>>
    suspend fun toggleReminder(prayerName: String, isEnabled: Boolean)
    fun isReminderEnabled(prayerName: String): Boolean
}
