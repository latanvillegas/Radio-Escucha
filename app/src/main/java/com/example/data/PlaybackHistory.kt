package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "playback_history")
data class PlaybackHistory(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val stationId: Long,
    val stationName: String,
    val stationGenre: String,
    val timestamp: Long = System.currentTimeMillis()
)
