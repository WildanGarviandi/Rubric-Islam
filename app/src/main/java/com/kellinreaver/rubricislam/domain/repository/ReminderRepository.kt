package com.kellinreaver.rubricislam.domain.repository

import kotlinx.coroutines.flow.Flow

interface ReminderRepository {
    fun getUpcomingReminders(): Flow<List<String>>
}
