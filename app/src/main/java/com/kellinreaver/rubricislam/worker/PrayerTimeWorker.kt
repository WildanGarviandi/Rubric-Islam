package com.kellinreaver.rubricislam.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.kellinreaver.rubricislam.data.repository.ReminderRepositoryImpl
import com.kellinreaver.rubricislam.domain.usecase.GetLocationUseCase
import com.kellinreaver.rubricislam.domain.usecase.GetPrayerTimesUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.util.*
import java.text.SimpleDateFormat

@HiltWorker
class PrayerTimeWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val getPrayerTimesUseCase: GetPrayerTimesUseCase,
    private val getLocationUseCase: GetLocationUseCase,
    private val reminderRepository: ReminderRepositoryImpl
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val location = getLocationUseCase().first()
            val prayerTimes = getPrayerTimesUseCase(location.latitude, location.longitude).first()

            prayerTimes.forEach { prayer ->
                val timeInMillis = parseTimeToMillis(prayer.time)
                if (timeInMillis != null) {
                    reminderRepository.schedulePrayerAlarm(prayer.name, timeInMillis)
                }
            }
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    private fun parseTimeToMillis(time: String): Long? {
        return try {
            val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
            val date = sdf.parse(time) ?: return null
            date.time
        } catch (e: Exception) {
            null
        }
    }
}
