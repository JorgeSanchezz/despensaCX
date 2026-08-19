package com.example.despensacx.data

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface ProductoDao {
    @Insert
    suspend fun insert(producto: ProductoEntity): Long

    @Update
    suspend fun update(producto: ProductoEntity)

    @Delete
    suspend fun delete(producto: ProductoEntity)

    @Query("SELECT * FROM productos WHERE listaId = :listaId")
    fun getProductosByLista(listaId: Long): LiveData<List<ProductoEntity>>

    @Query("SELECT * FROM productos WHERE listaId = :listaId")
    fun getProductosByListaSync(listaId: Long): List<ProductoEntity>

    @Query("UPDATE productos SET seleccionado = :seleccionado WHERE listaId = :listaId")
    suspend fun setAllSeleccionadoSync(listaId: Long, seleccionado: Boolean)

    @Query("SELECT * FROM productos")
    fun getAllSync(): List<ProductoEntity>

    @Query("DELETE FROM productos")
    suspend fun deleteAll()
}
