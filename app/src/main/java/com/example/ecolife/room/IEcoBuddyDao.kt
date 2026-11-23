package com.example.ecolife.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface IEcoBuddyDao {

    @Insert
    suspend fun insert(buddy: EcoBuddyEntity)

    @Update
    suspend fun update(buddy: EcoBuddyEntity)

    @Query("SELECT * FROM ecobuddy WHERE isAlive = 1 ORDER BY createdAt DESC LIMIT 1")
    suspend fun getActiveBuddy(): EcoBuddyEntity?

    @Query("SELECT * FROM ecobuddy ORDER BY createdAt DESC")
    suspend fun getAll(): List<EcoBuddyEntity>
}
