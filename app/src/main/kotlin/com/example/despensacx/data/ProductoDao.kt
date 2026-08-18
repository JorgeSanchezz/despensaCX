package com.example.despensacx.data

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface ProductoDao {
    @Insert
    fun insert(producto: ProductoEntity): Long

    @Update
    fun update(producto: ProductoEntity)

    @Delete
    fun delete(producto: ProductoEntity)

    @Query("SELECT * FROM productos WHERE listaId = :listaId")
    fun getProductosByLista(listaId: Long): LiveData<List<ProductoEntity>>

    @Query("SELECT * FROM productos WHERE listaId = :listaId")
    fun getProductosByListaSync(listaId: Long): List<ProductoEntity>

    @Query("UPDATE productos SET seleccionado = :seleccionado WHERE listaId = :listaId")
    fun setAllSeleccionadoSync(listaId: Long, seleccionado: Boolean)

    @Query("SELECT * FROM productos")
    fun getAllSync(): List<ProductoEntity>
}
