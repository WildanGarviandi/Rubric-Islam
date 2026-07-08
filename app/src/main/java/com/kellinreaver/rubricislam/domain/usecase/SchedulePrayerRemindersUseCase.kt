package com.kellinreaver.rubricislam.domain.usecase

import android.util.Log
import com.kellinreaver.rubricislam.domain.model.PrayerTime
import com.kellinreaver.rubricislam.domain.repository.ReminderRepository
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import kotlinx.coroutines.flow.first

class SchedulePrayerRemindersUseCase @Inject constructor(
    private val reminderRepository: ReminderRepository
) {
    suspend operator fun invoke(prayerTimes: List<PrayerTime>) {
        val reminders = reminderRepository.getPrayerReminders().first()
        val enabledMap = reminders.associateBy { it.prayerName }
        val now = LocalTime.now()
        val today = LocalDate.now()
        val formatter = DateTimeFormatter.ofPattern("HH:mm")

        prayerTimes.forEach { prayer ->
            val reminder = enabledMap[prayer.name]
            if (reminder == null || !reminder.isEnabled) {
                reminderRepository.cancelPrayerAlarm(prayer.name)
                return@forEach
            }

            try {
                // Aladhan API sometimes returns "HH:mm (Timezone)". We only need "HH:mm".
                val timeString = prayer.time.split(" ")[0]
                val prayerTime = LocalTime.parse(timeString, formatter)

                // Schedule for today if it hasn't passed, otherwise schedule for tomorrow
                val scheduledDate = if (prayerTime.isAfter(now)) today else today.plusDays(1)
                val zonedDateTime = prayerTime.atDate(scheduledDate)
                    .atZone(ZoneId.systemDefault())

                reminderRepository.schedulePrayerAlarm(
                    prayer.name,
                    zonedDateTime.toInstant().toEpochMilli()
                )
            } catch (e: Exception) {
                Log.e(
                    TAG,
                    "Failed to parse/schedule time ${prayer.time}: ${e.message}"
                )
            }
        }
    }

    companion object {
        private const val TAG = "SchedulePrayerRemindersUseCase"
    }
}
