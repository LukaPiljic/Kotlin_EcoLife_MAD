package com.example.ecolife.room

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "observations")
data class ObservationEntity(
    @PrimaryKey val timestamp: Long,
    val type: String,
    val value: String,
    val note: String,
    val lat: String,
    val lon: String
)
