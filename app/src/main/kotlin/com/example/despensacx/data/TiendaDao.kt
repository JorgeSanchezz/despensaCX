package com.example.despensacx.data

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface TiendaDao {
    @Insert
    suspend fun insert(tienda: TiendaEntity): Long

    @Update
    suspend fun update(tienda: TiendaEntity)

    @Delete
    suspend fun delete(tienda: TiendaEntity)

    @Query("SELECT * FROM tiendas ORDER BY orden ASC, nombre ASC")
    fun getAllTiendas(): LiveData<List<TiendaEntity>>

    @Query("SELECT * FROM tiendas ORDER BY orden ASC, nombre ASC")
    fun getAllTiendasSync(): List<TiendaEntity>

    @Query("SELECT COUNT(*) FROM tiendas")
    fun getCountSync(): Int

    @Query("SELECT COUNT(*) FROM productos WHERE tiendaId = :tiendaId")
    fun countProductosByTiendaSync(tiendaId: Long): Int

    @Query("DELETE FROM tiendas")
    suspend fun deleteAll()
}
