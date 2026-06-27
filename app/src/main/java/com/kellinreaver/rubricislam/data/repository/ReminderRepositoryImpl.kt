package com.kellinreaver.rubricislam.data.repository

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.kellinreaver.rubricislam.domain.repository.ReminderRepository
import com.kellinreaver.rubricislam.worker.AlarmReceiver
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class ReminderRepositoryImpl
@Inject
constructor(
    @param:ApplicationContext private val context: Context
) : ReminderRepository {
    override fun getUpcomingReminders(): Flow<List<String>> = flow {
        // For now, returning a dummy list.
        // In a full implementation, we'd query the AlarmManager or a local database.
        emit(
            listOf(
                "Fajr Reminder",
                "Dhuhr Reminder",
                "Asr Reminder",
                "Maghrib Reminder",
                "Isha Reminder"
            )
        )
    }

    @SuppressLint("ScheduleExactAlarm")
    fun schedulePrayerAlarm(prayerName: String, timeInMillis: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent =
            Intent(context, AlarmReceiver::class.java).apply {
                putExtra("PRAYER_NAME", prayerName)
            }
        val pendingIntent =
            PendingIntent.getBroadcast(
                context,
                prayerName.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            timeInMillis,
            pendingIntent
        )
    }
}
