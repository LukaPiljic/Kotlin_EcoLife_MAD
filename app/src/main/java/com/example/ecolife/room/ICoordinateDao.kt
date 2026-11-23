package com.example.ecolife.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ICoordinateDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(coord: CoordinateEntity)

    @Query("SELECT * FROM coordinates ORDER BY timestamp ASC")
    suspend fun getAll(): List<CoordinateEntity>
}
