package com.kellinreaver.rubricislam.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.kellinreaver.rubricislam.data.local.dao.PrayerTimeDao
import com.kellinreaver.rubricislam.data.local.entity.PrayerTimeEntity

@Database(entities = [PrayerTimeEntity::class], version = 1, exportSchema = false)
abstract class RubricDatabase : RoomDatabase() {
    abstract fun prayerTimeDao(): PrayerTimeDao
}
