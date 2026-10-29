package com.example.despensacx.data

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface TiendaTotalDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(tiendaTotal: TiendaTotalEntity)

    @Query("DELETE FROM tienda_totales WHERE listaId = :listaId AND tiendaId = :tiendaId")
    suspend fun delete(listaId: Long, tiendaId: Long)

    @Query("SELECT * FROM tienda_totales WHERE listaId = :listaId")
    fun getTotalesByLista(listaId: Long): LiveData<List<TiendaTotalEntity>>

    @Query("SELECT * FROM tienda_totales WHERE listaId = :listaId")
    suspend fun getTotalesByListaSync(listaId: Long): List<TiendaTotalEntity>
}
