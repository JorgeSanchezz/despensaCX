package com.example.despensacx.data

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface MembresiaDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(membresia: MembresiaEntity)

    @Update
    suspend fun update(membresia: MembresiaEntity)

    @Delete
    suspend fun delete(membresia: MembresiaEntity)

    @Query("SELECT * FROM membresias")
    fun getAll(): LiveData<List<MembresiaEntity>>

    @Query("SELECT * FROM membresias")
    fun getAllSync(): List<MembresiaEntity>

    @Query("DELETE FROM membresias")
    suspend fun deleteAll()
}
