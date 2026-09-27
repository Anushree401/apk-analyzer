package com.example.android_manager.data.history

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

/**
 * Main Room database instance for the APVM application.
 */
@Database(
    entities = [ScanHistoryEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(ScanHistoryConverters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun scanHistoryDao(): ScanHistoryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "apvm_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}