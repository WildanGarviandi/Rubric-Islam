package com.kellinreaver.rubricislam.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.kellinreaver.rubricislam.domain.usecase.GetLocationUseCase
import com.kellinreaver.rubricislam.domain.usecase.GetPrayerTimesUseCase
import com.kellinreaver.rubricislam.domain.usecase.SchedulePrayerRemindersUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

@HiltWorker
class PrayerTimeWorker
@AssistedInject
constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val getPrayerTimesUseCase: GetPrayerTimesUseCase,
    private val getLocationUseCase: GetLocationUseCase,
    private val schedulePrayerRemindersUseCase: SchedulePrayerRemindersUseCase
) : CoroutineWorker(context, workerParams) {
    override suspend fun doWork(): Result = try {
        val location = getLocationUseCase().first()
        val prayerTimes = getPrayerTimesUseCase(
            location.latitude,
            location.longitude
        ).first()

        schedulePrayerRemindersUseCase(prayerTimes)
        Result.success()
    } catch (e: Exception) {
        Log.e(TAG, "Error in PrayerTimeWorker: ${e.message}", e)
        Result.retry()
    }

    companion object {
        private const val TAG = "PrayerTimeWorker"
    }
}
