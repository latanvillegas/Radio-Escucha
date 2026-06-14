package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class RadioRepository(private val radioDao: RadioDao) {

    val allStations: Flow<List<RadioStation>> = radioDao.getAllStations()

    suspend fun insert(station: RadioStation): Long {
        return radioDao.insertStation(station)
    }

    suspend fun update(station: RadioStation) {
        radioDao.updateStation(station)
    }

    suspend fun delete(station: RadioStation) {
        radioDao.deleteStation(station)
    }

    suspend fun deleteById(id: Int) {
        radioDao.deleteStationById(id)
    }

    suspend fun checkAndPrepopulate() {
        val current = radioDao.getAllStations().first()
        if (current.isEmpty()) {
            val defaults = listOf(
                RadioStation(
                    name = "SomaFM Groove Salad",
                    url = "https://ice1.somafm.com/groovesalad-128-mp3",
                    genre = "Relax / Ambient",
                    isCustom = false
                ),
                RadioStation(
                    name = "Ibiza Global Radio",
                    url = "https://live.ibizaglobalradio.com/stream",
                    genre = "Electrónica / House",
                    isCustom = false
                ),
                RadioStation(
                    name = "Jazz24 Seattle",
                    url = "https://live.jazz24.org/jazz24-mp3",
                    genre = "Jazz / Blues",
                    isCustom = false
                ),
                RadioStation(
                    name = "SomaFM Indie Pop Rocks",
                    url = "https://ice1.somafm.com/indiepop-128-mp3",
                    genre = "Indie Rock",
                    isCustom = false
                ),
                RadioStation(
                    name = "KEXP Alternative",
                    url = "https://kexp-mp3-128.streamguys1.com/kexp128.mp3",
                    genre = "Alternativo",
                    isCustom = false
                )
            )
            radioDao.insertStations(defaults)
        }
    }
}
