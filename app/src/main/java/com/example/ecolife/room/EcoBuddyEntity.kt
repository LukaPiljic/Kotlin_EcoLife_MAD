package com.example.ecolife.room

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ecobuddy")
data class EcoBuddyEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val createdAt: Long,
    val hp: Double = 100.0,
    val lastCheckTimestamp: Long = System.currentTimeMillis(),
    val isAlive: Boolean = true
)
