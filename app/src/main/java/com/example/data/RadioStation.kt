package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "radio_stations")
data class RadioStation(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val url: String,
    val genre: String = "Varios",
    val isFavorite: Boolean = false,
    val isCustom: Boolean = false,
    val country: String = "",
    val region: String = "",
    val province: String = "",
    val district: String = ""
)
