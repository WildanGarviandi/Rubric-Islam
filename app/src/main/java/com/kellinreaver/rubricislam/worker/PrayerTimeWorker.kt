package com.kellinreaver.rubricislam.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.kellinreaver.rubricislam.data.repository.ReminderRepositoryImpl
import com.kellinreaver.rubricislam.domain.usecase.GetLocationUseCase
import com.kellinreaver.rubricislam.domain.usecase.GetPrayerTimesUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.flow.first

@HiltWorker
class PrayerTimeWorker
@AssistedInject
constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val getPrayerTimesUseCase: GetPrayerTimesUseCase,
    private val getLocationUseCase: GetLocationUseCase,
    private val reminderRepository: ReminderRepositoryImpl
) : CoroutineWorker(context, workerParams) {
    override suspend fun doWork(): Result = try {
        val location = getLocationUseCase().first()
        val prayerTimes = getPrayerTimesUseCase(
            location.latitude,
            location.longitude
        ).first()

        val now = LocalTime.now()
        val today = LocalDate.now()
        val formatter = DateTimeFormatter.ofPattern("HH:mm")

        prayerTimes.forEach { prayer ->
            if (!reminderRepository.isReminderEnabled(prayer.name)) {
                Log.d(TAG, "Skipping ${prayer.name} as it is disabled")
                return@forEach
            }
            try {
                val prayerTime = LocalTime.parse(prayer.time, formatter)

                // Schedule for today if it hasn't passed, otherwise schedule for tomorrow
                val scheduledDate = if (prayerTime.isAfter(now)) today else today.plusDays(1)
                val zonedDateTime = prayerTime.atDate(scheduledDate)
                    .atZone(ZoneId.systemDefault())

                reminderRepository.schedulePrayerAlarm(
                    prayer.name,
                    zonedDateTime.toInstant().toEpochMilli()
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to parse/schedule time ${prayer.time}: ${e.message}")
            }
        }
        Result.success()
    } catch (e: Exception) {
        Log.e(TAG, "Error in PrayerTimeWorker: ${e.message}", e)
        Result.retry()
    }

    companion object {
        private const val TAG = "PrayerTimeWorker"
    }
}
