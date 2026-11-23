package com.example.ecolife.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface IObservationDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(obs: ObservationEntity)

    @Query("SELECT * FROM observations ORDER BY timestamp DESC")
    suspend fun getAll(): List<ObservationEntity>

    @Query("SELECT COUNT(*) FROM observations")
    suspend fun getCount(): Int
}
