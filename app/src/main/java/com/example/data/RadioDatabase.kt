package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [RadioStation::class, PlaybackHistory::class], version = 3, exportSchema = false)
abstract class RadioDatabase : RoomDatabase() {
    abstract fun radioDao(): RadioDao
    abstract fun playbackHistoryDao(): PlaybackHistoryDao

    companion object {
        @Volatile
        private var INSTANCE: RadioDatabase? = null

        fun getDatabase(context: Context): RadioDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    RadioDatabase::class.java,
                    "radio_database"
                )
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
