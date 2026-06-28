package com.kellinreaver.rubricislam.data.repository

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.edit
import com.kellinreaver.rubricislam.domain.model.PrayerReminder
import com.kellinreaver.rubricislam.domain.repository.ReminderRepository
import com.kellinreaver.rubricislam.worker.AlarmReceiver
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class ReminderRepositoryImpl
@Inject
constructor(
    @param:ApplicationContext private val context: Context
) : ReminderRepository {
    private val sharedPrefs = context.getSharedPreferences("reminder_prefs", Context.MODE_PRIVATE)

    private val prayers = listOf("Fajr", "Dhuhr", "Asr", "Maghrib", "Isha")

    private val reminders = MutableStateFlow(loadRemindersFromPrefs())

    override fun getPrayerReminders(): Flow<List<PrayerReminder>> = reminders.asStateFlow()

    override suspend fun toggleReminder(prayerName: String, isEnabled: Boolean) {
        sharedPrefs.edit { putBoolean(prayerName, isEnabled) }
        reminders.update { current ->
            current.map {
                if (it.prayerName == prayerName) it.copy(isEnabled = isEnabled) else it
            }
        }
    }

    override fun isReminderEnabled(prayerName: String): Boolean =
        sharedPrefs.getBoolean(prayerName, true)

    private fun loadRemindersFromPrefs(): List<PrayerReminder> = prayers.map { name ->
        PrayerReminder(
            prayerName = name,
            isEnabled = sharedPrefs.getBoolean(name, true)
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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            !alarmManager.canScheduleExactAlarms()
        ) {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                timeInMillis,
                pendingIntent
            )
        } else {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                timeInMillis,
                pendingIntent
            )
        }
    }
}
