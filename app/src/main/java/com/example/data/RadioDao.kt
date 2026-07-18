package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface RadioDao {
    @Query("SELECT * FROM radio_stations ORDER BY name ASC")
    fun getAllStations(): Flow<List<RadioStation>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStation(station: RadioStation): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertStations(stations: List<RadioStation>)

    @Update
    suspend fun updateStation(station: RadioStation)

    @Delete
    suspend fun deleteStation(station: RadioStation)

    @Query("DELETE FROM radio_stations WHERE id = :id")
    suspend fun deleteStationById(id: Int)

    @Query("DELETE FROM radio_stations WHERE isCustom = 0")
    suspend fun deleteDefaultStations()

    @Query("SELECT * FROM radio_stations WHERE id = :id")
    suspend fun getStationById(id: Long): RadioStation?
}
