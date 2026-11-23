package com.example.ecolife.room

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "coordinates")
data class CoordinateEntity(
    @PrimaryKey val timestamp: Long,
    val latitude: Double,
    val longitude: Double,
    val altitude: Double
)
