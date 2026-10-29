package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaybackHistoryDao {
    @Query("SELECT * FROM playback_history ORDER BY timestamp DESC LIMIT 50")
    fun getRecentHistory(): Flow<List<PlaybackHistory>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistoryEntry(historyEntry: PlaybackHistory)

    @Query("DELETE FROM playback_history WHERE stationName = :stationName")
    suspend fun deleteByStationName(stationName: String)

    @Query("DELETE FROM playback_history WHERE id NOT IN (SELECT id FROM playback_history ORDER BY timestamp DESC LIMIT 50)")
    suspend fun trimOldHistory()

    @Transaction
    suspend fun recordStationPlay(historyEntry: PlaybackHistory) {
        deleteByStationName(historyEntry.stationName)
        insertHistoryEntry(historyEntry)
        trimOldHistory()
    }

    @Query("DELETE FROM playback_history")
    suspend fun clearHistory()
}
