package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaybackHistoryDao {
    @Query("SELECT * FROM playback_history ORDER BY timestamp DESC LIMIT 100")
    fun getRecentHistory(): Flow<List<PlaybackHistory>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistoryEntry(historyEntry: PlaybackHistory)

    @Query("DELETE FROM playback_history")
    suspend fun clearHistory()
}
