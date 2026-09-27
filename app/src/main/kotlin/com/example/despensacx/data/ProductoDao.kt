package com.example.despensacx.data

import androidx.lifecycle.LiveData
import androidx.room.*

data class ListaResumen(
    val listaId: Long,
    val total: Double,
    val totalSeleccionado: Double,
    val totalProductos: Int
)

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

    @Query("""
        SELECT 
            l.id AS listaId,
            COALESCE(SUM(p.precio * p.cantidad), 0.0) AS total,
            COALESCE(SUM(CASE WHEN p.seleccionado = 1 THEN p.precio * p.cantidad ELSE 0.0 END), 0.0) AS totalSeleccionado,
            COUNT(p.id) AS totalProductos
        FROM listas l
        LEFT JOIN productos p ON l.id = p.listaId
        GROUP BY l.id
    """)
    fun getResumenListas(): LiveData<List<ListaResumen>>

    @Query("DELETE FROM productos")
    suspend fun deleteAll()
}
