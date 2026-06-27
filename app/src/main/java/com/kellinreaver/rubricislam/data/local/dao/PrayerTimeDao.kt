package com.kellinreaver.rubricislam.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.kellinreaver.rubricislam.data.local.entity.PrayerTimeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PrayerTimeDao {
    @Query("SELECT * FROM prayer_times WHERE date = :date LIMIT 1")
    fun getPrayerTimesForDate(date: String): Flow<PrayerTimeEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPrayerTimes(prayerTime: PrayerTimeEntity)

    @Query("DELETE FROM prayer_times")
    suspend fun clearAll()
}
